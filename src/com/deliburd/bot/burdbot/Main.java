package com.deliburd.bot.burdbot;

import java.util.ConcurrentModificationException;
import java.util.Timer;
import java.util.TimerTask;

import javax.security.auth.login.LoginException;

import com.deliburd.bot.burdbot.commands.CommandManager;
import com.deliburd.bot.burdbot.commands.MultiCommand;
import com.deliburd.bot.burdbot.commands.MultiCommandAction;
import com.deliburd.bot.burdbot.util.BotUtil;
import com.deliburd.readingpuller.ReadingManager;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.ErrorLogger;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

public class Main {
	public static void main(String[] args)
    throws LoginException
    {
        //reloadTexts(Constant.RELOAD_TEXTS_INTERVAL);
		//Call the below line if not reloading text, otherwise comment it out. 
		ReadingManager.createTextFolderStructure();
        
		MultiCommand fetchTextCommand = CommandManager.addCommand("fetchtext", Constant.FETCH_TEXT_DESCRIPTION)
				.setMinArguments(2)
				.setDefaultAction(new MultiCommandAction() {
					@Override
					public void OnCommandRun(String[] args, MessageChannel channel) {
						if (!ReadingManager.isRegeneratingTexts()) {
							BotUtil.sendMessage(channel, "```" + ReadingManager.fetchText(args[0], args[1]) + "```");
						}
					}
				});
        
		int firstTime = 0;
		
        for(var language : ScraperLanguage.values()) {
        	fetchTextCommand.addArgument(0, language.toString(), language.getAliases());
        	for(var difficulty : ScraperDifficulty.values()) {
        		if(firstTime < 2) {
        			fetchTextCommand.addArgument(1, difficulty.toString(), difficulty.getAliases());
        			firstTime++;
        		}

        		fetchTextCommand = fetchTextCommand.addFinalArgumentPath(language.toString(), difficulty.toString());
        	}
        }
        
		fetchTextCommand.setArgumentDescriptions("The language of the text", "The difficulty of the text")
				.addCommandNames("fetchtxt", "ftxt", "ftext")
				.setCooldown(10)
				.finalizeCommand();

        JDA jda = JDABuilder.createDefault(Constant.BOT_TOKEN_STRING).build();
        jda.addEventListener(new CommandManager());
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
}
