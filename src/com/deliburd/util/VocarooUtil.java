package com.deliburd.util;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class VocarooUtil {
	private static final Pattern vocarooLinkPattern = Pattern.compile("https?://(?:www\\.)?(?:voca\\.ro|vocaroo\\.com)/([a-zA-Z0-9]+)");
	
	private VocarooUtil() {}
	
	public static List<String> extractVocarooDownloadLinks(String text) {
		return vocarooLinkPattern.matcher(text).results()
				.map(result -> "https://media1.vocaroo.com/mp3/" + result.group(1))
				.collect(Collectors.toList());
	}
}
