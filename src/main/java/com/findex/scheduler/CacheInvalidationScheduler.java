package com.findex.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheInvalidationScheduler {

    private final CacheManager cacheManager;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void clearIndexInfoCache() {
        Cache cache = cacheManager.getCache("indexInfoCache");
        if (cache != null) {
            cache.clear(); // 이름에 해당하는 캐시 전체 무효화
        }
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void clearIndexDataCache() {
        Cache cache = cacheManager.getCache("indexDataCache");
        if (cache != null) {
            cache.clear(); // 이름에 해당하는 캐시 전체 무효화
        }
    }
}
