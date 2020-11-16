package com.deliburd.bot.burdbot.commands;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.deliburd.bot.burdbot.commands.argumenttypes.CommandArgument;

public class CommandArgumentFactory {
	private static final Map<Class<? extends CommandArgument<?>>, MethodHandle> constructorHandleMap = new ConcurrentHashMap<>();
	
	private CommandArgumentFactory() {};
	
	public static <T extends CommandArgument<?>> T generateCommandArgument(Class<T> commandArgumentClass, CommandCall commandCall, int argumentIndex, String argumentData) throws Throwable {
		if(commandArgumentClass == CommandArgument.class) {
			throw new IllegalArgumentException("The generated command argument must be a subclass of CommandArgument, not CommandArgument itself.");
		}
		
		MethodHandle commandArgumentConstructorHandle = constructorHandleMap.get(commandArgumentClass);

		if(commandArgumentConstructorHandle == null) {
			commandArgumentConstructorHandle = MethodHandles.lookup().findConstructor(commandArgumentClass, MethodType.methodType(void.class, CommandCall.class, int.class, String.class));
			constructorHandleMap.put(commandArgumentClass, commandArgumentConstructorHandle);
		}

		return ((T) commandArgumentConstructorHandle.invoke(commandCall, argumentIndex, argumentData));
	}
}
