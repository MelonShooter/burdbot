package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.bot.burdbot.commands.IArgumentEnums;

public class FixedWordArgument<E extends Enum<E> & IArgumentEnums> extends WordArgument {
	private final E enumValue;

	public FixedWordArgument(int argumentIndex, E enumValue) {
		super(argumentIndex, enumValue.getBaseArgument());
		
		this.enumValue = enumValue;
	}

	public E getEnumValue() {
		return enumValue;
	}
	
	@Override
	public boolean isVariable() {
		return false;
	}
}
