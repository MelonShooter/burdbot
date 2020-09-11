package com.deliburd.bot.burdbot.commands.argumenttypes;

public abstract class CommandArgument {
	private final int argumentIndex;
	private final CommandArgumentType argumentType;
	
	CommandArgument(int argumentIndex, CommandArgumentType argumentType) {
		this.argumentIndex = argumentIndex;
		this.argumentType = argumentType;
	}

	public int getArgumentIndex() {
		return argumentIndex;
	}
	
	public CommandArgumentType getArgumentType() {
		return argumentType;
	}
}
