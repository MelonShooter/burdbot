package com.deliburd.util.scraper;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
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
import com.deliburd.util.scraper.ScraperFactory.ScraperType;

public class LaNacionSpanishScraper implements Scraper {
	private static final String LA_NACION_LINK_PAGE = "https://www.lanacion.com.ar/cultura";
	private static final String LA_NACION_MAIN_PAGE = "https://www.lanacion.com.ar";
	private static double lastLinkPull = 0;
	private static ArrayList<String> laNacionArticles = new ArrayList<String>(32);

	private ArrayList<String> getArticleLinks() {
		Document laNacionLinkPage;

		try {
			laNacionLinkPage = Jsoup.connect(LA_NACION_LINK_PAGE).get();
		} catch (IOException e) {
			return null;
		}

		Elements aTagLinksTitles = laNacionLinkPage.select("article.nota > h2 > a[href]");
		
		var linkList = aTagLinksTitles.eachAttr("href");
		ArrayUtil.prependAndAppendStringToList(LA_NACION_MAIN_PAGE, linkList, "/////");
		
		return ArrayUtil.concatStringLists(linkList, aTagLinksTitles.eachText());
	}
	
	private String stripArticle(Document story, String title) {
		String sectionClass = "section#cuerpo";
		String headerContent = "p.capital";
		String endContent = "section.listado.ademas.redaccion.notas4.floatFix";
		Element body = story.select(sectionClass).first().children().select(headerContent).first();
		Elements articleText = new Elements();
		articleText.add(body);

		boolean end = false;
		
		while(!end) {
			body = body.nextElementSibling();
			
			if(body.is("p")) {
				articleText.add(body);
			} else if(body.is(endContent)) {
				end = true;
			}
		}
		
		return articleText.text();
	}
	
	@Override
	public String getRandomTextBody() throws Exception {
		String randomLink;
		
		synchronized (this) {
			var currentTime = Instant.now().getEpochSecond();

			if (lastLinkPull + TextConstant.LINK_PULL_COOLDOWN <= currentTime || laNacionArticles.isEmpty()) { // Cooldown is over or laNacionArticles is empty

				laNacionArticles = getArticleLinks();
				lastLinkPull = currentTime;
			}
			
			if(laNacionArticles == null) {
				throw new Exception("Links from La nacion could not be fetched");
			}
			
			int randomLinkIndex = ArrayUtil.randomArrayIndex(laNacionArticles);
			randomLink = laNacionArticles.get(randomLinkIndex);
			laNacionArticles.remove(randomLinkIndex);
		}
		
		Pattern link = Pattern.compile("^.*/////");
		Matcher matchLink = link.matcher(randomLink);
		
		try {
			if(matchLink.find()) {
				return stripArticle(Jsoup.connect(randomLink.substring(0, matchLink.end())).get(), randomLink.substring(matchLink.end()));
			} else {
				throw new Exception("Link from La nacion malformed. Link + Title: " + randomLink);
			}
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	public String[] getRandomTextBodies(int numberOfTexts) throws ExecutionException {
		var textBodies = new String[numberOfTexts];
		var futureTexts = new ArrayList<Future<String>>();
		
		ExecutorService executorService = Executors.newCachedThreadPool();
		
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
	
	@Override
	public ScraperLanguage getLanguage() {
		return ScraperLanguage.Spanish;
	}

	@Override
	public ScraperType getSource() {
		return ScraperType.LaNacion;
	}

	@Override
	public int getRecommendedTextAmount() {
		return 30;
	}

	@Override
	public ScraperDifficulty getDifficulty() {
		return ScraperDifficulty.Medium;
	}

}
