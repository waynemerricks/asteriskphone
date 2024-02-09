package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueue;
import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
import org.asteriskjava.live.CallerId;
import org.asteriskjava.live.ChannelState;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.Extension;
import org.asteriskjava.live.LiveException;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.OriginateCallback;
import org.asteriskjava.live.internal.AsteriskAgentImpl;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.ExtensionStateAction;
import org.asteriskjava.manager.response.ExtensionStateResponse;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.phonebox.database.ActivePersonChanger;
import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.database.PersonChanger;
import com.thevoiceasia.phonebox.database.RecordUpdater;
import com.thevoiceasia.phonebox.records.OutgoingCall;
import com.thevoiceasia.phonebox.records.PhoneCall;
import com.thevoiceasia.phonebox.records.TrackDial;

public class AsteriskManager implements AsteriskServerListener, PropertyChangeListener, OriginateCallback, PacketListener, MessageListener {

	//STATICS
	private static final Logger AST_LOGGER = Logger.getLogger("org.asteriskjava"); 
	private static final Logger LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.asterisk"); 
	private static final String HANGUP_NORMAL = "Normal Clearing"; 
	private static final String HANGUP_UNACCEPTABLE = "Channel unacceptable"; 
	private static final String HANGUP_OFFLINE = "Subscriber absent"; 
	private static final String HANGUP_USER_BUSY = "User busy"; 
	private static final String HANGUP_ANSWERED_ELSEWHERE = "Answered elsewhere"; 
	private static final String SIP_PREFIX = "Local/"; 
	private static final int DEFAULT_PRIORITY = 1;
	private static final long DEFAULT_TIMEOUT = 30000L; //Default time out if database record is null
	private static final int DEFAULT_EXECUTOR_THREADS = 4; //Default threads if database is null
	private static final long DEFAULT_CHANNEL_LOCK = 3000; //Default time for the channel lock to be enforced if db is null

	//CLASS VARS
	private AsteriskServer asteriskServer;
	private ArrayList<AsteriskServer> trunkServers = new ArrayList<AsteriskServer>();//Holds other servers for extension lookups
	private HashMap<String, AsteriskChannel> activeChannels = new HashMap<String, AsteriskChannel>();
	private HashMap<String, Long> lockedChannels = new HashMap<String, Long>();
	private I18NStrings xStrings;
	private String autoAnswerContext, defaultContext, contextMacroAuto, queueNumber, dialPrefix;
	private long defaultTimeOut, channelLockTimeOut;
	private MultiUserChat controlRoom;
	private DatabaseManager databaseManager;
	private HashSet<String> systemExtensions = new HashSet<String>();
	private HashMap<String, String> settings;
	private HashMap<String, String> calls = new HashMap<String, String>(); //HashMap to store dialled calls in progress <fromNumber or channel, toWithoutPrefix>
	private HashMap<String, OutgoingCall> ringingExternal = new HashMap<String, OutgoingCall>(); //For external outbound calls channel => outbound
	private HashMap<String, String> expectedInQueue = new HashMap<String, String>(); //For calls we know will go to queue with a new channel, callerid => old/current channel
	private boolean startup = true; //Flag that we're starting up so ignore messages
	
	/* We need to spawn threads for event response with db lookups, in order to guard against
	 * craziness, we'll use the ExecutorService to have X threads available to use (set via
	 * DB threadPoolMax
	 */
	private ExecutorService dbLookUpService; 
	private int maxExecutorThreads;
	
	/**
	 * Creates a new Asterisk Manager instance that handles events and sends commands via XMPP
	 * to any clients listening
	 * @param databaseManager used for settings and saving records to DB
	 * @param controlRoom XMPP Chat Room to use for control messages
	 */
	public AsteriskManager(DatabaseManager databaseManager, MultiUserChat controlRoom){
		
		this.databaseManager = databaseManager;
		settings = databaseManager.getUserSettings();
		this.autoAnswerContext = settings.get("autoAnswerContext"); 
		this.defaultContext = settings.get("defaultContext"); 
		this.contextMacroAuto = settings.get("contextMacroAuto"); 
		this.queueNumber = settings.get("onAirQueueNumber"); 
		
		//Set the dial prefix to whatever we call for an outside line or "" if null/blank
		if(settings.containsKey("outsideCallPrefix")) 
			this.dialPrefix = settings.get("outsideCallPrefix"); 
		else
			dialPrefix = ""; 
		
		if(settings.containsKey("defaultTimeOut")) 
			this.defaultTimeOut = Long.parseLong(settings.get("defaultTimeOut")); 
		else
			this.defaultTimeOut = DEFAULT_TIMEOUT;
		
		if(settings.containsKey("threadPoolMax")) 
			this.maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); 
		else
			this.maxExecutorThreads = DEFAULT_EXECUTOR_THREADS;
		
		if(settings.containsKey("channelLockTimeOut")) 
			this.channelLockTimeOut = Long.parseLong(settings.get("channelLockTimeOut")); 
		else
			this.channelLockTimeOut = DEFAULT_CHANNEL_LOCK;
		
		this.controlRoom = controlRoom; //Control Room XMPP chat
		this.controlRoom.addMessageListener(this);
		
		//Turn off AsteriskJava logger for all but SEVERE
		AST_LOGGER.setLevel(Level.SEVERE);
		
		xStrings = new I18NStrings(settings.get("language"), settings.get("country"));  
		
		asteriskServer = new DefaultAsteriskServer(settings.get("asteriskHost"),  
				settings.get("asteriskUser"), settings.get("asteriskPass"));  
		
		/* Connect to any trunk hosts we have so we can lookup extensions on 
		 * other servers */
		String trunkHost = "trunkHost";
		int i = 1;
		
		while(settings.containsKey(trunkHost + i)){
			
			AsteriskServer trunkServer = new DefaultAsteriskServer(
					settings.get("trunkHost" + i),
					settings.get("trunkUser" + i),
					settings.get("trunkPassword" + i));
			
			trunkServers.add(trunkServer);
			
			i++;
			
		}
		
		systemExtensions = databaseManager.getSystemExtensions();
		
	}
	
	/**
	 * Set the startup flag to false once you're ready for this object to process
	 * XMPP control messages
	 */
	public void startProcessingMessages(){
		
		LOGGER.info(xStrings.getString("AsteriskManager.finishedStarting")); 
		startup = false;
		
	}
	
	/**
	 * Initialises and connects to Asterisk server
	 * @throws ManagerCommunicationException
	 */
	public void connect() throws ManagerCommunicationException {
		
		asteriskServer.initialize();
		asteriskServer.addAsteriskServerListener(this);
		dbLookUpService = Executors.newFixedThreadPool(maxExecutorThreads);
		
		//Connect to any trunks we might have
		for(int i = 0; i < trunkServers.size(); i++)
			trunkServers.get(i).initialize();
		
		getChannels();
		sendMessage(xStrings.getString("AsteriskManager.XMPPServerHello")); 
		
	}
	
	/**
	 * Disconnects from Asterisk server
	 */
	public void disconnect() {
		
		asteriskServer.shutdown();
		
		//Disconnect any trunks
		for(int i = 0; i < trunkServers.size(); i++)
			trunkServers.get(i).shutdown();
		
	}

	/**
	 * Gets the active Channels, adds a property change and keeps track of them via
	 * PropertyChangeListener 
	 */
	private void getChannels(){
		
		for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
            
			LOGGER.info(xStrings.getString("AsteriskManager.startupActiveChannels") + "/" + asteriskChannel.getId());  
			asteriskChannel.addPropertyChangeListener(this);
			addActiveChannel(asteriskChannel);
			//System.out.println(asteriskChannel);
			
        }
		
	}
	
	/**
	 * Adds the given channel to the active channels list
	 * @param channel
	 */
	private synchronized void addActiveChannel(AsteriskChannel channel){
	
		activeChannels.put(channel.getId(), channel);
		//System.out.println(activeChannels.size());
		
	}
	
	/**
	 * Removes the given channel to the active channels list
	 * @param channelID
	 */
	private synchronized void removeActiveChannel(String channelID){
		
		activeChannels.remove(channelID);
		
	}
	
	/**
	 * Lists the channels and queue members on the server and sends appropriate updates
	 * to the recipient listed via private XMPP messages
	 * @param recipient XMPP recipient to send messages to
	 */
	private void sendChannelInfo(String recipient){
		
		/*
		 * LinkedChannel == null means its in a queue?
		 * AsteriskQueue.getName 3000 = incoming so set ringing
		 * AsteriskQueue.getName 3001 = on air queue so set green
		 * 
		 * Loop through AsteriskQueue.getEntries().getChannel to pick up channels 
		 * and what not.
		 */
		/* FIXME Check for removePrefix on this lot might be difficult with 
		 * outbound caller id.  This causes the problem where if a client starts
		 * up and is connected to what was an outbound call they'll get a panel
		 * thats set as "ANSWERED_ELSEWHERE" even if it is a call active on
		 * their phone.
		 */
		LOGGER.info(xStrings.getString("AsteriskManager.sendingChannelInfo") + recipient); 
		
		Vector<AsteriskChannel> orderedChannels = new Vector<AsteriskChannel>();
		
		/* Need to put channels in order, lower id = older channel, oldest -> newest */
		for(AsteriskChannel asteriskChannel : asteriskServer.getChannels()){
            
			if(orderedChannels.size() == 0 && asteriskChannel.getLinkedChannel() != null)
				orderedChannels.add(asteriskChannel);
			else if(orderedChannels.size() > 0 
					&& asteriskChannel.getLinkedChannel() != null 
					&& systemExtensions.contains(asteriskChannel.getLinkedChannel()
	            				.getCallerId().getNumber())){
				
				//Find position to insert
				boolean inserted = false;
				int i = 0;
				
				while(i < orderedChannels.size() && !inserted){
					
					double indexId = Double.parseDouble(orderedChannels.get(i).getId());
					double channelId = Double.parseDouble(asteriskChannel.getId());
					
					if(channelId < indexId){
						orderedChannels.insertElementAt(asteriskChannel, i);
						inserted = true;
					}
					
					i++;
					
				}
				
				if(!inserted)
					orderedChannels.add(asteriskChannel);
				
			}
			
        }
		
		/* Now we're sorted, we can spam the person who requested the update */
		for(int i = 0; i < orderedChannels.size(); i++){
			
			//This is one we need to deal with CONNECTED/5003/5001/1377009449.5
        	String command = xStrings.getString("AsteriskManager.callConnected") + "/"   
        			+ orderedChannels.get(i).getCallerId().getNumber() + "/"  
        			+ orderedChannels.get(i).getLinkedChannel().getCallerId().getNumber() 
        			+ "/" + orderedChannels.get(i).getId() + "/" +  
        			orderedChannels.get(i).getDateOfCreation().getTime(); 
        	
        	//System.out.println(command);
        	sendPrivateMessage(recipient, command);
			
		}
		
		for(AsteriskQueue asteriskQueue : asteriskServer.getQueues()){

			//System.out.println("QUEUE: " + asteriskQueue);
			
			for(AsteriskQueueEntry entry : asteriskQueue.getEntries()){
				
				//CALL/5003/3000/1377009449.5
				String command = xStrings.getString("AsteriskManager.callRingingFrom") + "/"   
						+ entry.getChannel().getCallerId().getNumber() + "/"  
						+ asteriskQueue.getName() + "/"  
						+ entry.getChannel().getId() + "/" +  
						entry.getChannel().getDateOfCreation().getTime();
				
				//System.out.println(command);
				sendPrivateMessage(recipient, command);
				
			}
					
        }
		
	}
	
	/**
	 * Internal method to send a message to a given user
	 * @param recipient
	 * @param message
	 */
	private void sendPrivateMessage(String recipient, String message){
	
		Chat chat = controlRoom.createPrivateChat(controlRoom.getRoom() + "/" + recipient, this); 
		
		try {
			LOGGER.info(xStrings.getString("AsteriskManager.sendingPrivateMessage") +  
					recipient + "/" + message); 
			chat.sendMessage(message);
		} catch (XMPPException e) {
			LOGGER.warning(xStrings.getString("AsteriskManager.errorSendingPrivateMessage") + 
					recipient); 
		}
		
	}
	
	
	/**
	 * Creates a call to the given number from the given number
	 * @param to
	 * @param fromNumber
	 * @param fromName
	 */
	public void createCall(String to, String fromNumber, String fromName){
		
		String toWithoutPrefix = to;
		
		if(dialPrefix.length() > 0 && to.startsWith(dialPrefix))
			toWithoutPrefix = to.substring(dialPrefix.length());
		
		LOGGER.info(xStrings.getString("AsteriskManager.loggingExternalCall") + 
				fromNumber + "/" + toWithoutPrefix);  
		calls.put(fromNumber, toWithoutPrefix);
		
		trackDial(toWithoutPrefix, fromName);//Add an entry to callhistory D so we can track who dialled stuff
		
		HashMap<String, String> vars = new HashMap<String, String>();
		asteriskServer.originateToExtensionAsync(SIP_PREFIX + fromNumber + "@" +  
				autoAnswerContext,  autoAnswerContext, to, DEFAULT_PRIORITY, defaultTimeOut, 
				new CallerId(fromName, fromNumber), vars, this);
		
	}
	
	/**
	 * Adds an entry to the call history table for dialling a number
	 * @param phoneNumber number dialled
	 * @param operator operator who did it
	 */
	private void trackDial(String phoneNumber, String operator) {
		
		dbLookUpService.execute(new TrackDial(settings.get("language"),  
				settings.get("country"), databaseManager, operator,  
				phoneNumber));
		 
	}

	/**
	 * Returns the status of the given extension in the default context
	 *
	 * @param extension Extension to query
	 * @return  0: Extension Online/Ready
	 *		    1: Extension On a call
	 *		    4: Extension Offline
	 *		   -1: Extension does not exist
	 */
	private int isExtensionOnline(String extension){
	
		int online = -1;
		
		try {
			
			ExtensionStateResponse response = (ExtensionStateResponse)
					asteriskServer.getManagerConnection().sendAction(
					new ExtensionStateAction(extension, 
							settings.get("defaultContext"))); 
			
			online = response.getStatus();
			
			/* Call failed to transfer to us
			 * FAILED/Channel/Failure Code
			 * 0: Extension Online/Ready
			 * 1: Extension On a call
			 * 4: Extension Off line
			 *-1: Extension does not exist
			 */
			int coreOnline = online;
			
			if( (online == 4 || online == -1) && trunkServers.size() > 0){
				
				//Check other trunk servers if we have any
				int i = 0;
				
				while(online != 0 && online != 1 && i < trunkServers.size()){
					
					LOGGER.info(xStrings.getString("AsteriskManager.checkingTrunkServers")
							+ extension + " (" + 
							trunkServers.get(i).getManagerConnection().getHostname()
							+ ")");
					response = (ExtensionStateResponse)
							trunkServers.get(i).getManagerConnection()
							.sendAction(
							new ExtensionStateAction(extension, 
									settings.get("defaultContext"))); 
					
					online = response.getStatus();
					
					i++;
					
				}
				
			}
			
			if(online == -1 && coreOnline == 4)
				online = 4;
			
		} catch (IllegalArgumentException | IllegalStateException | IOException
				| TimeoutException e) {
			LOGGER.severe(xStrings.getString(
					"AsteriskManager.extensionQueryFailed" + extension)); 
			e.printStackTrace();
		}
		
		return online;
		
	}
	
	/**
	 * Redirects the given channel to the given extension
	 * @param channelID Channel to redirect
	 * @param to extension to send channel to
	 */
	public void redirectCall(String channelID, String to, String from){
	
		/* Need to check to see if we're already on a call
		 * If we are then park the original calls before transferring this one
		 */
		int extensionStatus = isExtensionOnline(to);
		
		if(extensionStatus == 1)//On a call
			parkActiveCalls(to, from);
		
		if(extensionStatus == 0) {//Ready/Online
		
			// Get the channel and if its not locked, transfer it to this phone
			AsteriskChannel channel = activeChannels.get(channelID);
			
			boolean locked = isLocked(channel);
			
			if(channel != null && !locked){//null = channel not found
				
				channel.redirect(autoAnswerContext, to, DEFAULT_PRIORITY);
			
				dbLookUpService.execute(new PhoneCall(databaseManager, 
						channel.getCallerId().getNumber(), channel.getId(), this, 'A', from));
				
			}else if(locked){
				
				String message = xStrings.getString(
						"AsteriskManager.commandLocked") + "/" +   
				channel.getId();
				sendMessage(message);
				
			}
			
		}else//Extension offline/in error state so send failed message
			sendPrivateMessage(from, xStrings.getString(
					"AsteriskManager.FAILED") + "/" + channelID + "/" +
					extensionStatus);
		
	}
	
	
	
	/**
	 * Checks for any active calls to the given extension
	 * If we have an active call, it will be put in the on air queue (or should we hang up?)
	 * @param extension extension to check active calls against
	 * @param from name of person who instigated keythis transfer
	 */
	private void parkActiveCalls(String extension, String from) {
		
		Iterator<String> channels = activeChannels.keySet().iterator();
		
		while(channels.hasNext()){
			
			AsteriskChannel linked = activeChannels.get(channels.next()).getLinkedChannel();
			
			if(linked != null){
				
				/* For some reason, even though I got the linked channel; the linked
				 * channel of this new channel is what I'm actually looking for.
				 * Answers on a post card :\ 
				 */
				AsteriskChannel daisyLinked = linked.getLinkedChannel();
				
				if(daisyLinked != null && 
						daisyLinked.getCallerId().getNumber().equals(extension))
					redirectCallToQueue(linked.getId(), from);
				
			}	
			
		}
		
	}

	/**
	 * Checks to see whether the given channel is locked and should not be transferred
	 * This is a preventative measure to make sure 3 requests to answer the call
	 * does not result in the call jumping between 3 operators
	 * 
	 * After a short TIME_OUT channel lock expires and can be transfered again
	 * 
	 * If channel is not locked, method will return false but the channel will become locked
	 * so that subsequent calls will get the correct locked value
	 * 
	 * @param channel
	 * @return true if channel lock time out has not expired
	 */
	private synchronized boolean isLocked(AsteriskChannel channel) {
		
		boolean locked = false;
		
		if(lockedChannels.get(channel.getId()) != null){
			
			//Potentially locked
			long lockedAt = lockedChannels.get(channel.getId());
			
			if(new Date().getTime() - lockedAt >= channelLockTimeOut){
				
				lockedChannels.put(channel.getId(), new Date().getTime());
				
			}else
				locked = true;
			
		}else
			lockedChannels.put(channel.getId(), new Date().getTime());
		
		return locked;
	}

	/**
	 * Redirects the given channel to the on air queue specified in DB
	 * @param channelID Channel to redirect
	 */
	public void redirectCallToQueue(String channelID, String from){
		
		AsteriskChannel channel = activeChannels.get(channelID);
		/* Check against outgoing calls and update DB as necessary because
		 * outgoing call will generate a final permanent channel as it enters
		 * the queue (no idea why)
		 * 
		 * The only way to track this is by caller ID which will be set for
		 * outbound calls as we dialled it
		 */
		
		if(channel != null){
			
			if(ringingExternal.containsKey(channelID)){
				
				OutgoingCall out = ringingExternal.get(channelID);
				expectedInQueue.put(out.destination, channelID);
				//Discuss: Potential memory leak, what if call never makes it to queue?
				
			}
				
			channel.redirect(defaultContext, queueNumber, DEFAULT_PRIORITY);
			String callerID = channel.getCallerId().getNumber();
			
			if(removePrefix(callerID))
				callerID = callerID.substring(dialPrefix.length());
				
			dbLookUpService.execute(new PhoneCall(databaseManager, 
					callerID, channel.getId(), this, 'Q', from));
			
		}
		
	}
	
	/**
	 * Sends an asterisk Command to hang up the given channel
	 * @param channelID channel to hang up
	 * @param from person who initiated hang up (for DB tracking purposes)
	 */
	public void hangupCall(String channelID, String from){
		
		AsteriskChannel channel = activeChannels.get(channelID);
		
		String callerID = channel.getCallerId().getNumber();
		
		if(removePrefix(callerID))
			callerID = callerID.substring(dialPrefix.length());
		
		if(channel != null)
			channel.hangup();
		
		dbLookUpService.execute(new PhoneCall(databaseManager, 
				callerID, channel.getId(), this, 'H', from));
		
	}
	
	/**
	 * Finishes a manual call outside of the system.  Logs all normal DB entries
	 * as it if it was a real call
	 * @param channelID Manual Channel ID "M_????"
	 * @param from user who requested hangup
	 */
	private void hangupManualCall(String channelID, String from){
		
		//Log to DB
		String logCause = xStrings.getString("AsteriskManager.channelHangup"); 
		String callerID = xStrings.getString("AsteriskManager.withHeldNumber");
		
		dbLookUpService.execute(new PhoneCall(databaseManager, 
					callerID, channelID, this, 
					'H', from)); 

		//Send XMPP Message
		String message = logCause + "/" + callerID + 
				"/" + channelID;  
		LOGGER.info(message);
		sendMessage(message);
		
	}
	
	
	/** AsteriskServerListener **/
	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info(xStrings.getString("AsteriskManager.newChannel") + "/" + channel.getId());  
		
		channel.addPropertyChangeListener(this);
		addActiveChannel(channel);
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//DB Lookup for PhoneCall info (namely just to create entries) via Executor Thread
		//Returns via sendNewQueueEntryMessage
		LOGGER.info(xStrings.getString("AsteriskManager.newQueueEntry") + 
				entry.getQueue().getName() + "/" + entry.getChannel().getId()); 

		String callerID = checkNumberWithHeld(entry.getChannel().getCallerId());
		
		int expected = isExpectedQueue(callerID);
		
		if(expected == 1 || expected == 2){
			
			if(expected == 2)
				callerID = callerID.substring(dialPrefix.length());
			
			//Create OutboundChannelUpdater to change old channel to new channel in call records
			dbLookUpService.execute(new OutboundChannelUpdater(
					settings.get("language"), settings.get("country"),  
					databaseManager.getReadConnection(), 
					databaseManager.getWriteConnection(),
					expectedInQueue.get(callerID), entry.getChannel().getId(), 
					callerID, this));
			
			expectedInQueue.remove(callerID);
			
		}else if(removePrefix(callerID))
			callerID = callerID.substring(dialPrefix.length());
		else
			callerID = null;//null because we don't want to override the entry callerid
		
		dbLookUpService.execute(new PhoneCall(databaseManager, entry, this, callerID));
		
	}

	/**
	 * Sends a message to the XMPP control room that we have a new QueueEntry
	 * @param entry
	 * @param callerID over ride the queue entry with this caller id
	 */
	public void sendNewQueueEntryMessage(AsteriskQueueEntry entry, String callerID){
		
		//Transfers from handlers goes into Studio Queue
		/* There seems to be two queue entries that are fired in the events.
		 * The first is null :| so lets ignore those
		 */
		if(entry != null){
			
			AsteriskQueue queue = entry.getQueue();
			String name = queue.getName();
			
			String number = null;
			
			/* If callerID not null then we need to add this number back to 
			 * remove prefix stuff until the permanent channel is dropped
			 */
			if(callerID != null){
				
				number = callerID;
				calls.put(entry.getChannel().getId(), callerID);//will add as channel, callerid need to check for channel in hangups
				
			}else
				number = checkNumberWithHeld(entry.getChannel().getCallerId());
			
			// Remove dialprefix from number if this is an external call
			if(removePrefix(number))
				number = number.substring(dialPrefix.length());
			
			String id = entry.getChannel().getId();
			
			String message = xStrings.getString("AsteriskManager.populatedQueueEntry") + "/" +  
					name + "/" + number + "/" + id;  
			
			LOGGER.info(message);
			sendMessage(message);
			
		}
		
	}
	
	/**
	 * Helper method to replace with held number with a suitable string
	 * @param id callerid to check
	 * @return caller id number or suitable string if withheld/null
	 */
	private String checkNumberWithHeld(CallerId id){
		
		String callerid = null;
		
		if(id != null && id.getNumber() != null && id.getNumber().trim().length() > 0 &&
				!id.getNumber().equalsIgnoreCase("null")) 
			callerid = id.getNumber();
		else
			callerid = xStrings.getString("AsteriskManager.withHeldNumber"); 
		
		return callerid;
		
	}
	
	/**
	 * Sends control message to XMPP control chat room
	 * @param message
	 */
	private void sendMessage(String message){
		
		LOGGER.info(xStrings.getString("AsteriskManager.logSendingMessage")); 
		
		try {
				
			controlRoom.sendMessage(message);
			
		}catch(XMPPException e){
			LOGGER.severe(xStrings.getString("AsteriskManager.XMPPError")); 
		}catch(IllegalStateException e){
			LOGGER.severe(xStrings.getString("AsteriskManager.XMPPServerGoneError")); 
		}
		
	}
	
	/** PacketListener **/
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(!startup && XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			String from = message.getFrom();
			
			if(from.contains("/")) 
				from = from.split("/")[1]; 
			
			if(!from.equals(controlRoom.getNickname())){//If the message didn't come from me 
				
				//React to commands thread all of this if performance is a problem
				LOGGER.info(xStrings.getString("AsteriskManager.receivedMessage") + 
						message.getBody());
				
				String[] command = message.getBody().split("/"); 
				
				if(command.length == 3 && command[0].equals(
								xStrings.getString("AsteriskManager.commandTransfer"))){ 
					
					//Received a transfer command
					redirectCall(command[1], command[2], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandQueue"))){ 
					
					//Received a command to put the call into the on air queue
					redirectCallToQueue(command[1], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandUpdate"))){ 
				
					//Send updates to the person who asked for it (usually when they login)
					sendChannelInfo(from);
					
					/* If the user has an extension check it is working
					 * If it is null they don't have an extension so they are 
					 * using this in read only mode */
					if(!command[1].equals("null")){ 
						
						int extensionStatus = isExtensionOnline(command[1]);
						
						if(extensionStatus != 0)//If the users phone is not working, tell them
							sendPrivateMessage(from, xStrings.getString(
									"AsteriskManager.FAILED") + "/NA/" +   
									extensionStatus);
						
					}
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandHangup"))){ 
					
					if(command[1].startsWith("M_")){
						
						//Send hang up message and log in DB
						hangupManualCall(command[1], from);
						
					}else //Real call so hang up
						hangupCall(command[1], from);
					
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.commandDial"))){ 
					
					createCall(command[1], command[2], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandTransferEndPoint"))){ 
					
					/* TODO 14/08/2014 WMM: Removed Transfer EndPoint from outgoing calls
					 * Test impact, do we even need this anymore?
					 */
					//We're transferring the other side of the call here
					/* When we transfer, a new channel is created for the receiver of this call
					 * This makes all the call information get dropped
					 * To work around this we have to do it client side so all clients stay in sync
					 * New Command ENDPOINT: Sends what the channel was and what the endpoint is so that
					 * clients can store endpoint extension and then when the queue message comes in
					 * update the channel properly (in theory)
					 */
					//Remove dial prefix if this was an outgoing external call
					String endPointCallerID = activeChannels.get(
							command[1]).getLinkedChannel().getCallerId().getNumber();
					
					if(removePrefix(endPointCallerID))
						endPointCallerID = endPointCallerID.substring(
								dialPrefix.length());
					
					sendMessage(xStrings.getString("AsteriskManager.commandEndPoint") + "/" + command[1] + "/"   
					 + activeChannels.get(command[1]).getLinkedChannel().getId() + "/"  
					 + endPointCallerID);
					
					redirectCallToQueue(activeChannels.get(command[1]).getLinkedChannel()
							.getId(), from);
					
				}else if(command.length == 4 && command[0].equals(
						xStrings.getString("AsteriskManager.commandUpdateField"))){ 
					
					//Spawn a thread to do a DB update
					dbLookUpService.execute(new RecordUpdater(settings.get("language"),  
							settings.get("country"), databaseManager.getReadConnection(), 
							databaseManager.getWriteConnection(), command[1], 
							command[2], command[3]));
				
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.ChangeToNewPerson"))){ 
					
					//Make a new person, update call references and then send back 
					//CHANGENEW/CHANNEL/NUMBER
					//XMPP Reply so clients can update
					dbLookUpService.execute(new PersonChanger(settings.get("language"),  
							settings.get("country"), databaseManager.getReadConnection(), 
							databaseManager.getWriteConnection(), controlRoom, 
							-1, command[2], command[1])); 
					
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.ChangeToExistingPerson"))){ 
					
					//CHANGEEXISTING/CHANNEL/PERSON
					//Update call references and then send back XMPP Reply so
					//clients can update
					dbLookUpService.execute(new PersonChanger(settings.get("language"),  
							settings.get("country"), databaseManager.getReadConnection(), 
							databaseManager.getWriteConnection(), controlRoom, 
							Integer.parseInt(command[2]), null, command[1])); 
					
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.changeActive"))){ 
					
					//Update the Active Person on an outgoing call
					dbLookUpService.execute(new ActivePersonChanger(settings.get("language"),  
							settings.get("country"),  
							databaseManager.getWriteConnection(), command[2], 
							command[1])); 
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandManual"))){
					
					/* Someone wants to add a manual call entry (from a landline or whatever)
					 * MANUAL/Name of Person who answered e.g.
					 * MANUAL/Wayne
					 * Channel is M_ + new Date().getTime()
					 * 
					 * We will set the caller number to unknown
					 * TODO find a way to set number later
					 */
					String manualChannel = "M_" + new Date().getTime();
					
					dbLookUpService.execute(new PhoneCall(xStrings.getString("AsteriskManager.withHeldNumber"), 
							manualChannel, 'M', from, databaseManager));
					
					//Send MANUAL/CHANNEL/ANSWERER back to clients
					sendMessage(xStrings.getString("AsteriskManager.commandManual")
							+ "/" + manualChannel + "/" + command[1]);
					
				}
				
			}
			
		}else if(startup)
			LOGGER.info(xStrings.getString("AsteriskManager.receivedXMPPWhileStarting")); 
		
		
	}
	
	/**
	 * Similar to removePrefix except checks the expectedInQueue list
	 * @param callerID
	 * @return 	0 NOT EXPECTED
	 * 			1 EXPECTED
	 * 			2 EXPECTED BUT REMOVE DIALPREFIX
	 */
	private int isExpectedQueue(String callerID) {
		
		int expected = 0;
		
		if(expectedInQueue.containsKey(callerID.substring(2)))
			expected = 2;
		else if(expectedInQueue.containsKey(callerID))
			expected = 1;
			
		return expected;
		
	}

	
	/**
	 * Signals true if you need to remove the dial prefix from the given caller
	 * ID
	 * @param callerID CallerID to lookup in the calls hashmap
	 * @return true if this has a dial prefix on it
	 */
	private boolean removePrefix(String callerID){
		
		boolean removePrefix = false;
		Iterator<String> keys = calls.keySet().iterator();
		
		while(keys.hasNext() && !removePrefix){
			
			String key = keys.next();
			String to = calls.get(key);
			
			/* Dial outs will have caller id and the channel so lets get just
			 * the id to check TODO we removed the channel?
			 */
			String[] temp = to.split("/"); 
			
			if((dialPrefix + temp[0]).equals(callerID))
				removePrefix = true;
			
		}
		
		return removePrefix;
		
	}
	
	/**
	 * Send a force update on panel to clients
	 * @param channel
	 */
	public void sendPanelUpdate(String channel){
		
		String message = xStrings.getString("AsteriskManager.channelUpdate") +  
				"/" + channel; 
		LOGGER.info(message);
		sendMessage(message);
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		if(evt.getPropertyName().equals("state") && 
				evt.getSource() instanceof AsteriskChannel){ 
			
			if(evt.getNewValue() instanceof ChannelState){
				
				ChannelState state = (ChannelState)evt.getNewValue();
				
				if(state.getStatus() == ChannelState.RING.getStatus() || 
						state.getStatus() == ChannelState.RINGING.getStatus()){ 
					
					//This channel is dialling out and listening to it ringing
					AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
					
					/* Check calls to see if ringing.getCallerId().equals anything in there
					 * If so, remove the dial prefix if we have one there
					 */
					String callerID = checkNumberWithHeld(ringing.getCallerId());
					
					if(removePrefix(callerID))
						callerID = callerID.substring(dialPrefix.length());
					
					String message = xStrings.getString("AsteriskManager.channelRinging") +  
							"/" + callerID + "/" + ringing.getId();  
					
					/* When we dial out to an external call you get connection messages
					 * based on the outbound callerid
					 * e.g. 
					 * (17:15:03) server: CONNECTED/5578098/01234567890/1408724095.656
					 * (17:15:03) server: CONNECTED/01234567890/5578098/1408724095.659
					 * What I want to do is store the dialler extension and substitute
					 * that for the outbound callerid so that we can properly track 
					 * channels without too much fiddling in the client
					 * 
					 * In order to do this for the dialler: at the CALL stage do a clid
					 * lookup in calls and append the dialler + channel
					 * 
					 * For the receiver: at the ringing stage do a clid lookup in calls
					 * and at the connected stage do a channel lookup in ringing external
					 * to substitute appropriately
					 * calls ==> <fromNumber, toNumber>
					 */
					if(calls.containsValue(callerID)){//this is an outgoing call we're tracking
						
						Set<Entry<String, String>> entries = calls.entrySet();
						
						Iterator<Entry<String, String>> index = entries.iterator();
						boolean done = false;
						
						while(index.hasNext() && !done){
							
							Entry<String, String> callRecord = index.next();
							
							if(callRecord.getValue().equals(callerID)){
								
								done = true;
								OutgoingCall out = new OutgoingCall(ringing.getId(), callRecord.getKey(), callerID);
								ringingExternal.put(ringing.getId(), out);
								LOGGER.info(xStrings.getString(
										"AsteriskManager.loggingExternalCall") + 
										out); 
								
							}
								
						}
						
					}
						
					LOGGER.info(message);
					sendMessage(message);
					
				}else if(state.getStatus() == ChannelState.HUNGUP.getStatus()){
					
					AsteriskChannel hangup = (AsteriskChannel)evt.getSource();
					
					/* Hangup Cause
					 * Normal Clearing = normal hangup
					 * Subscriber absent = number offline / doesn't exist?
					 * Channel unacceptable = can't make that call
					 * ???
					 */
					String hangupCause = hangup.getHangupCauseText();
					String logCause;
					boolean logHangup = false;
					
					if(hangupCause.equals(HANGUP_NORMAL)){
						logCause = xStrings.getString("AsteriskManager.channelHangup"); 
						/* We want to log normal hang ups as part of a normal call life cycle
						 * but not the errors as we're only interested complete calls from outside
						 */
						logHangup = true; 
						
					}else if(hangupCause.equals(HANGUP_OFFLINE))
						logCause = xStrings.getString("AsteriskManager.channelHangupOffline"); 
					else if(hangupCause.equals(HANGUP_UNACCEPTABLE))
						logCause = xStrings.getString("AsteriskManager.channelHangupUnacceptable"); 
					else if(hangupCause.equals(HANGUP_ANSWERED_ELSEWHERE))
						logCause = xStrings.getString("AsteriskManager.channelHangupAnsweredElsewhere"); 
					else if(hangupCause.equals(HANGUP_USER_BUSY))
						logCause = xStrings.getString("AsteriskManager.channelHangupUserBusy"); 
					else
						logCause = xStrings.getString("AsteriskManager.channelHangup") + " "   
								+ hangupCause;
					
					//Remove the dial out prefix from callerID if this is a call we dialled
					String callerID = checkNumberWithHeld(hangup.getCallerId());
					
					if(removePrefix(callerID))
						callerID = callerID.substring(dialPrefix.length());
					
					//Log this in the DB
					if(logHangup)
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								callerID, hangup.getId(), this, 
								'H', "NA")); 
						
					/* BUG FIX: Hangup Cause sometimes has a slash in it which messes with our / command separator
					 * HANGUP Circuit/channel congestion as one example there may be others so lets deal with it
					 */
					hangupCause = hangupCause.replaceAll("/", "-");  
					//END OF BUG FIXkey
					
					//Send XMPP Message
					String message = logCause + "/" + callerID + 
							"/" + hangup.getId();  
					LOGGER.info(message);
					sendMessage(message);

					if(ringingExternal.containsKey(hangup.getId())){
						
						ringingExternal.remove(hangup.getId());
						
						if(calls.containsValue(callerID)){
							
							Iterator<String> callKeys = calls.keySet().iterator();
							
							boolean removed = false;
							
							while(!removed && callKeys.hasNext()){
								
								String key = callKeys.next();
								
								if(calls.get(key).equals(callerID)){
									
									removed = true;
									calls.remove(key);
									
								}
									
							}
							
						}
						
					}
					
					//Clear calls of any channels that made it into the queue
					if(calls.containsKey(hangup.getId()))
						calls.remove(hangup.getId());
					
					removeActiveChannel(hangup.getId());
					
				}else if(state.getStatus() == ChannelState.BUSY.getStatus()){
					
					AsteriskChannel busy = (AsteriskChannel)evt.getSource();
					
					String message = xStrings.getString("AsteriskManager.channelBusy") + 
							"/" + checkNumberWithHeld(busy.getCallerId()) + "/" + busy.getId();  
					
					LOGGER.info(message);
					sendMessage(message);
					
				}
				
			}
			
			/* Other events we can ignore:
			 * oldValue DOWN && newValue UP == channel bought online
			 * oldValue RING && newValue UP == channel online prior to connecting call
			 * This is followed by a link event which confirms channels are connected
			 */
			
		}else if(evt.getPropertyName().equals("currentExtension") && 
					evt.getOldValue() == null){
			
			/*Shows us the extension the channel is ringing
			 * If old value != null its just dial plan changes that we don't need to 
			 * process
			 */
			if(evt.getNewValue() instanceof Extension &&
					evt.getSource() instanceof AsteriskChannel){
				
				Extension calling = (Extension)evt.getNewValue();
				
				//Check for macro-auto which is usually ring group connect
				if(!calling.getContext().equals(contextMacroAuto)){
					
					AsteriskChannel channel = (AsteriskChannel)evt.getSource();
					
					//TODO Check this can't remember what significance no id had
					if(channel.getCallerId().getNumber() != null && 
							!channel.getCallerId().getNumber().equals("null")){  
						//Remove dial prefix if this was a call we dialled
						String callerID = checkNumberWithHeld(channel.getCallerId());
						
						if(removePrefix(callerID))
							callerID = callerID.substring(dialPrefix.length());
						
						/* Do the same with the extension this channel is 
						 * calling because it could be 5103 (internal) to
						 * 9901234567890 (external dial out)
						 * So we need to remove prefix from external too
						 */
						String extensionCalling = calling.getExtension();
						
						if(removePrefix(extensionCalling))
							extensionCalling = extensionCalling.substring(
									dialPrefix.length());
						
						String message = xStrings.getString("AsteriskManager.callRingingFrom") +  
								"/" + callerID + "/" +   
								extensionCalling + "/" + channel.getId(); 
						
						/* When we dial out to an external call you get connection messages
						 * based on the outbound callerid
						 * e.g. 
						 * (17:15:03) server: CONNECTED/5578098/01234567890/1408724095.656
						 * (17:15:03) server: CONNECTED/01234567890/5578098/1408724095.659
						 * What I want to do is store the dialler extension and substitute
						 * that for the outbound callerid so that we can properly track 
						 * channels without too much fiddling in the client
						 * 
						 * In order to do this for the dialler: at the CALL stage do a clid
						 * lookup in calls and append the dialler + channel
						 * 
						 * For the receiver: at the ringing stage do a clid lookup in calls
						 * and at the connected stage do a channel lookup in ringing external
						 * to substitute appropriately
						 * calls ==> <fromNumber, toNumber>
						 */
						if(calls.containsKey(callerID)){
							
							OutgoingCall out = new OutgoingCall(channel.getId(), callerID, extensionCalling);
							ringingExternal.put( channel.getId(), out);
							LOGGER.info(xStrings.getString(
									"AsteriskManager.loggingExternalCall") + 
									out); 
							
						}
						
						dbLookUpService.execute(new PhoneCall(callerID, 
								channel.getId(), databaseManager));
					
						LOGGER.info(message);
						sendMessage(message);
					
					} //Reinstated null callerid check
					
				}
				
			}
			
		}else if(evt.getPropertyName().equals("linkedChannel") &&  
				evt.getSource() instanceof AsteriskChannel){
			
			//Linked Channel = connected (can also be disconnected)
			AsteriskChannel channel = (AsteriskChannel)evt.getSource();
			
			if(evt.getNewValue() != null){//null = unlinking, not null = linking
				//TODO unknown number here
				String linkedTo = channel.getLinkedChannel().getCallerId().getNumber(); 
				
				if(!linkedTo.equals(channel.getCallerId().getNumber())){
					
					//Remove dial prefix if it is on the callerID
					String callerID = checkNumberWithHeld(channel.getCallerId());
					
					if(removePrefix(callerID))
						callerID = callerID.substring(dialPrefix.length());
					
					//Do the same for the linkedTo ID
					if(removePrefix(linkedTo))
						linkedTo = linkedTo.substring(dialPrefix.length());
					
					/* When we dial out to an external call you get connection messages
					 * based on the outbound callerid
					 * e.g. 
					 * (17:15:03) server: CONNECTED/5578098/01234567890/1408724095.656
					 * (17:15:03) server: CONNECTED/01234567890/5578098/1408724095.659
					 * What I want to do is store the dialler extension and substitute
					 * that for the outbound callerid so that we can properly track 
					 * channels without too much fiddling in the client
					 * 
					 * In order to do this for the dialler: at the CALL stage do a clid
					 * lookup in calls and append the dialler + channel
					 * 
					 * For the receiver: at the ringing stage do a clid lookup in calls
					 * and at the connected stage do a channel lookup in ringing external
					 * to substitute dialler appropriately
					 * calls ==> <fromNumber, toNumber>
					 * ringingExternal ==> <channel, extension>
					 * 
					 * Would be good to stop the dialler connected message however this
					 * messes up the clients so we need a client side work around
					 * that when connected is received, if we have a panel in the 
					 * ringing me state (or whatever it is for other clients) check 
					 * if this is connected to that channel and remove it.
					 */
					if(ringingExternal.containsKey(channel.getId())){
						
						OutgoingCall out = ringingExternal.get(channel.getId());
						
						if(!callerID.equals(out.source) && !callerID.equals(out.destination)){
							
							//callerID probably needs substituting, lets make sure linkedTokey
							if(linkedTo.equals(out.source))
								callerID = out.destination;
							else
								callerID = out.source;
							
						}else if(!linkedTo.equals(out.source) && !linkedTo.equals(out.destination)){
							
							if(callerID.equals(out.source))
								linkedTo = out.destination;
							else
								linkedTo = out.source;
							
						}
						
					}
						
					String message = xStrings.getString("AsteriskManager.callConnected") +  
							"/" + callerID + "/" +  
							linkedTo + "/" + channel.getId(); 
					
					if(systemExtensions.contains(linkedTo))//if we're linked to a system phone
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								callerID, channel.getId(), this, 'A', "NA")); 
					
					if(calls.containsKey(callerID))
						LOGGER.info(xStrings.getString(
								"AsteriskManager.suppressingConnectedMessage") + 
								message); 
					else{
						
						LOGGER.info(message);
						sendMessage(message);
					
					}
					
				}else{
					
					/* Linked channel is same as caller ID, this is sent when dialling to 
					 * show that the call has connected to the originating phone before it
					 * starts to dial the number, we can ignore these events as otherwise
					 * you just spam clients with a ton of CONNECTED/X/X/CHANNEL while
					 * things go through the motions.
					 */
					
					
				}
				
			}
			
		}
		
	}
	
	/** MESSAGE LISTENER **/
	@Override
	public void processMessage(Chat chat, Message message) {
		
		//We don't care about any private messages we get so just ignore
		
	}
	
	/** UNUSED AsteriskServerListener methods **/
	@Override
	public void onNewMeetMeUser(MeetMeUser user) {}

	@Override
	public void onNewAgent(AsteriskAgentImpl agent) {}

	/** UNUSED OriginateCallback **/
	@Override
	public void onBusy(AsteriskChannel channel) {}

	@Override
	public void onDialing(AsteriskChannel channel) {}

	@Override
	public void onFailure(LiveException exception) {}

	@Override
	public void onNoAnswer(AsteriskChannel channel) {}

	@Override
	public void onSuccess(AsteriskChannel channel) {}

}
