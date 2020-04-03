package no.nav.peproxy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static no.nav.peproxy.config.Constants.HTTPHEADERS_MAX_AGE;
import static no.nav.peproxy.config.Constants.HTTPHEADERS_TARGET_URL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@ActiveProfiles("itest")
@RunWith(SpringRunner.class)
@AutoConfigureWireMock(port = 0)
@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProxyControllerTest {

    @Value("http://localhost:${wiremock.server.port}")
    private String wiremockBasePath;
    @Value("http://localhost:${local.server.port}")
    private String serverPath;

    @Before
    public void setUp() throws Exception {
        setupStubs();
    }

    @Test
    public void sikret() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder().GET().uri(new URI(serverPath)).build(),
                        BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
    }

    @Test
    public void sikretKorrektToken() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                                .GET()
                                .header(AUTHORIZATION, "Bearer " + TestTokenUtil.INVALID_TOKEN)
                                .uri(new URI(serverPath)).build(),
                        BodyHandlers.ofString());
        assertThat(response.statusCode(), is(401));
    }

    @Test
    public void obligatoriskTarget() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                                .GET()
                                .header(AUTHORIZATION, "Bearer " + TestTokenUtil.VALID_TOKEN)
                                .uri(new URI(serverPath)).build(),
                        BodyHandlers.ofString());

        assertThat(response.statusCode(), is(400));
        assertThat(response.body(), containsString("Mangler target"));
    }

    @Test
    public void getResult() throws Exception {
        HttpResponse<String> response = getForTarget();

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValueAsLong("age").getAsLong(), is(0L));
        assertThat(response.body(), containsString("success"));
    }

    @Test
    public void getResultWithCache() throws Exception {
        getForTarget();
        HttpResponse<String> response = getForTarget();

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValueAsLong("age").getAsLong(), greaterThanOrEqualTo(0L));
        assertThat(response.body(), containsString("success"));
        verify(1, getRequestedFor(urlEqualTo("/target")));
    }

    private HttpResponse<String> getForTarget() throws IOException, InterruptedException, URISyntaxException {
        return HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder()
                                .GET()
                                .header(AUTHORIZATION, "Bearer " + TestTokenUtil.VALID_TOKEN)
                                .header(HTTPHEADERS_TARGET_URL, wiremockBasePath + "/target")
                                .header(HTTPHEADERS_MAX_AGE, "30")
                                .uri(new URI(serverPath)).build(),
                        BodyHandlers.ofString());
    }

    private void setupStubs() throws IOException {
        stubFor(get("/target")
                .willReturn(okJson("{\"status\":\"success\"}")));
        stubFor(get("/sts/jwks")
                .willReturn(okJson(readFile("jwks.json"))));
    }

    private String readFile(String path) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
    }
}