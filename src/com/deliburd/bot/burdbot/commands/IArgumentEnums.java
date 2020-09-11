package com.deliburd.bot.burdbot.commands;

import javax.annotation.Nonnull;

/**
 * An interface that allows for enums to be used for the FixedWordArgument class.
 * This interface should only be used on enums.
 * 
 * @author DELIBURD
 */
public interface IArgumentEnums {
	/**
	 * The array constant that is used by default by getArgumentAliases()
	 */
	public static final String[] NOALIASES = new String[0];
	
	/**
	 * Gets the base argument of the enum. This method must always give the same output
	 * after initialization and can never be null. Otherwise, undefined behavior may occur.
	 * 
	 * @return The base argument for the enum.
	 */
	@Nonnull
	public String getBaseArgument();

	/**
	 * Gets the argument aliases of the enum. This method must always give the same output
	 * after initialization. In addition, the value returned cannot be null and the Strings within
	 * the array cannot be null either. Otherwise, undefined behavior may occur.
	 * 
	 * @return The argument aliases of the enum. A zero-length array means that there are no aliases.
	 */
	@Nonnull
	public default String[] getArgumentAliases() {
		return NOALIASES;
	}
}
