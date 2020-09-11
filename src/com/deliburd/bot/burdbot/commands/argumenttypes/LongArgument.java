package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;
import java.math.BigInteger;

public class LongArgument extends CommandArgument {
	private final long longValue;
	
	public LongArgument(int argumentIndex, long longValue) {
		super(argumentIndex, CommandArgumentType.LONG);
		
		this.longValue = longValue;
	}
	
	LongArgument(int argumentIndex, CommandArgumentType argumentType, long longValue) {
		super(argumentIndex, argumentType);
		
		this.longValue = longValue;
	}
	
	public DoubleArgument toDoubleArgument() {
		return new DoubleArgument(getArgumentIndex(), longValue);
	}
	
	public BigIntegerArgument toBigIntegerArgument() {
		return new BigIntegerArgument(getArgumentIndex(), BigInteger.valueOf(longValue));
	}
	
	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getArgumentIndex(), BigDecimal.valueOf(longValue));
	}

	public long getLong() {
		return longValue;
	}
}
