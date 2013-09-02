package com.thevoiceasia.phonebox.callinput;

import java.awt.Component;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class CallInputField {

	/** STATICS */
	public static final String TYPE_LABEL = "label"; //$NON-NLS-1$
	public static final String TYPE_TEXT_FIELD = "textfield"; //$NON-NLS-1$
	public static final String TYPE_TEXT_AREA = "textarea"; //$NON-NLS-1$
	public static final String TYPE_COMBO = "combo"; //$NON-NLS-1$
	public static final String TYPE_TAB = "tab"; //$NON-NLS-1$
	
	/* CLASS VARS */
	public String name, type, tooltip, options;
	public int id, order, parent;
	private Component component;
	private JLabel label;
	
	public CallInputField(int id, String name, String type, String tooltip, int order, 
			int parent, String options) {
		
		this.id = id;
		this.name = name;
		this.type = type;
		this.tooltip = tooltip;
		this.order = order;
		this.parent = parent;
		this.options = options;
		
		createComponent();
		
	}

	/**
	 * JLabel Check
	 * @return true if JLabel
	 */
	public boolean isLabel(){
		
		boolean label = false;
		
		if(type.equals(TYPE_LABEL))
			label = true;
		
		return label;
		
	}
	
	/**
	 * JTextField check
	 * @return true if JTextField
	 */
	public boolean isTextField(){
	
		boolean text = false;
		
		if(type.equals(TYPE_TEXT_FIELD))
			text = true;
		
		return text;
		
	}
	
	/**
	 * JTextArea check
	 * @return true if JTextArea
	 */
	public boolean isTextArea(){
		
		boolean text = false;
		
		if(type.equals(TYPE_TEXT_AREA))
			text = true;
		
		return text;
	}
	
	/**
	 * JComboBox Check
	 * @return true if JComboBox
	 */
	public boolean isCombo(){
		
		boolean combo = false;
		
		if(type.equals(TYPE_COMBO))
			combo = true;
		
		return combo;
		
	}
	
	/**
	 * Checks if we're a parent tab
	 * @return true if parent tab
	 */
	public boolean isTab(){
		
		boolean tab = false;
		
		if(type.equals(TYPE_TAB))
			tab = true;
		
		return tab;
		
	}
	
	/** 
	 * Make the component
	 */
	private void createComponent(){
		
		if(type.equals(TYPE_LABEL))
			createLabel();
		else if(type.equals(TYPE_TEXT_FIELD))
			createTextField();
		else if(type.equals(TYPE_TEXT_AREA))
			createTextArea();
		else if(type.equals(TYPE_COMBO))
			createCombo();
		
	}
	
	/**
	 * If component is a combobox will attempt to set the combobox to the given item
	 * @param selected item you want to be selected
	 * @return true if successful
	 */
	@SuppressWarnings("unchecked")
	public void setSelected(String selected){
		
		if(type.equals(TYPE_COMBO))
			((JComboBox<String>)component).setSelectedItem(selected);
			
	}
	
	/**
	 * Gets the value of this component
	 * @return
	 */
	@SuppressWarnings("unchecked") //Couldn't get around the JComboBox<String> cast warning
	public String getValue(){
		
		String value = null;
		
		if(type.equals(TYPE_TEXT_FIELD))
			value = ((JTextField)component).getText();
		else if(type.equals(TYPE_TEXT_AREA))
			value = ((JTextArea)component).getText();
		else if(type.equals(TYPE_COMBO) && component instanceof JComboBox)
			value = (String)((JComboBox<String>)component).getSelectedItem();

		return value;
		
	}
	
	/**
	 * Adds an item listener if this is a combobox
	 * @param listener
	 */
	public void addItemListener(ItemListener listener){
		
		if(type.equals(TYPE_COMBO)){
			
			@SuppressWarnings("unchecked")
			JComboBox<String> combo = (JComboBox<String>)component;
			
			combo.addItemListener(listener);
			
		}
		
	}
	
	/**
	 * Adds a key listener if this is a TEXT_FIELD or TEXT_AREA
	 * @param listener
	 */
	public void addKeyListener(KeyListener listener){
		
		if(type.equals(TYPE_TEXT_FIELD)){
			
			JTextField text = (JTextField)component;
			text.addKeyListener(listener);
			
		}else if(type.equals(TYPE_TEXT_AREA)){
			
			JTextArea text = (JTextArea)component;
			text.addKeyListener(listener);
			
		}
			
	}
	
	
	/**
	 * Creates a JComboBox splitting the options field by , to get the items
	 * If name is set, makes a JLabel to go with it
	 */
	private void createCombo() {
		
		String[] itemsArray = options.split(","); //$NON-NLS-1$
		Vector<String> items = new Vector<String>(itemsArray.length);
		
		JComboBox<String> combo = new JComboBox<String>(items);
		combo.setEditable(false);
		
		if(name != null && name.length() > 0){
			
			label = new JLabel(name);
			label.setLabelFor(combo);
			if(tooltip != null && tooltip.length() > 0)
				label.setToolTipText(tooltip);
			
		}
		
		component = combo;
		
	}

	/**
	 * Creates a JTextArea enclosed by a JScrollPane
	 * Uses name to create a label associated with this
	 */
	private void createTextArea() {
		
		JTextArea text = new JTextArea();
		
		if(name != null && name.length() > 0){
			
			label = new JLabel(name);
			label.setLabelFor(text);
			if(tooltip != null && tooltip.length() > 0)
				label.setToolTipText(tooltip);
			
		}
		
		if(tooltip != null && tooltip.length() > 0)
			text.setToolTipText(tooltip);
		
		text.setRows(3);
		text.setWrapStyleWord(true);
		text.setLineWrap(true);
		JScrollPane scroll = new JScrollPane(text);
		
		component = scroll;
		
	}

	/**
	 * Creates an empty JTextField
	 * Uses name to create a label for this field
	 */
	private void createTextField() {
		
		JTextField text = new JTextField();
		
		if(name != null && name.length() > 0){
			
			label = new JLabel(name);
			label.setLabelFor(text);
			if(tooltip != null && tooltip.length() > 0)
				label.setToolTipText(tooltip);
			
		}
		
		if(tooltip != null && tooltip.length() > 0)
			text.setToolTipText(tooltip);
		
		component = text;
		
	}

	/**
	 * Creates a JLabel using the name field
	 */
	private void createLabel() {
		
		JLabel label = new JLabel(name);
		
		if(tooltip != null && tooltip.length() > 0)
			label.setToolTipText(tooltip);
		
		component = label;
		
	}
	
}
