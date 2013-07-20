package com.thevoiceasia.phonebox.chat;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

/**
 * Creates a simple shortcut bar as a JPanel that has several ToggleButtons.
 * 
 * Designed to be used with an XMPP MultiUserChat room and will send subjects/messages
 * @author Wayne Merricks
 *
 */
public class ChatShortcutBar extends JPanel implements ActionListener {

	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(ChatShortcutBar.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static final long serialVersionUID = 1L;
	
	/** CLASS VARS **/
	private I18NStrings xStrings; //Link to external string resources
	private JToggleButton callToggle, breakToggle, helpToggle;
	private ButtonGroup shortCutGroup;
	private MultiUserChat chatRoom;
	private boolean callPressed = false, breakPressed = false, helpPressed = false;
	
	/**
	 * Shortcut bar for pre-canned phrases on the chat box
	 * @param language for use with I18N
	 * @param country for use with I18N
	 * @param chatRoom Room to send messages and other changes to
	 */
	public ChatShortcutBar(String language, String country, MultiUserChat chatRoom){
		
		xStrings = new I18NStrings(language, country);
		this.chatRoom = chatRoom;
		
		this.setLayout(new GridLayout(1, 4, 5, 5));
		
		/*
		 * No Calls Please (Toggle)
		 * Back soon (tea break Toggle)
		 * Send Help
		 */
		callToggle = new JToggleButton(createImageIcon("images/nocalls.png", "nocalls"), false); //$NON-NLS-1$ //$NON-NLS-2$
		callToggle.setToolTipText(xStrings.getString("ChatManager.buttonNoCallsToolTip")); //$NON-NLS-1$
		callToggle.setActionCommand("nocalls"); //$NON-NLS-1$
		callToggle.addActionListener(this);
		this.add(callToggle);
		
		breakToggle = new JToggleButton(createImageIcon("images/backsoon.png", "backsoon"), false);  //$NON-NLS-1$//$NON-NLS-2$
		breakToggle.setToolTipText(xStrings.getString("ChatManager.buttonBackSoonToolTip")); //$NON-NLS-1$
		breakToggle.setActionCommand("backsoon"); //$NON-NLS-1$
		breakToggle.addActionListener(this);
		this.add(breakToggle);
		
		//Spacer
		this.add(new JLabel());
		
		helpToggle = new JToggleButton(createImageIcon("images/helpme.png", "help"));  //$NON-NLS-1$//$NON-NLS-2$
		helpToggle.setToolTipText(xStrings.getString("ChatManager.buttonHelpToolTip")); //$NON-NLS-1$
		helpToggle.setActionCommand("help"); //$NON-NLS-1$
		helpToggle.addActionListener(this);
		this.add(helpToggle);
		
		//Add buttons to group so only one is selected (cuts down on duplicate offs)
		shortCutGroup = new ButtonGroup();
		shortCutGroup.add(callToggle);
		shortCutGroup.add(breakToggle);
		shortCutGroup.add(helpToggle);
		
	}

	/**
	 * Set the Logger object
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
			
			LOGGER.warning(xStrings.getString("ChatManager.loadIconError")); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}
	
	/**
	 * Sends a message to the chat room
	 * @param message
	 */
	private void sendMessage(String message){
		
		try {
			LOGGER.info(xStrings.getString("ChatManager.sendRoomMessage") + message); //$NON-NLS-1$
			chatRoom.sendMessage(message);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.chatRoomError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatManager.duplicateLoginError")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Changes the room topic
	 * @param topic
	 */
	private void changeTopic(String topic){
		
		try {
			LOGGER.info(xStrings.getString("ChatManager.changeRoomTopic")); //$NON-NLS-1$
			chatRoom.changeSubject(topic);
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.changeSubjectError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatManager.duplicateLoginError")); //$NON-NLS-1$
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
			LOGGER.info(xStrings.getString("ChatManager.logSetNoCalls")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatNoCalls")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.subjectNoCalls")); //$NON-NLS-1$
			
		}else if(callPressed){
			
			callPressed = false;
			shortCutGroup.clearSelection();
			callToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatManager.logSetCalls")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatResumeCalls")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.emptyTopic")); //$NON-NLS-1$
			
		}
			
	}
	
	/**
	 * Toggles back soon by sending a message and changing the topic alert
	 */
	private void backSoonPressed(){
		
		if(breakToggle.isSelected() && !breakPressed){
			
			breakPressed = true;
			callPressed = false;
			helpPressed = false;
			LOGGER.info(xStrings.getString("ChatManager.logSetBackSoon")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatBackSoon")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.subjectBackSoon")); //$NON-NLS-1$
			
		}else if(breakPressed){
			
			breakPressed = false;
			shortCutGroup.clearSelection();
			breakToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatManager.logSetReturned")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatResumeCalls")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.emptyTopic")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * Toggles send help by sending a message and changing the topic alert
	 */
	private void helpPressed(){
		
		if(helpToggle.isSelected() && !helpPressed){
			
			helpPressed = true;
			callPressed = false;
			breakPressed = false;
			LOGGER.info(xStrings.getString("ChatManager.logSetHelpMe")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatHelpMe")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.subjectHelp")); //$NON-NLS-1$
			
		}else if(helpPressed){
			
			helpPressed = false;
			shortCutGroup.clearSelection();
			helpToggle.setSelected(false);
			LOGGER.info(xStrings.getString("ChatManager.logSetPanicOver")); //$NON-NLS-1$
			sendMessage(xStrings.getString("ChatManager.chatCrisisOver")); //$NON-NLS-1$
			changeTopic(xStrings.getString("ChatManager.emptyTopic")); //$NON-NLS-1$
			
		}
		
	}
	
	@Override
	/**
	 * Standard actionListener entry for use with shortcut buttons
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		
		if(evt.getActionCommand().equals("nocalls")) //$NON-NLS-1$
			noCallsPressed();
		else if(evt.getActionCommand().equals("backsoon")) //$NON-NLS-1$
			backSoonPressed();
		else if(evt.getActionCommand().equals("help")) //$NON-NLS-1$
			helpPressed();
		
	}
	
}
