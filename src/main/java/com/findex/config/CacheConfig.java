package com.findex.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
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
        // 일자 기준으로 키가 매일 바뀌므로 TTL은 느슨하게(청소용) 2~3일 정도만 줍니다.
        CaffeineCacheManager m = new CaffeineCacheManager("indexInfoDaily", "indexDataDaily");
        m.setCaffeine(Caffeine.newBuilder()
            .maximumSize(200)
            .expireAfterWrite(Duration.ofDays(3)));
        return m;
    }
}
