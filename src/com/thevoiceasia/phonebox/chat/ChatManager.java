package com.thevoiceasia.phonebox.chat;

import java.io.IOException;
import java.util.Date;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.UserStatusListener;

import com.thevoiceasia.phonebox.misc.LastActionTimer;

public class ChatManager implements UserStatusListener, PacketListener {

	//XMPP Settings
	private String XMPPUserName, XMPPPassword, XMPPNickName, XMPPServerHostName, XMPPRoomName;
	private int XMPPChatHistory, idleTimeout;
	
	//XMPP Connections/Rooms
	private Connection XMPPServerConnection;
	private MultiUserChat phoneboxChat;
	
	//ChatManager vars
	private boolean hasErrors = false; //Error Flag used internally
	private I18NStrings xStrings; //Link to external string resources
	private Vector<LastActionTimer> actionTimers = new Vector<LastActionTimer>();
	private boolean available = true;
	private IdleCheckThread idleCheckThread;
	
	/** STATICS **/
	private static final int XMPP_CHAT_HISTORY = 600; //Chat Messages in the last x seconds
	private static final Logger LOGGER = Logger.getLogger(ChatManager.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.WARNING;
	
	/**
	 * Create ChatManager with given locale
	 * 
	 * Will login to the XMPP server given with the following details
	 * @param userName
	 * @param password
	 * @param nickName
	 * @param serverHostName
	 * @param roomName
	 * @param language
	 * @param country
	 */
	public ChatManager(String userName, String password, String nickName, 
			String serverHostName, String roomName, String language, String country, int idleTimeout){
		
		//Get I18N handle for external strings
		xStrings = new I18NStrings(language, country);
		this.XMPPUserName = userName;
		this.XMPPPassword = password;
		this.XMPPServerHostName = serverHostName;
		this.XMPPRoomName = roomName;
		this.XMPPChatHistory = XMPP_CHAT_HISTORY;
		this.XMPPNickName = nickName;
		this.idleTimeout = idleTimeout;
		setupLogging();
		
	}
	
	
	public void startIdleDetectThread(){
	
		LOGGER.info(xStrings.getString("ChatManager.logStartIdleDetect")); //$NON-NLS-1$
		idleCheckThread = new IdleCheckThread(this);
		idleCheckThread.start();
		
	}
	
	/**
	 * Notifies this ChatManager that here is an object that receives user input
	 * as such this object has a time since last action variable.
	 * 
	 * This is used for a simple idle timer so that status is set to away if required
	 * @param lastAction
	 */
	public void addActionTimeRecorder(LastActionTimer lastActionTimer, String name){
		
		LOGGER.info(xStrings.getString("ChatManager.logAddActionTimer") + name); //$NON-NLS-1$
		actionTimers.add(lastActionTimer);
		
	}
	
	public void checkIdle(){
		
		LOGGER.info(xStrings.getString("ChatManager.logCheckingIdle")); //$NON-NLS-1$
		
		if(actionTimers.size() > 0){
			
			int i = 0;
			boolean alive = false;
			long now = new Date().getTime();
			
			while(!alive && i < actionTimers.size()){
			
				if(now - actionTimers.get(i).getLastActionTime() < idleTimeout)
					alive = true;
					
				i++;
				
			}
			
			if(!alive && available)
				setPresenceAway();
			else if(alive && !available)
				setPresenceAvailable();
			
		}
		
	}
	
	
	/**
	 * Helper method to create a new XMPP user based on the current credentials
	 * Will fail if user already exists and flag hasErrors
	 */
	public void createUser(){
		
		LOGGER.info(xStrings.getString("ChatManager.logCreatingXMPPUser") + XMPPUserName); //$NON-NLS-1$
		XMPPConnection setupConnection = new XMPPConnection(XMPPServerHostName);
		
		try {
			setupConnection.connect();
		} catch (XMPPException e) {
			hasErrors = true;
			showError(e, xStrings.getString("ChatManager.setupUserConnectionFailure")); //$NON-NLS-1$
		}
		
		if(!hasErrors){
			
			AccountManager XMPPAccounts = new AccountManager(setupConnection);
			
			try {
				XMPPAccounts.createAccount(XMPPUserName.split("@")[0], XMPPPassword); //$NON-NLS-1$
				LOGGER.info(xStrings.getString("ChatManager.logCreatedXMPPUser") + XMPPUserName); //$NON-NLS-1$
				setupConnection.disconnect();
				LOGGER.info(xStrings.getString("ChatManager.logSetupDisconnected")); //$NON-NLS-1$
			} catch (XMPPException e) {
				showError(e, xStrings.getString("ChatManager.errorCreatingXMPPUser") + XMPPUserName); //$NON-NLS-1$
				LOGGER.severe(xStrings.getString("ChatManager.errorCreatingXMPPUser") + XMPPUserName); //$NON-NLS-1$
			}
			
		}
		
	}
	
	/**
	 * Returns the Phonebox Chat Room
	 * @return
	 */
	public MultiUserChat getChatRoom(){
		
		return phoneboxChat;
		
	}
	
	/**
	 * Returns true if hasErrors is set, usually from XMPP connect/message errors
	 * @return
	 */
	public boolean hasErrors(){
		return hasErrors;
	}
	
	/**
	 * Set the Logger object
	 */
	public void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.info("ChatManager.logSetupLogging"); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("chatLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("ChatManager.loggerCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	
	/**
	 * Set the default logging level
	 * @param logLevel
	 */
	public void setLoggingLevel(Level logLevel){
		
		LOGGER.setLevel(logLevel);
		
	}
	
	/**
	 * Set the locale to be used with any strings reflected to the user
	 * @param language language code e.g. en
	 * @param country country code e.g. GB
	 */
	public void setLocale(String language, String country){
		
		LOGGER.info(xStrings.getString("ChatManager.logSetLocale") + " " + language + ", " + country);   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Set the amount of history to be retrieved upon joining the chat room
	 * @param seconds messages received in the last x seconds
	 */
	public void setChatHistory(int seconds){
		
		LOGGER.info(xStrings.getString("ChatManager.logChatHistoryPrefix")+ " " + seconds +   //$NON-NLS-1$//$NON-NLS-2$
				xStrings.getString("ChatManager.logChatHistorySuffix")); //$NON-NLS-1$
		
		XMPPChatHistory = seconds;
		
	}
	
	/**
	 * Connects to the chat server and joins the room given at instantiation
	 * Will flag hasError and show a Dialog message upon any errors
	 */
	public void connect(){
		
		XMPPServerConnection = new XMPPConnection(XMPPServerHostName);
		hasErrors = false;
		
		//Connect to Server
		try{
			LOGGER.info(xStrings.getString("ChatManager.logConnect")); //$NON-NLS-1$
			XMPPServerConnection.connect();
			LOGGER.info(xStrings.getString("ChatManager.logConnected")); //$NON-NLS-1$
		}catch(XMPPException e){
			
			showError(e, xStrings.getString("ChatManager.connectError")); //$NON-NLS-1$
			hasErrors = true;
			disconnect();
			
		}
		
		if(!hasErrors){
			
			try{
				LOGGER.info(xStrings.getString("ChatManager.logLogin")); //$NON-NLS-1$
				XMPPServerConnection.login(XMPPUserName, XMPPPassword);
				LOGGER.info(xStrings.getString("ChatManager.logLoggedIn")); //$NON-NLS-1$
			}catch(XMPPException e){
				
				showError(e, xStrings.getString("ChatManager.loginError")); //$NON-NLS-1$
				hasErrors = true;
				disconnect();
				
			}
			
			//Join Group Chat Channel
			if(!hasErrors){
				
				phoneboxChat = new MultiUserChat(XMPPServerConnection, XMPPRoomName);
				DiscussionHistory chatHistory = new DiscussionHistory();
				chatHistory.setSeconds(XMPPChatHistory);
				
				try{
					LOGGER.info(xStrings.getString("ChatManager.logJoinRoom")); //$NON-NLS-1$
					phoneboxChat.join(XMPPNickName, XMPPPassword, chatHistory, 
							SmackConfiguration.getPacketReplyTimeout());
					LOGGER.info(xStrings.getString("ChatManager.logJoinedRoom")); //$NON-NLS-1$
					
				}catch(XMPPException e){
					
					showError(e, xStrings.getString("ChatManager.chatRoomError")); //$NON-NLS-1$
					hasErrors = true;
					disconnect();
					
				}
				
			}
			
		}
		
	}
	
	/**
	 * Disconnect from the XMPP Chat Server
	 */
	public void disconnect() throws IllegalStateException{
		
		
		if(idleCheckThread != null && idleCheckThread.isAlive()){
		
			LOGGER.info(xStrings.getString("ChatManager.logStopIdleDetect")); //$NON-NLS-1$
			idleCheckThread.interrupt();	
			
			try {
				idleCheckThread.join();//Should only be a few millis wait
			} catch (InterruptedException e) {}
			
			LOGGER.info(xStrings.getString("ChatManager.logIdleDetectStopped")); //$NON-NLS-1$
			
		}
		
		//Leave joined XMPP rooms, not strictly necessary as disconnect will handle this
		if(phoneboxChat != null){
			
			LOGGER.info(xStrings.getString("ChatManager.logLeaveRoom")); //$NON-NLS-1$
			phoneboxChat.leave();
			LOGGER.info(xStrings.getString("ChatManager.logLeftRoom")); //$NON-NLS-1$
			phoneboxChat = null;
			
		}
			
		//Disconnect from XMPP server and close resources
		if(XMPPServerConnection != null){
			
			LOGGER.info(xStrings.getString("ChatManager.logDisconnect")); //$NON-NLS-1$
			XMPPServerConnection.disconnect();
			LOGGER.info(xStrings.getString("ChatManager.logDisconnected")); //$NON-NLS-1$
			XMPPServerConnection = null;
			
		}
		
	}
	
	/**
	 * Resets ChatManager to defaults ready for a reconnect after errors
	 * or anything else you might want to do.
	 * 
	 * This will disconnect any connections currently open
	 */
	public void reset(){
		
		LOGGER.info(xStrings.getString("ChatManager.logReset")); //$NON-NLS-1$
		disconnect();
		hasErrors = false;
		
	}
	
	/**
	 * Sends the given string as a message to the XMPP chat room
	 * @param msg
	 */
	public void sendMessage(String msg){
		
		LOGGER.info(xStrings.getString("ChatManager.logSendMessage")); //$NON-NLS-1$
		try{
			phoneboxChat.sendMessage(msg);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.chatRoomError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatManager.serverGoneError")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}

	/** PacketListener methods **/
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			String friendlyFrom = message.getFrom();
			if(friendlyFrom.contains("/")) //$NON-NLS-1$
				friendlyFrom = friendlyFrom.split("/")[1]; //$NON-NLS-1$
			
			String b = message.getBody();
			
			//Check for status change
			if(friendlyFrom.equals(XMPPNickName)){
				
				if(b.equals(xStrings.getString("ChatManager.chatReturned"))) //$NON-NLS-1$
					setPresenceAvailable();
				else if(b.equals(xStrings.getString("ChatManager.chatBackSoon"))) //$NON-NLS-1$
					setPresenceAway();
				
			}
			
		}
		
	}
	
	private void setPresenceAvailable(){
		
		available = true;
		Presence presence = new Presence(Presence.Type.available, 
				xStrings.getString("ChatManager.available"), 1, //$NON-NLS-1$
				Presence.Mode.available); 
		presence.setTo(XMPPRoomName + "/" + XMPPUserName.split("@")[0]);  //$NON-NLS-1$//$NON-NLS-2$
		LOGGER.info(xStrings.getString("ChatManager.logSendingPresence") +  //$NON-NLS-1$
				presence.getFrom() + ": " + presence.getMode()); //$NON-NLS-1$
		XMPPServerConnection.sendPacket(presence);
		
	}
	
	private void setPresenceAway(){
		
		available = false;
		Presence presence = new Presence(Presence.Type.available, 
				xStrings.getString("ChatManager.away"), 0, //$NON-NLS-1$
						Presence.Mode.away); 
		presence.setTo(XMPPRoomName + "/" + XMPPUserName.split("@")[0]);  //$NON-NLS-1$//$NON-NLS-2$
		LOGGER.info(xStrings.getString("ChatManager.logSendingPresence") +  //$NON-NLS-1$
				presence.getFrom() + ": " + presence.getMode()); //$NON-NLS-1$
		XMPPServerConnection.sendPacket(presence);
		
	}
	
	/** UserStatusListener methods **/
	@Override
	public void kicked(String actor, String reason) {

		/*
		 * Called when a user is kicked from a room
		 * Should never happen unless client logs in twice as a duplicate
		 * or someone logs in as admin and manually kicks everyone
		 */
		LOGGER.info(xStrings.getString("ChatManager.logKickedFromRoom") + actor + ": " + reason); //$NON-NLS-1$ //$NON-NLS-2$
		disconnect();
		
	}

	/** UNUSED UserStatusListener methods **/
	@Override
	public void adminGranted() {}

	@Override
	public void adminRevoked() {}

	@Override
	public void membershipGranted() {}

	@Override
	public void membershipRevoked() {}

	@Override
	public void moderatorGranted() {}

	@Override
	public void moderatorRevoked() {}

	@Override
	public void ownershipGranted() {}

	@Override
	public void ownershipRevoked() {}

	@Override
	public void voiceGranted() {}

	@Override
	public void voiceRevoked() {}
	
	@Override
	public void banned(String actor, String reason) {}
	/** END UNUSED userStatusListener methods **/
	
}
