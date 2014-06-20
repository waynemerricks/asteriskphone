package com.thevoiceasia.phonebox.callinput;

public class SearchTerm {

	private boolean number = false, queueNumber = false, locked = false;
	private String term = null, queueTerm = null;
	
	/**
	 * Creates a search term
	 * @param numberMode
	 * @param term
	 */
	public SearchTerm(boolean numberMode, String term) {
		
		this.number = numberMode;
		this.term = term;
		
	}
	
	/**
	 * Checks if this term is locked due to being queried
	 * @return
	 */
	public synchronized boolean isLocked(){
		
		return locked;
		
	}
	
	/**
	 * Clears the search term to null
	 */
	public synchronized void clear(){
		
		term = null;
		
	}
	
	/**
	 * Sets the thread locked, if unlocked it will also move
	 * any queued terms to the main terms before unlocking
	 * @param lock
	 */
	public synchronized void setLocked(boolean lock){
		
		locked = lock;
		
		if(!locked)//We just unlocked so move any queued stuff to number/term
			if(queueTerm != null){
			
				term = queueTerm;
				number = queueNumber;
				
				queueTerm = null;
				
			}
		
	}
	
	/**
	 * Returns whether the search term is a number (true) or name (false)
	 * @return
	 */
	public synchronized boolean isNumber(){
		
		return number;
		
	}
	
	/**
	 * Returns the search term
	 * @return
	 */
	public synchronized String getSearchTerm(){
		
		return term;
		
	}
	
	/**
	 * Changes the mode/term to a new mode/term
	 * @param numberMode
	 * @param term
	 */
	public synchronized void updateSearch(boolean numberMode, String term){
		
		this.number = numberMode;
		this.term = term;
		
	}
	
	/**
	 * Changes the queued mode/term to a new mode/term
	 * @param numberMode
	 * @param term
	 */
	public synchronized void queueSearch(boolean numberMode, String term){
		
		this.queueNumber = numberMode;
		this.queueTerm = term;
		
	}

}
