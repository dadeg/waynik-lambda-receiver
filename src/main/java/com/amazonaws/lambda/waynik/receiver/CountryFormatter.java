package com.amazonaws.lambda.waynik.receiver;

public class CountryFormatter {
	private static String[] countryFragmentsThatNeedDefiniteArticle = {
 		"Bahamas",
 		"Islands",
 		"Republic",
 		"Comoros",
 		"Gambia",
 		"Isle of Man",
 		"Ivory Coast",
 		"Maldives",
 		"Netherlands",
 		"Philippines",
 		"United Arab Emirates",
 		"United Kingdom",
 		"United States"
	};
 	
 	private CountryFormatter() { }
 	
 	public static String format(String countryName)
 	{
 		return addDefiniteArticleIfNeeded(countryName);
 	}
 	
 	private static String addDefiniteArticleIfNeeded(String countryName)
 	{
 		for (String fragment : countryFragmentsThatNeedDefiniteArticle) {
 			if (countryName.toLowerCase().contains(fragment.toLowerCase())) {
 				return "the " + countryName;
 			}
 		}
 		return countryName;
 	}
}
