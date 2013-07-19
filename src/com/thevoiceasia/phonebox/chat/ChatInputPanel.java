package com.thevoiceasia.phonebox.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class ChatInputPanel extends JPanel implements ActionListener, KeyListener{

	/*
	 * Panel for chat input, possibly buttons for shortcuts
	 */
	private static final long serialVersionUID = 1L;
	private JTextArea message;
	private I18NStrings xStrings; //Link to external string resources
	private ChatManager chatManager;
	
	public ChatInputPanel(String language, String country, ChatManager chatManager){
		
		super();
		xStrings = new I18NStrings(language, country);
		this.chatManager = chatManager;
		
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
	
	
	private void sendMessage(){
		
		chatManager.sendMessage(message.getText());
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				message.setText(null);
			}
		});
		
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
