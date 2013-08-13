package com.thevoiceasia.phonebox.calls;

import java.util.logging.Logger;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class CallManager implements PacketListener{

	private MultiUserChat controlRoom;//room to send control messages to
	private I18NStrings xStrings;
	private static final Logger LOGGER = Logger.getLogger(CallManager.class.getName());//Logger
	
	public CallManager(String language, String country,MultiUserChat controlRoom){
		
		controlRoom.addMessageListener(this);
		
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Internal method to send a message to the control room
	 * @param message
	 */
	private void sendMessage(String message){
	
		try {
			controlRoom.sendMessage(message);
		} catch (XMPPException e) {
			LOGGER.severe(xStrings.getString("CallManager.errorSendingMessage") + message); //$NON-NLS-1$
		}
		
	}
	
	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			if(message.getFrom().equals(xStrings.getString("CallManager.SYSTEM"))){ //$NON-NLS-1$
				
				//Ignore everything unless it came from SYSTEM
				//TODO react to commands
				LOGGER.info(xStrings.getString("CallManager.receivedMessage") + //$NON-NLS-1$
						message.getBody()); 
				sendMessage("OK"); //$NON-NLS-1$
				
			}
			
		}
		
	}
	
}
