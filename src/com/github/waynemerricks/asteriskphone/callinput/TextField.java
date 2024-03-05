package com.github.waynemerricks.asteriskphone.callinput;

import javax.swing.JTextField;

public class TextField extends JTextField{

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	/* CLASS VARS */
	private String fieldMapping = null;
	
	public TextField(String fieldMapping) {
	
		super();
		
		this.fieldMapping = fieldMapping;
		
	}
	
	public String getFieldMapping(){
		
		return fieldMapping;
		
	}
	
}