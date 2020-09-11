package com.deliburd.bot.burdbot.commands.argumenttypes;

import com.deliburd.bot.burdbot.Main;
import com.deliburd.bot.burdbot.commands.CommandCall;
import com.deliburd.bot.burdbot.commands.MultiCommand;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class ServerIDArgument extends LongArgument {
	private boolean isValidServer = true;
	
	public ServerIDArgument(int argumentIndex, long serverID) {
		super(argumentIndex, CommandArgumentType.SERVERID, serverID);
	}

	public long getServerID() {
		return getLong();
	}
	
	public boolean isValidServer() {
		if(isValidServer) {
			isValidServer = getServer() != null;
		}
		
		return isValidServer;
	}
	
	public boolean isValidServerOrNotify(CommandCall commandCall) {
		if(!isValidServer()) {
			giveInvalidServerIDMessage(commandCall);
		}
		
		return isValidServer;
	}

	public Guild getServer() {
		Guild server = Main.getJDAInstance().getGuildById(getServerID());
		
		return server;
	}
	
	public Guild getServerOrNotify(CommandCall commandCall) {
		Guild server = getServer();
		
		if(server == null) {
			giveInvalidServerIDMessage(commandCall);
		}
		
		return server;
	}
	
	private void giveInvalidServerIDMessage(CommandCall commandCall) {
		MessageChannel channelToSendMessage = commandCall.getCommandEvent().getChannel();
		MultiCommand command = commandCall.getMultiCommand();
		command.giveInvalidArgumentMessage(channelToSendMessage, "Invalid server ID given.");
	}
}
