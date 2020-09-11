/**
 * 
 */
package com.deliburd.bot.burdbot.commands.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * An annotation that can be put on an overloaded command method.
 * The method must return nothing and take a ArgumentCombinationFilter as its only parameter.
 * 
 * @author DELIBURD
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface CheckArgumentCombinations {

}
