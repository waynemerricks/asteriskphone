package com.thevoiceasia.phonebox.callinput;

import java.awt.LayoutManager;

import javax.swing.JPanel;

public class TabPanel extends JPanel {

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	
	/* CLASS VARS */
	public int id;
	public String name;
	
	public TabPanel(int id, String name, LayoutManager layout) {
		super(layout);
		
		this.id = id;
		this.name = name;
		
	}

}
