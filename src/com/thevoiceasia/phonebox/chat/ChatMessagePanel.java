package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jivesoftware.smack.packet.Message;

import com.thevoiceasia.phonebox.gui.Client;
/**
 * Simple class which sets up a JPanel in border layout with a JScrollPane
 * 
 * Has an addMessage(String) method to add messages to the panel (for a simple
 * XMPP chat program but obviously this part is protocol agnostic)
 * @author waynemerricks
 *
 */
public class ChatMessagePanel extends JPanel implements MessageReceiver, TopicReceiver{

	private JTextPane messages = new JTextPane();
	private Style chatStyle;
	private String myNickName;
	private JLabel topic = new JLabel();
	
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private I18NStrings xStrings;
	
	public ChatMessagePanel(String language, String country, String myNickName){
		
		super();
		this.myNickName = myNickName;
		this.setLayout(new BorderLayout());
		setupLogging();
		
		//Setup TextArea
		messages.setEditable(false);
		xStrings = new I18NStrings(language, country);
		chatStyle = messages.addStyle("chatStyle", null); //$NON-NLS-1$
		
		//Setup Topic
		//Increase font size
		Font t = topic.getFont();
		t = t.deriveFont(Font.BOLD);
		t = t.deriveFont(24F);
		topic.setFont(t);
		topic.setHorizontalTextPosition(JLabel.CENTER);
		topic.setHorizontalAlignment(JLabel.CENTER);
		topic.setText(xStrings.getString("ChatManager.topicLabel")); //$NON-NLS-1$
		
		JPanel north = new JPanel(new BorderLayout());
		north.add(topic, BorderLayout.SOUTH);
		north.add(new JSeparator(JSeparator.HORIZONTAL));
		
		this.add(north, BorderLayout.NORTH);
		
		JScrollPane messageScroll = new JScrollPane(messages);
		messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(messageScroll, BorderLayout.CENTER);
		
	}
	
	private void setTextColour(Color c){
		
		StyleConstants.setForeground(chatStyle, c);
		
	}
	
	public void clear(){
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				messages.setText(null);
			}
		});
		
	}
	
	/**
	 * Set the Logger object
	 * 
	 */
	private void setupLogging(){
		
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
	
	private static final long serialVersionUID = 1L;

	@Override
	public void receiveMessage(Message message) {
		
		LOGGER.info(xStrings.getString("ChatManager.receivedMessage") + message); //$NON-NLS-1$
		String friendlyFrom = message.getFrom();
		if(friendlyFrom.contains("/")) //$NON-NLS-1$
			friendlyFrom = friendlyFrom.split("/")[1]; //$NON-NLS-1$
		
		final String from = friendlyFrom;
		final String body = message.getBody() + "\n"; //$NON-NLS-1$
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				try{
					
					if(from.equals(myNickName))
						setTextColour(Color.RED);
					else
						setTextColour(Color.BLUE);
					
					StyledDocument doc = messages.getStyledDocument();
					//Add From in RED TODO FROM ME = BLUE?
					doc.insertString(doc.getLength(), from + ": ", chatStyle); //$NON-NLS-1$
					
					//Add Message in normal black
					setTextColour(Color.BLACK);
					doc.insertString(doc.getLength(), body, chatStyle);
					
				}catch(BadLocationException e){
					LOGGER.severe(xStrings.getString("ChatManager.errorInsertingMessage")); //$NON-NLS-1$
					e.printStackTrace();
				}
				
			}
		});
		
	}

	@Override
	public void receiveTopic(String newTopic) {
		
		final String t = newTopic;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				topic.setText(t);
				
			}
		});
		
	}

}
