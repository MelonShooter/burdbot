package com.deliburd.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public final class FileUtil {
	private static String folderPath;

	private FileUtil() {}

	public static void emptyFolder(File folder) {
		File[] files = folder.listFiles();

		if (files == null) { // Supplied argument isn't a directory or is an empty folder.
			return;
		}

		folderPath = folder.getPath();
		recursiveEmptyFolder(files);
		folderPath = null;
	}

	private static void recursiveEmptyFolder(File[] files) {
		for (int i = 0; i < files.length; i++) {
			if (!files[i].delete()) {
				if (files[i].isDirectory()) {
					File[] subFiles = files[i].listFiles();

					if (subFiles.length != 0) {
						recursiveEmptyFolder(subFiles);
					} else {
						try {
							Files.deleteIfExists(subFiles[i].toPath());
						} catch (IOException e) {
							continue;
						}
					}
				} else {
					try {
						Files.deleteIfExists(files[i].toPath());
					} catch (IOException e) {
						continue;
					}
				}
			} else {
				if (files[i].getParent() != null && !files[i].getParent().equals(folderPath)) {
					files[i].getParentFile().delete();
				}
			}
		}
	}

	public static boolean isFolderEmpty(File folder) {
		if (!folder.isDirectory()) {
			throw new UncheckedIOException("Argument supplied is a file or doesn't exist.", new IOException());
		}

		return folder.list().length == 0;
	}

	public static boolean isFolderEmptyOrDoesNotExist(File folder) {
		return !folder.exists() || isFolderEmpty(folder);
	}
}
