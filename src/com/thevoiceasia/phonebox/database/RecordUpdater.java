package com.thevoiceasia.phonebox.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class RecordUpdater implements Runnable {

	private String fieldMapping, channelID, value;
	private Connection readConnection, writeConnection;
	private I18NStrings xStrings;
	private static final Logger LOGGER = Logger.getLogger(RecordUpdater.class.getName());//Logger
	
	/**
	 * Creates a runnable object to update fields related to a call/person
	 * @param language I18N Language
	 * @param country I18N Country
	 * @param readConnection Database Read Connection
	 * @param writeConnection Database Write Connection
	 * @param fieldMapping Field mapping we will be changing
	 * @param channelID ID of the channel we're changing
	 * @param value Value we're changing the field to
	 */
	public RecordUpdater(String language, String country, Connection readConnection, 
			Connection writeConnection, String fieldMapping, String channelID, String value) {
		
		xStrings = new I18NStrings(language, country);
		
		this.fieldMapping = fieldMapping;
		this.channelID = channelID;
		this.readConnection = readConnection;
		this.writeConnection = writeConnection;
		
		//Parse out the ^^%%$$ to /
		value = value.replaceAll("^^%%$$", "/");  //$NON-NLS-1$//$NON-NLS-2$
		
		
		if(isImageMappedCombo(fieldMapping)){
			
			/* Think about alert/badge icons ->
			 * lookup options for mapping of the same name, will have to parse out icons
			 * 
			 * + = /
			 * @@ = separator for field name mapped to icon path e.g. 
			 *   Favourite@@images+favourite.png
			 * ==> Favourite => images/favourite.png 
			 * 
			 * All we need is the Favourite part of that to save to the DB
			 */
			if(value.contains("@@")) //$NON-NLS-1$
				value = value.split("@@")[0]; //$NON-NLS-1$
				
		}
		
		this.value = value;
		
		LOGGER.info(xStrings.getString("RecordUpdater.updatingRecord") +  //$NON-NLS-1$
				"\n\tChannel: " + channelID + "\n\tField: " + fieldMapping +  //$NON-NLS-1$ //$NON-NLS-2$
				"\n\tValue: " + value); //$NON-NLS-1$
		
	}

	@Override
	public void run() {
		
		PreparedStatement statement = null;
		String SQL = null;
		
		try{
		
			if(fieldMapping.equals("calltype")){ //$NON-NLS-1$
				
				//Update callhistory.type
				SQL = "UPDATE callhistory SET type = ? WHERE callchannel = ? ORDER BY " //$NON-NLS-1$
						+ "callhistory_id DESC LIMIT 1"; //$NON-NLS-1$
				
				statement = writeConnection.prepareStatement(SQL);
				statement.setString(1, value);
				statement.setString(2, channelID);
				
				statement.executeUpdate();
				
			}else if(fieldMapping.equals("conversation")){ //$NON-NLS-1$
				
				//Special case for conversation
				/*
				 * If already exists update else insert
				 */
				if(conversationExists(channelID)){
					SQL = "UPDATE conversations SET conversation = ?, person_id = ? "  //$NON-NLS-1$
							+ "WHERE channel = ?"; //$NON-NLS-1$
					
					statement = writeConnection.prepareStatement(SQL);
					statement.setString(1, value);
					statement.setString(2, getPersonID(channelID));
					statement.setString(3, channelID);
					
				}else{
					SQL = "INSERT INTO conversations (conversation, channel, person_id) VALUES " //$NON-NLS-1$
							+ "(?, ?, ?)"; //$NON-NLS-1$
					
					statement = writeConnection.prepareStatement(SQL);
					statement.setString(1, value);
					statement.setString(2, channelID);
					statement.setString(3, getPersonID(channelID));
					
				}
				
				statement.executeUpdate();
				
			}else{
				
				//TODO Update field on person table still not sure how we'll do custom
				if(fieldMapping.equals("gender") || fieldMapping.equals("alert")) //$NON-NLS-1$ //$NON-NLS-2$
					value = value.substring(0, 1);
				
				SQL = "UPDATE person SET " + fieldMapping + " = ? WHERE person_id = ?"; //$NON-NLS-1$ //$NON-NLS-2$
				
				statement = writeConnection.prepareStatement(SQL);
				statement.setString(1, value);
				statement.setString(2, getPersonID(channelID));
				
				statement.executeUpdate();
				
			}
		
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("RecordUpdater.errorUpdatingRecord") //$NON-NLS-1$ 
        			+ fieldMapping + "/" + channelID + "/" + value); //$NON-NLS-1$ //$NON-NLS-2$
        	
        }finally{
        	
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
            
        }

	}

	/**
	 * Checks to see if this conversation is already in the DB by looking up the channel
	 * in the conversation table
	 * @param channel channel to lookup
	 * @return true if it exists
	 */
	private boolean conversationExists(String channel) {
		
		boolean exists = false;
		
		/* Look in the conversations table to see if this channel exists
		 * If it does change conversation to this.
		 * Possible issue if operator A type blah and B adds more, B will overwrite?
		 */
		String SQL = "SELECT conversations_id FROM conversations WHERE channel = " + channel; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet results = null;
		
		try{
			
			statement = readConnection.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next())
				exists = true;
				
		}catch (SQLException e){
			showError(e, xStrings.getString("RecordUpdater.errorCheckingForConversation")); //$NON-NLS-1$
		}finally {
			
			if (results != null) {
		        try {
		        	results.close();
		        } catch (SQLException sqlEx) { } // ignore
		        results = null;
		    }
		    
			if (statement != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        statement = null;
		    }
			
		}
		
		return exists;
	}

	/**
	 * Looks in the DB to find out if this fieldMapping belongs to a combo box with images
	 * @param fieldMapping mapping to lookup
	 * @return true if this combo box has mapped field values to images
	 */
	private boolean isImageMappedCombo(String fieldMapping) {
		
		boolean mappedCombo = false;
		
		/*
		 * Lookup the type in the DB if its combo get the options and search for =>
		 * 
		 * if both true then return true
		 */
		String SQL = "SELECT type, options FROM callinputfields WHERE mapping = '"  //$NON-NLS-1$
				+ fieldMapping + "'"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet results = null;
		
		try{
			
			statement = readConnection.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next())
				if(results.getString("type").equals("combo") &&  //$NON-NLS-1$ //$NON-NLS-2$
						results.getString("options").contains("=>")) //$NON-NLS-1$ //$NON-NLS-2$
					mappedCombo = true;
				
		}catch (SQLException e){
			showError(e, xStrings.getString("RecordUpdater.errorGettingActivePerson")); //$NON-NLS-1$
		}finally {
			
			if (results != null) {
		        try {
		        	results.close();
		        } catch (SQLException sqlEx) { } // ignore
		        results = null;
		    }
		    
			if (statement != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        statement = null;
		    }
			
		}
		
		return mappedCombo;
		
	}
	
	/**
	 * Gets the activePerson value on the given channel
	 * @param channel channel to lookup in the callhistory table
	 * @return id if found or null if none
	 */
	private String getPersonID(String channel) {
		
		String id = null;
		
		/*
		 * Lookup callhistory for this channel, find activePerson and return
		 */
		String SQL = "SELECT activePerson FROM callhistory WHERE callchannel = \"" + channel //$NON-NLS-1$
				+ "\" AND activePerson IS NOT NULL ORDER BY callhistory_id DESC LIMIT 1"; //$NON-NLS-1$
		
		LOGGER.info(SQL);
		
		Statement statement = null;
		ResultSet results = null;
		
		try{
			
			statement = readConnection.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next())
				id = results.getString("activePerson"); //$NON-NLS-1$
				
		}catch (SQLException e){
			showError(e, xStrings.getString("RecordUpdater.errorGettingActivePerson")); //$NON-NLS-1$
		}finally {
			
			if (results != null) {
		        try {
		        	results.close();
		        } catch (SQLException sqlEx) { } // ignore
		        results = null;
		    }
		    
			if (statement != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        statement = null;
		    }
			
		}
		
		return id;
		
	}

	/**
	 * Logs a severe error via LOGGER object
	 * @param e Exception
	 * @param friendlyError Error as it should appear in LOGGER
	 */
	private void showError(SQLException e, String friendlyError) {
		
		e.printStackTrace();
		LOGGER.severe(friendlyError);
		
	}
		
}

	
