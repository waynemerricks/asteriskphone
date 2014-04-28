package com.thevoiceasia.phonebox.callinput;


import java.awt.BorderLayout;
import java.awt.Component;
import java.sql.Connection;

import javax.swing.JDialog;

import com.thevoiceasia.phonebox.records.Person;

public class SearchPanel extends JDialog {

	private static final long serialVersionUID = 1L;
	
	private Person selectedPerson = null;
	
	public SearchPanel(Component owner, String title, String language, String country, 
			Connection readConnection, Connection writeConnection){
		
		this.setLayout(new BorderLayout());
		
	}
	
	public void addPersonChangedListener(PersonChangedListener pcl){
		
		//TODO
		
	}
	
	public Person getSelectedPerson(){
		
		return selectedPerson;
		
	}

}
