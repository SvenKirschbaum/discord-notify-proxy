package de.elite12.discord_notify_proxy;

import de.elite12.discord_notify_proxy.config.MyRuntimeHints;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportRuntimeHints;

@SpringBootApplication
@ImportRuntimeHints({MyRuntimeHints.class})
public class DiscordNotifyProxyApplication {

	public static void main(String[] args) {
		SpringApplication.run(DiscordNotifyProxyApplication.class, args);
	}

}
