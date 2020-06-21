package com.deliburd.bot.burdbot.commands;

import net.dv8tion.jda.api.entities.MessageChannel;

public interface MultiCommandAction {
	public void OnCommandRun(String[] args, MessageChannel channel);
}
