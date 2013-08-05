package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.AsteriskQueue;
import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.AsteriskServer;
import org.asteriskjava.live.AsteriskServerListener;
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.internal.AsteriskAgentImpl;

public class AsteriskManager implements AsteriskServerListener, PropertyChangeListener {

	//STATICS
	private static final Logger AST_LOGGER = Logger.getLogger("org.asteriskjava"); //$NON-NLS-1$
	private static final Logger LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.asterisk"); //$NON-NLS-1$
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	private Set<AsteriskChannel> activeChannels = Collections.synchronizedSet(new HashSet<AsteriskChannel>());
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
			activeChannels.add(asteriskChannel);
            
        }
		
	}
	
	//TODO showC + Q not required?
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
	
	/** AsteriskServerListener **/
	@Override
	public void onNewAsteriskChannel(AsteriskChannel channel) {
		
		//Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info(xStrings.getString("AsteriskManager.newChannel") + channel.getId()); //$NON-NLS-1$
		channel.addPropertyChangeListener(this);
		System.out.println(channel);
		activeChannels.add(channel);
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//TODO Waiting for studio goes to queue
		System.out.println(entry);
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		System.out.println("PROPCHANGE: " + evt); //$NON-NLS-1$
		
		//TODO branch depending on whats happening
		
		if(evt.getPropertyName().equals("state") && //$NON-NLS-1$
				evt.getSource() instanceof AsteriskChannel){ 
			
			
			if(evt.getNewValue().equals("RING")){ //$NON-NLS-1$
				
				AsteriskChannel ringing = (AsteriskChannel)evt.getSource();
				System.out.println("RINGING: " + ringing.getCallerId().getNumber()); //$NON-NLS-1$
				
			}
			
		}
		
	}
	
	/** UNUSED AsteriskServerListener methods **/
	@Override
	public void onNewMeetMeUser(MeetMeUser user) {}

	@Override
	public void onNewAgent(AsteriskAgentImpl agent) {}

	
	
}
