package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface MultiCommandAction {
	public void OnCommandRun(String[] args, MessageReceivedEvent event, MultiCommand command);
}
