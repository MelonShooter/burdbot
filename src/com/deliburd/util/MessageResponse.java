package com.deliburd.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageResponse {
	private final String initialMessage;
	private final JDA JDA;
	private final long channelID;
	private final long responseUserID;
	private final BiFunction<MessageReceivedEvent, MessageResponse, Boolean> responseCallback;
	private final Set<String> validResponses;
	private final boolean isTextChannel;
	private volatile BiFunction<MessageReceivedEvent, MessageResponse, Boolean> cancelCallback;
	private volatile Function<MessageResponse, Boolean> timeoutCallback;
	private volatile Set<String> cancelResponses;
	private volatile MessageChannel channel;
	private volatile MessageResponse nextResponse;
	private volatile long timeout;
	private volatile String cancelMessage;
	private volatile boolean isFinalized;
	
	/**
	 * Creates a message response
	 * 
	 * @param initialMessage The initial message to send. A blank initial message means to not send one.
	 * @param channel The channel to send the message in
	 * @param userID The user to listen to
	 * @param callback The callback to run after removal of the MessageResponse if a valid response is made. 
	 * Returning true means to continue the chain if there is one. Returning false halts the chain if there is one.
	 * @param validResponses The valid responses to listen for (case insensitive). 
	 * This is compared internally to messages sent using getContentDisplay().
	 * None of the responses can be null. A blank string as the only valid response means any response is allowed.
	 */
	public MessageResponse(String initialMessage, MessageChannel channel, long userID, BiFunction<MessageReceivedEvent, MessageResponse, Boolean> callback, String... validResponses) {
		if(initialMessage == null) {
			throw new IllegalArgumentException("The initial message can't be null.");
		} else if(channel == null) {
			throw new IllegalArgumentException("The channel can't be null.");
		} else if(callback == null) {
			throw new IllegalArgumentException("The callback to run can't be null.");
		} else if(validResponses == null || validResponses.length == 0) {
			throw new IllegalArgumentException("The array of valid responses can't be null or empty.");
		} 
		
		if(channel.getType() == ChannelType.PRIVATE) {
			isTextChannel = false;
		} else {
			isTextChannel = true;
		}
		
		this.initialMessage = initialMessage;
		this.channelID = channel.getIdLong();
		this.validResponses = new HashSet<String>();
		JDA = channel.getJDA();
		responseUserID = userID;
		responseCallback = callback;
		timeout = 30000;

		for(String response : validResponses) {
			if(response == null) {
				throw new IllegalArgumentException("No valid response can be null.");
			} else if(response.isEmpty() && validResponses.length != 1) {
				throw new IllegalArgumentException("If there is a blank string, it must be the only valid response in the array.");
			}
			
			String formattedResponse = response;
			
			if(!response.isEmpty()) {
				formattedResponse = BotUtil.stripWhiteSpace(response);
				
				if(formattedResponse.isEmpty()) {
					throw new IllegalArgumentException("None of the valid responses can contain purely whitespace.");
				}
			}
			
			this.validResponses.add(formattedResponse.toLowerCase());
		}
	}
	
	/**
	 * Copies a MessageResponse into a new one
	 * 
	 * @param response The original MessageResponse
	 */
	public MessageResponse(MessageResponse response) {
		this(response, null);
	}
	
	/**
	 * Copies a MessageResponse, but with a new initial message
	 * 
	 * @param response The original MessageResponse
	 * @param newInitialMessage The new initial message. A blank initial message means to not send one.
	 */
	public MessageResponse(MessageResponse response, String newInitialMessage) {
		this(response, newInitialMessage, response.getTimeout());
	}
	
	/**
	 * Copies a MessageResponse, but with a new initial message and a new timeout
	 * 
	 * @param response The original MessageResponse
	 * @param newInitialMessage The new initial message. A blank initial message means to not send one.
	 * @param newTimeout The new timeout.
	 */
	public MessageResponse(MessageResponse response, String newInitialMessage, long newTimeout) {
		String initialMessage = newInitialMessage;
		
		if(initialMessage == null) {
			initialMessage = response.getInitialMessage();
		}
		
		this.initialMessage = initialMessage;
		channelID = response.getChannelID();
		JDA = response.getJDA();
		responseUserID = response.getUserID();
		responseCallback = response.getResponseCallback();
		timeout = response.getTimeout();
		cancelCallback = response.getCancelCallback();
		timeoutCallback = response.getTimeoutCallback();
		nextResponse = response.getNextResponse();
		cancelMessage = response.getCancelMessage();
		validResponses = response.validResponses;
		cancelResponses = response.cancelResponses;
		isTextChannel = response.isTextChannel;
	}
	
	/**
	 * Creates a message response but with no success callback that will continue chaining.
	 * 
	 * @param initialMessage The initial message to send. A blank initial message means to not send one.
	 * @param channel The channel to send the message in
	 * @param userID The user to listen to
	 * @param validResponses The valid responses to listen for (case insensitive). 
	 * This is compared internally to messages sent using getContentDisplay().
	 * None of the responses can be null. A blank string as the only valid response means any response is allowed.
	 */
	public MessageResponse(String initialMessage, MessageChannel channel, long userID, String... validResponses) {
		this(initialMessage, channel, userID, (e, r) -> true, validResponses);
	}

	/**
	 * Gets the initial message of the MessageResponse
	 * 
	 * @return The initial message that is sent as soon as this callback is added
	 */
	public String getInitialMessage() {
		return initialMessage;
	}

	/**
	 * Gets the ID of the user to listen to
	 * 
	 * @return the ID of the user to listen to
	 */
	public long getUserID() {
		return responseUserID;
	}

	/**
	 * Gets the callback associated with the MessageResponse
	 * @return The callback to run if a valid response is said
	 */
	public BiFunction<MessageReceivedEvent, MessageResponse, Boolean> getResponseCallback() {
		return responseCallback;
	}
	
	/**
	 * Gets the callback to call on cancellation
	 * 
	 * @return The cancel callback
	 */
	public BiFunction<MessageReceivedEvent, MessageResponse, Boolean> getCancelCallback() {
		return cancelCallback;
	}
	
	/**
	 * Gets the callback to call on timeout
	 * 
	 * @return The timeout callback
	 */
	public Function<MessageResponse, Boolean> getTimeoutCallback() {
		return timeoutCallback;
	}

	/**
	 * Gets an unmodifiable set of valid responses
	 * 
	 * @return An unmodifiable set of the valid responses which runs the specified callback
	 */
	public Set<String> getValidResponses() {
		return Collections.unmodifiableSet(validResponses);
	}

	/**
	 * Gets an unmodifiable set of cancel responses
	 * 
	 * @return An unmodifiable set of responses for which to cancel and remove the callback for
	 */
	public Set<String> getCancelResponses() {
		if(cancelResponses == null) {
			return null;
		}
		
		return Collections.unmodifiableSet(cancelResponses);
	}
	
	/**
	 * Gets the channel ID in which the initial message was sent
	 * @return The channel ID in which the initial message was sent
	 */
	public long getChannelID() {
		return channelID;
	}
	
	/**
	 * The channel in which the initial message was sent. This won't update the channel.
	 * 
	 * @return The channel in which the initial message was sent based on the ID. Null if an internal error occurs.
	 */
	public MessageChannel getChannel() {
		return getChannel(false);
	}
	
	/**
	 * The channel in which the initial message was sent
	 * 
	 * @param update Whether to force update the channel's cache
	 * @return The channel in which the initial message was sent based on the ID. Null if an internal error occurs.
	 */
	public MessageChannel getChannel(boolean update) {
		if(isTextChannel) {
			channel = JDA.getTextChannelById(channelID);
		} else {
			if(!update && channel != null) {
				return channel;
			}
			
			try {
				User user = getUser();
				
				if(user == null) {
					return null;
				}
				
				channel = user.openPrivateChannel().submit().get(1, TimeUnit.SECONDS);
			} catch(Exception e) {
				return channel;
			}
		}
		
		return channel;
	}
	
	/**
	 * Get the user associated with the MessageResponse
	 * 
	 * @return The user associated with the MessageResponse. Null if an internal error occurs.
	 */
	public User getUser() {
		return BotUtil.getUser(responseUserID, JDA);
	}
	
	/**
	 * Returns the timeout. If no timeout was set, this defaults to 30 seconds.
	 * 
	 * @return The timeout in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}
	

	/**
	 * Returns the cancel message
	 * @return The cancel message. Defaults to "Cancelling..." if no message was set.
	 */
	public String getCancelMessage() {
		if(cancelMessage == null) {
			cancelMessage = "Cancelling...";
		}
		
		return cancelMessage;
	}
	
	/**
	 * Returns the JDA instance of the channel associated with this object
	 * 
	 * @return The JDA instance of the channel associated with this object
	 */
	public JDA getJDA() {
		return JDA;
	}
	
	/**
	 * Returns the MessageResponse chained to this one
	 * 
	 * @return The MessageResponse chained to this one. Null if there is none.
	 */
	public MessageResponse getNextResponse() {
		return nextResponse;
	}
	
	/**
	 * Returns whether the MessageResponse has been finalized.
	 * 
	 * @return Whether the MessageResponse has been finalized.
	 */
	public boolean isFinalized() {
		return isFinalized;
	}
	
	/**
	 * Sets the message to send after cancellation. This message won't be given
	 * on timeout if a timeout message is created or a timeout callback returns false.
	 * 
	 * @param cancelMessage The cancellation message
	 * @return The modified MessageResponse object
	 */
	public MessageResponse setCancelMessage(String cancelMessage) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		} else if(cancelMessage == null || cancelMessage.isBlank()) {
			throw new IllegalArgumentException("The cancel message can't be null or blank.");
		}
		
		this.cancelMessage = cancelMessage;
		
		return this;
	}
	
	/**
	 * Sets the message to give on timeout. The timeout callback takes precedence over this. Even if the timeout callback 
	 * returns true, the cancel message will display and not this.
	 * 
	 * @param timeoutMessage The message to send on timeout. A blank string means no message will be sent.
	 * @return The modified Messageresponse object.
	 */
	public MessageResponse setTimeoutMessage(String timeoutMessage) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		} else if(timeoutCallback == null) {
			String formattedTimeoutMessage = BotUtil.stripWhiteSpace(timeoutMessage);
			
			if(formattedTimeoutMessage.isEmpty()) {
				timeoutCallback = e -> false;
			} else {
				MessageChannel channel = getChannel();
				
				if(channel == null) {
					ErrorLogger.LogIssue("Timeout message couldn't be issued.");
				} else {
					timeoutCallback = response -> {BotUtil.sendMessage(channel, formattedTimeoutMessage); return false;};
				}
			}
		}
		
		return this;
	}
	
	/**
	 * Sets the responses to look for to cancel the response
	 * 
	 * @param cancelResponses The responses to listen to to cancel the response. 
	 * The array must not be empty or null and cannot contain null or blank responses.
	 * @return The modified MessageResponse object
	 */
	public MessageResponse setCancelResponses(String... cancelResponses) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		} else if(cancelResponses == null || cancelResponses.length == 0) {
			throw new IllegalArgumentException("The cancel response array can't be null or empty.");
		}
		
		String[] cancelResponsesCopy = cancelResponses.clone();
		HashSet<String> cancelResponseSet = new HashSet<String>();
		
		for(String response : cancelResponsesCopy) {
			if(response == null || response.isBlank()) {
				throw new IllegalArgumentException("None of the cancel responses can be null or blank.");
			}
			
			String formattedResponse = BotUtil.stripWhiteSpace(response);
			
			if(formattedResponse.isEmpty()) {
				throw new IllegalArgumentException("None of the cancel responses can be blank.");
			}
			
			cancelResponseSet.add(formattedResponse);
		}
		
		this.cancelResponses = cancelResponseSet;
		
		return this;
	}
	
	/**
	 * Adds a cancel callback to the response to run right after cancellation and removal of the MessageResponse. 
	 * Returning true displays the cancel message. Returning false supresses it. This is not called on timeout 
	 * and is run directly after cancellation.
	 * 
	 * @param cancelCallback The cancel callback
	 * @return The modified MessageResponse object
	 */
	public MessageResponse setCancelCallback(BiFunction<MessageReceivedEvent, MessageResponse, Boolean> cancelCallback) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		}
		
		this.cancelCallback = cancelCallback;
		
		return this;
	}
	
	/**
	 * Adds a timeout callback to the response to run after timeout and removal of the MessageResponse. Returning 
	 * true displays the cancel message. Returning false supresses it.
	 * 
	 * @param timeoutCallback The timeout callback
	 * @return The modified MessageResponse object
	 */
	public MessageResponse setTimeoutCallback(Function<MessageResponse, Boolean> timeoutCallback) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		}
		
		this.timeoutCallback = timeoutCallback;
		
		return this;
	}
	
	/**
	 * The time until the callback automatically cancels. The timeout has millisecond precision truncated.
	 * 30 seconds is the default. The timeout must be more than 0.
	 * 
	 * @param timeout The time in the specified unit
	 * @param timeUnit The unit of time to use
	 * @return The modified MessageResponse object
	 */
	public MessageResponse setTimeout(long timeout, TimeUnit timeUnit) {
		if(isFinalized) {
			throw new IllegalStateException("The MessageResponse has already been finalized.");
		} else if(timeout <= 0) {
			throw new IllegalArgumentException("The timeout must be more than 0.");
		}
		
		timeout = TimeUnit.MILLISECONDS.convert(timeout, timeUnit);
		
		this.timeout = timeout;
		
		return this;
	}
	
	/**
	 * Chains a MessageResponse on to the current MessageResponse. All getters and setters will only apply to the first MessageResponse
	 * in the chain. However, MessageResponses can be chained multiple times to allow for longer chains.
	 * 
	 * @param response The MessageResponse to chain on. It must be built already.
	 * @return The modified MessageResponse
	 */
	public MessageResponse chainResponse(MessageResponse response) {
		if(isFinalized) {
			throw new IllegalStateException("This MessageResponse has already been finalized.");
		} else if(!response.isFinalized) {
			throw new IllegalArgumentException("The provided MessageResponse hasn't been finalized yet.");
		}
		
		MessageResponse currentResponse = this;
		
		while(currentResponse.getNextResponse() != null) {
			currentResponse = currentResponse.getNextResponse();
		}
		
		currentResponse.nextResponse = response;
		
		return this;
	}
	
	/**
	 * Finalizes the MessageResponse, causing any calls to mutators in the future to throw an exception.
	 * Finalizing an already finalized MessageResponse will have no effect.
	 * 
	 * @return The modified MessageResponse object
	 */
	public MessageResponse build() {
		isFinalized = true;
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((cancelMessage == null) ? 0 : cancelMessage.hashCode());
		result = prime * result + ((cancelResponses == null) ? 0 : cancelResponses.hashCode());
		result = prime * result + (int) (channelID ^ (channelID >>> 32));
		result = prime * result + ((initialMessage == null) ? 0 : initialMessage.hashCode());
		result = prime * result + (int) (responseUserID ^ (responseUserID >>> 32));
		result = prime * result + (int) (timeout ^ (timeout >>> 32));
		result = prime * result + ((validResponses == null) ? 0 : validResponses.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		} else if(obj == null || !(obj instanceof MessageResponse)) {
			return false;
		}
		
		MessageResponse response = (MessageResponse) obj;
		
		return timeout == response.getTimeout() &&
				channelID == response.getChannelID() &&
				responseUserID == response.getUserID() &&
				Objects.equals(initialMessage, response.initialMessage) &&
				Objects.equals(validResponses, response.validResponses) &&
				Objects.equals(cancelResponses, response.cancelResponses) &&
				Objects.equals(cancelMessage, response.cancelMessage);
	}
}