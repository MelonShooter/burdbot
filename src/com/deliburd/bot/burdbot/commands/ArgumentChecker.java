package com.deliburd.bot.burdbot.commands;

import java.util.function.Predicate;

import com.deliburd.bot.burdbot.commands.argumenttypes.CommandArgument;
import net.dv8tion.jda.api.entities.MessageChannel;

public class ArgumentChecker {
	private ArgumentChecker() {}

	public static <T extends CommandArgument<?>> void require(CommandCall commandCall, T argument, Predicate<? super T> requirement, String requirementMessage) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		
		if(requirement.test(argument)) {
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, requirementMessage);
		}
	}
}
