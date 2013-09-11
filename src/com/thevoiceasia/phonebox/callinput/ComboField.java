package com.thevoiceasia.phonebox.callinput;

import java.util.HashMap;
import java.util.Vector;

import javax.swing.JComboBox;

public class ComboField extends JComboBox<String>{

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	/* CLASS VARS */
	private HashMap<String, String> itemMapping = null;
	
	public ComboField(Vector<String> items, HashMap<String, String> itemMapping) {
	
		super(items);
		
		this.itemMapping = itemMapping;
		this.setEditable(false);
		
	}
	
	/**
	 * Returns what the selected value of the combobox is mapped to in the DB/Program
	 * @param item
	 * @return
	 */
	public String getItemMapping(String item){
		
		String map = null;
		
		if(itemMapping != null)
			map = itemMapping.get(item);
		
		return map;
		
	}

}
