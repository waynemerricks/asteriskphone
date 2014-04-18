package com.thevoiceasia.phonebox.calls;

public class EndPointRecord {

	public String callerChannel, receiverChannel, receiverCallerID;
	
	public EndPointRecord(String callerChannel, String receiverChannel, String receiverCallerID) {
	
		this.callerChannel = callerChannel;
		this.receiverChannel = receiverChannel;
		this.receiverCallerID = receiverCallerID;
		
	}

	public String toString(){
		
		return "ENDPOINT RECORD:\n\tCaller Channel: " + callerChannel  //$NON-NLS-1$
				+ "\n\tReceiver Channel: " + receiverChannel //$NON-NLS-1$
				+ "\n\tReceiver Caller ID: " + receiverCallerID + "\n";  //$NON-NLS-1$//$NON-NLS-2$
		
	}
}
