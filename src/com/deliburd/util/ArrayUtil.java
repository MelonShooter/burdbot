package com.deliburd.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ArrayUtil {
	private ArrayUtil() {}
	
	/**
	 * Gets a random index within the given collection
	 * 
	 * @param <T> The type that the collection holds
	 * @param collection The collection to find an index for. This cannot be empty.
	 * @return The random index within the collection
	 */
	public static <T> int randomCollectionIndex(Collection<T> collection) {
		if(collection.isEmpty()) {
			throw new IllegalArgumentException("This collection cannot be empty.");
		}
		
		return ThreadLocalRandom.current().nextInt(collection.size());
	}
	
	/**
	 * Prepends a string to each value in a list of strings
	 * 
	 * @param stringToPrepend The string to prepend each value with
	 * @param list The list that contains the values to prepend
	 * @return The new list containing the modified values
	 */
	public static List<String> prependStringToList(String stringToPrepend, List<String> list) {
		return list.stream()
				.map(string -> stringToPrepend + string)
				.collect(Collectors.toList());
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
}
