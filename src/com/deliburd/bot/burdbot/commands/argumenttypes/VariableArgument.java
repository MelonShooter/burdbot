package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VariableArgument<T extends CommandArgument> extends CommandArgument {
	private final List<T> argumentList;
	
	VariableArgument(int argumentIndex, T[] arguments) {
		super(argumentIndex);
		
		if(arguments.length == 0) {
			throw new IllegalArgumentException("The argument array provided into a VariableArgument must have at least 1 element.");
		}
		
		argumentList = Arrays.asList(arguments);
	}
	
	public List<T> getArguments() {
		return Collections.unmodifiableList(argumentList);
	}
}
