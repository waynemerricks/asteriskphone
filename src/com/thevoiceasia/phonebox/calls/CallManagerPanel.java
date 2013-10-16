package com.thevoiceasia.phonebox.calls;

import java.awt.Component;
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
import com.thevoiceasia.phonebox.callinput.CallerUpdater;
import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.launcher.Client;
import com.thevoiceasia.phonebox.misc.LastActionTimer;

public class CallManagerPanel extends JPanel implements PacketListener, MouseListener, 
									LastActionTimer, ChatManagerListener, MessageListener,
									ManualHangupListener, DialListener {

	/* TODO When on a call, you can't answer another one unless that first one gets queued
	 * first? 
	 */
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
	private DialPanel dialler = null;
	private Vector<AnswerListener> answerListeners = new Vector<AnswerListener>();
	private CallerUpdater updateCallerThread = null;
	private CallInfoPanel storedAnsweredPanel = null;
	
	/* We need to spawn threads for event response with db lookups, in order to guard 
	 * against craziness, we'll use the ExecutorService to have X threads available 
	 * to use (set via DB threadPoolMax)
	 */
	private ExecutorService dbLookUpService; 
	private int maxExecutorThreads;
	
	public CallManagerPanel(HashMap<String, String> settings, MultiUserChat controlRoom, 
			DatabaseManager database, XMPPConnection connection){
		
		//Spawn a thread to update any callinfo panels in response to user input
		updateCallerThread = new CallerUpdater(controlRoom, settings.get("language"), //$NON-NLS-1$
					settings.get("country")); //$NON-NLS-1$
		new Thread(updateCallerThread).start();
		
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
			String connectedTo, long creationTime){
		
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
				settings.get("nickName"), timezoneOffset, null);//$NON-NLS-1$
		
		//Set the creation time as required
		if(creationTime != -1)
			call.setCallCreationTime(creationTime);
		
		//Lookup ConnectedTo if we have an entry in the userExtensions map swap to this name
		String friendlyConnected = ""; //$NON-NLS-1$
		
		if(userExtensions.get(connectedTo) != null)
			friendlyConnected = userExtensions.get(connectedTo);
		
		switch(mode){
		
			case CallInfoPanel.MODE_RINGING:
				call.setRinging(friendlyConnected, true);
				break;
			case CallInfoPanel.MODE_RINGING_ME:
				call.setRingingMe(true);
				break;
			case CallInfoPanel.MODE_ANSWERED:
				call.setAnswered(true);
				break;
			case CallInfoPanel.MODE_ANSWERED_ME:
				call.setAnsweredMe(friendlyConnected, true);
				break;
			case CallInfoPanel.MODE_ANSWERED_ELSEWHERE:
				call.setAnsweredElseWhere(friendlyConnected, true);
				break;
			case CallInfoPanel.MODE_QUEUED:
				call.setQueued(true);
				break;
			case CallInfoPanel.MODE_QUEUED_ME:
				call.setQueuedMe(true);
				break;
			case CallInfoPanel.MODE_ON_AIR:
				call.setOnAir(connectedTo);
				break;
			case CallInfoPanel.MODE_ON_AIR_ME:
				call.setOnAirMe(friendlyConnected, true);
				break;
				
		}
		
		call.setUpdaterThread(updateCallerThread);
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
	 * Gets the update thread for use with the CallInfo when it is updated
	 * @return
	 */
	public CallerUpdater getCallerUpdateThread(){
		
		return updateCallerThread;
		
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
				if(command.length == 4 || command.length == 5){
					
					//Creation time here if command.length = 5
					long creationTime = -1;
					
					if(command.length == 5)
						creationTime = getCreationTime(command[4]);
					
					if(command[0].equals(xStrings.getString("CallManagerPanel.callRingingFrom"))){//$NON-NLS-1$
						
						//Create a CallInfoPanel with skeleton details
						if(callPanels.get(command[3]) == null){
						
							/*
							 * Was too simplistic to use CALL/FROM/TO/CHANNEL as a way of determining
							 * outside calls coming in.
							 * 
							 * Exception case: Call from system extension to an outside line you get:
							 * CALL/1234/4444444/CHANNEL
							 * 
							 * What we want to happen is:
							 * 
							 * Check if call is from us and not another system extension and to outside
							 * 		set the panel to answered
							 * If call is from another system extension and to outside
							 * 		set the panel to answered elsewhere
							 * if call is internal from system to system carry on as normal but
							 * 		set connected to as the person they're dialling
							 */
							if(systemExtensions.contains(command[1]) && 
									systemExtensions.contains(command[2])){
								
								//Internal call amongst ourselves
								if(isMyPhone(command[1])){
									int mode = CallInfoPanel.MODE_RINGING_ME;
									
									if(isOnAirQueue(command[2]))
										mode = CallInfoPanel.MODE_QUEUED_ME;
									
									createSkeletonCallInfoPanel(command[1], command[3], 
											mode, command[2], creationTime);
									
									callPanels.get(command[3]).setOriginator(command[1]);
								}else{
									int mode = CallInfoPanel.MODE_RINGING;
									
									if(isOnAirQueue(command[2]))
										mode = CallInfoPanel.MODE_QUEUED;
									
									createSkeletonCallInfoPanel(command[1], command[3], 
											mode, command[2], creationTime);
									
									callPanels.get(command[3]).setOriginator(command[1]);
								}
								
							}else if(systemExtensions.contains(command[1]) && 
									!systemExtensions.contains(command[2])){
								
								//Internal call to outside from me
								if(isMyPhone(command[1])){
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_RINGING_ME, command[2], 
											creationTime);
									callPanels.get(command[3]).setOriginator(command[1]);
								}else{//Internal call to outside not from me
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_ANSWERED_ELSEWHERE, command[2], 
											creationTime);
									callPanels.get(command[3]).setOriginator(command[1]);
								}
							}else if(!systemExtensions.contains(command[1]) && 
									systemExtensions.contains(command[2])){
								
								//Outside call coming in
								if(isIncomingQueue(command[2])){
									
									//Outside call coming into a queue as normal
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_RINGING, null, creationTime);
									callPanels.get(command[3]).setOriginator(command[1]);
									
									if(settings.get("queue_" + command[2] + "_icon") != null) //$NON-NLS-1$ //$NON-NLS-2$
										callPanels.get(command[3]).getIconPanel().setBadgeIcon(settings.get("queue_" + command[2] + "_icon"));  //$NON-NLS-1$//$NON-NLS-2$
									
								}else if(isOnAirQueue(command[2])){
									
									//Outside call coming into a queue as normal
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_QUEUED, null, creationTime);
									
									callPanels.get(command[3]).setOriginator(command[1]);
									
									if(settings.get("queue_" + command[2] + "_icon") != null) //$NON-NLS-1$ //$NON-NLS-2$
										callPanels.get(command[3]).getIconPanel().setBadgeIcon(settings.get("queue_" + command[2] + "_icon"));  //$NON-NLS-1$//$NON-NLS-2$
									
								}else{
									
									//Outside call coming direct to a phone
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_RINGING, command[2], creationTime);
									callPanels.get(command[3]).setOriginator(command[1]);
								}
								
							}
							
							
						}
						
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
								if(isMyPhone(command[2])){
									callPanels.get(command[3]).setQueuedMe(true);
									LOGGER.info(xStrings.getString("CallManagerPanel.setQueueMeMode")); //$NON-NLS-1$
								}else{
									callPanels.get(command[3]).setQueued(true);
									LOGGER.info(xStrings.getString("CallManagerPanel.setQueueMode")); //$NON-NLS-1$
								}
								
							}else{
								
								//Not in our list so create skeleton and spawn update thread
								//queue, name, number, channel
								if(isMyPhone(command[2]))
									createSkeletonCallInfoPanel(command[2], command[3], 
											CallInfoPanel.MODE_QUEUED_ME, null, -1);
								else
									createSkeletonCallInfoPanel(command[2], command[3], 
											CallInfoPanel.MODE_QUEUED, null, -1);
								
							}
							
						}
						
						
					}else if(command[0].equals(
							xStrings.getString("CallManagerPanel.callConnected"))){ //$NON-NLS-1$
						
						if(callPanels.get(command[3]) != null){
							
							//Connected to a panel that already exists
							/* if 1st argument = myphone its my phone connecting to 
							 * someone else */
							if(isMyPhone(command[1])){
								/* At this stage, check if my phone is on air */
								if(isStudioExtension(command[2]))
									callPanels.get(command[3]).setOnAirMe(
											studioExtensions.get(command[2]), true);
								else
									callPanels.get(command[3]).setAnsweredMe(command[2], 
											true);
							/* if 2nd argument = myphone its a call I've answered */
							}else if(isMyPhone(command[2])){
								
								callPanels.get(command[3]).setAnswered(true);
								
								/* This is us answering a call so we need to alert any 
								 * listeners so that we can update the input panel as
								 * required
								 */
								notifyListeners(callPanels.get(command[3]));
								
							/* Not my phone, if 1st argument = one of our extensions, its
							 * an internal phone connected somewhere else
							 */
							}else if(systemExtensions.contains(command[1])){
								
								/* If 2nd argument is studio then internal call on air*/
								if(isStudioExtension(command[2]))
									callPanels.get(command[3]).setAnsweredElseWhere(
											studioExtensions.get(command[2]), true);
								else{
									
									/* Lookup the extension, if we have a reference
									 * exchange the number for a friendly name e.g.
									 * 5001 = Steve, if its null leave it as 5001
									 */
									String connectedTo = userExtensions.get(command[2]);
									
									if(connectedTo != null)
										callPanels.get(command[3]).setAnsweredElseWhere(
												connectedTo, true);
									else
										callPanels.get(command[3]).setAnsweredElseWhere(
												command[2], true);
								}
								
							}else if(systemExtensions.contains(command[2])){
								
								/* If second argument is a system extension, this is an
								 * outside call coming in but not to my phone
								 */
								if(isStudioExtension(command[2]))
									callPanels.get(command[3]).setOnAir(
											studioExtensions.get(command[2]));
								else{
									
									/* Lookup the extension, if we have a reference
									 * exchange the number for a friendly name e.g.
									 * 5001 = Steve, if its null leave it as 5001
									 */
									String connectedTo = userExtensions.get(command[2]);
									
									if(connectedTo != null)
										callPanels.get(command[3]).setAnsweredElseWhere(
												connectedTo, true);
									else
										callPanels.get(command[3]).setAnsweredElseWhere(
												command[2], true);
									
								}
								
							}else{
								
								/* Here we arrive in an odd state because neither number
								 * is registering as any phone in the system.  However, when dialling out
								 * the callerid is swapped to the outgoing caller id so a call that 
								 * would start as:
								 * 
								 * 5002 -> 907886123456
								 * 
								 * Would become:
								 * 
								 * 01211234567 -> 907886123456
								 * 
								 * This channel obviously exists so we need to look at it, figure out
								 * if its us and then set this as ANSWERED_ME
								 */
								if(isMyPhone(callPanels.get(command[3]).getOriginator())){ 
									//ANSWERED_ME
									callPanels.get(command[3]).setAnsweredMe(command[2], true);
									
								}else{
									//ANSWERED_ELSEWHERE
									callPanels.get(command[3]).setAnsweredElseWhere(command[2], true);
								}
								
							}
							
						}else{
							
							//Not exists so check details in case something slipped through
							if(!isMyPhone(command[1]) && !isMyPhone(command[2])){
								
								//This isn't us so someone connected to someone else
								//Check if we're connected to someone who is monitored by this program
								if(systemExtensions.contains(command[2])){
									
									//This is an outside call connecting to someone else
									//Check to see if someone else = studio
									if(!isStudioExtension(command[2]))
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_ANSWERED_ELSEWHERE, command[2], 
												creationTime);
									else{
										
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_ON_AIR, 
												studioExtensions.get(command[2]), 
												creationTime);
										
									}
									
								}
								//the caller is not me but the receiver is
							}else if(!isMyPhone(command[1]) && isMyPhone(command[2])){
								
								/* Someone connected to us, most likely this is a second channel
								 * for the receiver after we've dialled.
								 * 
								 * So check to see if we aren't already connected to this person
								 * on another channel and if so ignore it, else make the panel
								 */
								
								if(!isAlreadyConnected(command[1])){
									
									/* Don't have a panel for this AND caller is from an
									 * internal or external phone
									 */
									createSkeletonCallInfoPanel(command[1], command[3],
											CallInfoPanel.MODE_ANSWERED, null, creationTime);
								
									notifyListeners(callPanels.get(command[3]));
									
								}else{
									
									/* If already connected is an older channel then we 
									 * dialled this channel so need to change the mode to 
									 * an answered_me instead of generic answered 
									 */
									double oldChannel = Double.parseDouble(
											getAlreadyConnectedChannel(command[1]));
									double newChannel = Double.parseDouble(command[3]);
									
									if(oldChannel < newChannel && callPanels.get(
											oldChannel + "") != null) //$NON-NLS-1$
										callPanels.get(oldChannel + "").setAnsweredMe( //$NON-NLS-1$
												command[1], false);
									
								}
								
								//Caller is me and the receiver isn't
							}else if(isMyPhone(command[1]) && !isMyPhone(command[2])){
								
								//Not interested would have been dealt with elsewhere
								
							}
							
						}
						//UPDATEFIELD
					}else if(command[0].equals(xStrings.getString(
							"CallManagerPanel.commandUpdateField"))){ //$NON-NLS-1$
						//		0			1			2			3
						//UPDATEFIELD/field mapping/channel id/field value
						/*if(command[3].equals("<CLEAR>")) //$NON-NLS-1$
							command[3] = ""; //$NON-NLS-1$*/
						
						if(command[1].equals("name")){ //$NON-NLS-1$
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),  //$NON-NLS-1$ //$NON-NLS-2$
									false);
							callPanels.get(command[2]).setCallerName(
									command[3].replace("^^%%$$", "/"), false);  //$NON-NLS-1$//$NON-NLS-2$
						}else if(command[1].equals("location")){ //$NON-NLS-1$
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),  //$NON-NLS-1$ //$NON-NLS-2$
									false);
							callPanels.get(command[2]).setCallerLocation(command[3], false);
						}else if(command[1].equals("conversation")){ //$NON-NLS-1$
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),  //$NON-NLS-1$ //$NON-NLS-2$
									false);
							callPanels.get(command[2]).setConversation(command[3], false);
						}else if(command[1].equals("alert")){ //$NON-NLS-1$
							
							String[] temp = command[3].split("@@"); //$NON-NLS-1$
							
							try{
								int level = Integer.parseInt(temp[1]);
								callPanels.get(command[2]).setAlertLevel(null, level, false);
							}catch(NumberFormatException e){
								callPanels.get(command[2]).setAlertLevel(null,
										temp[1].replace("+", "/"), false);  //$NON-NLS-1$//$NON-NLS-2$
							}
							
							callPanels.get(command[2]).setPhoneCallField("alert", //$NON-NLS-1$
									temp[0], false);
							
						}else if(command[1].equals("calltype")){ //$NON-NLS-1$
							
							//Split the image from the value:
							String[] temp = command[3].replace("+", "/").split("@@");//$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
							callPanels.get(command[2]).setBadgeIcon(null,
										temp[1], false);  
							//Set the record call type
							callPanels.get(command[2]).setPhoneCallField("calltype",  //$NON-NLS-1$
									temp[0], false);
						}else{
							
							//Custom Field
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),  //$NON-NLS-1$ //$NON-NLS-2$
									false);
							
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
					
				}else if(command.length == 2 &&
						command[0].equals(xStrings.getString("CallManagerPanel.callLocked"))){ //$NON-NLS-1$
					
					/* Notification that call was locked wait 3 seconds and if panel is 
					 * still clicked reset it to normal mode
					 */
					if(callPanels.get(command[1]) != null){
						
						new Thread(new LockedWaitThread(callPanels.get(command[1]))).start();
						
					}//If it doesn't exist then ignore it, the call probably ended
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Parses the given time (should be in the form of Date().getTime())
	 * @param time time to parse
	 * @return a Date().getTime() based on the given time
	 */
	private long getCreationTime(String time){
		
		long created = new Date().getTime();
		
		try{
			
			created = Long.parseLong(time);
			
		}catch(NumberFormatException e){
			
			LOGGER.warning(xStrings.getString("CallManagerPanel.errorParsingCreationTime")); //$NON-NLS-1$
			
		}
		
		return created;
		
	}
	
	/**
	 * Gets the channel ID where this number is already connected to
	 * @param number
	 * @return Asterisk channel ID or null if not found 
	 */
	private String getAlreadyConnectedChannel(String number) {
		
		boolean found = false;
		String channelID = null;
		
		Iterator<String> panels = callPanels.keySet().iterator();
		
		while(panels.hasNext() && found == false){
			
			CallInfoPanel panel = callPanels.get(panels.next());
			
			if(panel.getConnectedTo() != null && panel.getConnectedTo().length() > 0){
				
				//5001 || tvaadmin
				try{
					Integer.parseInt(panel.getConnectedTo());
					
					if(panel.getConnectedTo().equals(number))
						channelID = panel.getChannelID();
				}catch(NumberFormatException e){
					
					if(number.equals(getNumberFromFriendlyName(panel.getConnectedTo())))
							channelID = panel.getChannelID();
					
				}
				
			}
			
		}
		
		return channelID;
		
	}

	/**
	 * Checks the panels to make sure we're not already connected to this number.  E.g.
	 * When connecting you get 5001 -> 5002 and the reverse, 5002 -> 5001 so you know
	 * both sides of the call are connected.
	 * 
	 * This causes problems with duplicate panels and weird behaviour
	 * 
	 * @param number number to check
	 * @return true if we're already connected
	 */
	private synchronized boolean isAlreadyConnected(String number){
		
		boolean isConnected = false;
		
		Iterator<String> panels = callPanels.keySet().iterator();
		
		while(panels.hasNext() && isConnected ==
				false){
			
			CallInfoPanel panel = callPanels.get(panels.next());
			
			if(panel.getConnectedTo() != null && panel.getConnectedTo().length() > 0){
				
				//5001 || tvaadmin
				try{
					Integer.parseInt(panel.getConnectedTo());
					
					if(panel.getConnectedTo().equals(number))
						isConnected = true;
				}catch(NumberFormatException e){
					
					if(number.equals(getNumberFromFriendlyName(panel.getConnectedTo())))
							isConnected = true;
					
				}
				
			}
			
		}
		
		return isConnected;
		
	}
	
	/**
	 * Iterates through userExtensions and returns the extension value given by 
	 * the name
	 * @param name name to search for
	 * @return null if no match or extension if match e.g. "5001"
	 */
	private String getNumberFromFriendlyName(String name) {
		
		String number = null;
		Iterator<String> extensions = userExtensions.keySet().iterator();
		
		while(extensions.hasNext() && number == null){
			
			String key = extensions.next();
			
			if(userExtensions.get(key).equals(name))
				number = key;
			
		}
		
		return number;
		
	}

			
	/**
	 * Checks if the given number is the on air queue
	 * @param number
	 * @return true if it is the on air queue
	 */
	private boolean isOnAirQueue(String number){
	
		boolean isOnAir = false;
		
		if(settings.get("onAirQueueNumber").equals(number)) //$NON-NLS-1$
			isOnAir = true;
		
		return isOnAir;
		
	}
	
	/**
	 * Checks if the given number is the on air queue
	 * @param number
	 * @return true if it is the on air queue
	 */
	private boolean isIncomingQueue(String number){
	
		boolean isIncoming = false;
		
		if(settings.get("incomingQueueNumber").equals(number)) //$NON-NLS-1$
			isIncoming = true;
		
		return isIncoming;
		
	}
	
	/**
	 * Notifies any object listening to this CallManagerPanel
	 * Primarily used to 
	 * @param callInfoPanel
	 */
	private void notifyListeners(CallInfoPanel callInfoPanel) {
		
		// Need to notify any listeners of this CallManagerPanel
		
		if(answerListeners.size() < 1){
			
			LOGGER.info(xStrings.getString("CallManagerPanel.storeForNotifyListeners")); //$NON-NLS-1$
			storedAnsweredPanel = callInfoPanel;
			
		}else{
			
			LOGGER.info(xStrings.getString("CallManagerPanel.notifyListeners") + //$NON-NLS-1$
					callInfoPanel.getChannelID()); 
			
			for(int i = 0; i < answerListeners.size(); i++)
				answerListeners.get(i).callAnswered(callInfoPanel);
			
			//We've notified so reset stored to null if applicable
			if(storedAnsweredPanel != null)
				storedAnsweredPanel = null;
			
		}
		
	}
	
	/**
	 * Add a listener for a call being answered
	 * @param listener
	 */
	public void addAnswerListener(AnswerListener listener){
		
		LOGGER.info(xStrings.getString("CallManagerPanel.addAnswerListener")); //$NON-NLS-1$
		
		//Deadlock somewhere in here and notify listeners
		synchronized(answerListeners){
			answerListeners.add(listener);
		}
		
		//If we have a stored panel notify
		if(storedAnsweredPanel != null)
			notifyListeners(storedAnsweredPanel);
			
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
		
		if(!callPanels.isEmpty() && settings.get("myExtension") != null){ //$NON-NLS-1$
			
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
	 * Helper to check if a given extension is my phone extension
	 * Helps to make code more readable
	 * @param extension extension to check
	 * @return true if it is my extension
	 */
	private boolean isMyPhone(String extension){
		
		boolean myPhone = false;
		
		if(settings.get("myExtension").equals(extension)) //$NON-NLS-1$
			myPhone = true;
		
		return myPhone;
		
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
		
		if(!callPanels.isEmpty() && settings.get("myExtension") != null){ //$NON-NLS-1$
			
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
	
	/**
	 * Spawns a dialling panel and will then make a call to the result
	 */
	public void dial() {
		
		if(dialler == null && settings.get("myExtension") != null){ //$NON-NLS-1$
			
			Component parent = this.getParent();
			Client owner = null;
			
			while(parent != null && !(parent instanceof Client))
				parent = parent.getParent();
			
			if(parent != null)
				owner = (Client)parent;
			
			dialler = new DialPanel(owner, xStrings.getString("DialPanel.title"), //$NON-NLS-1$
					settings.get("language"), settings.get("country"));  //$NON-NLS-1$//$NON-NLS-2$
			dialler.addDialListener(this);
			
		}
		
		if(settings.get("myExtension") != null) //$NON-NLS-1$
			dialler.setVisible(true);
		
	}

	/**
	 * DialListener entry point, this sends a message via control to call the given number
	 */
	public void dial(String number){
	
		try {
			controlRoom.sendMessage(xStrings.getString("CallManagerPanel.commandDial") +  //$NON-NLS-1$
					"/" + number + "/" + settings.get("myExtension")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} catch (XMPPException e) {
			LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingDialCommand")); //$NON-NLS-1$
		}
		
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

	//TODO When server connects, request an update?
	
}
