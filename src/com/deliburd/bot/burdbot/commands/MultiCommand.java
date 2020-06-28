package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.BotUtil;
import com.deliburd.util.NodeTreeMap;

import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;

public class MultiCommand extends Command {
	/**
	 * A list of arguments for this command
	 * Each arraylist index is the argument's position
	 * Each HashMap contains an argument as the key along with its aliases as the value
	 * A null array within the HashMap means the argument has no aliases
	 * An empty HashMap within the ArrayList means the argument at the position can take any value.
	 * Is null after the command is finalized.
	 */
	private ArrayList<LinkedHashMap<String, String[]>> argumentList;
	
	/**
	 * Used to match aliases up to their base argument
	 * An empty HashMap means that that argument is variable
	 */
	private ArrayList<HashMap<String, String>> argumentAliasLookup;
	private NodeTreeMap<String, MultiCommandAction> commandArgumentMap;
	private MultiCommandAction baseAction;
	private MultiCommandAction defaultAction;
	private String[] argumentDescriptions;
	private long normalCooldown;
	private int minArguments = 0;
	private int maxArguments = 0;
	
	/**
	 * A bot command with additional arguments
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	MultiCommand(String command, String description) {
		super(command, description);
		normalCooldown = commandCooldown.getTotalCooldown();
		argumentList = new ArrayList<LinkedHashMap<String, String[]>>();
		argumentAliasLookup = new ArrayList<HashMap<String, String>>();
		commandArgumentMap = new NodeTreeMap<String, MultiCommandAction>();
	}
	
	@Override
	public Command setCooldown(long newCooldown) {
		super.setCooldown(newCooldown);
		normalCooldown = newCooldown;
		return this;
	}
	
	/**
	 * Sets the minimum number of arguments this command must have.
	 * This will give the user an invalid argument message if this requirement isn't met.
	 * By default, this is 0.
	 * This will not affect a base action when no arguments are typed, but will affect the command in other cases.
	 * 
	 * @param minArgumentCount The minimum number of arguments
	 * @return The changed MultiCommand
	 */
	public MultiCommand setMinArguments(int minArgumentCount) {
		minArguments = minArgumentCount;
		
		return this;
	}
	
	/**
	 * Sets the argument descriptions.
	 * 
	 * @param argumentDescriptions The argument descriptions.
	 * @return The changed MultiCommand
	 */
	public MultiCommand setArgumentDescriptions(String... argumentDescriptions) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		this.argumentDescriptions = argumentDescriptions;
		
		return this;
	}
	
	/**
	 * Sets the default action for when no action is specified in AddFinalArgumentPath
	 * 
	 * @param defaultAction The default action
	 * @return The changed MultiCommand
	 */
	public MultiCommand setDefaultAction(MultiCommandAction defaultAction) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		this.defaultAction = defaultAction;
		
		return this;
	}
	
	/**
	 * Sets the base action for when no arguments are given in the command.
	 * 
	 * @param baseAction The base action
	 * @return The changed MultiCommand
	 */
	public MultiCommand setBaseAction(MultiCommandAction baseAction) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		this.baseAction = baseAction;
		
		return this;
	}

	/**
	 * Adds an action to a given series of arguments.
	 * A null argument means it can accept anything.
	 * 
	 * @param action The action to run when the given set of arguments is typed
	 * @param arguments The series of arguments to which the action is linked.
	 * @return The changed Multicommand
	 */
	public MultiCommand addFinalArgumentPath(MultiCommandAction action, String... arguments) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		if(commandArgumentMap.FindValue(arguments) != null) {
			throw new RuntimeException("This argument path has already been filled.");
		}

		commandArgumentMap.AddValue(arguments, action);
		
		return this;
	}
	
	/**
	 * Adds an action to a given series of arguments using the default action.
	 * A null argument means that that argument can be anything.
	 * 
	 * @param arguments The series of arguments to which the action is linked.
	 * @return The changed Multicommand
	 */
	public MultiCommand addFinalArgumentPath(String... arguments) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		} else if(defaultAction == null) {
			throw new RuntimeException("There is no default action to use. You must use this method with an action specified.");
		}
		
		addFinalArgumentPath(defaultAction, arguments);

		return this;
	}
	
	/**
	 * Adds an argument that can take any value
	 * 
	 * 
	 * @param argumentPosition The argument's position with 0 as the first argument
	 * @return The changed MultiCommand
	 */
	public MultiCommand addArgument(int argumentPosition) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		// Ensure that the ArrayList has expanded enough to be able to have the map added
		ArrayUtil.ensureArrayListSize(argumentList, argumentPosition + 1);
		ArrayUtil.ensureArrayListSize(argumentAliasLookup, argumentPosition + 1);
		var argumentMap = argumentList.get(argumentPosition);
		var argumentAliasMap = argumentAliasLookup.get(argumentPosition);

		if(argumentMap == null && argumentAliasMap == null) {
			argumentMap = new LinkedHashMap<String, String[]>();
			argumentAliasMap = new HashMap<String, String>();
			argumentList.set(argumentPosition, argumentMap);
			argumentAliasLookup.set(argumentPosition, argumentAliasMap);
			
			final int argumentNumber = argumentPosition + 1;
			
			if(argumentNumber > maxArguments) {
				maxArguments = argumentNumber;
			}
		} else {
			throw new RuntimeException("This argument was already declared.");
		}
		
		return this;
	}
	
	/**
	 * Adds an argument with no aliases
	 * The order in which you add arguments is the order in which they appear in the help command
	 * 
	 * @param argumentPosition The argument's position with 0 as the first argument
	 * @param baseArgument The base argument which will be displayed as the main one in the help command
	 * @return The changed MultiCommand
	 */
	public MultiCommand addArgument(int argumentPosition, String baseArgument) {
		return addArgument(argumentPosition, baseArgument, null);
	}
	
	/**
	 * Adds an argument with aliases
	 * The order in which you add arguments is the order in which they appear in the help command
	 * 
	 * @param argumentPosition The argument's position with 0 as the first element
	 * @param baseArgument The base argument which will be displayed as the main one in the help command
	 * @param aliases The aliases for the base argument
	 * @return The changed MultiCommand
	 */
	public MultiCommand addArgument(int argumentPosition, String baseArgument, String[] aliases) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}

		// Ensure that the ArrayList has expanded enough to be able to have the map added
		ArrayUtil.ensureArrayListSize(argumentList, argumentPosition + 1);
		ArrayUtil.ensureArrayListSize(argumentAliasLookup, argumentPosition + 1);
		var argumentMap = argumentList.get(argumentPosition);
		var argumentAliasMap = argumentAliasLookup.get(argumentPosition);

		if(argumentMap == null && argumentAliasMap == null) {
			argumentMap = new LinkedHashMap<String, String[]>();
			argumentAliasMap = new HashMap<String, String>();
			
			argumentList.set(argumentPosition, argumentMap);
			argumentAliasLookup.set(argumentPosition, argumentAliasMap);
		} else if(argumentMap.containsKey(baseArgument)) {
			throw new RuntimeException("This base argument has already been declared at this position.");
		}
		
		argumentMap.put(baseArgument, aliases);
		argumentAliasMap.put(baseArgument, baseArgument);
		
		if(aliases != null) {
			for(int i = 0; i < aliases.length; i++) {
				argumentAliasMap.put(aliases[i], baseArgument);
			}
		}
		
		final int argumentNumber = argumentPosition + 1;
		
		if(argumentNumber > maxArguments) {
			maxArguments = argumentNumber;
		}
		
		return this;
	}

	@Override
	public void finalizeCommand() {
		super.finalizeCommand(true);
		
		final String description = getCommandDescription().substring(0, getCommandDescription().length() - 3); //Gets rid of the ```;
		
		StringBuilder fullDescription = new StringBuilder(description);

		fullDescription.append("\n");
		
		for(int i = 0; i < argumentList.size(); i++) {
			fullDescription.append("\nArgument #");
			fullDescription.append(i + 1);
			fullDescription.append(": ");
			
			if(argumentDescriptions != null && argumentDescriptions.length > i) {
				fullDescription.append(argumentDescriptions[i]);
				fullDescription.append("\n");
			} else {
				fullDescription.append("No description provided.\n");
			}
			
			var argumentAliases = argumentList.get(i);
			
			if(argumentAliases == null) {
				throw new RuntimeException("Argument " + i + " not initialized.");
			} else if(argumentAliases.isEmpty()) {
				fullDescription.append("\n");
			} else {
				for(var entry : argumentAliases.entrySet()) {
					fullDescription.append(entry.getKey());
					
					final var aliasArray = entry.getValue();
					
					if(aliasArray != null) {
						for(var alias : aliasArray) {
							fullDescription.append(", ");
							fullDescription.append(alias);
						}
					}
					
					fullDescription.append("\n");
				}
			}
		}
		
		fullDescription.append("```");
		commandDescription = fullDescription.toString();
		argumentList = null; //We don't need this anymore
		isFinalized = true;
	}
	
	/**
	 * Gives a message for invalid or no arguments
	 * 
	 * @param channel The channel to give the message in
	 */
	public void giveInvalidArgumentMessage(MessageChannel channel) {
		StringBuilder invalidArgumentMessage = new StringBuilder("Invalid or no arguments provided.\n");
		invalidArgumentMessage.append(getCommandDescription());
		BotUtil.sendMessage(channel, invalidArgumentMessage);
		commandCooldown.changeTotalCooldown(Constant.DEFAULT_COOLDOWN);
	}

	@Override
	void onCommandCalled(String[] args, MessageChannel channel, User user) {
		if(args == null) {
			if(baseAction == null) { // There was no base action specified, so this is invalid
				giveInvalidArgumentMessage(channel);
			} else if(commandCooldown.isCooldownOver(user)) {
				baseAction.OnCommandRun(null, channel);
				commandCooldown.changeTotalCooldown(normalCooldown);
			}

			return;
		} else if(args.length < minArguments || args.length > maxArguments) {
			giveInvalidArgumentMessage(channel);
			return;
		}
		
		var parsedArgs = convertAliasesToBaseArgumentsAndParse(args);
		
		MultiCommandAction action = commandArgumentMap.FindValue(parsedArgs);
		
		if(action == null) {
			giveInvalidArgumentMessage(channel);
		} else if(commandCooldown.isCooldownOver(user)) {
			action.OnCommandRun(args, channel);
			commandCooldown.changeTotalCooldown(normalCooldown);
		}
	}
	
	/**
	 * Turns the aliases of an argument array into base arguments
	 * Then it parses the arguments by making all variable arguments null in a copy of the args array.
	 * 
	 * @param args The arguments
	 * @return The parsed arguments
	 */
	private String[] convertAliasesToBaseArgumentsAndParse(String[] args) {
		String[] parsedArgs = null;
		
		for(int i = 0; i < args.length; i++) {
			var aliasLookup = argumentAliasLookup.get(i);
			// Is not a variable argument
			if(!aliasLookup.isEmpty()) {
				args[i] = aliasLookup.get(args[i]);
			} else {
				if(parsedArgs == null) {
					parsedArgs = args.clone();
				}
				
				parsedArgs[i] = null;
			}
		}
		
		if(parsedArgs == null) {
			return args;
		}
		
		return parsedArgs;
	}
}
