package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.EnumSet;
import java.util.function.Consumer;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.MultiCommand;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class MessageIDArgument extends LongArgument {
	private volatile Message cachedMessage;
	private Throwable messageError;
	
	public MessageIDArgument(int argumentIndex, long messageID) {
		super(argumentIndex, messageID);
	}

	public long getMessageID() {
		return getLong();
	}
	
	public void getMessageOrNotify(MessageChannel channel, Consumer<? super Message> onSuccess, CommandCall commandCall, boolean useCache) {
		getMessage(channel, onSuccess, giveInvalidMessageIDMessage(commandCall), useCache);
	}
	
	public void getMessage(MessageChannel channel, Consumer<? super Message> onSuccess, Consumer<? super Throwable> onFailure, boolean useCache) {
		if(useCache && cachedMessage != null) {
			onSuccess.accept(cachedMessage);
			return;
		} else if(messageError != null) {
			onFailure.accept(messageError);
		}
		
		channel.retrieveMessageById(getMessageID()).queue(message -> {
			cachedMessage = message;
			onSuccess.accept(message);
		}, e -> {
			messageError = e;
			onFailure.accept(e);
		});
	}
	
	private ErrorHandler giveInvalidMessageIDMessage(CommandCall commandCall) {
		MessageChannel messageChannel = commandCall.getCommandEvent().getChannel();
		MultiCommand command = commandCall.getMultiCommand();
		var missingPermissionSet = EnumSet.of(ErrorResponse.MISSING_ACCESS, ErrorResponse.MISSING_PERMISSIONS);
		
		return new ErrorHandler().handle(missingPermissionSet, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I don't have permission to find messages in this channel.");
		}).handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I couldn't find a message with the given ID in this channel. "
					+ "Make sure that a message with this ID still exists and is in this channel.");
		}).handle(ErrorResponse.UNKNOWN_CHANNEL, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I can't find the channel the message was in anymore. "
					+ "The channel was most likely deleted.");
		});
	}
}
