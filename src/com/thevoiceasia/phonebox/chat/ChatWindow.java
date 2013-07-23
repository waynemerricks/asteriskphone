package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;

/**
 * Panel containing all the XMPP Chat Components
 * @author Wayne Merricks
 *
 */
public class ChatWindow extends JPanel implements MouseListener {

	/** CLASS VARS **/
	private UserStatusPanel userStatus;
	private boolean statusVisible = false;
	
	/** STATICS **/
	private static final long serialVersionUID = 1L;
	
	/**
	 * Makes a JPanel containing all the chat components.
	 * @param chatManager
	 * @param language
	 * @param country
	 * @param myNickName
	 * @param isStudio true for yes, enables extra buttons and topic label
	 */
	public ChatWindow(ChatManager chatManager, String language, String country, String myNickName, String isStudio){
		
		super();
		
		boolean studio = false;
		
		if(isStudio != null && isStudio.equals("true")) //$NON-NLS-1$
			studio = true;
		
		/*
		 * Auto connect to elastix xmpp
		 * user name = logged in user name (user.home abbrev?)
		 * auto join phonebox chat room
		 * 
		 * Chat Window is just an entry box + scrollpane box for conversation
		 * Buttons for away/available/help me?
		 * Create new scrollpane box to compartmentalise functionality
		 * 
		 * Contains ConversationPanel
		 *   ChatInputPanel (autohide?)
		 *   UserStatusPanel (toggle hide)
		 */
		ChatMessagePanel messages = new ChatMessagePanel(language, country, myNickName);
		chatManager.connect();
		
		if(!chatManager.hasErrors()){
		
			chatManager.getChatRoom().addMessageListener(messages);//Messages needs to know about new message packets
			chatManager.getChatRoom().addMessageListener(chatManager);//ChatManager needs to monitor messages for presence
			chatManager.getChatRoom().addSubjectUpdatedListener(messages);//Messages also needs to know about topic changes
			chatManager.getChatRoom().addParticipantStatusListener(messages);//And when people are joining/leaving
			messages.subjectUpdated(chatManager.getChatRoom().getSubject(), "");//Send the current topic to messages //$NON-NLS-1$
			messages.getTextPane().addMouseListener(this);
			
			this.setLayout(new BorderLayout());
			this.add(new ChatShortcutBar(language, country, chatManager.getChatRoom(), studio), BorderLayout.NORTH);
			this.add(messages, BorderLayout.CENTER);
			this.add(new ChatInputPanel(language, country, chatManager.getChatRoom()), BorderLayout.SOUTH);
			
			userStatus = new UserStatusPanel(language, country, chatManager.getChatRoom());
			chatManager.getChatRoom().addParticipantStatusListener(userStatus);
			chatManager.getChatRoom().addParticipantListener(userStatus);
			userStatus.getTextPane().addMouseListener(new MouseListener(){
				public void mouseClicked(MouseEvent evt){
					hideUserStatus();
				}
	
				public void mouseEntered(MouseEvent e) { }
				public void mouseExited(MouseEvent e) { }
				public void mousePressed(MouseEvent e) { }
				public void mouseReleased(MouseEvent e) { }
			
			});
		
		}
		
	}

	private void showUserStatus(){
		this.add(userStatus, BorderLayout.EAST);
		statusVisible = true;
		validate();
	}
	
	private void hideUserStatus(){
		this.remove(userStatus);
		statusVisible = false;
		validate();
	}
	
	@Override
	public void mouseClicked(MouseEvent arg0) {
		
		if(!statusVisible){
			showUserStatus();
		}else{
			hideUserStatus();
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {}

	@Override
	public void mouseExited(MouseEvent arg0) {}

	@Override
	public void mousePressed(MouseEvent arg0) {}

	@Override
	public void mouseReleased(MouseEvent arg0) {}

}
