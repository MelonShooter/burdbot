package com.deliburd.util.scraper;

import java.io.File;

import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

public abstract class WebScraper implements Scraper {
	private final static int MAX_SCRAPE_COUNT = 10;
	private final ScraperLanguage scraperLanguage;
	private final ScraperType scraperSource;
	private final int maxTextScrapeCount;
	
	WebScraper(File directory, ScraperLanguage language, ScraperType type) {
		this(directory, language, type, MAX_SCRAPE_COUNT);
	}
	
	WebScraper(File directory, ScraperLanguage language, ScraperType type, int maxScrapeCount) {
		scraperLanguage = language;
		scraperSource = type;
		maxTextScrapeCount = maxScrapeCount;
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
}
