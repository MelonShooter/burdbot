package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

@ArgumentType(argumentTypeName = "Integer")
public class BigIntegerArgument extends CommandArgument<BigInteger> {
	private final BigInteger bigIntegerValue;
	
	public BigIntegerArgument(CommandCall commandCall, int argumentIndex, String bigIntegerString) {
		super(commandCall, argumentIndex);
		
		BigInteger bigIntegerValue;

		try {
			bigIntegerValue = new BigInteger(bigIntegerString);
		} catch(NumberFormatException e) {
			bigIntegerValue = null;
		}
		
		this.bigIntegerValue = bigIntegerValue;
	}
	
	protected BigIntegerArgument(CommandCall commandCall, int argumentIndex, BigInteger bigIntegerValue) {
		super(commandCall, argumentIndex);
		
		this.bigIntegerValue = bigIntegerValue;
	}

	@Override
	public boolean isPossiblyValid() {
		return bigIntegerValue != null;
	}

	@Override
	public BigInteger getValue() {
		return bigIntegerValue;
	}
	
	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getCommandCall(), getArgumentIndex(), new BigDecimal(bigIntegerValue));
	}
}
