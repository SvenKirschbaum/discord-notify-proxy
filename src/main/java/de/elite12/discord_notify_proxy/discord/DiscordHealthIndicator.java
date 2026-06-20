package de.elite12.discord_notify_proxy.discord;

import net.dv8tion.jda.api.JDA;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("discord")
public class DiscordHealthIndicator implements HealthIndicator {

    private final JDA jda;

    public DiscordHealthIndicator(JDA jda) {
        this.jda = jda;
    }

    @Override
    public Health health() {
        JDA.Status status = jda.getStatus();
        Health.Builder builder = status == JDA.Status.CONNECTED ? Health.up() : Health.down();

        builder.withDetail("status", status.name())
                .withDetail("connected", status == JDA.Status.CONNECTED);

        if (status == JDA.Status.CONNECTED) {
            builder.withDetail("gatewayPing", jda.getGatewayPing());
        }

        return builder.build();
    }
}
