package com.thevoiceasia.phonebox.records;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import com.thevoiceasia.phonebox.database.DatabaseManager;

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
		
		LOGGER.info(xStrings.getString("TrackDial.trackDialEvent") +  //$NON-NLS-1$
				phoneNumber + "/"  + operator); //$NON-NLS-1$
		String SQL = "INSERT INTO `callhistory` (`phonenumber`, `state`, " + //$NON-NLS-1$
				"`operator`, `callchannel`) VALUES ('" + phoneNumber + "', 'D'," + //$NON-NLS-1$ //$NON-NLS-2$
						" '" + operator + "', 0)";  //$NON-NLS-1$//$NON-NLS-2$
		Statement statement = null;
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
			
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("TrackDial.errorTrackingDialEvent") //$NON-NLS-1$ 
        			+ phoneNumber + ":" + operator); //$NON-NLS-1$
        	
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
		
		System.err.println(xStrings.getString("TrackDial.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		LOGGER.severe(friendlyErrorMessage);
		
	}

}
