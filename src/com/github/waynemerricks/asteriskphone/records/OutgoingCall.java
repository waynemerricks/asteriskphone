package com.github.waynemerricks.asteriskphone.records;

import java.util.Date;

public class OutgoingCall {

	public String source, channel, destination;
	public long creationTime = new Date().getTime();
	
	/**
	 * Helper object that stores potential outgoing call info
	 * @param extension
	 * @param channel
	 * @param destination
	 */
	public OutgoingCall(String channel, String source, String destination){
		
		this.source = source;
		this.channel = channel;
		this.destination = destination;
		
	}
	
	/**
	 * Formats this as channel/source/destination
	 */
	public String toString(){
		
		return channel + "/" + source + "/" + destination;
		
	}
	
}
