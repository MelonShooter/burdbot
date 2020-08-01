package com.deliburd.util.scraper;

import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

/**
 * An interface for local and online text retrievers
 * 
 * @author DELIBURD
 */
public interface Scraper {
	/**
	 * Gets multiple texts
	 * 
	 * @param numberOfTexts The number of texts to get
	 * @return The texts, which can be null if it failed.
	 */
	public abstract String[] getRandomTextBodies(int numberOfTexts);

	/**
	 * Returns the language that the scraper gives texts in
	 * 
	 * @return The language the text is in
	 */
	public abstract ScraperLanguage getLanguage();
	
	/**
	 * Gets the source of the scraper
	 * 
	 * @return The source of the scraper
	 */
	public abstract ScraperType getSource();
	
	public abstract int getMaxTextScrapeCount();
}
