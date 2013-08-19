package com.thevoiceasia.phonebox.calls;

import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.records.PhoneCall;

public class InfoPanelPopulator implements Runnable {

	private DatabaseManager database;
	private CallInfoPanel infoPanel;
	private String callerID, channelID;
	
	public InfoPanelPopulator(DatabaseManager database, CallInfoPanel infoPanel, 
			String callerID, String channelID) {
		
		this.database = database;
		this.infoPanel = infoPanel;
		this.callerID = callerID;
		this.channelID = channelID;
		
	}

	@Override
	public void run() {
		
		infoPanel.setPhoneCallRecord(new PhoneCall(database, callerID, channelID));
		
	}

}
