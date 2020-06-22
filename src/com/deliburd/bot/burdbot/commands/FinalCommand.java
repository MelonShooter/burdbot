package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.entities.MessageChannel;

public class FinalCommand extends Command {
	private FinalCommandAction commandAction;

	/**
	 * A bot command with no additional arguments
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @param action The action to run when the command is typed.
	 */
	FinalCommand(String command, String description, FinalCommandAction action) {
		super(command, description);
		commandAction = action;
	}

	@Override
	void onCommandCalled(String[] args, MessageChannel channel) {
		commandAction.OnCommandRun(channel);
	}
}
