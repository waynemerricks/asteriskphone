package com.thevoiceasia.phonebox.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class ActivePersonChanger implements Runnable {

	private String channelID;
	private long activePerson;
	private Connection writeConnection;
	private I18NStrings xStrings;
	
	private static final Logger LOGGER = Logger.getLogger(ActivePersonChanger.class.getName());//Logger
	
	/**
	 * Changes the person associated with the given channel ID in the database
	 * @param language I18N Language
	 * @param country I18N Country
	 * @param readConnection DB Read Connection
	 * @param writeConnection DB Write Connection
	 * @param controlRoom Control room to send XMPP messages upon completion
	 * @param personID ID of the person to change to
	 * @param channelID Channel of call to change
	 */
	public ActivePersonChanger(String language, String country, 
			Connection writeConnection, String activePerson, String channelID) {
		
		xStrings = new I18NStrings(language, country);
		
		this.writeConnection = writeConnection;
		this.activePerson = Integer.parseInt(activePerson);
		this.channelID = channelID;
		
		LOGGER.info(xStrings.getString("ActivePersonChanger.ChangingPerson") +  
				"\n\tChannel: " + channelID + "\n\tNew Person ID: " + activePerson);
		
	}
	
	@Override
	public void run() {
		
		updateCallLog();
		
	}

	/**
	 * Updates the callhistory table for this objects channel
	 * Will add a C state record to signify person was manually changed
	 * on this call.
	 * @return success 
	 */
	private boolean updateCallLog() {
		
		// UPDATE callhistory SET activePerson = activePerson WHERE callchannel = channelID
		boolean success = false;
		String SQL = null;
		PreparedStatement statement = null;
		
		String error = xStrings.getString(
				"ActivePersonChanger.errorUpdatingCallLog") + 
				"\n\tChannel: " + channelID + 
				"\n\tPersonID: " + activePerson; 

		try{
				
			SQL = "UPDATE `callhistory` SET `activePerson` = ? " + 
					"WHERE callchannel = ?"; 
			
			statement = writeConnection.prepareStatement(SQL);
			statement.setLong(1, activePerson);
			statement.setString(2, channelID);
			
			if(statement.executeUpdate() == 0)
				showError(new SQLException(error), error);
			else
				success = true;
			
		}catch(SQLException e){
			
			showError(e, error);
			
		}finally{
			
			if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
				
		}
		
		return success;
		
	}

	/**
	 * Logs a severe SQL Error to the LOGGER object
	 * @param e Exception to stack trace
	 * @param friendlyError Error as it shows up in the LOGGER object
	 */
	private void showError(SQLException e, String friendlyError) {
		
		e.printStackTrace();
		LOGGER.severe(friendlyError);
		
	}

}

	
