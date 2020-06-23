package com.deliburd.bot.burdbot.util;

import java.util.ArrayList;

public class ArrayUtil {
	private ArrayUtil() {}
	
	/**
	 * Gets all combinations of strings between the inner arrays of a 2d or jagged array.
	 * 
	 * @param array A 2d or jagged array of strings
	 * @return All combinations between the inner arrays of the array given.
	 * @throws IllegalArgumentException  If an array within the given array is null or blank.
	 */
	public static ArrayList<String[]> findCrossArrayStringCombinations(String[][] array) {		
		ArrayList<String[]> combinationArray = null;
		int[] combo = new int[array.length];
		final int comboSize = array.length;
		int currentDigitMaxSize; // Size of the inner array being iterated through
		int currentDigitPosition = comboSize - 1;
		boolean didCarry = false;

		while(currentDigitPosition >= 0) {
			currentDigitMaxSize = array[currentDigitPosition].length;

			// While the current digit hasn't reached its max
			while(combo[currentDigitPosition] < array[currentDigitPosition].length) {
				if(combinationArray == null) { // If this is the first combination
					// Gets the number of combinations to intialize the arraylist as that size
					int cachedArraySize = 1;

					var firstCombination = new String[comboSize];

					for(int i = 0; i < comboSize; i++) {
						if(array[i] == null || array[i].length == 0) {
							throw new IllegalArgumentException("No inner array can be null or blank");
						} else {
							final String firstString = array[i][0];
						
							firstCombination[i] = firstString; // Insert the values of the first combination
							cachedArraySize *= array[i].length;
						}
					}
					
					combinationArray = new ArrayList<String[]>(cachedArraySize);
					combinationArray.add(firstCombination);
				} else {
					String[] combinationClone; // Clone of the last inserted array in the combinationArray

					if (didCarry) { //If the last iteration carried an index over
						final int newComboIndex = ++combo[currentDigitPosition];
						
						// If adding 1 to the current carry position went up to the max, then carry again
						if(newComboIndex >= currentDigitMaxSize) {
							continue;
						}
						
						// Get the new string for the carried digit
						final String newCombinationString = array[currentDigitPosition][newComboIndex];

						combinationClone = combinationArray.get(combinationArray.size() - 1).clone();
						combinationClone[currentDigitPosition] = newCombinationString;
						didCarry = false;
				
						// Sets all subsequent strings from the carry digit back to the 0th string of the inner arrays
						for(int i = currentDigitPosition + 1; i < comboSize; i++) {
							final String firstString = array[i][0];
							
							combinationClone[i] = firstString;
							combo[i] = 0;
						}
						
						currentDigitPosition = comboSize - 1; // Set the currentDigitPosition back to the end
						currentDigitMaxSize = array[currentDigitPosition].length; // Update currentDigitMaxSize
					} else {
						final int newComboIndex = combo[currentDigitPosition];
						final String newCombinationString = array[currentDigitPosition][newComboIndex];
						
						combinationClone = combinationArray.get(combinationArray.size() - 1).clone();
						combinationClone[currentDigitPosition] = newCombinationString;
					}
					
					combinationArray.add(combinationClone);
				}

				combo[currentDigitPosition]++;
				
				if(combo[currentDigitPosition] == currentDigitMaxSize) { //Is last iteration
					didCarry = true;
				}
			}
			
			currentDigitPosition--;
		}

		return combinationArray;
	}
	
	/**
	 * Expands the arraylist's size by adding null until the Arraylist is at the specified size, if necessary.
	 * Won't do anything if the ArrayList's size is more than or equal to the new size already.
	 * 
	 * @param <T> The type of values contained in the ArrayList
	 * @param arrayList The ArrayList
	 * @param newSize The new size
	 */
	public static <T> void ensureArrayListSize(ArrayList<T> arrayList, int newSize) {
		final int listSize = arrayList.size();
		if(newSize <= listSize) {
			return;
		}
		

		for(int i = listSize; i < newSize - 1; i++) {
			arrayList.add(null);
		}
	}
}
