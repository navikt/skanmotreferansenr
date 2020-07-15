package no.nav.skanmotreferansenr.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class LocalCacheConfig {

    public static final String STS_CACHE = "stsCache";

    @Bean
    @Primary
    @Profile({"nais", "local"})
    CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(Collections.singletonList(
                new CaffeineCache(STS_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(55, TimeUnit.MINUTES)
                        .build())
        ));
        return manager;
    }
}
