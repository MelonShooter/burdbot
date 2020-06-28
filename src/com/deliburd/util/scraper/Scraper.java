package com.deliburd.util.scraper;

import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

/**
 * An interface for local and online text retrievers
 * 
 * @author DELIBURD
 */
public interface Scraper {
	/**
	 * Fetches a random text.
	 * 
	 * @return The text. Will be null if it failed, usually with an exception logged.
	 */
	public abstract String getRandomTextBody();
	
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
	
	/**
	 * Gets the title of the text
	 * 
	 * @return The title of the scraper
	 */
	public abstract String getTitle();
	
	/**
	 * Gets the difficulty of the scraper
	 * 
	 * @return The difficulty of the scraper
	 */
	public abstract ScraperDifficulty getDifficulty();
	
	
	/**
	 * Get the recommended amount of texts
	 * 
	 * @return The texts
	 */
	public abstract String[] getRecommendedTextAmount();
	
	/**
	 * Gets the recommended amount of texts for the source
	 * 
	 * @return The number of texts recommended
	 */
	public abstract int getRecommendedTextCount();
}
