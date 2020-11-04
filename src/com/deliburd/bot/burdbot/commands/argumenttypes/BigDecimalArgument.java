package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.math.BigDecimal;
import java.util.Objects;

import com.deliburd.bot.burdbot.commands.ArgumentData;
import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

@ArgumentType(argumentTypeName = "Decimal")
public class BigDecimalArgument extends CommandArgument<BigDecimal> implements IHasInfinity {
	private final BigDecimal bigDecimalValue;
	private final boolean isPositiveInfinity;
	private final boolean isNegativeInfinity;
	
	/**
	 * 
	 * 
	 * @param commandCall
	 * @param argumentIndex
	 * @param argumentData
	 */
	public BigDecimalArgument(CommandCall commandCall, int argumentIndex, ArgumentData argumentData) {
		super(commandCall, argumentIndex, argumentData);
		
		BigDecimal bigDecimalValue;

		try {
			bigDecimalValue = new BigDecimal(argumentData.getArgumentDataString());
		} catch(NumberFormatException e) {
			bigDecimalValue = null;
		}
		
		this.bigDecimalValue = bigDecimalValue;
		isPositiveInfinity = argumentData.getArgumentAnnotationData(Infin)
	}
	
	protected BigDecimalArgument(CommandCall commandCall, int argumentIndex, ArgumentData argumentData, BigDecimal bigDecimalValue) {
		super(commandCall, argumentIndex, argumentData);
		
		this.bigDecimalValue = Objects.requireNonNull(bigDecimalValue, "bigDecimalValue cannot be null");
	}

	@Override
	public BigDecimal getValue() {
		return bigDecimalValue;
	}

	@Override
	public boolean isPossiblyValid() {
		return bigDecimalValue != null;
	}

	@Override
	public boolean isPositiveInfinity() {
		return false;
	}

	@Override
	public boolean isNegativeInfinity() {
		return false;
	}
}
