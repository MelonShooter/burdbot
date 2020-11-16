package com.deliburd.bot.burdbot;

import com.deliburd.util.BotUtil;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class SpanishServerEvents extends ListenerAdapter {
	private static final long spanishServerID = 243838819743432704L;
	
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
    	if(event.getGuild().getIdLong() != spanishServerID) {
    		return;
    	}
    	
    	handleMusicCommands(event);
    }

	private void handleMusicCommands(MessageReceivedEvent event) {
		boolean canWrite = BotUtil.hasWritePermission(event);
    	long musicChannelID = 263643662808776704L;
    	String message = event.getMessage().getContentRaw();
    	
    	String[] botPrefixes = {
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
}
