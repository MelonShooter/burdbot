package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.NumberUtil;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class DiscordLinkArgument extends WordArgument {
	private static final String messageNotFoundMessage = "I can't find the message from the link. Ensure "
			+ "that the message still exists and that the link is valid.";
	private final ServerIDArgument serverIDArgument;
	private final TextChannelIDArgument textChannelIDArgument;
	private final MessageIDArgument messageIDArgument;
	
	public DiscordLinkArgument(int argumentIndex, String discordLink) {
		super(argumentIndex, discordLink);
		
		Matcher discordLinkMatcher = Message.JUMP_URL_PATTERN.matcher(discordLink);
		
		if(discordLinkMatcher.matches()) {
			Long serverID = NumberUtil.stringToLong(discordLinkMatcher.group("guild"));
			
			if(serverID == null) {
				serverIDArgument = null;
			} else {
				var unverifiedServerIDArgument = new ServerIDArgument(getArgumentIndex(), serverID);
				
				if(unverifiedServerIDArgument.isValidServer()) {
					serverIDArgument = unverifiedServerIDArgument;
				} else {
					serverIDArgument = null;
				}
			}
			
			Long channelID = NumberUtil.stringToLong(discordLinkMatcher.group("channel"));
			
			if(channelID == null) {
				textChannelIDArgument = null;
			} else {
				var unverifiedTextChannelIDArgument = new TextChannelIDArgument(getArgumentIndex(), serverID);
				
				if(unverifiedTextChannelIDArgument.isValidChannel()) {
					textChannelIDArgument = unverifiedTextChannelIDArgument;
				} else {
					textChannelIDArgument = null;
				}
			}
			
			Long messageID = NumberUtil.stringToLong(discordLinkMatcher.group("message"));
			
			if(messageID == null || textChannelIDArgument == null) {
				messageIDArgument = null;
			} else {
				messageIDArgument = new MessageIDArgument(getArgumentIndex(), messageID);
			}
		} else {
			serverIDArgument = null;
			textChannelIDArgument = null;
			messageIDArgument = null;
		}
	}
	
	public boolean hasValidServer() {
		return serverIDArgument != null && serverIDArgument.isValidServer();
	}
	
	public boolean hasValidTextChannel() {
		return textChannelIDArgument != null && textChannelIDArgument.isValidChannel();
	}
	
	public Guild getServer() {
		if(serverIDArgument == null) {
			return null;
		}
		
		return serverIDArgument.getServer();
	}
	
	public TextChannel getTextChannel() {
		if(textChannelIDArgument == null) {
			return null;
		}
		
		return textChannelIDArgument.getChannel();
	}
	
	public void getMessage(Consumer<? super Message> onSuccess, Consumer<? super Throwable> onFailure, boolean useCache) {
		if(messageIDArgument == null) {
			onFailure.accept(new RuntimeException(messageNotFoundMessage));
		} else {
			TextChannel channel = getTextChannel();
			
			if(channel == null) {
				onFailure.accept(new RuntimeException("The channel ID given in the link is not valid."));
			} else {
				messageIDArgument.getMessage(channel, onSuccess, onFailure, useCache);
			}
		}
	}
	
	public Guild getServerOrNotify(CommandCall commandCall) {
		Guild server = getServer();
		MultiCommand command = commandCall.getMultiCommand();
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		
		if(server == null) {
			command.giveInvalidArgumentMessage(channel, "The server ID given in the link is not valid.");
		}
		
		return server;
	}
	
	public TextChannel getTextChannelOrNotify(CommandCall commandCall) {
		TextChannel textChannel = getTextChannel();
		MultiCommand command = commandCall.getMultiCommand();
		MessageChannel channelToSendMessage = commandCall.getCommandEvent().getChannel();
		
		if(textChannel == null) {
			command.giveInvalidArgumentMessage(channelToSendMessage, "The channel ID given in the link is not valid.");
		}

		return textChannel;
	}
	
	public void getMessageOrNotify(CommandCall commandCall, Consumer<? super Message> onSuccess, boolean useCache) {
		getMessage(onSuccess, giveInvalidLinkChannelIDMessage(commandCall), useCache);
	}
	
	private ErrorHandler giveInvalidLinkChannelIDMessage(CommandCall commandCall) {
		MessageChannel messageChannel = commandCall.getCommandEvent().getChannel();
		MultiCommand command = commandCall.getMultiCommand();
		var missingPermissionSet = EnumSet.of(ErrorResponse.MISSING_ACCESS, ErrorResponse.MISSING_PERMISSIONS);
		
		return new ErrorHandler().handle(missingPermissionSet, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I don't have permission to find messages in the given channel.");
		}).handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
			command.giveInvalidArgumentMessage(messageChannel, messageNotFoundMessage);
		}).handle(ErrorResponse.UNKNOWN_CHANNEL, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I can't find the channel the message was in anymore. "
					+ "The channel was most likely deleted.");
		}).handle(RuntimeException.class, e -> {
			String exceptionMessage = e.getMessage();
			
			if(exceptionMessage == null) {
				ErrorLogger.LogException(e, messageChannel);
			}
			
			command.giveInvalidArgumentMessage(messageChannel, exceptionMessage);
		});
	}
}
