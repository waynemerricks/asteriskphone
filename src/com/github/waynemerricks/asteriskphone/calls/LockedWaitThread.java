package com.github.waynemerricks.asteriskphone.calls;

public class LockedWaitThread implements Runnable {

	private CallInfoPanel resetMe;
	
	public LockedWaitThread(CallInfoPanel panelToReset) {
		
		resetMe = panelToReset;;
		
	}

	@Override
	public void run() {
		
		try{
		
			Thread.sleep(3000);
		
			if(resetMe.getMode() == CallInfoPanel.MODE_CLICKED){
				
				//Reset it
				resetMe.reset();
				
			}
			
		}catch(InterruptedException e){
			
			//Don't care
			
		}

	}

}
