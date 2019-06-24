package no.nav.peproxy.support;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;

import java.time.Duration;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.springframework.stereotype.Component;

@Component
public class ProxyCache {

    private final CacheManager cacheManager;
    private final Cache<String, ValueWrapper> cache;

    public ProxyCache() {
        cacheManager = newCacheManagerBuilder()
                .withCache("proxyCache",
                        newCacheConfigurationBuilder(String.class, ValueWrapper.class,
                                newResourcePoolsBuilder().heap(50, MemoryUnit.MB))
                                .withExpiry(new CustomExpiry())
                ).build(true);
        cache = cacheManager.getCache("proxyCache", String.class, ValueWrapper.class);
    }

    public void put(String key, Long maxAge, byte[] value) {
        cache.put(key, new ValueWrapper(value, maxAge));
    }

    public ValueWrapper get(String key, Long maxAgeSeconds) {
        ValueWrapper valueWrapper = cache.get(key);
        if (valueWrapper == null || valueWrapper.getAgeSeconds() > maxAgeSeconds) {
            return null;
        }
        return valueWrapper;
    }

    @PreDestroy
    public void close() {
        cacheManager.close();
    }

    public static class ValueWrapper {

        private byte[] value;
        private Long maxAgeSeconds;
        private Long createTime;

        ValueWrapper(byte[] value, Long maxAgeSeconds) {
            this.value = value;
            this.maxAgeSeconds = maxAgeSeconds;
            this.createTime = System.currentTimeMillis();
        }

        public byte[] getValue() {
            return value;
        }

        public Long getMaxAgeSeconds() {
            return maxAgeSeconds;
        }

        public Long getAgeSeconds() {
            return (System.currentTimeMillis() - createTime) / 1000;
        }
    }

    private static class CustomExpiry implements ExpiryPolicy<String, ValueWrapper> {

        @Override
        public Duration getExpiryForCreation(String key, ValueWrapper value) {
            return Duration.ofSeconds(value.getMaxAgeSeconds());
        }

        @Override
        public Duration getExpiryForAccess(String key, Supplier<? extends ValueWrapper> value) {
            return null;
        }

        @Override
        public Duration getExpiryForUpdate(String key, Supplier<? extends ValueWrapper> oldValue, ValueWrapper newValue) {
            return null;
        }
    }
}
