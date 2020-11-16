package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.function.Consumer;

import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public interface IRestActionVerifiable<T> {
	/**
	 * Fetches a REST action value such as a user from a user ID or a message from a message ID.
	 * 
	 * @param onSuccess The Consumer to run if the value was successfully retrieved
	 * @param onFailure The Consumer to run if an error is encountered.
	 * @param useCache Whether to use the cache. This can be provided by the internal cache of the wrapper used for the REST API or the object's
	 * own cache from a previous usage of this method. The value returned won't necessarily be up to date if this is set to true depending on 
	 * what is being retrieved and possibly the intents enabled.
	 */
	public void useRestActionValue(Consumer<? extends T> onSuccess, Consumer<? extends ErrorResponseException> onFailure, boolean useCache);
}
