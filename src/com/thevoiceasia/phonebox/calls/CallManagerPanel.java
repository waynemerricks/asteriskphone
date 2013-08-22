package com.thevoiceasia.phonebox.calls;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.misc.CountryCodes;
import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.misc.LastActionTimer;

public class CallManagerPanel extends JPanel implements PacketListener, MouseListener, 
									LastActionTimer, ChatManagerListener, MessageListener,
									ManualHangupListener {

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
	private DatabaseManager database;
	private HashMap<String, String> settings;
	private HashMap<String, String> studioExtensions = new HashMap<String, String>();
	private long lastActionTime = new Date().getTime();
	private HashMap<String, String> userExtensions = new HashMap<String, String>();
	private HashSet<String> systemExtensions = new HashSet<String>();
	private boolean dropMode = false;
	private Vector<ManualHangupListener> hangupListeners = new Vector<ManualHangupListener>();
	
	/* We need to spawn threads for event response with db lookups, in order to guard against
	 * craziness, we'll use the ExecutorService to have X threads available to use (set via
	 * DB threadPoolMax
	 */
	private ExecutorService dbLookUpService; 
	private int maxExecutorThreads;
	
	public CallManagerPanel(HashMap<String, String> settings, MultiUserChat controlRoom, 
			DatabaseManager database, XMPPConnection connection){
		
		this.controlRoom = controlRoom;
		this.controlRoom.addMessageListener(this);
		countries = new CountryCodes();
		
		this.settings = settings;
		xStrings = new I18NStrings(settings.get("language"), settings.get("country")); //$NON-NLS-1$ //$NON-NLS-2$
		maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); //$NON-NLS-1$
		dbLookUpService = Executors.newFixedThreadPool(maxExecutorThreads);
		this.database = database;
		
		populateStudioExtensions();
		
		this.setLayout(new MigLayout(new LC().fillX()));
		this.addMouseListener(this);
		
		//Add Private Chat Listener
		connection.getChatManager().addChatListener(this);
		
		//Store a copy of all the system extensions so we can decide what calls we need
		//to handle
		systemExtensions = database.getSystemExtensions();
		
	}
	
	/**
	 * Adds a manual hang up listener to this object
	 * @param listener
	 */
	public void addManualHangupListener(ManualHangupListener listener){
		
		hangupListeners.add(listener);
		
	}
	
	/**
	 * Alert our listeners that they can cancel the hang up mode
	 * this occurs if we're initiating a hang up or if it is someone elses call
	 * and we clicked No, it will also cancel hang up mode
	 */
	private void notifyManualHangupListeners(String channelID){
		
		for(int i = 0; i < hangupListeners.size(); i++){
			
			hangupListeners.get(i).hangupClicked(channelID);
			
		}
		
	}
	
	/**
	 * Sends an UPDATE command to the control room
	 */
	public void sendUpdateRequest(){
		
		try {
			controlRoom.sendMessage(xStrings.getString("CallManagerPanel.commandUpdate") +  //$NON-NLS-1$
					"/" + settings.get("myExtension")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (XMPPException e) {
			LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingUpdateCommand")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Internal method, reads studioExtensions from db and separates them into a hashmap
	 * [extension] => [name]
	 */
	private void populateStudioExtensions(){
		
		//Parse Studio Extensions
		if(settings.get("studioExtensions") != null){ //$NON-NLS-1$
			
			if(settings.get("studioExtensions").contains(",")){  //$NON-NLS-1$//$NON-NLS-2$
				
				String[] studios = settings.get("studioExtensions").split(","); //$NON-NLS-1$ //$NON-NLS-2$
				
				for(int i = 0; i < studios.length; i++){
				
					if(studios[i].contains("=>")){ //$NON-NLS-1$
						
						String[] temp = studios[i].split("=>"); //$NON-NLS-1$
						
						if(temp.length == 2)
							studioExtensions.put(temp[0], temp[1]);
						
					}
					
				}
				
			}else if(settings.get("studioExtensions").contains("=>")){  //$NON-NLS-1$//$NON-NLS-2$
			
				String[] temp = settings.get("studioExtensions").split("=>"); //$NON-NLS-1$ //$NON-NLS-2$
				
				if(temp.length == 2)
					studioExtensions.put(temp[0], temp[1]);
				
			}
				
		}
		
	}
	
	/**
	 * Helper method to create a call info panel and then spawn a thread to grab details
	 * from the DB (via standard Executor)
	 * @param phoneNumber phoneNumber of the call
	 * @param channelID channel id of the call
	 * @param mode Panel Mode to set the initial state to
	 * @param connectedTo Name or extension this panel is connected to
	 */
	private void createSkeletonCallInfoPanel(String phoneNumber, String channelID, int mode,
			String connectedTo){
		
		String location = null;
		LOGGER.info(xStrings.getString("CallManagerPanel.createSkeletonCallPanel") + //$NON-NLS-1$
				phoneNumber + "/" + channelID + "/" + mode); //$NON-NLS-1$ //$NON-NLS-2$
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
		
		int timezoneOffset = 0;
		if(settings.get("timezoneHourOffset") != null) //$NON-NLS-1$
			try{
				timezoneOffset = Integer.parseInt(settings.get("timezoneHourOffset")); //$NON-NLS-1$
			}catch(NumberFormatException e){
				LOGGER.warning(xStrings.getString(
						"CallManagerPanel.logErrorParsingTimezoneOffset") + //$NON-NLS-1$
						settings.get("timezoneHourOffset")); //$NON-NLS-1$
			}
		
		CallInfoPanel call = new CallInfoPanel(settings.get("language"),  //$NON-NLS-1$
				settings.get("country"),  //$NON-NLS-1$
				xStrings.getString("CallManagerPanel.callerUnknown"), //$NON-NLS-1$
				location, "", CallInfoPanel.ALERT_OK, channelID,  //$NON-NLS-1$
				dropMode, true, controlRoom, settings.get("myExtension"),//$NON-NLS-1$
				settings.get("nickName"), timezoneOffset);//$NON-NLS-1$
		
		//Lookup ConnectedTo if we have an entry in the userExtensions map swap to this name
		if(userExtensions.get(connectedTo) != null)
			connectedTo = userExtensions.get(connectedTo);
		
		switch(mode){
		
			case MODE_RINGING:
				call.setRinging();
				break;
			case MODE_ANSWERED:
				call.setAnswered();
				break;
			case MODE_ANSWERED_ELSEWHERE:
				call.setAnsweredElseWhere(connectedTo);
				break;
			case MODE_QUEUED:
				call.setQueued();
				break;
			case MODE_ON_AIR:
				call.setOnAir(connectedTo);
				break;
				
		}
		
		call.addManualHangupListener(this);
		final CallInfoPanel addMe = call;
		
		callPanels.put(channelID, call);
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				add(addMe, "grow, wrap"); //$NON-NLS-1$
				
			}
			
		});
		
		//Spawn thread to populate details
		dbLookUpService.execute(
				new InfoPanelPopulator(database, call, phoneNumber, channelID, location));
		
	}
	
	/**
	 * Internal method that checks if the given extension belongs to a studio
	 * @param extension
	 * @return true if it is a studio extension
	 */
	private boolean isStudioExtension(String extension){
		
		boolean isStudio = false;
		
		if(studioExtensions.get(extension) != null)
			isStudio = true;
		
		return isStudio;
		
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
				 * -- TRANSFER/Channel to Transfer/Number to transfer it to
				 */
				
				//RINGING - Can Ignore
				
				//CALL - Entry point to handler
				if(command.length == 4){
					
					if(command[0].equals(xStrings.getString("CallManagerPanel.callRingingFrom"))){//$NON-NLS-1$
						
						//Create a CallInfoPanel with skeleton details
						if(callPanels.get(command[3]) == null)
							createSkeletonCallInfoPanel(command[1], command[3], MODE_RINGING, null);
						
					}else if(command[0].equals(xStrings.getString("CallManagerPanel.callQueued"))){ //$NON-NLS-1$
					
						//Call Added to QUEUE read queue number and act accordingly
						if(command[1].equals(settings.get("incomingQueueNumber"))){ //$NON-NLS-1$
							
							//IncomingQueue
							/* 
							 * Normally handled by CALL keeping this in case I decide to implement
							 * on hold
							 */
							LOGGER.info(xStrings.getString("CallManagerPanel.CallIncomingQueue")); //$NON-NLS-1$
							
						}else if(command[1].equals(settings.get("onAirQueueNumber"))){ //$NON-NLS-1$
							
							LOGGER.info(xStrings.getString("CallManagerPanel.CallOnAirQueue")); //$NON-NLS-1$
							//On Air Queue
							if(callPanels.get(command[3]) != null){
								
								//Already in our list so update
								callPanels.get(command[3]).setQueued();
								LOGGER.info(xStrings.getString("CallManagerPanel.setQueueMode")); //$NON-NLS-1$
								
							}else{
								
								//Not in our list so create skeleton and spawn update thread
								//queue, name, number, channel
								createSkeletonCallInfoPanel(command[2], command[3], MODE_QUEUED, null);
								
							}
							
						}
						
						
					}else if(command[0].equals(
							xStrings.getString("CallManagerPanel.callConnected"))){ //$NON-NLS-1$
						
						/* I want to only deal with channels that are from outside
						 * In theory this should mean its a channel we already have
						 * however what happens when we log in and a call is in progress?
						 * 
						 * Solution??: When log in, ask for server update and queue up other
						 * commands until updates are dealt with (possible race conditions)
						 * 
						 * grab the channel from active, if it exists deal with it, if it 
						 * doesn't exist check if our number is in it and ignore it.
						 */
						if(callPanels.get(command[3]) != null){
							
							if(command[2].equals(settings.get("myExtension"))) //$NON-NLS-1$
									callPanels.get(command[3]).setAnswered();
							else if(!command[1].equals(settings.get("myExtension"))){ //$NON-NLS-1$
								
								if(systemExtensions.contains(command[2])){
								
									if(isStudioExtension(command[2])){
										
										callPanels.get(command[3]).setOnAir(
												studioExtensions.get(command[2]));
										
									}else{
									
										String connectedTo = userExtensions.get(command[2]);
										
										if(connectedTo != null)
											callPanels.get(command[3]).setAnsweredElseWhere(
													connectedTo);
										else
											callPanels.get(command[3]).setAnsweredElseWhere(
													command[2]);
									
									}
									
								}
								
							}
							
							notifyListeners(callPanels.get(command[3]));
							
						}else{
							
							//Not exists so check details in case something slipped through
							if(!command[1].equals(settings.get("myExtension")) && //$NON-NLS-1$
									!command[2].equals(settings.get("myExtension"))){ //$NON-NLS-1$
								
								//TODO BUG?? Unknown numbers?
								//This isn't us so someone connected to someone else
								//Check if we're connected to someone who is monitored by this program
								if(systemExtensions.contains(command[2])){
									
									//This is an outside call connecting to someone else
									//Check to see if someone else = studio
									if(!isStudioExtension(command[2]))
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_ANSWERED_ELSEWHERE, command[2]);
									else{
										
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_ON_AIR, 
												studioExtensions.get(command[2]));
										
									}
									
								}
								
							}else if(!command[1].equals(settings.get("myExtension"))){ //$NON-NLS-1$
								
								
								// This is us so we're active on a call
								if(systemExtensions.contains(command[2])){
									
									//This is an outside call connecting to us
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_ANSWERED, null);
									
									//Notify listeners that we've answered
									notifyListeners(callPanels.get(command[3]));
									
									/* Not Interested in the reverse because we'll have already dealt with
									 * it here */
									
								}
								
							}
							
						}
						
					}
					
				}else if(command.length == 3 && 
						command[0].startsWith(xStrings.getString("CallManagerPanel.callHangup"))){ //$NON-NLS-1$
					
					//Call Hangup received
					LOGGER.info(
							xStrings.getString("CallManagerPanel.removingPanelHangupReceived") +  //$NON-NLS-1$
									command[2]);
					//Check to see if we have the panel in the list and remove it
					if(callPanels.get(command[2]) != null){
						
						removePanel(command[2]);
						
					}
					
				}else if(command.length == 3 &&
						command[0].equals(xStrings.getString("CallManagerPanel.callTransfer"))){ //$NON-NLS-1$
					
					//Transfer from another user, add their name to our extensions list
					userExtensions.put(command[2], from);
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Notifies any object listening to this CallManagerPanel
	 * Primarily used to 
	 * @param callInfoPanel
	 */
	private void notifyListeners(CallInfoPanel callInfoPanel) {
		/* TODO Need to notify any listeners of this CallManagerPanel, to be implemented
		 * once we have components in place that will act as listeners (call data input 
		 * panel?)
		 */
		LOGGER.info(xStrings.getString("CallManagerPanel.notifyListeners") + //$NON-NLS-1$
				callInfoPanel.getChannelID()); 
		
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
				validate();
				repaint();
				
			}
			
		});
		
	}

	/**
	 * Gets the oldest channel we have in callPanels
	 * If we're a studio only looks at calls waiting to go on air
	 * If not looks at calls that are ringing
	 * @return
	 */
	private CallInfoPanel getOldestChannel(){
		
		Iterator<Entry<String, CallInfoPanel>> calls = callPanels.entrySet().iterator();
		
		String oldestKey = null;
		long oldestTime = 0;
		boolean studio = (settings.get("isStudio") != null //$NON-NLS-1$
				&& settings.get("isStudio").equals("true"));  //$NON-NLS-1$//$NON-NLS-2$
		
		while(calls.hasNext()){
			
			CallInfoPanel call = calls.next().getValue();
			
			if(studio && call.getMode() == CallInfoPanel.MODE_QUEUED){
				
				if(oldestTime == 0 || oldestTime > call.getCallCreationTime()){
					oldestTime = call.getCallCreationTime();
					oldestKey = call.getChannelID();
				}
			
			}else if(!studio && call.getMode() == CallInfoPanel.MODE_RINGING){
				
				if(oldestTime == 0 || oldestTime > call.getCallCreationTime()){
					oldestTime = call.getCallCreationTime();
					oldestKey = call.getChannelID();
				}
				
			}
			
		}
		
		return callPanels.get(oldestKey);
		
	}
	
	/**
	 * Helper method returns true if we're on a call with someone
	 * @return
	 */
	private String isOnCall(){
		
		String onCall = null;
		
		Iterator<Entry<String, CallInfoPanel>> calls = callPanels.entrySet().iterator();
		
		while(calls.hasNext() && onCall == null){
			
			CallInfoPanel call = calls.next().getValue();
			
			if(call.getMode() == CallInfoPanel.MODE_ANSWERED)
				onCall = call.getChannelID();
			
		}
		
		return onCall;
		
	}
	
	/**
	 * Answers the oldest call that is ringing
	 * unless you're a studio in which case it grabs oldest studio ready call
	 */
	public void answerNext() {
		
		if(!callPanels.isEmpty()){
			
			String onCall = isOnCall();
			
			if(onCall == null){
				
				CallInfoPanel oldest = getOldestChannel();
				
				if(oldest != null)
					oldest.answer();
				
			}else if(isStudio()){
				
				//Hangup call we're on and get next one
				callPanels.get(onCall).hangup();
				
				CallInfoPanel oldest = getOldestChannel();
				
				if(oldest != null)
					oldest.answer();
				
			}
			
		}
		
	}
	
	/**
	 * Checks if we're flagged as a studio in the settings
	 * @return
	 */
	private boolean isStudio(){
		
		boolean studio = false;
		
		if(settings.get("isStudio") != null && settings.get("isStudio").equals("true")){ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			
			studio = true;
			
		}
		
		return studio;
		
	}
	
	/**
	 * Answers a random call that is ringing
	 * Unless you're a studio in which case it grabs random studio ready call
	 */
	public void answerRandom() {
		
		if(!callPanels.isEmpty()){
			
			String onCall = isOnCall();
			
			if(onCall == null){
				
				CallInfoPanel random = getRandomCall();
				
				if(random != null)
					random.answer();
				
			}else if(isStudio()){
				
				//Hangup call we're on and get next one
				callPanels.get(onCall).hangup();
				
				CallInfoPanel random = getRandomCall();
				
				if(random != null)
					random.answer();
				
			}
			
		}
		
	}

	/**
	 * Gets a random call that we can answer
	 * @return
	 */
	private CallInfoPanel getRandomCall(){
		
		CallInfoPanel randomIshCall = null;
				
		Iterator<Entry<String, CallInfoPanel>> calls = callPanels.entrySet().iterator();
		
		Vector<String> validKeys = new Vector<String>();
		boolean studio = (settings.get("isStudio") != null //$NON-NLS-1$
				&& settings.get("isStudio").equals("true"));  //$NON-NLS-1$//$NON-NLS-2$
		
		while(calls.hasNext()){
			
			CallInfoPanel call = calls.next().getValue();
			
			if((studio && call.getMode() == CallInfoPanel.MODE_QUEUED) 
					|| (!studio && call.getMode() == CallInfoPanel.MODE_RINGING)){
				
				validKeys.add(call.getChannelID());
				
			}
			
		}
		
		if(validKeys.size() > 0){
			
			Object[] panels = validKeys.toArray();
			
			randomIshCall = callPanels.get(
					panels[new Random().nextInt(panels.length)]); 
			
		}
		
		return randomIshCall;
		
	}
	
	public void dial() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Sets call manager to drop mode, updates all CallInfoPanels to also show drop mode
	 * is active
	 */
	public void setDropMode(boolean active) {
		
		dropMode = active;
		
		if(!callPanels.isEmpty()){
			
			Iterator<Entry<String, CallInfoPanel>> calls = callPanels.entrySet().iterator();
			
			while(calls.hasNext()){
				
				calls.next().getValue().setHangupActive(dropMode);
				
			}
			
		}
		
	}
	
	/* MANUAL HANGUP LISTENER */
	@Override
	public void hangupClicked(String channelID) {
		
		notifyManualHangupListeners(channelID);
		
	}
	
	/* LAST ACTION TIMER */
	@Override
	public long getLastActionTime() {
		
		return lastActionTime;
		
	}
	
	/* CHAT MANAGER LISTENER */
	@Override
	public void chatCreated(Chat chat, boolean createdLocally) {
		
		//New chat initiated so add a message listener to it
		chat.addMessageListener(this);
		LOGGER.info(xStrings.getString("CallManagerPanel.receivedPrivateChatRequest")); //$NON-NLS-1$
		
	}

	/* MESSAGE LISTENER */
	@Override
	public void processMessage(Chat chat, Message message) {
		
		//Can pass this on to the processPacket method as part of normal message handling
		LOGGER.info(xStrings.getString("CallManagerPanel.receivedPrivateMessage") //$NON-NLS-1$
				+ message.getBody()); 
		processPacket(message);
		
	}
	
	/* MOUSE LISTENER */
	@Override
	public void mouseClicked(MouseEvent evt) {
		
		lastActionTime = new Date().getTime();
		
	}

	/* UNUSED MOUSE LISTENER METHODS */
	@Override
	public void mouseEntered(MouseEvent evt){}

	@Override
	public void mouseExited(MouseEvent evt){}

	@Override
	public void mousePressed(MouseEvent evt){}

	@Override
	public void mouseReleased(MouseEvent evt){}

	//TODO BUG Update gives incorrect call times get channel creation date?
	//Don't think I could ever figure out stage time
}
