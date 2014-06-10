package com.thevoiceasia.phonebox.callinput;

import javax.swing.table.AbstractTableModel;

import com.thevoiceasia.phonebox.records.CallLog;

import java.util.Vector;

public class ChangePersonModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private static final int COLUMN_COUNT = 4;
	
	private Vector<CallLog> data = null;
    private String[] columnNames = null;
    
	public ChangePersonModel(Vector<CallLog> data, String[] columnNames) {
		
		this.data = data;
		this.columnNames = columnNames;
		
	}
	
	public void addRow(CallLog log){
		
		data.add(log);
		//Fire Event to show that rows were added after the end of the table
		this.fireTableRowsInserted(data.size() - 1, data.size() - 1);
		
	}
	
	@Override
	public String getColumnName(int column){
		
		return columnNames[column];
		
	}
	
	@Override
	public boolean isCellEditable(int row, int col){
		
		return false;
		
	}
	
	@Override
	public Class<String> getColumnClass(int column){
		
		return String.class;
		
	}

	public String getChannel(int rowIndex){
		
		return data.get(rowIndex).getChannel();
		
	}
	
	@Override
	public int getRowCount() {
	
		return data.size();
		
	}

	@Override
	public int getColumnCount() {
		
		return COLUMN_COUNT;
		
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		
		String value = null;
		
		CallLog row = data.get(rowIndex);
		
		switch(columnIndex){
		
			case 0:
				value = row.getName();
				break;
			case 1:
				value = row.getConversation();
				break;
			case 2:
				value = row.getLocation();
				break;
			case 3:
				value = row.getTime();
				break;
		
		}
		
		return value;
		
	}

	/**
	 * Changes the value of the given channel
	 * @param channel Channel ID
	 * @param field Field name
	 * @param value Value to change to
	 */
	public void changeRow(String channel, String field, String value) {
		
		boolean done = false;
		int i = 0;
		
		while(!done && i < data.size()){
			
			if(data.get(i).getChannel().equals(channel)){
				
				//We found the record so lets change the value
				done = true;
				
				if(field.equals("name")) //$NON-NLS-1$
					data.get(i).setName(value);
				else if(field.equals("location")) //$NON-NLS-1$
					data.get(i).setLocation(value);
				else if(field.equals("conversation")) //$NON-NLS-1$
					data.get(i).setConversation(value);
				else if(field.equals("channel")) //$NON-NLS-1$
					data.get(i).setChannel(value);
				
				//fire data changed on row
				this.fireTableRowsUpdated(i, i);
				
			}
			
			i++;
			
		}
		
	}

}
