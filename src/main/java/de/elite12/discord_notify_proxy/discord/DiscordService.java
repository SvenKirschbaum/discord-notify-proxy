package de.elite12.discord_notify_proxy.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiscordService {

    private static final Logger log = LoggerFactory.getLogger(DiscordService.class);

    private final JDA jda;

    public DiscordService(JDA jda) {
        this.jda = jda;
    }

    public void sendDirectMessage(long discordUserId, MessageEmbed embed) {
        this.jda.retrieveUserById(discordUserId).queue(user ->
                user.openPrivateChannel().queue(channel ->
                                channel.sendMessageEmbeds(embed).queue(
                                        message -> log.info("Sent Seerr notification to Discord user {}", discordUserId),
                                        error -> log.warn("Failed to send Seerr notification to Discord user {}", discordUserId, error)
                                ),
                        error -> log.warn("Failed to open DM channel for Discord user {}", discordUserId, error)
                ),
                error -> log.warn("Failed to resolve Discord user {}", discordUserId, error)
        );
    }
}
