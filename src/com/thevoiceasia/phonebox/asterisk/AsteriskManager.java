package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueue;
import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
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
	private static final String HANGUP_ANSWERED_ELSEWHERE = "Answered elsewhere"; //$NON-NLS-1$
	private static final String CONTEXT_MACRO_AUTO = "macro-auto-blkvm"; //$NON-NLS-1$
	private static final String DEFAULT_CONTEXT = "from-internal"; //$NON-NLS-1$
	private static final String SIP_PREFIX = "SIP/"; //$NON-NLS-1$
	private static final int DEFAULT_PRIORITY = 1;
	private static final long DEFAULT_TIMEOUT = 30000L;
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	//private Set<AsteriskChannel> activeChannels = Collections.synchronizedSet(new HashSet<AsteriskChannel>());
	private I18NStrings xStrings; 
	
	public AsteriskManager(String language, String country){
		
		//Turn off AsteriskJava logger for all but SEVERE
		AST_LOGGER.setLevel(Level.SEVERE);
		
		xStrings = new I18NStrings(language, country);
		
		//TODO read settings properly
		asteriskServer = new DefaultAsteriskServer("10.43.10.91", "phonemanager", "P0l0m1nt");  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		
	}
	
	/**
	 * Initialises and connects to Asterisk server
	 * @throws ManagerCommunicationException
	 */
	public void connect() throws ManagerCommunicationException {
		
		asteriskServer.initialize();
		asteriskServer.addAsteriskServerListener(this);
		getChannels();
		createCall("907886031657", "6002");//to, from
		
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
			
        }
		
	}
	
	//TODO showC + Q not required?
	//Necessary when new clients join but not needed as public, rework this to send XMPP update
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
	
	public void createCall(String to, String from){
		
		asteriskServer.originateToExtensionAsync(SIP_PREFIX + from, DEFAULT_CONTEXT, to, 
				DEFAULT_PRIORITY, DEFAULT_TIMEOUT, this);
		
	}
	
	/** AsteriskServerListener **/
	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info(xStrings.getString("AsteriskManager.newChannel") + channel.getId()); //$NON-NLS-1$
		channel.addPropertyChangeListener(this);
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//TODO Waiting for studio goes to queue
		System.out.println(entry);
		
	}

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
							ringing.getCallerId().getNumber());
					sendMessage(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber());
					
				}else if(state.getStatus() == ChannelState.RINGING.getStatus()){
					
					//Someone is ringing this channel
					AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber());
					sendMessage(xStrings.getString("AsteriskManager.channelRinging") +  //$NON-NLS-1$
							ringing.getCallerId().getNumber());
					
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
					else
						logCause = hangupCause;
					
					LOGGER.info(logCause + hangup.getCallerId().getNumber());
					sendMessage(logCause + hangup.getCallerId().getNumber());
					
				}else if(state.getStatus() == ChannelState.BUSY.getStatus()){
					
					AsteriskChannel busy = (AsteriskChannel)evt.getSource();
					
					LOGGER.info(xStrings.getString("AsteriskManager.channelBusy") +  //$NON-NLS-1$
							busy.getCallerId().getNumber());
					sendMessage(xStrings.getString("AsteriskManager.channelBusy") +  //$NON-NLS-1$
							busy.getCallerId().getNumber());
					
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
							calling.getExtension());
					sendMessage(xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
							channel.getCallerId().getNumber() + " " +  //$NON-NLS-1$
							xStrings.getString("AsteriskManager.callRingingTo") + //$NON-NLS-1$
							calling.getExtension());
					
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
						linkedTo);
				sendMessage(xStrings.getString("AsteriskManager.callRingingFrom") +  //$NON-NLS-1$
						channel.getCallerId().getNumber() + " " + //$NON-NLS-1$
						xStrings.getString("AsteriskManager.callConnectedTo") + //$NON-NLS-1$
						linkedTo);
				
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

	@Override
	public void onBusy(AsteriskChannel channel) {
		// TODO Auto-generated method stub
		System.out.println("\nBUSY: " + channel);
		
	}

	@Override
	public void onDialing(AsteriskChannel channel) {
		// TODO Auto-generated method stub
		System.out.println("\nDIALING: " + channel);
		
	}

	@Override
	public void onFailure(LiveException exception) {
		// TODO Auto-generated method stub
		System.out.println("\nFAIL: " + exception);
		
	}

	@Override
	public void onNoAnswer(AsteriskChannel channel) {
		// TODO Auto-generated method stub
		System.out.println("\nNO ANSWER: " + channel);
		
		
	}

	@Override
	public void onSuccess(AsteriskChannel channel) {
		// TODO Auto-generated method stub
		System.out.println("\nCONNECTED: " + channel);
		
	}
	
}
