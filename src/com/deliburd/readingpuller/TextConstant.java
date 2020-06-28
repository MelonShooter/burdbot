package com.deliburd.readingpuller;

import java.io.File;

public class TextConstant {
	private TextConstant() {}
	
	/**
	 * The directory where all the bot's data will go
	 */
	public static final String WORKING_DIRECTORY = System.getProperty("user.dir") + File.separator + "sesion_bot_data" + File.separator;
	
	/**
	 * The log file for exceptions
	 */
	public static final String LOG_FILE = WORKING_DIRECTORY + "log.txt";
	
	/**
	 * The folder for the generated texts
	 */
	public static final String TEXT_FOLDER = WORKING_DIRECTORY + "texts";
	
	/**
	 * The folder containing the gutenburg texts
	 */
	public static final String GUTENBURG_CACHE_FOLDER = WORKING_DIRECTORY + "gutenburgcache" + File.separator + "ok";
	
	/**
	 * The folder containing the papelucho texts
	 */
	public static final String PAPELUCHO_CACHE_FOLDER = WORKING_DIRECTORY + "papeluchocache";
	
	/**
	 * The cooldown to pull links in seconds
	 */
	public static final int LINK_PULL_COOLDOWN = 86400; // 1 day in seconds
	
	/**
	 * The approximate text count to use per text fetched
	 */
	public static final int APRROXIMATE_CHARACTER_TEXT_COUNT = 650;
}
