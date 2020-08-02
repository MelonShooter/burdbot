package com.deliburd.recorder.util.audio;

import com.cloudburst.lame.mp3.Lame;
import com.cloudburst.lame.mp3.LameGlobalFlags;
import com.cloudburst.lame.mp3.MPEGMode;
import com.cloudburst.lame.mp3.VbrMode;
import com.cloudburst.lame.mp3.Version;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Wrapper for the jump3r encoder. Modified by DELIBURD to implement AutoCloseable among other things.
 *
 * @author Ken Händel
 */
class MP3Encoder implements AutoCloseable {
	/**
	 * property key to read/set the VBR mode (Boolean, default: false)
	 */
	private static final String P_VBR = "vbr";
	
	/**
	 * property key to read/set the channel mode (MPEGMode).
	 */
	private static final String P_CHMODE = "chmode";
	
	/**
	 * property key to read/set the bitrate (Integer : 32...320 kbit/s). Set to
	 * BITRATE_AUTO for default bitrate.
	 */
	private static final String P_BITRATE = "bitrate";
	
	/**
	 * property key to read/set the quality (Integer : 1 (highest) to 9 (lowest).
	 */
	private static final String P_QUALITY = "quality";
	
	/**
	 * suggested maximum buffer size for an mpeg frame
	 */
	private static final int DEFAULT_PCM_BUFFER_SIZE = 2048 * 16;
	private static final int BITRATE_AUTO = -1;
	private static final AudioFormat.Encoding MPEG1L3 = new AudioFormat.Encoding("MPEG1L3");

	private int bitRate = BITRATE_AUTO;
	private int quality = Lame.QUALITY_MIDDLE;
	private boolean vbrMode = false;
	
	/**
	 * MP3 encoder.
	 */
	private Lame lame = new Lame();
	private Version version = new Version();
	private int sampleSizeInBits;
	private ByteOrder byteOrder;
	private MPEGMode chMode;
	private int effQuality;
	private int effBitRate;
	private VbrMode effVbr;
	private MPEGMode effChMode;
	private int effSampleRate;

	/**
	 * Initializes the encoder, overriding any parameters set in the audio format's
	 * properties or in the system properties.
	 *
	 * @throws IllegalArgumentException when parameters are not supported by LAME.
	 */
	MP3Encoder(AudioFormat sourceFormat, int bitRate, MPEGMode channelMode, int quality, boolean VBR) {
		this.bitRate = bitRate;
		this.chMode = channelMode;
		this.quality = quality;
		this.vbrMode = VBR;
		initParams(sourceFormat);
	}

	private void initParams(AudioFormat sourceFormat) {
		sampleSizeInBits = sourceFormat.getSampleSizeInBits();
		byteOrder = sourceFormat.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
		// simple check that bitrate is not too high for MPEG2 and MPEG2.5
		// todo: exception ?
		if (sourceFormat.getSampleRate() < 32000 && bitRate > 160) {
			bitRate = 160;
		}
		int result = initParams(sourceFormat.getChannels(), Math.round(sourceFormat.getSampleRate()), bitRate, chMode,
				quality, vbrMode, sourceFormat.isBigEndian());
		if (result < 0) {
			throw new IllegalArgumentException("parameters not supported by LAME (returned " + result + ")");
		}
	}

	/**
	 * Initializes the lame encoder. Throws IllegalArgumentException when parameters
	 * are not supported by LAME.
	 */
	private int initParams(int channels, int sampleRate, int bitrate, MPEGMode mode, int quality, boolean VBR,
			boolean bigEndian) {
		// Set parameters
		lame.getFlags().setInNumChannels(channels);
		lame.getFlags().setInSampleRate(sampleRate);
		lame.getFlags().setMode(mode);
		if (VBR) {
			lame.getFlags().setVBR(VbrMode.vbr_default);
			lame.getFlags().setVBRQuality(quality);
		} else {
			if (bitrate != BITRATE_AUTO) {
				lame.getFlags().setBitRate(bitrate);
			}
		}
		lame.getFlags().setQuality(quality);
		lame.getId3().init(lame.getFlags());
		lame.getFlags().setWriteId3tagAutomatic(false);
		lame.getFlags().setFindReplayGain(true);
		// Analyze parameters and set more internal options accordingly
		int rc = lame.initParams();
		// return effective values
		effSampleRate = lame.getFlags().getOutSampleRate();
		effBitRate = lame.getFlags().getBitRate();
		effChMode = lame.getFlags().getMode();
		effVbr = lame.getFlags().getVBR();
		effQuality = (VBR) ? lame.getFlags().getVBRQuality() : lame.getFlags().getQuality();
		return rc;
	}

	/**
	 * Get encoder version string.
	 *
	 * @return encoder version string
	 */
	public final String getEncoderVersion() {
		return version.getLameVersion();
	}

	/**
	 * Returns the buffer needed pcm buffer size. The passed parameter is a wished
	 * buffer size. The implementation of the encoder may return a lower or higher
	 * buffer size. The encoder must be initalized (i.e. not closed) at this point.
	 * A return value of <0 denotes an error.
	 */
	public final int getPCMBufferSize() {
		return DEFAULT_PCM_BUFFER_SIZE;
	}

	public final int getMP3BufferSize() {
		return getPCMBufferSize() / 2 + 1024;
	}

	private int doEncodeBuffer(final byte[] pcm, final int pcmOffset, final int length, final byte[] encoded) {
		int bytesPerSample = sampleSizeInBits >> 3;
		int samplesRead = length / bytesPerSample;
		int[] sampleBuffer = new int[samplesRead];

		int sampleBufferPos = samplesRead;
		if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
			if (bytesPerSample == 1)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24;
			if (bytesPerSample == 2)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 16
							| (pcm[pcmOffset + i + 1] & 0xff) << 24;
			if (bytesPerSample == 3)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 8
							| (pcm[pcmOffset + i + 1] & 0xff) << 16 | (pcm[pcmOffset + i + 2] & 0xff) << 24;
			if (bytesPerSample == 4)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) | (pcm[pcmOffset + i + 1] & 0xff) << 8
							| (pcm[pcmOffset + i + 2] & 0xff) << 16 | (pcm[pcmOffset + i + 3] & 0xff) << 24;
		} else {
			if (bytesPerSample == 1)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = ((pcm[pcmOffset + i] & 0xff) ^ 0x80) << 24 | 0x7f << 16;
			if (bytesPerSample == 2)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
							| (pcm[pcmOffset + i + 1] & 0xff) << 16;
			if (bytesPerSample == 3)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
							| (pcm[pcmOffset + i + 1] & 0xff) << 16 | (pcm[pcmOffset + i + 2] & 0xff) << 8;
			if (bytesPerSample == 4)
				for (int i = samplesRead * bytesPerSample; (i -= bytesPerSample) >= 0;)
					sampleBuffer[--sampleBufferPos] = (pcm[pcmOffset + i] & 0xff) << 24
							| (pcm[pcmOffset + i + 1] & 0xff) << 16 | (pcm[pcmOffset + i + 2] & 0xff) << 8
							| (pcm[pcmOffset + i + 3] & 0xff);
		}

		sampleBufferPos = samplesRead;
		samplesRead /= lame.getFlags().getInNumChannels();

		float buffer[][] = new float[2][samplesRead];
		if (lame.getFlags().getInNumChannels() == 2) {
			for (int i = samplesRead; --i >= 0;) {
				buffer[1][i] = sampleBuffer[--sampleBufferPos];
				buffer[0][i] = sampleBuffer[--sampleBufferPos];
			}
		} else if (lame.getFlags().getInNumChannels() == 1) {
			Arrays.fill(buffer[1], 0, samplesRead, 0);
			for (int i = samplesRead; --i >= 0;) {
				buffer[0][i] = buffer[1][i] = sampleBuffer[--sampleBufferPos];
			}
		}
		return lame.encodeBuffer(buffer[0], buffer[1], samplesRead, encoded);
	}

	/**
	 * Encode a block of data. Throws IllegalArgumentException when parameters are
	 * wrong. When the <code>encoded</code> array is too small, an
	 * ArrayIndexOutOfBoundsException is thrown. <code>length</code> should be the
	 * value returned by getPCMBufferSize.
	 *
	 * @return the number of bytes written to <code>encoded</code>. May be 0.
	 */
	public final int encodeBuffer(final byte[] pcm, final int pcmOffset, final int pcmLength, final byte[] encoded)
			throws ArrayIndexOutOfBoundsException {
		if (pcmLength < 0 || (pcmOffset + pcmLength) > pcm.length) {
			throw new IllegalArgumentException("inconsistent parameters");
		}
		int result = doEncodeBuffer(pcm, pcmOffset, pcmLength, encoded);
		if (result < 0) {
			if (result == -1) {
				throw new ArrayIndexOutOfBoundsException("Encode buffer too small");
			}
			throw new RuntimeException("crucial error in encodeBuffer.");
		}
		return result;
	}

	public final int encodeFinish(final byte[] encoded) {
		return lame.encodeFlush(encoded);
	}

	@Override
	public final void close() {
		lame.close();
	}

	/**
	 * Return the audioformat representing the encoded mp3 stream. The format object
	 * will have the following properties:
	 * <ul>
	 * <li>P_QUALITY - Integer, 1 (highest) to 9 (lowest)<BR>
	 * <li>P_BITRATE - Integer, 32...320 kbit/s<BR>
	 * <li>P_CHMODE - MPEGMode<BR>
	 * <li>P_VBR - Boolean
	 * <li>encoder.name: a string with the name of the encoder
	 * <li>encoder.version: a string with the version of the encoder
	 * </ul>
	 */
	public final AudioFormat getEffectiveFormat() {
		// first gather properties
		final HashMap<String, Object> map = new HashMap<String, Object>();
		map.put(P_QUALITY, getEffectiveQuality());
		map.put(P_BITRATE, getEffectiveBitRate());
		map.put(P_CHMODE, getEffectiveChannelMode());
		map.put(P_VBR, getEffectiveVBR());
		// map.put(P_SAMPLERATE, getEffectiveSampleRate());
		// map.put(P_ENCODING,getEffectiveEncoding());
		map.put("encoder.name", "LAME");
		map.put("encoder.version", getEncoderVersion());
		int notSpecified = AudioSystem.NOT_SPECIFIED;
		int channels = (chMode == MPEGMode.MONO) ? 1 : 2;

		return new AudioFormat(getEffectiveEncoding(), getEffectiveSampleRate(), notSpecified, channels, notSpecified,
				notSpecified, false, map);
	}

	public final int getEffectiveQuality() {
		if (effQuality >= Lame.QUALITY_LOWEST) {
			return Lame.QUALITY_LOWEST;
		} else if (effQuality >= Lame.QUALITY_LOW) {
			return Lame.QUALITY_LOW;
		} else if (effQuality >= Lame.QUALITY_MIDDLE) {
			return Lame.QUALITY_MIDDLE;
		} else if (effQuality >= Lame.QUALITY_HIGH) {
			return Lame.QUALITY_HIGH;
		}
		return Lame.QUALITY_HIGHEST;
	}

	public final int getEffectiveBitRate() {
		return effBitRate;
	}

	public final MPEGMode getEffectiveChannelMode() {
		return effChMode;
	}

	public final boolean getEffectiveVBR() {
		return effVbr != VbrMode.vbr_off;
	}

	public final int getEffectiveSampleRate() {
		return effSampleRate;
	}

	public final AudioFormat.Encoding getEffectiveEncoding() {
		return MPEG1L3;
	}
	
	public final LameGlobalFlags getGlobalFlags() {
		return lame.getFlags();
	}
}
