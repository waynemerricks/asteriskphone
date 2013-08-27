package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
import com.thevoiceasia.phonebox.records.PhoneCall;

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
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	private HashMap<String, AsteriskChannel> activeChannels = new HashMap<String, AsteriskChannel>();
	private HashMap<String, Long> lockedChannels = new HashMap<String, Long>();
	private I18NStrings xStrings;
	private String autoAnswerContext, defaultContext, contextMacroAuto, queueNumber;
	private long defaultTimeOut, channelLockTimeOut;
	private MultiUserChat controlRoom;
	private DatabaseManager databaseManager;
	private HashSet<String> systemExtensions = new HashSet<String>();
	
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
		HashMap<String, String> settings = databaseManager.getUserSettings();
		this.autoAnswerContext = settings.get("autoAnswerContext"); //$NON-NLS-1$
		this.defaultContext = settings.get("defaultContext"); //$NON-NLS-1$
		this.contextMacroAuto = settings.get("contextMacroAuto"); //$NON-NLS-1$
		this.queueNumber = settings.get("onAirQueueNumber"); //$NON-NLS-1$
		this.defaultTimeOut = Long.parseLong(settings.get("defaultTimeOut")); //$NON-NLS-1$
		this.maxExecutorThreads = Integer.parseInt(settings.get("threadPoolMax")); //$NON-NLS-1$
		this.channelLockTimeOut = Long.parseLong(settings.get("channelLockTimeOut")); //$NON-NLS-1$
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
            
			if(orderedChannels.size() == 0)
				orderedChannels.add(asteriskChannel);
			else{
				
				if(asteriskChannel.getLinkedChannel() != null && 
	            		systemExtensions.contains(asteriskChannel.getLinkedChannel()
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
            			+ "/" + orderedChannels.get(i).getId(); //$NON-NLS-1$
            	
            	//System.out.println(command);
            	sendPrivateMessage(recipient, command);
				
			}
			
        }
		
		for(AsteriskQueue asteriskQueue : asteriskServer.getQueues()){

			//System.out.println("QUEUE: " + asteriskQueue);
			
			for(AsteriskQueueEntry entry : asteriskQueue.getEntries()){
				
				//CALL/5003/3000/1377009449.5
				String command = xStrings.getString("AsteriskManager.callRingingFrom") + "/"   //$NON-NLS-1$//$NON-NLS-2$
						+ entry.getChannel().getCallerId().getNumber() + "/"  //$NON-NLS-1$
						+ asteriskQueue.getName() + "/"  //$NON-NLS-1$
						+ entry.getChannel().getId();
				
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
		
		HashMap<String, String> vars = new HashMap<String, String>();
		asteriskServer.originateToExtensionAsync(SIP_PREFIX + fromNumber + "@" +  //$NON-NLS-1$
				autoAnswerContext,  autoAnswerContext, to, DEFAULT_PRIORITY, defaultTimeOut, 
				new CallerId(fromName, fromNumber), vars, this);
		
	}
	
	/**
	 * Redirects the given channel to the given extension
	 * @param channelID Channel to redirect
	 * @param to extension to send channel to
	 */
	public void redirectCall(String channelID, String to, String from){
	
		AsteriskChannel channel = activeChannels.get(channelID);
		
		if(channel != null && !isLocked(channel))//null = channel not found
			channel.redirect(autoAnswerContext, to, DEFAULT_PRIORITY);
		
		String callerNumber = channel.getCallerId().getNumber();
		
		if(callerNumber.length() >= 7 || callerNumber.equals("5003"))//DEBUG 5003 DEBUG //$NON-NLS-1$
			dbLookUpService.execute(new PhoneCall(databaseManager, 
					channel.getCallerId().getNumber(), channel.getId(), this, 'A', from));
		
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
		
		if(channel != null)
			channel.redirect(defaultContext, queueNumber, DEFAULT_PRIORITY);
		
		dbLookUpService.execute(new PhoneCall(databaseManager, 
				channel.getCallerId().getNumber(), channel.getId(), this, 'Q', from));
		
	}
	
	/**
	 * Sends an asterisk Command to hang up the given channel
	 * @param channelID channel to hang up
	 * @param from person who initiated hang up (for DB tracking purposes)
	 */
	public void hangupCall(String channelID, String from){
		
		AsteriskChannel channel = activeChannels.get(channelID);
		
		if(channel != null)
			channel.hangup();
		
		dbLookUpService.execute(new PhoneCall(databaseManager, 
				channel.getCallerId().getNumber(), channel.getId(), this, 'H', from));
		
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
		
		dbLookUpService.execute(new PhoneCall(databaseManager, entry, this));
		
	}

	/**
	 * Sends a message to the XMPP control room that we have a new QueueEntry
	 * @param entry
	 */
	public void sendNewQueueEntryMessage(AsteriskQueueEntry entry){
		
		//Transfers from handlers goes into Studio Queue
		String message = xStrings.getString("AsteriskManager.populatedQueueEntry") + "/" + //$NON-NLS-1$ //$NON-NLS-2$
				entry.getQueue().getName() + "/" + entry.getChannel().getCallerId() //$NON-NLS-1$
				.getNumber() + "/" + entry.getChannel().getId(); //$NON-NLS-1$
		
		LOGGER.info(message);
		sendMessage(message);
		
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
		
		if(XMPPPacket instanceof Message){
			
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
					
				}
				
			}
			
		}	
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		//System.out.println("PROPCHANGE: " + evt); //$NON-NLS-1$
		
		if(evt.getPropertyName().equals("state") && //$NON-NLS-1$
				evt.getSource() instanceof AsteriskChannel){ 
			
			if(evt.getNewValue() instanceof ChannelState){
				
				ChannelState state = (ChannelState)evt.getNewValue();
				
				if(state.getStatus() == ChannelState.RING.getStatus() || 
						state.getStatus() == ChannelState.RINGING.getStatus()){ 
					
					//This channel is dialling out and listening to it ringing
					AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
					
					String message = xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							"/" + ringing.getCallerId().getNumber() + "/" + ringing.getId(); //$NON-NLS-1$ //$NON-NLS-2$
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
						logCause = hangupCause;
					
					//Log this in the DB
					if(logHangup)
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								hangup.getCallerId().getNumber(), hangup.getId(), this, 'H', "NA")); //$NON-NLS-1$
					
					//Send XMPP Message
					String message = logCause + "/" + hangup.getCallerId().getNumber() + "/" + //$NON-NLS-1$ //$NON-NLS-2$
							hangup.getId(); 
					LOGGER.info(message);
					sendMessage(message);
					removeActiveChannel(hangup.getId());
					
				}else if(state.getStatus() == ChannelState.BUSY.getStatus()){
					
					AsteriskChannel busy = (AsteriskChannel)evt.getSource();
					
					String message = xStrings.getString("AsteriskManager.channelBusy") + //$NON-NLS-1$
							"/" + busy.getCallerId().getNumber() + "/" + busy.getId(); //$NON-NLS-1$ //$NON-NLS-2$
					
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
					
					if(channel.getCallerId().getNumber() != null && 
							!channel.getCallerId().getNumber().equals("null")){ //$NON-NLS-1$
						
						String message = xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
								"/" + channel.getCallerId().getNumber() + "/" +  //$NON-NLS-1$ //$NON-NLS-2$
								calling.getExtension() + "/" + channel.getId(); //$NON-NLS-1$
						
						LOGGER.info(message);
						sendMessage(message);
					
					}
					
				}
				
			}
			
		}else if(evt.getPropertyName().equals("linkedChannel") &&  //$NON-NLS-1$
				evt.getSource() instanceof AsteriskChannel){
			
			//Linked Channel = connected (can also be disconnected)
			AsteriskChannel channel = (AsteriskChannel)evt.getSource();
			
			if(evt.getNewValue() != null){//null = unlinking, not null = linking
				
				String linkedTo = channel.getLinkedChannel().getCallerId().getNumber(); 
				
				if(!linkedTo.equals(channel.getCallerId().getNumber())){
					
					String message = xStrings.getString("AsteriskManager.callConnected") +  //$NON-NLS-1$
							"/" + channel.getCallerId().getNumber() + "/" + //$NON-NLS-1$ //$NON-NLS-2$
							linkedTo + "/" + channel.getId(); //$NON-NLS-1$
					
					if(systemExtensions.contains(linkedTo))//if we're linked to a system phone
						dbLookUpService.execute(new PhoneCall(databaseManager, 
								channel.getCallerId().getNumber(), channel.getId(), this, 'A', "NA")); //$NON-NLS-1$
					
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
