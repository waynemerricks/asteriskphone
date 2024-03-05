package com.github.waynemerricks.asteriskphone.callinput;

public class PersonChangedThread extends Thread{
	
	public PersonChangedThread(SearchPanel searchPanel){
		
		searchPanel.removePersonChangedListeners();
		
	}

}
