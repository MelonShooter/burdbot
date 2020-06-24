package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.bot.burdbot.util.BotUtil;
import com.deliburd.bot.burdbot.util.Cooldown;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandManager extends ListenerAdapter {
	/**
	 * A list of all of the bot's commands mapped to the command object
	 */
	private static HashMap<String, Command> commandNameMap = new HashMap<String, Command>();
	
	/**
	 * A set of the registered commands.
	 */
	private static HashSet<Command> commandSet = new HashSet<Command>();
	
	/**
	 * Maps all command aliases to the base command
	 */
	private static HashMap<String, String> commandAliasLookup = new HashMap<String, String>();
	/**
	 * The string to display for the help command
	 */
	private static String commandHelp;
	
	/**
	 * The help command's cooldown
	 */
	private static Cooldown helpCooldown;
	
	static {
		final String helpDescription = "Displays a list of commands and their descriptions.";
		MultiCommand help = addCommand(Constant.HELP_COMMAND, helpDescription).setArgumentDescriptions("A command.").addArgument(0);
		help.addFinalArgumentPath(new MultiCommandAction() {
			@Override
			public void OnCommandRun(String[] args, MessageChannel channel) {
				final Command command = commandNameMap.get(args[0]);
				
				if (command == null) {
					help.giveInvalidArgumentMessage(channel);
				} else {
					BotUtil.sendMessage(channel, command.getCommandDescription());
				}
			}
		}, new String[] {null})
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
		registerCommand(command, newCommand);
		
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
		registerCommand(command, newCommand);
		
		return newCommand;
	}
	
	/**
	 * Detects if the message is our bot's command and call the according command's method if so.
	 * 
	 * @param event The message event
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		final boolean canWrite = BotUtil.hasWritePermission(event);
		if(canWrite) {
			final MessageChannel channel = event.getChannel();
			final String prefixWithSpace = Constant.COMMAND_PREFIX_WITH_SPACE;
			final int minCommandLength = prefixWithSpace.length();
			final String message = event.getMessage().getContentDisplay();
			final User user = event.getAuthor();
			
			if(message.length() >= minCommandLength && message.substring(0, minCommandLength).equals(prefixWithSpace)) {
				String[] messageArgs = message.split("\\s+");
				final String baseCommand = commandAliasLookup.get(messageArgs[1]);
				
				Command command = commandNameMap.get(baseCommand);
				
				if(command != null) {
					final Cooldown cooldown = command.commandCooldown;
					if(command.isFinalized() && cooldown.isCooldownOver(user)) {
						final String[] commandArguments = messageArgs.length == 2 ? null : Arrays.copyOfRange(messageArgs, 2, messageArgs.length);
						command.onCommandCalled(commandArguments, channel, user);
						cooldown.resetCooldown(user);
					} else if (!command.isFinalized()) {
						throw new RuntimeException("Tried to call a command that wasn't finalized");
					}

				}
			} else if(message.equals(Constant.COMMAND_PREFIX) && helpCooldown.isCooldownOver(user)) {
				String helpCommand = Constant.COMMAND_PREFIX_WITH_SPACE + Constant.HELP_COMMAND;
				BotUtil.sendMessage(channel, "Type " + helpCommand + " for the command list.");
				helpCooldown.resetCooldown(user);
			}
		}
	}
	
	/**
	 * Sends the help message
	 *  
	 * @param channel The channel's name to send the message to
	 */
	private static void sendHelp(MessageChannel channel) {
		if(commandHelp == null) {
			StringBuilder commandHelpString = new StringBuilder("BurdBot's Commands:\n");
			var sortedKeys = new ArrayList<String>(commandNameMap.keySet());
			Collections.sort(sortedKeys);
			
			for (var key : sortedKeys) {
				commandHelpString.append("```");
				commandHelpString.append(Constant.COMMAND_PREFIX_WITH_SPACE);
				commandHelpString.append(key);
				commandHelpString.append(" - ");
				commandHelpString.append(commandNameMap.get(key).getShortCommandDescription());
				commandHelpString.append("```");
			}
			
			commandHelpString.append("\nFor more information, type .sl help followed by the command\neg: .sl help fetchtext");
			commandHelp = commandHelpString.toString();
		}
		
		BotUtil.sendMessage(channel, commandHelp);
	}
	
	/**
	 * Checks if a command has been registered.
	 * 
	 * @param command The command to check
	 * @return Whether the command has been registered
	 */
	private static boolean doesCommandExist(Command command) {
		return commandSet.contains(command);
	}
	
	/**
	 * Registers a command with the CommandManager
	 * 
	 * @param name The command's name
	 * @param command The command
	 */
	private static void registerCommand(String name, Command command) {
		commandNameMap.put(name, command);
		commandAliasLookup.put(name, name);
		commandSet.add(command);
	}
	
	/**
	 * Creates an alias for a command.
	 * 
	 * @param command The command to make the alias for. The command must be registered and not finalized.
	 * @param alias The new alias for the command. This alias must not be an existing command name/alias.
	 */
	static void createAlias(Command command, String alias) {
		if(!doesCommandExist(command)) {
			throw new RuntimeException("Command does not exist.");
		} else if(command.isFinalized()) {
			throw new RuntimeException("Command has already been finalized.");
		} else if(commandNameMap.containsKey(alias)) {
			throw new RuntimeException("This alias is already a registered command name/alias.");
		}
		
		commandAliasLookup.put(alias, command.getCommandName());
	}
}
