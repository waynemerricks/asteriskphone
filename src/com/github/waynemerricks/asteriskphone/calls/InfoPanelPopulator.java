package com.github.waynemerricks.asteriskphone.calls;

import com.github.waynemerricks.asteriskphone.database.DatabaseManager;
import com.github.waynemerricks.asteriskphone.records.PhoneCall;

public class InfoPanelPopulator implements Runnable {

	private DatabaseManager database;
	private CallInfoPanel infoPanel;
	private String callerID, channelID, callLocation;

	public InfoPanelPopulator(DatabaseManager database, CallInfoPanel infoPanel, 
			String callerID, String channelID, String callLocation) {
		
		this.database = database;
		this.infoPanel = infoPanel;
		this.callerID = callerID;
		this.channelID = channelID;
		this.callLocation = callLocation;
		
	}

	@Override
	public void run() {
		
		infoPanel.setPhoneCallRecord(new PhoneCall(database, callerID, channelID, 
				callLocation));
		
	}

}
