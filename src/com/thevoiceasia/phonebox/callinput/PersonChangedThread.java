package com.thevoiceasia.phonebox.callinput;

public class PersonChangedThread extends Thread{
	
	public PersonChangedThread(SearchPanel searchPanel){
		
		searchPanel.removePersonChangedListeners();
		
	}

}
