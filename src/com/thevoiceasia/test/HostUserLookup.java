package com.thevoiceasia.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HostUserLookup {

	public static void main(String[] args) throws UnknownHostException{
		
		String machineName = InetAddress.getLocalHost().getHostName();
		
		String userName = System.getProperty("user.name");
		
		System.out.println("Host Name: " + machineName);
		System.out.println("User Name: " + userName);
		
	}

}
