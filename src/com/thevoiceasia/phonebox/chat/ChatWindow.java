package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import javax.swing.JPanel;

public class ChatWindow extends JPanel {

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	
	public ChatWindow(){
		
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
		 */
		/*
		 * Contains ConversationPanel
		 *   ChatInputPanel (autohide?)
		 *   UserStatusPanel (toggle hide)
		 */
		this.setLayout(new BorderLayout());
		this.add(new ConversationPanel(), BorderLayout.CENTER);
		this.add(new ChatInputPanel(), BorderLayout.SOUTH);
		this.add(new UserStatusPanel(), BorderLayout.EAST);
		
	}

}
