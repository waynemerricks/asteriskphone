package com.thevoiceasia.phonebox.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.jivesoftware.smackx.muc.MultiUserChat;

public class PersonChanger implements Runnable {

	private String fieldMapping, channelID, value;
	private Connection readConnection, writeConnection;
	private I18NStrings xStrings;
	private MultiUserChat controlRoom = null;
	private int personID = -1;
	
	private static final Logger LOGGER = Logger.getLogger(PersonChanger.class.getName());//Logger
	
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
	public PersonChanger(String language, String country, Connection readConnection, 
			Connection writeConnection, MultiUserChat controlRoom, int personID, 
			String channelID) {
		
		xStrings = new I18NStrings(language, country);
		
		this.readConnection = readConnection;
		this.writeConnection = writeConnection;
		this.personID = personID;
		this.controlRoom = controlRoom;
		
		LOGGER.info(xStrings.getString("PersonChanger.ChangingPerson") +  //$NON-NLS-1$
				"\n\tChannel: " + channelID + "\n\tNew Person ID: " + personID); //$NON-NLS-1$ //$NON-NLS-2$
		
	}

	@Override
	public void run() {
		
		//TODO

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

	
