package com.deliburd.bot.burdbot;

import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;
import javax.security.auth.login.LoginException;

import com.deliburd.bot.burdbot.commands.CommandManager;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.bot.burdbot.commands.MultiCommandAction;
import com.deliburd.readingpuller.ReadingManager;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.MessageResponseQueue;
import com.deliburd.util.ServerConfig;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Main extends ListenerAdapter {
	public static void main(String[] args) throws LoginException {
        reloadTexts(Constant.RELOAD_TEXTS_INTERVAL);
        
		ServerConfig.registerTable("templates");
		JDABuilder burdRecorder = JDABuilder.createDefault(BotConstant.BOT_TOKEN_STRING);
		String helpDescription = "Displays a list of commands and their descriptions.";
        CommandManager sesionManager = new CommandManager(Constant.COMMAND_PREFIX, helpDescription, burdRecorder);
        
		MultiCommand fetchTextCommand = sesionManager.addCommand("fetchtext", Constant.FETCH_TEXT_DESCRIPTION)
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

		burdRecorder.addEventListeners(MessageResponseQueue.getQueue()).build();
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
}
