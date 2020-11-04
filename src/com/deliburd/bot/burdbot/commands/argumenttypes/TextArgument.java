package com.deliburd.bot.burdbot.commands.argumenttypes;

public class TextArgument extends CommandArgument {
	private final String baseArgument;
	
	public TextArgument(int argumentIndex, String baseArgument) {
		super(argumentIndex);
		
		this.baseArgument = baseArgument;
	}

	public String getText() {
		return baseArgument;
	}
}