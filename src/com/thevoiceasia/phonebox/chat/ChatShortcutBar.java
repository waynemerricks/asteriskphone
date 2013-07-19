package com.thevoiceasia.phonebox.chat;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

/**
 * TODO
 * @author waynemerricks
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
	private ChatManager chatManager;
	
	/**
	 * TODO 
	 */
	public ChatShortcutBar(String language, String country, ChatManager chatManager){
		
		xStrings = new I18NStrings(language, country);
		this.chatManager = chatManager;
		
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
	 * TODO
	 */
	private void noCallsPressed(){
	
		//TODO Integrate with room TOPIC
		if(callToggle.isSelected()){
			
			LOGGER.info(xStrings.getString("ChatManager.setNoCalls")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatNoCalls")); //$NON-NLS-1$
			
		}else{
			
			LOGGER.info(xStrings.getString("ChatManager.setCalls")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatResumeCalls")); //$NON-NLS-1$
			
		}
			
	}
	
	/**
	 * TODO
	 */
	private void backSoonPressed(){
		
		//TODO Integrate with room TOPIC
		if(breakToggle.isSelected()){
			
			LOGGER.info(xStrings.getString("ChatManager.setBackSoon")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatBackSoon")); //$NON-NLS-1$
			
		}else{
			
			LOGGER.info(xStrings.getString("ChatManager.setReturned")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatResumeCalls")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * TODO
	 */
	private void helpPressed(){
		
		//TODO Integrate with room TOPIC
		if(helpToggle.isSelected()){
			
			LOGGER.info(xStrings.getString("ChatManager.setHelpMe")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatHelpMe")); //$NON-NLS-1$
			
		}else{
			
			LOGGER.info(xStrings.getString("ChatManager.setPanicOver")); //$NON-NLS-1$
			chatManager.sendMessage(xStrings.getString("ChatManager.chatCrisisOver")); //$NON-NLS-1$
			
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
