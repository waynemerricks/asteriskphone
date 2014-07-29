package com.thevoiceasia.phonebox.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.jivesoftware.smackx.muc.MultiUserChat;

public class PersonChanger implements Runnable {

	private String channelID;
	private Connection readConnection, writeConnection;
	private I18NStrings xStrings;
	private MultiUserChat controlRoom = null;
	private long personID = -1;
	
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
		
		//TODO Create new person if we're -1 and not an valid id
		if(personID == -1)//Create New
			personID = createNewPerson();
		
		boolean success = false;
		
		if(personID != -1)
			success = updateConversation();
		
		if(success)
			success = updateCallLog();
		
		if(!success){
			/* TODO Show Error but also show failed to clients via control XMPP
			 * because we'll have the initiator client waiting for the result
			 */
		}
		
	}

	private boolean updateCallLog() {
		// TODO Auto-generated method stub
		/*INSERT INTO callhistory (phonenumber, state, operator, callchannel, "
		+ "activePerson) VALUES ('" + phoneNumber + "', 'C', '" + userName + "', '"
				+ notifyMe.get(0).getChannel() + "', " + newRecordID + ")";*/
		return false;
	}

	private boolean updateConversation() {
		// TODO Auto-generated method stub
		//SQL = "UPDATE conversations SET person_id = " + personID + 
		//	WHERE channel = channelID
		boolean success = false;
		String error = xStrings.getString(
				"PersonChanger.errorUpdatingConversation") + //$NON-NLS-1$
				"\n\tChannel: " + channelID + //$NON-NLS-1$
				"\n\tPersonID: " + personID; //$NON-NLS-1$

		PreparedStatement statement = null;
		String SQL = null;
		
		try{
			
			SQL = "UPDATE `conversations` SET `person_id` = ? WHERE `channel` = ?"; //$NON-NLS-1$
			
			statement = writeConnection.prepareStatement(SQL);
			statement.setLong(1, personID);
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
	 * Inserts a blank record into the person table
	 * @return the id of the inserted record or -1 if failed
	 */
	private long createNewPerson() {
		
		//String SQL = "INSERT INTO person VALUES()";//Retain ID
		long insertID = -1;
		
		PreparedStatement statement = null;
		String SQL = null;
		ResultSet results = null;
		
		try{
			
			SQL = "INSERT INTO `person` VALUES()"; //$NON-NLS-1$
			
			statement = writeConnection.prepareStatement(SQL, 
					Statement.RETURN_GENERATED_KEYS);
			
			if(statement.executeUpdate() == 0)
				showError(new SQLException(
					xStrings.getString("PersonChanger.errorInsertingNewPerson")), //$NON-NLS-1$
					xStrings.getString("PersonChanger.errorInsertingNewPerson")); //$NON-NLS-1$
			else{
				
				results = statement.getGeneratedKeys();
				
				while(results.next())
					insertID = results.getLong(1);
				
			}
			
		}catch(SQLException e){
			
			showError(e, 
					xStrings.getString("PersonChanger.errorInsertingNewPerson")); //$NON-NLS-1$ 
			
		}finally{
			
			if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
			
			if(results != null)
				try{
					results.close();
				}catch(Exception e){}
			
		}
		
		return insertID;
		
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

	
