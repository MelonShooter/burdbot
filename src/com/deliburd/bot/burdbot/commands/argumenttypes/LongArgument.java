package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

@ArgumentType(argumentTypeName = "Integer")
public class LongArgument extends CommandArgument<Long> {
	private final Long longValue;
	
	public LongArgument(CommandCall commandCall, int argumentIndex, String longString) {
		super(commandCall, argumentIndex);
		
		Long longValue;
		
		try {
			longValue = Long.valueOf(longString);
		} catch(NumberFormatException e) {
			longValue = null;
		}
		
		this.longValue = longValue;
	}
	
	public DoubleArgument toDoubleArgument() {
		return new DoubleArgument(getCommandCall(), getArgumentIndex(), longValue);
	}
	
	public BigIntegerArgument toBigIntegerArgument() {
		return new BigIntegerArgument(getCommandCall(), getArgumentIndex(), BigInteger.valueOf(longValue));
	}
	
	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getCommandCall(), getArgumentIndex(), BigDecimal.valueOf(longValue));
	}

	@Override
	public boolean isPossiblyValid() {
		return longValue != null;
	}

	@Override
	public Long getValue() {
		return longValue;
	}
}
