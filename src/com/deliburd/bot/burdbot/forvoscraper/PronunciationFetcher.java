package com.deliburd.bot.burdbot.forvoscraper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.deliburd.bot.burdbot.Constant;
import com.deliburd.util.BotUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.Pair;

public class PronunciationFetcher {
	public enum ForvoLanguage {
		English ("en"), 
		Spanish ("es");

		private String language;
		
		private ForvoLanguage(String string) {
			language = string;
		}
		
		public String toString() {
			return language;
		}
	}
	
	private static final Timer cacheExpirationTimer = new Timer(true);
	private static final Map<String, Map<ForvoLanguage, Element>> pronunciationCache = new ConcurrentHashMap<>();
	private static final String forvoFolderWithSeparator = Constant.FORVO_FOLDER + File.separator;
	private static final String forvoSite = "https://forvo.com/word/";
	private static final String forvoMP3LinkPrefix = "https://audio.forvo.com/mp3/";
	private static final Pattern nationalityPattern = Pattern.compile("(?<= from ).*(?=\\))");
	private static final Pattern encodedArgumentPattern = Pattern.compile("(?<=, ?(?:'|\\\")).*?(?=(?:'|\\\"),)");
	private static final ExecutorService fetcherThreadPool = Executors.newCachedThreadPool();
	
	private PronunciationFetcher() {}
	
	public static void fetchEnglishPronunciation(String word, EnglishForvoCountries country, BiConsumer<File, String> onSuccess, Runnable onFailure) {
		fetcherThreadPool.submit(() -> asyncFetchPronunciation(ForvoLanguage.English, word, country, onSuccess, onFailure));
	}
	
	public static void fetchSpanishPronunciation(String word, SpanishForvoCountries country, BiConsumer<File, String> onSuccess, Runnable onFailure) {
		fetcherThreadPool.submit(() -> asyncFetchPronunciation(ForvoLanguage.Spanish, word, country, onSuccess, onFailure));
	}
	
	private static void asyncFetchPronunciation(ForvoLanguage language, String word, IForvoCountry country, BiConsumer<File, String> onSuccess, Runnable onFailure) {
		Pair<String, String> MP3LinkAndCountry = findMP3Link(language, word, country);
		
		if(MP3LinkAndCountry == null) {
			onFailure.run();
			return;
		}
		
		String MP3Link = MP3LinkAndCountry.getKey();
		
		if(MP3Link.isBlank()) {
			onFailure.run();
			return;
		}
		
		byte[] mp3FileBytes;
		
		try {
			mp3FileBytes = Jsoup.connect(MP3Link)
					.ignoreContentType(true)
					.maxBodySize(BotUtil.getFileSizeLimit())
					.execute()
					.bodyAsBytes();
		} catch (IOException e) {
			ErrorLogger.LogException(e);
			onFailure.run();
			return;
		}
		
		File forvoFolder = new File(Constant.FORVO_FOLDER);
		long uniqueID = Thread.currentThread().getId();
		String fileName = forvoFolderWithSeparator + word + "-" + uniqueID;
		File newMP3File = new File(fileName + ".mp3");
		
		forvoFolder.mkdirs();

		if (newMP3File.exists()) {
			int counter = 0;
			
			do {
				counter++;
				newMP3File = new File(fileName + "-" + counter + ".mp3");
			} while(newMP3File.exists());
			
			onFailure.run();
			ErrorLogger.LogIssue("The MP3 file already exists. This should never happen.");
			return;
		}

		try (FileOutputStream fileStream = new FileOutputStream(newMP3File)) {
			fileStream.write(mp3FileBytes);
		} catch (IOException e) {
			newMP3File.delete();
			ErrorLogger.LogException(e);
			onFailure.run();
			return;
		}
		
		String prettyCountryName = MP3LinkAndCountry.getValue();
		
		onSuccess.accept(newMP3File, prettyCountryName);
	}

	private static Pair<String, String> findMP3Link(ForvoLanguage language, String word, IForvoCountry country) {
		String pronunciationListLink = forvoSite + word;

		Element languageContainer;
		
		var pronunciationCacheMap = pronunciationCache.get(word);
				
		if(pronunciationCacheMap == null) {
			Document pronunciationPage;
			
			try {
				pronunciationPage = Jsoup.connect(pronunciationListLink).get();
			} catch (IOException e) {
				return null;
			}
			
			languageContainer = pronunciationPage.selectFirst("div#language-container-" + language.toString());
			
			if(languageContainer != null) {
				languageContainer = languageContainer.selectFirst("article.pronunciations:not([id])");
			}
			
			var languageContainerMap = cacheLanguageContainers(language, pronunciationPage, languageContainer);
			
			if(languageContainerMap != null) {
				pronunciationCache.putIfAbsent(word, languageContainerMap);
				
				cacheExpirationTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						pronunciationCache.remove(word);
					}
				}, 600000);
			}
		} else {
			languageContainer = pronunciationCacheMap.get(language);
		}
		

		if (languageContainer == null) {
			return null;
		}

		Elements nationalityList = languageContainer.select("span.from");
		Elements pronunciationList = languageContainer.select("span.play.icon-size-xl");

		if (nationalityList.size() != pronunciationList.size()) {
			ErrorLogger.LogIssue("More than one nationality match found. This should never happen.");
			return null;
		}

		Map<IForvoCountry, Element> countryFilter = new HashMap<>();

		for (int i = 0; i < nationalityList.size(); i++) {
			Element nationalityElement = nationalityList.get(i);
			String strippedUppercaseNationality = getNationality(nationalityElement)
					.toUpperCase()
					.replace(" ", "");
			
			if(strippedUppercaseNationality.isBlank()) {
				continue;
			}

			try {
				IForvoCountry pronunciationCountry;
				
				if(language == ForvoLanguage.English) {
					pronunciationCountry = EnglishForvoCountries.valueOf(strippedUppercaseNationality);
				} else {
					pronunciationCountry = SpanishForvoCountries.valueOf(strippedUppercaseNationality);
				}
				
				Element pronunciationElement = pronunciationList.get(i);

				if (pronunciationCountry == country) {
					return new Pair<String, String>(pronunciationElementToMP3Link(pronunciationElement), country.getPrettyName());
				}

				countryFilter.putIfAbsent(pronunciationCountry, pronunciationElement);
			} catch (IllegalArgumentException e) {
				continue;
			}
		}

		IForvoCountry closestPronunciationCountry = country.findClosestCountry(countryFilter.keySet());

		if (closestPronunciationCountry == null) {
			return null;
		}

		Element pronunciationElement = countryFilter.get(closestPronunciationCountry);

		if (pronunciationElement == null) {
			return null;
		}

		return new Pair<String, String>(pronunciationElementToMP3Link(pronunciationElement), closestPronunciationCountry.getPrettyName());
	}

	private static Map<ForvoLanguage, Element> cacheLanguageContainers(ForvoLanguage language, Document pronunciationPage, Element languageContainer) {
		Map<ForvoLanguage, Element> pronunciationCacheMap = new ConcurrentHashMap<>();
		
		if(languageContainer != null) {
			pronunciationCacheMap.put(language, languageContainer);
		}
		
		for(ForvoLanguage languageToCache : ForvoLanguage.values()) {
			if(language != languageToCache) {
				Element cachedLanguageContainer = pronunciationPage.selectFirst("div#language-container-" + languageToCache.toString());
				
				if(cachedLanguageContainer != null) {
					cachedLanguageContainer = cachedLanguageContainer.selectFirst("article.pronunciations:not([id])");
					pronunciationCacheMap.put(languageToCache, cachedLanguageContainer);
				}
			}
		}
		
		if(pronunciationCacheMap.isEmpty()) {
			return null;
		}
		
		return pronunciationCacheMap;
	}
	
	/**
	 * Turns an element containing the encoded partial MP3 link into a full link.
	 * 
	 * @param pronunciationElement The element
	 * @return The link to the MP3 for the recording. Returns an empty string if it fails.
	 */
	private static String pronunciationElementToMP3Link(Element pronunciationElement) {
		String pronunciationClickAttribute = pronunciationElement.attr("onclick");
		
		if(pronunciationClickAttribute.isBlank()) {
			ErrorLogger.LogIssue("The pronunciation's onclick attribute doesn't exist or is blank. This should never happen.");
			return "";
		} else if(!pronunciationClickAttribute.startsWith("Play(")) {
			ErrorLogger.LogIssue("The pronunciation's onclick attribute doesn't start with the Play function. This should never happen.");
			return "";
		}
		
		var possibleEncodedMP3Link = encodedArgumentPattern.matcher(pronunciationClickAttribute).results()
				.findFirst()
				.map(result -> result.group());
		
		if(possibleEncodedMP3Link.isPresent()) {
			String encodedMP3Link = possibleEncodedMP3Link.get();
			String decodedMP3PartialLink;
			
			try {
				byte[] decodedMP3LinkBytes = Base64.getDecoder().decode(encodedMP3Link);
				decodedMP3PartialLink = new String(decodedMP3LinkBytes, StandardCharsets.ISO_8859_1);
				
				if(decodedMP3PartialLink.endsWith(".mp3")) {
					return forvoMP3LinkPrefix + decodedMP3PartialLink;
				} else {
					ErrorLogger.LogIssue("The first decoded argument of the play function doesn't end in '.mp3'. This should never happen.");
					return "";
				}
			} catch(IllegalArgumentException e) {
				ErrorLogger.LogIssue("The first argument found wasn't in base-64. This should never happen.");
				return "";
			}
		} else {
			ErrorLogger.LogIssue("No argument for the play function could be found. This should never happen.");
			return "";
		}
	}
	
	/**
	 * Gets the nationality from an element
	 * 
	 * @param nationalityElement The element containing the nationality
	 * @return The nationality. Returns an empty string if it fails.
	 */
	private static String getNationality(Element nationalityElement) {
		String nationalityText = nationalityElement.text();
		
		if(nationalityText.isBlank()) {
			ErrorLogger.LogIssue("The text from the nationality element is blank. This should never happen.");
			return "";
		}
		
		Matcher nationalityMatcher = nationalityPattern.matcher(nationalityText);
		
		if(nationalityMatcher.find()) {
			String nationality = nationalityMatcher.group();

			if(nationalityMatcher.find()) {
				ErrorLogger.LogIssue("More than one nationality match found. This should never happen.");
				return "";
			}
			
			return nationality;
		} else {
			// No nationality was given.
			return "";
		}
	}
}
