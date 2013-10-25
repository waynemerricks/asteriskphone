package com.thevoiceasia.phonebox.callinput;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class MultiLineCellRenderer extends JTextArea implements
		TableCellRenderer {

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	public MultiLineCellRenderer() {
		
		this.setLineWrap(true);
		this.setWrapStyleWord(true);
		
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		// TODO Auto-generated method stub
		
		//Set the colours to match the normal table selection colours
		if(isSelected){
			
			setForeground(table.getSelectionForeground());
			setBackground(table.getSelectionBackground());
			
		}else{//Not Selected
			
		    setForeground(table.getForeground());
		    setBackground(table.getBackground());
		    
		}
		
		//Set the colours to match normal table focus colours
		if(hasFocus)
			setBorder(UIManager.getBorder("Table.focusCellHighlightBorder")); //$NON-NLS-1$
		else
			setBorder(UIManager.getBorder("Table.CellBorder")); //$NON-NLS-1$
		
		//Set the text of this object to the Object value
		if(value != null)
			setText(value.toString());
		else
			setText(""); //$NON-NLS-1$
		
		//Adjust the height to fit the text
		this.setSize(new Dimension(table.getTableHeader().getColumnModel().getColumn(column).
				getWidth(), 1000));
		table.setRowHeight(row, this.getPreferredSize().height);
		
		return this;
		
	}

}
