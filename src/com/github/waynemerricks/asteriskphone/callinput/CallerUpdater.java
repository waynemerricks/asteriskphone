package com.github.waynemerricks.asteriskphone.callinput;

import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class CallerUpdater implements Runnable{

	/** STATIC VARS **/
	private static final long UPDATE_PERIOD = 1000, SLEEP_PERIOD = 100;
	private static final Logger LOGGER = Logger.getLogger(CallerUpdater.class.getName());//Logger
	
	/* CLASS VARS */
	private MultiUserChat controlRoom;
	private HashMap<String, String> updateFields = new HashMap<String, String>();
	private boolean go = true;
	private I18NStrings xStrings;
	private long lastUpdate;
	
	/**
	 * Creates a CallerUpdater that takes in update commands and sends them to clients 
	 * via XMPP every UPDATE_PERIOD
	 * @param controlRoom
	 * @param language
	 * @param country
	 */
	public CallerUpdater(MultiUserChat controlRoom, String language, String country) {
		
		this.controlRoom = controlRoom;
		xStrings = new I18NStrings(language, country);
		lastUpdate = new Date().getTime();
		
	}
	
	/**
	 * Add a field to update to the queue
	 * @param channelID channel ID that this update applies to
	 * @param field mapping of the field to update
	 * @param value value to update it to
	 */
	public synchronized void addUpdate(String channelID, String field, String value){
		
		LOGGER.info(xStrings.getString("CallerUpdater.addingUpdateToQueue")); 
		lastUpdate = new Date().getTime();
		
		if( value == null || value.length() == 0)
			value = " "; 
		
		updateFields.put(field, channelID + "/" + value); 
		
	}
	
	/**
	 * Sends an XMPP message to the controlRoom MultiUserChat
	 * @param message
	 */
	private void sendMessage(String message){
		
		LOGGER.info(xStrings.getString("CallerUpdater.sendingControlMessage") + " "   
				+ message); 
		
		if(controlRoom != null){
			
			try {
				
				controlRoom.sendMessage(message);
				
			}catch(XMPPException e){
				LOGGER.severe(xStrings.getString("CallerUpdater.chatRoomError")); 
			}catch(IllegalStateException e){
				LOGGER.severe(xStrings.getString("CallerUpdater.ServerGoneError")); 
			}
			
		}else
			LOGGER.severe(xStrings.getString("CallerUpdater.noControlRoomSet")); 
		
	}
	
	/**
	 * Loops through the updateFields hash and sends update messages
	 */
	public void run(){
		
		LOGGER.info(xStrings.getString("CallerUpdater.running")); 
		
		while(go){
			
			synchronized(updateFields){
			
				if(updateFields.size() > 0 && 
						new Date().getTime() - lastUpdate > UPDATE_PERIOD){
					
					//We have fields to update so update it you fool
					try{
						Iterator<String> keys = updateFields.keySet().iterator();
						
						while(keys.hasNext()){
							
							String field = keys.next();
							
							sendMessage(xStrings.getString("CallerUpdater.commandUpdateField") + "/"  
									+ field + "/" + updateFields.get(field)); 
							
							updateFields.remove(field);
							
						}
					}catch(ConcurrentModificationException e){
						
						//Don't care just try again next time
						
					}
						
				}
				
			}
			
			try {
				Thread.sleep(SLEEP_PERIOD);
			} catch (InterruptedException e) {
				go = false;
			}
			
		}
		
		LOGGER.info(xStrings.getString("CallerUpdater.exiting")); 
		
	}

}
