package com.thevoiceasia.phonebox.launcher;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18NStrings {
	
	private static final String BUNDLE_NAME = "com.thevoiceasia.phonebox.launcher.strings"; //$NON-NLS-1$
	private static ResourceBundle RESOURCE_BUNDLE;
	
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
}
