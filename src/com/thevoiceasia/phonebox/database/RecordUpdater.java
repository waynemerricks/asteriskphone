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
	
	public RecordUpdater(String language, String country, Connection readConnection, 
			Connection writeConnection, String fieldMapping, String channelID, String value) {
		
		xStrings = new I18NStrings(language, country);
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
		
		this.fieldMapping = fieldMapping;
		this.channelID = channelID;
		this.value = value;
		this.readConnection = readConnection;
		this.writeConnection = writeConnection;
		
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
				
				statement.executeUpdate(SQL);
				
			}else if(fieldMapping.equals("conversation")){ //$NON-NLS-1$
				
				//Special case for conversation
				/*
				 * If already exists update else insert
				 */
				if(conversationExists(channelID))
					SQL = "UPDATE conversations SET conversation = ? "  //$NON-NLS-1$
							+ "WHERE channel = ?"; //$NON-NLS-1$
				else
					SQL = "INSERT INTO conversations (conversation, channel) VALUES " //$NON-NLS-1$
							+ "(?, ?)"; //$NON-NLS-1$
				
				statement = writeConnection.prepareStatement(SQL);
				statement.setString(1, value);
				statement.setString(2, channelID);
				
				statement.executeUpdate(SQL);
				
			}else{
				
				//TODO Update field on person table still not sure how we'll do custom
				SQL = "UPDATE person SET ? = ? WHERE person_id = ?"; //$NON-NLS-1$
				
				statement = writeConnection.prepareStatement(SQL);
				statement.setString(1, fieldMapping);
				statement.setString(2, value);
				statement.setString(3, getPersonID(channelID));
				
				statement.executeUpdate(SQL);
				
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

	private boolean conversationExists(String channel) {
		// TODO Auto-generated method stub
		
		
		return false;
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
				if(results.getString("type").equals("combo") &&//$NON-NLS-1$//$NON-NLS-2$
						results.getString("options").contains("=>"))//$NON-NLS-1$//$NON-NLS-2$
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
		String SQL = "SELECT activePerson FROM callhistory WHERE callchannel = " + channel //$NON-NLS-1$
				+ " AND activePerson IS NOT NULL ORDER BY callhistory_id DESC LIMIT 1"; //$NON-NLS-1$
		
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

	private void showError(SQLException e, String friendlyError) {
		
		e.printStackTrace();
		LOGGER.severe(friendlyError);
		
	}
		
}

	
