package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

@ArgumentType(argumentTypeName = "Decimal")
public class DoubleArgument extends CommandArgument<Double> {
	private final Double doubleValue;
	
	public DoubleArgument(CommandCall commandCall, int argumentIndex, String doubleString) {
		super(commandCall, argumentIndex);
		
		Double doubleValue;
	
		try {
			doubleValue = Double.valueOf(doubleString);
		}
		catch(NumberFormatException e) {
			doubleValue = null;
		}
		
		this.doubleValue = doubleValue;
	}
	
	protected DoubleArgument(CommandCall commandCall, int argumentIndex, double doubleValue) {
		super(commandCall, argumentIndex);

		this.doubleValue = doubleValue;
	}
	
	public BigDecimalArgument toBigDecimalArgument() {
		return new BigDecimalArgument(getCommandCall(), getArgumentIndex(), BigDecimal.valueOf(doubleValue));
	}

	@Override
	public boolean isPossiblyValid() {
		return doubleValue != null;
	}

	@Override
	public Double getValue() {
		return doubleValue;
	}
}
