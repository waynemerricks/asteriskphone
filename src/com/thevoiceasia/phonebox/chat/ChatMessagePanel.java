package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jivesoftware.smackx.muc.SubjectUpdatedListener;

/**
 * Simple class which sets up a JPanel in border layout with a JScrollPane
 * 
 * Has an addMessage(String) method to add messages to the panel (for a simple
 * XMPP chat program but obviously this part is protocol agnostic)
 * @author Wayne Merricks
 *
 */
public class ChatMessagePanel extends JPanel implements PacketListener, SubjectUpdatedListener, ParticipantStatusListener{

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
		chatStyle = messages.addStyle("chatStyle", null); //$NON-NLS-1$
		
		//Setup Topic
		//Increase font size
		Font t = topic.getFont();
		t = t.deriveFont(Font.BOLD);
		t = t.deriveFont(24F);
		topic.setFont(t);
		topic.setHorizontalTextPosition(JLabel.CENTER);
		topic.setHorizontalAlignment(JLabel.CENTER);
		topic.setText(xStrings.getString("ChatMessagePanel.topicLabel")); //$NON-NLS-1$
		
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
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logClear")); //$NON-NLS-1$
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				messages.setText(null);
			}
		});
		
	}
	
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logReceivedMessage") + XMPPPacket); //$NON-NLS-1$
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			String friendlyFrom = message.getFrom();
			if(friendlyFrom.contains("/")) //$NON-NLS-1$
				friendlyFrom = friendlyFrom.split("/")[1]; //$NON-NLS-1$
			
			String b = message.getBody();
			
			if(friendlyFrom.equals(xStrings.getString("ChatMessagePanel.SYSTEM"))){  //$NON-NLS-1$
				
				//Control Messages, need to clean up message body and act accordingly
				//chatroom@domain/username !ChatManager.chatParticipantLeft!
				if(b.contains("/")) //$NON-NLS-1$
					b = b.split("/")[1]; //$NON-NLS-1$
				
			}
			
			final String from = friendlyFrom;
			final String body = b + "\n"; //$NON-NLS-1$
			
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					
					try{
						
						if(from.equals(myNickName))
							setTextColour(Color.RED);
						else if(from.equals("SYSTEM")) //$NON-NLS-1$
							setTextColour(GREEN);
						else
							setTextColour(Color.BLUE);
						
						StyledDocument doc = messages.getStyledDocument();
						doc.insertString(doc.getLength(), from + ": ", chatStyle); //$NON-NLS-1$
						
						//Add Message in normal black
						if(!from.equals("SYSTEM")) //$NON-NLS-1$
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
						LOGGER.severe(xStrings.getString("ChatMessagePanel.logErrorInsertingMessage")); //$NON-NLS-1$
						e.printStackTrace();
					}
					
				}
			});
			
		}
		
	}

	@Override
	public void subjectUpdated(String subject, String from) {
		
		LOGGER.info(xStrings.getString("ChatMessagePanel.logSettingTopic") + subject); //$NON-NLS-1$
		
		/*
		 * Can't set subject to "" as XMPP server ignores the change.  This causes a bug when the subject is 
		 * set to the same subject as you get an XMPP error.  
		 * 
		 * To work around this, topic is set to a greeter message when we don't want to see it.
		 * 
		 * We then check if the topic is the greeter message and if so, set the topic label to ""
		 */
		
		if(subject != null && subject.equals(xStrings.getString("ChatMessagePanel.emptyTopic"))) //$NON-NLS-1$
			subject = null;
			
		final String t = subject;
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				topic.setText(t);
				
			}
		});
		
	}

	/** ParticipantStatusListener methods **/
	@Override
	public void joined(String participant) {
		
		LOGGER.info(participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantJoined"));  //$NON-NLS-1$//$NON-NLS-2$
		
		/*
		 * If you send a message with a custom from that isn't your user name, you will get silently kicked from the server
		 * So these are created and sent internally.
		 */
		Message joinedMessage = new Message();
		joinedMessage.addBody(language, participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantJoined"));  //$NON-NLS-1$//$NON-NLS-2$
		joinedMessage.setFrom(xStrings.getString("ChatMessagePanel.SYSTEM")); //$NON-NLS-1$
		processPacket(joinedMessage);
		
	}

	@Override
	public void left(String participant) {

		LOGGER.info(participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantLeft"));  //$NON-NLS-1$//$NON-NLS-2$
		
		/*
		 * If you send a message with a custom from that isn't your user name, you will get silently kicked from the server
		 * So these are created and sent internally.
		 */
		Message leftMessage = new Message();
		leftMessage.addBody(language, participant + " " + xStrings.getString("ChatMessagePanel.chatParticipantLeft")); //$NON-NLS-1$//$NON-NLS-2$
		leftMessage.setFrom(xStrings.getString("ChatMessagePanel.SYSTEM")); //$NON-NLS-1$
		processPacket(leftMessage);
		
	}
	
	@Override
	public void kicked(String participant, String actor, String reason) {
		
		left(participant);
		
	}
	
	/** UNUSED ParticipantStatusListener methods **/
	@Override
	public void adminGranted(String participant) {}

	@Override
	public void adminRevoked(String participant) {}

	@Override
	public void banned(String participant, String actor, String reason) {}

	@Override
	public void membershipGranted(String participant) {}

	@Override
	public void membershipRevoked(String participant) {}

	@Override
	public void moderatorGranted(String participant) {}

	@Override
	public void moderatorRevoked(String participant) {}

	@Override
	public void nicknameChanged(String participant, String newNick) {}

	@Override
	public void ownershipGranted(String participant) {}

	@Override
	public void ownershipRevoked(String participant) {}

	@Override
	public void voiceGranted(String participant) {}

	@Override
	public void voiceRevoked(String participant) {}
	
}
