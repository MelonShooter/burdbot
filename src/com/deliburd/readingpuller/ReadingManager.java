package com.deliburd.readingpuller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import com.deliburd.util.AdvancedTextReader;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.FileUtil;
import com.deliburd.util.scraper.ScraperManager;

public class ReadingManager {
	public enum ScraperLanguage {
		English ("english", new String[] {"eng", "en", "inglés"}), 
		Spanish ("spanish", new String[] {"sp", "español", "esp", "es"});

		private String language;
		private String[] aliases;
		
		private ScraperLanguage(String string, String[] aliases) {
			language = string;
			this.aliases = aliases;
		}
		
		public String toString() {
			return language;
		}
		
		public String[] getAliases() {
			return aliases;
		}
	}
	
	public enum ScraperDifficulty {
		Easy ("easy", new String[] {"e", "fácil", "f"}), 
		Medium ("medium", new String[] {"m", "intermedio", "i"});//,
		// Hard ("hard", new String[] {"h", "difícil", "d"});

		private String difficulty;
		private String[] aliases;
		
		private ScraperDifficulty(String string, String[] aliases) {
			difficulty = string;
			this.aliases = aliases;
		}
		
		public String toString() {
			return difficulty;
		}
		
		public String[] getAliases() {
			return aliases;
		}
	}
	
	private static final String[] languages = { ScraperLanguage.English.toString(), ScraperLanguage.Spanish.toString() };
	private static final String[] difficulties = { ScraperDifficulty.Easy.toString(), ScraperDifficulty.Medium.toString() };
	private static HashMap<String, HashMap<String, AdvancedTextReader>> readingManifest;
	private static volatile boolean isRegenerating = false;
	
	static {
		readingManifest = new HashMap<String, HashMap<String, AdvancedTextReader>>(2);
	}

	/**
	 * Creates the folder structure for texts
	 */
	private static void createTextFolderStructure() {
		File textDirectory = null;

		for (int i = 0; i < languages.length; i++) {
			var readingDifficultyManifest = new HashMap<String, AdvancedTextReader>(2);
			
			for (int j = 0; j < difficulties.length; j++) {
				String relativePath = File.separator + languages[i] + File.separator + difficulties[j];
				String textPathString = TextConstant.TEXT_FOLDER + relativePath;
				textDirectory = new File(textPathString);
				
				if(!textDirectory.exists() && !textDirectory.mkdirs()) {
					throw new UncheckedIOException("Could not create the directory for the texts", new IOException());
				}


				readingDifficultyManifest.put(difficulties[j], new AdvancedTextReader(new File(textPathString + File.separator + "text.txt")));
			}
			
			readingManifest.put(languages[i], readingDifficultyManifest);
		}
	}
	
	/**
	 * Fetches a text based on language and difficulty
	 * 
	 * @param language The language of the text
	 * @param difficulty The difficulty of the text
	 * @return The text
	 */
	public static String fetchText(ScraperLanguage language, ScraperDifficulty difficulty) {
		AdvancedTextReader textReader = readingManifest.get(language.toString()).get(difficulty.toString());
		textReader.splitFileTextSentences();

		return textReader.getNextExcerptInSentences(TextConstant.APRROXIMATE_CHARACTER_TEXT_COUNT);
	}
	
	/**
	 * Fetches a text based on language and difficulty
	 * 
	 * @param language The language of the text
	 * @param difficulty The difficulty of the text
	 * @return The text
	 */
	public static String fetchText(String language, String difficulty) {
		AdvancedTextReader textReader = readingManifest.get(language).get(difficulty);
		textReader.splitFileTextSentences();

		return textReader.getNextExcerptInSentences(TextConstant.APRROXIMATE_CHARACTER_TEXT_COUNT);
	}

	/**
	 * Regenerates the texts locally
	 */
	public static void regenerateTexts() {
		if(isRegenerating) {
			throw new ConcurrentModificationException("The texts are already being regenerated.");
		}
		
		isRegenerating = true;
		
		deleteTexts();
		
		createTextFolderStructure();

		scrapeAllSources();
		
		isRegenerating = false;
	}
	
	/**
	 * Returns if the texts are currently being regenerated
	 * 
	 * @return Whether the texts are being regenerated
	 */
	public static boolean isRegeneratingTexts() {
		return isRegenerating;
	}

	/**
	 * Scrapes texts from all sources
	 * 
	 * @return Whether the scraper was successful
	 */
	private static boolean scrapeAllSources() {
		var scrapers = ScraperManager.getAllScrapers();
		BufferedWriter fileWriter = null;
		
		for(var scraper : scrapers) {
			try {
				var filePathString = new StringBuilder();
				filePathString.append(TextConstant.TEXT_FOLDER);
				filePathString.append(File.separator);
				filePathString.append(scraper.getLanguage());
				filePathString.append(File.separator);
				filePathString.append(scraper.getDifficulty());
				filePathString.append(File.separator);
				
				var textFile = new File(filePathString.toString() + "text.txt");
				textFile.createNewFile();
				fileWriter = new BufferedWriter(new FileWriter(textFile, Charset.forName("UTF-8"), true));
				
				for(String text : scraper.getRandomTextBodies(scraper.getRecommendedTextCount())) {
					if(text == null) {
						fileWriter.close();
						return false;
					}
					
					fileWriter.write(text);
					fileWriter.write(' ');
				}
				
				fileWriter.close();
			} catch (IOException e) {
				ErrorLogger.LogException(e);
				
				if (fileWriter != null) {
					try {
						fileWriter.close();
					} catch (IOException e1) {
						ErrorLogger.LogException(e);
					}
				}

				return false;
			}
		}

		return true;
	}

	/**
	 * Empties out the text folder
	 */
	private static void deleteTexts() {
		File textDirectory = new File(TextConstant.TEXT_FOLDER);
		FileUtil.emptyFolder(textDirectory);
	}
}
