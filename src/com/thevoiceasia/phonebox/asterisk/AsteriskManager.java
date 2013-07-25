package com.thevoiceasia.phonebox.asterisk;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.internal.AsteriskAgentImpl;

public class AsteriskManager implements AsteriskServerListener {

	//STATICS
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	
	public AsteriskManager(){
		
		asteriskServer = new DefaultAsteriskServer("10.43.10.91", "phonemanager", "P0l0m1nt");
		
	}
	
	public void connect() throws ManagerCommunicationException {
		
		asteriskServer.initialize();
		
	}
	
	public void disconnect() {
		
		asteriskServer.shutdown();
		
	}

	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//TODO 
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//TODO
		
	}
	
	@Override
	public void onNewMeetMeUser(MeetMeUser user) {}

	@Override
	public void onNewAgent(AsteriskAgentImpl agent) {}
	
}
