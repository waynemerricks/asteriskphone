package com.thevoiceasia.phonebox.database;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.SecureRandom;
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
	private Connection databaseWriteConnection;
	
	private HashMap<String, String> settings = new HashMap<String, String>();
	private HashMap<String, String> database = new HashMap<String, String>();
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.WARNING;
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
			LOGGER.info(xStrings.getString("DatabaseManager.logDBSetupSuccess")); //$NON-NLS-1$
		
	}
	
	/**
	 * Gets settings from DB, will overwrite any keys already in existence.
	 * 
	 * Intended to be called as GLOBAL, machineName, userName 
	 * 
	 * GLOBAL is superceded by Machine which is superceded by userName in turn
	 * @param owner
	 * @return true if we found settings, false if not
	 */
	private boolean getSettingsFromDB(String owner){
		
		Statement statement = null;
		ResultSet resultSet = null;
		boolean gotSettings = false;
		
		try{
			
			String SQL = "SELECT option_name, option_value FROM clientsettings WHERE option_owner='"  //$NON-NLS-1$
					+ owner + "'"; //$NON-NLS-1$
			statement = databaseConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	settings.put(resultSet.getString("option_name"),  //$NON-NLS-1$
		    			resultSet.getString("option_value")); //$NON-NLS-1$
		    	gotSettings = true;
		    	
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
		
		return gotSettings;
		
	}
	
	/**
	 * Creates a new user with the given user name.
	 * Auto generates a random password and adds it to the config
	 * DB.
	 * @param userName
	 * @return false if could not create user
	 */
	private boolean createUser(String userName){
		
		boolean success = false;
		settings.put("XMPPLogin", userName); //$NON-NLS-1$
		settings.put("nickName", userName); //$NON-NLS-1$
		
		String password = generatePassword();
		
		if(password != null && password != ""){ //$NON-NLS-1$
		
			settings.put("password", password); //$NON-NLS-1$
			
			success = addNewXMPPUser(userName, password);
			
		}
		
		return success;
		
	}
	
	/**
	 * Executes an SQL Update or Insert Statement
	 * @param updateStatement SQL to perform
	 * @return success or fail (true/false)
	 */
	private boolean executeUpdate(String updateStatement){
		
		boolean updated = false;
		
		String[] tokens = updateStatement.split(" "); //$NON-NLS-1$
		
		if(tokens[0].equals("UPDATE") || tokens[0].equals("INSERT")){  //$NON-NLS-1$//$NON-NLS-2$
			
			Statement query = null;
			
			try{
				
				if(hasWriteConnection())
					query = databaseWriteConnection.createStatement();
				else
					query = databaseConnection.createStatement();
				
                query.executeUpdate(updateStatement);
                updated = true;
            }catch(SQLException e){
            	
            	showError(e, xStrings.getString("DatabaseManager.errorUpdatingDB")); //$NON-NLS-1$
            	
            }finally{
	            if(query != null)
	            	try{
	            		query.close();
	            	}catch(Exception e){}
	        }
			
		}
		
		return updated;
		
	}
	
	/**
	 * Adds a new XMPP user to the database
	 * @param userName
	 * @param password
	 * @return true if successful
	 */
	private boolean addNewXMPPUser(String userName, String password) {
		
		LOGGER.info(xStrings.getString("DatabaseManager.logAddNewXMPPUser")); //$NON-NLS-1$
		
		return executeUpdate("INSERT INTO clientsettings (option_owner, option_name, option_value)" + //$NON-NLS-1$
				" VALUES ('" + userName +"', 'XMPPLogin', '" + userName + "'), " + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"('" + userName + "', 'password', '" + password + "'), " +  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
						"('" + userName + "', 'nickName', '" + userName + "')");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		
	}

	/**
	 * Generates a random password 20 bytes long using Java's SecureRandom class
	 * Then encodes to a string using UTF-8
	 * @return
	 */
	private String generatePassword(){
		
		/*
		 * Generate random password.  Only for internal use with XMPP server.
		 * 
		 * Potential risk as password is stored in plain text however it is only for
		 * this service as a way of automating new machine deployments so its not a concern for me.
		 * 
		 * If this is a problem, need to re-implement with encryption (not hashing) so that
		 * the password can be decrypted before being sent to the XMPP server.
		 */
		SecureRandom random = new SecureRandom();
		LOGGER.info(xStrings.getString("DatabaseManager.logGeneratingPassword")); //$NON-NLS-1$
		
		return new BigInteger(130, random).toString(32);
		
	}
	
	/**
	 * Grabs settings from the database and puts them in a nice HashMap
	 * @return false if user needs to be created on chat server
	 */
	public boolean populateUserSettings(){
		
		boolean gotUser = false;
		LOGGER.info(xStrings.getString("DatabaseManager.logPopulatingUserSettings")); //$NON-NLS-1$
		
		try {
			String machineName = InetAddress.getLocalHost().getHostName();
			String userName = System.getProperty("user.name"); //$NON-NLS-1$
			
			getSettingsFromDB("GLOBAL"); //$NON-NLS-1$
			getSettingsFromDB(machineName);
				
			if(!getSettingsFromDB(userName)){
				if(!createUser(userName))
					showError(new Exception(xStrings.getString("DatabaseManager.errorCreatingUser")), //$NON-NLS-1$
							xStrings.getString("DatabaseManager.errorCreatingUser")); //$NON-NLS-1$
			}else
				gotUser = true;
			
		} catch (UnknownHostException e) {

			showError(e, xStrings.getString("DatabaseManager.errorGettingHostName")); //$NON-NLS-1$
			hasErrors = true;
			
		}
		
		return gotUser;
		
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
			
			LOGGER.info("DatabaseManager.logDisconnecting"); //$NON-NLS-1$
			try {
				
				databaseConnection.close();
				LOGGER.info("DatabaseManager.logDisconnected"); //$NON-NLS-1$
				
				if(hasWriteConnection()){
					
					databaseWriteConnection.close();
					LOGGER.info("DatabaseManager.logWriteDisconnected"); //$NON-NLS-1$
					
				}
					
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
	 * Returns the MySQL Read Only Connection, does not check if the connection is live.
	 * 
	 * Make sure connect is called first and completed without error.
	 * 
	 * @return
	 */
	public Connection getReadConnection(){
		
		return databaseConnection;
		
	}
	
	/**
	 * Returns the MySQL Write Connection, does not check if the connection is live.
	 * 
	 * Make sure connect is called first and completed without error.
	 * 
	 * @return
	 */
	public Connection getWriteConnection(){
		
		return databaseWriteConnection;
		
	}
	
	
	/**
	 * Connects to the DB, will alert if there are errors.
	 * 
	 * Does nothing if we're already connected.
	 */
	public void connect(){
		
		if(!connected){
			
			LOGGER.info(xStrings.getString("DatabaseManager.logConnecting")); //$NON-NLS-1$
			
			if(!hasErrors){
				
				try{
					
					Class.forName("com.mysql.jdbc.Driver").newInstance(); //$NON-NLS-1$
					databaseConnection = DriverManager.getConnection("jdbc:mysql://" +  //$NON-NLS-1$
							database.get("host") + "/" + database.get("database") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"?user=" + database.get("user") + "&password=" + database.get("password"));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					
					LOGGER.info(xStrings.getString("DatabaseManager.logConnected")); //$NON-NLS-1$
					
					if(hasWriteConnection()){
						
						//Connect to separate write DB too
						databaseWriteConnection = DriverManager.getConnection("jdbc:mysql://" +  //$NON-NLS-1$
								settings.get("writeDBHost") + "/" + database.get("writeDBDatabase") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"?user=" + database.get("writeDBUser") +  //$NON-NLS-1$ //$NON-NLS-2$
								"&password=" + database.get("writeDBPassword"));  //$NON-NLS-1$//$NON-NLS-2$
						
						LOGGER.info(xStrings.getString("DatabaseManager.logWriteConnected")); //$NON-NLS-1$
						
					}
					
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
	 * Helper method that checks if we have a separate DB for inserting/updating
	 * which in theory means we have a read only connection to the local DB
	 * @return true if we need to separate out connections
	 */
	private boolean hasWriteConnection(){
		
		boolean writeConnection = false;
		
		if(settings.get("writeDBHost") != null && //$NON-NLS-1$
				settings.get("writeDBUser") != null && //$NON-NLS-1$
				settings.get("writeDBPass") != null && //$NON-NLS-1$
				settings.get("writeDBDatabase") != null)  //$NON-NLS-1$
			writeConnection = true;
		
		return writeConnection;
		
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
			showWarning(e, xStrings.getString("DatabaseManager.loggerCreateError") + "databaseLog.log"); //$NON-NLS-1$ //$NON-NLS-2$
			
		}
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
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
		
		System.err.println(xStrings.getString("DatabaseManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}

}
