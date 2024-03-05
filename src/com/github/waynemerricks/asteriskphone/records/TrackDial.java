package com.github.waynemerricks.asteriskphone.records;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.github.waynemerricks.asteriskphone.database.DatabaseManager;

public class TrackDial implements Runnable {

	private DatabaseManager database = null;
	private String operator = null, phoneNumber = null;
	private I18NStrings xStrings = null;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(TrackDial.class.getName());//Logger
	
	public TrackDial(String language, String country, 
			DatabaseManager databaseManager, String operator, 
			String phoneNumber){
	
		xStrings = new I18NStrings(language, country);
		database = databaseManager;
		this.operator = operator;
		this.phoneNumber = phoneNumber;
		
	}
	
	@Override
	public void run() {
		
		LOGGER.info(xStrings.getString("TrackDial.trackDialEvent") +  
				phoneNumber + "/"  + operator); 
		String SQL = "INSERT INTO `callhistory` (`phonenumber`, `state`, " + 
				"`operator`, `callchannel`) VALUES ('" + phoneNumber + "', 'D'," +  
						" '" + operator + "', 0)";  
		Statement statement = null;
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
			
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("TrackDial.errorTrackingDialEvent")  
        			+ phoneNumber + ":" + operator); 
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
            
        }

	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("TrackDial.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		LOGGER.severe(friendlyErrorMessage);
		
	}

}
