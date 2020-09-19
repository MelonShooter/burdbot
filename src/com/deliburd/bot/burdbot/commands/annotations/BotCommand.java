package com.deliburd.bot.burdbot.commands.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.deliburd.bot.burdbot.commands.annotations.internal.NoEmptyAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.NoNegativeAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.NoPureWhitespaceAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.NoWhitespaceAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.UniqueAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.WarnDuplicateElementAnnotationParameter;

import net.dv8tion.jda.api.Permission;

/**
 * An annotation to use to create bot commands. This should be put on a method that returns void, isn't generic, 
 * and is in a class that directly extends CommandModule. The parameters of the method must be a CommandCall followed
 * by any arguments for the command. The number of strings in the argumentDescription annotation parameter must match
 * the number of arguments. The default name for a command will be the method name lowercased but can be changed through
 * the commandName annotation parameter.
 * 
 * @author DELIBURD
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BotCommand {
	/**
	 * The command's name (case-insensitive). The default is "", which means the command name
	 * becomes the name of the method lowercased. This command name cannot contain any whitespace.
	 */
	@NoWhitespaceAnnotationParameter
	@UniqueAnnotationParameter
	public String commandName() default "";
	
	/**
	 * The command's description. The command description cannot be empty or contain purely whitespace.
	 */
	@NoEmptyAnnotationParameter
	@NoPureWhitespaceAnnotationParameter
	public String commandDescription();
	
	/**
	 * The cooldown for the command. This cannot be negative.
	 */
	@NoNegativeAnnotationParameter
	public long cooldown() default 5;
	
	/**
	 * The command's argument description. This cannot contain any empty descriptions or descriptions that contain purely whitespace.
	 * The number of descriptions in this parameter must match the amount of argument parameters in the command method.
	 */
	@NoEmptyAnnotationParameter
	@NoPureWhitespaceAnnotationParameter
	public String[] argumentDescriptions() default {};
	
	/**
	 * The required permissions to execute this command.
	 */
	@WarnDuplicateElementAnnotationParameter
	public Permission[] requiredPermissions() default {};
}
