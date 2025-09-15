package com.findex.service;

import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.indexinfo.IndexInfoDto;
import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.IndexDataOpenApiResult;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.dto.syncjob.OpenApiIndexDataItem;
import com.findex.dto.syncjob.OpenApiIndexInfoItem;
import com.findex.dto.syncjob.OpenApiIndexInfoResponse;
import com.findex.dto.syncjob.SyncJobQuery;
import com.findex.entity.IndexData;
import com.findex.entity.IndexInfo;
import com.findex.enums.IndexSourceType;
import com.findex.enums.SyncJobStatus;
import com.findex.mapper.IndexInfoMapper;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import com.findex.repository.syncjob.SyncJobRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncJobService {

    private final MarketIndexClient client;
    private final SyncJobRepository syncJobRepository;
    private final IndexInfoRepository indexInfoRepository;
    private final IndexDataRepository indexDataRepository;
    private final IndexInfoMapper indexInfoMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int NUM_OF_ROWS = 300;
    private static final int PAGE_SIZE = 500;

    public CursorPageResponse findAll(SyncJobQuery query) {
        return syncJobRepository.findAll(query);
    }

    @Transactional
    public List<IndexInfoDto> syncIndexInfo() {
        OpenApiIndexInfoResponse response = client.callGetStockMarketIndex(NUM_OF_ROWS);
        if (response == null || response.items() == null || response.items().isEmpty()) {
            return List.of();
        }

        List<IndexInfoDto> out = new ArrayList<>();

        for (OpenApiIndexInfoItem item : response.items()) {
            //값 정리
            String indexClassification  = normIndexInfo(item.idxCsf());
            String indexName = normIndexInfo(item.idxNm());
            Integer employedItemsCount = item.epyItmsCnt();
            LocalDate basePointInTime = parseDate(item.basPntm());
            Integer baseIndex = item.basIdx();

            // null 있으면 생성/업데이트 모두 스킵해서 호출 (요구사항 개수와 같음)
            if (indexClassification == null || indexName == null || employedItemsCount == null || basePointInTime == null || baseIndex == null) {
                continue;
            }

            // 빌더 제거 후 upsert 사용
            Optional<IndexInfo> exist = indexInfoRepository.findByIndexClassificationAndIndexName(indexClassification, indexName);
            IndexInfo saved;
            if (exist.isPresent()) {
                IndexInfo indexInfo = exist.get();
                indexInfo.update(employedItemsCount, basePointInTime, baseIndex, null);
                saved = indexInfoRepository.save(indexInfo);
            } else {
                saved = indexInfoRepository.save(new IndexInfo(
                    indexClassification,
                    indexName,
                    employedItemsCount,
                    basePointInTime,
                    baseIndex,
                    IndexSourceType.OPEN_API,
                    false
                ));
            }

            out.add(indexInfoMapper.toDto(saved));
        }

        return out;
    }

    private static String normIndexInfo(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static LocalDate parseDate(String s) {
        try {
            return (s == null || s.isBlank()) ? null : LocalDate.parse(s.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.error("[index-data] parseYmd failed: {}", e.toString());
            return null;
        }
    }

    @Transactional
    public List<IndexDataOpenApiResult> syncIndexData(IndexDataOpenApiSyncRequest req, String worker) {
        // 날짜 정규화
        LocalDate from = req.baseDateFrom();
        LocalDate to   = req.baseDateTo();
        if (from.isAfter(to)) { LocalDate t = from; from = to; to = t; }

        // id 목록 순회
        var ids = new java.util.LinkedHashSet<>(req.indexInfoIds());
        var results = new java.util.ArrayList<IndexDataOpenApiResult>();

        for (Long indexInfoId : ids) {
            boolean ok = false;
            try {
                var one = syncOneIndex(indexInfoId, from, to);
                results.add(one);
                ok = true;
            } catch (Exception e) {
                // 실패한 요청 기록남김
                log.error("[index-data] indexInfoId={} sync failed: {}", indexInfoId, e.toString());
                // 필요 시 rethrow
            } finally {
                // 대상날짜
                saveJob(indexInfoId, to, worker, ok);
            }
        }
        return results;
    }

    // 기록 저장
    private void saveJob(Long indexInfoId, java.time.LocalDate targetDate, String worker, boolean ok) {
        var job = new com.findex.entity.SyncJob(
            indexInfoId,
            com.findex.enums.JobType.INDEX_DATA,
            targetDate,
            worker,
            ok ? SyncJobStatus.SUCCESS : SyncJobStatus.FAILED
        );
        syncJobRepository.save(job);
    }

    // 단일 인덱스 처리
    private IndexDataOpenApiResult syncOneIndex(Long indexInfoId, LocalDate from, LocalDate to) {
        IndexInfo info = indexInfoRepository.getOrThrow(indexInfoId);

        List<OpenApiIndexDataItem> items;

        // idxNm + idxCsf
        items = fetchAllPages(info.getIndexName(), info.getIndexClassification(), from, to);

        // idxNm만
        if (items.isEmpty()) {
            items = fetchAllPages(info.getIndexName(), null, from, to);
            log.warn("[index-data] fallback#1 only idxNm. indexInfoIds={}, collected={}", indexInfoId, items.size());
        }

        // 전체 받아 로컬 필터
        if (items.isEmpty()) {
            List<OpenApiIndexDataItem> all = fetchAllPages(null, null, from, to);
            String want = norm(info.getIndexName());
            items = all.stream().filter(it -> want.equals(norm(it.indexName()))).toList();
            log.warn("[index-data] fallback#2 local filter. indexInfoIds={}, collected={}", indexInfoId, items.size());
        }

        // 업서트
        int saved = 0;
        for (var it : items) {
            if (it.baseDate() == null) continue;
            IndexData e = indexDataRepository.findByIndexInfoIdAndBaseDate(indexInfoId, it.baseDate())
                .orElseGet(() -> new IndexData(indexInfoId, it.baseDate(), IndexSourceType.OPEN_API));
            e.updatePrices(it.marketPrice(), it.closingPrice(), it.highPrice(), it.lowPrice());
            e.updateFluctuation(it.versus(), it.fluctuationRate());
            e.updateTrading(it.tradingQuantity(), it.tradingPrice(), it.marketTotalAmount());
            indexDataRepository.save(e);
            saved++;
        }
        log.info("[index-data] indexInfoIds={} upsert saved={}", indexInfoId, saved);

        // 조회/반환
        var list = fetchByIndexId(indexInfoId, from, to);
        if (!list.isEmpty()) return list.get(0);

        // 저장 0건이어도 응답 스키마 유지
        return new IndexDataOpenApiResult(
            info.getIndexName(),
            java.util.List.of(new IndexDataOpenApiResult.Group(indexInfoId, info.getIndexClassification(), java.util.List.of()))
        );
    }

    @Transactional(readOnly = true)
    public List<IndexDataOpenApiResult> fetchByIndexId(Long indexInfoId, LocalDate from, LocalDate to) {
        var rows = indexDataRepository.findJoinedSortedByIndexId(indexInfoId, from, to);

        Map<Long, List<IndexDataDto>> byInfoId = new LinkedHashMap<>();
        for (var r : rows) {
            byInfoId.computeIfAbsent(r.indexInfoId(), k -> new ArrayList<>())
                .add(new IndexDataDto(
                    null, r.indexInfoId(), r.baseDate(), r.sourceType(),
                    r.marketPrice(), r.closingPrice(), r.highPrice(), r.lowPrice(),
                    r.versus(), r.fluctuationRate(), r.tradingQuantity(), r.tradingPrice(), r.marketTotalAmount()
                ));
        }

        IndexInfo info = indexInfoRepository.getOrThrow(indexInfoId);
        List<IndexDataOpenApiResult.Group> groups = new ArrayList<>();
        byInfoId.forEach((id, list) ->
            groups.add(new IndexDataOpenApiResult.Group(id, info.getIndexClassification(), list))
        );

        return List.of(new IndexDataOpenApiResult(info.getIndexName(), groups));
    }

    // 페이징 수집
    private List<OpenApiIndexDataItem> fetchAllPages(
        String idxNm, String idxCsf, LocalDate from, LocalDate to
    ) {
        List<OpenApiIndexDataItem> out = new ArrayList<>();
        int page = 1;
        while (true) {
            var pr = client.callGetStockMarketIndexData(idxNm, idxCsf, from, to, page, PAGE_SIZE);
            if (pr.items() != null && !pr.items().isEmpty()) out.addAll(pr.items());
            if (pr.items() == null || pr.items().isEmpty() || page * pr.numOfRows() >= pr.totalCount()) break;
            page++;
        }
        return out;
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }
}
