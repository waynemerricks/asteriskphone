package com.thevoiceasia.phonebox.gui;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.ManagerCommunicationException;

import com.thevoiceasia.phonebox.asterisk.AsteriskManager;

public class Server extends Thread{

	/** CLASS VARS **/
	private AsteriskManager asteriskManager;
	private boolean hasErrors = false;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static I18NStrings xStrings;
	
	public Server(String language, String country){
		
		xStrings = new I18NStrings(language, country);
		setupLogging();
		
		LOGGER.info(xStrings.getString("Server.creatingAsteriskManager")); //$NON-NLS-1$
		asteriskManager = new AsteriskManager();
		LOGGER.info(xStrings.getString("Server.asteriskConnecting")); //$NON-NLS-1$
		
		try{
			asteriskManager.connect();
		}catch(ManagerCommunicationException e){
			
			showError(e, xStrings.getString("Server.asteriskConnectionError")); //$NON-NLS-1$
			hasErrors = true;
		}
		
		if(!hasErrors){
			
			LOGGER.info(xStrings.getString("Server.asteriskShowChannels")); //$NON-NLS-1$
			asteriskManager.showChannels();
			LOGGER.info(xStrings.getString("Server.asteriskShowQueues")); //$NON-NLS-1$
			asteriskManager.showQueues();
			
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
		
		while(true)
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
