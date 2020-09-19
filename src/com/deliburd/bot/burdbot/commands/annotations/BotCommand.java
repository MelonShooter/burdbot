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
 * An annotation to use to create bot commands.
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
	 */
	@NoEmptyAnnotationParameter
	@NoPureWhitespaceAnnotationParameter
	public String[] argumentDescriptions();
	
	/**
	 * The required permissions to execute this command.
	 */
	@WarnDuplicateElementAnnotationParameter
	public Permission[] requiredPermissions() default {};
}
