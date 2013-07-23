package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;

/**
 * Simple online/available/away list of users in the MultiUserChat room
 * @author Wayne Merricks
 *
 */
public class UserStatusPanel extends JPanel implements ParticipantStatusListener, PacketListener {

	private HashMap<String, Integer> roomRoster = new HashMap<String, Integer>();
	private I18NStrings xStrings; //Link to external string resources
	private MultiUserChat chatRoom;
	private JTextPane onlineList = new JTextPane();
	private Style listStyle;
	private JScrollPane onlineScroll;
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(UserStatusPanel.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static final Color GREEN = new Color(109, 154, 11);
	
	/**
	 * Creates a JPanel containing all the current users of the room, which auto updates
	 * @param language
	 * @param country
	 * @param chatRoom
	 */
	public UserStatusPanel(String language, String country, MultiUserChat chatRoom){
		
		this.chatRoom = chatRoom;
		
		xStrings = new I18NStrings(language, country);
		setupLogging();
		
		this.setLayout(new BorderLayout());
		this.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
		onlineList.setEditable(false);
		listStyle = onlineList.addStyle("listStyle", null); //$NON-NLS-1$
		
		clear();
		onlineScroll = new JScrollPane(onlineList);
		onlineScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.add(onlineScroll, BorderLayout.CENTER);
		
		//Get current online list
		if(chatRoom.getOccupantsCount() > 0){
			
			Iterator<String> users = chatRoom.getOccupants();
			
			while(users.hasNext()){
				
				String user = users.next();
				String friendlyUser = user;
				
				if(friendlyUser.contains("/")) //$NON-NLS-1$
					friendlyUser = friendlyUser.split("/")[1]; //$NON-NLS-1$
				
				if(chatRoom.getOccupantPresence(user).getMode() == Mode.available ||
						chatRoom.getOccupantPresence(user).getMode() == Mode.chat || 
						chatRoom.getOccupantPresence(user).getMode() == null)
						roomRoster.put(friendlyUser, 2);
					else
						roomRoster.put(friendlyUser, 1);
				
			}
			
		}
		
		updateRosterDelayed();
		
	}
	
	/**
	 * Helper method, polls for panel visible every second.  
	 * Then runs update roster.
	 */
	private void updateRosterDelayed(){
		
		new Thread(){
			
			public void run(){
				
				boolean interrupted = false;
				
				while(!isVisible()){
					
					try {
						sleep(1000);
					} catch (InterruptedException e) {}
					
				}
				
				if(!interrupted)
					updateRoster();	
				
			}
		}.start();
		
	}
	
	public JTextPane getTextPane(){
		return onlineList;
	}
	
	/**
	 * Clears all the users in the online list
	 */
	private void clear(){
		
		LOGGER.info(xStrings.getString("UserStatusPanel.logClearOnlineList")); //$NON-NLS-1$
		
		setTextColour(Color.BLACK);
		StyledDocument doc = onlineList.getStyledDocument();
				
		try{
			doc.remove(0, doc.getLength());
			doc.insertString(doc.getLength(), xStrings.getString("UserStatusPanel.onlineTitle") + "\n", listStyle); //$NON-NLS-1$ //$NON-NLS-2$
		}catch(BadLocationException e){
			LOGGER.severe(xStrings.getString("UserStatusPanel.logErrorClearingOnlineList")); //$NON-NLS-1$
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Set the Logger object
	 */
	public void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.info(xStrings.getString("UserStatusPanel.logSetupLogging")); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("chatLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("UserStatusPanel.loggerCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("UserStatusPanel.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("UserStatusPanel.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	/**
	 * Sets the chat window text colour for the next string to be inserted
	 * @param c
	 */
	private void setTextColour(Color c){
		
		StyleConstants.setForeground(listStyle, c);
		
	}
	
	/**
	 * Updates the online roster
	 */
	private void updateRoster(){
	
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				
				clear();
						
				if(roomRoster.size() > 0){
					
					Iterator<Entry<String, Integer>> userList = roomRoster.entrySet().iterator();
					
					while(userList.hasNext()){
						
						Entry<String, Integer> user = userList.next();
						
						if(user.getValue() == 2)//Put User in GREEN
							setTextColour(GREEN);
						else if(user.getValue() == 1)
							setTextColour(Color.ORANGE);
							
						try{
							StyledDocument doc = onlineList.getStyledDocument();
							doc.insertString(doc.getLength(), user.getKey() + "\n", listStyle); //$NON-NLS-1$
							LOGGER.info(xStrings.getString("UserStatusPanel.logUpdatedOnlineList") + user.getKey() + user.getValue()); //$NON-NLS-1$
						}catch(BadLocationException e){
							LOGGER.severe(xStrings.getString("UserStatusPanel.logErrorUpdatingOnlineList") + user.getKey()); //$NON-NLS-1$
							e.printStackTrace();
						}
						
					}
					
				}
				
			}
		});
		
	}

	/** ParticipantStatusListener methods **/
	@Override
	public void joined(String participant) {
		
		LOGGER.info(participant + " " + xStrings.getString("UserStatusPanel.logChatParticipantJoined"));  //$NON-NLS-1$//$NON-NLS-2$
		String friendlyParticipant = participant;
		
		if(friendlyParticipant.contains("/")) //$NON-NLS-1$
			friendlyParticipant = friendlyParticipant.split("/")[1]; //$NON-NLS-1$
		
		if(chatRoom.getOccupantPresence(participant).getMode() == Mode.available ||
			chatRoom.getOccupantPresence(participant).getMode() == Mode.chat || 
			chatRoom.getOccupantPresence(participant).getMode() == null)
			roomRoster.put(friendlyParticipant, 2);
		else
			roomRoster.put(friendlyParticipant, 1);
		
		updateRoster();
		
	}

	@Override
	public void left(String participant) {

		LOGGER.info(participant + " " + xStrings.getString("UserStatusPanel.logChatParticipantLeft"));  //$NON-NLS-1$//$NON-NLS-2$
		String friendlyParticipant = participant;
		
		if(friendlyParticipant.contains("/")) //$NON-NLS-1$
			friendlyParticipant = friendlyParticipant.split("/")[1]; //$NON-NLS-1$
		
		roomRoster.remove(friendlyParticipant);
		updateRoster();
		
	}
	
	@Override
	public void kicked(String participant, String actor, String reason) {
		
		left(participant);
		
	}

	@Override
	public void nicknameChanged(String participant, String newNick) {
		// TODO Might be interested in this but no docs in Smack API so just guessing at terms
		LOGGER.info(xStrings.getString("UserStatusPanel.logNickNameChanged") + " " + participant + " " + newNick); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		
	}

	/** UNUSED ParticipantStatusListener methods **/
	@Override
	public void banned(String participant, String actor, String reason) {}

	@Override
	public void adminGranted(String participant) {}

	@Override
	public void adminRevoked(String participant) {}

	@Override
	public void membershipGranted(String participant) {}

	@Override
	public void membershipRevoked(String participant) {}

	@Override
	public void moderatorGranted(String participant) {}

	@Override
	public void moderatorRevoked(String participant) {}

	@Override
	public void ownershipGranted(String participant) {}

	@Override
	public void ownershipRevoked(String participant) {}

	@Override
	public void voiceGranted(String participant) {}

	@Override
	public void voiceRevoked(String participant) {}
	/** END UNUSED ParticipantStatusListener methods **/

	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Presence){
			
			Presence p = (Presence)XMPPPacket;
			LOGGER.info(xStrings.getString("UserStatusPanel.logPresenceUpdate") + p.getFrom() + ": " + p.getMode()); //$NON-NLS-1$ //$NON-NLS-2$
			String friendlyFrom = p.getFrom();
			
			if(friendlyFrom.contains("/")) //$NON-NLS-1$
					friendlyFrom = friendlyFrom.split("/")[1]; //$NON-NLS-1$
			
			if(p.isAvailable()){
				if(p.getMode() == Mode.available || p.getMode() == Mode.chat || p.getMode() == null)
					roomRoster.put(friendlyFrom, 2);
				else
					roomRoster.put(friendlyFrom, 1);
			}else
				roomRoster.remove(friendlyFrom);
			
			updateRoster();
			
		}
		
	}
	
}
