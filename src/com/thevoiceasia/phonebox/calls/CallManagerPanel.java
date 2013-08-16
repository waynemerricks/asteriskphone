package com.thevoiceasia.phonebox.calls;

import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.misc.CountryCodes;

public class CallManagerPanel extends JPanel implements PacketListener{

	/** STATICS */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallManagerPanel.class.getName());//Logger
	/* Modes:
	 * 1 = Ringing
	 * 2 = Answered
	 * 3 = Answered Elsewhere
	 * 4 = Queued
	 * 5 = On Air
	 */
	private static final int MODE_RINGING = 1;
	private static final int MODE_ANSWERED = 2;
	private static final int MODE_ANSWERED_ELSEWHERE = 3;
	private static final int MODE_QUEUED = 4;
	private static final int MODE_ON_AIR = 5;
	
	/** CLASS VARS */
	private MultiUserChat controlRoom;//room to send control messages to
	private I18NStrings xStrings;
	private CountryCodes countries;
	private HashMap<String, CallInfoPanel> callPanels = new HashMap<String, CallInfoPanel>();
	private Connection databaseReadConnection;
	private HashMap<String, String> settings;
	
	/* We need to spawn threads for event response with db lookups, in order to guard against
	 * craziness, we'll use the ExecutorService to have X threads available to use (set via
	 * DB threadPoolMax
	 */
	private ExecutorService dbLookUpService; 
	private int maxExecutorThreads;
	
	public CallManagerPanel(HashMap<String, String> settings, MultiUserChat controlRoom, 
			Connection databaseReadConnection){
		
		this.controlRoom = controlRoom;
		this.controlRoom.addMessageListener(this);
		countries = new CountryCodes();
		
		this.settings = settings;
		xStrings = new I18NStrings(settings.get("language"), settings.get("country")); //$NON-NLS-1$ //$NON-NLS-2$
		maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); //$NON-NLS-1$
		dbLookUpService = Executors.newFixedThreadPool(maxExecutorThreads);
		this.databaseReadConnection = databaseReadConnection;
		
	}
	
	/**
	 * Helper method to create a call info panel and then spawn a thread to grab details
	 * from the DB (via standard Executor)
	 * @param phoneNumber phoneNumber of the call
	 * @param channelID channel id of the call
	 * @param mode Panel Mode to set the initial state to
	 */
	private void createSkeletonCallInfoPanel(String phoneNumber, String channelID, int mode){
		
		String location = null;
		
		if(phoneNumber.length() < 6)
			location = xStrings.getString(
					"CallManagerPanel.callLocationInternal"); //$NON-NLS-1$
		else if(phoneNumber.length() < 8)
			location = xStrings.getString(
					"CallManagerPanel.callLocationLocal"); //$NON-NLS-1$
		else if(phoneNumber.length() < 12)
			location = xStrings.getString(
					"CallManagerPanel.callLocationNational"); //$NON-NLS-1$
		else//Lookup by phone number
			location = countries.getCountryNameByPhone(phoneNumber);
		
		if(location == null)
			location = xStrings.getString("CallManagerPanel.locationUnknown"); //$NON-NLS-1$
		
		CallInfoPanel call = new CallInfoPanel(settings.get("language"),  //$NON-NLS-1$
				settings.get("country"),  //$NON-NLS-1$
				xStrings.getString("CallManagerPanel.callerUnknown"), //$NON-NLS-1$
				location, "", CallInfoPanel.ALERT_OK, channelID,  //$NON-NLS-1$
				false, true);
		
		switch(mode){
		
			case MODE_RINGING:
				call.setRinging();
				break;
			case MODE_ANSWERED:
				call.setAnswered();
				break;
			case MODE_ANSWERED_ELSEWHERE:
				call.setAnsweredElseWhere();
				break;
			case MODE_QUEUED:
				call.setQueued();
				break;
			case MODE_ON_AIR:
				call.setOnAir();
				break;
				
		}
		
		final CallInfoPanel addMe = call;
		
		callPanels.put(channelID, call);
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				add(addMe);
				
			}
			
		});
		
		//Spawn thread to populate details
		dbLookUpService.execute(
				new InfoPanelPopulator(databaseReadConnection, call));
		
	}
	
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			String from = message.getFrom();
			
			if(from.contains("/")) //$NON-NLS-1$
				from = from.split("/")[1]; //$NON-NLS-1$
			
			if(!from.equals(controlRoom.getNickname())){//If the message didn't come from me 
				
				//React to commands thread all of this if performance is a problem
				LOGGER.info(xStrings.getString("CallManager.receivedMessage") + //$NON-NLS-1$
						message.getBody()); 
				
				String[] command = message.getBody().split("/"); //$NON-NLS-1$
				/*
				 * Current Control Messages:
				 * -- RINGING/Number thats ringing/Channel thats ringing
				 * -- CALL/From Number/To Number/Channel that is calling
				 * -- QUEUE/Queue Name/Channel that is queued
				 * -- HANGUP/Number Hung up/Channel that Hung up
				 */
				
				//RINGING - Can Ignore
				
				//CALL - Entry point to handler
				if(command.length == 4){
					
					if(command[0].equals("CallManagerPanel.callRingingFrom")){//$NON-NLS-1$
						
						//Create a CallInfoPanel with skeleton details
						if(callPanels.get(command[3]) == null)
							createSkeletonCallInfoPanel(command[1], command[3], MODE_RINGING);
						
					}else if(command[0].equals(xStrings.getString("CallManager.callQueued"))){ //$NON-NLS-1$
					
						//Call Added to QUEUE read queue number and act accordingly
						if(command[1].equals(settings.get("incomingQueueNumber"))){ //$NON-NLS-1$
							
							//IncomingQueue
							/* 
							 * should be handled by CALL event kept here in case I decide
							 * to implement on hold
							 */
							
						}else if(command[1].equals(settings.get("queueNumber"))){ //$NON-NLS-1$
							
							//On Air Queue
							if(callPanels.get(command[2]) != null){
								
								//Already in our list so update
								callPanels.get(command[2]).setQueued();
								
							}else{
								
								//Not in our list so create skeleton and spawn update thread
								//queue, name, number, channel
								createSkeletonCallInfoPanel(command[3], command[4], MODE_QUEUED);
								
							}
							
						}
						
						
					}
					
				}else if(command.length == 3 && 
						command[0].equals(xStrings.getString("CallManager.callHangup"))){ //$NON-NLS-1$
					
					//Call Hangup received
					//Check to see if we have the panel in the list and remove it
					if(callPanels.get(command[2]) != null){
						
						removePanel(command[2]);
						
					}
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Removes the given callInfoPanel (denoted by channel ID)
	 * @param channelID channel to remove
	 */
	public void removePanel(String channelID){
		
		final String channel = channelID;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				remove(callPanels.get(channel));
				callPanels.remove(channel);
				
			}
			
		});
		
	}
	
}
