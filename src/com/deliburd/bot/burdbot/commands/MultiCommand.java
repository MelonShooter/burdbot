package com.deliburd.bot.burdbot.commands;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.bot.burdbot.util.ArrayUtil;
import com.deliburd.bot.burdbot.util.NodeTreeMap;

import net.dv8tion.jda.api.entities.MessageChannel;

public class MultiCommand extends Command {
	private NodeTreeMap<String, MultiCommandAction> commandArgumentMap;
	private MultiCommandAction defaultAction;
	private String[] argumentDescriptions;
	private String[][] aliases;
	
	/**
	 * A bot command with additional arguments
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 */
	MultiCommand(String command, String description) {
		super(command, description);
		commandArgumentMap = new NodeTreeMap<String, MultiCommandAction>();
	}
	
	/**
	 * A bot command with additional arguments
	 * 
	 * @param command The command's name
	 * @param description The description of the command for the help.
	 * @param defaultAction The action to default to when there's no MultiCommandAction specified in addFinalArgumentPath()
	 */
	MultiCommand(String command, String description, MultiCommandAction defaultAction) {
		super(command, description);
		commandArgumentMap = new NodeTreeMap<String, MultiCommandAction>();
		this.defaultAction = defaultAction;
	}
	
	/**
	 * Sets the argument descriptions.
	 * 
	 * @param argumentDescriptions The argument descriptions.
	 * @return The changed Multicommand
	 */
	public MultiCommand setArgumentDescriptions(String[] argumentDescriptions) {
		this.argumentDescriptions = argumentDescriptions;
		
		return this;
	}

	/**
	 * Adds an action to a given series of arguments
	 * 
	 * @param action The action to run when the given set of arguments is typed
	 * @param arguments The series of arguments to which the action is linked. Each array is a series of arguments as an array of aliases.
	 * @return The changed Multicommand
	 */
	public MultiCommand addFinalArgumentPath(MultiCommandAction action, String[]... arguments) {
		for(String[] argumentAliasCombination : ArrayUtil.findCrossArrayStringCombinations(arguments)) {
			commandArgumentMap.AddValue(argumentAliasCombination, action);
		}
		//CHANGE THIS TO NOT GET ALIASES IN HERE.
		//INSTEAD, HAVE A SEPARATE FUNCTION CALLED CREATEARGUMENT
		//WHERE AN ARRAY CAN BE TAKEN OF THE BASE ARGUMENT AND ALIASES
		//THERE WILL BE A HASHMAP LINKING THE BASE ARGUMENT TO AN ARRAY OF THE BASE ARGUMENT AND ALL ALIASES
		//THEN BASE ARGUMENTS ARE TAKEN IN THROUGH THIS METHOD AND ALL ALIASES ARE FOUND OF EACH ARGUMENT AND PASSED THROUGH
		
		aliases = arguments;
		
		return this;
	}
	
	/**
	 * Adds an action to a given series of arguments
	 * 
	 * @param arguments The series of arguments to which the action is linked. Each array is a series of arguments as an array of aliases.
	 * @return The changed Multicommand
	 */
	public MultiCommand addFinalArgumentPath(String[]... arguments) {
		for(String[] argumentAliasCombination : ArrayUtil.findCrossArrayStringCombinations(arguments)) {
			commandArgumentMap.AddValue(argumentAliasCombination, defaultAction);
		}
		
		aliases = arguments;
		
		return this;
	}

	@Override
	public void finalizeDescription() {
		StringBuilder fullDescription = new StringBuilder("Command: ");
		fullDescription.append(Constant.COMMAND_PREFIX_WITH_SPACE);
		fullDescription.append(getCommandName());
		
		for (int i = 0; i < argumentDescriptions.length; i++) {
			var argumentDescription = argumentDescriptions[i];
			
			fullDescription.append("\n");
			fullDescription.append("Argument ");
			fullDescription.append(i + 1);
			fullDescription.append(": ");
		    fullDescription.append(aliases[i][0]);
		    fullDescription.append(" - ");
		    if(argumentDescription == null) {
		    	fullDescription.append("No description provided.");
		    } else {
		    	fullDescription.append(argumentDescription);
		    }

		    fullDescription.append("\nAliases: ");

			for (int j = 1; j < aliases[i].length; j++) {
				fullDescription.append(Constant.COMMAND_PREFIX_WITH_SPACE);
				fullDescription.append(aliases[i][j]);

				if (i != aliases.length - 1) {
					fullDescription.append(",");
				}
			}
		}
		
		commandDescription = fullDescription.toString();
	}
	
	/**
	 * Gives a message for invalid or no arguments
	 */
	private void giveInvalidArgumentMessage(MessageChannel channel) {
		StringBuilder invalidArgumentMessage = new StringBuilder("Invalid or no arguments provided.\n```");
		invalidArgumentMessage.append(commandDescription);
		invalidArgumentMessage.append("```");
		channel.sendMessage(invalidArgumentMessage);
	}

	@Override
	void onCommandCalled(String[] args, MessageChannel channel) {
		if(args == null) {
			giveInvalidArgumentMessage(channel);
		}
		
		MultiCommandAction action = commandArgumentMap.FindValue(args);

		if(action == null) {
			giveInvalidArgumentMessage(channel);
		} else {
			action.OnCommandRun(args, channel);
		}
	}
}
