package com.deliburd.bot.burdbot.commands.argumenttypes;

public interface IHasInfinity {
	public default boolean isInfinity() {
		return isPositiveInfinity() || isNegativeInfinity();
	}
	
	public boolean isPositiveInfinity();
	
	public boolean isNegativeInfinity();
}
