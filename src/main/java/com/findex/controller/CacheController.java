package com.findex.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CacheManager cacheManager;

    /**
     * 캐시 무효화 엔드포인트
     * 예: DELETE /api/cache/indexInfoCache/syncIndexInfo
     *
     * @param cacheName 캐시 이름 (예: indexInfoCache)
     * @param key       캐시 키   (예: syncIndexInfo)
     */
    @DeleteMapping("/{cacheName}/{key}")
    public ResponseEntity<String> evictCache(
        @PathVariable String cacheName,
        @PathVariable String key
    ) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.badRequest()
                .body("Cache not found: " + cacheName);
        }
        cache.evict(key);
        return ResponseEntity.ok("Cache entry evicted: " + cacheName + "::" + key);
    }

    /**
     * 캐시 전체 무효화
     * 예: DELETE /api/cache/indexInfoCache
     */
    @DeleteMapping("/{cacheName}")
    public ResponseEntity<String> evictAll(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return ResponseEntity.badRequest()
                .body("Cache not found: " + cacheName);
        }
        cache.clear();
        return ResponseEntity.ok("All entries cleared for cache: " + cacheName);
    }
}
