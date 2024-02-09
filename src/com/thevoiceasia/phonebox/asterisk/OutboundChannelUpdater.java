package com.thevoiceasia.phonebox.asterisk;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class OutboundChannelUpdater implements Runnable{

	private static final Logger LOGGER = Logger.getLogger(OutboundChannelUpdater.class.getName());//Logger
	
	private Connection read = null, write = null;
	private I18NStrings xStrings = null;
	private String oldChannel = null, newChannel = null, phoneNumber = null,
			type = null;
	private int activePerson = -1;
	private AsteriskManager server = null;
	
	/**
	 * Creates a new object to change conversation and call info from an old
	 * channel to this one (usually outbound call as when they enter a QUEUE
	 * for the first time, a new channel is created for reasons unknown)
	 * @param language I18N e.g. en
	 * @param country I18N e.g. GB
	 * @param readConnection read connection to DB
	 * @param writeConnection write connection to DB
	 * @param oldChannel Channel of entry to get info from
	 * @param newChannel Channel of entry to make with old info
	 * @param number Number/CallerID of call
	 * @param Where to send XMPP Control UPDATEX when finished
	 */
	public OutboundChannelUpdater(String language, String country, 
			Connection readConnection, Connection writeConnection,
			String oldChannel, String newChannel, String number,
			AsteriskManager asterisk){
		
		xStrings = new I18NStrings(language, country);
		this.read = readConnection;
		this.write = writeConnection;
		this.oldChannel = oldChannel;
		this.newChannel = newChannel;
		this.phoneNumber = number;
		this.server = asterisk;
		
	}
	
	public String toString(){
		
		return "OutboundChannelUpdater:\n" + 
				"\told: " + oldChannel +  
				"\tnew: " + newChannel + 
				"\tno: " + phoneNumber; 
		
	}
	
	public void run(){
		
		/* SELECT `type`, `activePerson` FROM `callhistory` WHERE `callchannel` = 'expectedInQueue.get(CallerID)' ORDER BY `callhistory_id` DESC LIMIT 1;
		 * INSERT INTO `callhistory` (`phonenumber`, `callchannel`, `type`, `activePerson`) VALUES (?, ?, ?, ?);
		 * UPDATE `conversations` SET `channel` = entry.getChannel().getId() WHERE `channel` = 'expectedInQueue.get(CallerID)' 
		 */
		if(getCallLogInfo() && updateCallLog() && updateConversation()){
			LOGGER.info(xStrings.getString(
					"OutboundChannelUpdater.updateSuccessful") + oldChannel +  
					"/" + newChannel); 
			server.sendPanelUpdate(newChannel);
		}else
			LOGGER.warning(xStrings.getString(
					"OutboundChannelUpdater.errorUpdating") + oldChannel + "/" +  
					newChannel);
		
	}
	
	/**
	 * Gets the latest type/activePerson associated with the old channel of 
	 * this object
	 * @return true if success (sets global type and activePerson vars)
	 */
	private boolean getCallLogInfo(){
	
		boolean success = false;
		
		Statement statement = null;
		String SQL = null;
		ResultSet results = null;
		
		try{
			
			/* Get number and operator from the DB */
			SQL = "SELECT `type`, `activePerson` FROM `callhistory` " + 
					"WHERE `state` = 'A' AND `callchannel` = " + oldChannel + 
					" ORDER BY `callhistory_id` DESC LIMIT 1";  
			
			statement = read.createStatement();
		    results = statement.executeQuery(SQL);
		    
			while(results.next()){
			
				type = results.getString("type"); 
				activePerson = results.getInt("activePerson"); 
				success = true;
				
			}
			
		}catch(SQLException e){
		
			showError(e, xStrings.getString(
					"OutboundChannelUpdater.errorGettingCallHistory") + 
					oldChannel);
			
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
	 * Updates the callhistory table for this objects channel
	 * @return success 
	 */
	private boolean updateCallLog() {
		
		//INSERT INTO `callhistory` (`phonenumber`, `callchannel`, `type`, `activePerson`) VALUES (?, ?, ?, ?);
		boolean success = false;
		String SQL = null;
		PreparedStatement statement = null;
		
		String error = xStrings.getString(
				"OutboundChannelUpdater.errorUpdatingCallLog") + 
				"\n\tChannel: " + oldChannel + "/" + newChannel;  

		try{
			
			SQL = "INSERT INTO `callhistory` (`phonenumber`, `callchannel`, " + 
					"`type`, `activePerson`) VALUES " + 
					"(?, ?, ?, ?)"; 
			
			statement = write.prepareStatement(SQL);
			statement.setString(1, phoneNumber);
			statement.setString(2, newChannel);
			statement.setString(3, type);
			statement.setInt(4, activePerson);
			
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
	 * Updates the conversation record thats associated with the channel id of this
	 * object.  Sets the person id of that record to the person id of this object.
	 * @return true if successful
	 */
	private boolean updateConversation() {
		
		//UPDATE `conversations` SET `channel` = entry.getChannel().getId() WHERE `channel` = 'expectedInQueue.get(CallerID)' 
		boolean success = false;
		String error = xStrings.getString(
				"OutboundChannelUpdater.errorUpdatingConversation") + 
				"\n\tChannel: " + oldChannel + "/" + newChannel; 

		PreparedStatement statement = null;
		String SQL = null;
		
		try{
			
			SQL = "UPDATE `conversations` SET `channel` = ? WHERE `channel` = ?"; 
			
			statement = write.prepareStatement(SQL);
			statement.setString(1, newChannel);
			statement.setString(2, oldChannel);
			
			if(statement.executeUpdate() == 0){
				
				//If no conversation has been typed, there won't be anything to
				//update, so we should expect 0 rows changed too
				LOGGER.info(xStrings.getString(
						"OutboundChannelUpdater.noConversationToUpdate") +  
						"\n\tChannel: " + oldChannel); 
				
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
	 * Logs a severe SQL Error to the LOGGER object
	 * @param e Exception to stack trace
	 * @param friendlyError Error as it shows up in the LOGGER object
	 */
	private void showError(SQLException e, String friendlyError) {
		
		e.printStackTrace();
		LOGGER.severe(friendlyError);
		
	}

}
