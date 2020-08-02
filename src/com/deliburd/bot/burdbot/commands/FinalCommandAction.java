package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface FinalCommandAction {
	public void OnCommandRun(MessageReceivedEvent event);
}
