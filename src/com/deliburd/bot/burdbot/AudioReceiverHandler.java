package com.deliburd.bot.burdbot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.FileUtil;
import com.deliburd.util.MessageResponse;
import com.deliburd.util.MessageResponseQueue;
import com.deliburd.util.NumberUtil;
import com.deliburd.util.ServerConfig;
import com.deliburd.recorder.RecorderConstant;
import com.deliburd.recorder.util.audio.AudioCompression;
import com.deliburd.recorder.util.audio.AudioWriter;
import com.fasterxml.jackson.databind.JsonNode;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.UserAudio;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * @author MelonShooter
 */
public class AudioReceiverHandler extends ListenerAdapter implements AudioReceiveHandler {
	/**
	 * The singleton instance of this class
	 */
	private final static AudioReceiverHandler audioReceiver = new AudioReceiverHandler();
	
	/**
	 * Links the user ID of the person using the bot to metadata on their recording.
	 */
	private final ConcurrentHashMap<Long, AudioServerInfo> userToAudioInfo;
	
	/**
	 * A Set backed by a ConcurrentHashMap containing server IDs
	 */
	private final Set<Long> serverRecordingList;
	
	/**
	 * A timer containing a task that runs periodically to delete merged files after 24 hours.
	 * It also runs a task periodically adding silence to files that have been inactive for too long.
	 */
	private final Timer fileUpdateTimer;
	
	/**
	 * The command that the user can type in DMs to start the file sending process
	 */
	private final String FILE_SEND_COMMAND = "sendfile";
	
	/**
	 * The command that the user can type in DMs to cancel the file sending process
	 */
	private final String CANCEL_SEND_COMMAND = "cancelsend";
	
	/**
	 * The message to send when the user's file has been deleted.
	 */
	private final String FILE_DELETED_MESSAGE = "Your file has been deleted permanently as it has been 24 hours.";
	
	private final String CANCEL_SEND_MESSAGE = "File sending cancelled... To send your file somewhere again, type ``" + FILE_SEND_COMMAND + "``.";
	private final String NO_CHANNEL_MESSAGE = "It appears there are no channels we can send this to. Contact DELIBURD if you think this is a mistake.";
	private final Pattern ID_PATTERN = Pattern.compile("^\\d+");
	
	private AudioReceiverHandler() {
		userToAudioInfo = new ConcurrentHashMap<Long, AudioServerInfo>();
		serverRecordingList = ConcurrentHashMap.newKeySet();
		fileUpdateTimer = new Timer(true);
		
		scheduleSilenceUpdates();
	}
	
	@Override
	public void onReady(ReadyEvent event) {
		emptyUserSplitFiles(event.getJDA());
		scheduleMergedFileDeletion(event.getJDA());

		var memberFiles = getAllMemberFiles();
		
		for(var memberFile : memberFiles) {
			String fileName = memberFile.getName();
			Matcher userIDMatcher = ID_PATTERN.matcher(memberFile.getName());
			
			if(userIDMatcher.find()) {
				Long possibleUserID = NumberUtil.stringToLong(fileName.substring(userIDMatcher.start(), userIDMatcher.end()));
				
				if(possibleUserID == null) {
					continue;
				}
				
				BotUtil.getUser(possibleUserID, event.getJDA(), user -> {
					if(user != null) {
						BotUtil.sendDM(user, "It appears that I've restarted. It appears I still have an audio file of yours "
								+ "stored. If you still want to send it somewhere, please type ``sendfile``.\nI apologize if this "
								+ "has caused you any inconvienience.");
					}
				});
			}
		}
	}

	/**
	 * Returns the singleton instance of this class
	 * 
	 * @return The singleton instance of this class
	 */
	public static AudioReceiverHandler getHandler() {
		return audioReceiver;
	}
	
	/**
	 * Returns whether the given user is being recorded
	 * 
	 * @param userID The user ID of the person
	 * @return Whether the given user is being recorded
	 */
	public boolean isRecordingUser(long userID) {
		return userToAudioInfo.containsKey(userID);
	}
	
	/**
	 * Returns whether the given server is currently being recorded in
	 * 
	 * @param serverID The server ID to check
	 * @return Whether the given server is currently being recorded in
	 */
	public boolean isRecordingInServer(long serverID) {
		return serverRecordingList.contains(serverID);
	}
	
	/**
	 * Gets the merged audio file given a server and a user
	 * 
	 * @param server The server to check for
	 * @param userID The user's ID to check
	 * @return The merged audio file. Null if it isn't found.
	 */
	public File getMemberFile(Guild server, long userID) {
		return getMemberFile(server.getIdLong(), Long.toString(userID));
	}
	
	/**
	 * Gets the merged audio file given a user
	 * 
	 * @param userID The user's ID
	 * @return The merged audio file. Null if it isn't found
	 */
	public File getMemberFile(long userID) {
		File[] serverFolders = new File(RecorderConstant.RECORDER_DIR).listFiles();
		
		if(serverFolders == null) {
			return null;
		}
		
		for(var serverFolder : serverFolders) {
			if(serverFolder.isDirectory()) {
				Long serverID = NumberUtil.stringToLong(serverFolder.getName());
				
				if(serverID == null) {
					continue;
				}
				
				File possibleMemberFile = getMemberFile(serverID, Long.toString(userID));
				
				if(possibleMemberFile != null) {
					return possibleMemberFile;
				}
			}
		}
		
		return null;
	}

	/**
	 * Gets the merged audio file given a server and a user
	 * 
	 * @param server The server to check for
	 * @param userID The user's ID to check
	 * @return The merged audio file. Null if it isn't found.
	 */
	private File getMemberFile(Guild server, String userID) {
		return getMemberFile(server.getIdLong(), userID);
	}
	
	private File getMemberFile(long serverID, String userID) {
		File[] memberFiles = new File(RecorderConstant.RECORDER_DIR_SEP + serverID).listFiles();

		if(memberFiles != null) {
			for(var memberFile : memberFiles) {
				if(isMemberFile(memberFile, userID)) {
					return memberFile;
				}
			}
		}
		
		return null;
	}
	
	
	/**
	 * Gets all of the merged audio files
	 * 
	 * @return All of the merged audio files.
	 */
	public ArrayList<File> getAllMemberFiles() {
		ArrayList<File> memberFileList = new ArrayList<File>();
		var serverDirectories = new File(RecorderConstant.RECORDER_DIR).listFiles();
		
		if(serverDirectories != null) {
			for(var serverDirectory : serverDirectories) {
				if(serverDirectory.isDirectory()) {
					File[] serverFiles = serverDirectory.listFiles();
					
					if(serverFiles == null) {
						continue;
					}
					
					for(var serverFile : serverFiles) {
						String fileName = serverFile.getName();
						
						if(fileName.endsWith(".mp3") || fileName.endsWith(".wav")) {
							memberFileList.add(serverFile);
						}
					}
				}
			}
		}
		
		return memberFileList;
	}
	
	private boolean isMemberFile(File possibleMemberFile, String userID) {
		if(possibleMemberFile == null || !possibleMemberFile.isFile()) {
			return false;
		}
		
		String fileName = possibleMemberFile.getName();
		
		return fileName.startsWith(userID + "-") && (fileName.endsWith(".mp3") || fileName.endsWith(".wav"));
	}
	
	/**
	 * Gets the time in the specified time unit from now in which the file for the given user and server will expire
	 * 
	 * @param server The server
	 * @param userID The user's ID
	 * @param unit The time unit to give the time in
	 * @return The time until expiration. 0 if the file has already expired.
	 */
	public long getFileExpirationTime(Guild server, String userID, TimeUnit unit) {
		File memberFile = getMemberFile(server, userID);
		
		if(memberFile == null) {
			return 0;
		}
		
		Instant expirationTime = Instant.ofEpochMilli(memberFile.lastModified() + RecorderConstant.AUDIO_FILE_DELETION_DELAY * 1000);
		
		Duration timeToExpiration = Duration.between(Instant.now(), expirationTime);
		
		if(timeToExpiration.isNegative()) {
			return 0;
		}
		
		return unit.convert(timeToExpiration);
	}

	/**
	 * Starts recording the given member
	 * 
	 * @param member The member to record
	 * @param compression Determines whether the audio written is compressed or not
	 * @return Whether the recording was started without an error in the creation of the audio file.
	 */
	public boolean startRecording(Member member, AudioCompression compression) {
		long memberID = member.getIdLong();
		VoiceChannel voiceChannel = member.getVoiceState().getChannel();
		Guild server = member.getGuild();
		long serverID = server.getIdLong();
		
		if(voiceChannel == null) {
			BotUtil.deleteLastDM(member.getUser());
			BotUtil.sendDM(member.getUser(), "It appears you left as soon as you started the recording. "
					+ "Your recording has been cancelled.");
			return true;
		}
		
		StringBuilder audioPath = new StringBuilder(RecorderConstant.RECORDER_DIR_SEP);
		audioPath.append(serverID);

		File audioFolder = new File(audioPath.toString());
		if(!audioFolder.exists()) {
			audioFolder.mkdirs();
		}

		audioPath.append(File.separator);
		audioPath.append(memberID);
		
		File audioSubfolder = new File(audioPath.toString());
		
		AudioWriter audioFileWriter;
		
		try {
			audioFileWriter = new AudioWriter(audioFolder, audioSubfolder, member.getId(), 
					BotUtil.getFileSizeLimit(), BotUtil.getFileSizeLimit(server), compression, OUTPUT_FORMAT);
		} catch (FileNotFoundException e) {
			ErrorLogger.LogException(e);
			audioSubfolder.delete();
			audioFolder.delete();
			return false;
		}
		
		AudioManager audioManager = server.getAudioManager();

		if(audioManager.getReceivingHandler() == null) {
			audioManager.setReceivingHandler(audioReceiver);
		}
		
		audioManager.openAudioConnection(voiceChannel);
		addData(memberID, serverID, audioFileWriter, member.getJDA());

		return true;
	}
	
	public void stopRecording(long userID, JDA JDA) {
		BotUtil.getUser(userID, JDA, (user) -> {
			Guild guild = userToAudioInfo.get(userID).getServer();
			
			if(guild != null && user != null) {
				stopRecording(user, guild);	
			} else { // This should never ever happen, but if it does, we don't want memory leaks.
				cleanupRecordings(userID, guild.getIdLong());
			}
		});
	}
	
	private void scheduleSilenceUpdates() {
		fileUpdateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				addSilence();
			}
		}, RecorderConstant.AUDIO_FILE_SILENCE_CHECK_DELAY, RecorderConstant.AUDIO_FILE_SILENCE_CHECK_DELAY);
	}
	
	private void emptyUserSplitFiles(JDA JDA) {
		File[] serverDirectories = new File(RecorderConstant.RECORDER_DIR).listFiles();
		
		if(serverDirectories != null) {
			for(var serverDirectory : serverDirectories) {
				var userFolderArray = serverDirectory.listFiles();
				
				if(userFolderArray == null) {
					continue;
				}
				
				for(var userFolder : userFolderArray) {
					String fileName = userFolder.getName();
					Matcher userIDMatcher = ID_PATTERN.matcher(fileName);
					if(userIDMatcher.find()) {
						Long userID = NumberUtil.stringToLong(fileName.substring(userIDMatcher.start(), userIDMatcher.end()));
						BotUtil.getUser(userID, JDA, user -> FileUtil.deleteFolder(userFolder));
					}
				}
			}
		}
	}
	
	private void cleanupRecordings(long userID, long serverID) {
		AudioServerInfo serverInfo = userToAudioInfo.get(userID);
		
		if(serverInfo == null) {
			return;
		}
		
		serverInfo.getLock().lock();
		
		AudioWriter userFile;
		
		try {
			if(!userToAudioInfo.containsKey(userID)) {
				return;
			}

			userFile = userToAudioInfo.get(userID).getAudioWriter();
			removeData(userID, serverID);
		} finally {
			serverInfo.getLock().unlock();
		}
	
		if (!userFile.isFinalized()) {
			userFile.finalizeFile();
		}

		deleteAllUserFiles(userFile.getSeparateFiles(), userFile.getFile());
	}

	private void stopRecording(User user, Guild server) {
		long userID = user.getIdLong();
		long serverID = server.getIdLong();
		AudioServerInfo serverInfo = userToAudioInfo.get(userID);
		
		if(serverInfo == null) {
			return;
		} else {
			serverInfo.getLock().lock();
		}
		
		long currentTime;
		AudioWriter userFile;
		long lastAudioGap;
		
		try {
			if (!userToAudioInfo.containsKey(userID)) {
				return;
			}
	
			currentTime = Instant.now().toEpochMilli();
			userFile = userToAudioInfo.get(userID).getAudioWriter();
			lastAudioGap = currentTime - userFile.getLastWriteTime();
	
			removeData(userID, serverID);
		} finally {
			serverInfo.getLock().unlock();
		}

		boolean gotTruncated = userFile.isFinalized();
		byte[] leftoverBytes = null;

		if (!userFile.isFinalized()) {
			try {
				userFile.writeSilence(lastAudioGap);
			} catch (IOException e) {
				ErrorLogger.LogException(e);
			}

			Instant creationTime = Instant.ofEpochMilli(userFile.getCreationTime());
			leftoverBytes = userFile.finalizeFile();

			if (Duration.between(creationTime, Instant.now()).abs().toSeconds() < 1) {
				deleteAllUserFiles(userFile.getSeparateFiles(), userFile.getFile());
				BotUtil.sendDM(user,"Your recording was less than a second. " 
						+ "Because of this, your recording was not sent. "
						+ "Please record for a longer period of time.");

				AudioManager audioManager = server.getAudioManager();
				audioManager.closeAudioConnection();

				return;
			}
		}

		File audioFile = userFile.getFile();
		File[] separateAudioFiles = userFile.getSeparateFiles();
		String message;

		if (separateAudioFiles.length == 1) {
			message = "Here is your audio file.";
		} else {
			message = "Here are your audio files. They have been split into approximately 8 MiB chunks, but will be "
					+ "one complete audio file if it is uploaded to a channel in the server.";
		}

		if (gotTruncated || leftoverBytes != null) {
			String truncatedMessage = "Your recording was truncated to fit the file size limit of the server. ";
			message = truncatedMessage + message;
		}

		final String finalMessage = message;

		user.openPrivateChannel().queue((channel) -> {
			RestAction<Message> messageAction = channel.sendMessage(finalMessage).addFile(separateAudioFiles[0]);

			for (int i = 1; i < separateAudioFiles.length; i++) {
				final int fileIndex = i;

				messageAction = messageAction.flatMap((msg) -> channel.sendFile(separateAudioFiles[fileIndex]));
			}

			messageAction.queue(msg -> deleteAndAddListener(separateAudioFiles, userID, channel, server), error -> {
				ErrorLogger.LogException(error, channel);
				deleteAllUserFiles(separateAudioFiles, audioFile);
			});
		}, (error) -> {
			ErrorLogger.LogException(error);
			deleteAllUserFiles(separateAudioFiles, audioFile);
		});
		
		AudioManager audioManager = server.getAudioManager();
		audioManager.closeAudioConnection();
	}
	
	private void deleteAndAddListener(File[] separateAudioFiles, long userID, PrivateChannel channel, Guild server) {
		deleteSplitFiles(separateAudioFiles);
		addListener(userID, channel, server);
	}
	
	private void addListener(long userID, PrivateChannel channel, Guild server) {
		JsonNode templateNode;
		try {
			templateNode = ServerConfig.getTableData("templates", server.getIdLong());
		} catch (IOException e) {
			ErrorLogger.LogException(e, channel);
			return;
		}
		
		if(templateNode == null) {
			String noChannelMessage = "It appears there are no channels we can send this to. Contact DELIBURD if you think this is a mistake.";
			BotUtil.sendMessage(channel, noChannelMessage, userID);
			return;
		}
		
		File memberFile = getMemberFile(server, Long.toString(userID));
		
		if(memberFile == null) {
			BotUtil.sendMessage(channel, "It appears I can't find the full audio file. Please contact DELIBURD. "
					+ "Because of this, I won't be able to send your recording anywhere.");
			return;
		}
		
		Consumer<Member> memberCallback = member -> addListenerWithMember(userID, channel, templateNode, member);
		server.retrieveMemberById(userID).queue(memberCallback, error -> {
			BotUtil.sendMessage(channel, "I couldn't find you in the server anymore. If you believe this is a mistake. "
					+ "Please contact DELIBURD.");
		});
	}

	private void addListenerWithMember(long userID, PrivateChannel channel, JsonNode templateNode, Member member) {
		Guild server = member.getGuild();
		LinkedHashMap<Long, String> serverAndChannelNames = new LinkedHashMap<Long, String>(templateNode.size() + 1);
		var channels = templateNode.fields();
		
		serverAndChannelNames.put(server.getIdLong(), null);
		
		StringBuilder initialMessage = new StringBuilder("``What channel would you like the full audio to be sent to?``\n"
				+ "If you would not like to send your audio file anywhere, type ``" + CANCEL_SEND_COMMAND + "``.\nYour file "
				+ "will be kept for a total of 24 hours or until they're sent and then will be permanently deleted afterwards.\n"
				+ "The whitelisted channel(s) is/are:\n");
		
		while(channels.hasNext()) {
			var currentChannelEntry = channels.next();
			String channelIDString = currentChannelEntry.getKey();
			Long channelID = NumberUtil.stringToLong(channelIDString);
			
			if(channelID == null) {
				continue;
			} else {
				TextChannel validChannel = server.getJDA().getTextChannelById(channelID);
				
				if(!isValidWhitelistedChannel(member, validChannel)) {
					continue;
				}
				
				JsonNode templateArray = currentChannelEntry.getValue();
				
				if(!templateArray.isArray() || templateArray.isEmpty() || !templateArray.get(0).isTextual()) {
					continue;
				}
				
				String validChannelName = validChannel.getName();

				if (validChannelName == null) {
					continue;
				}

				initialMessage.append(validChannel.getAsMention())
						.append(" - ``")
						.append(templateArray.get(0).asText())
						.append("``\n");
				serverAndChannelNames.put(validChannel.getIdLong(), validChannelName);
			}
		}
		
		if(serverAndChannelNames.size() == 1) {
			BotUtil.sendMessage(channel, NO_CHANNEL_MESSAGE);
			return;
		}
		
		long expirationTime = getFileExpirationTime(server, Long.toString(userID), TimeUnit.MILLISECONDS);
		
		if(expirationTime == 0) {
			BotUtil.sendMessage(channel, Constant.ERROR_MESSAGE);
			return;
		}
		
		BiFunction<MessageReceivedEvent, MessageResponse, Boolean> listenerSuccessCallback = (event, response) -> {
			onAudioResponse(event, response, Collections.unmodifiableMap(serverAndChannelNames), member);
			return false;
		};

		MessageResponse listener = new MessageResponse(initialMessage.toString(), channel, userID, listenerSuccessCallback, "")
				.setTimeout(expirationTime, TimeUnit.MILLISECONDS)
				.setTimeoutMessage(FILE_DELETED_MESSAGE)
				.setCancelResponses(CANCEL_SEND_COMMAND)
				.setCancelMessage(CANCEL_SEND_MESSAGE)
				.setCancelCallback((event, selfResponse) -> onFileSendCancel(event, selfResponse, server.getIdLong()));
		listener.build();
		
		MessageResponseQueue.getQueue().addMessageResponse(listener);
	}
	
	private void onAudioResponse(MessageReceivedEvent event, MessageResponse selfResponse, Map<Long, String> serverAndChannels, Member member) {
		PrivateChannel channel = event.getPrivateChannel();
		var serverAndChannelIDIterator = serverAndChannels.entrySet().iterator();
		long serverID = serverAndChannelIDIterator.next().getKey();
		Guild server = selfResponse.getJDA().getGuildById(serverID);
		var channels = new HashMap<Long, String>(serverAndChannels.size() - 1);
		
		serverAndChannelIDIterator.forEachRemaining(entry -> channels.put(entry.getKey(), entry.getValue()));
		
		if(server == null) {
			BotUtil.sendMessage(channel, "I can't find the server to send it to anymore. If you believe this to be an error, "
					+ "please contact DELIBURD.");
			return;
		}
		
		onMemberFoundAudioResponse(event, selfResponse, member, Collections.unmodifiableMap(channels));
	}

	private boolean onMemberFoundAudioResponse(MessageReceivedEvent event, MessageResponse selfResponse, Member member, Map<Long, String> channelIDs) {
		Guild server = member.getGuild();
		var channelIDIterator = channelIDs.entrySet().iterator();
		PrivateChannel channel = event.getPrivateChannel();
		String message = event.getMessage().getContentDisplay();
		ArrayList<TextChannel> channelList = new ArrayList<TextChannel>(2);
		ArrayList<String> nonexistentChannels = new ArrayList<String>(1);
		
		while(channelIDIterator.hasNext()) {
			var channelEntry = channelIDIterator.next();
			TextChannel textChannel = server.getTextChannelById(channelEntry.getKey());
			String textChannelName = "#" + channelEntry.getValue();
			
			if(textChannelName.contains(message)) {
				if(!isValidWhitelistedChannel(member, textChannel)) {
					nonexistentChannels.add(textChannel.getAsMention());
					channelIDIterator.remove();
				} else {
					channelList.add(textChannel);
				}
			}
		}
		
		if(!nonexistentChannels.isEmpty()) {
			String modifiedMessage = "It appears there is/are channel(s) that have been removed that would've affected your request. ";
			recreateChannelListener(selfResponse, channel, modifiedMessage, server);
			return false;
		}

		if(channelList.size() > 1) {
			StringBuilder channelConflictMessage = new StringBuilder("It appears more than 1 channel matches what you've typed.\n"
					+ "The channel names that match are: ");
			
			for(int i = 0; i < channelList.size(); i++) {
				channelConflictMessage.append(channelList.get(i).getName());

				if(i != channelList.size() - 1) {
					channelConflictMessage.append(", ");
				}
			}
			
			BotUtil.sendMessage(channel, channelConflictMessage);
			MessageResponseQueue.getQueue().addMessageResponse(selfResponse);
		} else if(channelList.size() == 1) {
			addAudioSendQuestions(selfResponse, event, channelList.get(0), member);
		} else {
			BotUtil.sendMessage(channel, "No channel matched any of the options. ");
			MessageResponseQueue.getQueue().addMessageResponse(selfResponse);
		}
		
		return true;
	}

	private void addAudioSendQuestions(MessageResponse selfResponse, MessageReceivedEvent event, TextChannel foundChannel, Member member) {
		Guild server = member.getGuild();
		long serverID = server.getIdLong();
		long channelID = foundChannel.getIdLong();
		PrivateChannel channel = event.getPrivateChannel();
		User user = event.getAuthor();
		String[] templateArray;
		
		try {
			templateArray = ServerConfig.getServerConfigNodeValueAsArray("templates", serverID, foundChannel.getId());
		} catch (IOException e) {
			ErrorLogger.LogException(e, channel);
			return;
		}

		if(templateArray == null || templateArray.length == 0) {
			String deWhitelistedMessage = "It appears this channel is no longer whitelisted.";
			recreateChannelListener(selfResponse, channel, deWhitelistedMessage, server);
			return;
		}
		
		long userID = user.getIdLong();

		StringBuilder selectedChannelText = new StringBuilder("Selected ")
				.append(foundChannel.getAsMention())
				.append(". ");
		
		if(templateArray.length == 1) {
			addNoTemplateResponse(serverID, channelID, channel, userID, selectedChannelText);
		} else {
			long expirationTime = getFileExpirationTime(server, user.getId(), TimeUnit.MILLISECONDS);

			if(expirationTime == 0) {
				String expirationMessage = "It appears your file has been deleted. If you believe this is a mistake. Please contact DELIBURD.";
				BotUtil.sendMessage(channel, expirationMessage);
				return;
			}
			
			selectedChannelText.append("The template for this channel is:```")
					.append(templateArray[1])
					.append("```Type ``continue`` to use this template.\n"
							+ "If you would not like to use this template and want to make your own message, type ``notemplate``.\n"
							+ "You can also type ``cancel`` to not send your file.\nDuring any point as "
							+ "you're filling out the template, you can type ``canceltemplate`` to come back to this point.\n"
							+ "If you want to literally type canceltemplate as a response to a question, you can put a backslash "
							+ "in front of it like so, ``\\canceltemplate``.");
			Matcher templateReplacementMatcher = RecorderConstant.REPLACEMENT_PATTERN.matcher(selectedChannelText);
			int templateLength = (templateArray.length - 1) / 2;
			
			if(templateReplacementMatcher.results().count() != templateLength) {
				BotUtil.sendMessage(channel, Constant.ERROR_MESSAGE);
				return;
			}
			
			ArrayList<String> names = new ArrayList<String>(templateLength);
			ArrayList<String> questions = new ArrayList<String>(templateLength);
			
			// Separates the names from the questions and puts the values into different arrays
			
			for(int i = 2; i < templateArray.length; i++) {
				if(i % 2 == 0) {
					names.add(templateArray[i]);
				} else {
					questions.add(templateArray[i]);
				}
			}
			
			var nameIterator = names.iterator();
			
			// Replace the %s with alligator brackets and the name
			String displayTemplate = templateReplacementMatcher.replaceAll(match -> {
				return "<" + nameIterator.next().toUpperCase() + ">";
			});
			Matcher nameReplacer = RecorderConstant.NAME_REPLACEMENT_PATTERN.matcher(displayTemplate);
			String fullTemplate = nameReplacer.replaceAll("@" + user.getName());
			String templateToFill = nameReplacer.reset(templateArray[1]).replaceAll(user.getAsMention());
			MessageBuilder templateBuilder = new MessageBuilder();
			
			BiFunction<MessageReceivedEvent, MessageResponse, Boolean> templateSucessCallback = (e, r) -> {
				Guild serverByID = r.getJDA().getGuildById(serverID);
				if(serverByID == null) {
					BotUtil.sendMessage(channel, "This server doesn't appear to exist anymore. If you believe this is a mistake. "
							+ "Please contact DELIBURD.");
					return false;
				} else if(serverByID.getTextChannelById(channelID) == null) {
					BotUtil.sendMessage(channel, "This channel doesn't appear to exist anymore. If you believe this is a mistake. "
							+ "Please contact DELIBURD.");
					return false;
				}
				
				templateBuilder.setContent(templateToFill);
				return true;
			};
			
			MessageResponse templateGuide = new MessageResponse(fullTemplate, channel, userID, templateSucessCallback, "continue")
					.setTimeout(expirationTime, TimeUnit.MILLISECONDS)
					.setTimeoutMessage(FILE_DELETED_MESSAGE)
					.setCancelResponses("notemplate", "cancel")
					.setCancelMessage(CANCEL_SEND_MESSAGE)
					.setCancelCallback((e, response) -> onTemplateCancel(e, response, serverID, foundChannel.getIdLong()));
			
			// adds the template questions
			var questionIterator = questions.iterator();
			while(questionIterator.hasNext()) {
				String question = questionIterator.next();
				boolean isLast = !questionIterator.hasNext();
				
				BiFunction<MessageReceivedEvent, MessageResponse, Boolean> cancelCallback = (e, r) -> {
					MessageResponseQueue.getQueue().addMessageResponse(templateGuide);
					return false;
				};
				
				BiFunction<MessageReceivedEvent, MessageResponse, Boolean> responseCallback = (e, r) -> {
					Member effectiveMember;
					
					if(isLast) {
						try {
							effectiveMember = server.retrieveMember(user).submit().get(2, TimeUnit.SECONDS);
						} catch (InterruptedException | TimeoutException exception) {
							effectiveMember = member;
						} catch(ExecutionException exception2) {
							BotUtil.sendMessage(channel, "I couldn't find you in the server anymore. If you believe this is a mistake. "
									+ "Please contact DELIBURD.");
							return false;
						}
					} else {
						effectiveMember = member;
					}
					
					return onTemplateAddition(e, r, templateBuilder, isLast, channelID, effectiveMember);
				};
				
				MessageResponse templateQuestion = new MessageResponse(question, channel, userID, responseCallback, "")
						.setTimeout(expirationTime, TimeUnit.MILLISECONDS)
						.setTimeoutMessage(FILE_DELETED_MESSAGE)
						.setCancelResponses("canceltemplate")
						.setCancelCallback(cancelCallback)
						.build();
				
				templateGuide.chainResponse(templateQuestion);
			}
			
			MessageResponseQueue.getQueue().addMessageResponse(templateGuide.build());
		}
	}

	private void addNoTemplateResponse(long serverID, long channelID, MessageChannel channel, long userID, StringBuilder selectedChannelText) {
		Guild server = channel.getJDA().getGuildById(serverID);
		long expirationTime = getFileExpirationTime(server, Long.toString(userID), TimeUnit.MILLISECONDS);

		if(expirationTime == 0) {
			String expirationMessage = "It appears your file has been deleted. If you believe this is a mistake. Please contact DELIBURD.";
			BotUtil.sendMessage(channel, expirationMessage);
			return;
		}
		
		String noTemplateMessage = selectedChannelText.append("Please type the message you want to send with the audio file.\n"
				+ "If you've changed your mind and don't want to send your file anywhere anymore, type cancel.").toString();
		BiFunction<MessageReceivedEvent, MessageResponse, Boolean> noTemplateSuccessCallback = (e, response) -> {
			MessageBuilder template = new MessageBuilder("%s");
			Consumer<Member> memberCallback = member -> onTemplateAddition(e, response, template, true, channelID, member);
			
			server.retrieveMemberById(userID).queue(memberCallback, error -> {
				BotUtil.sendMessage(channel, "I couldn't find you in the server anymore. If you believe this is a mistake. "
						+ "Please contact DELIBURD.");
			});
			
			return false;
		};
		
		MessageResponse noTemplateResponse = new MessageResponse(noTemplateMessage, channel, userID, noTemplateSuccessCallback, "")
				.setTimeout(expirationTime, TimeUnit.MILLISECONDS)
				.setTimeoutMessage(FILE_DELETED_MESSAGE)
				.setCancelResponses("cancel")
				.setCancelMessage(CANCEL_SEND_MESSAGE)
				.setCancelCallback((e, response) -> onFileSendCancel(e, response, serverID))
				.build();
		MessageResponseQueue.getQueue().addMessageResponse(noTemplateResponse);
	}
	
	private boolean onTemplateAddition(MessageReceivedEvent event, MessageResponse response, MessageBuilder template, boolean isLast, long channelID, Member member) {
		PrivateChannel userChannel = event.getPrivateChannel();
		Guild server = member.getGuild();

		TextChannel textChannel = response.getJDA().getTextChannelById(channelID);
		
		if(!isValidWhitelistedChannel(member, textChannel)) {
			BotUtil.sendMessage(userChannel, "I can't find this channel anymore. Check that the channel still exists and "
					+ "that I have permission to write in there. Restarting channel selection process...");
			addListener(event.getAuthor().getIdLong(), userChannel, server);
			return false;
		}
		
		Message message = event.getMessage();

		if(BotUtil.containsPing(message.getContentRaw())) {
			BotUtil.sendMessage(event.getChannel(), "This cannot contain any pings.");
			MessageResponseQueue.getQueue().addMessageResponse(response);
			return false;
		}

		Matcher templateMatcher = RecorderConstant.REPLACEMENT_PATTERN.matcher(template.getStringBuilder());
		String messageString = message.getContentRaw();
		
		if(messageString.equals("\\canceltemplate")) {
			messageString = "canceltemplate";
		}

		if(templateMatcher.find()) {
			template.getStringBuilder().replace(templateMatcher.start(), templateMatcher.end(), messageString);
		} else {
			ErrorLogger.LogIssue("The template doesn't contain enough %s matches", userChannel);
			return false;
		}
		
		if(template.length() > Message.MAX_CONTENT_LENGTH) {
			BotUtil.sendMessage(userChannel, "It appears that you've gone over the 2000 character limit imposed by Discord "
					+ "for a message. Restarting template...");
			response.getCancelCallback().apply(event, response); // Run the cancel callback to take them back to the start
			return false;
		}
		
		if(isLast) {
			File memberFile = getMemberFile(server, event.getAuthor().getId());
			
			if(memberFile == null) {
				ErrorLogger.LogIssue("The member file was null when someone attempted to send it.", userChannel);
				return false;
			}
			
			Function<Message, RestAction<Message>> sentSuccess = msg -> {
				memberFile.delete();
				return userChannel.sendMessage("Audio file sent.");
			};
			
			textChannel.sendMessage(template.build()).addFile(memberFile).flatMap(sentSuccess).queue(null, e -> {
				memberFile.delete();
				ErrorLogger.LogException(e, userChannel);
			});
		}
		
		return true;
	}
	
	private boolean onTemplateCancel(MessageReceivedEvent event, MessageResponse selfResponse, long serverID, long textChannelID) {
		long userID = event.getAuthor().getIdLong();
		Guild server = selfResponse.getJDA().getGuildById(serverID);
		MessageChannel userChannel = event.getChannel();
		
		if(server == null) {
			BotUtil.sendMessage(userChannel, "I can't find this server anymore. If you believe this is a mistake. "
						+ "Please contact DELIBURD.");
			return false;
		}
		
		long expirationTime = getFileExpirationTime(server, Long.toString(userID), TimeUnit.MILLISECONDS);

		if(expirationTime == 0) {
			String expirationMessage = "It appears your file has been deleted. If you believe this is a mistake. Please contact DELIBURD.";
			BotUtil.sendMessage(userChannel, expirationMessage);
			return false;
		}
		
		String message = event.getMessage().getContentDisplay();
		if(message.equals("notemplate")) {
			addNoTemplateResponse(serverID, textChannelID, event.getChannel(), userID, new StringBuilder());
			return false;
		} else {
			return onFileSendCancel(event, selfResponse, serverID);
		}
	}

	private void recreateChannelListener(MessageResponse selfResponse, PrivateChannel channel, String initialString, Guild server) {
		String modifiedMessage = initialString + "Please reselect a channel. ";
		BotUtil.sendMessage(channel, modifiedMessage);
		addListener(selfResponse.getUserID(), channel, server);
	}
	
	private boolean onFileSendCancel(MessageReceivedEvent event, MessageResponse selfResponse, long serverID) {
		Guild server = selfResponse.getJDA().getGuildById(serverID);
		long userID = event.getAuthor().getIdLong();
		
		if(server == null) {
			BotUtil.sendDM(event.getAuthor(), "File sending cancelled... However, it appears I can't find the server "
					+ "you recorded your audio originally. If this is a mistake, please contact DELIBURD. "
					+ "Because of this, I won't be able to send your recording anywhere if you change your mind.");
			return false;
		}
		
		long expirationTime = getFileExpirationTime(server, event.getAuthor().getId(), TimeUnit.MILLISECONDS);
		
		if(expirationTime == 0) {
			BotUtil.sendDM(event.getAuthor(), "File sending cancelled... However, it appears I can't find the full audio file. "
					+ "Please contact DELIBURD. Because of this, I won't be able to send your recording anywhere if you "
					+ "change your mind.");
			return false;
		}
		
		BiFunction<MessageReceivedEvent, MessageResponse, Boolean> redoCallback = (redoEvent, r) -> {
			addListener(userID, redoEvent.getPrivateChannel(), server);
			return true;
		};
		
		MessageResponse redoSendProcess = new MessageResponse("", event.getChannel(), userID, redoCallback,FILE_SEND_COMMAND)
				.setTimeout(expirationTime, TimeUnit.MILLISECONDS)
				.setCancelMessage(FILE_DELETED_MESSAGE)
				.build();
		MessageResponseQueue.getQueue().addMessageResponse(redoSendProcess);
		
		return true;
	}
	
	private boolean isValidWhitelistedChannel(Member member, TextChannel channel) {
		return channel != null && member.hasPermission(channel, Permission.MESSAGE_WRITE) && BotUtil.hasWritePermission(channel);
	}
	
	private void addData(long userID, long serverID, AudioWriter audioFileWriter, JDA JDA) {
		userToAudioInfo.put(userID, new AudioServerInfo(audioFileWriter, serverID, JDA));
		serverRecordingList.add(serverID);
	}
	
	private void removeData(long userID, long serverID) {
		serverRecordingList.remove(serverID);
		userToAudioInfo.remove(userID);
	}
	
	private void deleteAllUserFiles(File[] separateAudioFiles, File mergedFile) {
		deleteSplitFiles(separateAudioFiles);
		mergedFile.delete();
	}

	private void deleteSplitFiles(File[] separateAudioFiles) {
		if(separateAudioFiles == null) {
			return;
		}
		
		File parentDirectory = separateAudioFiles[0].getParentFile();
		
		for(var file : separateAudioFiles) {
			file.delete();
		}
		
		parentDirectory.delete();
	}
	
	private void scheduleMergedFileDeletion(JDA JDA) {
		fileUpdateTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				var serverDirectories = new File(RecorderConstant.RECORDER_DIR).listFiles();
				
				if(serverDirectories != null) {
					for(var serverDirectory : new File(RecorderConstant.RECORDER_DIR).listFiles()) {
						String serverDirectoryName = serverDirectory.getName();
						if(!serverDirectory.isDirectory() || !ID_PATTERN.matcher(serverDirectoryName).find()) {
							continue;
						}
						
						long serverID = NumberUtil.stringToLong(serverDirectoryName);
						
						if(JDA.getGuildById(serverID) == null) {
							continue;
						}
						
						FileUtil.deleteFiles(serverDirectory, audioReceiver::delete24HourConditionAndIsUserFile);
					}
				}
			}
		}, RecorderConstant.AUDIO_FILE_DELETION_CHECK_DELAY, RecorderConstant.AUDIO_FILE_DELETION_CHECK_DELAY);
	}
	
	private boolean delete24HourConditionAndIsUserFile(File file) {
		Instant lastModified = Instant.ofEpochMilli(file.lastModified());
		long timeDifference = Duration.between(lastModified, Instant.now()).abs().getSeconds();
		String fileName = file.getName();
		
		if(!ID_PATTERN.matcher(fileName).find()) {
			return false;
		}
		
		if(lastModified.toEpochMilli() != 0 && timeDifference > RecorderConstant.AUDIO_FILE_DELETION_DELAY) {
			return true;
		}
		
		return false;
	}
	
	private void addSilence() {
		var infoMap = userToAudioInfo.entrySet().iterator();
		
		while(infoMap.hasNext()) {
			var infoEntry = infoMap.next();
			AudioServerInfo serverInfo = infoEntry.getValue();
			
			AudioWriter writer = infoEntry.getValue().getAudioWriter();

			if (writer.isFinalized()) {
				return;
			}

			long lastAudioGap = Instant.now().toEpochMilli() - writer.getLastWriteTime();

			if (lastAudioGap > RecorderConstant.AUDIO_FILE_SILENCE_CHECK_DELAY) {
				if(!serverInfo.getLock().tryLock()) {
					return;
				}
				
				if (writer.isFinalized()) {
					serverInfo.getLock().unlock();
					return;
				}
				
				try {
					writer.writeSilence(lastAudioGap);
				} catch (IOException e) {
					ErrorLogger.LogException(e);
				} finally {
					serverInfo.getLock().unlock();
				}

				if (writer.isFinalized()) {
					File separatedFile = writer.getSeparateFiles()[0];
					long userID = Long.parseLong(separatedFile.getParentFile().getName());
					stopRecording(userID, userToAudioInfo.get(userID).getJDA());
					return;
				}
			}
		}
	}

	@Override
	public void handleUserAudio(UserAudio audio) {
		long userID = audio.getUser().getIdLong();
		var serverInfo = userToAudioInfo.get(userID);
		
		if(serverInfo == null || !serverInfo.getLock().tryLock()) {
			return;
		}

		try {
			AudioWriter userFile = serverInfo.getAudioWriter();
	
			if (userFile.isFinalized()) {
				return;
			}
	
			long lastAudioGap = Instant.now().toEpochMilli() - userFile.getLastWriteTime() - 20;
	
			if (lastAudioGap > 0) {
				try {
					userFile.writeSilence(lastAudioGap);
				} catch (IOException e) {
					ErrorLogger.LogException(e);
				}
			}
	
			if (userFile.isFinalized()) {
				stopRecording(userID, audio.getUser().getJDA());
				return;
			}
	
			try {
				userFile.writePCMAudio(audio.getAudioData(1));
			} catch (IOException e) {
				ErrorLogger.LogException(e);
			}
	
			if (userFile.isFinalized()) {
				stopRecording(userID, audio.getUser().getJDA());
				return;
			}
		} finally {
			serverInfo.getLock().unlock();
		}

	}
	
	@Override
	public boolean canReceiveUser() {
		return true;
	}
	
	@Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
		Member member = event.getEntity();
		if(userToAudioInfo.containsKey(member.getIdLong())) {
			stopRecording(member.getUser(), member.getGuild());
		} else if(member.getGuild().getSelfMember().equals(member)) { //BurdBot got disconnected
			VoiceChannel channelLeft = event.getChannelLeft();
					
			if(channelLeft == null) {
				return;
			}
			
			for(Member channelMember : channelLeft.getMembers()) {
				if(userToAudioInfo.containsKey(channelMember.getIdLong())) {
					stopRecording(channelMember.getUser(), channelMember.getGuild());
				}
			}
		}
	}
	
	/**
	 * Tell the user we don't have their file if they try and type sendfile
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		boolean isDM = event.isFromType(ChannelType.PRIVATE);
		String message = event.getMessage().getContentDisplay();
		MessageResponseQueue queue = MessageResponseQueue.getQueue();
		
		if(!event.getAuthor().isBot() && isDM && message.equals(FILE_SEND_COMMAND)) {
			long userID = event.getAuthor().getIdLong();

			if(!queue.hasMessageResponse(FILE_SEND_COMMAND, userID) && !queue.hasMessageResponse(CANCEL_SEND_COMMAND, userID)) {
				File memberFile = getMemberFile(userID);
				PrivateChannel privateChannel = event.getPrivateChannel();
				
				if(memberFile != null) {
					Long serverID = NumberUtil.stringToLong(memberFile.getParentFile().getName());
					
					if(serverID == null) {
						ErrorLogger.LogIssue("Server ID from folder name is null", privateChannel);
						return;
					}
					
					Guild server = event.getJDA().getGuildById(serverID);
					
					if(server != null) {
						addListener(userID, privateChannel, server);
					} else {
						BotUtil.sendMessage(privateChannel, "I can't find the server to send it to anymore. If you believe "
								+ "this to be an error, please contact DELIBURD.");
						return;
					}
				} else {
					BotUtil.sendMessage(event.getChannel(), "It appears we don't have any file to send to you. This could either be "
							+ "because you never recorded anything or that we deleted your audio file because it's been 24 hours. "
							+ "If you believe this is a mistake, please contact DELIBURD.");
				}
			}
		}
	}
	
	private class AudioServerInfo {
		private final JDA JDA;
		private final AudioWriter audioWriter;
		private final ReentrantLock lock;
		private final long serverID;
		
		private AudioServerInfo(AudioWriter writer, long serverID, JDA JDA) {
			this.JDA = JDA;
			this.serverID = serverID;
			audioWriter = writer;
			lock = new ReentrantLock();
		}

		public JDA getJDA() {
			return JDA;
		}

		public AudioWriter getAudioWriter() {
			return audioWriter;
		}
		
		public Guild getServer() {
			return JDA.getGuildById(serverID);
		}

		public ReentrantLock getLock() {
			return lock;
		}
	}
}
