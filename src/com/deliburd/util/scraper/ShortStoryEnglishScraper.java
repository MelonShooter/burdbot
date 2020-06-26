package com.deliburd.util.scraper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.deliburd.readingpuller.TextConstant;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

public class ShortStoryEnglishScraper implements Scraper {
	private static final String SHORTSTORYLINKPAGE = "https://easystoriesinenglish.com/category/beginner/";
	private static double lastLinkPull = 0;
	private static ArrayList<String> storyLinks = new ArrayList<String>(32);
	
	@Override
	public String getRandomTextBody() throws Exception {
		String randomLink;
		
		synchronized (this) {
			var currentTime = Instant.now().getEpochSecond();

			if (lastLinkPull + TextConstant.LINK_PULL_COOLDOWN <= currentTime || storyLinks.isEmpty()) { // Cooldown is over or storyLinks is empty
				storyLinks = getStoryLinks();
				
				lastLinkPull = currentTime;
			}
			
			if(storyLinks == null) {
				throw new Exception("Links from Easy Stories in English could not be fetched");
			}
			
			int randomLinkIndex = ArrayUtil.randomArrayIndex(storyLinks);
			randomLink = storyLinks.get(randomLinkIndex);
			storyLinks.remove(randomLinkIndex);
		}
		
		Pattern link = Pattern.compile("^.*/////");
		Matcher matchLink = link.matcher(randomLink);
		
		try {
			if(matchLink.find()) {
				return stripStory(Jsoup.connect(randomLink.substring(0, matchLink.end())).get(), randomLink.substring(matchLink.end()));
			} else {
				throw new Exception("Link from Easy Stories in English malformed. Link + Title: " + randomLink);
			}
		} catch (IOException e) {
			return ""; //Determine what error is the normal one and if it is that one, return "" otherwise throw an IOException saying something went wrong.
		}
	}

	@Override
	public String[] getRandomTextBodies(int numberOfTexts) throws ExecutionException {
		var textBodies = new String[numberOfTexts];
		var futureTexts = new ArrayList<Future<String>>();
		
		ExecutorService executorService = Executors.newFixedThreadPool(5); // Ensures request comes back because of site's limits.
		
		for(int i = 0; i < numberOfTexts; i++) {
			var futureText = executorService.submit( new Callable<String>() {
					@Override
				public String call() throws Exception {
					String text = getRandomTextBody();
					
					while(text != null && text.equals("")) {
						text = getRandomTextBody();
					}
					
					return text;
				}
			});
			
			futureTexts.add(futureText);
		}
		
		executorService.shutdown();
		
		int i = 0;
		
		for(var future : futureTexts) {
			try {
				var currentValue = future.get();
				if(currentValue != null) {
					textBodies[i] = currentValue;
				} else {
					continue;
				}
			} catch (InterruptedException e) {
				ErrorLogger.LogException(e);
			} finally {
				i++;
			}
		}
		
		return textBodies;
	}
	
	private static ArrayList<String> getStoryLinks() {
		var links = new ArrayList<String>(32);
		List<String> pages;
		
		try {
			final Document currentDocument = Jsoup.connect(SHORTSTORYLINKPAGE).get();
			ExecutorService executorService = Executors.newCachedThreadPool();
			pages = currentDocument.select("a[class=page-numbers][href]").eachAttr("href"); // Gets page links
			pages.add("default");
			
			var futurePages = new ArrayList<Future<ArrayList<String>>>();
			
			for(String page : pages) {
				var futurePage = executorService.submit( new Callable<ArrayList<String>>() {
					@Override
					public ArrayList<String> call() {
						try {
							var storyLinks = new ArrayList<String>();
							Document storyPage;
							
							if(page.equals("default")) {
								storyPage = currentDocument;
							} else {
								storyPage = Jsoup.connect(page).get();
							}
							
							for(Element element : storyPage.select("h2[class=secondline-blog-title]")) {
								storyLinks.add(element.child(0).attr("href") + "////" + element.child(0).text()); //Link with title at the end
							}
							
							return storyLinks;
						} catch (IOException e) {
							return null;
						}
					}
				});
				
				futurePages.add(futurePage);
			}
			
			executorService.shutdown();
			
			for(var future : futurePages) {
				try {
					var currentValue = future.get();
					if(currentValue != null) {
						links.addAll(currentValue);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
					continue;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		return links;
	}
	
	private String stripStory(Document story, String title) {
		String divClass = "div.secondline-themes-blog-single-excerpt";
		String headerContent = "h1:contains(" + title + "), h2:contains(" + title + "), h3:contains(" + title + "), h4:contains(" + title + ")";
		String endContent = "h1:contains(THE END), h2:contains(THE END), h3:contains(THE END), h4:contains(THE END)";
		Element body = story.select(divClass).first().children().select(headerContent).first();
		Elements storyText = new Elements();
		boolean end = false;
		
		while(!end) {
			body = body.nextElementSibling();

			if(body.is("p")) {
				storyText.add(body);
			} else if(body.is(endContent)) {
				end = true;
			}
		}
		
		return storyText.text();
	}

	@Override
	public ScraperLanguage getLanguage() {
		return ScraperLanguage.English;
	}

	@Override
	public ScraperType getSource() {
		return ScraperType.EnglishStory;
	}

	@Override
	public int getRecommendedTextAmount() {
		return 9;
	}
	
	@Override
	public ScraperDifficulty getDifficulty() {
		return ScraperDifficulty.Easy;
	}
}
