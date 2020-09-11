package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.util.StringUtil;

public class WordArgument extends TextArgument {
	public WordArgument(int argumentIndex, String baseArgument) {
		super(argumentIndex, CommandArgumentType.WORD, baseArgument);
		
		checkBaseArgumentWhitespace(baseArgument);
	}
	
	WordArgument(int argumentIndex, CommandArgumentType argumentType, String baseArgument) {
		super(argumentIndex, argumentType, baseArgument);
		
		checkBaseArgumentWhitespace(baseArgument);
	}
	
	public boolean isVariable() {
		return true;
	}
	
	private static void checkBaseArgumentWhitespace(String baseArgument) {
		if(StringUtil.containsWhitespace(baseArgument)) {
			String exceptionMessage = "The base argument for a WordArgument cannot contain whitespace.";
			throw new IllegalArgumentException(exceptionMessage);
		}
	}
}
