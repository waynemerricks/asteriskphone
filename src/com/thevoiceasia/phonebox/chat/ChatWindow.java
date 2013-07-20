package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import javax.swing.JPanel;

public class ChatWindow extends JPanel {

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	
	public ChatWindow(ChatManager chatManager, String language, String country, String myNickName){
		
		super();
		
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
		chatManager.getChatRoom().addMessageListener(messages);//Messages needs to know about new message packets
		chatManager.getChatRoom().addSubjectUpdatedListener(messages);//Messages also needs to know about topic changes
		messages.subjectUpdated(chatManager.getChatRoom().getSubject(), "");//Send the current topic to messages //$NON-NLS-1$

		this.setLayout(new BorderLayout());
		this.add(new ChatShortcutBar(language, country, chatManager.getChatRoom()), BorderLayout.NORTH);
		this.add(messages, BorderLayout.CENTER);
		this.add(new ChatInputPanel(language, country, chatManager.getChatRoom()), BorderLayout.SOUTH);
		this.add(new UserStatusPanel(), BorderLayout.EAST);
		
	}

}
