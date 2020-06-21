package com.deliburd.bot.burdbot.commands;

import com.deliburd.bot.burdbot.util.Cooldown;

import net.dv8tion.jda.api.entities.MessageChannel;

public abstract class Command {
	protected Cooldown commandCooldown;
	protected String commandDescription;
	private String commandName;

	/**
	 * A bot command
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	Command(String command, String description) {
		commandName = command;
		commandDescription = description;
		commandCooldown = new Cooldown(1);
	}
	
	/**
	 * Sets the cooldown for the command
	 * @param newCooldown The cooldown in seconds
	 * @return The command
	 */
	public Command setCooldown(long newCooldown) {
		commandCooldown.ChangeTotalCooldown(newCooldown);
		return this;
	}
	
	/**
	 * Finalizes the description of a command after aliases, cooldowns, etc. have been set.
	 */
	public abstract void finalizeDescription();
	
	/**
	 * Gets the command's name
	 * 
	 * @return The command's name
	 */
	protected String getCommandName() {
		return commandName;
	}
	
	/** 
	 * Gets the command's description for the help command
	 * 
	 * @return The command's description
	 */
	public String getCommandDescription() {
		return commandDescription;
	}
	
	/**
	 * Called when the command is run
	 * 
	 * @param args Arguments associated with the command, if any. Will be null if there are none.
	 * @param messageChannel The channel that the message was posted in
	 */
	abstract void onCommandCalled(String[] args, MessageChannel messageChannel);
}
