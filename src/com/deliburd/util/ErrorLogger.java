package com.deliburd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.deliburd.readingpuller.TextConstant;

import net.dv8tion.jda.api.entities.MessageChannel;

public class ErrorLogger {
	private ErrorLogger() {}
	
	public static synchronized void LogException(Throwable e) {
		File logFile = new File(TextConstant.LOG_FILE);
		
		if(!logFile.exists()) {
			try {
				logFile.createNewFile();
			} catch (IOException e1) {
				System.out.println("Failed to create logging file");
			}
		}

		PrintStream printStream;
		try {
			printStream = new PrintStream(new FileOutputStream(logFile, true));
			e.printStackTrace(printStream);
			printStream.append('\n');
			printStream.close();
		} catch (FileNotFoundException e1) {
			System.out.println("Logging file could not be found.");
		}
	}
	
	public static synchronized void LogException(Throwable e, MessageChannel channel) {
		BotUtil.sendMessage(channel, "I'm sorry, but something has gone wrong. Please notify <@367538590520967181> of this.");
		LogException(e);
	}
}
