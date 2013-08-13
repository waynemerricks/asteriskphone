package com.thevoiceasia.phonebox.gui;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.ManagerCommunicationException;

import com.thevoiceasia.phonebox.asterisk.AsteriskManager;
import com.thevoiceasia.phonebox.chat.ChatManager;
import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.database.Settings;

public class Server extends Thread{

	/** CLASS VARS **/
	private AsteriskManager asteriskManager;
	private boolean hasErrors = false;
	private DatabaseManager databaseManager;
	private ChatManager chatManager;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static I18NStrings xStrings;
	
	public Server(String language, String country){
		
		xStrings = new I18NStrings(language, country);
		setupLogging();
		
		hasErrors = true;
		
		databaseManager = new DatabaseManager(new Settings(), language, country);
		
		if(!databaseManager.hasErrors()){
			
			if(databaseManager.connect()){
				
				boolean createUser = !databaseManager.populateUserSettings();
				
				chatManager = new ChatManager(databaseManager.getUserSettings()
						.get("XMPPLogin") + "@" +  //$NON-NLS-1$ //$NON-NLS-2$
					databaseManager.getUserSettings().get("XMPPDomain"), //$NON-NLS-1$ 
					databaseManager.getUserSettings().get("password"), //$NON-NLS-1$
					databaseManager.getUserSettings().get("nickName"), //$NON-NLS-1$
					databaseManager.getUserSettings().get("XMPPServer"), //$NON-NLS-1$
					null,
					databaseManager.getUserSettings().get("XMPPControlRoom"), //$NON-NLS-1$
					databaseManager.getUserSettings().get("language"), //$NON-NLS-1$
					databaseManager.getUserSettings().get("country"), //$NON-NLS-1$
					Integer.parseInt(databaseManager.getUserSettings()
							.get("idleTimeout"))); //$NON-NLS-1$
				
				if(createUser){
					
					chatManager.createUser();
					
				}
				
				if(!chatManager.hasErrors()){
					
					chatManager.connect();

					if(chatManager.hasErrors())
						hasErrors = true;
					else{
							
						//Create and Connect to Asterisk
						LOGGER.info(xStrings.getString("Server.creatingAsteriskManager")); //$NON-NLS-1$
						asteriskManager = new AsteriskManager(databaseManager,
								chatManager.getControlChatRoom());
						
						LOGGER.info(xStrings.getString("Server.asteriskConnecting")); //$NON-NLS-1$
					
						try{
							asteriskManager.connect();
							LOGGER.info(xStrings.getString("Server.asteriskConnected")); //$NON-NLS-1$
						
							hasErrors = false; //Reset flag as everything is working
							//asteriskManager.createCall("5001", "5002", "TEST 2");
							//asteriskManager.redirectCall("1376399231.403", "5002", "TEST 2");
							//asteriskManager.redirectCallToQueue("1376313386.343");
							//asteriskManager.hangupCall("1376399231.403", "TEST 2");
						}catch(ManagerCommunicationException e){
						
							showError(e, xStrings.getString("Server.asteriskConnectionError")); //$NON-NLS-1$
							hasErrors = true;
						
						}
						
					}
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Set the Logger object
	 * 
	 */
	private void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		
		LOGGER.info(xStrings.getString("Server.logSetupLogging")); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("serverLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("Server.loggerCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Server.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Server.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	public void run(){
		
		while(!hasErrors)
			try{
				sleep(1000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
	}
	
	public static void main(String[] args){
		
		Server srv = null;
		
		if(args.length == 2)
			srv = new Server(args[0], args[1]);
		else
			srv = new Server("en", "GB"); //$NON-NLS-1$ //$NON-NLS-2$
		
		srv.start();
		
		try {
			srv.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}
