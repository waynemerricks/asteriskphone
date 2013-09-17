package com.thevoiceasia.phonebox.callinput;

import javax.swing.JTextArea;

public class AreaField extends JTextArea{

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	/* CLASS VARS */
	private String fieldMapping = null;
	
	public AreaField(String fieldMapping) {
	
		super();
		
		this.fieldMapping = fieldMapping;
		
	}
	
	public String getFieldMapping(){
		
		return fieldMapping;
		
	}
	
}