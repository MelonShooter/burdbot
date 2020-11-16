package com.deliburd.bot.burdbot.commands.annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.deliburd.bot.burdbot.commands.annotations.internal.NoEmptyAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.NoNegativeAnnotationParameter;
import com.deliburd.bot.burdbot.commands.annotations.internal.NoPureWhitespaceAnnotationParameter;

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface ArgumentType {
	/**
	 * The amount of words the argument must be. If 0 is provided, then an infinite amount of words can be taken. This cannot be negative.
	 */
	@NoNegativeAnnotationParameter
	public int argumentWordCount() default 1;
	
	@NoEmptyAnnotationParameter
	@NoPureWhitespaceAnnotationParameter
	public String argumentTypeName();
}
