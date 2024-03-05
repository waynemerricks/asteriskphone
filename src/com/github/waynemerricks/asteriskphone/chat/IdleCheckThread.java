package com.github.waynemerricks.asteriskphone.chat;

/**
 * Simple class that invokes ChatManager.checkIdle every CHECK_INTERVAL period
 * @author Wayne Merricks
 *
 */
public class IdleCheckThread extends Thread {

	private ChatManager chatManager;
	private boolean go = true;
	private static final int CHECK_INTERVAL = 10000;
	
	public IdleCheckThread(ChatManager chatManager){
		
		this.chatManager = chatManager;
		
	}
	
	public void run(){
		
		while(go){
			
			try{
				sleep(CHECK_INTERVAL);
				chatManager.checkIdle();
			}catch(InterruptedException e){
				go = false;
			}
			
		}
		
	}
	
}
