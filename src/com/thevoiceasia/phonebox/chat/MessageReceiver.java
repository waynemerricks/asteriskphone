package com.thevoiceasia.phonebox.chat;

import org.jivesoftware.smack.packet.Message;

public interface MessageReceiver {

	
	public void receiveMessage(Message msg);
	
}
