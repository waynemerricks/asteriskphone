package com.thevoiceasia.phonebox.database;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NStrings {
	
	private static final String BUNDLE_NAME = "com.thevoiceasia.phonebox.database.strings"; 
	private ResourceBundle RESOURCE_BUNDLE;
	
	public I18NStrings(String language, String country){
		
		Locale currentLocale = new Locale(language, country);
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, currentLocale);
		
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
