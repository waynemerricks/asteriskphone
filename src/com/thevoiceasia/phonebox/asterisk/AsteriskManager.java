package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.database.PersonChanger;
import com.thevoiceasia.phonebox.database.RecordUpdater;
import com.thevoiceasia.phonebox.records.PhoneCall;
import com.thevoiceasia.phonebox.records.TrackDial;

public class AsteriskManager implements AsteriskServerListener, PropertyChangeListener, OriginateCallback, PacketListener, MessageListener {

	//STATICS
	private static final Logger AST_LOGGER = Logger.getLogger("org.asteriskjava"); //$NON-NLS-1$
	private static final Logger LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.asterisk"); //$NON-NLS-1$
	private static final String HANGUP_NORMAL = "Normal Clearing"; //$NON-NLS-1$
	private static final String HANGUP_UNACCEPTABLE = "Channel unacceptable"; //$NON-NLS-1$
	private static final String HANGUP_OFFLINE = "Subscriber absent"; //$NON-NLS-1$
	private static final String HANGUP_USER_BUSY = "User busy"; //$NON-NLS-1$
	private static final String HANGUP_ANSWERED_ELSEWHERE = "Answered elsewhere"; //$NON-NLS-1$
	private static final String SIP_PREFIX = "Local/"; //$NON-NLS-1$
	private static final int DEFAULT_PRIORITY = 1;
	private static final long DEFAULT_TIMEOUT = 30000L; //Default time out if database record is null
	private static final int DEFAULT_EXECUTOR_THREADS = 4; //Default threads if database is null
	private static final long DEFAULT_CHANNEL_LOCK = 3000; //Default time for the channel lock to be enforced if db is null

	//CLASS VARS
	private AsteriskServer asteriskServer;
	private HashMap<String, AsteriskChannel> activeChannels = new HashMap<String, AsteriskChannel>();
	private HashMap<String, Long> lockedChannels = new HashMap<String, Long>();
	private I18NStrings xStrings;
	private String autoAnswerContext, defaultContext, contextMacroAuto, queueNumber, dialPrefix;
	private long defaultTimeOut, channelLockTimeOut;
	private MultiUserChat controlRoom;
	private DatabaseManager databaseManager;
	private HashSet<String> systemExtensions = new HashSet<String>();
	private HashMap<String, String> settings;
	private HashMap<String, String> calls = new HashMap<String, String>(); //HashMap to store dialled calls in progress
	/* To work around the dial prefix issue, we need to keep hold of any calls we're expecting
	 * to hang up as part of a TRANSFERENDPOINT event.
	 * Normally this would cause the original channel to hang up and be removed from the 
	 * calls hash map.  What we'll do is add them to expectHangup so that we can stop them
	 * being removed from calls when the HANGUP message follows as they're transferred to the
	 * QUEUE
	 */
	private Vector<String> expectHangup = new Vector<String>();
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
		this.autoAnswerContext = settings.get("autoAnswerContext"); //$NON-NLS-1$
		this.defaultContext = settings.get("defaultContext"); //$NON-NLS-1$
		this.contextMacroAuto = settings.get("contextMacroAuto"); //$NON-NLS-1$
		this.queueNumber = settings.get("onAirQueueNumber"); //$NON-NLS-1$
		
		//Set the dial prefix to whatever we call for an outside line or "" if null/blank
		if(settings.containsKey("outsideCallPrefix")) //$NON-NLS-1$
			this.dialPrefix = settings.get("outsideCallPrefix"); //$NON-NLS-1$
		else
			dialPrefix = ""; //$NON-NLS-1$
		
		if(settings.containsKey("defaultTimeOut")) //$NON-NLS-1$
			this.defaultTimeOut = Long.parseLong(settings.get("defaultTimeOut")); //$NON-NLS-1$
		else
			this.defaultTimeOut = DEFAULT_TIMEOUT;
		
		if(settings.containsKey("threadPoolMax")) //$NON-NLS-1$
			this.maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); //$NON-NLS-1$
		else
			this.maxExecutorThreads = DEFAULT_EXECUTOR_THREADS;
		
		if(settings.containsKey("channelLockTimeOut")) //$NON-NLS-1$
			this.channelLockTimeOut = Long.parseLong(settings.get("channelLockTimeOut")); //$NON-NLS-1$
		else
			this.channelLockTimeOut = DEFAULT_CHANNEL_LOCK;
		
		this.controlRoom = controlRoom; //Control Room XMPP chat
		this.controlRoom.addMessageListener(this);
		
		//Turn off AsteriskJava logger for all but SEVERE
		AST_LOGGER.setLevel(Level.SEVERE);
		
		xStrings = new I18NStrings(settings.get("language"), settings.get("country")); //$NON-NLS-1$ //$NON-NLS-2$
		
		asteriskServer = new DefaultAsteriskServer(settings.get("asteriskHost"),  //$NON-NLS-1$
				settings.get("asteriskUser"), settings.get("asteriskPass"));  //$NON-NLS-1$//$NON-NLS-2$
		
		systemExtensions = databaseManager.getSystemExtensions();
		
	}
	
	/**
	 * Set the startup flag to false once you're ready for this object to process
	 * XMPP control messages
	 */
	public void startProcessingMessages(){
		
		LOGGER.info(xStrings.getString("AsteriskManager.finishedStarting")); //$NON-NLS-1$
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
		
		getChannels();
		sendMessage(xStrings.getString("AsteriskManager.XMPPServerHello")); //$NON-NLS-1$
		
	}
	
	/**
	 * Disconnects from Asterisk server
	 */
	public void disconnect() {
		
		asteriskServer.shutdown();
		
	}

	/**
	 * Gets the active Channels, adds a property change and keeps track of them via
	 * PropertyChangeListener 
	 */
	private void getChannels(){
		
		for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
            
			LOGGER.info(xStrings.getString("AsteriskManager.startupActiveChannels") + "/" + asteriskChannel.getId()); //$NON-NLS-1$ //$NON-NLS-2$
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
		//System.out.println(activeChannels.size());
		
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
		LOGGER.info(xStrings.getString("AsteriskManager.sendingChannelInfo") + recipient); //$NON-NLS-1$
		
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
        	String command = xStrings.getString("AsteriskManager.callConnected") + "/"  //$NON-NLS-1$ //$NON-NLS-2$
        			+ orderedChannels.get(i).getCallerId().getNumber() + "/"  //$NON-NLS-1$
        			+ orderedChannels.get(i).getLinkedChannel().getCallerId().getNumber() 
        			+ "/" + orderedChannels.get(i).getId() + "/" + //$NON-NLS-1$ //$NON-NLS-2$
        			orderedChannels.get(i).getDateOfCreation().getTime(); 
        	
        	//System.out.println(command);
        	sendPrivateMessage(recipient, command);
			
		}
		
		for(AsteriskQueue asteriskQueue : asteriskServer.getQueues()){

			//System.out.println("QUEUE: " + asteriskQueue);
			
			for(AsteriskQueueEntry entry : asteriskQueue.getEntries()){
				
				//CALL/5003/3000/1377009449.5
				String command = xStrings.getString("AsteriskManager.callRingingFrom") + "/"   //$NON-NLS-1$//$NON-NLS-2$
						+ entry.getChannel().getCallerId().getNumber() + "/"  //$NON-NLS-1$
						+ asteriskQueue.getName() + "/"  //$NON-NLS-1$
						+ entry.getChannel().getId() + "/" +  //$NON-NLS-1$
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
	
		Chat chat = controlRoom.createPrivateChat(controlRoom.getRoom() + "/" + recipient, this); //$NON-NLS-1$
		
		try {
			LOGGER.info(xStrings.getString("AsteriskManager.sendingPrivateMessage") +  //$NON-NLS-1$
					recipient + "/" + message); //$NON-NLS-1$
			chat.sendMessage(message);
		} catch (XMPPException e) {
			LOGGER.warning(xStrings.getString("AsteriskManager.errorSendingPrivateMessage") + //$NON-NLS-1$
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
		
		//BUG FIX Store the call creation stuff here so we can look it up later
		//BUG FIX Remove any dial prefix's from the call here
		String toWithoutPrefix = to;
		
		if(dialPrefix.length() > 0 && to.startsWith(dialPrefix))
			toWithoutPrefix = to.substring(dialPrefix.length());
		
		calls.put(fromNumber, toWithoutPrefix);
		
		trackDial(toWithoutPrefix, fromName);//Add an entry to callhistory D so we can track who dialled stuff
		
		HashMap<String, String> vars = new HashMap<String, String>();
		asteriskServer.originateToExtensionAsync(SIP_PREFIX + fromNumber + "@" +  //$NON-NLS-1$
				autoAnswerContext,  autoAnswerContext, to, DEFAULT_PRIORITY, defaultTimeOut, 
				new CallerId(fromName, fromNumber), vars, this);
		
	}
	
	/**
	 * Adds an entry to the call history table for dialling a number
	 * @param phoneNumber number dialled
	 * @param operator operator who did it
	 */
	private void trackDial(String phoneNumber, String operator) {
		
		dbLookUpService.execute(new TrackDial(settings.get("language"),  //$NON-NLS-1$
				settings.get("country"), databaseManager, operator,  //$NON-NLS-1$
				phoneNumber));
		 
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
		parkActiveCalls(to, from);
		
		// Get the channel and if its not locked, transfer it to this phone
		AsteriskChannel channel = activeChannels.get(channelID);
		
		boolean locked = isLocked(channel);
		
		if(channel != null && !locked){//null = channel not found
			
			channel.redirect(autoAnswerContext, to, DEFAULT_PRIORITY);
		
			dbLookUpService.execute(new PhoneCall(databaseManager, 
					channel.getCallerId().getNumber(), channel.getId(), this, 'A', from));
			
		}else if(locked){
			
			String message = xStrings.getString("AsteriskManager.commandLocked") + "/" +  //$NON-NLS-1$ //$NON-NLS-2$
			channel.getId();
			sendMessage(message);
			
		}
		
	}
	
	/**
	 * Checks for any active calls to the given extension
	 * If we have an active call, it will be put in the on air queue (or should we hang up?)
	 * @param extension extension to check active calls against
	 * @param from name of person who instigated this transfer
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
		
		if(channel != null){
			
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
	
	
	/** AsteriskServerListener **/
	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info(xStrings.getString("AsteriskManager.newChannel") + "/" + channel.getId()); //$NON-NLS-1$ //$NON-NLS-2$
		channel.addPropertyChangeListener(this);
		addActiveChannel(channel);
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//DB Lookup for PhoneCall info (namely just to create entries) via Executor Thread
		//Returns via sendNewQueueEntryMessage
		LOGGER.info(xStrings.getString("AsteriskManager.newQueueEntry") + //$NON-NLS-1$
				entry.getQueue().getName() + "/" + entry.getChannel().getId()); //$NON-NLS-1$

		String callerID = checkNumberWithHeld(entry.getChannel().getCallerId());
		
		if(removePrefix(callerID))
			callerID = callerID.substring(dialPrefix.length());
		else
			callerID = null;
		
		dbLookUpService.execute(new PhoneCall(databaseManager, entry, this, callerID));
		
	}

	/**
	 * Sends a message to the XMPP control room that we have a new QueueEntry
	 * @param entry
	 */
	public void sendNewQueueEntryMessage(AsteriskQueueEntry entry){
		
		//Transfers from handlers goes into Studio Queue
		/* There seems to be two queue entries that are fired in the events.
		 * The first is null :| so lets ignore those
		 */
		if(entry != null){
			
			AsteriskQueue queue = entry.getQueue();
			String name = queue.getName();
			String number = checkNumberWithHeld(entry.getChannel().getCallerId());
			
			// Remove dialprefix from number if this is an external call
			if(removePrefix(number))
				number = number.substring(dialPrefix.length());
			
			String id = entry.getChannel().getId();
			
			String message = xStrings.getString("AsteriskManager.populatedQueueEntry") + "/" + //$NON-NLS-1$ //$NON-NLS-2$
					name + "/" + number + "/" + id; //$NON-NLS-1$ //$NON-NLS-2$
			
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
				!id.getNumber().equalsIgnoreCase("null")) //$NON-NLS-1$
			callerid = id.getNumber();
		else
			callerid = xStrings.getString("AsteriskManager.withHeldNumber"); //$NON-NLS-1$
		
		return callerid;
		
	}
	
	/**
	 * Sends control message to XMPP control chat room
	 * @param message
	 */
	private void sendMessage(String message){
		
		LOGGER.info(xStrings.getString("AsteriskManager.logSendingMessage")); //$NON-NLS-1$
		
		try {
				
			controlRoom.sendMessage(message);
			
		}catch(XMPPException e){
			LOGGER.severe(xStrings.getString("AsteriskManager.XMPPError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			LOGGER.severe(xStrings.getString("AsteriskManager.XMPPServerGoneError")); //$NON-NLS-1$
		}
		
	}
	
	/** PacketListener **/
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(!startup && XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			String from = message.getFrom();
			
			if(from.contains("/")) //$NON-NLS-1$
				from = from.split("/")[1]; //$NON-NLS-1$
			
			if(!from.equals(controlRoom.getNickname())){//If the message didn't come from me 
				
				//React to commands thread all of this if performance is a problem
				LOGGER.info(xStrings.getString("AsteriskManager.receivedMessage") + //$NON-NLS-1$
						message.getBody());
				
				String[] command = message.getBody().split("/"); //$NON-NLS-1$
				
				if(command.length == 3 && command[0].equals(
								xStrings.getString("AsteriskManager.commandTransfer"))){ //$NON-NLS-1$
					
					//Received a transfer command
					redirectCall(command[1], command[2], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandQueue"))){ //$NON-NLS-1$
					
					//Received a command to put the call into the on air queue
					redirectCallToQueue(command[1], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandUpdate"))){ //$NON-NLS-1$
				
					//Send updates to the person who asked for it (usually when they login)
					sendChannelInfo(from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandHangup"))){ //$NON-NLS-1$
					
					hangupCall(command[1], from);
					
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.commandDial"))){ //$NON-NLS-1$
					
					createCall(command[1], command[2], from);
					
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.commandTransferEndPoint"))){ //$NON-NLS-1$
					
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
					
					sendMessage(xStrings.getString("AsteriskManager.commandEndPoint") + "/" + command[1] + "/"  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
					 + activeChannels.get(command[1]).getLinkedChannel().getId() + "/"  //$NON-NLS-1$
					 + endPointCallerID);
					
					/* BUG FIX: Store in expectHangup so that when channel gets dropped
					 * we don't remove it from the calls hash map
					 */
					expectHangup.add(endPointCallerID);
					
					redirectCallToQueue(activeChannels.get(command[1]).getLinkedChannel()
							.getId(), from);
					
				}else if(command.length == 4 && command[0].equals(
						xStrings.getString("AsteriskManager.commandUpdateField"))){ //$NON-NLS-1$
					
					//Spawn a thread to do a DB update
					dbLookUpService.execute(new RecordUpdater(settings.get("language"),  //$NON-NLS-1$
							settings.get("country"), databaseManager.getReadConnection(), //$NON-NLS-1$
							databaseManager.getWriteConnection(), command[1], 
							command[2], command[3]));
				
				}else if(command.length == 2 && command[0].equals(
						xStrings.getString("AsteriskManager.ChangeToNewPerson"))){ //$NON-NLS-1$
					
					//Make a new person, update call references and then send back 
					//XMPP Reply so clients can update
					dbLookUpService.execute(new PersonChanger(settings.get("language"),  //$NON-NLS-1$
							settings.get("country"), databaseManager.getReadConnection(), //$NON-NLS-1$
							databaseManager.getWriteConnection(), controlRoom, 
							-1, command[1]));
					
				}else if(command.length == 3 && command[0].equals(
						xStrings.getString("AsteriskManager.ChangeToExistingPerson"))){ //$NON-NLS-1$
					
					//Update call references and then send back XMPP Reply so
					//clients can update
					dbLookUpService.execute(new PersonChanger(settings.get("language"),  //$NON-NLS-1$
							settings.get("country"), databaseManager.getReadConnection(), //$NON-NLS-1$
							databaseManager.getWriteConnection(), controlRoom, 
							Integer.parseInt(command[2]), command[1])); 
					
				}
				
			}
			
		}else if(startup)
			LOGGER.info(xStrings.getString("AsteriskManager.receivedXMPPWhileStarting")); //$NON-NLS-1$
		
		
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
			 * the id to check 
			 */
			String[] temp = to.split("/"); //$NON-NLS-1$
			
			if((dialPrefix + temp[0]).equals(callerID))
				removePrefix = true;
			
		}
		
		return removePrefix;
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		if(evt.getPropertyName().equals("state") && //$NON-NLS-1$
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
					
					String message = xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							"/" + callerID + "/" + ringing.getId(); //$NON-NLS-1$ //$NON-NLS-2$
					LOGGER.info(message);
					sendMessage(message);
					
				}else if(state.getStatus() == ChannelState.HUNGUP.getStatus()){
					
					AsteriskChannel hangup = (AsteriskChannel)evt.getSource();
					
					/* Remove any old dialled calls in progress unless we're expecting
					 * a hang up on this CallerID due to TRANSFERENDPOINT */
					if(expectHangup.size() > 0){ //Check we're not expecting this to hangup
						
						boolean expected = false;
						int i = 0;
						
						while(!expected && i < expectHangup.size()){
						
							String hangupCallerID = 
									hangup.getCallerId().getNumber();
							
							if(removePrefix(hangupCallerID))
								hangupCallerID = hangupCallerID.substring(
										dialPrefix.length());
							
							if(hangupCallerID.equals(expectHangup.get(i))){
								expected = true;
								
								//Remove it as we don't need it anymore
								expectHangup.remove(i);
								
							}
							
						}
						
						if(!expected && calls.size() > 0){
							
							Iterator<String> keys = calls.keySet().iterator();
							boolean done = false;
							
							while(keys.hasNext() && !done){
								
								String key = keys.next();
								String to = calls.get(key);
								
								if(to.contains(hangup.getId())){
									calls.remove(key);
									done = true;
								}
								
							}
							
						}
						
					}
					
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
						logCause = xStrings.getString("AsteriskManager.channelHangup"); //$NON-NLS-1$
						/* We want to log normal hang ups as part of a normal call life cycle
						 * but not the errors as we're only interested complete calls from outside
						 */
						logHangup = true; 
						
					}else if(hangupCause.equals(HANGUP_OFFLINE))
						logCause = xStrings.getString("AsteriskManager.channelHangupOffline"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_UNACCEPTABLE))
						logCause = xStrings.getString("AsteriskManager.channelHangupUnacceptable"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_ANSWERED_ELSEWHERE))
						logCause = xStrings.getString("AsteriskManager.channelHangupAnsweredElsewhere"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_USER_BUSY))
						logCause = xStrings.getString("AsteriskManager.channelHangupUserBusy"); //$NON-NLS-1$
					else
						logCause = xStrings.getString("AsteriskManager.channelHangup") + " "   //$NON-NLS-1$//$NON-NLS-2$
								+ hangupCause;
					
					//Remove the dial out prefix from callerID if this is a call we dialled
					String callerID = checkNumberWithHeld(hangup.getCallerId());
					
					if(removePrefix(callerID))
						callerID = callerID.substring(dialPrefix.length());
					
					//Log this in the DB
					if(logHangup)
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								callerID, hangup.getId(), this, 
								'H', "NA")); //$NON-NLS-1$
						
					/* BUG FIX: Hangup Cause sometimes has a slash in it which messes with our / command separator
					 * HANGUP Circuit/channel congestion as one example there may be others so lets deal with it
					 */
					hangupCause = hangupCause.replaceAll("/", "-"); //$NON-NLS-1$ //$NON-NLS-2$
					//END OF BUG FIX
					
					//Send XMPP Message
					String message = logCause + "/" + callerID + //$NON-NLS-1$
							"/" + hangup.getId();  //$NON-NLS-1$
					LOGGER.info(message);
					sendMessage(message);
					removeActiveChannel(hangup.getId());
					
				}else if(state.getStatus() == ChannelState.BUSY.getStatus()){
					
					AsteriskChannel busy = (AsteriskChannel)evt.getSource();
					
					String message = xStrings.getString("AsteriskManager.channelBusy") + //$NON-NLS-1$
							"/" + checkNumberWithHeld(busy.getCallerId()) + "/" + busy.getId(); //$NON-NLS-1$ //$NON-NLS-2$
					
					LOGGER.info(message);
					sendMessage(message);
					
				}
				
			}
			
			/* Other events we can ignore:
			 * oldValue DOWN && newValue UP == channel bought online
			 * oldValue RING && newValue UP == channel online prior to connecting call
			 * This is followed by a link event which confirms channels are connected
			 */
			
		}else if(evt.getPropertyName().equals("currentExtension") && //$NON-NLS-1$
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
					/*if(channel.getCallerId().getNumber() != null && 
							!channel.getCallerId().getNumber().equals("null")){ //$NON-NLS-1$ */
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
						
						String message = xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
								"/" + callerID + "/" +  //$NON-NLS-1$ //$NON-NLS-2$
								extensionCalling + "/" + channel.getId(); //$NON-NLS-1$
						
						/* Track Incoming
						 * We need to get an active person if possible or create a skeleton
						 * record if its someone new before the clients start processing
						 * and things go crazy
						 */
						/* At this point it could be a call from this channel that we're making to 
						 * someone else.  Ideally we'd like to have the panel show up with the right
						 * person attached to it but in order to do that we need to swap this channels
						 * caller id with the id of the channel we're trying to call
						 * 
						 * On Dial store the channel stuff so we can substitute here?
						 */
						
						if(calls.containsKey(callerID)){
							
							String to = calls.get(callerID);
							calls.put(callerID, to + "/" + channel.getId()); //$NON-NLS-1$
							
						}
						
						dbLookUpService.execute(new PhoneCall(callerID, 
								channel.getId(), databaseManager));
					
						LOGGER.info(message);
						sendMessage(message);
					
					//} //TODO Check this
					
				}
				
			}
			
		}else if(evt.getPropertyName().equals("linkedChannel") &&  //$NON-NLS-1$
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
					
					String message = xStrings.getString("AsteriskManager.callConnected") +  //$NON-NLS-1$
							"/" + callerID + "/" + //$NON-NLS-1$ //$NON-NLS-2$
							linkedTo + "/" + channel.getId(); //$NON-NLS-1$
					
					if(systemExtensions.contains(linkedTo))//if we're linked to a system phone
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								callerID, channel.getId(), this, 'A', "NA")); //$NON-NLS-1$
					
					LOGGER.info(message);
					sendMessage(message);
					
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
