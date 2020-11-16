package com.deliburd.bot.burdbot;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.CommandModule;
import com.deliburd.bot.burdbot.commands.annotations.BotCommand;
import com.deliburd.bot.burdbot.commands.argumenttypes.TextChannelIDArgument;

public class TextFetcher extends CommandModule {
	public TextFetcher() {
		super("Text Fetcher", "Command(s) for fetching texts.");
	}
	
	@BotCommand
	public void fjweiofj(CommandCall commandCall, TextChannelIDArgument argument) {
		
	}
}
