package com.thevoiceasia.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostUserLookup {

	public static void main(String[] args) throws UnknownHostException{
		
		String machineName = InetAddress.getLocalHost().getHostName();
		
		String userName = System.getProperty("user.name"); //$NON-NLS-1$
		
		System.out.println("Host Name: " + machineName); //$NON-NLS-1$
		System.out.println("User Name: " + userName); //$NON-NLS-1$
		
	}

}
