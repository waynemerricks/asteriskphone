package com.thevoiceasia.phonebox.callinput;

import com.thevoiceasia.phonebox.records.Person;

public interface PersonChangedListener {

	/**
	 * Notifies the object that implements this interface that the Person object it references
	 * has been changed to a different Person
	 * @param changedTo null = need to create a new person, otherwise swap it to the referenced
	 * Person
	 */
	public void personChanged(Person changedTo);
	
}
