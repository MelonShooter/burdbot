package com.deliburd.recorder.util.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.sound.sampled.AudioFormat;

import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

public class WaveFileWriter implements IAudioFileWriter {
	private final RandomAccessFile fileStream; 
	private final File file;
	private final int channelCount;
	private final int sampleRate;
	private final int bitsPerSample;
	private final boolean isBigEndian;
	private final long creationTime;
	private final int HEADER_SIZE = 44;
	private final int minFileSize;
	private volatile boolean isFinalized = false;
	private long lastWriteTime;
	private long targetSize;

	{
		creationTime = Instant.now().toEpochMilli();
		
	}
	
	/**
	 * Creates a WAVE audio file writer
	 * 
	 * @param fileToWrite The file to write to
	 * @param format The format of the inputted PCM audio
	 * @param targetSize The target size of the file in bytes. The file will be truncated to this target size if necessary.
	 * A target size of 0 means the file size can be infinite. Non-zero values that are too small for the WAVE file 
	 * will be increased to fit the minimum requirements. The target size will be rounded down to the nearest valid data
	 * size if necessary.
	 * 
	 * @throws FileNotFoundException If the file cannot be created
	 */
	public WaveFileWriter(File fileToWrite, AudioFormat format, long targetSize) throws FileNotFoundException {
		fileStream = new RandomAccessFile(fileToWrite, "rw");
		file = fileToWrite;
		channelCount = format.getChannels();
		sampleRate = (int) format.getSampleRate();
		bitsPerSample = format.getSampleSizeInBits();
		isBigEndian = format.isBigEndian();
		minFileSize = HEADER_SIZE + channelCount * bitsPerSample / 8;
		
		if(targetSize == 0) {
			this.targetSize = Long.MAX_VALUE;
		} else if(targetSize < minFileSize) {
			targetSize = minFileSize;
		} else if(findClosestValidDataSize(targetSize - HEADER_SIZE) != targetSize - HEADER_SIZE) {
			this.targetSize = findClosestValidDataSize(targetSize - HEADER_SIZE) + HEADER_SIZE;
		} else {
			this.targetSize = targetSize;
		}
		
		writeWaveHeader();
	}
	
	@Override
	public byte[] writeSilence(long milliseconds) throws IOException {
		return directWrite(new byte[(int) (bitsPerSample * channelCount * sampleRate / 8 * (milliseconds / 1000))]);
	}
	
	@Override
	public byte[] writePCMAudio(byte[] bytes) throws IOException {
		if(bytes.length != findClosestValidDataSize(bytes.length)) {
			throw new IllegalArgumentException("Invalid number of bytes being written. Sample is incomplete.");
		}
	
		if (isBigEndian) {
			int reverseOffset = bitsPerSample / 8;
			bytes = bytes.clone();

			for (int i = 0; i < bytes.length; i += reverseOffset) {
				reverseBytes(bytes, i, reverseOffset);
			}
		}

		return directWrite(bytes);

	}
	
	@Override
	public byte[] directWrite(byte[] bytes) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write to WAVE file. The audio file has already been finalized.");
		}
		
		long extraBytesToBeWritten = bytes.length - (fileStream.length() - fileStream.getFilePointer());
		byte[] leftoverBytes = null;
		
		if(fileStream.length() + extraBytesToBeWritten > targetSize) {

			int spaceLeft = (int) (targetSize - fileStream.length()); // Will always be a valid data size
			byte[] bytesToWrite = new byte[spaceLeft];
			
			leftoverBytes = ArrayUtil.splitByteArray(bytes, bytesToWrite);
			
			fileStream.write(bytesToWrite);
			finalizeFile();
		} else {
			fileStream.write(bytes);
		}

		lastWriteTime = Instant.now().toEpochMilli();
		
		return leftoverBytes;
	}
	
	/**
	 * Finalizes the file, adding the necessary metadata.
	 * 
	 * @return The bytes that couldn't be written. Will always be null with WAVE files
	 */
	@Override
	public byte[] finalizeFile() {
		if(isFinalized) {
			throw new IllegalStateException("This WAVE file has already been finalized.");
		}
		
		try(fileStream) {
			ByteBuffer byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.putInt((int) (fileStream.getChannel().size() - 8));
			fileStream.seek(4);
			directWrite(ArrayUtil.byteBufferToArray(byteBuffer)); // Writes the file size into the header
			byteBuffer = ByteBuffer.allocate(4);
			byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
			byteBuffer.putInt((int) (fileStream.getChannel().size() - 44));
			fileStream.seek(40);
			directWrite(ArrayUtil.byteBufferToArray(byteBuffer)); // Writes the data size into the header
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		} catch(Exception e){
			ErrorLogger.LogException(e);
		}
		
		isFinalized = true;
		
		return null;
	}
	
	@Override
	public boolean isFinalized() {
		return isFinalized;
	}

	@Override
	public long getLastWriteTime() {
		return lastWriteTime;
	}
	
	@Override
	public long getCreationTime() {
		return creationTime;
	}
	
	@Override
	public File getFile() {
		return file;
	}

	@Override
	public long getFileLength() throws IOException {
		if(isFinalized) {
			return file.length();
		} else {
			return fileStream.length();
		}
	}

	@Override
	public long getTargetSize() {
		return targetSize;
	}
	
	/**
	 * Writes the header for the WAVE file
	 */
	private void writeWaveHeader() {
		ByteBuffer byteBuffer = ByteBuffer.allocate(HEADER_SIZE);
		byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		byteBuffer.put((byte) 'R');
		byteBuffer.put((byte) 'I');
		byteBuffer.put((byte) 'F');
		byteBuffer.put((byte) 'F');
		byteBuffer.putInt(0); // Placeholder for the file size
		byteBuffer.put((byte) 'W');
		byteBuffer.put((byte) 'A');
		byteBuffer.put((byte) 'V');
		byteBuffer.put((byte) 'E');
		byteBuffer.put((byte) 'f');
		byteBuffer.put((byte) 'm');
		byteBuffer.put((byte) 't');
		byteBuffer.put((byte) ' ');
		byteBuffer.putInt(16); // The amount of bytes in the part of the header above
		byteBuffer.putChar('\1'); // Specifies that the data is PCM
		byteBuffer.putChar((char) channelCount); // Sets the amount of channels to put the data in as
		byteBuffer.putInt(sampleRate); // Puts the sample rate in
		byteBuffer.putInt(sampleRate * bitsPerSample * channelCount / 8);
		byteBuffer.putChar((char) (bitsPerSample * channelCount / 8));
		byteBuffer.putChar((char) bitsPerSample);
		byteBuffer.put((byte) 'd');
		byteBuffer.put((byte) 'a');
		byteBuffer.put((byte) 't');
		byteBuffer.put((byte) 'a');
		byteBuffer.putInt(0); // Placeholder for the data size
		
		try {
			directWrite(ArrayUtil.byteBufferToArray(byteBuffer));
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		}
	}
	
	/**
	 * Finds the closest valid audio data size rounding down
	 * @param size The size in bytes
	 * @return The closest valid size in bytes based on the input.
	 */
	private long findClosestValidDataSize(long size) {
		long factor = bitsPerSample / 8 * channelCount;
		
		if(size < factor) {
			throw new IllegalArgumentException("There is no closest valid size with this small of a value.");
		}
		
		long dataSizeRemainder = size % factor;
		return size - dataSizeRemainder;
	}
	
	/**
	 * Reverse the bytes in an array
	 * 
	 * @param array The array to reverse the bytes for
	 * @param offset The position to start reversing bytes for
	 * @param reverseCount The amount of bytes to reverse
	 */
	private static void reverseBytes(byte[] array, int offset, int reverseCount) {
		for(int i = 0; i < reverseCount / 2; i++) {
			var firstValue = array[i + offset];
			var secondIndex = reverseCount - i + offset - 1;
			array[i + offset] = array[secondIndex];
			array[secondIndex] = firstValue;
		}
	}
}
