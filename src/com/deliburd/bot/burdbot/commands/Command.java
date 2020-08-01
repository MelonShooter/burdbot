package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.util.Cooldown;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public abstract class Command {
	protected Cooldown commandCooldown;
	protected String commandDescription;
	private String shortCommandDescription;
	protected ArrayList<String> commandAliases;
	protected boolean isFinalized = false;

	/**
	 * A bot command
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	Command(String command, String description) {
		commandAliases = new ArrayList<String>();
		commandAliases.add(command);
		commandDescription = description;
		shortCommandDescription = description;
		commandCooldown = new Cooldown(Constant.DEFAULT_COOLDOWN);
	}
	
	/**
	 * Sets the cooldown for the command
	 * @param newCooldown The cooldown in seconds
	 * @return The command
	 */
	public Command setCooldown(long newCooldown) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		commandCooldown.changeTotalCooldown(newCooldown);
		return this;
	}
	
	/**
	 * Sets the cooldown for the command
	 * @param newCooldown The cooldown object
	 * @return The command
	 */
	public Command setCooldown(Cooldown newCooldown) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		commandCooldown = newCooldown;
		return this;
	}
	
	/**
	 * Finalizes the command meaning it can't be modified anymore.
	 */
	public void finalizeCommand() {
		finalizeCommand(false);
	}
	
	/**
	 * Finalizes the command meaning it can't be modified anymore.
	 * 
	 * @param isOverriden If overriden and more stuff is run after this
	 */
	protected void finalizeCommand(boolean isOverriden) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		StringBuilder description = new StringBuilder("```Command: ");
		final var commandNames = getCommandNames();
		final long cooldownTime = commandCooldown.getTotalCooldown();
		
		description.append(Constant.COMMAND_PREFIX);
		description.append(getCommandNames().get(0));
		
		if(commandNames.size() > 1) {
			description.append("\nAliases: ");
	
			for(int i = 1; i < commandNames.size(); i++) {
				description.append(Constant.COMMAND_PREFIX);
				description.append(commandNames.get(i));
				
				if(i != commandNames.size() - 1) {
					description.append("; ");
				}
			}
		}
		
		description.append("\nDescription: ");
		description.append(getShortCommandDescription());
		description.append("\nCooldown: ");
		description.append(cooldownTime);
		description.append(" second");
		description.append(cooldownTime == 1 ? "```" : "s```" );
		
		commandDescription = description.toString();
		isFinalized = !isOverriden;
	}
	
	/**
	 * Returns whether or not the command has been finalized
	 * @return Whether the command is finalized
	 */
	public boolean isFinalized() {
		return isFinalized;
	}
	
	/**
	 * Gets the command's name and aliases
	 * 
	 * @return The command's name and aliases
	 */
	protected ArrayList<String> getCommandNames() {
		return commandAliases;
	}
	
	/**
	 * Adds additional aliases to the base command
	 * 
	 * @param aliases The additional aliases
	 */
	public Command addCommandNames(String... aliases) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		for (int i = 0; i < aliases.length; i++) {
			registerAlias(aliases[i]);
		}
		
		return this;
	}
	
	private void registerAlias(String alias) {
		commandAliases.add(alias);
		CommandManager.createAlias(this, alias);
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
	 * Gets the command's short description for the help command
	 * 
	 * @return The command's short description
	 */
	public String getShortCommandDescription() {
		return shortCommandDescription;
	}
	
	/**
	 * Gets the command's base name
	 * 
	 * @return The command's base name
	 */
	public String getCommandName() {
		return commandAliases.get(0);
	}
	
	/**
	 * Called when the command is run
	 * 
	 * @param args Arguments associated with the command, if any. Will be null if there are none.
	 * @param messageChannel The channel that the message was posted in
	 */
	abstract void onCommandCalled(String[] args, MessageChannel messageChannel, User user);
}
