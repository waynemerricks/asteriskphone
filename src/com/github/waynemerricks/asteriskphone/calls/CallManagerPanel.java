package com.github.waynemerricks.asteriskphone.calls;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.ArrayList;
//import java.math.BigDecimal;
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

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jxmpp.jid.EntityBareJid;

import com.github.waynemerricks.asteriskphone.callinput.CallerUpdater;
import com.github.waynemerricks.asteriskphone.database.DatabaseManager;
import com.github.waynemerricks.asteriskphone.launcher.Client;
import com.github.waynemerricks.asteriskphone.misc.CountryCodes;
import com.github.waynemerricks.asteriskphone.misc.LastActionTimer;
import com.github.waynemerricks.asteriskphone.misc.PhoneRinger;
import com.github.waynemerricks.asteriskphone.records.Person;
import com.github.waynemerricks.asteriskphone.records.PhoneCall;

public class CallManagerPanel extends JPanel implements MessageListener, MouseListener, 
									LastActionTimer, IncomingChatMessageListener,
									ManualHangupListener, DialListener {

	/** STATICS */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallManagerPanel.class.getName());//Logger
	
	/** CLASS VARS */
	private MultiUserChat controlRoom;//room to send control messages to
	private I18NStrings xStrings;
	private String country, language;
	private CountryCodes countries;
	private HashMap<String, CallInfoPanel> callPanels = new HashMap<String, CallInfoPanel>();
	//private HashMap<String, EndPointRecord> endPoints = new HashMap<String, EndPointRecord>();
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
	private PhoneRinger ringer = null;
	private ArrayList<String> ringingChannels = new ArrayList<String>();

	/* We need to spawn threads for event response with db lookups, in order to guard 
	 * against craziness, we'll use the ExecutorService to have X threads available 
	 * to use (set via DB threadPoolMax)
	 */
	private ExecutorService dbLookUpService; 
	private int maxExecutorThreads;
	
	public CallManagerPanel(HashMap<String, String> settings, MultiUserChat controlRoom, 
			DatabaseManager database, XMPPConnection connection){
		
		//Spawn a thread to update any callinfo panels in response to user input
		updateCallerThread = new CallerUpdater(controlRoom, settings.get("language"), 
					settings.get("country")); 
		new Thread(updateCallerThread).start();
		
		this.controlRoom = controlRoom;
		this.controlRoom.addMessageListener(this);
		countries = new CountryCodes();
		
		this.settings = settings;
		xStrings = new I18NStrings(settings.get("language"), settings.get("country"));  
		this.language = settings.get("language"); 
		this.country = settings.get("country"); 
		
		maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); 
		dbLookUpService = Executors.newFixedThreadPool(maxExecutorThreads);
		this.database = database;
		
		populateStudioExtensions();
		
		this.setLayout(new MigLayout(new LC().fillX()));
		this.addMouseListener(this);
		
		//Add Private Chat Listener
		ChatManager.getInstanceFor(connection).addIncomingListener(this);
		
		//Store a copy of all the system extensions so we can decide what calls we need
		//to handle
		systemExtensions = database.getSystemExtensions();
		
		//Setup a ringer so we can play ringing noises for calls
		if(settings.get("playRinging") != null &&
				settings.get("playRinging").equals("true")){
			
			ringer = new PhoneRinger(getAudioURL("audio/ringing.wav"));
			LOGGER.info(xStrings.getString("CallManagerPanel.ringingEnabled"));
			
		}

	}
	
	/**
	 * Adds a manual hang up listener to this object
	 * @param listener
	 */
	public void addManualHangupListener(ManualHangupListener listener){
		
		hangupListeners.add(listener);
		
	}
	
	/**
	 * Sends MANUAL call request to server
	 */
	public void addManualCall(){
		
		try {
			
			controlRoom.sendMessage(xStrings.getString("calls.manual") + "/"
					+ settings.get("nickName"));
			
		} catch (Exception e) {
			
			showWarning(xStrings.getString("CallManagerPanel.errorSendingManualCommand"));
			LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingManualCommand"));
			e.printStackTrace();
			
		}
		
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
	 * Sends a control room message telling the server to change the 
	 * activePerson for a given channel
	 * @param channel Channel to change
	 * @param activePerson Person to change it to (by record id)
	 */
	/*private void sendSetActivePersonOnChannel(String channel, int activePerson){
		
		try {
			controlRoom.sendMessage(xStrings.getString("CallManagerPanel.changeActive") +  
					"/" + channel + "/" + activePerson);  
		} catch (XMPPException e) {
			LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingChangeActive")); 
			showWarning(xStrings.getString("CallManagerPanel.errorSendingChangeActive")); 
		}
		
	}*/
	
	/**
	 * Sends an UPDATE command to the control room
	 */
	public void sendUpdateRequest(){
		
		try {
			controlRoom.sendMessage(xStrings.getString("CallManagerPanel.commandUpdate") +  
					"/" + settings.get("myExtension"));  
		} catch (Exception e) {
			LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingUpdateCommand")); 
			showWarning(xStrings.getString("CallManagerPanel.errorSendingUpdateCommand")); 
		}
		
	}
	
	/**
	 * Internal method, reads studioExtensions from db and separates them into a hashmap
	 * [extension] => [name]
	 */
	private void populateStudioExtensions(){
		
		//Parse Studio Extensions
		if(settings.get("studioExtensions") != null){ 
			
			if(settings.get("studioExtensions").contains(",")){  
				
				String[] studios = settings.get("studioExtensions").split(",");  
				
				for(int i = 0; i < studios.length; i++){
				
					if(studios[i].contains("=>")){ 
						
						String[] temp = studios[i].split("=>"); 
						
						if(temp.length == 2)
							studioExtensions.put(temp[0], temp[1]);
						
					}
					
				}
				
			}else if(settings.get("studioExtensions").contains("=>")){  
			
				String[] temp = settings.get("studioExtensions").split("=>");  
				
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
		
		/* If Ringing, add to ringing channels array
		 * We also have an edge case where connected calls to our phone can be 
		 * created as a skeleton.  Just in case, we'll make sure we're not 
		 * ringing if this happens
		 */
		if(ringer != null){
			
			if(mode == CallInfoPanel.MODE_RINGING)
				startRinging(channelID);
			else if(mode == CallInfoPanel.MODE_ANSWERED)
				stopRinging(channelID);
			
		}
					
		String location = null;
		LOGGER.info(xStrings.getString("CallManagerPanel.createSkeletonCallPanel") + 
				phoneNumber + "/" + channelID + "/" + mode);  
		
		if(phoneNumber.equals(xStrings.getString("CallManagerPanel.numberWithHeld"))) 
			location = xStrings.getString("CallManagerPanel.locationUnknown"); 
		else if(phoneNumber.length() < 6)
			location = xStrings.getString(
					"CallManagerPanel.callLocationInternal"); 
		else if(phoneNumber.length() < 8)
			location = settings.get("callLocal"); 
		else if(phoneNumber.length() < 12)
			location = settings.get("callNational"); 
		else//Lookup by phone number
			location = countries.getCountryNameByPhone(phoneNumber);
		
		if(location == null)
			location = xStrings.getString("CallManagerPanel.locationUnknown"); 
		
		//timeZoneOffset, required if the timezone is a part hour +/- from UTC so that 
		//for example, the call panel timer starts at 00:30 rather than 00:00
		int timezoneOffset = 0;
		if(settings.get("timezoneHourOffset") != null) 
			try{
				timezoneOffset = Integer.parseInt(settings.get("timezoneHourOffset")); 
			}catch(NumberFormatException e){
				LOGGER.warning(xStrings.getString(
						"CallManagerPanel.logErrorParsingTimezoneOffset") + 
						settings.get("timezoneHourOffset")); 
			}
		
		boolean canTake = false;
		
		if(settings.containsKey("canTakeCall") && settings.get("canTakeCall").equals("true"))   
			canTake = true;
		
		CallInfoPanel call = new CallInfoPanel(settings.get("language"),  
				settings.get("country"),  
				xStrings.getString("CallManagerPanel.callerUnknown"), 
				location, "", CallInfoPanel.ALERT_OK, channelID,  
				dropMode, canTake, controlRoom, settings.get("myExtension"),
				settings.get("nickName"), timezoneOffset, null);
		
		//Set the creation time as required
		if(creationTime != -1)
			call.setCallCreationTime(creationTime);
		
		//Lookup ConnectedTo if we have an entry in the userExtensions map swap to this name
		String friendlyConnected = ""; 
		
		if(userExtensions.get(connectedTo) != null)
			friendlyConnected = userExtensions.get(connectedTo);
		
		switch(mode){
		
			case CallInfoPanel.MODE_RINGING:
				call.setRinging(friendlyConnected, true);
				break;
			case CallInfoPanel.MODE_RINGING_ME:
				call.setRingingMe(phoneNumber, true);//FIX Outgoing Call needs dialling number here
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
				
				add(addMe, "grow, wrap"); 
				
			}
			
		});
		
		//Removed Endpoint to reflect new Server behaviour WMM 23/08/2014
		//Spawn thread to populate details, if updateme not null then we need to transfer
		//info from the original call channel
		//if(updateMe != null)
		//	dbLookUpService.execute(
		//			new InfoPanelPopulator(database, call, phoneNumber, 
		//					updateMe.callerChannel, location));
		//else
		
			dbLookUpService.execute(
				new InfoPanelPopulator(database, call, phoneNumber, channelID, 
						location));
		
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
	public void processMessage(Message message) {
		//TODO Break this out into own processing thread/class for readability
		//TODO Encapsulate String.split into PhoneMessage class (getChannel etc)
		String from = message.getFrom().toString();
		
		if(from.contains("/")) 
			from = from.split("/")[1]; 
		
		if(!from.equals(controlRoom.getNickname().toString())){//If the message didn't come from me 
			
			//React to commands thread all of this if performance is a problem
			LOGGER.info(xStrings.getString("CallManager.receivedMessage") + 
					message.getBody()); 
			
			String[] command = message.getBody().split("/"); 
			/*
			 * Current Control Messages:
			 * SEE Wiki
			 */
			
			//RINGING - Can Ignore
			
			//CALL - Entry point to handler
			if(command.length == 4 || command.length == 5){
				
				//Creation time here if command.length = 5
				long creationTime = -1;
				
				if(command.length == 5)
					creationTime = getCreationTime(command[4]);
				
				if(command[0].equals(xStrings.getString("CallManagerPanel.callRingingFrom"))){
					//CALL/FROM/TO/CHANNEL
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
							int mode = CallInfoPanel.MODE_RINGING;
							
							if(isMyPhone(command[1])){
								
								mode = CallInfoPanel.MODE_RINGING_ME;
								
								if(isOnAirQueue(command[2]))
									mode = CallInfoPanel.MODE_QUEUED_ME;
								
							}else if(isOnAirQueue(command[2]))
								mode = CallInfoPanel.MODE_QUEUED;
								
							createSkeletonCallInfoPanel(command[1], command[3], 
									mode, command[2], creationTime);
								
							callPanels.get(command[3]).setOriginator(command[1]);
							
						}else if(systemExtensions.contains(command[1]) && 
								!systemExtensions.contains(command[2])){
							
							/* Call from our system to someone else
							 * CALL/1234/4444444/1396477192.139
						     */
							int mode = CallInfoPanel.MODE_ANSWERED_ELSEWHERE;
							
							if(isMyPhone(command[1]))//Internal call to someone from me
								mode = CallInfoPanel.MODE_RINGING_ME;
							
							createSkeletonCallInfoPanel(command[2], command[3], 
								mode, command[1], 
								creationTime);
						
							callPanels.get(command[3]).setOriginator(command[1]);
							
						}else if(!systemExtensions.contains(command[1]) && 
								systemExtensions.contains(command[2])){
							
							//Outside call coming in
							if(isIncomingQueue(command[2])){
								
								//Outside call coming into a queue as normal
								createIncomingCall(command, creationTime);
	
							}else if(isOnAirQueue(command[2])){
								
								//Outside call coming into a on air queue as normal
								
								/* TODO ENDPOINT Removed, check this functions as intended
								 * After placing a call and transferring the endpoint, a new channel
								 * is created and all the record information is lost as the channel
								 * is different.
								 * 
								 * We need to check the endpoint records to see if we're expecting
								 * a channel change and then point the record to the original call
								 * channel
								 */
								/*if(endPoints.containsKey(command[1])){
									
									EndPointRecord updateMe = endPoints.get(command[1]);
									endPoints.remove(command[2]);
									
									if(isMyPhone(command[2]))
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_QUEUED_ME, null, creationTime, updateMe);
									else
										createSkeletonCallInfoPanel(command[1], command[3], 
												CallInfoPanel.MODE_QUEUED, null, creationTime, updateMe);
									//TODO Creation time taken from original channel at point of transferendpoint?
									
								}else{*/
									
									//TODO Should we check for MODE_QUEUED_ME here?
									createSkeletonCallInfoPanel(command[1], command[3], 
											CallInfoPanel.MODE_QUEUED, null, creationTime);
									
								//}
								
								//Set Number and Queue Badge
								callPanels.get(command[3]).setOriginator(command[1]);
								
								if(settings.get("queue_" + command[2] + "_icon") != null)  
									callPanels.get(command[3]).getIconPanel().setBadgeIcon(settings.get("queue_" + command[2] + "_icon"));  
								
								
							}else if(!command[1].equals(xStrings.getString("CallManagerPanel.callSystemUnknown"))){ 
								
								//Outside call coming direct to a phone TODO wasn't this UNKNOWN removed from server?
								createSkeletonCallInfoPanel(command[1], command[3], 
										CallInfoPanel.MODE_RINGING, command[2], creationTime);
								callPanels.get(command[3]).setOriginator(command[1]);
	
							}
															
						}
						
					}
					
				}else if(command[0].equals(xStrings.getString("CallManagerPanel.callQueued"))){ 
				
					//Call Added to QUEUE read queue number and act accordingly
					if(command[1].equals(settings.get("incomingQueueNumber"))){ 
						
						//IncomingQueue
						LOGGER.info(xStrings.getString("CallManagerPanel.CallIncomingQueue")); 
						
						/* 
						 * Normally handled by CALL however depending on the trunk a CALL may have the following format:
						 * CALL/person's phone number/trunk phone number that the person called/channel
						 * 
						 * In this case, CALL does not handle the call creation so we can do it here instead
						 *
						 * If channel not exists create call same as CALL outside to us
						 *  
						 */
						if(callPanels.get(command[3]) == null){
							
							/* Normal command array at this point looks like this:
							 * QUEUE/4000/012341523456/1452171951.10
							 * 
							 * In order for creation to lookup correctly we need to swap
							 * to the CALL format which is this:
							 * CALL/012345123456/441211231234/1452171951.10
							 */
							String[] temp = new String[4];
							temp[0] = xStrings.getString("CallManagerPanel.callRingingFrom"); 
							temp[1] = command[2];
							temp[2] = command[1];
							temp[3] = command[3];
							
							createIncomingCall(temp, creationTime);
	
						}
						
					}else if(command[1].equals(settings.get("onAirQueueNumber"))){ 
						
						LOGGER.info(xStrings.getString("CallManagerPanel.CallOnAirQueue")); 
						
						//On Air Queue - This is a call going into the ready for on air / studio queue 4001
						if(callPanels.get(command[3]) != null){
							
							//Already in our list so update
							if(isMyPhone(command[2])){
								callPanels.get(command[3]).setQueuedMe(true);
								LOGGER.info(xStrings.getString("CallManagerPanel.setQueueMeMode")); 
							}else{
								callPanels.get(command[3]).setQueued(true);
								LOGGER.info(xStrings.getString("CallManagerPanel.setQueueMode")); 
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
						xStrings.getString("CallManagerPanel.callConnected"))){ 
					
					//Turn off ringing for this channel
					if(ringer != null)
						stopRinging(command[3]);
					
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
							
							/* BUG FIX: Forgot to notify listeners that we'd answered something
							 * This only occurs when we dial out
							 */
							//notifyListeners(callPanels.get(command[3]));
							/* TODO Do we still need this?
							 * Seems logically incorrect to notify a listener that our own call has
							 * been answered to ourselves */
							
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
							
							/* WMM 23/08/2014 Server will parse out outgoing callerid, probably
							 * don't need this anymore TODO
							 * Here we arrive in an odd state because neither number
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
								
								/* BUG FIX: Forgot to notify listeners that we'd answered something
								 * This only occurs when we dial out
								 */
								notifyListeners(callPanels.get(command[3]));
								
							}else{
								//ANSWERED_ELSEWHERE
								callPanels.get(command[3]).setAnsweredElseWhere(command[2], true);
							}
							
						}
						
					}else{
						
						//Not exists so check details in case something slipped through
						if(!isMyPhone(command[1]) && !isMyPhone(command[2])){
							//TODO
							//This isn't us so someone connected to someone else
							//Check if we're connected to someone who is monitored by this program
							if(systemExtensions.contains(command[2])){
								
								/* If we have a panel that was spawned by CALL
								 * and it was from command[2] then drop the original
								 * panel 
								 */
								checkDialledCall(command[1]);
								
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
							 * on another channel and if so we'll need to change the channelID
							 * otherwise you lose conversation and call type as soon as the
							 * call is put on hold under its own channelID
							 */
							
							if(!isAlreadyConnected(command[1])){
								
								/* Don't have a panel for this AND caller is from an
								 * internal or external phone
								 */
								createSkeletonCallInfoPanel(command[1], command[3],
										CallInfoPanel.MODE_ANSWERED, null, creationTime);
							
								notifyListeners(callPanels.get(command[3]));
								
							}else{
								
								/* Need to drop original channel to reflect new
								 * server behaviour 23/08/2014 WMM */
								//CONNECTED/01234567890/5103/1408832327.787
								String oldChannelID = getAlreadyConnectedChannel(command[1]);
								
								if(callPanels.get(oldChannelID) != null)
									removePanel(oldChannelID);
								
								//Create new panel based on old outgoing call
								createSkeletonCallInfoPanel(command[1], command[3],
										CallInfoPanel.MODE_ANSWERED, command[2], creationTime);
							
								notifyListeners(callPanels.get(command[3]));
								
								/*Removed Old behaviour 23/08/2014
								 * String oldChannelID = getAlreadyConnectedChannel(command[1]);
								 * 
								 * If already connected is an older channel then we 
								 * dialled this channel so need to change the mode to 
								 * an answered_me instead of generic answered
								 * 
								 *  BUG FIX can't use doubles as it won't pick up
								 *  "1.407...E9" etc from callPanels
								 *
								
								BigDecimal oldChannel = new BigDecimal(oldChannelID);
								BigDecimal newChannel = new BigDecimal(command[3]);
								
								CallInfoPanel temp = callPanels.get(oldChannelID);
								
								if(oldChannel.compareTo(newChannel) == -1 &&
										temp != null){
									
									/* Get the active person of the old channel ID because
									 * When we swap to the new channel, the active person
									 * won't be set and we'll lose info (and nullpointer)
									 *//*
									int activePerson = callPanels.get(oldChannelID).getPhoneCallRecord().getActivePerson().id;
									sendSetActivePersonOnChannel(command[3], activePerson);
									
									callPanels.remove(temp.getChannelID());
									temp.changeChannelID(command[3]);
									callPanels.put(command[3], temp);
									temp.setAnsweredMe(command[1], false);
									notifyListeners(temp);
									
								}*/
								
							}
							
							//Caller is me and the receiver isn't
						}else if(isMyPhone(command[1]) && !isMyPhone(command[2])){
							
							/* If we get here by normal control messages then this should 
							 * be ignored
							 * TODO We can't ignore this, if you dial out then reload program
							 * you won't get any panel for this call
							 */
							
						}
						
					}
					//UPDATEFIELD
				}else if(command[0].equals(xStrings.getString(
						"CallManagerPanel.commandUpdateField"))){ 
					//		0			1			2			3
					//UPDATEFIELD/field mapping/channel id/field value
					/*if(command[3].equals("<CLEAR>")) 
						command[3] = ""; */
					//Panel can be gone when update happens, check for exists first
					if(callPanels.get(command[2]) != null){
						
						if(command[1].equals("name")){ 
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),   
									false);
							callPanels.get(command[2]).setCallerName(
									command[3].replace("^^%%$$", "/"), false);  
						}else if(command[1].equals("location")){ 
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),   
									false);
							callPanels.get(command[2]).setCallerLocation(command[3], false);
						}else if(command[1].equals("conversation")){ 
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),   
									false);
							callPanels.get(command[2]).setConversation(command[3], false);
						}else if(command[1].equals("alert")){ 
							
							String[] temp = command[3].split("@@"); 
							
							try{
								int level = Integer.parseInt(temp[1]);
								callPanels.get(command[2]).setAlertLevel(null, level, false);
							}catch(NumberFormatException e){
								callPanels.get(command[2]).setAlertLevel(null,
										temp[1].replace("+", "/"), false);  
							}
							
							callPanels.get(command[2]).setPhoneCallField("alert", 
									temp[0], false);
							
						}else if(command[1].equals("calltype")){ 
							
							//Split the image from the value:
							String[] temp = command[3].replace("+", "/").split("@@"); 
							callPanels.get(command[2]).setBadgeIcon(null,
										temp[1], false);  
							//Set the record call type
							callPanels.get(command[2]).setPhoneCallField("calltype",  
									temp[0], false);
						}else{
							
							//Custom Field
							callPanels.get(command[2]).setPhoneCallField(command[1], 
									command[3].replace("^^%%$$", "/"),   
									false);
							
						}
					
					}
				}else if(command.length == 4 &&
						command[0].equals(xStrings.getString("CallManagerPanel.endPoint"))){ 
					
					//Store endpoint and extension here then act on it in subsequent queue message
					//ENDPOINT/1397214684.388/1397214684.391/9901234567890
					//ENDPOINT/dialler ch    /receiver ch   /receiver clid
					//endPoints.put(command[3], new EndPointRecord(command[1], command[2], command[3]));
					//TODO Check removal is OK
					
				}
				
			}else if(command.length == 3 && 
					command[0].startsWith(xStrings.getString("CallManagerPanel.callHangup"))){ 
				
				//Call Hangup received
				LOGGER.info(
						xStrings.getString("CallManagerPanel.removingPanelHangupReceived") +  
								command[2]);
				
				//Turn off ringing for this channel (in case we get hang up with no answer)
				if(ringer != null)
					stopRinging(command[2]);
				
				//Check to see if we have the panel in the list and remove it
				if(callPanels.get(command[2]) != null){
					
					//If this is our manual call being ended re-enable the add call button
					if(command[2].startsWith("M_") &&
							callPanels.get(command[2]).getModeWhenClicked() ==
							CallInfoPanel.MODE_ANSWERED)
						notifyManualListeners();
	
					removePanel(command[2]);
					
				}
				
			}else if(command.length == 3 &&
					command[0].equals(xStrings.getString("CallManagerPanel.callTransfer"))){ 
				
				//Transfer from another user, add their name to our extensions list
				userExtensions.put(command[2], from);
				
				//Stop ringing it transferred to someone else
				if(ringer != null)
					stopRinging(command[1]);
	
			}else if(command.length == 2 &&
					command[0].equals(xStrings.getString("CallManagerPanel.callLocked"))){ 
				
				/* Notification that call was locked wait 3 seconds and if panel is 
				 * still clicked reset it to normal mode
				 */
				if(callPanels.get(command[1]) != null){
					
					new Thread(new LockedWaitThread(callPanels.get(command[1]))).start();
					
				}//If it doesn't exist then ignore it, the call probably ended
				
			}else if(command.length == 2 && command[0].equals(
					xStrings.getString("CallManagerPanel.changeFailed"))){ 
			
				showWarning(
						xStrings.getString("CallManagerPanel.errorChangingPerson")); 
			
			}else if(command.length == 3 && command[0].equals(
					xStrings.getString("CallManagerPanel.changed"))){ 
				
				//CHANGED
				//Update CallInfoPanel, CallInputPanel.  CallLogPanel has its own listener
				//CallInputPanel to be notified via notifyListeners method
				if(callPanels.get(command[1]) != null){//Check we actually have this panel
				
					callPanels.get(command[1]).changeActivePerson(
							new Person(Integer.parseInt(command[2]), 
									language, country, 
									database.getReadConnection()));
					
					//Notify listeners
					notifyListeners(callPanels.get(command[1]));
					
				}
				
			}else if(command.length == 3 && command[0].equals(
					xStrings.getString("CallManagerPanel.FAILED"))){ 
				
				/* Call failed to transfer to us
				 * FAILED/Channel/Failure Code
				 * 0: Extension Online/Ready
				 * 1: Extension On a call
				 * 4: Extension Off line
				 *-1: Extension does not exist
				 */
				LOGGER.info(xStrings.getString(
						"CallManagerPanel.clientPhoneError")); 
				
				//Reset CallInfoPanel
				if(!command[1].equals("NA") &&  
						callPanels.get(command[1]) != null)
					callPanels.get(command[1]).reset();
				
				int errorCode = Integer.parseInt(command[2]);
				
				if(errorCode == 4)
					showWarning(xStrings.getString(
							"CallManagerPanel.errorExtensionOffline")); 
				else if(errorCode == -1)
					showWarning(xStrings.getString(
							"CallManagerPanel.errorExtensionDoesNotExist")); 
				else if(errorCode != 1)
					showWarning(xStrings.getString(
							"CallManagerPanel.errorExtension") + errorCode); 
				
			}else if(command.length == 2 && 
					command[0].equals(xStrings.getString(
							"CallManagerPanel.channelUpdate"))){ 
				
				//Update given panel looking for X in callhistory
				//Then redo call answered if necessary
				if(callPanels.containsKey(command[1])){
					
					CallInfoPanel panel = callPanels.get(command[1]);
					
					dbLookUpService.execute(
							new InfoPanelPopulator(database, 
									panel, 
									panel.getNumber(), 
									command[1], 
									panel.getCallLocation()));
					
					if(panel.getMode() == CallInfoPanel.MODE_ANSWERED)
						notifyListeners(panel);
					
				}
				
			}else if(command.length == 3 &&
					command[0].equals(xStrings.getString(
							"calls.manual"))){
				
				/* Manual Call Created
				 * If [2] = our name this is manual for us
				 * else manual for someone else just display answered by
				 * them
				 */
				//TODO
				//TODO
				//TODO
				int mode = CallInfoPanel.MODE_ANSWERED_ELSEWHERE;
				
				if(command[2].equals(settings.get("nickName")))
					mode = CallInfoPanel.MODE_ANSWERED_ME;
				
				//MANUAL/M_1458563910616/waynemerricks
				//String phoneNumber, String channelID, int mode,
				//String connectedTo, long creationTime
				createSkeletonCallInfoPanel(
						xStrings.getString("CallManagerPanel.callSystemUnknown"),
						command[1], mode, command[2], new Date().getTime());
					
				callPanels.get(command[1]).setOriginator(command[2]);
				
				//Set the panel to answered by us and notify listeners
				if(mode == CallInfoPanel.MODE_ANSWERED_ME){
	
					callPanels.get(command[1]).setAnswered(true);
					notifyListeners(callPanels.get(command[1]));
	
				}
	
			}
			
		}
	
	}
	
	/**
	 * Checks all the panels to see if we dialled a call with this number
	 * If it exists then remove the dialler end as we will be creating the
	 * receiver end now we're connected
	 * @param callerID
	 */
	private void checkDialledCall(String callerID) {
		
		if(callPanels.size() > 0){
			
			Iterator<CallInfoPanel> calls = callPanels.values().iterator();
			boolean done = false;
			
			while(calls.hasNext() && !done){
				
				CallInfoPanel panel = calls.next();
				
				if(panel.getNumber().equals(callerID)){
					
					done = true;
					//We found the dialler end of this connected so remove
					//the panel (because we're about to create the receiver end)
					removePanel(panel.getChannelID());
					
				}
				
			}
			
		}
		
	}

	/**
	 * Logs a warning message and displays friendly message to user
	 * @param friendlyErrorMessage
	 */
	private void showWarning(String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("CallManagerPanel.logErrorPrefix") 
				+ friendlyErrorMessage); 
		
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, 
				xStrings.getString("CallManagerPanel.errorBoxTitle"), 
				JOptionPane.WARNING_MESSAGE); 
		LOGGER.warning(friendlyErrorMessage);
		
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
			
			LOGGER.warning(xStrings.getString("CallManagerPanel.errorParsingCreationTime")); 
			
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
					
					if(panel.getConnectedTo().equals(number))
						channelID = panel.getChannelID();
					
					if(channelID == null)//We didn't find so try a friendly name
						Integer.parseInt(panel.getConnectedTo());//FIX Don't parse the number until you've tested equality
					
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
					
					if(panel.getConnectedTo().equals(number))
						isConnected = true;
					
					if(!isConnected)//We didn't find so try a friendly name
						Integer.parseInt(panel.getConnectedTo());//FIX don't fail the parse before you've checked it!
					
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
		
		if(settings.get("onAirQueueNumber").equals(number)) 
			isOnAir = true;
		
		return isOnAir;
		
	}
	
	/**
	 * Gets the audio from a relative path and returns a URL for reference
	 * @param path path where audio resides
	 * @return URL pointing to the audio
	 */
	private URL getAudioURL(String path){

		URL audioURL = null;

		if(path != null){

			audioURL = getClass().getResource(path);

			if(audioURL == null)
				LOGGER.warning(xStrings.getString("CallManagerPanel.logLoadAudioError") + path); 

		}

		return audioURL;

	}

	/**
	 * TODO
	 * @param command
	 * @param creationTime
	 */
	private void createIncomingCall(String[] command, long creationTime){
		
		createSkeletonCallInfoPanel(command[1], command[3], 
				CallInfoPanel.MODE_RINGING, null, creationTime);
		callPanels.get(command[3]).setOriginator(command[1]);
		
		if(settings.get("queue_" + command[2] + "_icon") != null)  
			callPanels.get(command[3]).getIconPanel().setBadgeIcon(settings.get("queue_" + command[2] + "_icon"));  

	}
	
	/**
	 * Checks if the given number is the on air queue
	 * @param number
	 * @return true if it is the on air queue
	 */
	private boolean isIncomingQueue(String number){
	
		boolean isIncoming = false;
		
		if(settings.get("incomingQueueNumber").equals(number)) 
			isIncoming = true;
		
		return isIncoming;
		
	}
	
	/**
	 * Notifies our answer listeners that they can re-enable manual call
	 * functionality
	 */
	private void notifyManualListeners(){

		for(int i = 0; i < answerListeners.size(); i++)
			answerListeners.get(i).manualCallEnded();

	}

	/**
	 * Notifies any object listening to this CallManagerPanel
	 * Primarily used to notify call input panel we've answered something
	 * @param callInfoPanel
	 */
	private void notifyListeners(CallInfoPanel callInfoPanel) {
		
		// Need to notify any listeners of this CallManagerPanel
		
		//Stop Ringing, we've answered a call, first remove from ringingChannels array
		if(ringer != null){
			
			ringingChannels.remove(callInfoPanel.getChannelID());
			stopRinging();
			
		}
		
		if(answerListeners.size() < 1){
			
			/* Usually only happens on startup when the CallInfoPanel is not 
			 * quite finished being created.  So we store this as something
			 * we'll notify later
			 */
			LOGGER.info(xStrings.getString("CallManagerPanel.storeForNotifyListeners")); 
			storedAnsweredPanel = callInfoPanel;
			
		}else{
			
			LOGGER.info(xStrings.getString("CallManagerPanel.notifyListeners") + 
					callInfoPanel.getChannelID()); 
			
			/* When there is an outbound call that we've answered, we will be passing
			 * a call answered message from this panel but the active person won't have 
			 * been set yet because the updaterThread may not have finished yet
			 * 
			 * So We should wait and pass answered once the update thread has finished
			 * 
			 * We can be lazy and just introduce a 500ms wait for a max of 3 times
			 * 
			 * Ideally we'd have a call back from the updater thread TODO
			 */
			int waitForActivePerson = 0;
			PhoneCall record = callInfoPanel.getPhoneCallRecord();
			
			while(record == null && waitForActivePerson < 3){
				
				LOGGER.info(xStrings.getString(
						"CallManagerPanel.waitingForActivePerson") +  
						waitForActivePerson);
				try {
					
					Thread.sleep(500L);
					
				} catch (InterruptedException e) {
					
					showWarning(xStrings.getString(
							"CallManagerPanel.waitForActiveInterrupted")); 
					waitForActivePerson = 3;
					e.printStackTrace();
					
				}
				
				record = callInfoPanel.getPhoneCallRecord();
				waitForActivePerson++;
				
			}
			
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
		
		LOGGER.info(xStrings.getString("CallManagerPanel.addAnswerListener")); 
		
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
		boolean studio = (settings.get("isStudio") != null 
				&& settings.get("isStudio").equals("true"));  
		
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
	 * Helper method returns channelID if we're on a call with someone
	 * @return null if not on a call, channelID if we are on a call
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
	 * Sets the client to play ringing.  Currently only plays ringing audio
	 * but you could use this as an entry to flash/shake the client or whatever
	 * else you want to know
	 */
	private void playRinging(){

		if(ringer != null && !ringer.isRinging()){
			
			ringer.ring();
			LOGGER.info(xStrings.getString("CallManagerPanel.ringingStarted"));
			
		}

	}

	/**
	 * Sets the client to stop ringing.  Currently only stops audio ringing but
	 * could be used as entry to stop other actions
	 */
	private void stopRinging(){

		if(ringer != null){
			
			if(ringer.isRinging()){
				
				ringer.stop();
				LOGGER.info(xStrings.getString("CallManagerPanel.ringingStopped"));
				
			}else{
				
				//If we're not on a call and there are more ringing channels, play ringing
				if(isOnCall() == null && ringingChannels.size() > 0)
					playRinging();
					
				
			}
			
		}

	}
	
	/**
	 * Helper method to remove given channel from ringing list and stop ringing
	 * if appropriate
	 * @param channelID Channel to remove from ringing list
	 */
	private void stopRinging(String channelID){
		
		if(ringer != null){
			
			ringingChannels.remove(channelID);
			stopRinging();
			
		}
	}
	
	/**
	 * Helper method to add given channel to ringing list and start ringing
	 * if appropriate
	 * @param channelID Channel to add to ringing list
	 */
	private void startRinging(String channelID){
		
		if(ringer != null){
			
			ringingChannels.add(channelID);
			
			if(isOnCall() == null)
				playRinging();
			
		}
		
	}

	/**
	 * Answers the oldest call that is ringing
	 * unless you're a studio in which case it grabs oldest studio ready call
	 */
	public void answerNext() {
		
		if(!callPanels.isEmpty() && settings.get("myExtension") != null){ 
			
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
		
		if(settings.get("myExtension") != null && 
				settings.get("myExtension").equals(extension)) 
			myPhone = true;
		
		return myPhone;
		
	}
	
	/**
	 * Checks if we're flagged as a studio in the settings
	 * @return
	 */
	private boolean isStudio(){
		
		boolean studio = false;
		
		if(settings.get("isStudio") != null && settings.get("isStudio").equals("true")){   
			
			studio = true;
			
		}
		
		return studio;
		
	}
	
	/**
	 * Answers a random call that is ringing
	 * Unless you're a studio in which case it grabs random studio ready call
	 */
	public void answerRandom() {
		
		if(!callPanels.isEmpty() && settings.get("myExtension") != null){ 
			
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
		boolean studio = (settings.get("isStudio") != null 
				&& settings.get("isStudio").equals("true"));  
		
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
		
		if(dialler == null && settings.get("myExtension") != null){ 
			
			Component parent = this.getParent();
			Client owner = null;
			
			while(parent != null && !(parent instanceof Client))
				parent = parent.getParent();
			
			if(parent != null)
				owner = (Client)parent;
			
			dialler = new DialPanel(owner, xStrings.getString("DialPanel.title"), 
					settings.get("language"), settings.get("country"), settings.get("outsideCallPrefix"));   
			dialler.addDialListener(this);
			
			dialler.setVisible(true);
			
		}else if(dialler != null){//else if we're open, close the window
			
			if(dialler.isVisible()){
				dialler.setVisible(false);
				dialler.dispose();
				dialler = null;
			}else
				dialler.setVisible(true);
			
		}
		
	}

	/**
	 * DialListener entry point, this sends a message via control to call the given number
	 */
	public void dial(String number){
	
		if(number.length() > 0)
			try {
				controlRoom.sendMessage(xStrings.getString("CallManagerPanel.commandDial") +  
						"/" + number + "/" + settings.get("myExtension"));   
			} catch (Exception e) {
				LOGGER.severe(xStrings.getString("CallManagerPanel.errorSendingDialCommand")); 
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
	
	/* CHAT MANAGER LISTENER -> IncomingChatMessageListener Smack 4 */
	@Override
	public void newIncomingMessage(EntityBareJid jid, Message message, Chat chat) {
		
		//Can pass this on to the processPacket method as part of normal message handling
		LOGGER.info(xStrings.getString("CallManagerPanel.receivedPrivateMessage") 
				+ message.getBody()); 
		
		/* We need to deal with connected here only when:
		 * Caller = Us, Receiver = Someone else
		 * Command = CONNECTED
		 * This is not strictly true as at this stage there is no way of knowing
		 * whether we phoned out or they phoned us!
		 * 
		 * Judgement Call: 95% of the time they will have phoned us so we'll set
		 * it to that.
		 * 
		 * BUG? Most of the time the asterisk server will show the call originator
		 * as the first channel e.g.:
		 * 
		 * CONNECTED/CALL ORIGINATOR/CALL ANSWERER/CHANNEL ORIGINATOR/CREATION TIME
		 * 
		 * However sometimes this gets flipped around and causes havoc so we need to
		 * deal with that here:
		 * 
		 * CONNECTED/CALL ANSWERER/CALL ORIGINATOR/CHANNEL ??/CREATION TIME
		 */
		String[] command = message.getBody().split("/"); 
		
		if(command.length == 5 && command[0].equals(
				xStrings.getString("CallManagerPanel.callConnected")) 
				&& callPanels.get(command[3]) != null
				&& isMyPhone(command[1]) && !isMyPhone(command[2])){ 
			
			long creationTime = getCreationTime(command[4]);
			
			createSkeletonCallInfoPanel(command[2], command[3],
					CallInfoPanel.MODE_ANSWERED, null, creationTime);
		
			notifyListeners(callPanels.get(command[3]));
			
		}else
			processMessage(message); 
		
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
