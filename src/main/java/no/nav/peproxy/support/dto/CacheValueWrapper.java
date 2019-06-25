package no.nav.peproxy.support.dto;

public class CacheValueWrapper {

    private HttpResponse httpResponse;
    private Long maxAgeSeconds;
    private Long createTime;

    public CacheValueWrapper(HttpResponse httpResponse, Long maxAgeSeconds) {
        this.httpResponse = httpResponse;
        this.maxAgeSeconds = maxAgeSeconds;
        this.createTime = System.currentTimeMillis();
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Long getMaxAgeSeconds() {
        return maxAgeSeconds;
    }

    public Long getAgeSeconds() {
        return (System.currentTimeMillis() - createTime) / 1000;
    }
}
