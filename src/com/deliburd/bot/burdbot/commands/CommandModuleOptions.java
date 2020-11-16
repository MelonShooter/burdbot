package com.deliburd.bot.burdbot.commands;

/**
 * An enum of options for a command module.
 * 
 * @author DELIBURD
 *
 */
public enum CommandModuleOptions {
	/**
	 * Enables the commands in the module to be used in text (and news) channels.
	 */
	TEXTCHANNELENABLED,
	
	/**
	 * Enables the commands in the module to be used in private channels.
	 */
	PRIVATECHANNELENABLED,
	
	/**
	 * Disables checks on the validity of commands on runtime. If the annotation processor for the command framework is already enabled, the runtime
	 * checks are unnecessary because the checks are done on compile-time. However, if the annotation processor isn't present to check for errors
	 * in the creation of a bot command and the checks on runtime for them are disabled, then the behavior is undefined.
	 */
	DISABLECOMMANDRUNTIMECHECKS,
	
	/**
	 * Hides commands from the help command if the user doesn't have permission to use the command.
	 */
	HIDECOMMANDSIFINSUFFICIENTPERMISSIONS
	
}
