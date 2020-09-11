package com.deliburd.bot.burdbot.commands.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation that can be put on an overloaded command method.
 * The method must returns a boolean and takes a CommandCall as its only parameter.
 * 
 * @author DELIBURD
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface CheckPermission {
	/**
	 * Whether to run the command module's permission check for this command as well or not.
	 */
	public boolean overrideModulePermissionCheck() default false;
}
