package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.entities.MessageChannel;

public interface FinalCommandAction {
	public void OnCommandRun(MessageChannel channel);
}
