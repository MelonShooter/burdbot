package com.deliburd.recorder.util.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import com.cloudburst.lame.mp3.Encoder;
import com.cloudburst.lame.mp3.Lame;
import com.cloudburst.lame.mp3.LameGlobalFlags;
import com.cloudburst.lame.mp3.MPEGMode;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.BitUtil;
import com.deliburd.util.ErrorLogger;

public class MP3FileWriter implements IAudioFileWriter {
	private static final Map<Integer, Integer> bitrateMap;
	private final short MP3_HEADER = (short) 0xFFFB;
	private final int SAMPLES_PER_FRAME = 1152;
	private final File file;
	private final RandomAccessFile mp3FileStream;
	private final AudioFormat audioFormat;
	private final MP3Encoder mp3Encoder;
	private final LameGlobalFlags globalFlags;
	private final byte[] mp3Buffer;
	private final long creationTime;
	private final int maxMP3FrameSize;
	private volatile boolean isFinalized = false;
	private long lastWriteTime;
	private long targetSize;
	private int reservoirFrameCount;
	
	static {
		@SuppressWarnings("serial")
		var bitrateHashMap = new HashMap<Integer, Integer>() {{;
			put(1, 32);
			put(2, 40);
			put(3, 48);
			put(4, 56);
			put(5, 64);
			put(6, 80);
			put(7, 96);
			put(8, 112);
			put(9, 128);
			put(10, 160);
			put(11, 192);
			put(12, 224);
			put(13, 256);
			put(14, 320);
		}};
		
		bitrateMap = Collections.unmodifiableMap(bitrateHashMap);
	}

	{
		creationTime = Instant.now().toEpochMilli();
	}
	
	/**
	 * Creates an MP3 (MPEG 1 Layer 3) audio file writer
	 * 
	 * @param fileToWrite The file to write to
	 * @param format The format of the inputted PCM audio
	 * @param targetSize The target size of the file in bytes. The file will be truncated to this target size if necessary.
	 * A target size of 0 means the file size can be infinite. Non-zero values that are too small for the MP3 file 
	 * will be increased to fit the minimum requirements.
	 * @throws FileNotFoundException If the file cannot be created
	 */
	public MP3FileWriter(File fileToWrite, AudioFormat format, long targetSize) throws FileNotFoundException {
		this(fileToWrite, format, targetSize, false);
	}
	
	/**
	 * Creates an MP3 (MPEG 1 Layer 3) audio file writer
	 * 
	 * @param fileToWrite The file to write to
	 * @param format The format of the inputted PCM audio
	 * @param targetSize The target size of the file in bytes. The file will be truncated to this target size if necessary.
	 * A target size of 0 means the file size can be infinite. Non-zero values that are too small for the MP3 file 
	 * will be increased to fit the minimum requirements.
	 * @param mergeAudio Whether to merge stereo audio into joint stereo audio.
	 * @throws FileNotFoundException If the file cannot be created
	 */
	public MP3FileWriter(File fileToWrite, AudioFormat format, long targetSize, boolean mergeAudio) throws FileNotFoundException {
		MPEGMode channelMode;
		
		if(mergeAudio && format.getChannels() == 2) {
			channelMode = MPEGMode.JOINT_STEREO;
		} else if(mergeAudio && format.getChannels() != 2) {
			throw new IllegalArgumentException("To merge audio, you must have stereo audio.");
		} else {
			if(format.getChannels() == 1) {
				channelMode = MPEGMode.MONO;
			} else {
				channelMode = MPEGMode.STEREO;
			}
		}
		
		file = fileToWrite;
		mp3FileStream = new RandomAccessFile(file, "rw");
		mp3Encoder = new MP3Encoder(format, 256, channelMode, Lame.QUALITY_HIGHEST, true);
		globalFlags = mp3Encoder.getGlobalFlags();
		mp3Buffer = new byte[mp3Encoder.getPCMBufferSize()];
		audioFormat = format;
		maxMP3FrameSize = SAMPLES_PER_FRAME * 40000 / 48000; // Samples per frame * Max bitrate (in bytes) / Sample rate = Max frame size
		lastWriteTime = Instant.now().toEpochMilli();
		
		if(targetSize == 0) {
			this.targetSize = Long.MAX_VALUE;
		} else {
			this.targetSize = targetSize;
		}
	}
	
	@Override
	public byte[] writeSilence(long milliseconds) throws IOException {
		int byteCount = (int) (audioFormat.getSampleSizeInBits() * audioFormat.getChannels() * audioFormat.getSampleRate() / 8 * (milliseconds / 1000));
		return writePCMAudio(new byte[byteCount]);
	}

	@Override
	public byte[] writePCMAudio(byte[] bytes) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write to MP3 file. The audio file has already been finalized.");
		}

		ByteArrayOutputStream bytesEncoded = null;
		int bytesToTransfer = Math.min(mp3Buffer.length, bytes.length);
		int bytesWritten;
		int currentPcmPosition = 0;

		while (0 < (bytesWritten = mp3Encoder.encodeBuffer(bytes, currentPcmPosition, bytesToTransfer, mp3Buffer))) {
			currentPcmPosition += bytesToTransfer;
			bytesToTransfer = Math.min(mp3Buffer.length, bytes.length - currentPcmPosition);

			if(bytesEncoded == null) {
				bytesEncoded = new ByteArrayOutputStream(bytesToTransfer);
			}
			
			bytesEncoded.write(mp3Buffer, 0, bytesWritten);
		}
		
		if(bytesEncoded != null) {
			return directWrite(bytesEncoded.toByteArray(), false);
		} else {
			return null;
		}
	}
	
	/**
	 * Writes audio data directly into the file without conversions or checks.
	 * Once a file has met the minimum requirements or target size, whichever is larger, it will automatically finalize it,
	 * returning any bytes that weren't written into the file, if applicable.
	 * Automatically adds to the running frame count
	 * You most likely want to use writePCMAudio instead.
	 * There is a significant chance that this file will corrupt if you use this directly because of how the encoder works.
	 * If an invalid array is given, the behavior is undefined.
	 * 
	 * @param bytes The bytes in the audio file's format.
	 * @return The converted bytes that couldn't be written into the file. Returns null if the file isn't full
	 * @throws IOException If an IOException occurs
	 * @throws IllegalStateException The file has already been finalized.
	 */
	@Override
	public byte[] directWrite(byte[] bytes) throws IOException {
		return directWrite(bytes, true);
	}
	
	@Override
	public byte[] finalizeFile() {
		return finalizeFile(false);
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
	public long getFileLength() throws IOException {
		if(isFinalized) {
			return file.length();
		} else {
			return mp3FileStream.length();
		}
	}
	
	@Override
	public File getFile() {
		return file;
	}

	@Override
	public long getTargetSize() {
		return targetSize;
	}
	
	/**
	 * Finalizes the file, either returning extra or all bytes produced from the finalization.
	 * 
	 * @param returnAllBytes Whether to not write anything to the file and just return all of the bytes.
	 * @return The converted bytes that couldn't be written into the file because the limit was reached.
	 * This will always be null if no additional data needs to be added to the file on finalization, like with WAVE files.
	 */
	private byte[] finalizeFile(boolean returnAllBytes) {
		if(isFinalized) {
			throw new IllegalStateException("This MP3 file has already been finalized.");
		}

		byte[] encoderBuffer = new byte[getMaxSizeOfReservoir() + 10240];
		int bytesEncoded = mp3Encoder.encodeFinish(encoderBuffer);
		encoderBuffer = Arrays.copyOf(encoderBuffer, bytesEncoded);
		
		try(mp3Encoder) {
			if(!returnAllBytes) {
				byte[][] bytes = new byte[2][];
				bytes[1] = encoderBuffer.clone();
				int byteCountToWrite = calculateSpaceLeft(bytes);
				
				if(bytes[0] != null) {
					encoderBuffer = ArrayUtil.mergeByteArrays(bytes[0], bytes[1]);
				} else {
					encoderBuffer = bytes[1];
				}
				
				byte[] bytesToWrite = new byte[byteCountToWrite];
				encoderBuffer = ArrayUtil.splitByteArray(encoderBuffer, bytesToWrite);
				mp3FileStream.write(bytesToWrite);
				lastWriteTime = Instant.now().toEpochMilli();
			}

			addXingHeader();
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		} catch(Exception e) {
			ErrorLogger.LogException(e);
		}
		
		try {
			if(!returnAllBytes) {
				mp3FileStream.close();
			}
		} catch (IOException e) {
			ErrorLogger.LogException(e);
		}
		
		isFinalized = true;
		
		if(encoderBuffer.length == 0) {
			return null;
		}

		return encoderBuffer;
	}
	
	@Override
	public boolean isFinalized() {
		return isFinalized;
	}
	
	/**
	 * Writes audio data directly into the file without conversions or checks.
	 * Once a file has met the minimum requirements or target size, whichever is larger, it will automatically finalize it,
	 * returning any bytes that weren't written into the file, if applicable.
	 * You most likely want to use writePCMAudio instead.
	 * There is a significant chance that this file will corrupt if you use this directly because of how the encoder works.
	 * If an invalid array is given, the behavior is undefined.
	 * 
	 * @param bytes The bytes in the audio file's format.
	 * @param addToFrameCount Whether to add to the frame count or not
	 * @return The converted bytes that couldn't be written into the file. Returns null if the file isn't full
	 * @throws IOException If an IOException occurs
	 * @throws IllegalStateException The file has already been finalized.
	 */
	private byte[] directWrite(byte[] bytes, boolean addToFrameCount) throws IOException {
		if(isFinalized) {
			throw new IllegalStateException("Failed to write to MP3 file. The audio file has already been finalized.");
		}
		
		// If addToFrameCount is true, then this direct write came externally, so we need to add the XING header ourselves.
		if(addToFrameCount && mp3FileStream.length() == 0) {
			writePCMAudio(new byte[audioFormat.getSampleSizeInBits() / 8 * audioFormat.getChannels()]);
		}
		
		long extraBytesToBeWritten = bytes.length - (mp3FileStream.length() - mp3FileStream.getFilePointer());
		
		if(extraBytesToBeWritten < 0) {
			extraBytesToBeWritten = 0;
		}
		
		updateReservoirFrameCount();

		long potentialFileSize = mp3FileStream.length() + extraBytesToBeWritten + getMaxSizeOfReservoir();
		int frameCount = globalFlags.frameNum + reservoirFrameCount;
		
		if(frameCount < 10 && addToFrameCount) {
			frameCount += getFrameCount(bytes);
		}
		
		byte[] leftoverBytes = null;
		
		if(potentialFileSize > targetSize && frameCount >= 10) {
			byte[] finalBytes = finalizeFile(true);
			int oldBytesLength = bytes.length;
			// Merge the finalized bytes with the inputted byte array.
			bytes = Arrays.copyOf(bytes, bytes.length + finalBytes.length);
			
			for(int i = oldBytesLength; i < bytes.length; i++) {
				bytes[i] = finalBytes[i - oldBytesLength];
			}
			
			long frameOffset = findFrameOffset(10);
			int spaceLeft;
			
			if(frameOffset < 10) {
				int frameSize = getFrameSize(10 - frameOffset, bytes);
				
				if(frameSize < 10 - frameOffset) { // This should never happen
					throw new IllegalStateException("Not enough frames. This shouldn't ever happen.");
				}
				
				targetSize = frameSize + mp3FileStream.length();
				spaceLeft = frameSize;
			} else {
				if(frameOffset > targetSize) {
					targetSize = frameOffset;
				}
				
				var bytesArray = new byte[2][];
				bytesArray[1] = bytes.clone();
				
				spaceLeft = calculateSpaceLeft(bytesArray);

				if(bytesArray[0] != null) {
					bytes = ArrayUtil.mergeByteArrays(bytesArray[0], bytesArray[1]);
				} else {
					bytes = bytesArray[1];
				}
			}
			
			byte[] bytesToWrite = new byte[spaceLeft];
			
			leftoverBytes = ArrayUtil.splitByteArray(bytes, bytesToWrite);
			bytes = bytesToWrite;
		}
		
		mp3FileStream.write(bytes);
		
		if(addToFrameCount && bytes.length > 0) {
			globalFlags.frameNum += getFrameCount(bytes);
		}

		lastWriteTime = Instant.now().toEpochMilli();
		
		if(leftoverBytes != null) {
			mp3FileStream.close();
			
			if(leftoverBytes.length == 0) {
				return null;
			}
		}
		
		return leftoverBytes;
	}

	/**
	 * Calculates the space left in the file rounding down to the nearest frame. If necessary, the byte array
	 * and the file will be modified to fit the target size. It also adjusts the frame count.
	 * @param bytes A byte array containing 2 other byte arrays. The second byte array contains the data.
	 * The first byte array are any values that need to be prepended.
	 * @return The space left.
	 * @throws IOException  If an IOException occurs
	 */
	private int calculateSpaceLeft(byte[][] bytesArray) throws IOException {
		int incompleteFrameSize = getFrameSize(0, bytesArray[1]);
		
		// Insert an incomplete frame if it exists.
		if(incompleteFrameSize > 0) {
			byte[] incompleteFrame = new byte[getFrameSize(0, bytesArray[1])];
			bytesArray[1] = ArrayUtil.splitByteArray(bytesArray[1], incompleteFrame);
			mp3FileStream.write(incompleteFrame);
		}
		
		if(mp3FileStream.length() > targetSize) { // It's already over the limit
			long oldFilePointer = mp3FileStream.getFilePointer();
			mp3FileStream.seek(targetSize - 1);
			
			while(mp3FileStream.readShort() != MP3_HEADER) {
				mp3FileStream.seek(mp3FileStream.getFilePointer() - 3);
			}
			
			mp3FileStream.seek(mp3FileStream.getFilePointer() - 2);
			
			int truncatedLength = (int) (mp3FileStream.length() - mp3FileStream.getFilePointer());
			bytesArray[0] = new byte[truncatedLength];
			mp3FileStream.read(bytesArray[0]);
			mp3FileStream.setLength(mp3FileStream.length() - truncatedLength);
			mp3FileStream.seek(oldFilePointer);
			globalFlags.frameNum -= getFrameCount(bytesArray[0]);
			globalFlags.frameNum -= getFrameCount(bytesArray[1]);
			return 0;
		} else if(mp3FileStream.length() == targetSize) {
			globalFlags.frameNum -= getFrameCount(bytesArray[1]);
			return 0;
		} else {
			int maxFramesSize = getMaxFramesSize(bytesArray[1]);
			
			if(maxFramesSize < bytesArray[1].length) {
				byte[] truncatedBytes = Arrays.copyOfRange(bytesArray[1], maxFramesSize, bytesArray[1].length);
				globalFlags.frameNum -= getFrameCount(truncatedBytes);
			}

			return maxFramesSize;
		}
	}
	
	/**
	 * Finds the position in the file in which the (frameNumber + 1)-th frame of the audio stream starts
	 * or the number of frames in the stream of the file if there aren't enough frames.
	 * @param frameNumber The frame number
	 * @return The position in the file in which the (frameNumber + 1)-th frame starts 
	 * or the number of frames in the file
	 * @throws IOException If an IOException occurs
	 */
	private long findFrameOffset(int frameNumber) throws IOException {
		long oldFilePointerPosition = mp3FileStream.getFilePointer();
		int frameCount = 0;
		
		mp3FileStream.seek(0);
		
		//ID3 tag is present
		if(mp3FileStream.readShort() == 0x4944 && mp3FileStream.readByte() == 0x33) {
			mp3FileStream.seek(6); //The position of the tag size
			int tagSize = mp3FileStream.readByte() << 21 + 
								mp3FileStream.readByte() << 14 +
								mp3FileStream.readByte() << 7 +
								mp3FileStream.readByte();
			mp3FileStream.seek(mp3FileStream.getFilePointer() + tagSize);
		} else {
			mp3FileStream.seek(0);
		}
		
		findNextFrame(); //Ignore the first frame because it will or does contain the Xing header

		while(frameCount < frameNumber) {
			if(mp3FileStream.length() == mp3FileStream.getFilePointer()) {
				return frameCount;
			} else {
				findNextFrame();
				frameCount++;
			}
		}
		
		long offset = mp3FileStream.getFilePointer();
		mp3FileStream.seek(oldFilePointerPosition);
		
		return offset;
	}
	
	/**
	 * Gets the size of the maximum amount of valid frames that can be put into the file without going over the target size
	 * @param bytes The byte array to get frames from
	 * @return The size of the frames that can be put into the file
	 * @throws IOException If an IOException occurs
	 * @throws IllegalArgumentException If the byte array's length is less than 0 or the array contains an invalid frame.
	 */
	private int getMaxFramesSize(byte[] bytes) throws IOException {
		if (bytes.length <= 0) {
			throw new IllegalArgumentException("The length of the byte array must be over 0.");
		}

		DataInputStream byteStream = new DataInputStream(new ByteArrayInputStream(bytes));
		long spaceLeft = targetSize - mp3FileStream.length(); // Will always be more than 0

		while(byteStream.available() >= 2 && byteStream.readShort() == MP3_HEADER) {
			byteStream.mark(0);
			int bitrate = BitUtil.UnsignedByte(byteStream.readByte()) >>> 4;

			if (bitrate == 0 || bitrate == 15) {
				throw new IllegalArgumentException("Frame has an unsupported or bad bitrate.");
			} else {
				bitrate = bitrateMap.get(bitrate);
			}

			int bytesToSkip = SAMPLES_PER_FRAME * bitrate * 125 / (int) (globalFlags.getOutSampleRate()) - 3;

			if (byteStream.skipBytes(bytesToSkip) != bytesToSkip) {
				throw new IllegalArgumentException("Truncated frame encountered.");
			}
			
			// We've gone over
			if(spaceLeft < bytes.length - byteStream.available()) {
				byteStream.reset();
				return bytes.length - byteStream.available() - 2;
			}
		};
		
		if(byteStream.available() >= 2) {
			throw new IllegalArgumentException("Invalid frame encountered");
		}
		
		return bytes.length;
	}
	
	/**
	 * Gets the size of frameNumber frame in the byte array.
	 * Returns the number of frames found if there are too few frames.
	 * It will skip to the first frame header it sees.
	 * 
	 * @param frameNumber The number of frames to search for
	 * @param bytes The byte array to read from
	 * @return The offset of where the frameNumber-th frame is or the number of frames in the byte array 
	 * if there aren't enough frames.
	 * @throws IllegalArgumentException The byte array contains
	 * an invalid frame except for the initial data in the byte array;
	 * frameNumber < 0; bytes.length <= 0
	 * @throws EOFException The byte array doesn't contain any frame headers.
	 * @throws IOException If an IOException occurs
	 */
	private int getFrameSize(long frameNumber, byte[] bytes) throws IOException {
		if(frameNumber < 0) {
			throw new IllegalArgumentException("The number of frames to get must be 0 or more.");
		} else if(bytes.length <= 0) {
			throw new IllegalArgumentException("The length of the byte array must be over 0.");
		}
		
		DataInputStream byteStream = new DataInputStream(new ByteArrayInputStream(bytes));
		
		do {
			byteStream.reset();
			
			if(byteStream.readByte() == (byte) 0xFF) {
				byteStream.mark(0);
				
				if(byteStream.readByte() == (byte) 0xFB) {
					break;
				}
			} else {
				byteStream.mark(0);
			}
		} while(true);
		
		if(frameNumber == 0) {
			return bytes.length - byteStream.available() - 2;
		}
		
		int frameCount = 0;
		
		do {
			int bitrate = BitUtil.UnsignedByte(byteStream.readByte()) >>> 4;
			
			if(bitrate == 0 || bitrate == 15) {
				throw new IllegalArgumentException("Frame has an unsupported or bad bitrate.");
			} else {
				bitrate = bitrateMap.get(bitrate);
			}

			int bytesToSkip = SAMPLES_PER_FRAME * bitrate * 125 / (int) (globalFlags.getOutSampleRate()) - 3;
			
			if(byteStream.skipBytes(bytesToSkip) != bytesToSkip) {
				throw new IllegalArgumentException("Truncated frame encountered.");
			}
			
			frameCount++;
			
			if(frameCount == frameNumber || byteStream.available() < 2) {
				break;
			} else if(byteStream.readShort() != MP3_HEADER) {
				throw new IllegalArgumentException("Invalid frame encountered.");
			}
		} while(true);
		
		if(frameCount != frameNumber) {
			return frameCount;
		}
		
		// If available bytes less than 2, then the next mp3 header was never read, but if it was, we must add 2.
		int bytesAvailable = byteStream.available() < 2 ? byteStream.available() : byteStream.available() + 2;
		
		return bytes.length - bytesAvailable;
	}
	
	/**
	 * Gets the frame count of a MP3 byte array
	 * 
	 * @param bytes The MP3 byte array
	 * @return The amount of frames in the array.
	 * @throws IOException If an IOException occurs
	 * @throws IllegalArgumentException The byte array contains
	 * an invalid frame except for the initial data in the byte array;
	 * bytes.length <= 0
	 */
	private int getFrameCount(byte[] bytes) throws IOException {
		return getFrameSize(Integer.MAX_VALUE, bytes);
	}
	
	/**
	 * Searches for the beginning of the next frame or where it would be if we're at the end of the file
	 * @throws IOException If an IOException occurs
	 */
	private void findNextFrame() throws IOException {
		boolean found = false;

		while(mp3FileStream.getFilePointer() <= mp3FileStream.length() - 2 && mp3FileStream.readShort() != MP3_HEADER) {
			mp3FileStream.seek(mp3FileStream.getFilePointer() - 1);
			found = true;
		}
		
		if(mp3FileStream.getFilePointer() > mp3FileStream.length() - 2) {
			mp3FileStream.seek(mp3FileStream.length());
			return;
		} else if(found) {
			return;
		}
		
		int bitrate = BitUtil.UnsignedByte(mp3FileStream.readByte()) >>> 4;
		
		if(bitrate == 0 || bitrate == 15) {
			throw new IllegalStateException("Bitrate bad or not supported.");
		} else {
			bitrate = bitrateMap.get(bitrate);
		}
		
		// Samples per frame * bitrate (in bytes) / Sample rate = Frame size (in bytes)
		int frameSize = SAMPLES_PER_FRAME * bitrate * 125 / (int) (globalFlags.getOutSampleRate());
		mp3FileStream.skipBytes(frameSize - 3);
	}
	
	/**
	 * Gets the maximum amount of bytes that could be in the bit reservoir based on reservoirFrameCount
	 * @return The maximum amount of bytes in the bit reservoir
	 */
	private int getMaxSizeOfReservoir() {
		return reservoirFrameCount * maxMP3FrameSize;
	}
	
	/**
	 * Updates the number of frames in the bit reservoir
	 */
	private void updateReservoirFrameCount() {
		int outSampleRate = globalFlags.getOutSampleRate();
		int inSampleRate = globalFlags.getInSampleRate();
		int frameSize = globalFlags.getFrameSize();
		int samplesToEncode = globalFlags.internal_flags.mf_samples_to_encode - Encoder.POSTDELAY;

		if (inSampleRate != outSampleRate) {
			samplesToEncode += 16 * outSampleRate / inSampleRate;
		}
		
		int endPadding = frameSize - (samplesToEncode % frameSize);
		
		if (endPadding < 576) {
			endPadding += frameSize;
		}

		reservoirFrameCount = (samplesToEncode + endPadding) / frameSize;
	}
	
	/**
	 * Seeks the position that the VBR Xing header should be written at
	 * 
	 * @throws IOException If an IO exception occurs
	 */
	private void seekXingOffset() throws IOException {
		if (globalFlags.getMode() == MPEGMode.MONO) {
			mp3FileStream.seek(21);
		} else {
			mp3FileStream.seek(36);
		}
	}
	
	/**
	 * Adds a VBR header to the MP3 file
	 * 
	 * @throws IOException If an IO exception occurs
	 */
	private void addXingHeader() throws IOException {
		long oldFilePointer = mp3FileStream.getFilePointer();
		
		seekXingOffset();
		mp3FileStream.writeBytes("Xing");
		mp3FileStream.writeInt(3); //Enable frames and bytes flag
		mp3FileStream.writeInt(globalFlags.frameNum); // For some reason, this number is 8 higher than it should be
		mp3FileStream.writeInt((int) mp3FileStream.length());
		mp3FileStream.seek(oldFilePointer);
	}
}
