package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.bot.burdbot.util.Cooldown;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandManager extends ListenerAdapter {
	/**
	 * A list of all of the bot's commands
	 */
	private static HashMap<String, Command> commands = new HashMap<String, Command>();
	
	/**
	 * The string to display for the help command
	 */
	private static String commandHelp;
	
	/**
	 * The help command
	 */
	private static Cooldown helpCooldown;
	
	static {
		final String helpDescription = "Displays a list of commands and their descriptions.";
		MultiCommand help = addCommand(Constant.HELP_COMMAND, helpDescription).setArgumentDescriptions("A command.").addArgument(0);
		help.addFinalArgumentPath(new MultiCommandAction() {
			@Override
			public void OnCommandRun(String[] args, MessageChannel channel) {
				final Command command = commands.get(args[0]);
				
				if (command == null) {
					help.giveInvalidArgumentMessage(channel);
				} else {
					channel.sendMessage(command.commandDescription);
				}
			}
		}, null)
		.setBaseAction(new MultiCommandAction() {
			@Override
			public void OnCommandRun(String[] args, MessageChannel channel) {
				sendHelp(channel);
			}
		});
		helpCooldown = help.commandCooldown;
		help.finalizeCommand();
	}
	
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
		final String prefixWithSpace = Constant.COMMAND_PREFIX_WITH_SPACE;
		if(event.isFromType(ChannelType.TEXT)) {
			final MessageChannel channel = event.getChannel();
			final String message = event.getMessage().getContentDisplay();
			if(message.substring(0, prefixWithSpace.length()).equals(prefixWithSpace)) {
				String[] messageArgs = message.split("\\s+");
				
				Command command = commands.get(messageArgs[1]);
				
				if(command != null) {
					final Cooldown cooldown = command.commandCooldown;
					if(command.isFinalized() && cooldown.isCooldownOver()) {
						final String[] commandArguments = messageArgs.length == 2 ? null : Arrays.copyOfRange(messageArgs, 2, messageArgs.length);
						command.onCommandCalled(commandArguments, channel);
						cooldown.resetCooldown();
					} else if (!command.isFinalized()) {
						throw new RuntimeException("Tried to call a command that wasn't finalized");
					}

				}
			} else if(message.equals(Constant.COMMAND_PREFIX) && helpCooldown.isCooldownOver()) {
				sendHelp(channel);
			}
		}
	}
	
	
	/**
	 * Sends the help message
	 */
	private static void sendHelp(MessageChannel channel) {
		if(commandHelp == null) {
			StringBuilder commandHelpString = new StringBuilder("BurdBot's Commands:\n");
			var sortedKeys = new ArrayList<String>(commands.keySet());
			Collections.sort(sortedKeys);
			
			for (var key : sortedKeys) {
				commandHelpString.append(Constant.COMMAND_PREFIX_WITH_SPACE);
				commandHelpString.append(key);
				commandHelpString.append(" - ");
				commandHelpString.append(commands.get(key).getShortCommandDescription());
				commandHelpString.append("\n");
			}
			
			commandHelpString.append("\nFor more information, type .sl help followed by the command\neg: .sl help fetchtext");
			commandHelp = commandHelpString.toString();
		}
		
		channel.sendMessage(commandHelp).queue();
	}
}
