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
import java.util.concurrent.atomic.AtomicInteger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.deliburd.readingpuller.TextConstant;
import com.deliburd.readingpuller.ReadingManager.ScraperDifficulty;
import com.deliburd.readingpuller.ReadingManager.ScraperLanguage;
import com.deliburd.util.ArrayUtil;
import com.deliburd.util.ErrorLogger;
import com.deliburd.util.scraper.ScraperManager.ScraperType;

public class LaNacionSpanishScraper implements Scraper {
	private static final int MAX_FAIL_COUNT = 10;
	private static final String LA_NACION_LINK_PAGE = "https://www.lanacion.com.ar/cultura";
	private static final String LA_NACION_MAIN_PAGE = "https://www.lanacion.com.ar";
	private static long lastLinkPull = 0;
	private static List<String> laNacionArticles;

	private List<String> getArticleLinks() {
		Document laNacionLinkPage;

		try {
			laNacionLinkPage = Jsoup.connect(LA_NACION_LINK_PAGE).get();
		} catch (IOException e) {
			return null;
		}

		Elements articleLinks = laNacionLinkPage.select("article.mod-article > div > section > figure > a[href]");
		articleLinks.addAll(laNacionLinkPage.select("article.mod-caja-nota > div > section > figure > a[href]")); // Add links in the most popular bar.

		return ArrayUtil.prependStringToList(LA_NACION_MAIN_PAGE, articleLinks.eachAttr("href"));
	}
	
	private String stripArticle(Document story) {
		Elements paragraphs = story.select("p.com-paragraph");
		
		return paragraphs.text();
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
			
			if(laNacionArticles == null || laNacionArticles.isEmpty()) {
				throw new Exception("Links from La nacion could not be fetched");
			}
			
			int randomLinkIndex = ArrayUtil.randomCollectionIndex(laNacionArticles);
			randomLink = laNacionArticles.get(randomLinkIndex);
			laNacionArticles.remove(randomLinkIndex);
		}
		
		try {
			return stripArticle(Jsoup.connect(randomLink).get());
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	public String[] getRandomTextBodies(int numberOfTexts) throws ExecutionException {
		var textBodies = new String[numberOfTexts];
		var futureTexts = new ArrayList<Future<String>>();
		AtomicInteger failedFetchCount = new AtomicInteger();
		
		ExecutorService executorService = Executors.newCachedThreadPool();
		
		for(int i = 0; i < numberOfTexts; i++) {
			var futureText = executorService.submit( new Callable<String>() {
				@Override
				public String call() throws Exception {
					String text = getRandomTextBody();
					while(text != null && text.equals("")) {
						int failedCount = failedFetchCount.incrementAndGet();
						
						if(failedCount > MAX_FAIL_COUNT) {
							throw new IllegalStateException("More than 5 articles total have failed. Cancelling the fetch retry.");
						}
						
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
			}

			i++;
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
