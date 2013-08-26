package com.thevoiceasia.phonebox.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class KeepAliveThread implements Runnable {

	private Connection[] connections = new Connection[2];
	private I18NStrings xStrings;
	private static final Logger LOGGER = Logger.getLogger(KeepAliveThread.class.getName());//Logger
	private boolean go = true;
	private long KEEPALIVE_TIMEOUT = 3600000;
	
	/**
	 * Creates a thread that performs a simple SELECT 1 query on the DB in order to
	 * keep the connection open
	 * @param read read connection to keep alive
	 * @param write write connection to keep alive
	 * @param language language for I18N Strings
	 * @param country country for I18N Strings
	 */
	public KeepAliveThread(Connection read, Connection write, String language, 
			String country){
		
		connections[0] = read;
		connections[1]= write;
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Creates a thread that performs a simple SELECT 1 query on the DB in order to 
	 * keep the connection open
	 * @param readWrite Read and Write connection to keep alive
	 * @param language language for I18N Strings
	 * @param country country for I18N Strings
	 */
	public KeepAliveThread(Connection readWrite, String language, String country) {
		
		connections[0] = readWrite;
		xStrings = new I18NStrings(language, country);
		
	}

	/**
	 * Performs a simple SELECT 1; query on the given connection
	 * @param connection
	 */
	private void testConnection(Connection connection){
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		String SQL = "SELECT 1"; //$NON-NLS-1$
		
		try{
			
			statement = connection.createStatement();
			resultSet = statement.executeQuery(SQL);
			
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("KeepAliveThread.SQLError")); //$NON-NLS-1$
			
		}finally {
		    
			if (resultSet != null) {
		        try {
		        	resultSet.close();
		        } catch (SQLException sqlEx) { } // ignore
		        resultSet = null;
		    }
			
		    if (statement != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        statement = null;
		    }
		    
		}
		
	}
	
	/**
	 * KeepAlive Loop
	 */
	private void keepAlive(){
		
		for(int i = 0; i < connections.length; i++){
			
			if(connections[i] != null)
				testConnection(connections[i]);
				
		}
		
	}
	
	@Override
	public void run() {
		
		while(go){
			
			try{
				
				keepAlive();
				Thread.sleep(KEEPALIVE_TIMEOUT);
				
			}catch(InterruptedException e){
				
				go = false;
				LOGGER.info(xStrings.getString("KeepAliveThread.threadInterrupted")); //$NON-NLS-1$
				
			}
			
		}
		
	}

}
