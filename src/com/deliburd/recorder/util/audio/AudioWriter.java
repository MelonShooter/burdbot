package com.deliburd.recorder.util.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;

import com.deliburd.util.ErrorLogger;


public class AudioWriter implements IAudioFileWriter {
	private final IAudioFileWriter mergedFileWriter;
	private final ArrayList<IAudioFileWriter> separateFileWriters;
	private final AudioFormat audioFormat;
	private final String filePrefix;
	private final String audioFileExtension;
	private final File subfolder;
	private final File baseFolder;
	private final int separateFileSizeLimit;
	private final long targetSize;
	private volatile boolean isFinalized;
	private IAudioFileWriter currentFile;
	private long lastSeparateWritePosition;
	private IAudioFileWriter lastFileWritten;
	
	/**
	 * Creates an audio writer which creates separated files according to partitionSize and a merged audio file
	 * which has a limit of targetSize.
	 * 
	 * @param baseFolder The folder to put the merged audio into.
	 * @param subfolder The sub-folder to put the separated files into
	 * @param filePrefix The string to prefix all files created by this writer with
	 * @param partitionSize The size in bytes to partition the files with
	 * @param targetSize The size in bytes for the whole audio writer.
	 * @param compression The quality of the audio outputed by the writer.
	 * @param format The format of the audio
	 * @throws FileNotFoundException If the file cannot be created
	 * @throws IllegalArgumentException If subFolder isn't a subfolder of baseFolder
	 */
	public AudioWriter(File baseFolder, File subfolder, String filePrefix, int partitionSize, long targetSize, AudioCompression compression, AudioFormat format) throws FileNotFoundException {
		if(!subfolder.getParentFile().equals(baseFolder)) {
			throw new IllegalArgumentException("The given subfolder must be a subfolder of the base folder.");
		}
		
		baseFolder.mkdirs();
		subfolder.mkdirs();
		
		this.baseFolder = baseFolder;
		this.filePrefix = filePrefix;
		this.subfolder = subfolder;
		
		if(compression == AudioCompression.UNCOMPRESSED) {
			audioFileExtension = ".wav";
		} else {
			audioFileExtension = ".mp3";
		}
		
		separateFileWriters = new ArrayList<IAudioFileWriter>();
		audioFormat = format;
		separateFileSizeLimit = partitionSize;
		
		if(targetSize == 0) {
			this.targetSize = Long.MAX_VALUE;
		} else {
			this.targetSize = targetSize;
		}
		
		mergedFileWriter = createNewAudioFile(true);
		currentFile = createNewAudioFile();
	}
	
	@Override
	public byte[] directWrite(byte[] bytes) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write to audio file. The audio file has already been finalized.");
		}

		byte[] mergedLeftoverBytes = mergedFileWriter.directWrite(bytes);
		
		if(currentFile.isFinalized()) {
			currentFile = createNewAudioFile();
		}
		
		writeOverflow(currentFile.directWrite(bytes));
		
		if(mergedFileWriter.isFinalized()) {
			finalizeAllFiles();
			//Truncate audio that's in this array from split files starting at lastSeparateWritePosition in lastWrittenFile
		}

		return mergedLeftoverBytes;
	}
	
	@Override
	public byte[] writeSilence(long milliseconds) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write silence. The audio file has already been finalized.");
		}
		
		byte[] mergedLeftoverBytes = mergedFileWriter.writeSilence(milliseconds);
		
		if(currentFile.isFinalized()) {
			currentFile = createNewAudioFile();
		}
		
		writeOverflow(currentFile.writeSilence(milliseconds));
		
		if(mergedFileWriter.isFinalized()) {
			finalizeAllFiles();
			//Truncate audio that's in this array from split files starting at lastSeparateWritePosition in lastWrittenFile
		}

		return mergedLeftoverBytes;
	}

	@Override
	public byte[] writePCMAudio(byte[] bytes) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write PCM audio. The audio file has already been finalized.");
		}
		
		byte[] mergedLeftoverBytes = mergedFileWriter.writePCMAudio(bytes);
		
		if(currentFile.isFinalized()) {
			currentFile = createNewAudioFile();
		}
		
		writeOverflow(currentFile.writePCMAudio(bytes));
		
		if(mergedFileWriter.isFinalized()) {
			finalizeAllFiles();
			//Truncate audio that's in this array from split files starting at lastSeparateWritePosition in lastWrittenFile
		}

		return mergedLeftoverBytes;
	}

	@Override
	public byte[] finalizeFile() {
		if(isFinalized) {
			throw new IllegalStateException("The audio file has already been finalized.");
		}
		
		byte[] mergedBytesLeftover = mergedFileWriter.finalizeFile();
		
		try {
			finalizeAllFiles();
			
			if(mergedBytesLeftover != null) {
				//Truncate audio that's in this array from split files starting at lastSeparateWritePosition in lastWrittenFile
			}
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		}
		
		return mergedBytesLeftover;
	}
	
	@Override
	public boolean isFinalized() {
		return isFinalized;
	}

	@Override
	public long getLastWriteTime() {
		return mergedFileWriter.getLastWriteTime();
	}

	@Override
	public long getCreationTime() {
		return mergedFileWriter.getCreationTime();
	}

	@Override
	public File getFile() {
		return mergedFileWriter.getFile();
	}
	
	@Override
	public long getFileLength() throws IOException {
		return mergedFileWriter.getFileLength();
	}
	
	@Override
	public long getTargetSize() {
		return targetSize;
	}
	
	/**
	 * Gets an array of the underlying separate files for this audio writer.
	 * 
	 * @return The array of separate files
	 */
	public File[] getSeparateFiles() {
		File[] files = new File[separateFileWriters.size()];
		
		for(int i = 0; i < files.length; i++) {
			files[i] = separateFileWriters.get(i).getFile();
		}
		
		return files;
	}
	
	/**
	 * Creates a new separate audio file writer
	 * 
	 * @return The separate IAudioFileWriter
	 * @throws FileNotFoundException If the file cannot be created
	 */
	private IAudioFileWriter createNewAudioFile() throws FileNotFoundException {
		return createNewAudioFile(false);
	}
	
	/**
	 * Creates a new audio file writer
	 * 
	 * @param isMergedFile Whether the file is the merged one or not
	 * @return The new IAudioFileWriter
	 * @throws FileNotFoundException If the file cannot be created
	 */
	private IAudioFileWriter createNewAudioFile(boolean isMergedFile) throws FileNotFoundException {
		File baseDirectory;
		StringBuilder audioFilePath = new StringBuilder(baseFolder.getAbsolutePath());
		long fileSize;
		audioFilePath.append(File.separator);
		
		if(isMergedFile) {
			baseDirectory = baseFolder;
			fileSize = targetSize;
		} else {
			baseDirectory = subfolder;
			fileSize = separateFileSizeLimit;
			audioFilePath.append(subfolder.getName())
					.append(File.separator);
		}
		
		audioFilePath.append(filePrefix)
				.append("-")
				.append(baseDirectory.listFiles().length)
				.append(audioFileExtension);
		
		IAudioFileWriter newAudioFileWriter;
		File newAudioFile = new File(audioFilePath.toString());
		
		if(audioFileExtension.equals(".mp3")) {
			newAudioFileWriter = new MP3FileWriter(newAudioFile, audioFormat, fileSize, true);
		} else {
			newAudioFileWriter = new WaveFileWriter(newAudioFile, audioFormat, fileSize);
		}
		
		if(!isMergedFile) {
			separateFileWriters.add(newAudioFileWriter);
		}

		return newAudioFileWriter;
	}
	
	/**
	 * Finalizes the current file and writes overflow into new files. Then finalizes the last file and the merged file if necessary
	 * @throws IOException
	 */
	private void finalizeAllFiles() throws IOException {
		isFinalized = true;
		
		if(!mergedFileWriter.isFinalized()) {
			mergedFileWriter.finalizeFile();
		}
		
		if(currentFile.isFinalized()) {
			return;
		}
		
		var oldFile = currentFile;
		
		writeOverflow(currentFile.finalizeFile(), false);
		
		if(oldFile != currentFile) {
			writeOverflow(currentFile.finalizeFile(), false);
		}
	}
	
	/**
	 * Writes the overflow from the separated files continuously into new files until there is no more overflow
	 * 
	 * @param writtenBytes The bytes to write
	 * @throws IOException If an IOException occurs
	 */
	private void writeOverflow(byte[] writtenBytes) throws IOException {
		writeOverflow(writtenBytes, true);
	}
	
	/**
	 * Writes the overflow from the separated files continuously into new files until there is no more overflow
	 * 
	 * @param writtenBytes The bytes to write
	 * @param setFilePosition Whether to update the lastFileWritten and lastSeparateWritePosition or not
	 * @throws IOException If an IOException occurs
	 */
	private void writeOverflow(byte[] writtenBytes, boolean setFilePosition) throws IOException {
		if(setFilePosition) {
			lastFileWritten = currentFile;
			lastSeparateWritePosition = currentFile.getFileLength();
		}
		
		while(writtenBytes != null) {
			currentFile = createNewAudioFile();
			
			if(setFilePosition) {
				lastFileWritten = currentFile;
				lastSeparateWritePosition = currentFile.getFileLength();
			}
			
			writtenBytes = currentFile.directWrite(writtenBytes);
		}
	}
}
