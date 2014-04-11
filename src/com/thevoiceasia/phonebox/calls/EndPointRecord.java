package com.thevoiceasia.phonebox.calls;

public class EndPointRecord {

	public String callerChannel, receiverChannel, receiverCallerID;
	
	public EndPointRecord(String callerChannel, String receiverChannel, String receiverCallerID) {
	
		this.callerChannel = callerChannel;
		this.receiverChannel = receiverChannel;
		this.receiverCallerID = receiverCallerID;
		
	}

}
