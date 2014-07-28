package com.thevoiceasia.phonebox.database;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
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
	private boolean hasErrors = false, connected = false, writeConnected = false;
	private Connection databaseConnection;
	private Connection databaseWriteConnection;
	private Thread keepAliveThread;
	
	private HashMap<String, String> settings = new HashMap<String, String>();
	private Settings database;
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());//Logger
	private static I18NStrings xStrings;
	
	public DatabaseManager(Settings settingsFile, String language, String country){
		
		xStrings = new I18NStrings(language, country);
		database = settingsFile;
		checkDBSettings();
		
	}

	/**
	 * Returns the hashmap containing the user settings that were read from the
	 * database
	 * @return
	 */
	public HashMap<String, String> getUserSettings(){
		
		return settings;
		
	}
	
	/**
	 * Spawns a thread (two if write is separate) to send a simple query
	 * to the server every hour to work around MySQL 8 hour idle time out.
	 * 
	 * In our situation this isn't a big issue due to volume of calls however 
	 * might as well do it just in case.
	 */
	public void spawnKeepAlive(String language, String country){
	
		Connection read = getReadConnection();
		Connection write = null;
		KeepAliveThread keepAlive = null;
		
		if(hasWriteConnection()){
			
			write = getWriteConnection();
			keepAlive = new KeepAliveThread(read, write, language, country);
			
		}else
			keepAlive = new KeepAliveThread(read, language, country);
		
		keepAliveThread = new Thread(keepAlive);
		keepAliveThread.start();
		
	}
	
	/**
	 * Sanity check to make sure we have all the required DB settings
	 */
	private void checkDBSettings(){
		
		if(database.getString("host").startsWith("!") || //$NON-NLS-1$ //$NON-NLS-2$
				database.getString("user").startsWith("!") || //$NON-NLS-1$ //$NON-NLS-2$
				database.getString("password").startsWith("!") || //$NON-NLS-1$ //$NON-NLS-2$
				database.getString("database").startsWith("!")) //$NON-NLS-1$ //$NON-NLS-2$
			hasErrors = true;
		
		if(hasErrors)
			LOGGER.severe(xStrings.getString("DatabaseManager.DBSetupError")); //$NON-NLS-1$
		else{
			LOGGER.info(xStrings.getString("DatabaseManager.logDBSetupSuccess")); //$NON-NLS-1$
		}
		
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
		
		if(!writeConnected)
			connectWrite();
		
		if(!hasErrors){
			
			settings.put("XMPPLogin", userName); //$NON-NLS-1$
			settings.put("nickName", userName); //$NON-NLS-1$
			
			String password = generatePassword();
			
			if(password != null && password != ""){ //$NON-NLS-1$
			
				settings.put("password", password); //$NON-NLS-1$
				
				success = addNewXMPPUser(userName, password);
				
			}
			
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
	public boolean populateUserSettings(String userName){
		
		boolean gotUser = false;
		LOGGER.info(xStrings.getString("DatabaseManager.logPopulatingUserSettings")); //$NON-NLS-1$
		
		try {
			String machineName = InetAddress.getLocalHost().getHostName();
			
			if(userName == null)
				userName = System.getProperty("user.name"); //$NON-NLS-1$
			
			getSettingsFromDB("GLOBAL"); //$NON-NLS-1$
			getSettingsFromDB(machineName);
				
			/* Check to see if we don't already have a user account tied to the machine name
			 * If we do, use that, if we don't try and create a new account
			 */
			if(!getSettingsFromDB(userName) && settings.get("XMPPLogin") == null){//If this user has no settings in DB //$NON-NLS-1$
				
				if(!createUser(userName))//try and create a new user, if not error
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
				
				if(hasWriteConnection() && writeConnected){
					
					databaseWriteConnection.close();
					LOGGER.info("DatabaseManager.logWriteDisconnected"); //$NON-NLS-1$
					writeConnected = false;
					
				}
					
				connected = false;
				
			} catch (SQLException e) {
	
				showWarning(e, xStrings.getString("DatabaseManager.errorDisconnecting")); //$NON-NLS-1$
				
			}
			
			LOGGER.info(xStrings.getString("DatabaseManager.shutdownKeepAlive")); //$NON-NLS-1$
			
			if(keepAliveThread != null)
				keepAliveThread.interrupt();
			
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
		
		try {
			if(!databaseConnection.isValid(3)){
				
				//Reconnect
				reconnect(false);
				
			}
			
			
		} catch (SQLException e) {
			
			
			LOGGER.severe(xStrings.getString("DatabaseManager.logErrorReconnecting")); //$NON-NLS-1$
			hasErrors = true;
			
		}
		
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
		
		return getConnection();
		
	}
	
	/**
	 * Returns the MySQL Write Connection.
	 * 
	 * If we haven't connected yet, it will connect first
	 * 
	 * @return
	 */
	public Connection getWriteConnection(){
		
		Connection connection = databaseConnection;
		
		if(hasWriteConnection()){
			
			if(!writeConnected)
				connectWrite();
			
			connection = databaseWriteConnection;
				
		}
		
		try {
			if(!connection.isValid(3)){
				
				//Reconnect
				reconnect(true);
				
			}
			
			
		} catch (SQLException e) {
			
			
			LOGGER.severe(xStrings.getString("DatabaseManager.logErrorReconnecting")); //$NON-NLS-1$
			hasErrors = true;
			
		}
		
		return connection;
		
	}
	
	private void reconnect(boolean write){
		
		if(write){
			writeConnected = false;
			connectWrite();
		}else{
			connected = false;
			connect();
		}
		
	}
	
	/**
	 * Separate method to connect to the write server if it is separate to read server
	 */
	public void connectWrite(){
		
		if(hasWriteConnection()){
			
			try{
				//Connect to separate write DB too
				databaseWriteConnection = DriverManager.getConnection("jdbc:mysql://" +  //$NON-NLS-1$
						settings.get("writeDBHost") + "/" + settings.get("writeDBDatabase") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"?user=" + settings.get("writeDBUser") +  //$NON-NLS-1$ //$NON-NLS-2$
						"&password=" + settings.get("writeDBPass"));  //$NON-NLS-1$//$NON-NLS-2$
				
				writeConnected = true;
				LOGGER.info(xStrings.getString("DatabaseManager.logWriteConnected")); //$NON-NLS-1$
			}catch(SQLException e){
				
				showError(e, xStrings.getString("DatabaseManager.mysqlConnectionError")); //$NON-NLS-1$
				hasErrors = true;
				
			}catch(Exception e){
				
				showError(e, xStrings.getString("DatabaseManager.mysqlDriverException")); //$NON-NLS-1$
				hasErrors = true;
				
			}
			
		}
		
	}
	/**
	 * Connects to the DB, will alert if there are errors.
	 * 
	 * Does nothing if we're already connected.
	 */
	public boolean connect(){
		
		if(!connected){
			
			LOGGER.info(xStrings.getString("DatabaseManager.logConnecting")); //$NON-NLS-1$
			
			if(!hasErrors){
				
				try{
					
					Class.forName("com.mysql.jdbc.Driver").newInstance(); //$NON-NLS-1$
					databaseConnection = DriverManager.getConnection("jdbc:mysql://" +  //$NON-NLS-1$
							database.getString("host") + "/" + database.getString("database") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							"?user=" + database.getString("user") + "&password=" + database.getString("password"));  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					
					LOGGER.info(xStrings.getString("DatabaseManager.logConnected")); //$NON-NLS-1$
					
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
		
		return connected;
		
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
		
		//Further check to see if we're not the same as read connection
		if(writeConnection){
			
			if(settings.get("writeDBHost").equals(database.getString("host")) && //$NON-NLS-1$ //$NON-NLS-2$
					settings.get("writeDBUser").equals(database.getString("user")) && //$NON-NLS-1$ //$NON-NLS-2$
					settings.get("writeDBDatabase").equals(database.getString("database"))) //$NON-NLS-1$ //$NON-NLS-2$
				writeConnection = false;
			
		}
		
		
		return writeConnection;
		
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
	
	/**
	 * Reads the systems phone extensions from the db and populates the systemExtensions
	 * HashSet.  We will use this to figure out if a call is coming into the system or not
	 */
	public HashSet<String> getSystemExtensions(){
		
		LOGGER.info(xStrings.getString("DatabaseManager.readingSystemExtensions")); //$NON-NLS-1$
		
		String SQL = "SELECT option_value FROM clientsettings WHERE option_name = 'myExtension' " + //$NON-NLS-1$
				"OR option_name = 'incomingQueueNumber' OR option_name = 'onAirQueueNumber'"; //$NON-NLS-1$
		
		HashSet<String> systemExtensions = new HashSet<String>();
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			statement = getReadConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	systemExtensions.add(resultSet.getString(1));
		    	
		    }

		}catch (SQLException e){
			LOGGER.severe(xStrings.getString("DatabaseManager.databaseSQLError") +  //$NON-NLS-1$
					e.getMessage());
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
		
		return systemExtensions;
		
	}

}
