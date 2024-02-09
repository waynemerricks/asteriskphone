package com.thevoiceasia.phonebox.calls;

public class EndPointRecord {

	public String callerChannel, receiverChannel, receiverCallerID;
	
	public EndPointRecord(String callerChannel, String receiverChannel, String receiverCallerID) {
	
		this.callerChannel = callerChannel;
		this.receiverChannel = receiverChannel;
		this.receiverCallerID = receiverCallerID;
		
	}

	public String toString(){
		
		return "ENDPOINT RECORD:\n\tCaller Channel: " + callerChannel  
				+ "\n\tReceiver Channel: " + receiverChannel 
				+ "\n\tReceiver Caller ID: " + receiverCallerID + "\n";
		
	}
}
