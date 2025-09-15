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
        CaffeineCacheManager m = new CaffeineCacheManager("indexInfoCache", "indexDataCache");
        m.setCaffeine(Caffeine.newBuilder()
            .maximumSize(10_000)
            .recordStats());
        return m;
    }
}
