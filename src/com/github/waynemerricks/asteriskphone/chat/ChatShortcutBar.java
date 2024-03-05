package com.github.waynemerricks.asteriskphone.chat;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.github.waynemerricks.asteriskphone.misc.AlertSounder;
import com.github.waynemerricks.asteriskphone.misc.LastActionTimer;

/**
 * Creates a simple shortcut bar as a JPanel that has several ToggleButtons.
 * 
 * Designed to be used with an XMPP MultiUserChat room and will send subjects/messages
 * @author Wayne Merricks
 *
 */
public class ChatShortcutBar extends JPanel implements ActionListener, LastActionTimer, PacketListener {

	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(ChatShortcutBar.class.getName());//Logger
	private static final long serialVersionUID = 1L;
	
	/** CLASS VARS **/
	private I18NStrings xStrings; //Link to external string resources
	private JToggleButton callToggle, breakToggle, helpToggle;
	private JButton alertButton;
	private ButtonGroup shortCutGroup;
	private MultiUserChat chatRoom, controlRoom;
	private boolean callPressed = false, breakPressed = false, helpPressed = false, isStudio;
	private long lastActionTime;
	private AlertSounder alert = null;
	
	/**
	 * Shortcut bar for pre-canned phrases on the chat box
	 * @param language for use with I18N
	 * @param country for use with I18N
	 * @param chatRoom Room to send messages and other changes to
	 * @param isStudio if true enables studio specific behaviour
	 */
	public ChatShortcutBar(String language, String country, MultiUserChat chatRoom, 
			MultiUserChat controlRoom, boolean isStudio, String currentTopic){
		
		xStrings = new I18NStrings(language, country);
		
		this.chatRoom = chatRoom;
		this.controlRoom = controlRoom;
		this.isStudio = isStudio;
		
		//Topic can be null, check for that
		if(currentTopic == null)
			currentTopic = xStrings.getString("ChatMessagePanel.emptyTopic");

		this.setLayout(new GridLayout(1, 5, 5, 5));
		
		//Set up alert sounder
		if(!isStudio){
			
			alert = new AlertSounder("audio/alert.wav");
			this.controlRoom.addMessageListener(this);
			
		}
		
		/*
		 * No Calls Please (Toggle)
		 * Back soon (tea break Toggle)
		 * Play Alert
		 * Send Help
		 */
		callToggle = new JToggleButton(createImageIcon("images/nocalls.png", "nocalls"), false);  
		callToggle.setToolTipText(xStrings.getString("ChatShortcutBar.buttonNoCallsToolTip")); 
		callToggle.setActionCommand("nocalls"); 
		callToggle.addActionListener(this);
		callToggle.setEnabled(isStudio);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectNoCalls"))){ 
			callToggle.setSelected(true);
			callPressed = true;
			breakPressed = false;
			helpPressed = false;
		}
		
		this.add(callToggle);
		
		breakToggle = new JToggleButton(createImageIcon("images/backsoon.png", "backsoon"), 
				false); 
		breakToggle.setToolTipText(xStrings.getString(
				"ChatShortcutBar.buttonBackSoonToolTip")); 
		breakToggle.setActionCommand("backsoon"); 
		breakToggle.addActionListener(this);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectBackSoon"))){ 
			breakToggle.setSelected(true);
			callPressed = false;
			breakPressed = true;
			helpPressed = false;
		}
		
		this.add(breakToggle);
		
		//Spacer
		this.add(new JLabel());
		
		//Alert
		alertButton = new JButton(createImageIcon("images/buzz.png", "alert"));
		alertButton.setToolTipText(xStrings.getString("ChatShortcutBar.buttonAlertToolTip"));
		alertButton.setActionCommand("alert");
		alertButton.addActionListener(this);
		alertButton.setEnabled(isStudio);
		
		this.add(alertButton);
		
		//Help
		helpToggle = new JToggleButton(createImageIcon("images/helpme.png", "help"));  
		helpToggle.setToolTipText(xStrings.getString("ChatShortcutBar.buttonHelpToolTip")); 
		helpToggle.setActionCommand("help"); 
		helpToggle.addActionListener(this);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectHelp"))){ 
			helpToggle.setSelected(true);
			callPressed = false;
			breakPressed = false;
			helpPressed = true;
		}
		
		this.add(helpToggle);
		
		//Add buttons to group so only one is selected (cuts down on duplicate offs)
		shortCutGroup = new ButtonGroup();
		shortCutGroup.add(callToggle);
		shortCutGroup.add(breakToggle);
		shortCutGroup.add(helpToggle);
		
		lastActionTime = new Date().getTime();
		
	}

	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("ChatShortcutBar.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatShortcutBar.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); 
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	/**
	 * Gets the image from a relative path and creates an icon for use with buttons
	 * @param path path where image resides
	 * @param description identifier for this image, for internal use
	 * @return the image loaded as a Java Icon
	 */
	private ImageIcon createImageIcon(String path, String description){
		
		ImageIcon icon = null;
		
		URL imgURL = getClass().getResource(path);
		
		if(imgURL != null)
			icon = new ImageIcon(imgURL, description);
		else{
			
			LOGGER.warning(xStrings.getString("ChatShortcutBar.logLoadIconError")); 
			
		}
		
		return icon;
		
	}
	
	/**
	 * Sends a message to the chat room
	 * @param message
	 */
	private void sendMessage(String message, MultiUserChat room){
		
		try {
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSendRoomMessage") + message); 
			room.sendMessage(message);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.chatRoomError")); 
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.serverGoneError")); 
		}
		
	}
	
	/**
	 * Changes the room topic
	 * @param topic
	 */
	private void changeTopic(String topic){
		
		try {
			LOGGER.info(xStrings.getString("ChatShortcutBar.logChangeRoomTopic")); 
			chatRoom.changeSubject(topic);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.changeSubjectError")); 
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.serverGoneError")); 
		}
		
	}
	
	
	/**
	 * Toggles no calls by sending a message and changing the topic alert
	 */
	private void noCallsPressed(){
	
		if(callToggle.isSelected() && !callPressed){
			
			callPressed = true;
			breakPressed = false;
			helpPressed = false;
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetNoCalls")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatNoCalls"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectNoCalls")); 
			
		}else if(callPressed){
			
			callPressed = false;
			shortCutGroup.clearSelection();
			callToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetCalls")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatResumeCalls"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); 
			
		}
			
	}
	
	/**
	 * Toggles back soon by sending a message and changing the topic alert
	 */
	private void backSoonPressed(){
		
		if(breakToggle.isSelected() && !breakPressed){
			
			//Set LastActionTime to something really old so we don't kick the idle thread
			lastActionTime = 1;
			breakPressed = true;
			callPressed = false;
			helpPressed = false;
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetBackSoon")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatBackSoon"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectBackSoon")); 
			
		}else if(breakPressed){
			
			breakPressed = false;
			shortCutGroup.clearSelection();
			breakToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetReturned")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatReturned"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); 
			
		}
		
	}
	
	/**
	 * Sends alert command
	 */
	private void alertPressed(){
		
		LOGGER.info(xStrings.getString("ChatShortcutBar.logAlert"));
		sendMessage(xStrings.getString("ChatShortcutBar.commandAlert"), controlRoom);
		sendMessage(xStrings.getString("ChatShortcutBar.chatAlert"), chatRoom);
		
	}
	
	/**
	 * Toggles send help by sending a message and changing the topic alert
	 */
	private void helpPressed(){
		
		if(helpToggle.isSelected() && !helpPressed){
			
			helpPressed = true;
			callPressed = false;
			breakPressed = false;
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetHelpMe")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatHelpMe"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectHelp")); 
			
		}else if(helpPressed){
			
			helpPressed = false;
			shortCutGroup.clearSelection();
			helpToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetPanicOver")); 
			sendMessage(xStrings.getString("ChatShortcutBar.chatCrisisOver"), chatRoom); 
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); 
			
		}
		
	}
		
	@Override
	/**
	 * Standard actionListener entry for use with shortcut buttons
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		
		//Don't interfere with idle check if we've set ourselves away
		if(!evt.getActionCommand().equals("backsoon")) 
			lastActionTime = new Date().getTime();
		
		if(evt.getActionCommand().equals("nocalls")) 
			noCallsPressed();
		else if(evt.getActionCommand().equals("backsoon")) 
			backSoonPressed();
		else if(evt.getActionCommand().equals("help")) 
			helpPressed();
		else if(evt.getActionCommand().equals("alert"))
			alertPressed();
		
	}

	@Override
	public long getLastActionTime() {
		return lastActionTime;
	}

	@Override
	public void processPacket(Packet XMPPPacket) {

		/* As part of the chat alert process I want this to monitor for alerts
		 * from other people.
		 * 
		 * Control Room: ALERT/ALERT = Play alert noise
		 */
		LOGGER.info(xStrings.getString("ChatMessagePanel.logReceivedMessage") + XMPPPacket); 
		
		if(XMPPPacket instanceof Message){
		
			Message message = (Message)XMPPPacket;
			
			String from = message.getFrom();
			
			if(from.contains("/")) 
				from = from.split("/")[1]; 
			
			if(!from.equals(controlRoom.getNickname())){//If the message didn't come from me 
			
				if(message.getBody().equals(xStrings.getString("ChatShortcutBar.commandAlert")))
						alert.play();
			
			}
			
		}
		
	}
	
}
