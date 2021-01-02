package com.deliburd.util.scraper;

import java.io.File;

public class TextConstant {
	private TextConstant() {}
	
	public static final String WORKING_DIRECTORY = System.getProperty("user.dir") + File.separator + "sesion_bot_data" + File.separator;
	public static final String LOG_FILE = WORKING_DIRECTORY + "log.txt";
	public static final String TEXT_FOLDER = WORKING_DIRECTORY + "texts";
	public static final String GUTENBURG_CACHE_FOLDER = WORKING_DIRECTORY + "gutenburgcache" + File.separator + "ok";
	public static final String PAPELUCHO_CACHE_FOLDER = WORKING_DIRECTORY + "papeluchocache";
	public static final int LINK_PULL_COOLDOWN = 86400; //1 day in seconds
	public static final int CHARACTER_TEXT_COUNT = 650;
}
