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
import org.asteriskjava.live.DefaultAsteriskServer;
import org.asteriskjava.live.ManagerCommunicationException;
import org.asteriskjava.live.MeetMeUser;
import org.asteriskjava.live.internal.AsteriskAgentImpl;

public class AsteriskManager implements AsteriskServerListener, PropertyChangeListener {

	//STATICS
	private static final Logger ASTERISK_LOGGER = Logger.getLogger("org.asteriskjava"); //$NON-NLS-1$
	
	//CLASS VARS
	private AsteriskServer asteriskServer;
	
	public AsteriskManager(){
		
		//Turn off AsteriskJava logger
		ASTERISK_LOGGER.setLevel(Level.SEVERE);
		asteriskServer = new DefaultAsteriskServer("10.43.10.91", "phonemanager", "P0l0m1nt");
		
	}
	
	/**
	 * Initialises and connects to Asterisk server
	 * @throws ManagerCommunicationException
	 */
	public void connect() throws ManagerCommunicationException {
		
		asteriskServer.initialize();
		asteriskServer.addAsteriskServerListener(this);
		
	}
	
	/**
	 * Disconnects from Asterisk server
	 */
	public void disconnect() {
		
		asteriskServer.shutdown();
		
	}

	public void showChannels(){
		
		for (AsteriskChannel asteriskChannel : asteriskServer.getChannels()) {
            System.out.println(asteriskChannel);
            asteriskChannel.addPropertyChangeListener(this);
            
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
		
		//TODO Registers a new channel, need a listener on each channel
		System.out.println(channel);
		channel.addPropertyChangeListener(this);
		
		
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
