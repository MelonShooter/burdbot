package com.deliburd.recorder.util.audio;

import java.io.File;
import java.io.IOException;

/**
 * An interface for creating writers for different audio files
 * 
 * @author DELIBURD
 *
 */
public interface IAudioFileWriter {
	/**
	 * Writes silence into the audio file
	 * Once a file has met the minimum requirements or target size, whichever is larger, it will automatically finalize it,
	 * returning any converted bytes that weren't written into the file, if applicable.
	 * 
	 * @param milliseconds The amount of milliseconds to write silence
	 * @throws IOException Thrown when an IOException occurs
	 */
	public byte[] writeSilence(long milliseconds) throws IOException;

	/**
	 * Writes the PCM data into the file up to the target size and converts it into the correct format if necessary.
	 * Once a file has met the minimum requirements or target size, whichever is larger, it will automatically finalize it,
	 * returning any converted bytes that weren't written into the file, if applicable.
	 * 
	 * @param bytes The audio data
	 * @return The converted bytes that couldn't be written into the file. Returns null if the file isn't full
	 * @throws IOException Thrown when an IOException occurs
	 * @throws IllegalArgumentException An invalid number of bytes according to the AudioFormat were given.
	 */
	public byte[] writePCMAudio(byte[] bytes) throws IOException;
	
	/**
	 * Writes audio data directly into the file without conversions or checks.
	 * Once a file has met the minimum requirements or target size, whichever is larger, it will automatically finalize it,
	 * returning any bytes that weren't written into the file, if applicable.
	 * If an invalid array is given, the behavior is undefined.
	 * 
	 * @param bytes The bytes in the audio file's format.
	 * @return The converted bytes that couldn't be written into the file. Returns null if the file isn't full
	 * @throws IllegalStateException The file has already been finalized.
	 */
	public byte[] directWrite(byte[] bytes) throws IOException;

	/**
	 * Finalizes the file, adding the necessary metadata and stripping the file down to its target size if necessary
	 * 
	 * @return The converted bytes that couldn't be written into the file because the limit was reached.
	 * This will always be null if no additional data needs to be added to the file on finalization, like with WAVE files.
	 */
	public byte[] finalizeFile();
	
	/**
	 * Returns whether the writer has been finalized.
	 * 
	 * @return Whether the writer has been finalized.
	 */
	public boolean isFinalized();

	/**
	 * Gets the last time the file was written to
	 * 
	 * @return The time since the epoch in milliseconds that the file was written to
	 */
	public long getLastWriteTime();

	/**
	 * Gets the time the file was created
	 * 
	 * @return The time since the epoch in milliseconds that the file was created
	 */
	public long getCreationTime();
	
	/**
	 * Gets the target size of the file
	 * 
	 * @return The target size in bytes. 
	 * This value will be Long.MAX_VALUE if it was specified to be infinite.
	 * The file won't take into account the target size if it's too small for the audio file's requirements.
	 * Instead, it'll automatically finalize the file as soon as the minimum requirements are met.
	 */
	public long getTargetSize();

	/**
	 * Gets the file that the AudioFileWriter writes to
	 * 
	 * @return The file
	 */
	public File getFile();

	/**
	 * Gets the underlying file's length
	 * 
	 * @return The file's length in bytes
	 * @throws IOException If an IO exception occurs
	 */
	public long getFileLength() throws IOException;
}