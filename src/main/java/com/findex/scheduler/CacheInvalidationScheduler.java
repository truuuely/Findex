package com.findex.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationScheduler {

    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void clearIndexInfoCache() {
        log.info("Clearing indexInfoCache");
        Cache cache = cacheManager.getCache("indexInfoCache");
        if (cache != null) {
            cache.clear();
            log.info("indexInfoCache cleared");
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void clearIndexDataCache() {
        log.info("Clearing indexDataCache");
        Cache cache = cacheManager.getCache("indexDataCache");
        if (cache != null) {
            cache.clear();
            log.info("indexDataCache cleared");
        }
    }
}
