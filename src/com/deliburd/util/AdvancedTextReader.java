package com.deliburd.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class AdvancedTextReader {
	private File file;
	private BufferedReader fileSentences;
	private int fileLength;

	public AdvancedTextReader(File file) {
		this.file = file;
	}
	
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

		fileText = fileText.replaceAll("(?<!Mr\\.|Dr\\.|Mrs\\.|Ms\\.|\\..\\.)(?<=\\?|!|\\.) ", "\n");
		//To add a new abbreviation, type |abbrev\\. in the negative lookbehind
		
		if(fileText.isBlank()) {
			return; // Makes fileSentences stay null so getNextExcerptInSentences() returns ""
		}
		
		fileSentences = new BufferedReader(new StringReader(fileText));
		fileLength = fileText.length();
		
		try {
			fileSentences.mark(fileLength + 1);
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	public String getNextExcerptInSentences(int approximateCharacterCount) {
		if(fileSentences == null || approximateCharacterCount == 0) {
			return "";
		}

		StringBuilder text = new StringBuilder(approximateCharacterCount + 128);
		
		try {
			while(text.length() < approximateCharacterCount) {
				String sentence = fileSentences.readLine();
				
				if(sentence == null) {
					fileSentences.reset();
					fileSentences.mark(fileLength + 1);
					sentence = fileSentences.readLine();
				}
				
				text.append(sentence)
						.append(" ");
			}
			
			text.deleteCharAt(text.length() - 1); // Delete space after last sentence
		} catch(IOException e) {
			return "";
		}
		
		return text.toString();
	}
}
