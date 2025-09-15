package com.findex.service;

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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final SyncJobMapper syncJobMapper;

    public CursorPageResponse findAll(SyncJobQuery query) {
        return syncJobRepository.findAll(query);
    }

    @Transactional
    public List<SyncJobDto> syncIndexInfo(String worker) {
        List<OpenApiIndexInfoItem> items = client.fetchIndexInfo();
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

        Set<Long> ids = new HashSet<>(request.indexInfoIds());
        List<SyncJobDto> out = new ArrayList<>();
        for (Long indexInfoId : ids) {
            out.addAll(syncOneIndexWithClassificationFilter(indexInfoId, from, to, worker));
        }
        return out;
    }

    @Transactional
    protected List<SyncJobDto> syncOneIndexWithClassificationFilter(Long indexInfoId, LocalDate from, LocalDate to, String worker) {
        IndexInfo info = indexInfoRepository.getOrThrow(indexInfoId);
        String cls  = info.getIndexClassification();

        // 1) idxNm 로만 조회(최대 500건, 1회 호출)
        List<OpenApiIndexDataItem> raw = client.fetchIndexData(info.getIndexName(), from, to);

        // 2) 분류 불일치 제외 (원본값 그대로 비교)
        List<OpenApiIndexDataItem> items = raw.stream()
            .filter(r -> Objects.equals(cls, r.indexClassification()))
            .toList();

        // 3) basDt 중복은 last-wins로 맵핑
        Map<LocalDate, OpenApiIndexDataItem> byDate = new LinkedHashMap<>();
        for (OpenApiIndexDataItem item : items) {
            if (item.baseDate() != null) {
                byDate.put(item.baseDate(), item);
            }
        }

        // 4) 날짜 구간 순회하며 업서트 + SyncJob(targetDate=해당 일자) 기록
        List<SyncJobDto> jobs = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            try {
                OpenApiIndexDataItem item = byDate.get(d);
                if (item != null) {
                    upsertOneDay(indexInfoId, item);
                    SyncJob job = new SyncJob(indexInfoId, JobType.INDEX_DATA, d, worker, SyncJobStatus.SUCCESS);
                    jobs.add(syncJobMapper.toDto(syncJobRepository.save(job)));
                }
            } catch (Exception e) {
                SyncJob job = new SyncJob(indexInfoId, JobType.INDEX_DATA, d, worker, SyncJobStatus.FAILED);
                jobs.add(syncJobMapper.toDto(syncJobRepository.save(job)));
            }
        }
        return jobs;
    }

    private void upsertOneDay(Long indexInfoId, OpenApiIndexDataItem r) {
        IndexData e = indexDataRepository.findByIndexInfoIdAndBaseDate(indexInfoId, r.baseDate())
            .orElseGet(() -> new IndexData(indexInfoId, r.baseDate(), IndexSourceType.OPEN_API));
        e.updatePrices(r.marketPrice(), r.closingPrice(), r.highPrice(), r.lowPrice());
        e.updateFluctuation(r.versus(), r.fluctuationRate());
        e.updateTrading(r.tradingQuantity(), r.tradingPrice(), r.marketTotalAmount());
        indexDataRepository.save(e);
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
}
