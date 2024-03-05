package com.github.waynemerricks.asteriskphone.misc;

public interface LastActionTimer {

	/**
	 * Returns a long in the form of Date.getTime() which signifies the last 
	 * time a user performed an action on this object.
	 * @return
	 */
	public long getLastActionTime();
	
}
