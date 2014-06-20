package com.thevoiceasia.phonebox.callinput;

public class SearchPersonThread extends Thread {

	private boolean go = true;
	private SearchPanel search = null;
	
	private static final long SEARCH_INTERVAL = 500l;
	
	public SearchPersonThread(SearchPanel search) {
		
		this.search = search;
		
	}

	@Override
	public void run() {
		
		while(go){
			
			try {
				
				//Fire an update check on the SearchPanel
				search.checkDataNeedsUpdating();
				//Wait for 500ms to prevent db query spam
				Thread.sleep(SEARCH_INTERVAL);
				
			} catch (InterruptedException e) {
				
				go = false;
				
			}
			
		}
	}
	
	public void stopMeGracefully(){
		
		go = false;
		
	}

}
