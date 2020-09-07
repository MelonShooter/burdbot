package com.deliburd.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
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

		for(var file : files) {
			if(file.isDirectory()) {
				deleteFolder(file);
			} else {
				file.delete();
			}
		}
	}

	/**
	 * Recursively deletes a folder. Does nothing if the folder doesn't exist or isn't a directory
	 * 
	 * @param folder The folder to delete. Cannot be null
	 */
	public static void deleteFolder(File folder) {
		if(folder == null) {
			throw new IllegalArgumentException("Directory is null.");
		} else if(!folder.isDirectory()) {
			return;
		}

		try(var folderContentStream = Files.walk(folder.toPath())) {
			folderContentStream.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEachOrdered(File::delete);
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		}
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

	/**
	 * Returns whether a folder is empty or does not exist
	 * 
	 * @param folder The folder to check. Must not be null.
	 * @return Whether the folder is empty or does not exist
	 */
	public static boolean isFolderEmptyOrDoesNotExist(File folder) {
		if(folder == null) {
			throw new IllegalArgumentException("Argument supplied is null.");
		}
		
		return !folder.exists() || isFolderEmpty(folder);
	}

	/**
	 * Shallowly deletes files in the specified folder with a condition
	 * 
	 * @param baseFolder The folder to delete files in
	 * @param condition The condition to delete files with. Null for no condition.
	 */
	public static void deleteFiles(File baseFolder, Predicate<File> condition) {
		if(baseFolder == null || !baseFolder.isDirectory()) {
			throw new IllegalArgumentException("The base folder specified must be a folder and can't be null.");
		}
		
		File[] files = baseFolder.listFiles();
		
		for(var file : files) {
			if(file.isFile() && (condition == null || condition.test(file))) {
				file.delete();
			}
		}
	}
}
