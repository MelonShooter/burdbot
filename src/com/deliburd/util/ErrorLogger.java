package com.deliburd.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.deliburd.readingpuller.TextConstant;

public class ErrorLogger {
	private ErrorLogger() {}
	
	/**
	 * Logs an exception to TextConstant.LOG_FILE
	 * 
	 * @param e The exception
	 */
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
}
