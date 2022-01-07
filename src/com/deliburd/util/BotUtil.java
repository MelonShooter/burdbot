package com.deliburd.util;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class BotUtil {
	private static final Pattern whitespaceStripper = Pattern.compile("^\\s+|\\s+$");
	private static final Pattern pingPattern = Pattern.compile("@(?:everyone|here)|<@(?:!|&)?\\d+>");
	
	private BotUtil() {}
	
	/**
	 * Checks if the bot has write permissions. If the channel isn't a TextChannel or ThreadChannel provided, this returns true
	 *
	 * @param channel The channel
	 * @return Whether the bot has write permissions.
	 */
	public static boolean hasWritePermission(MessageChannel channel) {
		if(channel.getType() == ChannelType.TEXT) {
			TextChannel textChannel = (TextChannel) channel;
			return textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_SEND);
		} else if(channel.getType().isThread()) {
			ThreadChannel threadChannel = (ThreadChannel) channel;

			return threadChannel.getGuild().getSelfMember().hasPermission(threadChannel, Permission.MESSAGE_SEND_IN_THREADS);
		}

		return true;
	}
	
	/**
	 * Checks if the bot has write permissions. If the channel isn't a TextChannel provided, this returns true
	 * 
	 * @param event The mssage received event
	 * @return Whether the bot has write permissions. Returns true if not from a guild or not a text channel
	 */
	public static boolean hasWritePermission(MessageReceivedEvent event) {
		return hasWritePermission(event.getChannel());
	}
	
	/**
	 * Queues a message to be sent. Will silently fail if it can't be sent
	 * 
	 * @param channel The channel to send the emssage to
	 * @param message The message's content
	 */
	public static void sendMessage(MessageChannel channel, CharSequence message) {
		channel.sendMessage(message).queue(null, error -> {
			if(error instanceof InsufficientPermissionException || isErrorResponseType(error, ErrorResponse.CANNOT_SEND_TO_USER)) {
				return;
			}
			
			ErrorLogger.LogException(error);
		});
	}
	
	/**
	 * Queues a message to be sent. Will try sending a DM if it fails.
	 * 
	 * @param channel The channel to send the emssage to
	 * @param message The message's content
	 * @param userID The user to send the DM to if the message fails
	 */
	public static void sendMessage(MessageChannel channel, CharSequence message, long userID) {
		sendMessage(channel, message, userID, null);
	}
	
	/**
	 * Queues a message to be sent. Will try sending a DM if it fails. On success, the callback
	 * provided will be run.
	 * 
	 * @param channel The channel to send the emssage to
	 * @param message The message's content
	 * @param userID The user to send the fallback DM to.
	 * @param consumer The callback to run on success
	 */
	public static void sendMessage(MessageChannel channel, CharSequence message, long userID, Consumer<Message> consumer) {
		if(channel != null && hasWritePermission(channel)) {
			channel.sendMessage(message).queue(consumer, error -> {
				if(channel.getType() == ChannelType.PRIVATE) {
					return;
				}
				
				BotUtil.getUser(userID, channel.getJDA(), user -> {
					if(user == null) {
						return;
					}
					
					sendDM(user, message, consumer);
				});
			});
		} else if(channel != null) {
			BotUtil.getUser(userID, channel.getJDA(), user -> {
				if(user == null) {
					return;
				}
				
				sendDM(user, message, consumer);
			});
		}
	}
	
	/**
	 * Queues a message to be sent. Will try sending a DM if it fails. On success, the callback
	 * provided will be run.
	 * 
	 * @param channel The channel to send the emssage to
	 * @param message The message's content
	 * @param user The user to send the fallback DM to.
	 * @param consumer The callback to run on success
	 */
	public static void sendMessage(TextChannel channel, CharSequence message, User user, Consumer<Message> consumer) {
		if(channel != null && hasWritePermission(channel)) {
			channel.sendMessage(message).queue(consumer, error -> {
				if(user == null) {
					return;
				}
				
				sendDM(user, message, consumer);
			});
		} else {
			if(user == null) {
				return;
			}
			
			sendDM(user, message, consumer);
		}
	}
	
	/**
	 * Sends a DM to the specified user with the given message and runs the callback provided. 
	 * 
	 * @param user The user to send the DM to
	 * @param message The DM's content.
	 * @param consumer The callback to run when the message is sent
	 */
	public static void sendDM(User user, CharSequence message, Consumer<Message> consumer) {
		user.openPrivateChannel().queue(channel -> {
			channel.sendMessage(message).queue(consumer, error -> {
				if (isErrorResponseType(error, ErrorResponse.CANNOT_SEND_TO_USER)) {
					return;
				}

				ErrorLogger.LogException(error);
			});
		}, error -> ErrorLogger.LogException(error));
	}
	
	/**
	 * Returns whether the throwable is an ErrorResponseException and if it equals the given ErrorResponse
	 *
	 * @param error The error
	 * @param response The ErrorResponse enumeration to check the error against
	 * @return Whether the throwable is an ErrorResponseException and equals the given ErrorResponse
	 */
	public static boolean isErrorResponseType(Throwable error, ErrorResponse response) {
		if(error instanceof ErrorResponseException) {
			return ((ErrorResponseException) error).getErrorResponse().equals(response);
		}
		
		return false;
	}
	
	/**
	 * Gets a user by their ID and runs the Consumer provided. Silently fails if an error is thrown.
	 * 
	 * @param userID The user's ID
	 * @param JDA The JDA instance
	 * @param userFunction The Consumer to run when the user is found.
	 */
	public static void getUser(long userID, JDA JDA, Consumer<User> userFunction) {
		if(JDA == null) {
			return;
		}
		
		User user = JDA.getUserById(userID);

		if(user == null) {
			JDA.retrieveUserById(userID, false).queue(userFunction, (error) -> {});
		} else {
			userFunction.accept(user);
		}
	}
	
	/**
	 * Gets a user by their ID and returns it.
	 * 
	 * @param userID The user's ID
	 * @param JDA The JDA instance
	 * @return The user. Null if it fails.
	 */
	public static User getUser(long userID, JDA JDA) {
		if(JDA == null) {
			return null;
		}
		
		User cachedUser = JDA.getUserById(userID);
		
		if(cachedUser == null) {
			try {
				return JDA.retrieveUserById(userID, false).submit().get(1, TimeUnit.SECONDS);
			} catch(Exception e) {
				ErrorLogger.LogException(e);
				return null;
			}
		}
		
		return cachedUser;
	}
	
	/**
	 * Parses a CharSequence and gets rid of whitespace on both ends of the message
	 * 
	 * @param text The text to strip whitespace of
	 * @return The new string with the whitespace stripped
	 */
	public static String stripWhiteSpace(CharSequence text) {
		return whitespaceStripper.matcher(text).replaceAll("");
	}
}
