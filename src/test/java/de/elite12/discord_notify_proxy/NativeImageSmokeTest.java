package de.elite12.discord_notify_proxy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "APP_DISCORD_TOKEN", matches = ".+")
class NativeImageSmokeTest {

    private static final Duration JDA_STARTUP_TIMEOUT = Duration.ofSeconds(90);
    private static final Duration JDA_POLL_INTERVAL = Duration.ofMillis(250);

    @Autowired
    private JDA jda;

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @Test
    void startsWithRealJdaAndAcceptsWebhook() throws Exception {
        waitForConnectedJda();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/webhooks/seerr"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("""
                                {
                                  "notificationType": "MEDIA_AVAILABLE",
                                  "event": "Your request is now available",
                                  "subject": "Native smoke test",
                                  "message": "Validate native startup and request handling."
                                }
                                """))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(202);

        JsonNode body = readJson(response.body());
        assertThat(body.path("status").asText()).isEqualTo("IGNORED");
        assertThat(body.path("recipientCount").asInt()).isZero();
        assertThat(body.path("detail").asText()).contains("No Discord recipients");
    }

    private void waitForConnectedJda() throws InterruptedException {
        Instant deadline = Instant.now().plus(JDA_STARTUP_TIMEOUT);

        while (Instant.now().isBefore(deadline)) {
            JDA.Status status = jda.getStatus();
            if (status == JDA.Status.CONNECTED) {
                return;
            }
            if (status == JDA.Status.SHUTDOWN) {
                fail("JDA shut down before connecting");
            }

            Thread.sleep(JDA_POLL_INTERVAL.toMillis());
        }

        fail("JDA did not reach CONNECTED status within %s, last status was %s"
                .formatted(JDA_STARTUP_TIMEOUT, jda.getStatus()));
    }

    private JsonNode readJson(String body) throws IOException {
        return objectMapper.readTree(body);
    }
}
