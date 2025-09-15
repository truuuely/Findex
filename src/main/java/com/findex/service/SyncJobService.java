package com.findex.service;

import com.findex.dto.indexdata.IndexDataDto;
import com.findex.dto.response.CursorPageResponse;
import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.dto.syncjob.NormalizedIndexInfoItem;
import com.findex.dto.syncjob.OpenApiIndexDataItem;
import com.findex.dto.syncjob.OpenApiIndexInfoItem;
import com.findex.dto.syncjob.SyncJobDto;
import com.findex.dto.syncjob.SyncJobQuery;
import com.findex.entity.IndexData;
import com.findex.entity.IndexInfo;
import com.findex.entity.SyncJob;
import com.findex.enums.IndexSourceType;
import com.findex.enums.JobType;
import com.findex.enums.SyncJobStatus;
import com.findex.mapper.SyncJobMapper;
import com.findex.openapi.MarketIndexClient;
import com.findex.repository.indexdata.IndexDataRepository;
import com.findex.repository.indexinfo.IndexInfoRepository;
import com.findex.repository.syncjob.SyncJobRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SyncJobService {

    private final MarketIndexClient client;
    private final SyncJobRepository syncJobRepository;
    private final IndexInfoRepository indexInfoRepository;
    private final IndexDataRepository indexDataRepository;
    private final SyncJobMapper syncJobMapper;

    private static final int PAGE_SIZE = 500;

    public CursorPageResponse findAll(SyncJobQuery query) {
        return syncJobRepository.findAll(query);
    }

    @Transactional
    public List<SyncJobDto> syncIndexInfo(String worker) {
        List<OpenApiIndexInfoItem> items = client.getStockMarketIndex();
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        final List<NormalizedIndexInfoItem> normalizedItems = items.stream()
            .map(NormalizedIndexInfoItem::from)
            .filter(Objects::nonNull) // 필수값 누락은 스킵
            .toList();

        if (normalizedItems.isEmpty()) {
            return List.of();
        }

        List<String> keys = normalizedItems.stream()
            .map(n -> key(n.indexClassification(), n.indexName()))
            .distinct()
            .toList();

        Map<String, IndexInfo> existingByKey = indexInfoRepository.findAllByCompositeKeyIn(keys)
            .stream()
            .collect(Collectors.toMap(
                i -> key(i.getIndexClassification(), i.getIndexName()),
                Function.identity()
            ));

        // 신규 후보는 키 기준으로 한 번만 생성 (중복 insert 방지)
        Map<String, IndexInfo> stagedNewByKey = new LinkedHashMap<>();
        List<IndexInfo> toUpdate = new ArrayList<>();

        for (NormalizedIndexInfoItem item : normalizedItems) {
            String k = key(item.indexClassification(), item.indexName());
            IndexInfo exist = existingByKey.get(k);

            if (exist != null) {
                if (applyIfChanged(exist, item)) {
                    toUpdate.add(exist);
                }
            } else {
                IndexInfo staging = stagedNewByKey.get(k);
                if (staging == null) {
                    staging = new IndexInfo(
                        item.indexClassification(),
                        item.indexName(),
                        item.employedItemsCount(),
                        item.basePointInTime(),
                        item.baseIndex(),
                        IndexSourceType.OPEN_API,
                        false
                    );
                    stagedNewByKey.put(k, staging);
                } else {
                    staging.update(
                        item.employedItemsCount(),
                        item.basePointInTime(),
                        item.baseIndex(),
                        null
                    );
                }
            }
        }

        // 신규 일괄 저장 (중복 없음)
        List<IndexInfo> savedNew = stagedNewByKey.isEmpty()
            ? List.of()
            : indexInfoRepository.saveAll(stagedNewByKey.values());

        List<SyncJob> jobs = new ArrayList<>(normalizedItems.size());
        for (IndexInfo u : toUpdate) {
            jobs.add(new SyncJob(u.getId(), JobType.INDEX_INFO, null, worker, SyncJobStatus.SUCCESS));
        }
        for (IndexInfo c : savedNew) {
            jobs.add(new SyncJob(c.getId(), JobType.INDEX_INFO, null, worker, SyncJobStatus.SUCCESS));
        }

        List<SyncJob> savedJobs = jobs.isEmpty() ? List.of() : syncJobRepository.saveAll(jobs);
        return savedJobs.stream().map(syncJobMapper::toDto).toList();
    }

    @Transactional
    public List<SyncJobDto> syncIndexData(
        IndexDataOpenApiSyncRequest request,
        String worker
    ) {
        // 날짜 정규화
        LocalDate from = request.baseDateFrom();
        LocalDate to   = request.baseDateTo();
        if (from.isAfter(to)) {
            LocalDate temp = from;
            from = to;
            to = temp;
        }

        // id 목록 순회
        Set<Long> ids = new LinkedHashSet<>(request.indexInfoIds());
        List<SyncJobDto> results = new ArrayList<>();

        for (Long indexInfoId : ids) {
            boolean ok = false;
            try {
                List<SyncJobDto> syncJobs = syncOneIndex(indexInfoId, from, to);
                results.addAll(syncJobs);
                ok = true;
            } catch (Exception ignore) {
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
    private List<SyncJobDto> syncOneIndex(Long indexInfoId, LocalDate from, LocalDate to) {
        IndexInfo info = indexInfoRepository.getOrThrow(indexInfoId);

        List<OpenApiIndexDataItem> items;

        // idxNm + idxCsf
        items = fetchAllPages(info.getIndexName(), info.getIndexClassification(), from, to);

        // idxNm만
        if (items.isEmpty()) {
            items = fetchAllPages(info.getIndexName(), null, from, to);
        }

        // 전체 받아 로컬 필터
        if (items.isEmpty()) {
            List<OpenApiIndexDataItem> all = fetchAllPages(null, null, from, to);
            String want = norm(info.getIndexName());
            items = all.stream().filter(it -> want.equals(norm(it.indexName()))).toList();
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

        return null;
        // 조회/반환
        // SyncJobDto syncJobDto = fetchByIndexId(indexInfoId, from, to);
        // if (!list.isEmpty()) {
        //     return list.get(0);
        // }
        //
        // // 저장 0건이어도 응답 스키마 유지
        // return new IndexDataOpenApiResult(
        //     info.getIndexName(),
        //     List.of(new IndexDataOpenApiResult.Group(indexInfoId, info.getIndexClassification(), java.util.List.of()))
        // );
    }

    @Transactional(readOnly = true)
    public List<SyncJobDto> fetchByIndexId(Long indexInfoId, LocalDate from, LocalDate to) {
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

        return null;
        // IndexInfo info = indexInfoRepository.getOrThrow(indexInfoId);
        // List<IndexDataOpenApiResult.Group> groups = new ArrayList<>();
        // byInfoId.forEach((id, list) ->
        //     groups.add(new IndexDataOpenApiResult.Group(id, info.getIndexClassification(), list))
        // );
        //
        // return List.of(new IndexDataOpenApiResult(info.getIndexName(), groups));
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
    private boolean applyIfChanged(IndexInfo target, NormalizedIndexInfoItem item) {
        if (
            !Objects.equals(target.getEmployedItemsCount(), item.employedItemsCount())
                || !Objects.equals(target.getBasePointInTime(), item.basePointInTime())
                || !Objects.equals(target.getBaseIndex(), item.baseIndex())
        ) {
            target.update(
                item.employedItemsCount(),
                item.basePointInTime(),
                item.baseIndex(),
                null
            );
            return true;
        }
        return false;
    }

    private static String key(String cls, String name) {
        return (cls + "||" + name);
    }

    private static String norm(String s) {
        if (s == null) return "";
        return s.trim().replaceAll("\\s+", " ");
    }
}
