package com.deliburd.bot.burdbot.commands.argumenttypes;

import java.util.Objects;
import java.util.Optional;

import com.deliburd.bot.burdbot.commands.ArgumentData;
import com.deliburd.bot.burdbot.commands.CommandArgumentFactory;
import com.deliburd.bot.burdbot.commands.CommandCall;

/**
 * The base class for all command arguments. To use a subclass of this in a bot command, the subclass must be marked with
 * the @ArgumentType annotation. With that annotation, the subclass must have a constructor containing an int argument, which
 * is the argument's index followed by a String argument which is the argument's data. The constructor should not throw an exception.
 * If a valid value cannot be retrieved from the String, then the value should be set to null and checked for in isPossiblyValid().
 * 
 * @param <T> The value stored inside the command argument
 * 
 * @author DELIBURD
 */
public abstract class CommandArgument<T> {
	private final CommandCall commandCall;
	private final int argumentIndex;
	private final ArgumentData argumentData;
	
	/**
	 * Creates a new command argument.
	 * 
	 * @param commandCall The command call that prompted the argument to be created. Cannot be null.
	 * @param argumentIndex The new command argument's index. Cannot be null.
	 * @param argumentData The argument's data. Cannot be null.
	 */
	CommandArgument(CommandCall commandCall, int argumentIndex, ArgumentData argumentData) {
		this.commandCall = Objects.requireNonNull(commandCall, "commandCall cannot be null");
		this.argumentIndex = Objects.requireNonNull(argumentIndex, "argumentIndex cannot be null");
		this.argumentData = Objects.requireNonNull(argumentData, "argumentData cannot be null");
	}
	
	/**
	 * Creates a new argument of the specified class with the same argument data as this one.
	 * 
	 * @param <U> The type that argumentClass belongs to.
	 * @param argumentClass The class of the argument to create with the same argument data.
	 * @return An optional command argument if it could be created and was possibly valid.
	 * @throws Throwable If the constructor being invoked for the argument throws an exception
	 */
	public <U extends CommandArgument<?>> Optional<U> asOtherArgument(Class<U> argumentClass) throws Throwable {
		U commandArgument = CommandArgumentFactory.generateCommandArgument(argumentClass, commandCall, argumentIndex, getArgumentDataString());
		
		return commandArgument.isPossiblyValid() ? Optional.of(commandArgument) : Optional.empty();
	}

	/**
	 * Gets the command argument's index.
	 * 
	 * @return The command argument's index
	 */
	public int getArgumentIndex() {
		return argumentIndex;
	}
	
	protected CommandCall getCommandCall() {
		return commandCall;
	}
	
	/**
	 * Gets the argument's data as a string. This is the string that is converted into a CommandArgument.
	 * 
	 * @return The argument's data as a string.
	 */
	public String getArgumentDataString() {
		return argumentData.getArgumentDataString();
	}
	
	/**
	 * Returns a string containing the name of the argument's class and its data as a string.
	 */
	@Override
	public String toString() {
		String className = getClass().getSimpleName();
		
		if(className.isEmpty()) {
			className = "Anonymous class";
		}
		
		return className + ": " + getArgumentDataString();
	}
	
	/**
	 * Checks whether the value of a CommandArgument is equal to the value of another.
	 * This method does not take into account anything else.
	 * This is equivalent to commandArgument.getValue().equals(commandArgument2.getValue())
	 * 
	 * @return Whether the 2 CommandArguments' values are equal.
	 */
	@Override
	public boolean equals(Object object) {
		if(this == object) {
			return true;
		} else if(object == null || !(object instanceof CommandArgument)) {
			return false;
		}
		
		CommandArgument<?> otherCommandArgument = (CommandArgument<?>) object;
		
		return getValue().equals(otherCommandArgument.getValue());
	}
	
	/**
	 * Gets whether the argument has a possibly valid value. This check must ensure that the value cannot be null if there's
	 * a possibility of that happening, but is free to check other things about the value as well. This method is run before being 
	 * passed to a bot command and is used to differentiate between other CommandArguments when there are multiple optional arguments 
	 * in a row. It is also used to ensure a possibly valid, non-null value is provided before the argument object is passed to a command's 
	 * method. This method should check the validity of the value as much as possible unless it would require blocking the thread for substantial 
	 * time as would be the case for interactions to Discord's REST API. If verification requires making a request to Discord's REST API, consider
	 * implementing IRestActionVerifiable. This method can return false after the argument is passed into the bot command method if some condition
	 * about the value isn't met.
	 * 
	 * @return Whether the argument has a non-null, possibly valid value.
	 */
	public abstract boolean isPossiblyValid();
	
	/**
	 * Gets the value held within the command argument. This value will be guaranteed to not be null by the time the argument
	 * object is passed into a command's method.
	 * 
	 * @return The value held within the command argument.
	 */
	public abstract T getValue();
}
