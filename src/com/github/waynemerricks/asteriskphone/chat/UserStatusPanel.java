package com.github.waynemerricks.asteriskphone.chat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

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

import org.jivesoftware.smack.PresenceListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.ParticipantStatusListener;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

/**
 * Simple online/available/away list of users in the MultiUserChat room
 * @author Wayne Merricks
 *
 */
public class UserStatusPanel extends JPanel implements ParticipantStatusListener, PresenceListener {

	private HashMap<String, Integer> roomRoster = new HashMap<String, Integer>();
	private I18NStrings xStrings; //Link to external string resources
	private MultiUserChat chatRoom;
	private JTextPane onlineList = new JTextPane();
	private Style listStyle;
	private JScrollPane onlineScroll;
	
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(UserStatusPanel.class.getName());//Logger
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
		
		this.setLayout(new BorderLayout());
		this.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
		onlineList.setEditable(false);
		listStyle = onlineList.addStyle("listStyle", null); 
		
		clear();
		onlineScroll = new JScrollPane(onlineList);
		onlineScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		this.add(onlineScroll, BorderLayout.CENTER);
		
		//Get current online list
		if(chatRoom.getOccupantsCount() > 0){
			
			Iterator<EntityFullJid> users = chatRoom.getOccupants().iterator();
			
			while(users.hasNext()){
				
				EntityFullJid user = users.next();
				String friendlyUser = user.toString();
				
				if(friendlyUser.contains("/")) 
					friendlyUser = friendlyUser.split("/")[1]; 
				
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
		
		LOGGER.info(xStrings.getString("UserStatusPanel.logClearOnlineList")); 
		
		setTextColour(Color.BLACK);
		StyledDocument doc = onlineList.getStyledDocument();
				
		try{
			doc.remove(0, doc.getLength());
			doc.insertString(doc.getLength(), xStrings.getString("UserStatusPanel.onlineTitle") + "\n", listStyle);  
		}catch(BadLocationException e){
			LOGGER.severe(xStrings.getString("UserStatusPanel.logErrorClearingOnlineList")); 
			e.printStackTrace();
		}
		
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
							doc.insertString(doc.getLength(), user.getKey() + "\n", listStyle); 
							LOGGER.info(xStrings.getString("UserStatusPanel.logUpdatedOnlineList") + user.getKey() + user.getValue()); 
						}catch(BadLocationException e){
							LOGGER.severe(xStrings.getString("UserStatusPanel.logErrorUpdatingOnlineList") + user.getKey()); 
							e.printStackTrace();
						}
						
					}
					
				}
				
			}
		});
		
	}

	/** ParticipantStatusListener methods **/
	@Override
	public void joined(EntityFullJid participant) {
		
		LOGGER.info(participant + " " + xStrings.getString("UserStatusPanel.logChatParticipantJoined"));  
		String friendlyParticipant = participant.toString();
		
		if(friendlyParticipant.contains("/")) 
			friendlyParticipant = friendlyParticipant.split("/")[1]; 
		
		if(chatRoom.getOccupantPresence(participant).getMode() == Mode.available ||
			chatRoom.getOccupantPresence(participant).getMode() == Mode.chat || 
			chatRoom.getOccupantPresence(participant).getMode() == null)
			roomRoster.put(friendlyParticipant, 2);
		else
			roomRoster.put(friendlyParticipant, 1);
		
		updateRoster();
		
	}

	@Override
	public void left(EntityFullJid participant) {

		LOGGER.info(participant + " " + xStrings.getString("UserStatusPanel.logChatParticipantLeft"));  
		String friendlyParticipant = participant.toString();
		
		if(friendlyParticipant.contains("/")) 
			friendlyParticipant = friendlyParticipant.split("/")[1]; 
		
		roomRoster.remove(friendlyParticipant);
		updateRoster();
		
	}
	
	@Override
	public void kicked(EntityFullJid participant, Jid actor, String reason) {
		
		left(participant);
		
	}

	@Override
	public void processPresence(Presence presence) {
		
		LOGGER.info(xStrings.getString("UserStatusPanel.logPresenceUpdate") + presence.getFrom() + ": " + presence.getMode());  
		String friendlyFrom = presence.getFrom().toString();
		
		if(friendlyFrom.contains("/")) 
				friendlyFrom = friendlyFrom.split("/")[1]; 
		
		if(presence.isAvailable()){
			if(presence.getMode() == Mode.available || presence.getMode() == Mode.chat || presence.getMode() == null)
				roomRoster.put(friendlyFrom, 2);
			else
				roomRoster.put(friendlyFrom, 1);
		}else
			roomRoster.remove(friendlyFrom);
		
		updateRoster();
		
	}
	
}
