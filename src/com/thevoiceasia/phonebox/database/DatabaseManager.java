package com.thevoiceasia.phonebox.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.JOptionPane;

/**
 * Provides project wide access to MySQL backend
 * @author Wayne Merricks
 *
 */
public class DatabaseManager {
	
	// CLASS VARS
	private boolean hasErrors = false, connected = false;
	private Connection databaseConnection;
	private HashMap<String, String> settings = new HashMap<String, String>();
	private HashMap<String, String> database = new HashMap<String, String>();
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static I18NStrings xStrings;
	
	public DatabaseManager(File settingsFile, String language, String country){
		
		xStrings = new I18NStrings(language, country);
		setupLogging();
		readSettingsFromFile(settingsFile);
		checkDBSettings();
		
	}

	public HashMap<String, String> getUserSettings(){
		
		return settings;
		
	}
	
	/**
	 * Sanity check to make sure we have all the required DB settings
	 */
	private void checkDBSettings(){
		
		if(!database.containsKey("host") || //$NON-NLS-1$
				!database.containsKey("user") || //$NON-NLS-1$
				!database.containsKey("password") || //$NON-NLS-1$
				!database.containsKey("database")) //$NON-NLS-1$
			hasErrors = true;
		
		if(hasErrors)
			LOGGER.severe(xStrings.getString("DatabaseManager.DBSetupError")); //$NON-NLS-1$
		else
			LOGGER.info(xStrings.getString("DatabaseManager.DBSetupSuccess")); //$NON-NLS-1$
		
	}
	
	/**
	 * Gets settings from DB, will overwrite any keys already in existence.
	 * 
	 * Intended to be called as GLOBAL, machineName, userName 
	 * 
	 * GLOBAL is superceded by Machine which is superceded by userName in turn
	 * @param owner
	 * @return
	 */
	private void getSettingsFromDB(String owner){
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			String SQL = "SELECT option_name, option_value FROM clientsettings WHERE option_owner='"  //$NON-NLS-1$
					+ owner + "'"; //$NON-NLS-1$
			statement = databaseConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	settings.put(resultSet.getString("option_name"),  //$NON-NLS-1$
		    			resultSet.getString("option_value")); //$NON-NLS-1$
		    	
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("DatabaseManager.getSettingsSQLError")); //$NON-NLS-1$
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
	 * Grabs settings from the database and puts them in a nice HashMap
	 */
	public void populateUserSettings(){
		
		try {
			String machineName = InetAddress.getLocalHost().getHostName();
			String userName = System.getProperty("user.name"); //$NON-NLS-1$
			
			getSettingsFromDB("GLOBAL"); //$NON-NLS-1$
			getSettingsFromDB(machineName);
			getSettingsFromDB(userName);
			
		} catch (UnknownHostException e) {

			showError(e, xStrings.getString("DatabaseManager.errorGettingHostName")); //$NON-NLS-1$
			hasErrors = true;
			
		}
		
	}
	
	/**
	 * Returns hasError flag, signifies any errors that should abort the program
	 * @return
	 */
	public boolean hasErrors(){
		return hasErrors;
	}
	
	/**
	 * Closes the MySQL Connection gracefully
	 * 
	 * Does nothing if we're not connected
	 */
	public void disconnect(){
		
		if(connected){
			
			LOGGER.info("DatabaseManager.disconnecting"); //$NON-NLS-1$
			try {
				
				databaseConnection.close();
				LOGGER.info("DatabaseManager.disconnected"); //$NON-NLS-1$
				connected = false;
				
			} catch (SQLException e) {
	
				showWarning(e, xStrings.getString("DatabaseManager.errorDisconnecting")); //$NON-NLS-1$
				
			}
		
		}
		
	}
	
	/**
	 * Returns the MySQL Connection, does not check if the connection is live.
	 * 
	 * Make sure connect is called first and completed without error.
	 * 
	 * @return
	 */
	public Connection getConnection(){
		
		return databaseConnection;
		
	}
	
	
	/**
	 * Connects to the DB, will alert if there are errors.
	 * 
	 * Does nothing if we're already connected.
	 */
	public void connect(){
		
		if(!connected){
			
			LOGGER.info(xStrings.getString("DatabaseManager.connecting")); //$NON-NLS-1$
			
			if(!hasErrors){
				
				try{
					
					Class.forName("com.mysql.jdbc.Driver").newInstance(); //$NON-NLS-1$
					databaseConnection = DriverManager.getConnection("jdbc:mysql://" +  //$NON-NLS-1$
							database.get("host") + "/" + database.get("database") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"?user=" + database.get("user") + "&password=" + database.get("password"));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					
					LOGGER.info(xStrings.getString("DatabaseManager.connected")); //$NON-NLS-1$
					connected = true;
					
				}catch(SQLException e){
					
					showError(e, xStrings.getString("DatabaseManager.mysqlConnectionError")); //$NON-NLS-1$
					hasErrors = true;
					
				}catch(Exception e){
					
					showError(e, xStrings.getString("DatabaseManager.mysqlDriverException")); //$NON-NLS-1$
					hasErrors = true;
					
				}
				
			}else
				showError(new Exception(xStrings.getString("DatabaseManager.DBSetupError")), xStrings.getString("DatabaseManager.DBSetupError"));  //$NON-NLS-1$//$NON-NLS-2$
		
		}
	}
	
	/**
	 * Reads database settings from the given file
	 * Expects a standard ini style file e.g. key=value
	 * Any other lines are ignored.
	 * 
	 * Required for operation are the following keys:
	 * host - database host name/ip
	 * user - database user to use (make a user only for the database needs read/write access, don't use root)
	 * password - password for the user above
	 * database - database name to use
	 * 
	 * @param settingsFile
	 */
	private void readSettingsFromFile(File settingsFile) {
		
		try {
			FileReader file = new FileReader(settingsFile);
			BufferedReader in = new BufferedReader(file);
			
			String line = null;
			
			while((line = in.readLine()) != null){
				
				if(line.contains("=")){ //$NON-NLS-1$
					
					String[] keyPair = line.split("="); //$NON-NLS-1$
					
					if(keyPair.length == 2){
						
						database.put(keyPair[0], keyPair[1]);
						
					}
					
				}
				
			}
			
			in.close();
			file.close();
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
			showError(e, xStrings.getString("DatabaseManager.settingsFileNotFound") + settingsFile.getAbsolutePath()); //$NON-NLS-1$
			hasErrors = true;
			
		} catch (IOException e) {
			
			e.printStackTrace();
			showError(e, xStrings.getString("DatabaseManager.settingsFileIOException") + settingsFile.getAbsolutePath()); //$NON-NLS-1$
			hasErrors = true;
			
		}
		
	}
	
	/**
	 * Set the Logger object
	 * 
	 */
	private void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		
		try{
			LOGGER.addHandler(new FileHandler("databaseLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("DatabaseManager.logCreateError") + "databaseLog.log"); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.errorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.errorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}

}
