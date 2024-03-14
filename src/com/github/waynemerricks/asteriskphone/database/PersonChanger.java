package com.github.waynemerricks.asteriskphone.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.jid.impl.JidCreate;

public class PersonChanger implements Runnable {

	private String channelID, operator, phoneNumber;
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
			String phoneNumber, String channelID) {
		
		xStrings = new I18NStrings(language, country);
		
		this.readConnection = readConnection;
		this.writeConnection = writeConnection;
		this.personID = personID;
		this.channelID = channelID;
		this.controlRoom = controlRoom;
		
		//BUG FIX: Phone Number can't be null
		if(phoneNumber == null || phoneNumber.trim().length() < 1)
			phoneNumber = xStrings.getString("PersonChanger.unknown");
		
		this.phoneNumber = phoneNumber;
		
		LOGGER.info(xStrings.getString("PersonChanger.ChangingPerson") +  
				"\n\tChannel: " + channelID + "\n\tNew Person ID: " + personID);  
		
	}
	
	/**
	 * Internal method to send a message to a given user
	 * @param recipient
	 * @param message
	 */
	private void sendPrivateMessage(String recipient, String message){
	
		//Smack 4 rework from MUC -> ChatManager -> Chat2
		ChatManager cm = ChatManager.getInstanceFor(controlRoom.getXmppConnection());
		
		try {
			
			LOGGER.info(xStrings.getString("PersonChanger.sendingPrivateMessage") +  
					recipient + "/" + message); 
			Chat chat = cm.chatWith(JidCreate.entityBareFrom(recipient + "@" + controlRoom.getRoom()));
			chat.send(message);
			
		} catch (Exception e) {
			
			LOGGER.severe(xStrings.getString(
					"PersonChanger.XMPPSendErrorChangeFailed")); 
			e.printStackTrace();
			
		}
		
	}

	@Override
	public void run() {
		
		//Create new person if we're -1
		if(personID == -1){//Create New
			
			personID = createNewPerson();
			createPhoneNumberRecord();
			
		}else
			phoneNumber = getPhoneNumber(personID);
		
		boolean success = false;
		
		//If we have a valid id, update the conversation
		if(personID != -1)
			success = updateConversation();
		
		//If ok so far update the call log
		if(success)
			success = updateCallLog();
		
		//If something went wrong send XMPP message failure notice
		if(!success){
			
			/* Show Error but also show failed to clients via control XMPP
			 * because we'll have the initiator client waiting for the result
			 * CHANGEFAILED/CHANNEL
			 * 
			 * Change failed will only need to notify the person who requested
			 * the change so lets do that via a private message.
			 * 
			 * It will save other clients from unnecessary processing
			 */
			sendPrivateMessage(operator, xStrings.getString(
						"PersonChanger.changeFailed") + "/" + channelID);  
			
		}else{
			
			//Send XMPP Changed notice CHANGED/CHANNEL/PERSONID
			try {
				
				controlRoom.sendMessage(xStrings.getString(
						"PersonChanger.changed") + "/" + channelID + "/" +    
						personID);
			
			} catch (Exception e) {
				
				LOGGER.severe(xStrings.getString(
						"PersonChanger.XMPPSendErrorChanged")); 
				e.printStackTrace();
				
			}

		}
		
	}

	/**
	 * Gets the latest operator/phonenumber associated with the channel of 
	 * this object
	 * @return true if success (sets global phoneNumber and operator vars)
	 */
	private boolean getCallLogInfo(){
	
		boolean success = false;
		
		Statement statement = null;
		String SQL = null;
		ResultSet results = null;
		
		try{
			
			/* Get number and operator from the DB */
			SQL = "SELECT `phonenumber`, `operator` FROM `callhistory` " + 
					"WHERE (`state` = 'A' OR `state` = 'M') AND `callchannel` = \"" + channelID + 
					"\" AND `operator` != 'NA' ORDER BY `time` DESC LIMIT 1";  
			
			statement = readConnection.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next()){
			
				//Use the phone number from this result unless it is unknown
				String temp = results.getString("phonenumber");
				
				if(!temp.equals("PersonChanger.unknown") && phoneNumber.equals("PersonChanger.unknown"))
					phoneNumber = temp;
					
				operator = results.getString("operator"); 
				success = true;
				
			}
			
		}catch(SQLException e){
		
			showError(e, xStrings.getString("PersonChanger.errorGettingNumOp") + 
					channelID);
			
			success = false;
			
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
		
		return success;
		
	}
	
	/**
	 * Gets the phonenumber associated with the given person ID
	 * @param personID ID of the person to lookup in the phonenumbers table
	 * @return Phone Number of this record (can be unknown)
	 */
	private String getPhoneNumber(long personID){
	
		String phoneNumber = xStrings.getString("PersonChanger.unknown");
		
		Statement statement = null;
		String SQL = null;
		ResultSet results = null;
		
		try{
			
			/* Get number and operator from the DB */
			SQL = "SELECT `phone_number` FROM `phonenumbers` WHERE `person_id` = " + personID;
			
			statement = readConnection.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next())
				phoneNumber = results.getString("phone_number");
				
		}catch(SQLException e){
		
			showError(e, xStrings.getString("PersonChanger.errorGettingNumber") + 
					channelID);
			
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
		
		return phoneNumber;
		
	}
	
	/**
	 * Updates the callhistory table for this objects channel
	 * Will add a C state record to signify person was manually changed
	 * on this call.
	 * @return success 
	 */
	private boolean updateCallLog() {
		
		/* INSERT INTO callhistory (phonenumber, state, operator, callchannel, 
		 *      activePerson) VALUES ('01234567890', 'C', 'operator', 
		 *      1234567890.123, 555) 
		 */
		boolean success = false;
		String SQL = null;
		PreparedStatement statement = null;
		
		if(getCallLogInfo() && phoneNumber != null && operator != null){
			
			String error = xStrings.getString(
					"PersonChanger.errorUpdatingCallLog") + 
					"\n\tChannel: " + channelID + 
					"\n\tPersonID: " + personID; 

			try{
				
				SQL = "INSERT INTO `callhistory` (`phonenumber`, `state`, " + 
						"`operator`, `callchannel`, `activePerson`) VALUES " + 
						"(?, ?, ?, ?, ?)"; 
				
				statement = writeConnection.prepareStatement(SQL);
				statement.setString(1, phoneNumber);
				statement.setString(2, "C"); 
				statement.setString(3, operator);
				statement.setString(4, channelID);
				statement.setLong(5, personID);
				
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
			
		}
		
		return success;
		
	}

	/**
	 * Updates the conversation record thats associated with the channel id of this
	 * object.  Sets the person id of that record to the person id of this object.
	 * @return true if successful
	 */
	private boolean updateConversation() {
		
		//SQL = "UPDATE conversations SET person_id = " + personID + 
		//	WHERE channel = channelID
		boolean success = false;
		String error = xStrings.getString(
				"PersonChanger.errorUpdatingConversation") + 
				"\n\tChannel: " + channelID + 
				"\n\tPersonID: " + personID; 

		PreparedStatement statement = null;
		String SQL = null;
		
		try{
			
			SQL = "UPDATE `conversations` SET `person_id` = ? WHERE `channel` = ?"; 
			
			statement = writeConnection.prepareStatement(SQL);
			statement.setLong(1, personID);
			statement.setString(2, channelID);
			
			if(statement.executeUpdate() == 0){
				
				//If no conversation has been typed, there won't be anything to
				//update, so we should expect 0 rows changed too
				LOGGER.info(xStrings.getString(
						"PersonChanger.noConversationToUpdate") +  
						"\n\tChannel: " + channelID); 
				
			}
			
			success = true;
			
		}catch(SQLException e){
			
			showError(e, error);
			success = false;
			
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
			
			SQL = "INSERT INTO `person` VALUES()"; 
			
			statement = writeConnection.prepareStatement(SQL, 
					Statement.RETURN_GENERATED_KEYS);
			
			if(statement.executeUpdate() == 0)
				showError(new SQLException(
					xStrings.getString("PersonChanger.errorInsertingNewPerson")), 
					xStrings.getString("PersonChanger.errorInsertingNewPerson")); 
			else{
				
				results = statement.getGeneratedKeys();
				
				while(results.next())
					insertID = results.getLong(1);
				
			}
			
		}catch(SQLException e){
			
			showError(e, 
					xStrings.getString("PersonChanger.errorInsertingNewPerson"));  
			
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
	 * Creates a phone number record for the person associated with this object
	 */
	private void createPhoneNumberRecord() {
		
		PreparedStatement statement = null;
		String SQL = null;
		ResultSet results = null;
		
		try{
			
			SQL = "INSERT INTO `phonenumbers` (`phone_number`, `person_id`) VALUES(?, ?)";
			
			statement = writeConnection.prepareStatement(SQL);
			statement.setString(1, phoneNumber);
			statement.setLong(2, personID);
			
			if(statement.executeUpdate() == 0)
				showError(new SQLException(
					xStrings.getString("PersonChanger.errorInsertingNumber")),
					xStrings.getString("PersonChanger.errorInsertingNumber"));
			
		}catch(SQLException e){
			
			showError(e, 
					xStrings.getString("PersonChanger.errorInsertingNumber"));
			
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

	
