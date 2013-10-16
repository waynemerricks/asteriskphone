package com.thevoiceasia.phonebox.calls;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NStrings {
	
	private static final String BUNDLE_NAME = "com.thevoiceasia.phonebox.calls.strings"; //$NON-NLS-1$
	private ResourceBundle RESOURCE_BUNDLE;
	private String language, country;
	
	public I18NStrings(String language, String country){
		
		this.language = language;
		this.country = country;
		Locale currentLocale = new Locale(language, country);
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
		
	}

	/**
	 * Returns the locale this is using in a comma separated list
	 * E.g. en GB becomes en,GB
	 * @return
	 */
	public String getLocale(){
		return language + "," + country; //$NON-NLS-1$
	}
	
	
	public String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
	public static String getString(String key, String language, String country){
		
		Locale currentLocale = new Locale(language, country);
		ResourceBundle resource = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
		
		try {
			return resource.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
		
	}

}
