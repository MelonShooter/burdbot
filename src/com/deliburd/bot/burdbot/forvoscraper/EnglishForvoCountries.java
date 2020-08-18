package com.deliburd.bot.burdbot.forvoscraper;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public enum EnglishForvoCountries implements IForvoCountry {
	AUSTRALIA("au", "Australia", "NewZealand", "UnitedKingdom"),
	CANADA("ca", "Canada", "UnitedStates", "UnitedKingdom"),
	IRELAND("ie", "Ireland", "UnitedKingdom"),
	NEWZEALAND("nz", "New Zealand", "Australia", "UnitedKingdom"),
	UNITEDKINGDOM("uk", "United Kingdom", "Australia", "Ireland", "Canada", "UnitedStates"),
	UNITEDSTATES("us", "United States", "Canada");

	private static final EnumMap<EnglishForvoCountries, List<EnglishForvoCountries>> countryProximityMap = new EnumMap<>(EnglishForvoCountries.class);
	private static final HashMap<String, EnglishForvoCountries> abbreviationToEnum = new HashMap<>();
	private final String countryAbbreviation;
	private final String prettyName;
	private final String[] closeCountries;

	static {
		EnglishForvoCountries[] countries = EnglishForvoCountries.values();
		for (EnglishForvoCountries country : countries) {
			abbreviationToEnum.put(country.getCountryAbbreviation(), country);

			List<EnglishForvoCountries> closeCountriesAsEnumList = new ArrayList<>(countries.length);
			addCountriesByProximityToList(country, closeCountriesAsEnumList);
			
			countryProximityMap.put(country, closeCountriesAsEnumList);
		}
	}

	private EnglishForvoCountries(String countryAbbreviation, String prettyName, String... closeCountries) {
		this.countryAbbreviation = countryAbbreviation;
		this.prettyName = prettyName;
		this.closeCountries = closeCountries;
	}
	
	private static void addCountriesByProximityToList(EnglishForvoCountries country, List<EnglishForvoCountries> countryListToPopulate) {
		Set<String> excludedCountries = new HashSet<>();
		excludedCountries.add(country.toString());

		for(String closeCountryName : country.getCloseCountries()) {
			String uppercaseCountry = closeCountryName.toUpperCase();
			countryListToPopulate.add(valueOf(uppercaseCountry));
			excludedCountries.add(uppercaseCountry);
		}
		
		addCountriesByProximityToList(country, countryListToPopulate, excludedCountries);
		
		EnglishForvoCountries[] countries = EnglishForvoCountries.values();
		
		if(countryListToPopulate.size() != EnglishForvoCountries.values().length) {
			for(EnglishForvoCountries missingCountry : countries) {
				if(!excludedCountries.contains(missingCountry.toString())) {
					countryListToPopulate.add(missingCountry);
				}
			}
		}
	}
	
	private static void addCountriesByProximityToList(IForvoCountry country, List<EnglishForvoCountries> countryListToPopulate, Set<String> excludedCountries) {
		int currentDepthPosition = 0;
		
		while(countryListToPopulate.size() != currentDepthPosition) {
			int sizeBeforePopulating = countryListToPopulate.size();
			for(int i = currentDepthPosition; i < sizeBeforePopulating; i++) {
				for(String countryName : countryListToPopulate.get(i).getCloseCountries()) {
					String uppercaseCountry = countryName.toUpperCase();
					
					if(excludedCountries.contains(uppercaseCountry)) {
						continue;
					}
					
					EnglishForvoCountries countryToAdd = valueOf(uppercaseCountry);
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
