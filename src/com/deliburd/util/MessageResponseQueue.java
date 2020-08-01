package com.deliburd.util;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class MessageResponseQueue extends ListenerAdapter {
	private final static MessageResponseQueue responseQueue = new MessageResponseQueue();
	private final ConcurrentHashMap<String, Set<MessageResponse>> responseMap;
	private final ConcurrentHashMap<MessageResponse, TimerTask> responseTimeoutMap;
	private final Set<Long> responseUsers;
	private final Timer callbackTimeouts; 
	
	private MessageResponseQueue() {
		responseMap = new ConcurrentHashMap<String, Set<MessageResponse>>(8, .75f, 3);
		responseTimeoutMap = new ConcurrentHashMap<MessageResponse, TimerTask>(8, .75f, 3);
		responseUsers = ConcurrentHashMap.newKeySet();
		callbackTimeouts = new Timer(true);
	}
	
	/**
	 * Gets the singleton MessageResponseQueue object
	 * 
	 * @return The MessageResponseQueue
	 */
	public static MessageResponseQueue getQueue() {
		return responseQueue;
	}
	
	/**
	 * Registers a callback to run after a response is given to your initial message.
	 * Does nothing if an equivalent MessageResponse has already been added.
	 * 
	 * @param response The message response
	 */
	public void addMessageResponse(MessageResponse response) {
		if(!response.isFinalized()) {
			throw new IllegalStateException("Can't add a non-finalized MessageResponse.");
		} else if(hasMessageResponse(response)) {
			return;
		}
		
		var channel = response.getChannel();
		String initialMessage = response.getInitialMessage();
		
		if(channel == null) {
			ErrorLogger.LogIssue("Channel was null upon adding MessageResponse, probably because it was deleted.");
			return;
		}
		
		if(initialMessage.isBlank()) {
			addCallback(response);
		} else {
			BotUtil.sendMessage(channel, response.getInitialMessage(), response.getUserID(), message -> addCallback(response));
		}
	}
	
	/**
	 * Internally used to register the callback to run after a response is given
	 * 
	 * @param message The message which isn't used
	 */
	private void addCallback(MessageResponse response) {
		if(hasMessageResponse(response)) {
			return;
		}
		
		for(String validResponse : response.getValidResponses()) {
			var responseList = responseMap.get(validResponse);
			
			if(responseList == null) {
				responseList = ConcurrentHashMap.newKeySet();
				responseMap.putIfAbsent(validResponse, responseList);
			}
			
			responseList.add(response);
		}
		
		var cancelResponses = response.getCancelResponses();
		
		if(cancelResponses != null) {
			for(String cancelResponse : cancelResponses) {
				var responseList = responseMap.get(cancelResponse);
				
				if(responseList == null) {
					responseList = ConcurrentHashMap.newKeySet();
					responseMap.putIfAbsent(cancelResponse, responseList);
				}
				
				responseList.add(response);
			}
		}
		
		long timeout = response.getTimeout();
		
		TimerTask task = generateRemoveTask(response);

		responseTimeoutMap.put(response, task);
		callbackTimeouts.schedule(task, timeout);
		responseUsers.add(response.getUserID());
	}
	
	/**
	 * Returns true if the queue contains an equivalent MessageResponse
	 * 
	 * @param response The MessageResponse to check
	 * @return Whether the queue contains an equivalent MessageResponse
	 */
	public boolean hasMessageResponse(MessageResponse response) {
		return responseTimeoutMap.containsKey(response);
	}
	
	/**
	 * Returns true if the queue contains a listener for this message whether it be a cancel message or valid response message
	 * for the given user
	 * 
	 * @param responseMessage The response message to check
	 * @param userID The id of the user to check
	 * @return Whether the queue contains an equivalent MessageResponse
	 */
	public boolean hasMessageResponse(String responseMessage, long userID) {
		var responseSet = responseMap.get(responseMessage);
		
		if(responseSet == null) {
			return false;
		} else {
			return responseMap.get(responseMessage).stream().anyMatch(r -> r.getUserID() == userID);
		}
	}
	
	/**
	 * Returns true if the queue contains a listener for this message whether it be a cancel message or valid response message
	 * for the given user in a certain channel
	 * 
	 * @param message The response message to check
	 * @param userID The id of the user to check
	 * @param chID The channel ID to check
	 * @return Whether the queue contains an equivalent MessageResponse
	 */
	public boolean hasMessageResponse(String message, long userID, long chID) {
		var responseSet = responseMap.get(message);
		
		if(responseSet == null) {
			return false;
		} else {
			return responseSet.stream().anyMatch(r -> r.getUserID() == userID && r.getChannelID() == chID);
		}
	}
	
	/**
	 * Resets the timeout of the equivalent MessageResponse in the queue
	 * 
	 * @param response The Messageresponse to check the queue against and reset the timeout
	 */
	public void resetTimeout(MessageResponse response) {
		TimerTask retreivedTask = responseTimeoutMap.get(response);
		
		if(retreivedTask != null) {
			boolean isSuccessful = retreivedTask.cancel();
			
			if(isSuccessful) {
				TimerTask newTask = generateRemoveTask(response);
				responseTimeoutMap.put(response, newTask);
				callbackTimeouts.schedule(newTask, response.getTimeout());
			}
		}
	}
	
	/**
	 * Removes an equivalent MessageResponse in the queue and gives the cancel message
	 * 
	 * @param response The response to remove
	 */
	public void removeMessageResponse(MessageResponse response) {
		removeMessageResponse(response, true);
	}
	
	/**
	 * Removes an equivalent MessageResponse in the queue.
	 * 
	 * @param response The response to remove
	 * @param displayCancelMessage If set to true, the cancel message will be given. The cancel callback will not be called
	 * even if the message is displayed in this case.
	 */
	public void removeMessageResponse(MessageResponse response, boolean displayCancelMessage) {
		removeMessageResponse(response, null, displayCancelMessage);
	}

	/**
	 * Removes the MessageResponses for a given user in the given channel.
	 * 
	 * @param userID The id of the user to check
	 * @param chID The channel ID to check
	 */
	public void removeMessageResponses(long userID, long chID) {
		var responseMapIterator = responseMap.values().iterator();
		boolean foundExtraUserMessageResponse = false;

		while(responseMapIterator.hasNext()) {
			var responseSet = responseMapIterator.next();
			var responseSetIterator = responseSet.iterator();
			
			while(responseSetIterator.hasNext()) {
				MessageResponse response = responseSetIterator.next();
				
				if(response.getUserID() == userID && response.getChannelID() == chID) {
					responseTimeoutMap.remove(response);
					responseSetIterator.remove();
				} else if(response.getUserID() == userID) { // There's a MessageResponse for this user we aren't going to remove
					foundExtraUserMessageResponse = true;
				}
			}

			if(responseSet.isEmpty()) {
				responseMapIterator.remove();
			}
		}
		
		if(!foundExtraUserMessageResponse) {
			responseUsers.remove(userID);
		}
	}
	
	/**
	 * Used internally to remove MessageResponses while disregarding any sets containing the given
	 * message and any sets not containing a response in the given MessageResponse
	 * 
	 * @param response The MessageResponse to remove
	 * @param message The message to disregard the mapped set for
	 * @param displayCancelMessage Whether the callback ran or not. If set to true, the
	 * cancel message will be given.
	 */
	private void removeMessageResponse(MessageResponse response, String message, boolean displayCancelMessage) {
		responseUsers.remove(response.getUserID());
		
		TimerTask responseTimeoutTask = responseTimeoutMap.get(response);
		
		if(responseTimeoutTask != null) {
			responseTimeoutTask.cancel();
			responseTimeoutMap.remove(response);
		} else {
			return;
		}
		
		var responseIterator = responseMap.entrySet().iterator();
		
		while(responseIterator.hasNext()) {
			var messageAndResponseSetEntry = responseIterator.next();
			String triggerMessage = messageAndResponseSetEntry.getKey();
			boolean setContainsResponse = response.getValidResponses().contains(triggerMessage) || 
					response.getCancelResponses() != null && response.getCancelResponses().contains(triggerMessage);
			
			// This message is either a valid or cancel response for this MessageResponse
			// And the response wasn't already taken out of the set.
			if(setContainsResponse && !triggerMessage.equals(message)) {
				var responseSet = messageAndResponseSetEntry.getValue();
				var responseSetIterator = responseSet.iterator();
				
				while(responseSetIterator.hasNext()) {
					if(responseSetIterator.next().equals(response)) {
						responseSetIterator.remove();
						break;
					}
				}
				
				if(responseSet.isEmpty()) {
					responseIterator.remove();
				}
			}
		}
		
		if(displayCancelMessage) {
			MessageChannel channel = response.getChannel();
			
			if(channel == null) {
				ErrorLogger.LogIssue("Cancel message couldn't be sent because channel was probably deleted.");
				return;
			}
			
			sendCancelMessage(response.getChannel(), response.getCancelMessage(), response.getUserID());
		}
	}
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if(event.getAuthor().isBot() || !responseUsers.contains(event.getAuthor().getIdLong())) {
			return;
		}
		
		var messageContents = event.getMessage().getContentDisplay().toLowerCase();
		var responseList = responseMap.get(messageContents);

		if(responseList != null) {
			processResponse(responseList, event, messageContents, false);
		} else {
			var variableResponses = responseMap.get("");
			
			if(variableResponses != null) {
				processResponse(variableResponses, event, "", true); 
			}
		}
	}
	
	private void processResponse(Set<MessageResponse> responseList, MessageReceivedEvent event, String message, boolean isVariable) {
		var setIterator = responseList.iterator();
		
		while(setIterator.hasNext()) {
			MessageResponse response = setIterator.next();
			boolean matchesUser = event.getAuthor().getIdLong() == response.getUserID();
			boolean matchesChannel = event.getChannel().getIdLong() == response.getChannelID();
			
			if(!matchesUser || !matchesChannel || !responseTimeoutMap.containsKey(response)) {
				continue;
			}
			
			setIterator.remove();
			
			if(responseList.isEmpty()) {
				responseMap.remove(message);
			}
			
			var validResponses = response.getValidResponses();
			var cancelResponses = response.getCancelResponses();
			removeMessageResponse(response, message, false);
			
			if(validResponses.contains(message) && cancelResponses != null && cancelResponses.contains(message)) {
				throw new IllegalStateException("No MessageResponse object can contain a duplicate valid and cancel response string. Removing the object...");
			} else if(validResponses.contains(message)) {
				boolean isContinuing = response.getResponseCallback().apply(event, response);
				
				MessageResponse chainedResponse = response.getNextResponse();
				
				if(isContinuing && chainedResponse != null) {
					addMessageResponse(chainedResponse);
				}
			} else if(cancelResponses != null && cancelResponses.contains(message)) {
				boolean doCancelMessage;
				var cancelCallback = response.getCancelCallback();
				
				if(cancelCallback != null) {
					doCancelMessage = cancelCallback.apply(event, response);
				} else {
					doCancelMessage = true;
				}
				
				if(doCancelMessage) {
					MessageChannel channel = response.getChannel();
					
					if(channel == null) {
						ErrorLogger.LogIssue("Cancel message couldn't be sent because channel was probably deleted.");
						return;
					}
					
					sendCancelMessage(response.getChannel(), response.getCancelMessage(), response.getUserID());
				}
			} else {
				throw new IllegalStateException("Corrupt MessageResponse. This should never happen.");
			}
		}
	}
	
	private void sendCancelMessage(MessageChannel channel, String cancelMessage, long userID) {
		BotUtil.sendMessage(channel, cancelMessage, userID);
	}
	
	private TimerTask generateRemoveTask(MessageResponse response) {
		return new TimerTask() {
			@Override
			public void run() {
				removeMessageResponse(response, false);
				
				boolean doCancelMessage;
				
				if(response.getTimeoutCallback() != null) {
					doCancelMessage = response.getTimeoutCallback().apply(response);
				} else {
					doCancelMessage = true;
				}
				
				if(doCancelMessage) {
					MessageChannel channel = response.getChannel(true);
					
					if(channel == null) {
						ErrorLogger.LogIssue("Cancel message not displayed because channel was deleted or there was some error.");
						return;
					}
					
					sendCancelMessage(channel, response.getCancelMessage(), response.getUserID());
				}
			}
		};
	}
}
