package com.github.waynemerricks.asteriskphone.misc;

public class Country {

	private String name, ISO, phone;
	
	public Country(String name, String ISOCode, String phonePrefix){
		this.name = name;
		this.ISO = ISOCode;
		this.phone = phonePrefix;
	}
	
	public String getName(){
		return name;
	}
	
	public String getISOCode(){
		return ISO;
	}
	
	public String getPhoneCode(){
		return phone;
	}
	
}
