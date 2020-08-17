package com.deliburd.bot.burdbot.forvoscraper;

import java.util.Set;

public interface IForvoCountry {

	/**
	 * Gets the country's 2 letter abbreviation
	 * 
	 * @return The country's 2 letter abbreviation
	 */
	public String getCountryAbbreviation();
	
	/**
	 * Gets the pretty name of the country
	 * 
	 * @return The pretty name of the country
	 */
	public String getPrettyName();
	
	/**
	 * Finds the closest country in accent to the given country. This does not include itself.
	 * 
	 * @param country The country
	 * @param countryFilter The set of countries that can be returned.
	 * @return The closest country found. Null if it no country could be found.
	 */
	public IForvoCountry findClosestCountry(Set<IForvoCountry> countryFilter);
}