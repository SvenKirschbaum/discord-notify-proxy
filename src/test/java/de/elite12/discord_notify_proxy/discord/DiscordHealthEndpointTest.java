package de.elite12.discord_notify_proxy.discord;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "app.discord-token=test-token",
                "management.endpoint.health.show-details=always"
        })
@DisabledInAotMode("Uses Mockito bean overrides that are only needed for JVM tests")
class DiscordHealthEndpointTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @MockitoBean
    private JDA jda;

    @LocalServerPort
    private int port;

    @AfterEach
    void tearDown() {
        reset(jda);
    }

    @Test
    void overallHealthGoesDownWhenDiscordIsDisconnected() throws Exception {
        when(jda.getStatus()).thenReturn(JDA.Status.DISCONNECTED);

        HttpResponse<String> response = get("/actuator/health");
        JsonNode body = readJson(response);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(body.path("status").asText()).isEqualTo("DOWN");
        assertThat(body.path("components").path("discord").path("status").asText()).isEqualTo("DOWN");
        assertThat(body.path("components").path("discord").path("details").path("status").asText())
                .isEqualTo(JDA.Status.DISCONNECTED.name());
    }

    @Test
    void readinessIncludesDiscordHealth() throws Exception {
        when(jda.getStatus()).thenReturn(JDA.Status.DISCONNECTED);

        HttpResponse<String> response = get("/actuator/health/readiness");
        JsonNode body = readJson(response);

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(body.path("status").asText()).isEqualTo("DOWN");
        assertThat(body.path("components").path("discord").path("status").asText()).isEqualTo("DOWN");
    }

    @Test
    void livenessIgnoresDiscordHealth() throws Exception {
        when(jda.getStatus()).thenReturn(JDA.Status.DISCONNECTED);

        HttpResponse<String> response = get("/actuator/health/liveness");
        JsonNode body = readJson(response);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(body.path("status").asText()).isEqualTo("UP");
        assertThat(body.path("components").has("discord")).isFalse();
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return HTTP_CLIENT.send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readJson(HttpResponse<String> response) throws IOException {
        return OBJECT_MAPPER.readTree(response.body());
    }
}
