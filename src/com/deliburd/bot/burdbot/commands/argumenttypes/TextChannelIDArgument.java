package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Optional;

import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.annotations.ArgumentType;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.TextChannel;

@ArgumentType(argumentTypeName = "Text Channel ID")
public class TextChannelIDArgument extends GuildChannelIDArgument {
	public TextChannelIDArgument(CommandCall commandCall, int argumentIndex, String channelIDString) {
		super(commandCall, argumentIndex, channelIDString);
	}
	
	@Override
	public ChannelType getChannelType() {
		return ChannelType.TEXT;
	}
	
	/**
	 * Gets a TextChannel from the channel ID. This will return an empty optional if the channel doesn't or no longer exists.
	 *
	 * @return The optional TextChannel from the channel ID.
	 * @see TextChannel
	 */
	@Override
	public Optional<? extends TextChannel> getObjectFromID() {
		JDA JDA = getCommandCall().getCommandEvent().getJDA();
		
		return Optional.ofNullable(JDA.getTextChannelById(getValue()));
	}
}
