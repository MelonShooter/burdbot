package com.deliburd.util.scraper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;

public abstract class LocalScraper implements Scraper {
	private File[] localFiles;
	
	LocalScraper(File directory) {
		localFiles = directory.listFiles();

		if(localFiles == null) {
			ErrorLogger.LogException(new Exception("Invalid directory for LocalScraper. Can't retreive any texts."));
		}
	}
	
	@Override
	public String getRandomTextBody() {		
		if(localFiles == null) {
			return null;
		}
		
		final var randomText = ArrayUtil.randomArrayValue(localFiles);
		
		try {
			return Files.readString(randomText.toPath());
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return null;
		}
	}
	
	public String[] getRecommendedTextAmount() {
		return getRandomTextBodies(getRecommendedTextCount());
	}

	@Override
	public String[] getRandomTextBodies(int numberOfTexts) {
		var texts = new String[numberOfTexts];
		
		for(int i = 0; i < numberOfTexts; i++) {
			texts[i] = getRandomTextBody();
		}
		
		return texts;
	}
	
	@Override
	public int getRecommendedTextCount() {
		return 1;
	}
}
