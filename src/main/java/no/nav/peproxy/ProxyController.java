package no.nav.peproxy;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.micrometer.core.annotation.Timed;
import no.nav.peproxy.config.NavProperties;
import no.nav.peproxy.support.CacheValueWrapper;
import no.nav.peproxy.support.JsonUtils;
import no.nav.peproxy.support.ProxyCache;
import no.nav.peproxy.support.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ProxyController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final int TIMEOUT_SECONDS = 10;
    private static final String DEFAULT_EXPIRE_SECONDS = "60";

    private final HttpClient client;
    private ProxyCache proxyCache;

    public ProxyController(NavProperties navProperties, ProxyCache proxyCache) {
        this.proxyCache = proxyCache;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                .proxy(ProxySelector.of(new InetSocketAddress(navProperties.getProxyHost(), navProperties.getProxyPort())))
                .build();
    }

    @RequestMapping
    @Timed(value = "proxy_timer", percentiles = {.5, .9, .99})
    public ResponseEntity post(
            @RequestHeader(value = "target", required = false) String target,
            @RequestHeader(value = "max-age", defaultValue = DEFAULT_EXPIRE_SECONDS) Long maxAgeSeconds,
            @RequestBody(required = false) byte[] body,
            HttpMethod httpMethod,
            JwtAuthenticationToken jwtAuthenticationToken
    ) {
        if (isBlank(target)) {
            return ResponseEntity.status(400).body(error(new IllegalArgumentException("Mangler target")));
        }
        try {
            String clientId = jwtAuthenticationToken.getToken().getSubject();
            String cacheKey = target + clientId;
            CacheValueWrapper valueWrapper = proxyCache.get(cacheKey, maxAgeSeconds);
            boolean fromCache = valueWrapper != null;
            Value value;
            if (fromCache) {
                value = valueWrapper.getValue();
            } else {
                value = fetchFromTarget(target, body, httpMethod);
                proxyCache.put(cacheKey, maxAgeSeconds, value);
            }
            logger.info("{} {} {} - maxAgeSeconds: {} fromCache: {}", clientId, httpMethod, target, maxAgeSeconds, fromCache);
            String age = fromCache ? "" + valueWrapper.getAgeSeconds() : "0";
            return ResponseEntity
                    .status(value.getStatus())
                    .header(HttpHeaders.AGE, age)
                    .header(HttpHeaders.CONTENT_TYPE, value.getContentType())
                    .body(value.getData());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
    }

    private Value fetchFromTarget(String target, byte[] body, HttpMethod httpMethod)
            throws URISyntaxException, java.io.IOException, InterruptedException {
        Value value;
        HttpRequest request = HttpRequest.newBuilder()
                .method(httpMethod.name(), body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody())
                .uri(new URI(target))
                .build();
        HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
        byte[] result = response.body();
        int status = response.statusCode();
        String contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        value = new Value(result, contentType, status);
        return value;
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }

}
