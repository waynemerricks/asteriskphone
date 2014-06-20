package com.thevoiceasia.phonebox.callinput;

public class SearchTerm {

	private boolean number = false, queueNumber = false, empty = true, locked = false;
	private String term = null, queueTerm = null;
	
	public SearchTerm(boolean numberMode, String term) {
		
		this.number = numberMode;
		this.term = term;
		
	}
	
	public synchronized boolean isLocked(){
		
		return locked;
		
	}
	
	public synchronized void setLocked(boolean lock){
		
		locked = lock;
		
		if(!locked)//We just unlocked so move any queued stuff to number/term
			if(queueTerm != null){
			
				term = queueTerm;
				number = queueNumber;
				
				queueTerm = null;
				
			}
		
	}
	
	public boolean isEmpty(){
		
		return empty;
		
	}
	
	public void setEmpty(boolean yes){
		
		empty = yes;
		
	}
	
	public synchronized boolean isNumber(){
		
		return number;
		
	}
	
	public synchronized String getSearchTerm(){
		
		return term;
		
	}
	
	public synchronized void updateSearch(boolean numberMode, String term){
		
		this.number = numberMode;
		this.term = term;
		
	}
	
	public synchronized void queueSearch(boolean numberMode, String term){
		
		this.queueNumber = numberMode;
		this.queueTerm = term;
		
	}

}
