package com.deliburd.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {
	private static final Pattern pattern = Pattern.compile("\\s");
	
	private StringUtil() {}
	
	public static boolean containsWhitespace(String string) {
		Matcher matcher = pattern.matcher(string);
		return matcher.find();
	}
}
