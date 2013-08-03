package com.thevoiceasia.phonebox.asterisk;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
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
	private static final Logger LOGGER = Logger.getLogger("org.asteriskjava"); //$NON-NLS-1$
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	private HashSet<AsteriskChannel> activeChannels = 
			(HashSet<AsteriskChannel>)Collections.synchronizedSet(new HashSet<AsteriskChannel>());
	
	public AsteriskManager(){
		
		//Turn off AsteriskJava logger for all but SEVERE
		LOGGER.setLevel(Level.SEVERE);
		
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
            
			asteriskChannel.addPropertyChangeListener(this);
			activeChannels.add(asteriskChannel);
            
        }
		
	}
	
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
		
		//TODO Registers a new channel, need a listener on each channel and keep track of them
		LOGGER.info("");
		
		//TODO xStrings etc
		
	}

	@Override
	public void onNewQueueEntry(AsteriskQueueEntry entry) {
		
		//TODO
		System.out.println(entry);
		
	}
	
	/** PropertyChangeListener **/
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		// TODO Auto-generated method stub
		System.out.println(evt);
	}
	
	/** UNUSED AsteriskServerListener methods **/
	@Override
	public void onNewMeetMeUser(MeetMeUser user) {}

	@Override
	public void onNewAgent(AsteriskAgentImpl agent) {}

	
	
}
