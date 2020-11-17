package com.deliburd.bot.burdbot;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.VocarooUtil;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SpanishServerEvents extends ListenerAdapter {
	private static final long SPANISH_SERVER_ID = 243838819743432704L;
	
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
    	if(event.getGuild().getIdLong() != SPANISH_SERVER_ID || !BotUtil.hasWritePermission(event)) {
    		return;
    	}
    	
    	handleMusicCommands(event);
    	
    	try {
			handleVocarooRecordings(event);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		} catch (RejectedExecutionException e) {}
    }

	private void handleVocarooRecordings(MessageReceivedEvent event) throws IOException {
		List<String> vocarooLinks = VocarooUtil.extractVocarooDownloadLinks(event.getMessage().getContentRaw());
		
		for(String vocarooLink : vocarooLinks) {
			URL vocarooURL = new URL(vocarooLink);
			InputStream mp3Stream = vocarooURL.openStream();
			event.getChannel().sendMessage("Here is <@")
					.append(Long.toString(event.getAuthor().getIdLong()))
					.append(">'s vocaroo recording as an MP3 file.").addFile(mp3Stream, "vocaroo-to-mp3.mp3")
					.queue(sucess -> closeVocarooStream(mp3Stream, null), failure -> closeVocarooStream(mp3Stream, failure));
		}
	}
	
	private void closeVocarooStream(InputStream vocarooStream, Throwable failure) {
		if(failure != null) {
			ErrorLogger.LogException(failure);
		}
		
		try {
			vocarooStream.close();
		} catch (IOException e) {
			System.out.println("Couldn't close a vocaroo download stream. Printing stack trace...");
			ErrorLogger.LogException(e);
		}
	}

	private void handleMusicCommands(MessageReceivedEvent event) {
    	long musicChannelID = 263643662808776704L;
    	String message = event.getMessage().getContentRaw();
    	
    	String[] botPrefixes = {
    		"-",
    		"--",
    		"---",
    		"!",
    		"!!",
    	};
    	
    	boolean containsMusicPrefix = false;
    	
    	for (var prefix : botPrefixes) {
    		if(message.startsWith(prefix)) {
    			containsMusicPrefix = true;
    		}
    	}
    	
    	if(event.getChannel().getIdLong() == musicChannelID && containsMusicPrefix) {
    		BotUtil.sendMessage(event.getChannel(), "Please put music bot commands in <#247135634265735168> as they do not work here");
    	}
	}
}
