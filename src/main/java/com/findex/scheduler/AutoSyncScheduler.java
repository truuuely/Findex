package com.findex.scheduler;

import com.findex.dto.syncjob.IndexDataOpenApiSyncRequest;
import com.findex.dto.syncjob.SyncJobDto;
import com.findex.enums.JobType;
import com.findex.repository.autosyncconfig.AutoSyncConfigRepository;
import com.findex.repository.syncjob.SyncJobRepository;
import com.findex.service.SyncJobService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoSyncScheduler {

    private static final int DEFAULT_LOOKBACK_DAYS = 14;

    private final AutoSyncConfigRepository autoSyncConfigRepository;
    private final SyncJobRepository syncJobRepository;
    private final SyncJobService syncJobService;

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    public void runDaily() {
        LocalDate to = LocalDate.now();

        List<Long> ids = autoSyncConfigRepository.findEnabledIndexInfoIds();
        if (ids == null || ids.isEmpty()) {
            log.info("[auto-sync] enabled index_info_id not found — skip");
            return;
        }

        log.info("[auto-sync] start: ids={}, to={}", ids.size(), to);

        int ok = 0, fail = 0;
        for (Long id : ids) {
            try {
                LocalDate last = syncJobRepository.findLastAutoTargetDate(id, JobType.INDEX_DATA, "scheduler");
                // 마지막 기록이 없으면 기본 lookback 사용
                LocalDate from = (last != null) ? last : to.minusDays(DEFAULT_LOOKBACK_DAYS);

                // from > to 방어
                if (from.isAfter(to)) {
                    log.warn("[auto-sync] skip id={}, invalid range: from={} > to={}", id, from, to);
                    continue;
                }

                IndexDataOpenApiSyncRequest req = new IndexDataOpenApiSyncRequest(List.of(id), from, to);
                List<SyncJobDto> jobs = syncJobService.syncIndexData(req, "scheduler");

                ok++;
                log.info("[auto-sync] id={} range=[{}..{}] -> jobs={}", id, from, to, jobs.size());
            } catch (Exception e) {
                fail++;
                log.error("[auto-sync] id={} failed: {}", id, e.getMessage(), e);
            }
        }

        log.info("[auto-sync] done: total={}, success={}, failed={}", ids.size(), ok, fail);
    }
}
