package com.deliburd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.deliburd.bot.burdbot.Constant;
import net.dv8tion.jda.api.entities.MessageChannel;

public class ErrorLogger {
	private ErrorLogger() {}
	
	/**
	 * Logs a throwable to TextConstant.LOG_FILE
	 * 
	 * @param e The exception
	 */
	public static void LogException(Throwable e) {
		File logFile = new File(Constant.LOG_FILE);
		
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e1) {
				System.out.println("Failed to create logging file");
				return;
			}
		}

		try(PrintStream printStream = new PrintStream(new FileOutputStream(logFile, true))) {
			e.printStackTrace(printStream);
			printStream.append('\n');
			e.printStackTrace();
		} catch (FileNotFoundException e1) {
			System.out.println("Logging file could not be found.");
		}
	}
	
	
	/**
	 * Logs a throwable to the log file and gives a generic error message to a certain channel to alert users
	 * 
	 * @param e The throwable to log
	 * @param channel The channel to give the error message in
	 */
	public static void LogException(Throwable e, MessageChannel channel) {
		BotUtil.sendMessage(channel, Constant.ERROR_MESSAGE);
		LogException(e);
	}
	
	/**
	 * Logs an issue to the log file and gives a generic error message to a certain channel to alert users
	 * 
	 * @param issue The issue to log
	 * @param channel The channel to give the error message in
	 */
	public static void LogIssue(String issue, MessageChannel channel) {
		BotUtil.sendMessage(channel, Constant.ERROR_MESSAGE);
		LogIssue(issue);
	}
	
	/**
	 * Logs an issue to the log file
	 * 
	 * @param issue The issue to log
	 */
	public static void LogIssue(String issue) {
		LogException(new Exception(issue));
	}
}
