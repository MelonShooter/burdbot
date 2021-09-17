package com.deliburd.util;

import java.io.File;
import java.util.function.Predicate;

public final class FileUtil {
	private FileUtil() {}
	
	/**
	 * Empties out a folder. Does nothing if the folder doesn't exist or isn't a directory
	 * 
	 * @param folder The folder to empty
	 */
	public static void emptyFolder(File folder) {
		if(folder == null) {
			throw new IllegalArgumentException("Directory is null.");
		}
		
		File[] files = folder.listFiles();

		if (files == null) {
			return;
		}
		
		if(files.length == 0) {
			folder.delete();
		}

		for(int i = 0; i < files.length; i++) {
			if(files[i].isDirectory()) {
				deleteFolder(files[i]);
			} else {
				files[i].delete();
			}
		}
	}

	/**
	 * Recursively deletes a folder. Does nothing if the folder doesn't exist or isn't a directory
	 * 
	 * @param folder The folder to delete
	 */
	public static void deleteFolder(File folder) {
		if(folder == null) {
			throw new IllegalArgumentException("Directory is null.");
		}
		
		File[] files = folder.listFiles();

		if (files == null) {
			return;
		}
		
		if(files.length == 0) {
			folder.delete();
		}

		for(int i = 0; i < files.length; i++) {
			if(files[i].isDirectory()) {
				deleteFolder(files[i]);
			} else {
				files[i].delete();
			}
		}
		
		folder.delete();
	}

	/**
	 * Returns whether a folder is empty
	 * 
	 * @param folder The folder to check. Must not be null and must be a directory.
	 * @return Whether the folder is empty or not
	 */
	public static boolean isFolderEmpty(File folder) {
		if (folder == null || !folder.isDirectory()) {
			throw new IllegalArgumentException("Argument supplied is null or a file or doesn't exist.");
		}

		return folder.list().length == 0;
	}
}
