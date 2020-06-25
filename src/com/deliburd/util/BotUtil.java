package com.deliburd.util;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class BotUtil {
	private BotUtil() {}
	/**
	 * Checks if the bot has write permissions
	 * 
	 * @param guild The guild
	 * @param channel The channel
	 * @return Whether the bot has write permissions.
	 */
	public static boolean hasWritePermission(Guild guild, GuildChannel channel) {
		return guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE);
	}
	
	/**
	 * Checks if the bot has write permissions
	 * 
	 * @param event The mssage received event
	 * @return Whether the bot has write permissions. Returns false if not from a guild or not a text channel
	 */
	public static boolean hasWritePermission(MessageReceivedEvent event) {
		if(!event.isFromGuild() || event.getChannelType() != ChannelType.TEXT) {
			return false;
		}
		
		final Guild guild = event.getGuild();
		final TextChannel channel = event.getTextChannel();

		return guild.getSelfMember().hasPermission(channel, Permission.MESSAGE_WRITE);
	}
	
	/**
	 * Queues a message to be sent
	 * 
	 * @param channel The channel to send the emssage to
	 * @param message The message's content
	 */
	public static void sendMessage(MessageChannel channel, CharSequence message) {
		channel.sendMessage(message).queue();
	}
}
