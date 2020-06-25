package com.deliburd.util.scraper;

import java.util.concurrent.ExecutionException;

import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.scraper.ScraperFactory.ScraperType;

public interface Scraper {
	public abstract String getRandomTextBody() throws Exception;
	
	public abstract String[] getRandomTextBodies(int numberOfTexts) throws ExecutionException;

	public abstract ScraperLanguage getLanguage();
	
	public abstract ScraperType getSource();
	
	public abstract ScraperDifficulty getDifficulty();
	
	public abstract int getRecommendedTextAmount();
}
