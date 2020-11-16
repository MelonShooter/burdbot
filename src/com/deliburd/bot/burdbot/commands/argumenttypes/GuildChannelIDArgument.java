package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Optional;

import com.deliburd.bot.burdbot.commands.CommandCall;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;

public abstract class GuildChannelIDArgument extends LongArgument implements IFullyCachedIDArgument<GuildChannel> {
	protected GuildChannelIDArgument(CommandCall commandCall, int argumentIndex, String channelIDString) {
		super(commandCall, argumentIndex, channelIDString);
	}
	
	/**
	 * Checks whether or not the channel ID provided was valid by checking if it is the ID of a known channel.
	 *
	 *@return Whether the channel ID for this argument is valid.
	 */
	@Override
	public boolean isPossiblyValid() {
		boolean isPossiblyValid = super.isPossiblyValid();
		
		if(isPossiblyValid) {
			isPossiblyValid = getObjectFromID().isPresent();
		}
		
		return isPossiblyValid;
	}
	
	/**
	 * Gets an optional GuildChannel from the channel ID. This will return an empty optional if the channel doesn't or no longer exists.
	 *
	 * @return The optional GuildChannel from the channel ID.
	 * @see GuildChannel
	 */
	@Override
	public Optional<? extends GuildChannel> getObjectFromID() {
		JDA JDA = getCommandCall().getCommandEvent().getJDA();
		
		return Optional.ofNullable(JDA.getGuildChannelById(getValue()));
	}
	
	public Optional<? extends GuildChannel> getObjectFromIDInServer() {
		Guild server = getCommandCall().getCommandEvent().getGuild();
		
		return Optional.ofNullable(server.getGuildChannelById(getValue()));
	}
	
	/**
	 * Gets the channel type of the channel ID this object should hold.
	 * 
	 * @return The channel type of the channel ID this object should hold.
	 */
	public abstract ChannelType getChannelType();
}
