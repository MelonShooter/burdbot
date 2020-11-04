package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Optional;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.VoiceChannel;

@ArgumentType(argumentTypeName = "Voice Channel ID")
public class VoiceChannelIDArgument extends GuildChannelIDArgument {
	public VoiceChannelIDArgument(CommandCall commandCall, int argumentIndex, String channelIDString) {
		super(commandCall, argumentIndex, channelIDString);
	}
	
	@Override
	public ChannelType getChannelType() {
		return ChannelType.VOICE;
	}
	
	/**
	 * Gets a VoiceChannel from the channel ID. This will return an empty optional if the channel doesn't or no longer exists.
	 *
	 * @return The optional VoiceChannel from the channel ID.
	 * @see VoiceChannel
	 */
	@Override
	public Optional<? extends VoiceChannel> getObjectFromID() {
		JDA JDA = getCommandCall().getCommandEvent().getJDA();
		
		return Optional.ofNullable(JDA.getVoiceChannelById(getValue()));
	}
}
