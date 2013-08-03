package com.thevoiceasia.phonebox.asterisk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.jivesoftware.smack.packet.PacketExtension;

public class AsteriskPacketExtension implements PacketExtension{

	private HashMap<String, String> xmlAttributes = new HashMap<String, String>();
	
	public AsteriskPacketExtension(){
		
		//TODO all the info Asterisk needs to send
		
	}
	
	@Override
	public String getElementName() {
		return "AsteriskPacket"; //$NON-NLS-1$
	}

	@Override
	public String getNamespace() {
		return "com.thevoiceasia.phonebox.asterisk"; //$NON-NLS-1$
	}

	@Override
	public String toXML() {
	
		StringBuffer buf = new StringBuffer();
		
		//WRITE OUT XML ROOT NODE
		buf.append("<").append(getElementName()) //$NON-NLS-1$
			.append(" xmlns=\"") //$NON-NLS-1$
			.append(getNamespace())
			.append("\">"); //$NON-NLS-1$
	    
		Iterator<Entry<String, String>> keys = xmlAttributes.entrySet().iterator();
		
		//Add item keys/values
		while(keys.hasNext()){
			
			Entry<String, String> entry = keys.next();
			
			buf.append("<").append(entry.getKey()).append(">")  //$NON-NLS-1$//$NON-NLS-2$
				.append(entry.getValue())
				.append("</").append(entry.getKey()).append(">"); //$NON-NLS-1$//$NON-NLS-2$
			
		}
		
		buf.append("</").append(getElementName()).append(">"); //$NON-NLS-1$//$NON-NLS-2$
		
		return buf.toString();
		
	}

}
