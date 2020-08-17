package com.deliburd.bot.burdbot.forvoscraper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public enum SpanishForvoCountries implements IForvoCountry {
	CHILE("cl", "Chile", "Argentina", "Uruguay", "Bolivia", "Peru"), 
	ARGENTINA("ar", "Argentina", "Uruguay", "Chile", "Paraguay", "Bolivia", "Peru"), 
	URUGUAY("uy", "Uruguay", "Argentina"), 
	PARAGUAY("py", "Paraguay", "Argentina", "Uruguay", "Bolivia", "Peru"), 
	BOLIVIA("bo", "Bolivia", "Peru", "Paraguay", "Chile", "Argentina"), 
	PERU("pe", "Perú", "Bolivia", "Chile", "Paraguay", "Ecuador"),
	ECUADOR("ec", "Ecuador", "Peru", "Colombia"),
	COLOMBIA("co", "Colombia", "Venezuela", "Cuba", "DominicanRepublic", "Ecuador", "Panama"),
	VENEZUELA("ve", "Venezuela", "Colombia", "Cuba", "DominicanRepublic"),
	PANAMA("pa", "Panamá", "CostaRica", "Nicaragua", "Honduras", "Colombia"),
	COSTARICA("cr", "Costa Rica", "Nicaragua", "Panama"),
	NICARAGUA("ni", "Nicaragua", "ElSalvador", "Honduras", "CostaRica"),
	HONDURAS("hn", "Honduras", "Nicaragua", "ElSalvador", "Guatemala"),
	ELSALVADOR("sv", "El Salvador", "Nicaragua", "Honduras", "Guatemala"),
	GUATEMALA("gt", "Guatemala", "Mexico", "ElSalvador", "Honduras"),
	MEXICO("mx", "México", "Guatemala"),
	CUBA("cu", "Cuba", "DominicanRepublic", "Colombia", "Venezuela"),
	DOMINICANREPUBLIC("dr", "Dominican Republic", "Cuba", "Colombia", "Venezuela"),
	SPAIN("es", "Spain", "Argentina", "Uruguay");
	
	

	private static final EnumMap<SpanishForvoCountries, List<SpanishForvoCountries>> countryProximityMap = new EnumMap<>(SpanishForvoCountries.class);
	private static final HashMap<String, SpanishForvoCountries> abbreviationToEnum = new HashMap<>();
	private final String countryAbbreviation;
	private final String prettyName;
	private final String[] closeCountries;

	static {
		SpanishForvoCountries[] countries = SpanishForvoCountries.values();
		for (SpanishForvoCountries country : countries) {
			abbreviationToEnum.put(country.getCountryAbbreviation(), country);

			List<SpanishForvoCountries> closeCountriesAsEnumList = new ArrayList<>(countries.length);
			addCountriesByProximityToList(country, closeCountriesAsEnumList);
			
			countryProximityMap.put(country, closeCountriesAsEnumList);
		}
	}

	private SpanishForvoCountries(String countryAbbreviation, String prettyName, String... closeCountries) {
		this.countryAbbreviation = countryAbbreviation;
		this.prettyName = prettyName;
		this.closeCountries = closeCountries;
	}
	
	private static void addCountriesByProximityToList(SpanishForvoCountries country, List<SpanishForvoCountries> countryListToPopulate) {
		Set<String> excludedCountries = new HashSet<>();
		excludedCountries.add(country.toString());

		for(String closeCountryName : country.getCloseCountries()) {
			String uppercaseCountry = closeCountryName.toUpperCase();
			countryListToPopulate.add(valueOf(uppercaseCountry));
			excludedCountries.add(uppercaseCountry);
		}
		
		addCountriesByProximityToList(country, countryListToPopulate, excludedCountries);
		
		SpanishForvoCountries[] countries = SpanishForvoCountries.values();
		
		if(countryListToPopulate.size() != SpanishForvoCountries.values().length) {
			for(SpanishForvoCountries missingCountry : countries) {
				if(!excludedCountries.contains(missingCountry.toString())) {
					countryListToPopulate.add(missingCountry);
				}
			}
		}
	}
	
	private static void addCountriesByProximityToList(IForvoCountry country, List<SpanishForvoCountries> countryListToPopulate, Set<String> excludedCountries) {
		int currentDepthPosition = 0;
		
		while(countryListToPopulate.size() != currentDepthPosition) {
			int sizeBeforePopulating = countryListToPopulate.size();
			for(int i = currentDepthPosition; i < sizeBeforePopulating; i++) {
				for(String countryName : countryListToPopulate.get(i).getCloseCountries()) {
					String uppercaseCountry = countryName.toUpperCase();
					
					if(excludedCountries.contains(uppercaseCountry)) {
						continue;
					}
					
					SpanishForvoCountries countryToAdd = valueOf(uppercaseCountry);
					countryListToPopulate.add(countryToAdd);
					excludedCountries.add(uppercaseCountry);
				}
			}
			
			currentDepthPosition = sizeBeforePopulating;
		}
	}

	/**
	 * Gets the country's 2 letter abbreviation
	 * 
	 * @return The country's 2 letter abbreviation
	 */
	@Override
	public String getCountryAbbreviation() {
		return countryAbbreviation;
	}
	
	/**
	 * Gets the pretty name of the country
	 * 
	 * @return The pretty name of the country
	 */
	@Override
	public String getPrettyName() {
		return prettyName;
	}

	/**
	 * Gets an array of countries as strings closest in accent to the country
	 * 
	 * @return An array of countries as strings closest in accent to the country
	 */
	private String[] getCloseCountries() {
		return closeCountries;
	}

	/**
	 * Converts an abbreviation to a country. This is not case-sensitive
	 * 
	 * @param abbreviation The country's 2 letter abbreviation
	 * @return The country. Null if the abbreviation doesn't match a country.
	 */
	public static IForvoCountry abbreviationToCountry(String abbreviation) {
		return abbreviationToEnum.get(abbreviation.toLowerCase());
	}
	
	/**
	 * Finds the closest country in accent to the given country. This does not include itself.
	 * 
	 * @param country The country
	 * @param countryFilter The set of countries that can be returned.
	 * @return The closest country found. Null if it no country could be found.
	 */
	@Override
	public IForvoCountry findClosestCountry(Set<IForvoCountry> countryFilter) {
		for(IForvoCountry countryByProximity : countryProximityMap.get(this)) {
			if(countryFilter.contains(countryByProximity)) {
				return countryByProximity;
			}
		}
		
		return null;
	}
}
