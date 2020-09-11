package com.deliburd.bot.burdbot.commands.argumenttypes;

public class TextArgument extends CommandArgument {
	private final String baseArgument;
	
	public TextArgument(int argumentIndex, String baseArgument) {
		this(argumentIndex, CommandArgumentType.MULTIWORD, baseArgument);
	}

	TextArgument(int argumentIndex, CommandArgumentType argumentType, String baseArgument) {
		super(argumentIndex, argumentType);
		this.baseArgument = baseArgument;
	}

	public String getText() {
		return baseArgument;
	}
}