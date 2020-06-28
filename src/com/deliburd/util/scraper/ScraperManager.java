package com.deliburd.util.scraper;

import java.util.Collection;
import java.util.EnumMap;

public class ScraperManager {
	public enum ScraperType {
		Gutenburg,
		LaNacion,
		Papelucho,
		EnglishStory
	}
	
	public static EnumMap<ScraperType, Scraper> scraperMap = new EnumMap<>(ScraperType.class);
	
	static {
		scraperMap.put(ScraperType.Gutenburg, new GutenburgEnglishScraper());
		scraperMap.put(ScraperType.EnglishStory, new ShortStoryEnglishScraper());
		scraperMap.put(ScraperType.LaNacion, new LaNacionSpanishScraper());
		scraperMap.put(ScraperType.Papelucho, new PapeluchoScraper());
	}
	
	private ScraperManager() {}
	
	/**
	 * Gets a scraper given the type
	 * 
	 * @param scraper The scraper's type
	 * @return The scraper
	 */
	public static Scraper getScraper(ScraperType scraper) {
		return scraperMap.get(scraper);
	}
	
	/**
	 * Gets all scrapers
	 * 
	 * @return A collection of the scrapers
	 */
	public static Collection<Scraper> getAllScrapers() {
		return scraperMap.values();
	}
}
