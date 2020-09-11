/**
 * 
 */
package com.deliburd.bot.burdbot.commands.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
	 * The command's name. The default is "", which means the command name
	 * becomes the name of the method lowercased.
	 */
	public String commandName() default "";
	
	/**
	 * The command's description
	 */
	public String commandDescription();
	
	/**
	 * The cooldown for the command.
	 */
	public long cooldown() default 5;
	
	/**
	 * The command's argument descriptions
	 */
	public String[] argumentDescriptions();
	
	/**
	 * The required permissions to execute this command
	 */
	public Permission[] requiredPermissions();
}
