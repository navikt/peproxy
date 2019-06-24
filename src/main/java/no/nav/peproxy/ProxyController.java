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
import no.nav.security.oidc.api.Protected;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
@Protected
public class ProxyController {

    private HttpClient client;
    private Logger logger = LoggerFactory.getLogger(getClass());

    public ProxyController(NavProperties navProperties) {
        this.client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(navProperties.getProxyHost(), navProperties.getProxyPort())))
                .build();
    }

    @GetMapping
    @Timed(value = "proxy_get_timer", percentiles = {.5, .9, .99})
    public ResponseEntity get(@RequestHeader(value = "target", required = false) String target) {
        HttpResponse<byte[]> response;
        try {
            if (isBlank(target)) {
                throw new IllegalArgumentException("Mangler target");
            }
            response = client
                    .send(HttpRequest.newBuilder()
                            .timeout(Duration.of(10, ChronoUnit.SECONDS))
                            .GET()
                            .uri(new URI(target))
                            .build(), BodyHandlers.ofByteArray()
                    );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @Timed(value = "proxy_post_timer", percentiles = {.5, .9, .99})
    public ResponseEntity post(@RequestHeader(value = "target", required = false) String target, @RequestBody(required = false) byte[] body) {
        HttpResponse<byte[]> response;
        try {
            if (isBlank(target)) {
                throw new IllegalArgumentException("Mangler target");
            }
            response = client
                    .send(HttpRequest.newBuilder()
                            .timeout(Duration.of(10, ChronoUnit.SECONDS))
                            .POST(body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody())
                            .uri(new URI(target))
                            .build(), BodyHandlers.ofByteArray()
                    );
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
        return ResponseEntity.ok(response);
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }

}
