package com.deliburd.bot.burdbot;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import javax.security.auth.login.LoginException;

import com.deliburd.bot.burdbot.commands.CommandManager;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.bot.burdbot.commands.MultiCommandAction;
import com.deliburd.readingpuller.ReadingManager;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.bot.burdbot.Main;
import com.deliburd.recorder.RecorderConstant;
import com.deliburd.util.MessageResponse;
import com.deliburd.util.NumberUtil;
import com.deliburd.util.Pair;
import com.deliburd.recorder.util.audio.AudioCompression;
import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.MessageResponseQueue;
import com.deliburd.util.ServerConfig;
import com.fasterxml.jackson.databind.JsonNode;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;

public class Main extends ListenerAdapter {
	public static void main(String[] args) throws LoginException {
        reloadTexts(Constant.RELOAD_TEXTS_INTERVAL);
        
		ServerConfig.registerTable("templates");
		JDABuilder burdRecorder = JDABuilder.createDefault(BotConstant.BOT_TOKEN_STRING);
		String helpDescription = "Displays a list of commands and their descriptions.";
        CommandManager commandManager = new CommandManager(Constant.COMMAND_PREFIX, helpDescription, burdRecorder);
        
		MultiCommand fetchTextCommand = commandManager.addCommand("fetchtext", Constant.FETCH_TEXT_DESCRIPTION)
				.setMinArguments(2)
				.setDefaultAction(new MultiCommandAction() {
					@Override
					public void OnCommandRun(String[] args, MessageReceivedEvent event, MultiCommand command) {
						if (!ReadingManager.isRegeneratingTexts()) {
							MessageChannel channel = event.getChannel();
							BotUtil.sendMessage(channel, "```" + ReadingManager.fetchText(args[0], args[1]) + "```");
						}
					}
				});
        
		boolean firstTime = true;
		
        for(var language : ScraperLanguage.values()) {
        	fetchTextCommand.addArgument(0, language.toString(), language.getAliases());
        	
        	for(var difficulty : ScraperDifficulty.values()) {
        		if(firstTime) {
        			fetchTextCommand.addArgument(1, difficulty.toString(), difficulty.getAliases());
        		}

        		fetchTextCommand = fetchTextCommand.addFinalArgumentPath(language.toString(), difficulty.toString());
        	}
        	
        	firstTime = false;
        }
        
		fetchTextCommand.setArgumentDescriptions("The language of the text", "The difficulty of the text")
				.addCommandNames("fetchtxt", "ftxt", "ftext")
				.setCooldown(10)
				.finalizeCommand();
		
		String startRecordDescription = "Starts recording your voice in the voice channel you are in "
				+ "and DMs an audio file back when the recording is stopped. "
			+ "The recording goes on until the upload limit for the server is reached. "
			+ "After the recording is finished, you can choose to send it to the channels designated when "
			+ "you type ,listwhitelistedchannels";
		commandManager.addCommand("startrecording", startRecordDescription)
				.addArgument(0, "compressed")
				.addArgument(0, "uncompressed")
				.setBaseAction(Main::startRecord)
				.setDefaultAction(Main::startRecord)
				.addFinalArgumentPath("compressed")
				.addFinalArgumentPath("uncompressed")
				.setArgumentDescriptions("Sets whether or not the audio is compressed (default, recommended) or uncompressed.")
				.addCommandNames("startrecord", "srecord", "startrec", "srec", "sr")
				.finalizeCommand();
		
		String stopRecordDescription = "Stops recording your voice and DMs the audio file back for review. "
				+ "If you like it, you can have it sent to a channel by responding to the bot.";
		commandManager.addCommand("endrecording", stopRecordDescription, Main::stopRecord)
				.addCommandNames("endrecord", "erecord", "endrec", "erec", "er")
				.finalizeCommand();
	
		String channelWhitelistDescription = "Whitelists a channel for audio to be sent to. Templates can be optionally created"
				+ "To create a template, replace any strings you want the user to fill in with '%s' like so "
				+ "\"I am a pink fluffy %s.\" To fill in the part of the template with a ping to the user, use %p. To use %s or %p "
				+ "literally, put a % in front of it like so, '%%s' or '%%p'. After you've typed the command, the bot will "
				+ "guide through the process step by step to complete the template.";
		String channelIDArgumentDescription = "The channel ID of the channel you want to whitelist.";
		String templateArgumentDescription = "The template's structure. Type the help command to learn how to create one if you don't know already. (Optional)";
		commandManager.addCommand("addwhitelistedchannel", channelWhitelistDescription)
				.setMinArguments(1)
				.addArgument(0)
				.addMultiArgument()
				.setDefaultAction(Main::addChannelToWhitelist)
				.addFinalArgumentPath("")
				.addFinalArgumentPath("", "")
				.setArgumentDescriptions(channelIDArgumentDescription, templateArgumentDescription)
				.addCommandNames("addwchannel", "addwc")
				.setPermissionRestrictions(Permission.MANAGE_CHANNEL)
				.finalizeCommand();
		
		String removeChannelCommandDescription = "Removes a whitelisted channel so that audio can't be sent there anymore.";
		String removedChannelIDDescription = "The channel ID of the whitelisted channel you want to remove from the whitelist.";
		commandManager.addCommand("removewhitelistedchannel", removeChannelCommandDescription)
				.setMinArguments(1)
				.addArgument(0)
				.addFinalArgumentPath(Main::removeChannelFromWhitelist, "")
				.setArgumentDescriptions(removedChannelIDDescription)
				.addCommandNames("rmwhitelistedchannel", "removewchannel", "removewc", "rmwchannel", "rmwc")
				.setPermissionRestrictions(Permission.MANAGE_CHANNEL)
				.finalizeCommand();
		
		String showChannelCommandDescription = "Shows a list of all whitelisted channels and their templates if they have one.";
		commandManager.addCommand("listwhitelistedchannels", showChannelCommandDescription, Main::showWhitelistedChannels)
				.addCommandNames("listwchannels", "listwcs")
				.finalizeCommand();
		
		String getTemplateInfoCommandDescription = "Gets info on the template for a whitelisted channel.";
		commandManager.addCommand("gettemplateinfo", getTemplateInfoCommandDescription)
				.setMinArguments(1)
				.addArgument(0)
				.addFinalArgumentPath(Main::getTemplateInfo, "")
				.setArgumentDescriptions("The channel ID of the whitelisted channel you want to get the template info on")
				.finalizeCommand();
		
		String deleteAudioFileCommandDescription = "PERMANENTLY deletes an audio file that you recorded in the past.";
		commandManager.addCommand("deleteaudiofile", deleteAudioFileCommandDescription)
				.setMinArguments(1)
				.addArgument(0)
				.addFinalArgumentPath(Main::deleteAudioFile, "")
				.setArgumentDescriptions("The message id or link to your audio file that you want deleted. You must be in the "
						+ "channel the audio file was sent to unless a link to the message was provided instead of an ID.")
				.finalizeCommand();

		burdRecorder.addEventListeners(AudioReceiverHandler.getHandler(), MessageResponseQueue.getQueue()).build();
    }
    
    /**
     * Reloads texts on startup and daily
     * 
     * @param interval The interval to reload texts in seconds
     */
    private static void reloadTexts(int interval) {
		Timer reloadTextsTimer = new Timer();
		reloadTextsTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					ReadingManager.regenerateTexts();
				} catch(ConcurrentModificationException e) {
					ErrorLogger.LogException(e);
				}
			}
		}, 0, interval * 1000);
	}
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
    	final boolean canWrite = BotUtil.hasWritePermission(event);
    	final long musicChannelID = 263643662808776704L;
    	final String message = event.getMessage().getContentRaw();
    	
    	final String[] botPrefixes = {
    		"-",
    		"--",
    		"---",
    		";;",
    		"!",
    		"!!",
    		"/"
    	};
    	
    	boolean containsMusicPrefix = false;
    	
    	for (var prefix : botPrefixes) {
    		if(message.startsWith(prefix)) {
    			containsMusicPrefix = true;
    		}
    	}
    	
    	if(canWrite && event.getChannel().getIdLong() == musicChannelID && containsMusicPrefix) {
    		BotUtil.sendMessage(event.getChannel(), "Please put music bot commands in <#247135634265735168> as they do not work here");
    	}
    }
    
	/**
	 * Runs when someone calls the delete command for an audio file
	 * 
	 * @param args The arguments given from the command
	 * @param event The MessageReceivedEvent associated when the delete command was run
	 * @param command The delete command's MultiCommand object
	 */
	private static void deleteAudioFile(String[] args, MessageReceivedEvent event, MultiCommand command) {
		String linkOrID = args[0];
		MessageChannel commandChannel = event.getChannel();
		Long messageID = NumberUtil.stringToLong(linkOrID);
		MessageChannel targetChannel;
		
		String noMessageFoundString = "I couldn't find the message to delete. Ensure that you are in the channel in which your "
				+ "audio file was sent.";
		
		if(messageID == null && BotUtil.isPossibleDiscordLink(linkOrID)) {
			Matcher possibleLinkMatcher = Message.JUMP_URL_PATTERN.matcher(linkOrID);

			if(possibleLinkMatcher.matches()) {
				long channelID = NumberUtil.stringToLong(possibleLinkMatcher.group("channel"));
				targetChannel = event.getGuild().getTextChannelById(channelID);
				
				if(targetChannel == null || !possibleLinkMatcher.group("guild").equals(event.getGuild().getId())) {
					command.giveInvalidArgumentMessage(commandChannel, "This jump URL isn't valid or doesn't lead to this server.");
					return;
				}
				
				messageID = NumberUtil.stringToLong(possibleLinkMatcher.group("message"));
			} else {
				command.giveInvalidArgumentMessage(commandChannel, "This link isn't valid.");
				return;
			}
		} else if(messageID == null) {
			command.giveInvalidArgumentMessage(commandChannel, "This message ID isn't valid.");
			return;
		} else {
			targetChannel = commandChannel;
		}
		
		if(event.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) {
			Consumer<Message> onMessageFound = msg -> {
				if(msg.getAuthor().getIdLong() != event.getGuild().getSelfMember().getIdLong()) {
					command.giveInvalidArgumentMessage(commandChannel, "This message isn't an audio file that I've posted.");
					return;
				}
				
				var files = msg.getAttachments();
				
				if(files.isEmpty()) {
					command.giveInvalidArgumentMessage(commandChannel, "This message doesn't have an audio file attached to it.");
					return;
				} else if(files.size() > 1) {
					command.giveInvalidArgumentMessage(commandChannel, "There's more than 1 file attached to this message.");
					return;
				}
				
				Attachment file = files.get(0);
				String fileExtension = file.getFileExtension();
				
				if(fileExtension == null || !fileExtension.equals("mp3") && !fileExtension.equals("wav")) {
					String invalidAudioFileMessage = "This message doesn't have an audio file recorded by me attached to it.";
					command.giveInvalidArgumentMessage(commandChannel, invalidAudioFileMessage);
					return;
				} else if(!file.getFileName().startsWith(event.getAuthor().getId() + "-")) {
					command.giveInvalidArgumentMessage(commandChannel, "This audio file isn't yours.");
					return;
				}
				
				msg.delete().queue(m -> BotUtil.sendMessage(commandChannel, "Your recording was deleted."), e -> {
					if(e instanceof ErrorResponseException) {
						command.giveInvalidArgumentMessage(commandChannel, noMessageFoundString);
					} else {
						ErrorLogger.LogException(e, commandChannel);
					}
				});
			};
			
			targetChannel.retrieveMessageById(messageID).queue(onMessageFound, error -> {
				if(error instanceof ErrorResponseException) {
					command.giveInvalidArgumentMessage(commandChannel, noMessageFoundString);
				} else {
					ErrorLogger.LogException(error, commandChannel);
				}
			});
		} else {
			
		}
	}
	
	/**
	 * Runs when someone calls the command to get the info on a certain template
	 * 
	 * @param args The arguments given from the command
	 * @param event The MessageReceivedEvent associated when the command was run
	 * @param command The command's MultiCommand object
	 */
	private static void getTemplateInfo(String[] args, MessageReceivedEvent event, MultiCommand command) {
		Long channelID = NumberUtil.stringToLong(args[0]);
		MessageChannel commandChannel = event.getChannel();
		
		if(channelID == null) {
			command.giveInvalidArgumentMessage(commandChannel, "This channel ID isn't valid.");
			return;
		}
		
		TextChannel foundChannel = event.getJDA().getTextChannelById(channelID);
		
		if(foundChannel == null) {
			command.giveInvalidArgumentMessage(commandChannel, "This channel ID isn't valid.");
			return;
		}
		
		JsonNode templateNode;
		
		try {
			templateNode = ServerConfig.getNode("templates", event.getGuild().getIdLong(), args[0]);
		} catch (IOException e) {
			ErrorLogger.LogException(e, commandChannel);
			return;
		}
		
		if(templateNode == null) {
			command.giveInvalidArgumentMessage(commandChannel, "This channel isn't whitelisted.");
			return;
		}
		
		MessageBuilder templateInfoBuilder = new MessageBuilder(getChannelTemplateData(event, args[0], templateNode));
		if(templateNode.isArray()) {
			if(templateNode.size() > 3) {
				templateInfoBuilder.append("Template questions:");
				
				for(int i = 3; i < templateNode.size(); i += 2) {
					String templateQuestion = templateNode.get(i).asText();
					templateInfoBuilder.append("\n").appendCodeLine(templateQuestion);
				}
			} else {
				templateInfoBuilder.append("No template questions.");
			}
		}
		
		BotUtil.sendMessage(commandChannel, templateInfoBuilder);
	}
	
	/**
	 * Is run when the command is run to show all whitelisted channels
	 * 
	 * @param event The MessageReceivedEvent associated with the command being run
	 */
	private static void showWhitelistedChannels(MessageReceivedEvent event) {
		TextChannel channel = event.getTextChannel();
		Iterator<Entry<String, JsonNode>> templateIterator;
		
		try {
			templateIterator = ServerConfig.getTableData("templates", event.getGuild().getIdLong()).fields();
		} catch (IOException e) {
			ErrorLogger.LogException(e, channel);
			return;
		}
		
		if(!templateIterator.hasNext()) {
			BotUtil.sendMessage(channel, "There are no whitelisted channels.");
			return;
		}
		
		MessageBuilder whitelistedChannelListBuilder = new MessageBuilder();
		
		while(templateIterator.hasNext()) {
			var channelTemplateEntry = templateIterator.next();
			String channelLine = getChannelTemplateData(event, channelTemplateEntry.getKey(), channelTemplateEntry.getValue());
			whitelistedChannelListBuilder.append(channelLine);
		}
		
		whitelistedChannelListBuilder.append("For more info on a specific template, use ``")
				.append(Constant.COMMAND_PREFIX)
				.append("gettemplateinfo``");

		BotUtil.sendMessage(channel, whitelistedChannelListBuilder);
	}

	/**
	 * Gets the data associated with a template given a channelID, the event, and the JsonNode for the template
	 * 
	 * @param event The MessageReceivedEvent associated when the command was run
	 * @param channelIDString The channel ID of the channel to get the template data for as a string
	 * @param channelTemplate The JsonNode containing the template data
	 * @return The template data for the given channel as a string
	 */
	private static String getChannelTemplateData(MessageReceivedEvent event, String channelIDString, JsonNode channelTemplate) {
		MessageBuilder channelInfoBuilder = new MessageBuilder(); 
		Long channelID = NumberUtil.stringToLong(channelIDString);
		
		if(channelID == null) {
			channelInfoBuilder.append("Malformed channel ID (Contact DELIBURD):");
			ErrorLogger.LogIssue("Malformed channel ID when someone tried listing them.");
		} else {
			TextChannel foundChannel = event.getJDA().getTextChannelById(channelID);
			
			if(foundChannel == null) {
				channelInfoBuilder.append("Deleted channel");
			} else {
				channelInfoBuilder.append(foundChannel);
				
				if(!BotUtil.hasWritePermission(foundChannel)) {
					channelInfoBuilder.append(" (I don't have permission to write in this channel)");
				}
			}
		}
		
		if(channelTemplate.isArray() && !channelTemplate.isEmpty()) {
			channelInfoBuilder.append(" - ")
					.append(channelTemplate.get(0).asText());
			
			if(channelTemplate.size() == 1) {
				channelInfoBuilder.appendCodeLine("\nNo template\n");
			} else {
				var templateInfo = new ArrayList<String>(channelTemplate.size() / 2 + 2);
				templateInfo.add(channelTemplate.get(1).asText());
				
				for(int i = 2; i < channelTemplate.size(); i += 2) {
					String replacementName = channelTemplate.get(i).asText().toUpperCase();
					
					if(replacementName.isBlank()) {
						templateInfo.add("UNKNOWN (CONTACT DELIBURD)");
					} else {
						templateInfo.add(replacementName);	
					}
				}
				
				var channelTemplateIterator = templateInfo.iterator();
				String template = channelTemplateIterator.next();
				
				if(template.isBlank()) {
					channelInfoBuilder.appendCodeLine("\nMalformed template (Contact DELIBURD)");
					ErrorLogger.LogIssue("Malformed template when someone tried listing them.");
				} else {
					String formattedTemplate = RecorderConstant.REPLACEMENT_PATTERN.matcher(template).replaceAll(result -> {	
						if(!channelTemplateIterator.hasNext()) {
							return "<UNKNOWN (CONTACT DELIBURD)>";
						}
						
						return "<" + channelTemplateIterator.next() + ">";
					});
					
					formattedTemplate = RecorderConstant.NAME_REPLACEMENT_PATTERN.matcher(formattedTemplate).replaceAll("@USER");
					
					channelInfoBuilder.append("\n")
							.appendCodeLine(formattedTemplate)
							.append("\n");
				}
			}
			
			channelInfoBuilder.append("\n");
		} else {
			channelInfoBuilder.appendCodeLine(":\nMalformed template (Contact DELIBURD)");
			ErrorLogger.LogIssue("Malformed template when someone tried listing them.");
		}
		
		return channelInfoBuilder.getStringBuilder().toString();
	}
	
	/**
	 * Runs when someone calls the command to remove a whitelisted channel
	 * 
	 * @param args The arguments given from the command
	 * @param event The MessageReceivedEvent associated when the command was run
	 * @param command The command's MultiCommand object
	 */
	private static void removeChannelFromWhitelist(String[] args, MessageReceivedEvent event, MultiCommand command) {
		String channelString = args[0];
		Long channelID = NumberUtil.stringToLong(channelString);
		long serverID = event.getGuild().getIdLong();
		MessageChannel channel = event.getChannel();
		
		if(channelID == null || event.getJDA().getTextChannelById(channelID) == null) {
			command.giveInvalidArgumentMessage(channel, "The channel ID is invalid. Make sure it's a real text channel ID.");
			return;
		}
		
		try {
			if (ServerConfig.getNode("templates", serverID, channelString) == null) {
				command.giveInvalidArgumentMessage(channel, "This channel is not whitelisted.");
				return;
			}

			ServerConfig.removeNode("templates", serverID, channelString);
			BotUtil.sendMessage(event.getChannel(), "Channel removed from the whitelist.");
		} catch (IOException e) {
			ErrorLogger.LogException(e, channel);
		}
	}

	/**
	 * Runs when someone calls the command to add a channel to the whitelist
	 * 
	 * @param args The arguments given from the command
	 * @param event The MessageReceivedEvent associated when the command was run
	 * @param command The command's MultiCommand object
	 */
	private static void addChannelToWhitelist(String[] args, MessageReceivedEvent event, MultiCommand command) {
		long userID = event.getMember().getIdLong();
		Long channelID = NumberUtil.stringToLong(args[0]);
		MessageChannel channel = event.getChannel();
		
		if(channelID == null || event.getJDA().getTextChannelById(channelID) == null) {
			command.giveInvalidArgumentMessage(channel, "The channel ID is invalid. Make sure it's a real text channel ID.");
			return;
		}

		int replacementCount;
		
		ArrayList<String> replacements;
		String replacementString;
		
		if(args.length == 1) {
			replacementString = null;
			replacementCount = 0;
			replacements = new ArrayList<String>(1);
		} else {
			replacementString = args[1];
			replacementCount = (int) RecorderConstant.REPLACEMENT_PATTERN.matcher(replacementString).results().count();
			replacements = new ArrayList<String>(replacementCount * 2 + 2);
		}
		
		boolean noReplacements = replacementCount == 0;
		
		String cancelComment = "If something isn't correct, type cancel and retype the command.";
		String templateMessage = "Please type the **question** you want to ask for the user to replace the %s replacement. "
				+ cancelComment;
		String nameMessage = "Please type the **name** you want to show for the user in the template for the %s replacement. "
				+ cancelComment;
		String firstTemplateMessage;
		
		Pair<String, ArrayList<String>> channelTemplates = new Pair<String, ArrayList<String>>(channelID.toString(), replacements);
		
		String channelDescriptionQuestion = "Please give a brief description of this channel.";

		var descriptionResponse = new MessageResponse(channelDescriptionQuestion, channel, userID, (e, r) -> templateCallback(e, r, channelTemplates, noReplacements), "")
				.setCancelResponses("cancel")
				.setTimeout(120, TimeUnit.SECONDS);
		
		if(noReplacements || replacementCount == 0) {
			descriptionResponse.build();
			MessageResponseQueue.getQueue().addMessageResponse(descriptionResponse);
			return;
		}
		
		if(args[1].isBlank()) {
			command.giveInvalidArgumentMessage(channel, "This template is invalid.");
			return;
		}
		
		String confirmationMessage = new StringBuilder("I detected ")
				.append(replacementCount)
				.append(" place(s) where you want the user to fill in text. ")
				.toString();
		firstTemplateMessage = confirmationMessage + "\n" + String.format(nameMessage, NumberUtil.toOrdinalNumber(1));
		
		boolean isOnlyCallback = replacementCount == 1;
		var templateResponses = new MessageResponse(firstTemplateMessage, channel, userID, (e, r) -> {
			replacements.add(replacementString);
			return templateCallback(e, r, channelTemplates, false);
		}, "")
				.setCancelResponses("cancel")
				.setTimeout(120, TimeUnit.SECONDS)
				.build();
		
		descriptionResponse.chainResponse(templateResponses);
		
		String firstTemplateBody = String.format(templateMessage, NumberUtil.toOrdinalNumber(1));
		var firstresponse = new MessageResponse(firstTemplateBody, channel, userID, (e, r) -> templateCallback(e, r, channelTemplates, isOnlyCallback), "")
				.setTimeout(120, TimeUnit.SECONDS)
				.setCancelResponses("cancel")
				.build();
		
		descriptionResponse.chainResponse(firstresponse);
		
		for(int i = 1; i < replacementCount; i++) {
			boolean isLast;
			
			if(replacementCount - 1 == i) {
				isLast = true;
			} else {
				isLast = false;
			}
			
			String currentNameMessage = String.format(nameMessage, NumberUtil.toOrdinalNumber(i + 1));
			var nameResponse = new MessageResponse(currentNameMessage, channel, userID, (e, r) -> templateCallback(e, r, channelTemplates, false), "")
					.setTimeout(120, TimeUnit.SECONDS)
					.setCancelResponses("cancel")
					.build();
			
			descriptionResponse.chainResponse(nameResponse);
			
			String currentTemplateMessage = String.format(templateMessage, NumberUtil.toOrdinalNumber(i + 1));
			var templateResponse = new MessageResponse(currentTemplateMessage, channel, userID, (e, r) -> templateCallback(e, r, channelTemplates, isLast), "")
					.setTimeout(120, TimeUnit.SECONDS)
					.setCancelResponses("cancel")
					.build();
			
			descriptionResponse.chainResponse(templateResponse);
		}
		
		descriptionResponse.build();
		
		MessageResponseQueue.getQueue().addMessageResponse(descriptionResponse);
	}
	
	/**
	 * Runs when a response is given to fill out a template
	 * 
	 * @param event The MessageReceivedEvent associated with the response given
	 * @param response The MessageResponse associated with this callback
	 * @param replacements A pair containing channel ID as the key and an ArrayList of strings containing questions and
	 * names of each replacement as the value
	 * @param isLast Whether this is the last callback that will be run in the chain
	 * @return Whether the template callback should continue chaining
	 */
	private static boolean templateCallback(MessageReceivedEvent event, MessageResponse response, Pair<String, ArrayList<String>> replacements, boolean isLast) {
		String message = event.getMessage().getContentDisplay();
		ArrayList<String> configList = replacements.getValue();
		
		if(message.isBlank()) {
			BotUtil.sendMessage(event.getChannel(), "This can't be blank");
			MessageResponseQueue.getQueue().addMessageResponse(response);
			return false;
		}
		
		configList.add(message);
		
		if(isLast) {
			String[] templateArray = configList.toArray(String[]::new);
			long serverID = event.getGuild().getIdLong();
			long userID = event.getAuthor().getIdLong();
			long whitelistChannelID = NumberUtil.stringToLong(replacements.getKey());
			completeTemplate(event, whitelistChannelID, serverID, userID, templateArray);
		}
		
		return true;
	}
	
	private static void completeTemplate(MessageReceivedEvent event, long channelID, long serverID, long userID, String[] templateArray) {
		boolean success = ServerConfig.writeToConfig("templates", serverID, templateArray, Long.toString(channelID));

		if(success) {
			BotUtil.sendMessage(event.getChannel(), "Template added.", userID);
		} else {
			BotUtil.sendMessage(event.getChannel(), Constant.ERROR_MESSAGE, userID);
		}
	}
	
	/**
	 * Runs when someone calls the command to start a recording
	 * 
	 * @param args The arguments given from the command
	 * @param event The MessageReceivedEvent associated when the command was run
	 * @param command The command's MultiCommand object
	 */
	private static void startRecord(String[] args, MessageReceivedEvent event, MultiCommand command) {
		AudioCompression compression;
		
		if(args != null && args[0] == "uncompressed") {
			compression = AudioCompression.UNCOMPRESSED;
		} else {
			compression = AudioCompression.COMPRESSED;
		}
		
		startRecord(new RecorderInfo(event.getTextChannel(), event.getMember(), compression));
	}
	
	/**
	 * Starts a recording in the voice channel the member is in.
	 * 
	 * @param info A RecorderInfo object holding metadata that will be used to start the recording
	 */
	private static void startRecord(RecorderInfo info) {
		AudioReceiverHandler handler = info.getHandler();
		TextChannel channel = info.getChannel();
		Member member = info.getMember();
		Guild server = member.getGuild();
		long userID = member.getUser().getIdLong();
		VoiceChannel voiceChannel = member.getVoiceState().getChannel();
		File userFile = handler.getMemberFile(userID);
		
		if(handler.isRecordingUser(userID)) {
			BotUtil.sendMessage(channel, "The bot is already recording you.");
			return;
		} else if(handler.isRecordingInServer(server.getIdLong())) {
			BotUtil.sendMessage(channel, "This bot is currently in use by someone else. Please wait.");
			return;
		} else if(voiceChannel == null) {
			BotUtil.sendMessage(channel, "Please join a voice channel to start recording.");
			return;
		} else if(!BotUtil.hasVoiceChannelPermission(voiceChannel, Permission.VOICE_CONNECT)) {
			BotUtil.sendMessage(channel, "I don't have the necessary permissions to join " + voiceChannel.getName());
			return;
		} else if(BotUtil.voiceChannelIsFull(voiceChannel) && !BotUtil.hasVoiceChannelPermission(voiceChannel, Permission.VOICE_MOVE_OTHERS)) {
			BotUtil.sendMessage(channel, voiceChannel.getName() + " is full");
			return;
		} else if(userFile != null) {
			String message = "It appears we still have an audio file of yours that you haven't sent yet. Would you like us to delete them"
					+ " so you can start another recording? This will delete your old audio file permanently. Say yes to delete it and start recording."
					+ " Say no to keep your existing record so you can send it to a channel first. I've already messaged you on how to send the existing file.";
			var responseCallback = new MessageResponse(message, channel, userID, (e, r) -> deleteAndRecord(e, info), "yes")
					.setCancelMessage("Keeping existing file...")
					.setCancelResponses("no")
					.build();
			
			MessageResponseQueue queue = MessageResponseQueue.getQueue();
			
			if(!queue.hasMessageResponse(responseCallback)) {
				queue.addMessageResponse(responseCallback);
			} else {
				queue.resetTimeout(responseCallback);
				BotUtil.sendMessage(channel, message);
			}

			return;
		}
		
		openChannelAndDMAndRecord(info);
	}
	
	/**
	 * Runs when a user wants to delete their old recording and start a new one
	 * 
	 * @param event The MessageReceivedEvent associated with the affirmative response
	 * @param info A RecorderInfo object containing data to start the recording after the old one is deleted
	 * @return Whether to continue the chain (Does nothing)
	 */
	private static boolean deleteAndRecord(MessageReceivedEvent event, RecorderInfo info) {
		TextChannel channel = event.getTextChannel();
		if(BotUtil.hasWritePermission(channel)) {
			Member member = event.getMember();
			AudioCompression compression = info.getCompression();
			RecorderInfo updatedInfo = new RecorderInfo(channel, member, compression);
			File mergedFile = info.getHandler().getMemberFile(member.getUser().getIdLong());
			
			if(mergedFile != null) {
				mergedFile.delete();
			}

			member.getUser().openPrivateChannel().queue(privateChannel -> {
				long userPrivateChannelID = privateChannel.getIdLong();
				MessageResponseQueue.getQueue().removeMessageResponses(member.getIdLong(), userPrivateChannelID);
				startRecord(updatedInfo);
			}, error -> {
				ErrorLogger.LogException(error, channel);
			});
		}
		
		return false;
	}

	/**
	 * Opens a DM with the user from the RecorderInfo and starts a recording for them
	 * 
	 * @param info The RecorderInfo containing data to start the recording.
	 */
	private static void openChannelAndDMAndRecord(RecorderInfo info) {
		info.getMember().getUser().openPrivateChannel().queue((channel) -> {
			startRecordAndDM(info);
		}, (error) -> {
			ErrorLogger.LogException(error, info.getChannel());
		});
	}

	/**
	 * Starts a recording and DMs the user to test that they can accept DMs
	 * 
	 * @param info A RecorderInfo object containing data to start the recording.
	 */
	private static void startRecordAndDM(RecorderInfo info) {
		AudioCompression compression = info.getCompression();
		Member member = info.getMember();
		Guild server = member.getGuild();
		VoiceChannel voiceChannel = member.getVoiceState().getChannel();
		TextChannel textChannel = info.getChannel();
		String compressionString;
		
		if(voiceChannel == null) {
			BotUtil.sendDM(member.getUser(), "It appears you left as soon as you started the recording. "
					+ "Your recording has been cancelled.");
			return;
		}
		
		if(compression == AudioCompression.COMPRESSED) {
			compressionString = "written as a compressed MP3 file.";
		} else {
			compressionString = "written as an uncompressed WAV file.";
		}
		
		var testDMBuilder = new MessageBuilder("Your audio will be ");
		testDMBuilder.append(compressionString)
				.append(" The maximum upload size taking into account the upload limit of ")
				.append(server.getName())
				.append(" will be about ")
				.append(BotUtil.getFileSizeLimit(server) / 1024)
				.append("KiB.");
		sendDMWithCallback(member.getUser(), testDMBuilder.build(), success -> {
			boolean successfulCreation = info.getHandler().startRecording(member, compression);
			
			if(!successfulCreation) {
				BotUtil.sendMessage(textChannel, Constant.ERROR_MESSAGE, member.getUser().getIdLong());
			}
			
			var startRecordMessage = new MessageBuilder("Starting to record ")
					.append(member.getAsMention())
					.append(" in ")
					.append(voiceChannel.getName())
					.append("... I've also sent you a DM containing data about your recording.");
			
			BotUtil.sendMessage(textChannel, startRecordMessage);
		}, error -> {
			if(BotUtil.isErrorResponseType(error, ErrorResponse.CANNOT_SEND_TO_USER)) {
				BotUtil.sendMessage(textChannel, "I couldn't send you a DM. Ensure that you can receive messages from server members that aren't friends.");
			} else {
				ErrorLogger.LogException(error, textChannel);
			}
		});
	}
	
	/**
	 * Called when the command to stop a recording is run.
	 * 
	 * @param event The MessageReceivedEvent associated with the command
	 */
	private static void stopRecord(MessageReceivedEvent event) {
		AudioReceiverHandler handler = AudioReceiverHandler.getHandler();
		
		if(handler.isRecordingInServer(event.getGuild().getIdLong())) {
			if(handler.isRecordingUser(event.getAuthor().getIdLong())) {
				handler.stopRecording(event.getMember().getIdLong(), event.getJDA());
				BotUtil.sendMessage(event.getChannel(), "Stopping recording...");
			} else {
				BotUtil.sendMessage(event.getChannel(), "You cannot stop a recording you don't own.");
			}
		} else {
			BotUtil.sendMessage(event.getChannel(), "A recording hasn't been started.");
		}
	}
	
	/**
	 * Sends a DM to a user allowing for a success and failure callback to be added
	 * 
	 * @param user The user to send the DM to
	 * @param message The message to send the user
	 * @param success The success callback
	 * @param failure The failure callback
	 */
	private static void sendDMWithCallback(User user, Message message, Consumer<Message> success, Consumer<Throwable> failure) {
		user.openPrivateChannel().queue(channel -> {
			channel.sendMessage(message).queue(success, failure);
		});
	}
	
	private static class RecorderInfo {
		private AudioReceiverHandler handler;
		private AudioCompression compression;
		private TextChannel channel;
		private Member member;
		
		public RecorderInfo(TextChannel channel, Member member, AudioCompression compression) {
			if(channel == null) {
				throw new IllegalArgumentException("Channel can't be null");
			} else if(member == null) {
				throw new IllegalArgumentException("Member can't be null");
			} else if(compression == null) {
				throw new IllegalArgumentException("Audio compression can't be null");
			}
			
			handler = AudioReceiverHandler.getHandler();
			this.channel = Objects.requireNonNull(channel, "The channel cannot be null.");
			this.member = Objects.requireNonNull(member, "The member cannot be null.");
			this.compression = Objects.requireNonNull(compression, "The compression cannot be null.");
		}

		/**
		 * Gets the AudioReceiverHandler instance associated with the object
		 * 
		 * @return The AudioReceiverHandler instance associated with the object
		 */
		public AudioReceiverHandler getHandler() {
			return handler;
		}

		/**
		 * Gets the text channel associated with the object
		 * 
		 * @return The text channel associated with the object
		 */
		public TextChannel getChannel() {
			return channel;
		}

		/**
		 * Gets the member associated with the object
		 * 
		 * @return The member associated with the object
		 */
		public Member getMember() {
			return member;
		}

		/**
		 * Gets the AudioCompression enum associated with the object
		 * 
		 * @return The AudioCompression enum associated with the object
		 */
		public AudioCompression getCompression() {
			return compression;
		}
	}
}
