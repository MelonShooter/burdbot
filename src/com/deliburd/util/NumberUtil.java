package com.deliburd.util;

public class NumberUtil {
	private static final String[] ordinalSuffixes = {"st", "nd", "rd", "th"};
	
	private NumberUtil() {}
	
	public static String toOrdinalNumber(long cardinalNumber) {
		switch((int)(cardinalNumber % 100)) {
		case 11:
		case 12:
		case 13:
			return cardinalNumber + ordinalSuffixes[3];
		default:
			int tensPlace = (int) (cardinalNumber % 10);
			
			if(tensPlace == 0 || tensPlace > 3) {
				return cardinalNumber + ordinalSuffixes[3];
			} else {
				return cardinalNumber + ordinalSuffixes[tensPlace - 1];
			}
		}
	}
	
	public static Long stringToLong(String number) {
		try {
			return Long.parseLong(number);
		} catch(NumberFormatException e) {
			return null;
		}
	}
}
