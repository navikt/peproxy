package no.nav.peproxy.support;

public class CacheValueWrapper {

    private Value value;
    private Long maxAgeSeconds;
    private Long createTime;

    CacheValueWrapper(Value value, Long maxAgeSeconds) {
        this.value = value;
        this.maxAgeSeconds = maxAgeSeconds;
        this.createTime = System.currentTimeMillis();
    }

    public Value getValue() {
        return value;
    }

    public Long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public Long getAgeSeconds() {
        return (System.currentTimeMillis() - createTime) / 1000;
    }
}
