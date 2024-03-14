package com.github.waynemerricks.asteriskphone.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Simple class which sets up a JPanel in border layout with a JScrollPane
 * 
 * Has an addMessage(String) method to add messages to the panel (for a simple
 * XMPP chat program but obviously this part is protocol agnostic)
 * @author Wayne Merricks
 *
 */
public class ChatMessagePanel extends JPanel implements MessageListener, SubjectUpdatedListener, ParticipantStatusListener{

	private JTextPane messages = new JTextPane();
	private Style chatStyle;
	private String myNickName;
	private JLabel topic = new JLabel();
	private I18NStrings xStrings;
	private String language;
	
	private static final Logger LOGGER = Logger.getLogger(ChatMessagePanel.class.getName());//Logger
	private static final Color GREEN = new Color(109, 154, 11);
	private static final long serialVersionUID = 1L;

	/**
	 * Sets up the Message Panel, uses language/country to get the right I18N strings
	 * Nickname is used to highlight your own messages in a different colour
	 * @param language
	 * @param country
	 * @param myNickName
	 */
	public ChatMessagePanel(String language, String country, String myNickName){
		
		super();
		xStrings = new I18NStrings(language, country);
		this.myNickName = myNickName;
		this.language = language;
		this.setLayout(new BorderLayout());
		
		//Setup TextArea
		messages.setEditable(false);
		chatStyle = messages.addStyle("chatStyle", null); 
		
		//Setup Topic
		//Increase font size
		Font t = topic.getFont();
		t = t.deriveFont(Font.BOLD);
		t = t.deriveFont(24F);
		topic.setFont(t);
		topic.setHorizontalTextPosition(JLabel.CENTER);
		topic.setHorizontalAlignment(JLabel.CENTER);
		topic.setText(xStrings.getString("ChatMessagePanel.topicLabel")); 
		
		this.add(topic, BorderLayout.NORTH);
		
		JScrollPane messageScroll = new JScrollPane(messages);
		messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		this.add(messageScroll, BorderLayout.CENTER);
		
	}
	
	/**
	 * Helper method to return the messages text panel
	 * @return
	 */
	public JTextPane getTextPane(){
		return messages;
	}
	
	/**
	 * Sets the chat window text colour for the next string to be inserted
	 * @param c
	 */
	private void setTextColour(Color c){
		
		StyleConstants.setForeground(chatStyle, c);
		
	}
	
	/**
	 * Clears all text in the chat window
	 */
	public void clear(){
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logClear")); 
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				messages.setText(null);
			}
		});
		
	}
	
	/*
	 * MessageListener Methods
	 */
	@Override
	public void subjectUpdated(String subject, EntityFullJid from) {
		// Not using subject changes yet
	}

	@Override
	public void processMessage(Message message) {
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logReceivedMessage") + message); 
		
		String friendlyFrom = message.getFrom().toString();
		
		if(friendlyFrom.contains("/")) 
			friendlyFrom = friendlyFrom.split("/")[1]; 
		
		String b = message.getBody();
		
		if(friendlyFrom.equals(xStrings.getString("ChatMessagePanel.SYSTEM"))){  
			
			//Control Messages, need to clean up message body and act accordingly
			//chatroom@domain/username !ChatManager.chatParticipantLeft!
			if(b.contains("/")) 
				b = b.split("/")[1]; 
			
		}
		
		final String from = friendlyFrom;
		final String body = b + "\n"; 
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				try{
					
					if(from.equals(myNickName))
						setTextColour(Color.RED);
					else if(from.equals("SYSTEM")) 
						setTextColour(GREEN);
					else
						setTextColour(Color.BLUE);
					
					StyledDocument doc = messages.getStyledDocument();
					doc.insertString(doc.getLength(), from + ": ", chatStyle); 
					
					//Add Message in normal black
					if(!from.equals("SYSTEM")) 
						setTextColour(Color.BLACK);
					doc.insertString(doc.getLength(), body, chatStyle);
					
					//Make sure chat scrolls to new message (basically scroll to the end)
					try{
						messages.setCaretPosition(messages.getText().length());
					}catch(IllegalArgumentException e){
						
						/* Stupid Windows machines throw an error here no matter what position
						 * Lets ignore it */
						
					}
					
				}catch(BadLocationException e){
					LOGGER.severe(xStrings.getString("ChatMessagePanel.logErrorInsertingMessage")); 
					e.printStackTrace();
				}
				
			}
		});
		
	}

	public void subjectUpdated(String subject, String from) {
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logSettingTopic") + subject); 
		
		/*
		 * Can't set subject to "" as XMPP server ignores the change.  This causes a bug when the subject is 
		 * set to the same subject as you get an XMPP error.  
		 * 
		 * To work around this, topic is set to a greeter message when we don't want to see it.
		 * 
		 * We then check if the topic is the greeter message and if so, set the topic label to ""
		 */
		
		if(subject != null && subject.equals(xStrings.getString("ChatMessagePanel.emptyTopic"))) 
			subject = null;
			
		final String t = subject;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				topic.setText(t);
				
			}
		});
		
	}

	/**
	 * Internal method to handle MessageBuilder message creation of user joined/left messages
	 * @param participant person who joined/left
	 * @param status status to use for message
	 * @return built message or null if failed
	 */
	private Message buildStatusMessage(String participant, String status) {
		
		Message msg = null;
		MessageBuilder mb = MessageBuilder.buildMessage();
		
		mb.addBody(language, participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantJoined"));
		
		try {
			
			mb.from(JidCreate.bareFrom(xStrings.getString("ChatMessagePanel.SYSTEM")));
			msg = mb.build();
		
		}catch(Exception e) {
			LOGGER.severe(xStrings.getString("ChatMessagePanel.logErrorCreatingStatusMessage")); 
		}
		
		return msg;
		
	}
	
	/** ParticipantStatusListener methods **/
	public void joined(String participant) {
		
		LOGGER.info(participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantJoined"));  
		
		/*
		 * If you send a message with a custom from that isn't your user name, you will get silently kicked from the server
		 * So these are created and sent internally.
		 */
		Message statusMessage = buildStatusMessage(participant, xStrings.getString("ChatMessagePanel.chatParticipantJoined"));
		
		if(statusMessage != null)
			processMessage(statusMessage);
		
	}

	public void left(String participant) {

		LOGGER.info(participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantLeft"));  
		
		/*
		 * If you send a message with a custom from that isn't your user name, you will get silently kicked from the server
		 * So these are created and sent internally.
		 */
		Message statusMessage = buildStatusMessage(participant, xStrings.getString("ChatMessagePanel.chatParticipantLeft"));
		
		if(statusMessage != null)
			processMessage(statusMessage);
		
	}
	
	public void kicked(String participant, String actor, String reason) {
		
		left(participant);
		
	}
	
}
