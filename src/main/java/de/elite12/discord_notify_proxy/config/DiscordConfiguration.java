package de.elite12.discord_notify_proxy.config;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DiscordConfiguration {

    @Bean(destroyMethod = "shutdown")
    public JDA jda(AppProperties appProperties) {
        return JDABuilder.createLight(appProperties.getDiscordToken()).build();
    }
}
