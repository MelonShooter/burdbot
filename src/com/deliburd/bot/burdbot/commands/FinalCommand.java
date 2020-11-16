package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class FinalCommand extends Command {
	private FinalCommandAction commandAction;

	/**
	 * A bot command with no additional arguments
	 * 
	 * @param module The command's module
	 * @param prefix The command's prefix
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @param action The action to run when the command is typed.
	 */
	FinalCommand(CommandModule module, String prefix, String command, String description, FinalCommandAction action) {
		super(module, prefix, command, description);
		commandAction = action;
	}

	@Override
	void onCommandCalled(String[] args, MessageReceivedEvent event) {
		if(commandCooldown.isCooldownOver(event.getAuthor())) {
			commandAction.OnCommandRun(event);
			commandCooldown.resetCooldown(event.getAuthor());
		}
	}
}
