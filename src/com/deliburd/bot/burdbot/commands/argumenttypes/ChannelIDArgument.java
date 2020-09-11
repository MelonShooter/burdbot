package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.bot.burdbot.Main;
import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.MultiCommand;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.MessageChannel;

public abstract class ChannelIDArgument extends LongArgument {
	private ChannelType channelType;
	private boolean isValidChannel = true;
	
	public ChannelIDArgument(int argumentIndex, CommandArgumentType argumentType, long channelID) {
		super(argumentIndex, argumentType, channelID);
	}

	public ChannelType getChannelType() {
		if(channelType == null && isValidChannel()) {
			GuildChannel channel = getChannel();
			
			channelType = channel.getType();
		}
		
		return channelType;
	}
	
	public boolean isValidChannel() {
		if(isValidChannel) {
			isValidChannel = getChannel() != null;
		}
		
		return isValidChannel;
	}
	
	public boolean isValidChannel(long serverID) {
		boolean isChannelValid;
		
		if(isValidChannel) {
			GuildChannel channel = getChannel();
			isChannelValid = getChannel() != null && channel.getGuild().getIdLong() == serverID;
		} else {
			isChannelValid = false;
		}
		
		return isChannelValid;
	}
	
	public boolean isValidChannelOrNotify(CommandCall commandCall, boolean checkServer) {
		boolean isChannelValid;
		
		if(checkServer) {
			isChannelValid = isValidChannel(commandCall.getCommandEvent().getGuild().getIdLong());
		} else {
			isChannelValid = isValidChannel();
		}
		
		if(!isChannelValid) {
			giveInvalidChannelIDMessage(commandCall);
		}
		
		return isValidChannel;
	}
	
	public GuildChannel getChannel() {
		return Main.getJDAInstance().getGuildChannelById(getLong());
	}
	
	public GuildChannel getChannelOrNotify(CommandCall commandCall, boolean checkServer) {
		GuildChannel channel = getChannel();
		long commandServerID = commandCall.getCommandEvent().getGuild().getIdLong();
		
		if(channel == null || checkServer && channel.getGuild().getIdLong() != commandServerID) {
			giveInvalidChannelIDMessage(commandCall);
			channel = null;
		}
		
		return channel;
	}
	
	public long getChannelID() {
		return getLong();
	}
	
	private void giveInvalidChannelIDMessage(CommandCall commandCall) {
		MessageChannel channelToSendMessage = commandCall.getCommandEvent().getChannel();
		MultiCommand command = commandCall.getMultiCommand();
		command.giveInvalidArgumentMessage(channelToSendMessage, "Invalid channel ID given.");
	}
}
