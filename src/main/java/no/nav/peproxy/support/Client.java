package no.nav.peproxy.support;

import no.nav.peproxy.config.ForbiddenHttpHeaders;
import no.nav.peproxy.config.NavProperties;
import no.nav.peproxy.support.dto.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static no.nav.peproxy.config.Constants.*;
import static no.nav.peproxy.config.Constants.HTTPHEADERS_TARGET_AUTHORIZATION;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class Client {

    private static final int TIMEOUT_SECONDS = 10;
    private final HttpClient httpClient;
    private Logger logger = LoggerFactory.getLogger(getClass());

    public Client(NavProperties navProperties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                .proxy(ProxySelector.of(new InetSocketAddress(navProperties.getProxyHost(), navProperties.getProxyPort())))
                .build();
    }

    public HttpResponse invoke(String method, String target, byte[] body, HttpHeaders httpHeaders) throws URISyntaxException, java.io.IOException, InterruptedException {
        var httpRequestBuilder = HttpRequest.newBuilder()
                .method(method, body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody())
                .uri(new URI(target));
        buildUpheadersInHttpbuilder(httpHeaders, httpRequestBuilder);
        var request = httpRequestBuilder.build();

        logger.error("Body: {}.", body);
        logger.error("httpRequestBuilder: {}.", httpRequestBuilder);

        var response = httpClient.send(request, BodyHandlers.ofByteArray());
        var contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        return new HttpResponse(response.body(), contentType, response.statusCode());
    }

    public void buildUpheadersInHttpbuilder(HttpHeaders httpHeaders, HttpRequest.Builder request) {
        this.removeHeadersNotForClient(httpHeaders);

        for (Map.Entry<String, List<String>> entry : httpHeaders.entrySet()) {
            if (ForbiddenHttpHeaders.checkIfHttpHeaderIsViable(entry.getKey())) {
                request.header(entry.getKey(), entry.getValue().get(0));
            }
        }
    }

    private void removeHeadersNotForClient(HttpHeaders httpHeaders) {
        httpHeaders.remove(HTTPHEADERS_MAX_AGE);
        httpHeaders.remove(HTTPHEADERS_TARGET);

        if (httpHeaders.containsKey(HTTPHEADERS_TARGET_AUTHORIZATION)) {
            httpHeaders.add(AUTHORIZATION, httpHeaders.get(HTTPHEADERS_TARGET_AUTHORIZATION).get(0));
        }

        httpHeaders.remove(HTTPHEADERS_TARGET_AUTHORIZATION);
    }
}
