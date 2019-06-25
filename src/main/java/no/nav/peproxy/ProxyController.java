package no.nav.peproxy;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;

import io.micrometer.core.annotation.Timed;
import javax.servlet.ServletRequest;
import no.nav.peproxy.support.Client;
import no.nav.peproxy.support.JsonUtils;
import no.nav.peproxy.support.ProxyCache;
import no.nav.peproxy.support.dto.CacheValueWrapper;
import no.nav.peproxy.support.dto.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    private static final String DEFAULT_EXPIRE_SECONDS = "60";

    private final Client client;
    private final ProxyCache proxyCache;

    public ProxyController(Client client, ProxyCache proxyCache) {
        this.client = client;
        this.proxyCache = proxyCache;
    }

    @RequestMapping
    @Timed(value = "proxy_timer", percentiles = {.5, .9, .99})
    public ResponseEntity post(
            @RequestHeader(value = "target", required = false) String target,
            @RequestHeader(value = "max-age", defaultValue = DEFAULT_EXPIRE_SECONDS) Long maxAgeSeconds,
            @RequestBody(required = false) byte[] body,
            HttpMethod httpMethod,
            JwtAuthenticationToken jwtAuthenticationToken,
            ServletRequest servletRequest
    ) {
        if (isBlank(target)) {
            return ResponseEntity.status(400).body(error(new IllegalArgumentException("Mangler target")));
        }
        try {
            String clientId = Optional.ofNullable(jwtAuthenticationToken)
                    .map(jwt -> jwt.getToken().getSubject())
                    .orElse(servletRequest.getRemoteAddr());
            String cacheKey = clientId + httpMethod.name() + target;
            CacheValueWrapper wrapper = proxyCache.get(cacheKey, maxAgeSeconds);
            boolean fromCache = wrapper != null;
            HttpResponse httpResponse;
            if (fromCache) {
                httpResponse = wrapper.getHttpResponse();
            } else {
                httpResponse = client.invoke(target, body, httpMethod);
                proxyCache.put(cacheKey, maxAgeSeconds, httpResponse);
            }
            String age = fromCache ? "" + wrapper.getAgeSeconds() : "0";
            logger.info("{} {} {} - age: {} maxAge: {} fromCache: {}", clientId, httpMethod, target, age, maxAgeSeconds, fromCache);
            return ResponseEntity
                    .status(httpResponse.getStatus())
                    .header(HttpHeaders.AGE, age)
                    .header(HttpHeaders.CONTENT_TYPE, httpResponse.getContentType())
                    .body(httpResponse.getData());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }

}
