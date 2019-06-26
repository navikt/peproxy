package no.nav.peproxy.support;

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

import no.nav.peproxy.config.NavProperties;
import no.nav.peproxy.support.dto.HttpResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class Client {

    private static final int TIMEOUT_SECONDS = 10;
    private final HttpClient httpClient;

    public Client(NavProperties navProperties) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.of(TIMEOUT_SECONDS, ChronoUnit.SECONDS))
                .proxy(ProxySelector.of(new InetSocketAddress(navProperties.getProxyHost(), navProperties.getProxyPort())))
                .build();
    }

    public HttpResponse invoke(String method, String target, byte[] body) throws URISyntaxException, java.io.IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .method(method, body != null ? BodyPublishers.ofByteArray(body) : BodyPublishers.noBody())
                .uri(new URI(target))
                .build();
        var response = httpClient.send(request, BodyHandlers.ofByteArray());
        var contentType = response.headers().firstValue(HttpHeaders.CONTENT_TYPE).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        return new HttpResponse(response.body(), contentType, response.statusCode());
    }
}
