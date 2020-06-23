package com.deliburd.bot.burdbot.commands;

import java.util.ArrayList;
import java.util.HashMap;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.bot.burdbot.util.ArrayUtil;
import com.deliburd.bot.burdbot.util.NodeTreeMap;

import net.dv8tion.jda.api.entities.MessageChannel;

public class MultiCommand extends Command {
	/**
	 * A list of arguments for this command
	 * Each arraylist index is the argument's position
	 * Each HashMap contains an argument as the key along with its aliases as the value
	 * A null array within the HashMap means the argument has no aliases
	 * An empty HashMap within the ArrayList means the argument at the position can take any value.
	 */
	private ArrayList<HashMap<String, String[]>> argumentList;
	private NodeTreeMap<String, MultiCommandAction> commandArgumentMap;
	private MultiCommandAction baseAction;
	private MultiCommandAction defaultAction;
	private String[] argumentDescriptions;
	private long normalCooldown;
	
	/**
	 * A bot command with additional arguments
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	MultiCommand(String command, String description) {
		super(command, description);
		argumentList = new ArrayList<HashMap<String, String[]>>();
		commandArgumentMap = new NodeTreeMap<String, MultiCommandAction>();
	}
	
	@Override
	public Command setCooldown(long newCooldown) {
		super.setCooldown(newCooldown);
		normalCooldown = newCooldown;
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
	 * @throws 
	 */
	public MultiCommand addFinalArgumentPath(MultiCommandAction action, String... arguments) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}
		
		var argumentsWithAliases = new String [arguments.length][];
		
		for(int i = 0; i < arguments.length; i++) {
			final var argument = arguments[i];
			final var argumentMap = argumentList.get(i);
			String[] argumentArray;
			
			if(argument == null) {
				if(argumentMap.isEmpty()) { // Means it can accept any arguments
					argumentArray = new String[1];
					argumentArray[0] = null;
				} else { // Shouldn't be null
					throw new RuntimeException("Argument " + i + " was null and not a variable argument.");
				}
 			} else if (argument.isBlank()) { //An argument was blank
				throw new RuntimeException("Argument " + i + " was blank.");
			} else if(argumentMap == null) { //No argument declared at this index
				throw new RuntimeException("Argument " + i + " wasn't declared");
			} else if(argumentMap.containsKey(argument)) {
				if(argumentMap.get(argument) == null) { // Must have no aliases
					argumentArray = new String[1];
					argumentArray[0] = argument;
				} else { // Has aliases
					var argumentAliases = argumentMap.get(argument);
					argumentArray = new String[1 + argumentAliases.length];
					argumentArray[0] = argument;
					
					for(int j = 1; j < argumentArray.length; j++) {
						argumentArray[j] = argumentAliases[j - 1];
					}
				}
			} else { // This argument doesn't exist
				throw new RuntimeException("The argument " + argument + " doesn't exist.");
			}
			
			argumentsWithAliases[i] = argumentArray;
		}
		
		for(String[] argumentAliasCombination : ArrayUtil.findCrossArrayStringCombinations(argumentsWithAliases)) {
			commandArgumentMap.AddValue(argumentAliasCombination, action);
		}
		
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
	 * @param argumentPosition The argument's position with 0 as the first argument
	 * @return The changed MultiCommand
	 */
	public MultiCommand addArgument(int argumentPosition) {
		if(isFinalized) {
			throw new RuntimeException("This command has already been finalized.");
		}

		ArrayUtil.ensureArrayListSize(argumentList, argumentPosition + 1);
		var argumentMap = argumentList.get(argumentPosition);

		if(argumentMap == null) {
			argumentMap = new HashMap<String, String[]>();
			argumentList.add(argumentPosition, argumentMap);
		} else {
			throw new RuntimeException("This argument was already declared.");
		}
		
		return this;
	}
	
	/**
	 * Adds an argument with no aliases
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

		ArrayUtil.ensureArrayListSize(argumentList, argumentPosition + 1);
		var argumentMap = argumentList.get(argumentPosition);

		if(argumentMap == null) {
			argumentMap = new HashMap<String, String[]>();
			argumentList.add(argumentPosition, argumentMap);
		} else {
			throw new RuntimeException("This argument was already declared.");
		}

		argumentMap.put(baseArgument, aliases);
		
		return this;
	}

	@Override
	public void finalizeCommand() {
		super.finalizeCommand(true);
		
		StringBuilder fullDescription = new StringBuilder(commandDescription.substring(3)); //Gets rid of the ```

		fullDescription.append("\n");
		
		for(int i = 0; i < argumentList.size(); i++) {
			fullDescription.append("\nArgument #");
			fullDescription.append(i);
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
		isFinalized = true;
	}
	
	/**
	 * Gives a message for invalid or no arguments
	 */
	public void giveInvalidArgumentMessage(MessageChannel channel) {
		StringBuilder invalidArgumentMessage = new StringBuilder("Invalid or no arguments provided.\n```");
		invalidArgumentMessage.append(commandDescription);
		invalidArgumentMessage.append("```");
		channel.sendMessage(invalidArgumentMessage);
		commandCooldown.changeTotalCooldown(Constant.DEFAULT_COOLDOWN);
	}

	@Override
	void onCommandCalled(String[] args, MessageChannel channel) {
		if(args == null) {
			if(baseAction == null) { // There was no base action specified, so this is invalid
				giveInvalidArgumentMessage(channel);
			} else {
				baseAction.OnCommandRun(null, channel);
			}
		}
		
		MultiCommandAction action = commandArgumentMap.FindValue(args);

		if(action == null) {
			giveInvalidArgumentMessage(channel);
		} else if (commandCooldown.isCooldownOver()) {
			action.OnCommandRun(args, channel);
			commandCooldown.changeTotalCooldown(normalCooldown);
		}
	}
}
