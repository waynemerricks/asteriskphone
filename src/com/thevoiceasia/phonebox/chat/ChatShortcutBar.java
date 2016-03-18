package com.thevoiceasia.phonebox.chat;

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

import com.thevoiceasia.phonebox.misc.AlertSounder;
import com.thevoiceasia.phonebox.misc.LastActionTimer;

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
		callToggle = new JToggleButton(createImageIcon("images/nocalls.png", "nocalls"), false); //$NON-NLS-1$ //$NON-NLS-2$
		callToggle.setToolTipText(xStrings.getString("ChatShortcutBar.buttonNoCallsToolTip")); //$NON-NLS-1$
		callToggle.setActionCommand("nocalls"); //$NON-NLS-1$
		callToggle.addActionListener(this);
		callToggle.setEnabled(isStudio);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectNoCalls"))){ //$NON-NLS-1$
			callToggle.setSelected(true);
			callPressed = true;
			breakPressed = false;
			helpPressed = false;
		}
		
		this.add(callToggle);
		
		breakToggle = new JToggleButton(createImageIcon("images/backsoon.png", "backsoon"), //$NON-NLS-1$//$NON-NLS-2$
				false); 
		breakToggle.setToolTipText(xStrings.getString(
				"ChatShortcutBar.buttonBackSoonToolTip")); //$NON-NLS-1$
		breakToggle.setActionCommand("backsoon"); //$NON-NLS-1$
		breakToggle.addActionListener(this);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectBackSoon"))){ //$NON-NLS-1$
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
		helpToggle = new JToggleButton(createImageIcon("images/helpme.png", "help"));  //$NON-NLS-1$//$NON-NLS-2$
		helpToggle.setToolTipText(xStrings.getString("ChatShortcutBar.buttonHelpToolTip")); //$NON-NLS-1$
		helpToggle.setActionCommand("help"); //$NON-NLS-1$
		helpToggle.addActionListener(this);
		
		if(isStudio && currentTopic.equals(xStrings.getString(
				"ChatShortcutBar.subjectHelp"))){ //$NON-NLS-1$
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
		
		System.err.println(xStrings.getString("ChatShortcutBar.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("ChatShortcutBar.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
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
			
			LOGGER.warning(xStrings.getString("ChatShortcutBar.logLoadIconError")); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}
	
	/**
	 * Sends a message to the chat room
	 * @param message
	 */
	private void sendMessage(String message, MultiUserChat room){
		
		try {
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSendRoomMessage") + message); //$NON-NLS-1$
			room.sendMessage(message);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.chatRoomError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.serverGoneError")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Changes the room topic
	 * @param topic
	 */
	private void changeTopic(String topic){
		
		try {
			LOGGER.info(xStrings.getString("ChatShortcutBar.logChangeRoomTopic")); //$NON-NLS-1$
			chatRoom.changeSubject(topic);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.changeSubjectError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatShortcutBar.serverGoneError")); //$NON-NLS-1$
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
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetNoCalls")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatNoCalls"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectNoCalls")); //$NON-NLS-1$
			
		}else if(callPressed){
			
			callPressed = false;
			shortCutGroup.clearSelection();
			callToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetCalls")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatResumeCalls"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); //$NON-NLS-1$
			
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
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetBackSoon")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatBackSoon"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectBackSoon")); //$NON-NLS-1$
			
		}else if(breakPressed){
			
			breakPressed = false;
			shortCutGroup.clearSelection();
			breakToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetReturned")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatReturned"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); //$NON-NLS-1$
			
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
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetHelpMe")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatHelpMe"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.subjectHelp")); //$NON-NLS-1$
			
		}else if(helpPressed){
			
			helpPressed = false;
			shortCutGroup.clearSelection();
			helpToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatShortcutBar.logSetPanicOver")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatShortcutBar.chatCrisisOver"), chatRoom); //$NON-NLS-1$
			
			if(isStudio)
				changeTopic(xStrings.getString("ChatShortcutBar.emptyTopic")); //$NON-NLS-1$
			
		}
		
	}
		
	@Override
	/**
	 * Standard actionListener entry for use with shortcut buttons
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		
		//Don't interfere with idle check if we've set ourselves away
		if(!evt.getActionCommand().equals("backsoon")) //$NON-NLS-1$
			lastActionTime = new Date().getTime();
		
		if(evt.getActionCommand().equals("nocalls")) //$NON-NLS-1$
			noCallsPressed();
		else if(evt.getActionCommand().equals("backsoon")) //$NON-NLS-1$
			backSoonPressed();
		else if(evt.getActionCommand().equals("help")) //$NON-NLS-1$
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
		LOGGER.info(xStrings.getString("ChatMessagePanel.logReceivedMessage") + XMPPPacket); //$NON-NLS-1$
		
		if(XMPPPacket instanceof Message){
		
			Message message = (Message)XMPPPacket;
			
			String from = message.getFrom();
			
			if(from.contains("/")) //$NON-NLS-1$
				from = from.split("/")[1]; //$NON-NLS-1$
			
			if(!from.equals(controlRoom.getNickname())){//If the message didn't come from me 
			
				if(message.getBody().equals(xStrings.getString("ChatShortcutBar.commandAlert")))
						alert.play();
			
			}
			
		}
		
	}
	
}
