package com.thevoiceasia.phonebox.launcher;

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
	private static final Logger LOGGER = Logger.getLogger("com.thevoiceasia.phonebox");//Logger //$NON-NLS-1$
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
				
				boolean createUser = !databaseManager.populateUserSettings("server"); //$NON-NLS-1$
				
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
							
							//Turn off the startup flag so we start processing XMPP messages
							try {
								
								sleep(STARTUP_DELAY);
								asteriskManager.startProcessingMessages();
								
							} catch (InterruptedException e) {
								//We were interrupted flag error and shut down
								showError(e, xStrings.getString("Server.startupInterruptedError")); //$NON-NLS-1$
								e.printStackTrace();
							}
							
							
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
