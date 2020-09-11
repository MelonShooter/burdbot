package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.bot.burdbot.Main;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.VoiceChannel;

public class VoiceChannelIDArgument extends ChannelIDArgument {

	VoiceChannelIDArgument(int argumentIndex, long channelID) {
		super(argumentIndex, CommandArgumentType.VOICECHANNELID, channelID);
	}

	@Override
	public ChannelType getChannelType() {
		return ChannelType.VOICE;
	}
	
	@Override
	public VoiceChannel getChannel() {
		return Main.getJDAInstance().getVoiceChannelById(getChannelID());
	}
}
