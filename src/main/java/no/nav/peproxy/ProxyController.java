package no.nav.peproxy;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Optional;

import com.sun.jdi.LongValue;
import io.micrometer.core.annotation.Timed;
import javax.servlet.ServletRequest;
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

@RestController
@RequestMapping("/")
public class ProxyController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final Long DEFAULT_EXPIRE_SECONDS = 60l;

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
            @RequestBody(required = false) byte[] body,
            HttpMethod httpMethod,
            JwtAuthenticationToken jwtAuthenticationToken,
            ServletRequest servletRequest
    ) {


        if (httpHeaders == null || httpHeaders.isEmpty() || httpHeaders.containsKey("target")) {
            return ResponseEntity.status(400).body(error(new IllegalArgumentException("Mangler target")));
        }

        String target = httpHeaders.get("target").get(0);
        httpHeaders.remove("target");

        Long maxAgeSeconds = httpHeaders.containsKey("max-age") ?  Long.parseLong(httpHeaders.get("max-age").get(0)) : DEFAULT_EXPIRE_SECONDS;
        httpHeaders.remove("max-age");


        logger.info("headers: {}", httpHeaders);

        try {
            var clientId = Optional.ofNullable(jwtAuthenticationToken)
                    .map(jwt -> jwt.getToken().getSubject())
                    .orElse(servletRequest.getRemoteAddr());

            var cacheKey = clientId + httpMethod.name() + target;
            var wrapper = proxyCache.get(cacheKey, maxAgeSeconds);
            var fromCache = wrapper != null;
            HttpResponse httpResponse;

            if (fromCache) {
                httpResponse = wrapper.getHttpResponse();
            } else {
                httpResponse = client.invoke(httpMethod.name(), target, body, httpHeaders);
                // Lets not cache errors
                if (httpResponse.is2xxSuccessful()) {
                    proxyCache.put(cacheKey, maxAgeSeconds, httpResponse);
                }
            }
            var age = fromCache ? "" + wrapper.getAgeSeconds() : "0";
            logger.info("{} {} {} - status={} age={} maxAge={} fromCache={}", clientId, httpMethod, target,
                    httpResponse.getStatus(), age, maxAgeSeconds, fromCache);
            return ResponseEntity
                    .status(httpResponse.getStatus())
                    .header(HttpHeaders.AGE, age)
                    .header(HttpHeaders.CONTENT_TYPE, httpResponse.getContentType())
                    .body(httpResponse.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(error(e));
        }
    }

    private String error(Exception e) {
        logger.warn("Feil", e);
        return JsonUtils.toJson(e);
    }

}
