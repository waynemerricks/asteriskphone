package com.github.waynemerricks.asteriskphone.launcher;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.ManagerCommunicationException;

import com.github.waynemerricks.asteriskphone.asterisk.AsteriskManager;
import com.github.waynemerricks.asteriskphone.chat.ChatManager;
import com.github.waynemerricks.asteriskphone.database.DatabaseManager;
import com.github.waynemerricks.asteriskphone.database.Settings;

public class Server extends Thread{

	/** CLASS VARS **/
	private AsteriskManager asteriskManager;
	private boolean hasErrors = false;
	private DatabaseManager databaseManager;
	private ChatManager chatManager;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger("com.github.waynemerricks.asteriskphone");//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static final long STARTUP_DELAY = 1000L;
	private static I18NStrings xStrings;
	
	public Server(String language, String country){
		
		xStrings = new I18NStrings(language, country);
		setupLogging();
		
		hasErrors = true;
		
		databaseManager = new DatabaseManager(new Settings(), language, country);
		
		if(!databaseManager.hasErrors()){
			
			if(databaseManager.connect()){
				
				boolean createUser = !databaseManager.populateUserSettings("server"); 
				
				chatManager = new ChatManager(databaseManager.getUserSettings()
						.get("XMPPLogin") + "@" +   
					databaseManager.getUserSettings().get("XMPPDomain"),  
					databaseManager.getUserSettings().get("password"), 
					databaseManager.getUserSettings().get("nickName"), 
					databaseManager.getUserSettings().get("XMPPServer"), 
					null,
					databaseManager.getUserSettings().get("XMPPControlRoom"), 
					databaseManager.getUserSettings().get("language"), 
					databaseManager.getUserSettings().get("country"), 
					Integer.parseInt(databaseManager.getUserSettings()
							.get("idleTimeout"))); 
				
				if(createUser){
					
					chatManager.createUser();
					
				}
				
				if(!chatManager.hasErrors()){
					
					chatManager.connect();

					if(chatManager.hasErrors())
						hasErrors = true;
					else{
							
						//Create and Connect to Asterisk
						LOGGER.info(xStrings.getString("Server.creatingAsteriskManager")); 
						asteriskManager = new AsteriskManager(databaseManager,
								chatManager.getControlChatRoom());
						
						LOGGER.info(xStrings.getString("Server.asteriskConnecting")); 
					
						try{
							asteriskManager.connect();
							LOGGER.info(xStrings.getString("Server.asteriskConnected")); 
						
							hasErrors = false; //Reset flag as everything is working
							
							//Turn off the startup flag so we start processing XMPP messages
							try {
								
								sleep(STARTUP_DELAY);
								asteriskManager.startProcessingMessages();
								
							} catch (InterruptedException e) {
								//We were interrupted flag error and shut down
								showError(e, xStrings.getString("Server.startupInterruptedError")); 
								e.printStackTrace();
							}
							
							
						}catch(ManagerCommunicationException e){
						
							showError(e, xStrings.getString("Server.asteriskConnectionError")); 
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
		
		LOGGER.info(xStrings.getString("Server.logSetupLogging")); 
		
		try{
			LOGGER.addHandler(new FileHandler("serverLog.log")); 
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("Server.loggerCreateError")); 
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Server.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Server.logErrorPrefix") + friendlyErrorMessage); 
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
			srv = new Server("en", "GB");  
		
		srv.start();
		
		try {
			srv.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
}
