package no.nav.peproxy.support;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;

import java.time.Duration;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.springframework.stereotype.Component;


/**
 * Same key with different age will not hit the same cache
 */
@Component
public class ProxyCache {

    private final CacheManager cacheManager;
    private final Cache<String, byte[]> cache;

    public ProxyCache() {
        cacheManager = newCacheManagerBuilder()
                .withCache("proxyCache",
                        newCacheConfigurationBuilder(String.class, byte[].class,
                                newResourcePoolsBuilder().heap(50, MemoryUnit.MB))
                                .withExpiry(new NameBasedExpiry())
                ).build(true);
        cache = cacheManager.getCache("proxyCache", String.class, byte[].class);
    }


    public void put(String key, Long maxAge, byte[] value) {
        cache.put(createCacheKey(key, maxAge), value);
    }

    public byte[] get(String key, Long maxAge) {
        return cache.get(createCacheKey(key, maxAge));
    }

    /**
     * key is 'age in seconds'-'keyname' to be used in {@link NameBasedExpiry}
     */
    private String createCacheKey(String key, Long maxAge) {
        return String.format("%d-%s", maxAge, key);
    }

    @PreDestroy
    public void close() {
        cacheManager.close();
    }

    private static class NameBasedExpiry implements ExpiryPolicy<String, Object> {

        @Override
        public Duration getExpiryForCreation(String key, Object value) {
            String age = StringUtils.substringBefore(key, "-");
            return Duration.ofSeconds(Long.parseLong(age));
        }

        @Override
        public Duration getExpiryForAccess(String key, Supplier<?> value) {
            return null;
        }

        @Override
        public Duration getExpiryForUpdate(String key, Supplier<?> oldValue, Object newValue) {
            return null;
        }
    }
}
