package com.deliburd.bot.burdbot;

import java.io.File;
import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.security.auth.login.LoginException;

import com.deliburd.bot.burdbot.commands.CommandManager;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.readingpuller.ReadingManager;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.FileUtil;
import com.deliburd.util.MessageResponseQueue;
import com.deliburd.util.ActivitySwitcher;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

public class Main {
	private static volatile JDA JDAInstance;

	public static void main(String[] a) throws LoginException, InterruptedException {
        reloadTexts(Constant.RELOAD_TEXTS_INTERVAL);
        FileUtil.deleteFolder(new File(Constant.FORVO_FOLDER));

		JDABuilder burdRecorder = JDABuilder.createDefault(BotConstant.BOT_TOKEN_STRING);
		String helpDescription = "Displays a list of commands and their descriptions.";
        CommandManager commandManager = new CommandManager(Constant.COMMAND_PREFIX, helpDescription, burdRecorder);
        
		MultiCommand fetchTextCommand = commandManager.addCommand("fetchtext", Constant.FETCH_TEXT_DESCRIPTION)
				.setMinArguments(2)
				.setDefaultAction((args, event, command) -> {
					MessageChannel channel = event.getChannel();

					if (!ReadingManager.isRegeneratingTexts()) {
						String text = ReadingManager.fetchText(args[0], args[1]);

						if(text.isEmpty()) {
							ErrorLogger.LogIssue("User tried to fetch text, but returned text was blank.", channel);
						} else {
							BotUtil.sendMessage(channel, "```" + text + "```");
						}
					} else {
						BotUtil.sendMessage(channel, "I'm currently regenerating the texts. Please wait a moment.");
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
		
		ActivitySwitcher.addState(",help | Making AOTW/VC recordings", 3, TimeUnit.DAYS);
		ActivitySwitcher.addState(",help | Fetching pronunciations", 1, TimeUnit.DAYS);

		JDAInstance = burdRecorder.addEventListeners(MessageResponseQueue.getQueue())
				.build().awaitReady();
    }
	
	/**
	 * Returns an instance of JDA
	 * 
	 * @return An instance of JDA. Null if the instance isn't ready.
	 */
	public static JDA getJDAInstance() {
		return JDAInstance;
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
		}, 0, interval * 1000L);
	}
}
