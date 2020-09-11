package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CommandCall {
	private final MessageReceivedEvent commandEvent;
	private final MultiCommand multiCommand;
	private final FinalCommand finalCommand;
	
	public CommandCall(MessageReceivedEvent commandEvent, MultiCommand command) {
		this.commandEvent = commandEvent;
		this.multiCommand = command;
		finalCommand = null;
	}
	
	public CommandCall(MessageReceivedEvent commandEvent, FinalCommand command) {
		this.commandEvent = commandEvent;
		multiCommand = null;
		finalCommand = command;
	}

	public MessageReceivedEvent getCommandEvent() {
		return commandEvent;
	}
	
	public Command getCommand() {
		if(multiCommand == null) {
			return finalCommand;
		} else {
			return multiCommand;
		}
	}

	public MultiCommand getMultiCommand() {
		return multiCommand;
	}
	
	public FinalCommand getFinalCommand() {
		return finalCommand;
	}
}
