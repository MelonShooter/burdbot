package com.deliburd.bot.burdbot.commands;

import java.util.Arrays;
import java.util.HashMap;

import com.deliburd.bot.burdbot.Constant;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandManager extends ListenerAdapter {
	/**
	 * A list of all of the bot's commands
	 */
	private static HashMap<String, Command> commands = new HashMap<String, Command>();
	
	/**
	 * Creates a command without arguments or aliases
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @param action The action to run when the command is typed.
	 * @return The new FinalCommand
	 */
	public static FinalCommand addCommand(String command, String description, FinalCommandAction action) {
		FinalCommand newCommand = new FinalCommand(command, description, action);
		commands.put(command, newCommand);
		
		return newCommand;
	}
	
	/**
	 * Creates a command that can have arguments added to it
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @return The new MultiCommand
	 */
	public static MultiCommand addCommand(String command, String description) {
		MultiCommand newCommand = new MultiCommand(command, description);
		commands.put(command, newCommand);
		
		return newCommand;
	}
	
	/**
	 * Detects if the message is our bot's command and call the according command's method if so.
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		if (event.isFromType(ChannelType.TEXT)) {
			String message = event.getMessage().getContentDisplay();
			if(message.substring(0, 4).equals(Constant.COMMAND_PREFIX_WITH_SPACE)) {
				String[] messageArgs = message.split("\\s+");
				
				Command command = commands.get(messageArgs[1]);
				
				if(command != null) {
					final String[] commandArguments = messageArgs.length == 2 ? null : Arrays.copyOfRange(messageArgs, 2, messageArgs.length);
					command.onCommandCalled(commandArguments, event.getChannel());
				}
			}
		}
	}
}
