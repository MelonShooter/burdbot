package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;

public class DoubleArgument extends CommandArgument {
	private final double doubleValue;
	
	public DoubleArgument(int argumentIndex, double doubleValue) {
		super(argumentIndex, CommandArgumentType.DOUBLE);

		this.doubleValue = doubleValue;
	}

	public double getDouble() {
		return doubleValue;
	}
	
	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getArgumentIndex(), BigDecimal.valueOf(doubleValue));
	}
}
