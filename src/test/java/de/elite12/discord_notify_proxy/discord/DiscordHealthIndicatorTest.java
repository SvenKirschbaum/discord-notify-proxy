package de.elite12.discord_notify_proxy.discord;

import net.dv8tion.jda.api.JDA;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DiscordHealthIndicatorTest {

    @Test
    void reportsUpWhenJdaIsConnected() {
        JDA jda = mock(JDA.class);
        when(jda.getStatus()).thenReturn(JDA.Status.CONNECTED);
        when(jda.getGatewayPing()).thenReturn(42L);

        DiscordHealthIndicator indicator = new DiscordHealthIndicator(jda);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails())
                .containsEntry("status", JDA.Status.CONNECTED.name())
                .containsEntry("connected", true)
                .containsEntry("gatewayPing", 42L);
    }

    @Test
    void reportsDownWhenJdaIsDisconnected() {
        JDA jda = mock(JDA.class);
        when(jda.getStatus()).thenReturn(JDA.Status.DISCONNECTED);

        DiscordHealthIndicator indicator = new DiscordHealthIndicator(jda);

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
        assertThat(indicator.health().getDetails())
                .containsEntry("status", JDA.Status.DISCONNECTED.name())
                .containsEntry("connected", false)
                .doesNotContainKey("gatewayPing");
    }
}
