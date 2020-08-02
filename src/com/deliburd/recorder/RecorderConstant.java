package com.deliburd.recorder;

import java.io.File;
import java.util.regex.Pattern;

public class RecorderConstant {
	/**
	 * The path to the directory containing recorder data
	 */
	public static final String RECORDER_DIR = System.getProperty("user.dir") + File.separator + "recorder_data";
	
	/**
	 * The path leading to the recorder data with the file separator character
	 */
	public static final String RECORDER_DIR_SEP = RECORDER_DIR + File.separator;
	
	/**
	 * A pattern to find all instances of %s, but not %%s
	 */
	public static final Pattern REPLACEMENT_PATTERN = Pattern.compile("(?<!%)%s");
	
	/**
	 * The delay in seconds to delete merged audio files
	 */
	public static final int AUDIO_FILE_DELETION_DELAY = 60 * 60 * 24;
	
	/**
	 * The delay in milliseconds to check for deleting merged audio files
	 */
	public static final int AUDIO_FILE_DELETION_CHECK_DELAY = 1000 * 60 * 5; // 5 minutes

	/**
	 * The delay in milliseconds to check for silence and update it if necessary
	 */
	public static final int AUDIO_FILE_SILENCE_CHECK_DELAY = 60 * 1000; // 1 minute

	/**
	 * A pattern to find all instances of %p, but not %%p
	 */
	public static final Pattern NAME_REPLACEMENT_PATTERN = Pattern.compile("(?<!%)%p");
}
