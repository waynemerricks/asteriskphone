package com.thevoiceasia.phonebox.records;

import java.util.Date;

public class OutgoingCall {

	public String extension, channel, destination;
	public long creationTime = new Date().getTime();
	
	/**
	 * Helper object that stores potential outgoing call info
	 * @param extension
	 * @param channel
	 * @param destination
	 */
	public OutgoingCall(String extension, String channel, String destination){
		
		this.extension = extension;
		this.channel = channel;
		this.destination = destination;
		
	}
	
}
