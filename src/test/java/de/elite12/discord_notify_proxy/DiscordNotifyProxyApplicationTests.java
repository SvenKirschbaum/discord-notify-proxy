package de.elite12.discord_notify_proxy;

import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.elite12.discord_notify_proxy.web.SeerrWebhookController;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.discord-token=test-token")
class DiscordNotifyProxyApplicationTests {

    @Autowired
    private SeerrWebhookController seerrWebhookController;

    @MockitoBean
    private JDA jda;

    @Test
    void contextLoads() {
        assertThat(seerrWebhookController).isNotNull();
    }
}
