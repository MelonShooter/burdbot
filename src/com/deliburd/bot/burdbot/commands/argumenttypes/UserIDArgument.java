package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.function.Consumer;

import com.deliburd.bot.burdbot.Main;
import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.util.BotUtil;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class UserIDArgument extends LongArgument {
	private User cachedUser;
	
	UserIDArgument(int argumentIndex, long userID) {
		super(argumentIndex, CommandArgumentType.USERID, userID);
	}
	
	public long getUserID() {
		return getLong();
	}
	
	public void getUserOrNotify(Consumer<? super User> onSuccess, CommandCall commandCall, boolean useCache) {
		getUser(onSuccess, giveInvalidUserIDMessage(commandCall), useCache);
	}
	
	public void getUser(Consumer<? super User> onSuccess, Consumer<? super Throwable> onFailure, boolean useCache) {
		if(useCache && cachedUser != null) {
			onSuccess.accept(cachedUser);
			return;
		}
		
		BotUtil.getUser(getUserID(), Main.getJDAInstance(), user -> {
			cachedUser = user;
			onSuccess.accept(user);
		}, onFailure);
	}
	
	private ErrorHandler giveInvalidUserIDMessage(CommandCall commandCall) {
		MessageChannel messageChannel = commandCall.getCommandEvent().getChannel();
		MultiCommand command = commandCall.getMultiCommand();
		
		return new ErrorHandler().handle(ErrorResponse.UNKNOWN_USER, e -> {
			command.giveInvalidArgumentMessage(messageChannel, "I can't find a user with this user ID.");
		});
	}
}
