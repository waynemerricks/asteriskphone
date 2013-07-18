package com.thevoiceasia.phonebox.chat;

import java.io.IOException;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class ChatManager implements PacketListener {

	//XMPP Settings
	private String XMPPUserName, XMPPPassword, XMPPNickName, XMPPServerHostName, XMPPRoomName;
	private int XMPPChatHistory;
	
	//XMPP Connections/Rooms
	private Connection XMPPServerConnection;
	private MultiUserChat phoneboxChat;
	
	//ChatManager vars
	private boolean hasErrors = false; //Error Flag used internally
	private I18NStrings xStrings; //Link to external string resources
	private Vector<MessageReceiver> receivers = new Vector<MessageReceiver>();
	
	
	/** STATICS **/
	private static final int XMPP_CHAT_HISTORY = 600; //Chat Messages in the last x seconds
	private static final Logger LOGGER = Logger.getLogger(ChatManager.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	
	/**
	 * Create ChatManager with default locale (en, GB)
	 * 
	 * Will login to the XMPP server given with the following details
	 * @param userName
	 * @param password
	 * @param serverHostName
	 * @param roomName
	 */
	public ChatManager(String userName, String password, String nickName, 
			String serverHostName, String roomName){
		
		//Get I18N handle for external strings
		xStrings = new I18NStrings("en", "GB"); //$NON-NLS-1$ //$NON-NLS-2$
		this.XMPPUserName = userName;
		this.XMPPPassword = password;
		this.XMPPServerHostName = serverHostName;
		this.XMPPRoomName = roomName;
		this.XMPPChatHistory = XMPP_CHAT_HISTORY;
		this.XMPPNickName = nickName;
		setupLogging();
		
	}
	
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
			String serverHostName, String roomName, String language, String country){
		
		//Get I18N handle for external strings
		xStrings = new I18NStrings(language, country);
		this.XMPPUserName = userName;
		this.XMPPPassword = password;
		this.XMPPServerHostName = serverHostName;
		this.XMPPRoomName = roomName;
		this.XMPPChatHistory = XMPP_CHAT_HISTORY;
		this.XMPPNickName = nickName;
		setupLogging();
		
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
	 * 
	 */
	public void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		
		try{
			LOGGER.addHandler(new FileHandler("chatLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("ChatManager.logCreateError")); //$NON-NLS-1$
			
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
		
		LOGGER.info(xStrings.getString("ChatManager.setLocale") + " " + language + ", " + country);   //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Set the amount of history to be retrieved upon joining the chat room
	 * @param seconds messages received in the last x seconds
	 */
	public void setChatHistory(int seconds){
		
		LOGGER.info(xStrings.getString("ChatManager.chatHistoryPrefix")+ " " + seconds +   //$NON-NLS-1$//$NON-NLS-2$
				xStrings.getString("ChatManager.chatHistorySuffix")); //$NON-NLS-1$
		
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
				phoneboxChat.addMessageListener(this);
				
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
	public void disconnect(){
		
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
		
		try{
			phoneboxChat.sendMessage(msg);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.chatRoomError")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatManager.errorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
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
		
		System.err.println(xStrings.getString("ChatManager.errorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatManager.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}

	/**
	 * TODO
	 * @param mr
	 */
	public void addMessageReceiver(MessageReceiver mr){
	
		LOGGER.info(xStrings.getString("ChatManager.addMessageReceiver") + mr.toString()); //$NON-NLS-1$
		receivers.add(mr);
		
	}
	
	/**
	 * TODO
	 * @param mr
	 */
	public void removeMessageReceiver(MessageReceiver mr){
		
		LOGGER.info(xStrings.getString("ChatManager.removeMessageReceiver") + mr.toString()); //$NON-NLS-1$
		
		int i = 0;
		boolean removed = false;
		
		while(!removed && i < receivers.size()){
			
			if(receivers.get(i) == mr){
				
				receivers.remove(i);
				removed = true;
				LOGGER.info(xStrings.getString("ChatManager.removedMessageReceiver")); //$NON-NLS-1$
				
			}
			
		}
		
		if(!removed)
			LOGGER.warning(xStrings.getString("ChatManager.errorRemovingMessageReceiver") + mr.toString()); //$NON-NLS-1$
		
	}
	
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		// TODO Auto-generated method stub
		LOGGER.info(xStrings.getString("ChatManager.receivedMessage") + XMPPPacket); //$NON-NLS-1$
		Message msg = (Message)XMPPPacket;
		LOGGER.info(msg.getBody());
		
		for(int i = 0; i < receivers.size(); i++)
			receivers.get(i).receiveMessage(msg);
		
	}
	
}
