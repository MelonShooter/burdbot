package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Optional;

import net.dv8tion.jda.api.entities.ISnowflake;

/**
 * An interface for subclasses of a command argument when they're ID arguments that are fully cached and don't require requests to Discord's
 * REST API such as guild channel ID arguments.
 * 
 * @param <T> The type of the object that can be derived from the ID
 * 
 * @author DELIBURD
 * @see CommandArgument
 */
public interface IFullyCachedIDArgument<T extends ISnowflake> {
	/**
	 * Gets the object associated with the ID.
	 * 
	 * @return The object associated with the ID
	 */
	public Optional<? extends T> getObjectFromID();
}
