package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;

public class BigDecimalArgument extends CommandArgument {
	private final BigDecimal bigDecimalValue;
	
	public BigDecimalArgument(int argumentIndex, BigDecimal bigDecimalValue) {
		super(argumentIndex, CommandArgumentType.BIGDECIMAL);

		this.bigDecimalValue = bigDecimalValue;
	}

	public BigDecimal getBigDecimal() {
		return bigDecimalValue;
	}
}
