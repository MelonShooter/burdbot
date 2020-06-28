package com.deliburd.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class AdvancedTextReader {
	private File file;
	private String[] fileSentences;
	private int fileLength;
	private int filePosition = 0;
	
	/**
	 * Creates a way to read texts
	 * @param file The file to read
	 */
	public AdvancedTextReader(File file) {
		this.file = file;
	}
	
	/**
	 * Splits the file's text into sentences
	 */
	public void splitFileTextSentences() {
		if(!file.exists()) {
			throw new UncheckedIOException("File doesn't exist yet.", new IOException());
		} else if (fileSentences != null) {
			return;
		}
		
		String fileText;
		
		try {
			fileText = Files.readString(file.toPath());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		
		fileLength = fileText.length();
		fileSentences = fileText.split("(?<!Mr\\.|Dr\\.|Mrs\\.|Ms\\.|\\..\\.)(?<=\\?|!|\\.) "); //To add a new abbreviation, type |abbrev\\. in the neg lookbehind
	}
	
	/**
	 * Gets the next excerpt given an approximate character count
	 * 
	 * @param approximateCharacterCount The approximate character count
	 * @return The next excerpt
	 */
	public String getNextExcerptInSentences(int approximateCharacterCount) {
		if(fileSentences == null) {
			return null;
		} else if(fileLength < approximateCharacterCount) {
			return String.join(" ", fileSentences);
		} else if(approximateCharacterCount == 0) {
			
		}
		
		int characterCount = 0;
		int characterDifference = approximateCharacterCount;
		StringBuilder text = new StringBuilder(approximateCharacterCount);
		
		for(int sentenceNumber = filePosition; sentenceNumber < fileSentences.length; sentenceNumber++) {
			characterCount += fileSentences[sentenceNumber].length();
			
			int newCharacterDifference = Math.abs(approximateCharacterCount - characterCount);
			
			if(newCharacterDifference < characterDifference) {
				if(sentenceNumber == fileSentences.length - 1 && approximateCharacterCount > characterCount - fileSentences[sentenceNumber].length()) { // Last index of the sentence array and had string going to last sentence from the beginning.
					return text.toString();
				}
				
				characterDifference = newCharacterDifference;
				text.append(fileSentences[sentenceNumber] + " ");
			} else {
				filePosition = sentenceNumber; //Set the new position in the file to here.
				return text.toString();
			}
		}
		
		//Went to the end of the file. go back to the beginning.
		return getNextExcerptInSentences(approximateCharacterCount);
	}
}
