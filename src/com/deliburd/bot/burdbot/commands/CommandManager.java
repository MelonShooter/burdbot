package com.deliburd.bot.burdbot.commands;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.bot.burdbot.commands.annotations.BotCommand;
import com.deliburd.bot.burdbot.commands.annotations.CheckPermission;
import com.deliburd.util.BotUtil;
import com.deliburd.util.Cooldown;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.Pair;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandManager extends ListenerAdapter {
	public static final String NO_DESCRIPTION_TEXT = "No description provided.";
	
	/**
	 * Links each command manager's prefix to the manager itself.
	 */
	private static final ConcurrentHashMap<String, CommandManager> prefixToCommandManagerMap = new ConcurrentHashMap<>();
	
	/**
	 * An alphabetical map of modules linked to an list of their commands.
	 */
	private final NavigableSet<CommandModule> commandModuleSet;
	
	/**
	 * A set of the registered commands.
	 */
	private final Set<Command> commandSet;
	
	/**
	 * Maps all command names and aliases to the command
	 */
	private final ConcurrentHashMap<String, Command> commandNameLookup;

	private final String prefix;
	private final JDA JDA;
	private final JDABuilder JDABuilder;
	private final CommandModule helpModule;
	private final String helpDescription;
	private volatile boolean isInitialized;
	
	/**
	 * Creates a CommandManager that has its commands registered before JDA starts
	 * 
	 * @param prefix The command manager's prefix to use for all commands registered in it
	 * @param helpDescription The description of the help command for this command manager
	 * @param helpModule The module to put the help command into.
	 * @param builder The JDABuilder to register the commands for when the first command is added.
	 */
	public CommandManager(String prefix, String helpDescription, CommandModule helpModule, JDABuilder builder) {
		this.prefix = Objects.requireNonNull(prefix, "The prefix cannot be null");
		this.helpDescription = Objects.requireNonNull(helpDescription, "The help description cannot be null");
		this.JDA = null;
		this.JDABuilder = Objects.requireNonNull(builder, "The JDA builder cannot be null");
		this.helpModule = helpModule; 
		commandModuleSet = Collections.synchronizedNavigableSet(new TreeSet<>());
		commandSet = ConcurrentHashMap.newKeySet();
		commandNameLookup = new ConcurrentHashMap<>();
	}
	
	/**
	 * Creates a CommandManager that has its commands registered after JDA starts
	 * 
	 * @param prefix The command manager's prefix to use for all commands registered in it
	 * @param helpDescription The description of the help command for this command manager
	 * @param helpModule The module to put the help command into.
	 * @param JDA The JDA instance to register the commands for when the first command is added.
	 */
	public CommandManager(String prefix, String helpDescription, CommandModule helpModule, JDA JDA) {
		this.prefix = Objects.requireNonNull(prefix, "The prefix cannot be null");
		this.helpDescription = Objects.requireNonNull(helpDescription, "The help description cannot be null");
		this.JDA = Objects.requireNonNull(JDA, "The JDA instance cannot be null");
		this.JDABuilder = null;
		this.helpModule = helpModule;
		commandModuleSet = new ConcurrentSkipListSet<>();
		commandSet = ConcurrentHashMap.newKeySet();
		commandNameLookup = new ConcurrentHashMap<>();
	}
	
	/**
	 * Adds the provided modules to the CommandManager. Does nothing if a CommandModule with the same name was already added.
	 * 
	 * @param modules The modules to add.
	 */
	public void addModules(CommandModule... modules) {
		for(CommandModule module : modules) {
			addModule(module);
		}
	}

	private void addModule(CommandModule module) {
		if(!commandModuleSet.add(module)) {
			return;
		}
		
		Method[] moduleMethods = module.getClass().getDeclaredMethods();
		var permissionMethodMap = new HashMap<String, Pair<Method, Boolean>>();
		
		for(Method permissionMethod : moduleMethods) {
			CheckPermission permissionAnnotation = permissionMethod.getAnnotation(CheckPermission.class);
			
			if(permissionAnnotation != null) {
				boolean overrideModulePermissionCheck = permissionAnnotation.overrideModulePermissionCheck();
				permissionMethodMap.put(permissionMethod.getName().toLowerCase(), new Pair<>(permissionMethod, overrideModulePermissionCheck));
			}
		}
		
		for(Method botCommand : moduleMethods) {
			BotCommand commandAnnotation = botCommand.getAnnotation(BotCommand.class);
			
			if(commandAnnotation != null) {	
				Command newCommand = addCommand(module, commandAnnotation, botCommand, permissionMethodMap);
				
				newCommand.setPermissionRestrictions(commandAnnotation.requiredPermissions())
						.addCommandNames(commandAnnotation.commandAliases())
						.setCooldown(commandAnnotation.cooldown())
						.finalizeCommand();
			}
		}
	}
	
	/*
	 * Aliases will be dealt with via enums. Checking arguments will be dealt with through the ArgumentChecker.
	 */
	

	/**
	 * Gets an umodifiable NavigableSet of the command modules for this manager
	 * 
	 * @return An unmodifiable NavigableSet of the command modules for this manager
	 */
	public NavigableSet<CommandModule> getCommandModules() {
		return Collections.unmodifiableNavigableSet(commandModuleSet);
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
	 * @param module The module to put the command in.
	 * @param commandInfo The command's info.
	 * @param action The action to run when the command is typed.
	 * @param permissionCheck The check to run for permissions
	 * @param ignoreModulePermissionCheck Whether to ignore the module's permission check
	 * @return The new FinalCommand
	 */
	private Command addCommand(CommandModule module, BotCommand commandInfo, Method action, Map<String, Pair<Method, Boolean>> permissionMap) {
		if(!isInitialized) {
			initializeManager();
		}
		
		var permissionInfo = permissionMap.get(action.getName());
		
		if(!action.trySetAccessible()) {
			ErrorLogger.LogIssue("Couldn't set the command method to be accessible.");
			return null;
		} else if(permissionInfo != null && !permissionInfo.getKey().trySetAccessible()) {
			ErrorLogger.LogIssue("Couldn't set the permission check method to be accessible.");
			return null;
		}
		
		String commandName = getCommandName(commandInfo, action);					
		String commandDescription = commandInfo.commandDescription();
		int argumentCount = action.getParameterCount() - 1;
		Command newCommand;
		
		if(argumentCount == 0) {
			newCommand = addNewFinalCommand(module, action, permissionInfo, commandName, commandDescription);
		} else {
			String[] argumentDescriptions = commandInfo.argumentDescriptions();
			
			if(argumentDescriptions.length < argumentCount) {
				String[] fullArgumentDescriptions = Arrays.copyOf(argumentDescriptions, argumentCount);
				Arrays.fill(fullArgumentDescriptions, argumentDescriptions.length, fullArgumentDescriptions.length, NO_DESCRIPTION_TEXT);
				argumentDescriptions = fullArgumentDescriptions;
			}
			
			newCommand = addNewMultiCommand(module, action, permissionInfo, commandName, commandDescription)
					.setArgumentDescriptions(argumentDescriptions);
		}

		registerCommand(module, commandName, newCommand);
		
		return newCommand;
	}
	
	private MultiCommand addNewMultiCommand(CommandModule module, Method action, Pair<Method, Boolean> permissionInfo, String commandName, String commandDescription) {
		MultiCommandAction defaultAction = (args, event, command) -> {
			CommandCall commandCall = new CommandCall(event, command);

			if(permissionInfo != null) {
				runMultiCommand(args, commandCall, action, permissionInfo.getKey(), permissionInfo.getValue());
			} else {
				runMultiCommand(args, commandCall, action, null, null);
			}
		};
		
		MultiCommand command = new MultiCommand(module, prefix, commandName, commandDescription)
				.setDefaultAction(defaultAction);
		
		return command;
				
	}

	private void runMultiCommand(String[] args, CommandCall commandCall, Method action, Method permissionCheck, Boolean ignoreModulePermissionCheck) {
		if(!checkPermission(commandCall, permissionCheck, ignoreModulePermissionCheck)) {
			return;
		}
		//MethodHandle

		
		try {
			action.invoke(commandCall);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			ErrorLogger.LogException(e, commandCall.getCommandEvent().getChannel());
		}
	}

	private FinalCommand addNewFinalCommand(CommandModule module, Method action, Pair<Method, Boolean> permissionInfo, String commandName, String commandDescription) {
		return new FinalCommand(module, prefix, commandName, commandDescription, event -> {
			CommandCall commandCall = new CommandCall(event, (FinalCommand) commandNameLookup.get(commandName));
			
			if(permissionInfo != null) {
				runFinalCommand(commandCall, action, permissionInfo.getKey(), permissionInfo.getValue());
			} else {
				runFinalCommand(commandCall, action, null, null);
			}
		});
	}
	
	private String getCommandName(BotCommand commandAnnotation, Method botCommand) {
		String commandName = commandAnnotation.commandName();
		
		if(commandName.isEmpty()) {
			commandName = botCommand.getName().toLowerCase();
		}
		
		return commandName;
	}
	
	private void runFinalCommand(CommandCall commandCall, Method action, Method permissionCheck, Boolean ignoreModulePermissionCheck) {
		if(!checkPermission(commandCall, permissionCheck, ignoreModulePermissionCheck)) {
			return;
		}
		
		try {
			action.invoke(commandCall);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			ErrorLogger.LogException(e, commandCall.getCommandEvent().getChannel());
		}
	}
	
	private boolean checkPermission(CommandCall commandCall, Method permissionCheck, Boolean ignoreModulePermissionCheck) {
		MessageReceivedEvent event = commandCall.getCommandEvent();
		Command command = commandCall.getCommand();
		boolean isDELIBURD = event.getAuthor().getIdLong() == Constant.DELIBURD_ID;
		MessageChannel channel = event.getChannel();

		if(isDELIBURD) {
			return true;
		}
		
		boolean hasNoModulePermission = (ignoreModulePermissionCheck == null || !ignoreModulePermissionCheck) && !command.getCommandModule().hasPermission(commandCall);
		
		try {
			if(hasNoModulePermission || permissionCheck != null && !(boolean) permissionCheck.invoke(commandCall)) {
				command.giveInsufficientPermissionsMessage(channel);
				return false;
			}
			
			return true;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			ErrorLogger.LogException(e, channel);
			return false;
		}
	}

	/**
	 * Creates the help command.
	 * 
	 * @param module The module to put the command in.
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @return The help command.
	 */
	private MultiCommand addHelpCommand(CommandModule module, String command, String description) {
		if(!isInitialized) {
			initializeManager();
		}
		
		MultiCommand newCommand = new MultiCommand(module, prefix, command, description);
		registerCommand(module, command, newCommand);
		
		return newCommand;
	}
	
	/**
	 * Returns whether the command exists in this command manager
	 * 
	 * @param commandName The command's name or an alias of its name
	 * @return Whether the command exists in this command manager
	 */
	public boolean hasCommand(String commandName) {
		return commandNameLookup.containsKey(commandName);
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
			Command command = commandNameLookup.get(baseCommand);

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
		boolean isDELIBURD = event.getAuthor().getIdLong() == Constant.DELIBURD_ID;

		if(event.getMember().hasPermission(restrictions) || isDELIBURD) {
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
		} else if(hasCommand(alias)) {
			throw new RuntimeException("This alias is already a registered command name/alias.");
		}
		
		commandNameLookup.put(alias, command);
	}
	
	/**
	 * Initializes the manager and sets it up to receive event calls for messages
	 */
	private void initializeManager() {
		isInitialized = true;
		
		addHelpCommand(helpModule, Constant.HELP_COMMAND, helpDescription)
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

		for (var commandModule : commandModuleSet) {
			commandHelpString.append(commandModule.getModuleName())
					.append(" - ")
					.append(commandModule.getModuleDescription())
					.append("\n");

			var commandSet = commandModule.getCommands();
			
			for(Command command : commandSet) {
				if(hasPermission(command, event)) {
					commandHelpString.append("```")
							.append(Constant.COMMAND_PREFIX)
							.append(command.getCommandName())
							.append(" - ")
							.append(command.getShortCommandDescription())
							.append("```");
				}
			}
			
			commandHelpString.append("\n");
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
	 * Registers a command with the CommandManager. Throws an exception if a command with the same
	 * name has already been registered.
	 * 
	 * @param module The module to put the command into
	 * @param name The command's name
	 * @param command The command
	 */
	private void registerCommand(CommandModule module, String name, Command command) {
		if(commandNameLookup.putIfAbsent(name, command) != null) {
			return;
		}
		
		module.addCommand(command);
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
		Command command = commandNameLookup.get(args[0]);
		
		if(command == null) {
			helpCommand.giveInvalidArgumentMessage(channel);
			return;
		}
		
		if (!hasPermission(command, event)) {
			BotUtil.sendMessage(channel, "You don't have access to this command.");
		} else {
			BotUtil.sendMessage(channel, command.getCommandDescription());
		}
	}
}
