package com.deliburd.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ArrayUtil {
	private ArrayUtil() {}
	
	/**
	 * Gets a random index within the given collection
	 * 
	 * @param <T> The type that the collection holds
	 * @param collection The collection to find an index for
	 * @return The random index within the collection
	 */
	public static <T> int randomCollectionIndex(Collection<T> collection) {
		return ThreadLocalRandom.current().nextInt(collection.size());
	}
	
	/**
	 * Concatenates each string from one list with the string of the same index in the other list
	 * 
	 * @param list1 The first list
	 * @param list2 The second list which must be the same size as the first list
	 * @return The list with the merged strings.
	 */
	public static ArrayList<String> concatStringLists(List<String> list1, List<String> list2) {
		if(list1 == null || list2 == null || list1.size() != list2.size()) {
			throw new IllegalArgumentException("Neither list can be null and they must be the same sizes");
		} else if(list1.isEmpty()) {
			return new ArrayList<String>();
		}

		ArrayList<String> mergedList = new ArrayList<String>(list1.size() + 16);
		
		for(int i = 0; i < list1.size(); i++) {
			mergedList.add(list1.get(i) + list2.get(i));
		}
		
		return mergedList;
	}
	
	/**
	 * Prepends and appends a string to each value in a list of strings
	 * 
	 * @param stringToPrepend The string to prepend each value with
	 * @param list The list that contains the values tp prepend and append
	 * @param stringToAppend The string to append each value with
	 * @return The new list containing the modified values
	 */
	public static List<String> prependAndAppendStringToList(String stringToPrepend, List<String> list, String stringToAppend) {
		var listCopy = new ArrayList<String>(list.size());
		var concatenatedValue = new StringBuilder();
		
		list.forEach(string -> listCopy.add(string));
		
		for(int i = 0; i < list.size(); i++) {
			concatenatedValue.append(stringToPrepend)
					.append(list.get(i))
					.append(stringToAppend);
			listCopy.set(i, concatenatedValue.toString());
			concatenatedValue.setLength(0);
		}
		
		return listCopy;
	}

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
		
	
		for(int i = listSize; i < newSize; i++) {
			arrayList.add(null);
		}
	}
	
	/**
	 * Splits a byte array into 2 parts.
	 * 
	 * @param fullArray The byte array to split
	 * @param firstPartition The byte array that will contain the first part
	 * @return The second part of the split
	 * @throws IllegalArgumentException fullArray == firstPartition || fullArray == null || firstPartition == null ||
	 * firstPartition.length > fullArray.length
	 */
	public static byte[] splitByteArray(byte[] fullArray, byte[] firstPartition) {
		if(fullArray == null || firstPartition == null) {
			throw new IllegalArgumentException("None of the arrays in the arguments can be null.");
		} else if(fullArray == firstPartition) {
			throw new IllegalArgumentException("The 2 byte arrays must be different arrays");
		} else if(firstPartition.length > fullArray.length) {
			throw new IllegalArgumentException("The first partition's length must be less than or equal to the full array's length");
		}

		byte[] secondPartition = new byte[fullArray.length - firstPartition.length];
		
		for(int i = 0; i < fullArray.length; i++) {
			if (i < firstPartition.length) {
				firstPartition[i] = fullArray[i];
			} else {
				secondPartition[i - firstPartition.length] = fullArray[i];
			}
		}
		
		return secondPartition;
	}

	/**
	 * Merges byte arrays
	 * 
	 * @param arrays The byte arrays
	 * @return A merged byte array
	 * @throws IllegalArgumentException If any of the inputted arrays are null
	 */
	public static byte[] mergeByteArrays(byte[]... arrays) {
		if(arrays == null) {
			throw new IllegalArgumentException("None of the inputted arrays cannot be null");
		}
		
		int length = 0;
		
		for(int i = 0; i < arrays.length; i++) {
			if(arrays[i] == null) {
				throw new IllegalArgumentException("None of the inputted arrays cannot be null");
			}
			
			length += arrays[i].length;
		}
		
		byte[] combinedArray = new byte[length];
		int counter = 0;
		
		for(int i = 0; i < arrays.length; i++) {
			for(int j = 0; j < arrays[i].length; j++) {
				combinedArray[counter] = arrays[i][j];
				counter++;
			}
		}
		
		return combinedArray;
	}
	
	/**
	 * Returns either a copy of the byte buffer or the array that backs it
	 * @param byteBuffer
	 * @return
	 */
	public static byte[] byteBufferToArray(ByteBuffer byteBuffer) {
		byte[] bytes = new byte[byteBuffer.capacity()];
		if(byteBuffer.hasArray()) {
			bytes = byteBuffer.array();
		} else {
			bytes = new byte[byteBuffer.capacity()];
			byteBuffer.get(bytes);
		}
		
		return bytes;
	}
}
