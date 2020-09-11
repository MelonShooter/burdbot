package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.bot.burdbot.Main;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;

public class TextChannelIDArgument extends ChannelIDArgument {
	TextChannelIDArgument(int argumentIndex, long channelID) {
		super(argumentIndex, CommandArgumentType.TEXTCHANNELID, channelID);
	}
	
	@Override
	public ChannelType getChannelType() {
		return ChannelType.TEXT;
	}
	
	@Override
	public TextChannel getChannel() {
		return Main.getJDAInstance().getTextChannelById(getChannelID());
	}
}
