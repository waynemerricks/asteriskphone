package com.thevoiceasia.phonebox.callinput;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.table.TableCellRenderer;

public class MultiLineCellRenderer extends JTextPane implements
		TableCellRenderer {

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	public MultiLineCellRenderer() {
		
		//this.setLineWrap(true);
		//this.setWrapStyleWord(true);
		
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		//Set the text of this object to the Object value
		if(value != null)
			setText(value.toString());
		else
			setText("");
		
		//Adjust the height to fit the text
		this.setSize(new Dimension(table.getTableHeader().getColumnModel().getColumn(column).
				getWidth(), 1000));
		table.setRowHeight(row, this.getPreferredSize().height);
		
		return this;
		
	}

}
