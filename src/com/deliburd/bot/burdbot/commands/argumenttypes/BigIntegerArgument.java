package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;
import java.math.BigInteger;

public class BigIntegerArgument extends CommandArgument {
	private final BigInteger bigIntegerValue;
	
	public BigIntegerArgument(int argumentIndex, BigInteger bigIntegerValue) {
		super(argumentIndex, CommandArgumentType.BIGINTEGER);
		
		this.bigIntegerValue = bigIntegerValue;
	}

	public BigInteger getBigInteger() {
		return bigIntegerValue;
	}

	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getArgumentIndex(), new BigDecimal(bigIntegerValue));
	}
}
