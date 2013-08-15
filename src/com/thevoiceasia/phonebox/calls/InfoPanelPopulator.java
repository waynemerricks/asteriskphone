package com.thevoiceasia.phonebox.calls;

import java.sql.Connection;

public class InfoPanelPopulator implements Runnable {

	private Connection databaseConnection;
	private CallInfoPanel infoPanel;
	
	public InfoPanelPopulator(Connection databaseConnection, CallInfoPanel infoPanel) {
		
		this.databaseConnection = databaseConnection;
		this.infoPanel = infoPanel;
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}

}
