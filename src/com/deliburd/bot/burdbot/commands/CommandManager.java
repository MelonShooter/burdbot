package com.deliburd.bot.burdbot.commands;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.util.BotUtil;
import com.deliburd.util.Cooldown;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * @author MelonShooter
 *
 */
public class CommandManager extends ListenerAdapter {	
	/**
	 * Links each command manager's prefix to the manager itself.
	 */
	private final static ConcurrentHashMap<String, CommandManager> prefixToCommandManagerMap = new ConcurrentHashMap<>();
	
	/**
	 * A list of all of the bot's commands mapped to the command object
	 */
	private final ConcurrentSkipListMap<String, Command> commandNameMap;
	
	/**
	 * A set of the registered commands.
	 */
	private final Set<Command> commandSet;
	
	/**
	 * Maps all command aliases to the base command
	 */
	private final ConcurrentHashMap<String, String> commandAliasLookup;

	private final String prefix;
	private final JDA JDA;
	private final JDABuilder JDABuilder;
	private final String helpDescription;
	private volatile boolean isInitialized;
	
	/**
	 * Creates a CommandManager that has its commands registered before JDA starts
	 * 
	 * @param prefix The command manager's prefix to use for all commands registered in it
	 * @param helpDescription The description of the help command for this command manager
	 * @param builder The JDABuilder to register the commands for when the first command is added.
	 */
	public CommandManager(String prefix, String helpDescription, JDABuilder builder) {
		this.prefix = Objects.requireNonNull(prefix, "The prefix cannot be null");
		this.helpDescription = Objects.requireNonNull(helpDescription, "The help description cannot be null");
		this.JDA = null;
		this.JDABuilder = Objects.requireNonNull(builder, "The JDA builder cannot be null");
		commandNameMap = new ConcurrentSkipListMap<String, Command>();
		commandSet = ConcurrentHashMap.newKeySet();
		commandAliasLookup = new ConcurrentHashMap<String, String>();
	}
	
	/**
	 * Creates a CommandManager that has its commands registered after JDA starts
	 * 
	 * @param prefix The command manager's prefix to use for all commands registered in it
	 * @param helpDescription The description of the help command for this command manager
	 * @param JDA The JDA instance to register the commands for when the first command is added.
	 */
	public CommandManager(String prefix, String helpDescription, JDA JDA) {
		this.prefix = Objects.requireNonNull(prefix, "The prefix cannot be null");
		this.helpDescription = Objects.requireNonNull(helpDescription, "The help description cannot be null");
		this.JDA = Objects.requireNonNull(JDA, "The JDA instance cannot be null");
		this.JDABuilder = null;
		commandNameMap = new ConcurrentSkipListMap<String, Command>();
		commandSet = ConcurrentHashMap.newKeySet();
		commandAliasLookup = new ConcurrentHashMap<String, String>();
	}
	
	/**
	 * Gets the command manager associated with the given prefix.
	 * Returns null if the command manager isn't found or
	 * if the command manager doesn't have a command attached to it yet
	 * 
	 * @param prefix The prefix of the command manager
	 * @return The command manager
	 */
	public static CommandManager getManager(String prefix) {
		return prefixToCommandManagerMap.get(prefix);
	}
	
	/**
	 * Gets all the command managers.
	 * 
	 * @return All of the command managers
	 */
	public static CommandManager[] getManagers() {
		return prefixToCommandManagerMap.values().toArray(new CommandManager[prefixToCommandManagerMap.size()]);
	}
	
	/**
	 * Creates a command without arguments or aliases.
	 * If a command with this command name already exists,
	 * the command returned won't have any effect even when finalized.
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @param action The action to run when the command is typed.
	 * @return The new FinalCommand
	 */
	public FinalCommand addCommand(String command, String description, FinalCommandAction action) {
		if(!isInitialized) {
			initializeManager();
		}
		
		FinalCommand newCommand = new FinalCommand(prefix, command, description, action);
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
	public MultiCommand addCommand(String command, String description) {
		if(!isInitialized) {
			initializeManager();
		}
		
		MultiCommand newCommand = new MultiCommand(prefix, command, description);
		registerCommand(command, newCommand);
		
		return newCommand;
	}
	
	/**
	 * Returns whether the command exists in this command manager
	 * 
	 * @param commandName The command's name or an alias of its name
	 * @return Whether the command exists in this command manager
	 */
	public boolean hasCommand(String commandName) {
		return commandAliasLookup.containsKey(commandName);
	}
	
	/**
	 * Detects if the message is our bot's command and call the according command's method if so.
	 * 
	 * @param event The message event
	 */
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		boolean canWrite = BotUtil.hasWritePermission(event);
		if(event.getChannelType() == ChannelType.TEXT && canWrite && !event.getAuthor().isBot()) {
			String prefix = Constant.COMMAND_PREFIX;
			int minCommandLength = prefix.length() + 1;
			String message = event.getMessage().getContentRaw();
			User user = event.getAuthor();
			
			if(message.length() < minCommandLength || !message.startsWith(prefix)) {
				return;
			}

			String[] messageArgs = message.split(" +");
			String baseCommand = messageArgs[0].substring(prefix.length());
			String parsedBaseCommand = commandAliasLookup.get(baseCommand);

			if (parsedBaseCommand == null) {
				return;
			}

			Command command = commandNameMap.get(parsedBaseCommand);

			if (command == null) {
				return;
			}

			Cooldown cooldown = command.commandCooldown;
			if (command.isFinalized() && cooldown.isCooldownOver(user)) {
				if(hasPermission(command, event)) {
					String[] commandArguments = messageArgs.length == 1 ? null : Arrays.copyOfRange(messageArgs, 1, messageArgs.length);
					command.onCommandCalled(commandArguments, event);
				} else {
					command.giveInsufficientPermissionsMessage(event.getChannel());
				}
				
				cooldown.resetCooldown(user);
			} else if (!command.isFinalized()) {
				throw new RuntimeException("Tried to call a command that wasn't finalized");
			}
			
		}
	}
	
	private boolean hasPermission(Command command, MessageReceivedEvent event) {
		Permission[] restrictions = command.getPermissionRestrictions();
		if(restrictions == null || event.getMember().hasPermission(restrictions)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Creates an alias for a command.
	 * 
	 * @param command The command to make the alias for. The command must be registered and not finalized.
	 * @param alias The new alias for the command. This alias must not be an existing command name/alias.
	 */
	void createAlias(Command command, String alias) {
		if(!doesCommandExist(command)) {
			throw new RuntimeException("Command does not exist.");
		} else if(command.isFinalized()) {
			throw new RuntimeException("Command has already been finalized.");
		} else if(commandNameMap.containsKey(alias)) {
			throw new RuntimeException("This alias is already a registered command name/alias.");
		}
		
		commandAliasLookup.put(alias, command.getCommandName());
	}
	
	/**
	 * Initializes the manager and sets it up to receive event calls for messages
	 */
	private void initializeManager() {
		isInitialized = true;
		
		addCommand(Constant.HELP_COMMAND, helpDescription)
				.setArgumentDescriptions("A command.")
				.addArgument(0)
				.addFinalArgumentPath(this::helpWithCommandArgument, "")
				.setBaseAction(this::sendHelp)
				.finalizeCommand();

		prefixToCommandManagerMap.put(prefix, this);
		
		if(JDA != null) {
			JDA.addEventListener(this);
		}
		else {
			JDABuilder.addEventListeners(this);
		}
	}

	/**
	 * Sends the help message
	 * 
	 * @param args The arguments for this command (will be null)
	 * @param event The event associated with the command typed
	 * @param selfCommand The MultiCommand object associated with this command
	 */
	private void sendHelp(String[] args, MessageReceivedEvent event, MultiCommand selfCommand) {
		StringBuilder commandHelpString = new StringBuilder(Constant.BOT_NAME + "'s Commands:\n");
		var commandEntrySet = commandNameMap.entrySet();

		for (var commandEntry : commandEntrySet) {
			Command command = commandEntry.getValue();
			
			if(hasPermission(command, event)) {
				String commandName = commandEntry.getKey();
				
				commandHelpString
						.append("```")
						.append(Constant.COMMAND_PREFIX)
						.append(commandName)
						.append(" - ")
						.append(commandNameMap.get(commandName).getShortCommandDescription())
						.append("```");
			}
		}

		String fullHelpCommand = Constant.COMMAND_PREFIX + Constant.HELP_COMMAND;

		commandHelpString.append("\nFor more information, type ")
				.append(fullHelpCommand)
				.append(" followed by the command.\n")
				.append(fullHelpCommand)
				.append(" <Command>");

		BotUtil.sendMessage(event.getChannel(), commandHelpString.toString());
	}
	
	/**
	 * Checks if a command has been registered.
	 * 
	 * @param command The command to check
	 * @return Whether the command has been registered
	 */
	private boolean doesCommandExist(Command command) {
		return commandSet.contains(command);
	}
	
	/**
	 * Registers a command with the CommandManager
	 * 
	 * @param name The command's name
	 * @param command The command
	 */
	private void registerCommand(String name, Command command) {
		if(hasCommand(name)) {
			return;
		}

		commandAliasLookup.putIfAbsent(name, name);
		commandNameMap.putIfAbsent(name, command);
		commandSet.add(command);
	}
	
	/**
	 * Is run when the help command is run with a command as an argument
	 * 
	 * @param args The arguments for the command
	 * @param event The event from the command
	 * @param helpCommand The help command's object
	 */
	private void helpWithCommandArgument(String[] args, MessageReceivedEvent event, MultiCommand helpCommand) {
		MessageChannel channel = event.getChannel();
		String baseCommand = commandAliasLookup.get(args[0]);
		
		if(baseCommand == null) {
			helpCommand.giveInvalidArgumentMessage(channel);
			return;
		}
		
		Command command = commandNameMap.get(baseCommand);

		if (command == null) {
			helpCommand.giveInvalidArgumentMessage(channel);
		} else if (!hasPermission(command, event)) {
			BotUtil.sendMessage(channel, "You don't have access to this command.");
		} else {
			BotUtil.sendMessage(channel, command.getCommandDescription());
		}
	}
}
