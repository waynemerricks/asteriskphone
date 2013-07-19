package com.thevoiceasia.phonebox.chat;

import org.jivesoftware.smack.packet.Message;

/**
 * TODO
 * @author waynemerricks
 *
 */
public interface MessageReceiver {

	/**
	 * TODO
	 * @param msg
	 */
	public void receiveMessage(Message msg);
	
}
