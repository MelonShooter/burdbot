package com.deliburd.bot.burdbot;

public class Constant {
	private Constant() {}

	/**
	 * The prefix for the bot
	 */
	public static final String COMMAND_PREFIX = ".sl";

	/**
	 * The interval in which to reload the texts in seconds
	 */
	public static final int RELOAD_TEXTS_INTERVAL = 86400;
	
	/**
	 * The interval in which texts can be requested per user
	 */
	public static final int TEXT_INTERVAL = 10;
	
	/**
	 * The prefix for the bot with a space after it
	 */
	public static final String COMMAND_PREFIX_WITH_SPACE = COMMAND_PREFIX + " ";
	
	/**
	 * The description of the fetchtext command
	 */
	public static final String FETCH_TEXT_DESCRIPTION = "Gets a text based on the language and difficulty specified";
	
	/**
	 * The default command cooldown time
	 */
	public static final long DEFAULT_COOLDOWN = 5;
	
	/**
	 * The base command for help.
	 */
	public static final String HELP_COMMAND = "help";
}
