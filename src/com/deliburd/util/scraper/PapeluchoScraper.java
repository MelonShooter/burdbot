package com.deliburd.util.scraper;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

import com.deliburd.readingpuller.TextConstant;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

public class PapeluchoScraper implements Scraper {
	private static ArrayList<File> papeluchoFiles = new ArrayList<File>(13);

	@Override
	public String getRandomTextBody() throws Exception {
		if(papeluchoFiles.isEmpty()) {
			papeluchoFiles = new ArrayList<File>(Arrays.asList(new File(TextConstant.PAPELUCHO_CACHE_FOLDER).listFiles()));
		}
		
		int randomTextIndex = ArrayUtil.randomArrayIndex(papeluchoFiles);
		String text = Files.readString(papeluchoFiles.get(randomTextIndex).toPath());
		papeluchoFiles.remove(randomTextIndex);
		
		return text;
	};

	@Override
	public String[] getRandomTextBodies(int numberOfTexts) {
		String[] texts = new String[numberOfTexts];

		for (int i = 0; i < numberOfTexts; i++) {
			String textBody;
			
			try {
				textBody = getRandomTextBody();
			} catch (Exception e) {
				textBody = null;
				ErrorLogger.LogException(e);
			}
			
			if(textBody != null) {
				texts[i] = textBody;
			}
		}

		return texts;
	}

	@Override
	public ScraperLanguage getLanguage() {
		return ScraperLanguage.Spanish;
	}

	@Override
	public ScraperType getSource() {
		return ScraperType.Papelucho;
	}

	@Override
	public int getRecommendedTextCount() {
		return 7;
	}
	
	@Override
	public ScraperDifficulty getDifficulty() {
		return ScraperDifficulty.Easy;
	}
}
