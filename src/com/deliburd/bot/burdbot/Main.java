package com.deliburd.bot.burdbot;

import java.time.Instant;

import javax.security.auth.login.LoginException;

import com.deliburd.bot.burdbot.commands.CommandManager;
import com.deliburd.readingpuller.ReadingManager;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Main extends ListenerAdapter {
	private static long lastCreateTime = 0;
	private static long lastFetchTime = 0;
	private static volatile int isFetching = 0;
	private static final int LINK_PULL_COOLDOWN = 86400; //1 day in seconds
	private static final int FETCH_COOLDOWN = 10; //1 day in seconds

    public static void main(String[] args)
    throws LoginException
    {
        CommandManager.addCommand("f", "aah");

        JDA jda = JDABuilder.createDefault(Constant.BOT_TOKEN_STRING).build();
        jda.addEventListener(new CommandManager(), new Main());
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.isFromType(ChannelType.TEXT))
        {
        	String message = event.getMessage().getContentDisplay();
        	MessageChannel channel = event.getChannel();
        	
        	if(message.length() < 4) {
        		if(message.equals(".sl")) {
            		channel.sendMessage("To start a sesión, type .sl rltxt then .sl en/es e/m to get a text.").queue();
            	}
        		
        		return;
        	}
        	
            if (message.substring(0, 4).equals(".sl ")) {
            	if(message.equals(".sl rltxt")) {
        			var currentTime = Instant.now().getEpochSecond();

        			if (lastCreateTime + LINK_PULL_COOLDOWN <= currentTime) { // Cooldown is over
        				isFetching = 1;
        				channel.sendMessage("Reloading texts! This can take a while.").queue();
        				ReadingManager.regenerateTexts();
        				channel.sendMessage("Done fetching texts!").queue();
        				isFetching = 2;
        				lastCreateTime = currentTime;
        			} else {
        				channel.sendMessage("Texts already loaded!").queue();
        			}

            		return;
            	}
            	
            	var messageArgs = message.split(" ");
            	
            	if(messageArgs.length != 3) {
            		return;
            	}
            	
            	if(messageArgs[1].equals(ScraperLanguage.English.toString()) || messageArgs[1].equals(ScraperLanguage.Spanish.toString())) {
            		if(messageArgs[2].equals(ScraperDifficulty.Easy.toString()) || messageArgs[2].equals(ScraperDifficulty.Medium.toString())) {
                		if(isFetching == 0) {
                			channel.sendMessage("Reload the texts first by typing .sl rltxt").queue();
                		}

                		if(isFetching < 2) {
                			return;
                		}
                		
                		var currentTime = Instant.now().getEpochSecond();
                		
            			if (lastFetchTime + FETCH_COOLDOWN <= currentTime) { // Cooldown is over
            				lastFetchTime = currentTime;
            				channel.sendMessage("```" + ReadingManager.fetchText(messageArgs[1], messageArgs[2]) + "```").queue();
            			}
            			
            		}
            	}
            }
        }
    }
}
