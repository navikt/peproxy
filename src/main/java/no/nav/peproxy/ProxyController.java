package no.nav.peproxy;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import io.micrometer.core.annotation.Timed;
import no.nav.peproxy.config.NavProperties;
import no.nav.peproxy.support.JsonUtils;
import no.nav.peproxy.support.ProxyCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader(value = "max-age", defaultValue = DEFAULT_EXPIRE_SECONDS) Long maxAge,
            @RequestBody(required = false) byte[] body,
            HttpMethod httpMethod
    ) {
        if (isBlank(target)) {
            return ResponseEntity.status(400).body(error(new IllegalArgumentException("Mangler target")));
        }
        try {
            int status = 200;
            byte[] result = proxyCache.get(target, maxAge);
            if (result == null) {
                HttpRequest request = HttpRequest.newBuilder()
                        .method(httpMethod.name(), body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody())
                        .uri(new URI(target))
                        .build();
                HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
                result = response.body();
                status = response.statusCode();
                proxyCache.put(target, maxAge, result);
            }
            return ResponseEntity.status(status).body(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }

}
