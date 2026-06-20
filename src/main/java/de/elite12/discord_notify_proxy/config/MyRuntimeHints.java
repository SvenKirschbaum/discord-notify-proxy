package de.elite12.discord_notify_proxy.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;


public class MyRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection().registerType(net.dv8tion.jda.api.entities.User.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.Role.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.Guild.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.Member.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.ScheduledEvent.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.ThreadMember.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.channel.Channel.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.channel.forums.ForumTag.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.channel.middleman.GuildChannel.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.emoji.RichCustomEmoji.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.entities.sticker.GuildSticker.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.api.managers.AudioManager.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.internal.entities.GuildVoiceStateImpl.class.arrayType());
		hints.reflection().registerType(net.dv8tion.jda.internal.entities.MemberPresenceImpl.class.arrayType());
	}
}
