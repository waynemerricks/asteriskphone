package com.thevoiceasia.phonebox.chat;

import java.util.Date;
import java.util.Vector;
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
	private String XMPPUserName, XMPPPassword, XMPPNickName, XMPPServerHostName, XMPPRoomName,
		XMPPControlRoomName;
	private int XMPPChatHistory, idleTimeout;
	
	//XMPP Connections/Rooms
	private Connection XMPPServerConnection;
	private MultiUserChat phoneboxChat, controlChat;
	
	//ChatManager vars
	private boolean hasErrors = false; //Error Flag used internally
	private I18NStrings xStrings; //Link to external string resources
	private Vector<LastActionTimer> actionTimers = new Vector<LastActionTimer>();
	private boolean available = true, manualAway = false;
	private IdleCheckThread idleCheckThread;
	
	/** STATICS **/
	private static final int XMPP_CHAT_HISTORY = 600; //Chat Messages in the last x seconds
	private static final Logger LOGGER = Logger.getLogger(ChatManager.class.getName());//Logger
	
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
			String serverHostName, String roomName, String controlRoomName,
			String language, String country, int idleTimeout){
		
		//Get I18N handle for external strings
		xStrings = new I18NStrings(language, country);
		this.XMPPUserName = userName;
		this.XMPPPassword = password;
		this.XMPPServerHostName = serverHostName;
		this.XMPPRoomName = roomName;
		this.XMPPControlRoomName = controlRoomName;
		this.XMPPChatHistory = XMPP_CHAT_HISTORY;
		this.XMPPNickName = nickName;
		this.idleTimeout = idleTimeout;
		
	}
	
	/**
	 * Returns the connection to the XMPP server
	 * @return
	 */
	public XMPPConnection getConnection(){
		
		return (XMPPConnection)XMPPServerConnection;
		
	}
	
	/**
	 * Starts a thread which checks if the user is idle
	 */
	public void startIdleDetectThread(){
	
		LOGGER.info(xStrings.getString("ChatManager.logStartIdleDetect")); 
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
		
		LOGGER.info(xStrings.getString("ChatManager.logAddActionTimer") + name); 
		actionTimers.add(lastActionTimer);
		
	}
	
	/**
	 * Called by checkIdle thread to see if the user is idle
	 */
	public void checkIdle(){
		
		LOGGER.info(xStrings.getString("ChatManager.logCheckingIdle")); 
		
		if(actionTimers.size() > 0){
			
			int i = 0;
			boolean alive = false;
			long now = new Date().getTime();
			
			while(!alive && i < actionTimers.size()){
			
				if(now - actionTimers.get(i).getLastActionTime() < idleTimeout)
					alive = true;
				/* 
				 * Refactoring so that this class holds the last action time and objects
				 * might make sense but then that means every class needs a reference
				 * to ChatManager which is yet another thing to swap between classes
				 */
				//reset it
				i++;
				
			}
			
			if(!alive && available)
				setPresenceAway();
			else if(alive && !available && !manualAway)
				setPresenceAvailable();
			
		}
		
	}
	
	/**
	 * Helper method to create a new XMPP user based on the current credentials
	 * Will fail if user already exists and flag hasErrors
	 */
	public boolean createUser(){
		
		boolean created = false;
		
		LOGGER.info(xStrings.getString("ChatManager.logCreatingXMPPUser") + XMPPUserName); 
		XMPPConnection setupConnection = new XMPPConnection(XMPPServerHostName);
		
		try {
			setupConnection.connect();
		} catch (XMPPException e) {
			hasErrors = true;
			showError(e, xStrings.getString("ChatManager.setupUserConnectionFailure")); 
		}
		
		if(!hasErrors){
			
			AccountManager XMPPAccounts = new AccountManager(setupConnection);
			
			try {
				XMPPAccounts.createAccount(XMPPUserName.split("@")[0], XMPPPassword); 
				LOGGER.info(xStrings.getString("ChatManager.logCreatedXMPPUser") + XMPPUserName); 
				created = true;
				setupConnection.disconnect();
				LOGGER.info(xStrings.getString("ChatManager.logSetupDisconnected")); 
			} catch (XMPPException e) {
				showError(e, xStrings.getString("ChatManager.errorCreatingXMPPUser") + XMPPUserName); 
				LOGGER.severe(xStrings.getString("ChatManager.errorCreatingXMPPUser") + XMPPUserName); 
			}
			
		}
		
		return created;
		
	}
	
	/**
	 * Returns the Phonebox Chat Room
	 * @return
	 */
	public MultiUserChat getChatRoom(){
		
		return phoneboxChat;
		
	}
	
	/**
	 * Returns the Phonebox Control Chat Room
	 * @return
	 */
	public MultiUserChat getControlChatRoom(){
		
		return controlChat;
		
	}
	
	/**
	 * Returns true if hasErrors is set, usually from XMPP connect/message errors
	 * @return
	 */
	public boolean hasErrors(){
		return hasErrors;
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
		
		LOGGER.info(xStrings.getString("ChatManager.logSetLocale") + " " + language + ", " + country);
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Set the amount of history to be retrieved upon joining the chat room
	 * @param seconds messages received in the last x seconds
	 */
	public void setChatHistory(int seconds){
		
		LOGGER.info(xStrings.getString("ChatManager.logChatHistoryPrefix")+ " " + seconds +   
				xStrings.getString("ChatManager.logChatHistorySuffix")); 
		
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
			LOGGER.info(xStrings.getString("ChatManager.logConnect")); 
			XMPPServerConnection.connect();
			LOGGER.info(xStrings.getString("ChatManager.logConnected")); 
		}catch(XMPPException e){
			
			showError(e, xStrings.getString("ChatManager.connectError")); 
			hasErrors = true;
			disconnect();
			
		}
		
		if(!hasErrors){
			
			try{
				LOGGER.info(xStrings.getString("ChatManager.logLogin")); 
				
				if(XMPPRoomName == null)
					XMPPServerConnection.login(XMPPUserName, XMPPPassword, "Client"); 
				else
					XMPPServerConnection.login(XMPPUserName, XMPPPassword, "Server"); 
				LOGGER.info(xStrings.getString("ChatManager.logLoggedIn")); 
			}catch(XMPPException e){
				
				showError(e, xStrings.getString("ChatManager.loginError")); 
				hasErrors = true;
				disconnect();
				
			}
			
			//Join Group Chat Channel
			if(!hasErrors){
				
				if(XMPPRoomName != null){
					phoneboxChat = new MultiUserChat(XMPPServerConnection, XMPPRoomName);
					joinRoom(phoneboxChat);
				}
				
				controlChat = new MultiUserChat(XMPPServerConnection, XMPPControlRoomName);
				joinRoom(controlChat);
				
			}
			
		}
		
	}
	
	/**
	 * Joins the given MUC to the chat room, sets error flag if there are problems
	 * @param room room to join
	 * @return true if successful
	 */
	private boolean joinRoom(MultiUserChat room){
		
		boolean success = false;
		
		DiscussionHistory chatHistory = new DiscussionHistory();
		chatHistory.setSeconds(XMPPChatHistory);
		
		try{
			LOGGER.info(xStrings.getString("ChatManager.logJoinRoom")); 
			room.join(XMPPNickName, XMPPPassword, chatHistory, 
					SmackConfiguration.getPacketReplyTimeout());
			LOGGER.info(xStrings.getString("ChatManager.logJoinedRoom")); 
			success = true;
			
		}catch(XMPPException e){
			
			showError(e, xStrings.getString("ChatManager.chatRoomError")); 
			hasErrors = true;
			disconnect();
			
		}
		
		return success;
		
	}
	
	/**
	 * Disconnect from the XMPP Chat Server
	 */
	public void disconnect() throws IllegalStateException{
		
		
		if(idleCheckThread != null && idleCheckThread.isAlive()){
		
			LOGGER.info(xStrings.getString("ChatManager.logStopIdleDetect")); 
			idleCheckThread.interrupt();	
			
			try {
				idleCheckThread.join();//Should only be a few millis wait
			} catch (InterruptedException e) {}
			
			LOGGER.info(xStrings.getString("ChatManager.logIdleDetectStopped")); 
			
		}
		
		//Leave joined XMPP rooms, not strictly necessary as disconnect will handle this
		if(phoneboxChat != null){
			
			LOGGER.info(xStrings.getString("ChatManager.logLeaveRoom")); 
			phoneboxChat.leave();
			LOGGER.info(xStrings.getString("ChatManager.logLeftRoom")); 
			phoneboxChat = null;
			
		}
			
		//Disconnect from XMPP server and close resources
		if(XMPPServerConnection != null){
			
			LOGGER.info(xStrings.getString("ChatManager.logDisconnect")); 
			XMPPServerConnection.disconnect();
			LOGGER.info(xStrings.getString("ChatManager.logDisconnected")); 
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
		
		LOGGER.info(xStrings.getString("ChatManager.logReset")); 
		disconnect();
		hasErrors = false;
		
	}
	
	/**
	 * Sends the given string as a message to the XMPP chat room
	 * @param msg
	 * @param controlMessage flag to indicate whether this goes to control room or not
	 */
	public void sendMessage(String msg, boolean controlMessage){
		
		LOGGER.info(xStrings.getString("ChatManager.logSendMessage")); 
		try{
			if(phoneboxChat != null && !controlMessage)
				phoneboxChat.sendMessage(msg);
			else
				controlChat.sendMessage(msg);
			
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.chatRoomError")); 
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatManager.serverGoneError")); 
		}
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); 
		LOGGER.warning(friendlyErrorMessage);
		
	}

	/** PacketListener methods **/
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			String friendlyFrom = message.getFrom();
			if(friendlyFrom.contains("/")) 
				friendlyFrom = friendlyFrom.split("/")[1]; 
			
			String b = message.getBody();
			
			//Check for status change
			if(friendlyFrom.equals(XMPPNickName)){
				
				if(b.equals(xStrings.getString("ChatManager.chatReturned"))) 
					setPresenceAvailable();
				else if(b.equals(xStrings.getString("ChatManager.chatBackSoon"))) 
					setPresenceAwayManual();
				
			}
			
		}
		
	}
	
	/**
	 * Sets the XMPP Presence to available
	 */
	private void setPresenceAvailable(){
		
		available = true;
		manualAway = false;
		
		Presence presence = new Presence(Presence.Type.available, 
				xStrings.getString("ChatManager.available"), 1, 
				Presence.Mode.available); 
		//BUG FIX Nickname is ignored and this presence sets name back to the login name
		if(XMPPNickName != null && XMPPNickName.length() > 0)
			presence.setTo(XMPPRoomName + "/" + XMPPNickName); 
		else
			presence.setTo(XMPPRoomName + "/" + XMPPUserName.split("@")[0]);  
		
		LOGGER.info(xStrings.getString("ChatManager.logSendingPresence") +  
				presence.getFrom() + ": " + presence.getMode()); 
		XMPPServerConnection.sendPacket(presence);
		
	}
	
	/**
	 * Flags that we've manually set ourselves away so checkIdle won't interfere
	 * then calls setPresenceAway to complete the process
	 */
	private void setPresenceAwayManual(){
		
		manualAway = true;
		setPresenceAway();
		
	}
	
	/**
	 * Sets our presence to away
	 */
	private void setPresenceAway(){
		
		available = false;
		Presence presence = new Presence(Presence.Type.available, 
				xStrings.getString("ChatManager.away"), 0, 
						Presence.Mode.away); 
		
		//BUG FIX Nickname is ignored and this presence sets name back to the login name
		if(XMPPNickName != null && XMPPNickName.length() > 0)
			presence.setTo(XMPPRoomName + "/" + XMPPNickName); 
		else
			presence.setTo(XMPPRoomName + "/" + XMPPUserName.split("@")[0]);  
		
		LOGGER.info(xStrings.getString("ChatManager.logSendingPresence") +  
				presence.getFrom() + ": " + presence.getMode()); 
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
		LOGGER.info(xStrings.getString("ChatManager.logKickedFromRoom") + actor + ": " + reason);  
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
