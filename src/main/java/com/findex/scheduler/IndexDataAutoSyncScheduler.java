package com.findex.scheduler;

import com.findex.service.AutoSyncConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IndexDataAutoSyncScheduler {
    private AutoSyncConfigService autoSyncConfigService;

    @Scheduled(cron = "0 0 10 * * *")
    public void autoSyncJob() {
        autoSyncConfigService.syncAuto();
    }
}
