package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
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

public class AsteriskManager implements AsteriskServerListener, PropertyChangeListener, OriginateCallback {

	//STATICS
	private static final Logger AST_LOGGER = Logger.getLogger("org.asteriskjava"); //$NON-NLS-1$
	private static final Logger LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.asterisk"); //$NON-NLS-1$
	private static final String HANGUP_NORMAL = "Normal Clearing"; //$NON-NLS-1$
	private static final String HANGUP_UNACCEPTABLE = "Channel unacceptable"; //$NON-NLS-1$
	private static final String HANGUP_OFFLINE = "Subscriber absent"; //$NON-NLS-1$
	private static final String HANGUP_USER_BUSY = "User busy"; //$NON-NLS-1$
	private static final String HANGUP_ANSWERED_ELSEWHERE = "Answered elsewhere"; //$NON-NLS-1$
	private static final String CONTEXT_MACRO_AUTO = "macro-auto-blkvm"; //$NON-NLS-1$
	private static final String AUTO_ANSWER_CONTEXT = "custom-answerme"; //$NON-NLS-1$
	private static final String SIP_PREFIX = "Local/"; //$NON-NLS-1$
	private static final int DEFAULT_PRIORITY = 1;
	private static final long DEFAULT_TIMEOUT = 30000L;
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	private HashMap<String, AsteriskChannel> activeChannels = new HashMap<String, AsteriskChannel>();
	private I18NStrings xStrings;
	
	/**
	 * Creates a new Asterisk Manager instance that handles events and sends commands via XMPP
	 * to any clients listening
	 * @param language language for strings used to set locale
	 * @param country country for strings used to set locale
	 * @param asteriskHost host name/ip for Asterisk server
	 * @param asteriskUser user name for Asterisk Manager Connection
	 * @param asteriskPass password for Asterisk Manager Connection
	 */
	public AsteriskManager(String language, String country, String asteriskHost, 
			String asteriskUser, String asteriskPass){
		
		//Turn off AsteriskJava logger for all but SEVERE
		AST_LOGGER.setLevel(Level.SEVERE);
		
		xStrings = new I18NStrings(language, country);
		
		asteriskServer = new DefaultAsteriskServer(asteriskHost, asteriskUser, asteriskPass);
		
	}
	
	/**
	 * Initialises and connects to Asterisk server
	 * @throws ManagerCommunicationException
	 */
	public void connect() throws ManagerCommunicationException {
		
		asteriskServer.initialize();
		asteriskServer.addAsteriskServerListener(this);
		getChannels();
		
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
            
			LOGGER.info(xStrings.getString("AsteriskManager.startupActiveChannels") + asteriskChannel.getId()); //$NON-NLS-1$
			asteriskChannel.addPropertyChangeListener(this);
			addActiveChannel(asteriskChannel);
			System.out.println(asteriskChannel);
			
        }
		
	}
	
	private synchronized void addActiveChannel(AsteriskChannel channel){
	
		activeChannels.put(channel.getId(), channel);
		//System.out.println(activeChannels.size());
		
	}
	
	private synchronized void removeActiveChannel(String channelID){
		
		activeChannels.remove(channelID);
		//System.out.println(activeChannels.size());
		
	}
	
	//Necessary when new clients join but not needed as public, rework this to send XMPP update
	//TODO need to decide what is happening on channels and send update via XMPP
	public void showChannels(){
		
		for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
            System.out.println(asteriskChannel);
        }
		
	}
	
	public void showQueues(){
		
		for (AsteriskQueue asteriskQueue : asteriskServer.getQueues()) {
            System.out.println(asteriskQueue);
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
				AUTO_ANSWER_CONTEXT,  AUTO_ANSWER_CONTEXT, to, DEFAULT_PRIORITY, DEFAULT_TIMEOUT, 
				new CallerId(fromName, fromNumber), vars, this);
		
	}
	
	/**
	 * Redirects the given channel to the given extension
	 * @param channelID Channel to redirect
	 * @param to extension to send channel to
	 */
	public void redirectCall(String channelID, String to){
	
		AsteriskChannel channel = activeChannels.get(channelID);
		
		if(channel != null)//null = channel not found
			channel.redirect(AUTO_ANSWER_CONTEXT, to, DEFAULT_PRIORITY);
		
	}
	
	public void hangupCall(String channelID){
		
		AsteriskChannel channel = activeChannels.get(channelID);
		
		if(channel != null)
			channel.hangup();
		
	}
	
	
	/** AsteriskServerListener **/
	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info(xStrings.getString("AsteriskManager.newChannel") + channel.getId()); //$NON-NLS-1$
		channel.addPropertyChangeListener(this);
		addActiveChannel(channel);
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//TODO Waiting for studio goes to queue
		System.out.println(entry);
		
	}

	/* TODO Transfer to queue
	 * XMPP CHAT Messages
	 * XMPP Extension with Asterisk Info, Class it for easy messing around
	 * XMPP Control Chat Toggle for debugging?
	 */
	/**
	 * Sends control message to XMPP control chat room
	 * @param message
	 */
	private void sendMessage(String message){
		
		//TODO Send Control Message
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		
		System.out.println("PROPCHANGE: " + evt); //$NON-NLS-1$
		
		if(evt.getPropertyName().equals("state") && //$NON-NLS-1$
				evt.getSource() instanceof AsteriskChannel){ 
			
			if(evt.getNewValue() instanceof ChannelState){
				
				ChannelState state = (ChannelState)evt.getNewValue();
				
				if(state.getStatus() == ChannelState.RING.getStatus()){ 
					
					//This channel is dialling out and listening to it ringing
					AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber() + " " + ringing.getId()); //$NON-NLS-1$
					sendMessage(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber() + " " + ringing.getId()); //$NON-NLS-1$
					
				}else if(state.getStatus() == ChannelState.RINGING.getStatus()){
					
					//Someone is ringing this channel
					AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber() + " " + ringing.getId()); //$NON-NLS-1$
					sendMessage(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber() + " " + ringing.getId()); //$NON-NLS-1$
					
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
					
					if(hangupCause.equals(HANGUP_NORMAL))
						logCause = xStrings.getString("AsteriskManager.channelHangup"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_OFFLINE))
						logCause = xStrings.getString("AsteriskManager.channelHangupOffline"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_UNACCEPTABLE))
						logCause = xStrings.getString("AsteriskManager.channelHangupUnacceptable"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_ANSWERED_ELSEWHERE))
						logCause = xStrings.getString("AsteriskManager.channelHangupAnsweredElsewhere"); //$NON-NLS-1$
					else if(hangupCause.equals(HANGUP_USER_BUSY))
						logCause = xStrings.getString("AsteriskManager.channelHangupUserBusy"); //$NON-NLS-1$
					else
						logCause = hangupCause;
					
					LOGGER.info(logCause + hangup.getCallerId().getNumber() + " " + hangup.getId()); //$NON-NLS-1$
					sendMessage(logCause + hangup.getCallerId().getNumber() + " " + hangup.getId()); //$NON-NLS-1$
					removeActiveChannel(hangup.getId());
					
				}else if(state.getStatus() == ChannelState.BUSY.getStatus()){
					
					AsteriskChannel busy = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.channelBusy") +  //$NON-NLS-1$
							busy.getCallerId().getNumber() + " " + busy.getId()); //$NON-NLS-1$
					sendMessage(xStrings.getString("AsteriskManager.channelBusy") +  //$NON-NLS-1$
							busy.getCallerId().getNumber() + " " + busy.getId()); //$NON-NLS-1$
					
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
				if(!calling.getContext().equals(CONTEXT_MACRO_AUTO)){
					
					AsteriskChannel channel = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
							channel.getCallerId().getNumber() + " " +  //$NON-NLS-1$
							xStrings.getString("AsteriskManager.callRingingTo") + //$NON-NLS-1$
							calling.getExtension() + " " + channel.getId()); //$NON-NLS-1$
					sendMessage(xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
							channel.getCallerId().getNumber() + " " +  //$NON-NLS-1$
							xStrings.getString("AsteriskManager.callRingingTo") + //$NON-NLS-1$
							calling.getExtension() + " " + channel.getId()); //$NON-NLS-1$
					
				}
				
			}
			
		}else if(evt.getPropertyName().equals("linkedChannel") &&  //$NON-NLS-1$
				evt.getSource() instanceof AsteriskChannel){
			
			//Linked Channel = connected (can also be disconnected)
			AsteriskChannel channel = (AsteriskChannel)evt.getSource();
			
			if(evt.getNewValue() != null){//null = unlinking, not null = linking
				
				String linkedTo = getNumberFromChannelName(channel.getLinkedChannel().getName());
				
				LOGGER.info(xStrings.getString("AsteriskManager.callConnected") +  //$NON-NLS-1$
						channel.getCallerId().getNumber() + " " + //$NON-NLS-1$
						xStrings.getString("AsteriskManager.callConnectedTo") + //$NON-NLS-1$
						linkedTo + " " + channel.getId()); //$NON-NLS-1$
				sendMessage(xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
						channel.getCallerId().getNumber() + " " + //$NON-NLS-1$
						xStrings.getString("AsteriskManager.callConnectedTo") + //$NON-NLS-1$
						linkedTo + " " + channel.getId()); //$NON-NLS-1$
				
			}
			
		}
		
	}
	
	/**
	 * Splits standard channel name into its extension in the form of:
	 * SIP/6001-blahblah 
	 * to 6001
	 * @param name
	 * @return
	 */
	private String getNumberFromChannelName(String name){
		
		String number = name;
		
		if(number.contains("/")){ //$NON-NLS-1$
		
			number = number.split("/")[1]; //$NON-NLS-1$
			
			if(number.contains("-")) //$NON-NLS-1$
				number = number.split("-")[0]; //$NON-NLS-1$
			
			name = number;
			
		}
		
		return name;
		
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
