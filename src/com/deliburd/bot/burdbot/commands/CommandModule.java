package com.deliburd.bot.burdbot.commands;

import java.util.Collections;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class CommandModule implements Comparable<CommandModule> {
	private final String moduleName;
	private final String moduleDescription;
	private final ConcurrentSkipListSet<Command> commandSet;
	
	public CommandModule(String moduleName, String moduleDescription) {
		this.moduleName = moduleName;
		this.moduleDescription = moduleDescription;
		commandSet = new ConcurrentSkipListSet<Command>((command1, command2) -> {
			return command1.getCommandName().compareTo(command2.getCommandName());	
		});
	}
	
	public final String getModuleName() {
		return moduleName;
	}
	
	public final String getModuleDescription() {
		return moduleDescription;
	}
	
	public final NavigableSet<Command> getCommands() {
		return Collections.unmodifiableNavigableSet(commandSet);
	}
	
	public boolean hasPermission(CommandCall commandCall) {
		return true;
	}
	
	/**
	 * Gets the message to send to the user when an invalid command argument is given.
	 * 
	 * @param argumentTypeName The argument's type name.
	 * @return The invalid argument message to send to the user.
	 */
	public String getInvalidArgumentMessage(String argumentTypeName) {
		return "Invalid " + argumentTypeName + " given.";
	}
	
	final void addCommand(Command newCommand) {
		commandSet.add(newCommand);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(moduleName);
	}
	
	@Override
	public boolean equals(Object otherObject) {
		if (this == otherObject) {
			return true;
		} else if (!(otherObject instanceof CommandModule)) {
			return false;
		}
		
		CommandModule commandModule = (CommandModule) otherObject;
		
		return Objects.equals(moduleName, commandModule.moduleName);
	}
	
	@Override
	public int compareTo(CommandModule otherCommandModule) {
		return getModuleName().compareTo(otherCommandModule.getModuleName());
	}
}
