package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class ChatInputPanel extends JPanel implements ActionListener, KeyListener{

	/*
	 * Panel for chat input, possibly buttons for shortcuts
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea message;
	private I18NStrings xStrings; //Link to external string resources
	private MultiUserChat chatRoom;
	private static final Logger LOGGER = Logger.getLogger(ChatInputPanel.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	
	/**
	 * Creates a basic text input panel with a send button.
	 * @param language used for I18N
	 * @param country used for I18N
	 * @param chatRoom chat room to send messages to
	 */
	public ChatInputPanel(String language, String country, MultiUserChat chatRoom){
		
		super();
		xStrings = new I18NStrings(language, country);
		this.chatRoom = chatRoom;
		
		this.setLayout(new BorderLayout());
		message = new JTextArea();
		message.setEditable(true);
		message.setLineWrap(true);
		message.setWrapStyleWord(true);
		message.addKeyListener(this);
		
		String sendButtonText = xStrings.getString("ChatManager.buttonSend");  //$NON-NLS-1$
		JButton send = new JButton(sendButtonText);
		send.setToolTipText(xStrings.getString("ChatManager.buttonSendToolTip")); //$NON-NLS-1$
		send.setMnemonic(sendButtonText.substring(0,1).toLowerCase().toCharArray()[0]);
		send.addActionListener(this);
		
		this.add(new JScrollPane(message), BorderLayout.CENTER);
		this.add(send, BorderLayout.EAST);
		
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
	 * Sends a message to the chat room based on the text in the input box
	 */
	private void sendMessage(){
		
		try {
			
			chatRoom.sendMessage(message.getText());
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					message.setText(null);
				}
			});
			
		}catch(XMPPException e){
			showWarning(e, xStrings.getString("ChatManager.chatRoomError")); //$NON-NLS-1$
		}catch(IllegalStateException e){
			showWarning(e, xStrings.getString("ChatManager.duplicateLoginError")); //$NON-NLS-1$
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
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		//Only one action performed by the send button so lets sort it
		sendMessage();
		
	}


	@Override
	public void keyReleased(KeyEvent evt) {

		if(evt.getKeyCode() == KeyEvent.VK_ENTER){
			evt.consume();
			sendMessage();
		}
		
	}

	@Override
	public void keyTyped(KeyEvent evt) {
		
		if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			evt.consume();
		
	}
	
	@Override
	public void keyPressed(KeyEvent evt) {
		
		if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			evt.consume();
		
	}
	

}
