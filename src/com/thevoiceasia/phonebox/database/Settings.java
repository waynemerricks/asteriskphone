package com.thevoiceasia.phonebox.database;

import java.util.HashMap;

/**
 * Helper Class storing settings from Database
 * @author Wayne Merricks
 *
 */
public class Settings {
	
	private HashMap<String, String> settings;
	
	public void storeSettings(HashMap<String, String> settings){
		
		this.settings = settings;
		
	}
	
	public String getLanguage(){
		
		return settings.get("language"); //$NON-NLS-1$
		
	}
	
	public String getCountry(){
		
		return settings.get("country"); //$NON-NLS-1$
		
	}
	
	public String getXMPPServer(){
		
		return settings.get("XMPPServer"); //$NON-NLS-1$
		
	}
	
	public String getXMPPDomain(){
		
		return settings.get("XMPPDomain"); //$NON-NLS-1$
		
	}
	
	public String getXMPPRoom(){
		
		return settings.get("XMPPRoom"); //$NON-NLS-1$
		
	}
	
	public boolean isStudio(){
		
		return settings.get("isStudio").equals("true");  //$NON-NLS-1$//$NON-NLS-2$
		
	}
	
	public String getHashedPassword(){
		
		return settings.get("password"); //$NON-NLS-1$
		
	}
	
	public String getNickName(){
		
		return settings.get("nickName"); //$NON-NLS-1$
		
	}
	
}
