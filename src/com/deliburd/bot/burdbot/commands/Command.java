package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;
import java.util.Objects;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.util.Cooldown;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
	protected final ArrayList<String> commandAliases;
	protected Cooldown commandCooldown;
	protected String commandDescription;
	protected boolean isFinalized = false;
	private final String commandName;
	private final String shortCommandDescription;
	private final String commandPrefix;

	/**
	 * A bot command
	 * 
	 * @prefix The command's prefix
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	Command(String prefix, String command, String description) {
		commandName = command;
		commandPrefix = prefix;
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
		
		description.append(Constant.COMMAND_PREFIX)
				.append(getCommandNames().get(0));
		
		if(commandNames.size() > 1) {
			description.append("\nAliases: ");
	
			for(int i = 1; i < commandNames.size(); i++) {
				description.append(Constant.COMMAND_PREFIX)
						.append(commandNames.get(i));
				
				if(i != commandNames.size() - 1) {
					description.append("; ");
				}
			}
		}
		
		description.append("\nDescription: ")
				.append(getShortCommandDescription())
				.append("\nCooldown: ")
				.append(cooldownTime)
				.append(" second")
				.append(cooldownTime == 1 ? "```" : "s```" );
		
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((commandName == null) ? 0 : commandName.hashCode());
		result = prime * result + ((commandPrefix == null) ? 0 : commandPrefix.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		} else if(obj == null || !(obj instanceof Command)) {
			return false;
		}
		
		Command command = (Command) obj;
		
		return Objects.equals(commandName, command.commandName) &&
				Objects.equals(commandPrefix, command.commandPrefix);
	}

	private void registerAlias(String alias) {
		commandAliases.add(alias);
		CommandManager.getManager(commandPrefix).createAlias(this, alias);
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
	 * @param event The message received event
	 */
	abstract void onCommandCalled(String[] args, MessageReceivedEvent event);
}
