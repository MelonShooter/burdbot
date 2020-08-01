package com.deliburd.util.scraper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

public abstract class LocalScraper implements Scraper {
	private final static int MAX_SCRAPE_COUNT = 1;
	private final File[] localFiles;
	private final ScraperLanguage scraperLanguage;
	private final ScraperType scraperSource;
	private final int maxTextScrapeCount;
	
	LocalScraper(File directory, ScraperLanguage language, ScraperType type) {
		this(directory, language, type, MAX_SCRAPE_COUNT);
	}
	
	LocalScraper(File directory, ScraperLanguage language, ScraperType type, int maxScrapeCount) {
		localFiles = directory.listFiles();
		
		if(localFiles == null || localFiles.length == 0) {
			throw new IllegalArgumentException("The directory for a LocalScraper cannot be null or empty.");
		}
		
		scraperLanguage = language;
		scraperSource = type;
		maxTextScrapeCount = maxScrapeCount;
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
	public ScraperLanguage getLanguage() {
		return scraperLanguage;
	}

	@Override
	public ScraperType getSource() {
		return scraperSource;
	}
	
	@Override
	public int getMaxTextScrapeCount() {
		return maxTextScrapeCount;
	}
	
	private String getRandomTextBody() {		
		if(localFiles == null) {
			return null;
		}
		
		File randomText = ArrayUtil.randomArrayValue(localFiles);
		
		try {
			return Files.readString(randomText.toPath());
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			return null;
		}
	}
}
