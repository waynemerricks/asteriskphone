package com.github.waynemerricks.asteriskphone.records;

import java.util.Date;

public class Conversation {

	private String conversation;
	private Date timeStamp;
	
	/** 
	 * Create a new object with the specified conversation string
	 * Initialises the timestamp to the current system time
	 * We use the java.util.Date().getTime() to convert to GMT millis
	 * to get around time zone confusion when storing in DB
	 * 
	 * Wherever possible SQL TIMESTAMP fields are used directly so Java
	 * does not need to time stamp anything.
	 * 
	 * @param conversation
	 */
	public Conversation(String conversation){
		
		this.conversation = conversation;
		timeStamp = new Date();
		
	}
	
	/**
	 * Create new object with given date and conversation
	 * @param timeStamp
	 * @param conversation
	 */
	public Conversation(Date timeStamp, String conversation){
		
		this.conversation = conversation;
		this.timeStamp = timeStamp;
		
	}
	
	/**
	 * Returns set timestamp on this object
	 * @return
	 */
	public Date getTime(){
		
		return timeStamp;
		
	}
	
	/**
	 * Returns conversation this object holds
	 * @return
	 */
	public String getConversation(){
		
		return conversation;
		
	}
	
}
