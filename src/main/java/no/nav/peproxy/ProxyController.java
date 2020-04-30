package no.nav.peproxy;

import io.micrometer.core.annotation.Timed;
import no.nav.peproxy.support.Client;
import no.nav.peproxy.support.JsonUtils;
import no.nav.peproxy.support.ProxyCache;
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

import javax.servlet.ServletRequest;
import java.util.Optional;

import static no.nav.peproxy.config.Constants.*;

@RestController
@RequestMapping("/")
public class ProxyController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final Client client;
    private final ProxyCache proxyCache;

    public ProxyController(Client client, ProxyCache proxyCache) {
        this.client = client;
        this.proxyCache = proxyCache;
    }

    @RequestMapping
    @Timed(value = "proxy_timer", percentiles = {.5, .9, .99})
    public ResponseEntity post(
            @RequestHeader HttpHeaders httpHeaders,
            @RequestHeader(value = HTTPHEADERS_TARGET, required = false) String target,
            @RequestHeader(value = HTTPHEADERS_MAX_AGE, defaultValue = DEFAULT_EXPIRE_SECONDS) Long maxAge,
            @RequestBody(required = false) byte[] body,
            HttpMethod httpMethod,
            JwtAuthenticationToken jwtAuthToken,
            ServletRequest servletRequest
    ) {
        if (httpHeaders == null || httpHeaders.isEmpty() || !httpHeaders.containsKey(HTTPHEADERS_TARGET)) {
            logger.error("Missing target in header, returns 400 code.");
            return ResponseEntity.status(400).body(error(new IllegalArgumentException("Mangler " + HTTPHEADERS_TARGET)));
        }

        logger.info("Trying to call {} with httpMethod {} and headers {}.", target, httpMethod, httpHeaders);

        try {
            var clientId = Optional.ofNullable(jwtAuthToken)
                    .map(jwt -> jwt.getToken().getSubject())
                    .orElse(servletRequest.getRemoteAddr());

            var cacheKey = clientId + httpMethod.name() + target;
            var wrapper = proxyCache.get(cacheKey, maxAge);
            var fromCache = wrapper != null;
            HttpResponse httpResponse;

            if (fromCache) {
                httpResponse = wrapper.getHttpResponse();
            } else {
                httpResponse = client.invoke(httpMethod.name(), target, body, httpHeaders);
                if (httpResponse.is2xxSuccessful()) {
                    proxyCache.put(cacheKey, maxAge, httpResponse);
                }
            }
            var age = fromCache ? "" + wrapper.getAgeSeconds() : "0";
            logger.info("{} {} {} - status={} age={} maxAge={} fromCache={} body={}", clientId, httpMethod, target, httpResponse.getStatus(), age, maxAge, fromCache, httpResponse.getBodyAsString());
            return ResponseEntity
                    .status(httpResponse.getStatus())
                    .header(HttpHeaders.AGE, age)
                    .header(HttpHeaders.CONTENT_TYPE, httpResponse.getContentType())
                    .body(httpResponse.getBody());
        } catch (Exception e) {
            logger.info("Unable to handle httprequest got Exception with {} to {}. With body: {}", httpMethod, target, e);
            return ResponseEntity.status(500).body(error(e));
        }
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }
}
