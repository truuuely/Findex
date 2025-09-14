// src/main/java/com/findex/config/CacheConfig.java
package com.findex.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // expireAfterWrite 미설정 → TTL 없음(무기한)
        CaffeineCacheManager m = new CaffeineCacheManager("indexInfoCache", "indexDataCache");
        m.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)     // 메모리 보호용 상한(필요시 조정)
            .recordStats());
        return m;
    }
}
