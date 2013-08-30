package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;

import javax.swing.JPanel;

import com.thevoiceasia.phonebox.misc.LastActionTimer;

/**
 * Panel containing all the XMPP Chat Components
 * @author Wayne Merricks
 *
 */
public class ChatWindow extends JPanel implements MouseListener, LastActionTimer {

	/** CLASS VARS **/
	private UserStatusPanel userStatus;
	private boolean statusVisible = false;
	private long lastActionTime;
	
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
			ChatShortcutBar shortcuts = new ChatShortcutBar(language, country, 
					chatManager.getChatRoom(), studio, 
					chatManager.getChatRoom().getSubject());
			chatManager.addActionTimeRecorder(shortcuts, shortcuts.getClass().getName());
			this.add(shortcuts, BorderLayout.NORTH);
			this.add(messages, BorderLayout.CENTER);
			ChatInputPanel input = new ChatInputPanel(language, country, chatManager.getChatRoom());
			chatManager.addActionTimeRecorder(input, input.getClass().getName());
			this.add(input, BorderLayout.SOUTH);
			
			userStatus = new UserStatusPanel(language, country, chatManager.getChatRoom());
			chatManager.getChatRoom().addParticipantStatusListener(userStatus);
			chatManager.getChatRoom().addParticipantListener(userStatus);
			userStatus.getTextPane().addMouseListener(new MouseListener(){
				public void mouseClicked(MouseEvent evt){
					lastActionTime = new Date().getTime();
					hideUserStatus();
				}
	
				public void mouseEntered(MouseEvent e) { }
				public void mouseExited(MouseEvent e) { }
				public void mousePressed(MouseEvent e) { }
				public void mouseReleased(MouseEvent e) { }
			
			});
		
		}
		
		lastActionTime = new Date().getTime();
		chatManager.addActionTimeRecorder(this, ChatWindow.class.getName());
		
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
	public void mouseClicked(MouseEvent evt) {
		
		lastActionTime = new Date().getTime();
		if(!statusVisible){
			showUserStatus();
		}else{
			hideUserStatus();
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent evt) {}

	@Override
	public void mouseExited(MouseEvent evt) {}

	@Override
	public void mousePressed(MouseEvent evt) {}

	@Override
	public void mouseReleased(MouseEvent evt) {}

	@Override
	public long getLastActionTime() {
		return lastActionTime;
	}

}
