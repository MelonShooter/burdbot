package com.deliburd.bot.burdbot.commands;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.Predicate;

import com.deliburd.bot.burdbot.commands.argumenttypes.BigDecimalArgument;
import com.deliburd.bot.burdbot.commands.argumenttypes.BigIntegerArgument;
import com.deliburd.bot.burdbot.commands.argumenttypes.CommandArgument;
import com.deliburd.bot.burdbot.commands.argumenttypes.DoubleArgument;
import com.deliburd.bot.burdbot.commands.argumenttypes.LongArgument;
import com.deliburd.bot.burdbot.commands.argumenttypes.TextArgument;
import com.deliburd.util.NumberUtil;

import net.dv8tion.jda.api.entities.MessageChannel;

public class ArgumentChecker {
	private ArgumentChecker() {}
	
	public static <T extends LongArgument> void requireMinMax(CommandCall commandCall, T argument, long minimum, long maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		long argumentValue = argument.getLong();
		
		if(argumentValue < minimum || argumentValue > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is not in the required range. (")
					.append(minimum)
					.append(" <= argument <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends LongArgument> void requireMin(CommandCall commandCall, T argument, long minimum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		long argumentValue = argument.getLong();
		
		if(argumentValue < minimum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append(minimum)
					.append(" <= argument")
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends LongArgument> void requireMax(CommandCall commandCall, T argument, long maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		long argumentValue = argument.getLong();
		
		if(argumentValue > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append("argument <=")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMinMax(CommandCall commandCall, DoubleArgument argument, double minimum, double maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		double argumentValue = argument.getDouble();
		
		if(argumentValue < minimum || argumentValue > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is not in the required range. (")
					.append(minimum)
					.append(" <= argument <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMin(CommandCall commandCall, DoubleArgument argument, double minimum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		double argumentValue = argument.getDouble();
		
		if(argumentValue < minimum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append(minimum)
					.append(" <= argument")
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMax(CommandCall commandCall, DoubleArgument argument, double maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		double argumentValue = argument.getDouble();
		
		if(argumentValue > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append("argument <=")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMinMax(CommandCall commandCall, BigIntegerArgument argument, BigInteger minimum, BigInteger maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigInteger argumentValue = argument.getBigInteger();
		
		if(argumentValue.compareTo(minimum) < 0 || argumentValue.compareTo(minimum) > 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is not in the required range. (")
					.append(minimum)
					.append(" <= argument <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMin(CommandCall commandCall, BigIntegerArgument argument, BigInteger minimum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigInteger argumentValue = argument.getBigInteger();
		
		if(argumentValue.compareTo(minimum) < 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append(minimum)
					.append(" <= argument")
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMax(CommandCall commandCall, BigIntegerArgument argument, BigInteger maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigInteger argumentValue = argument.getBigInteger();
		
		if(argumentValue.compareTo(maximum) > 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append("argument <=")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMinMax(CommandCall commandCall, BigDecimalArgument argument, BigDecimal minimum, BigDecimal maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigDecimal argumentValue = argument.getBigDecimal();
		
		if(argumentValue.compareTo(minimum) < 0 || argumentValue.compareTo(minimum) > 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is not in the required range. (")
					.append(minimum)
					.append(" <= argument <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMin(CommandCall commandCall, BigDecimalArgument argument, BigDecimal minimum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigDecimal argumentValue = argument.getBigDecimal();
		
		if(argumentValue.compareTo(minimum) < 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append(minimum)
					.append(" <= argument")
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static void requireMax(CommandCall commandCall, BigDecimalArgument argument, BigDecimal maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		BigDecimal argumentValue = argument.getBigDecimal();
		
		if(argumentValue.compareTo(maximum) > 0) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("does not meet the required minimum. (")
					.append("argument <=")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends TextArgument> void requireMinMaxCharacters(CommandCall commandCall, T argument, long minimum, long maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		String argumentValue = argument.getText();
		
		if(argumentValue.length() < minimum || argumentValue.length() > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is out of the required character range. (")
					.append(minimum)
					.append(" <= character count <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends TextArgument> void requireMinCharacters(CommandCall commandCall, T argument, long minimum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		String argumentValue = argument.getText();
		
		if(argumentValue.length() < minimum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is out of the required character range. (")
					.append(minimum)
					.append(" <= character count")
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends TextArgument> void requireMaxCharacters(CommandCall commandCall, T argument, long maximum) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		String argumentValue = argument.getText();
		
		if( argumentValue.length() > maximum) {
			String outOfBoundsMessage = new StringBuilder(getArgumentPrettyIndex(argument))
					.append("is out of the required character range. (")
					.append("character count <= ")
					.append(maximum)
					.append(")")
					.toString();
			
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, outOfBoundsMessage);
		}
	}
	
	public static <T extends CommandArgument> void require(CommandCall commandCall, T argument, Predicate<? super T> requirement, String requirementMessage) {
		MessageChannel channel = commandCall.getCommandEvent().getChannel();
		
		if(requirement.test(argument)) {
			commandCall.getMultiCommand().giveInvalidArgumentMessage(channel, requirementMessage);
		}
	}
	
	private static String getArgumentPrettyIndex(CommandArgument argument) {
		return "The " + NumberUtil.toOrdinalNumber(argument.getArgumentIndex() + 1) + " argument ";
	}
}
