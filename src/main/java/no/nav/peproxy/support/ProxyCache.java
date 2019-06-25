package no.nav.peproxy.support;

import static org.ehcache.config.builders.CacheConfigurationBuilder.newCacheConfigurationBuilder;
import static org.ehcache.config.builders.CacheManagerBuilder.newCacheManagerBuilder;
import static org.ehcache.config.builders.ResourcePoolsBuilder.newResourcePoolsBuilder;

import java.time.Duration;
import java.util.function.Supplier;

import io.prometheus.client.Counter;
import javax.annotation.PreDestroy;
import no.nav.peproxy.support.dto.CacheValueWrapper;
import no.nav.peproxy.support.dto.HttpResponse;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.ExpiryPolicy;
import org.springframework.stereotype.Component;

@Component
public class ProxyCache {

    private final CacheManager cacheManager;
    private final Cache<String, CacheValueWrapper> cache;

    private static final Counter cacheCounter = Counter.build()
            .name("peproxy_cache_counter")
            .help("Cache status")
            .labelNames("hit")
            .register();

    public ProxyCache() {
        cacheCounter.labels("hit");
        cacheCounter.labels("miss");
        cacheCounter.labels("expired");
        cacheManager = newCacheManagerBuilder()
                .withCache("proxyCache",
                        newCacheConfigurationBuilder(String.class, CacheValueWrapper.class,
                                newResourcePoolsBuilder().heap(50, MemoryUnit.MB))
                                .withExpiry(new CustomExpiry())
                ).build(true);
        cache = cacheManager.getCache("proxyCache", String.class, CacheValueWrapper.class);
    }

    public void put(String key, Long maxAge, HttpResponse value) {
        cache.put(key, new CacheValueWrapper(value, maxAge));
    }

    public CacheValueWrapper get(String key, Long maxAgeSeconds) {
        CacheValueWrapper valueWrapper = cache.get(key);
        if (valueWrapper == null) {
            cacheCounter.labels("miss").inc();
            return null;
        } else if (valueWrapper.getAgeSeconds() > maxAgeSeconds) {
            cacheCounter.labels("expired").inc();
            return null;
        }
        cacheCounter.labels("hit").inc();
        return valueWrapper;
    }

    @PreDestroy
    public void close() {
        cacheManager.close();
    }

    private static class CustomExpiry implements ExpiryPolicy<String, CacheValueWrapper> {

        @Override
        public Duration getExpiryForCreation(String key, CacheValueWrapper value) {
            return Duration.ofSeconds(value.getMaxAgeSeconds());
        }

        @Override
        public Duration getExpiryForAccess(String key, Supplier<? extends CacheValueWrapper> value) {
            return null;
        }

        @Override
        public Duration getExpiryForUpdate(String key, Supplier<? extends CacheValueWrapper> oldValue, CacheValueWrapper newValue) {
            return null;
        }
    }
}
