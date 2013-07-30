package com.thevoiceasia.phonebox.database;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Helper Class storing settings from Database
 * @author Wayne Merricks
 *
 */
public class Settings {
	
	private static final String BUNDLE_NAME = "com.thevoiceasia.phonebox.database.settings"; //$NON-NLS-1$
	private ResourceBundle RESOURCE_BUNDLE;
	
	public Settings(){
		
		RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
		
	}

	public String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
	
}
