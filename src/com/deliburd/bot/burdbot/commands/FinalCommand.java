package com.deliburd.bot.burdbot.commands;

import com.deliburd.bot.burdbot.Constant;

import net.dv8tion.jda.api.entities.MessageChannel;

public class FinalCommand extends Command {
	private FinalCommandAction commandAction;
	private String[] aliases;
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
	
	/**
	 * Sets the aliases of the command
	 * 
	 * @param aliases The aliases for the command
	 * @return The changed FinalCommand
	 */
	public FinalCommand setAliases(String[] aliases) {
		this.aliases = aliases;
		return this;
	}

	@Override
	public void finalizeDescription() {
		StringBuilder description = new StringBuilder("Command: ");
		description.append(Constant.COMMAND_PREFIX_WITH_SPACE);
		description.append(getCommandName());
		description.append("\nAliases: ");

		for(int i = 0; i < aliases.length; i++) {
			description.append(Constant.COMMAND_PREFIX_WITH_SPACE);
			description.append(aliases[i]);
			
			if(i != aliases.length - 1) {
				description.append(",");
			}
		}
		
		description.append("\n");
		description.append("Description: ");
		description.append(commandDescription);
		
		commandDescription = description.toString();
	}

	@Override
	void onCommandCalled(String[] args, MessageChannel channel) {
		commandAction.OnCommandRun(channel);
	}
}
