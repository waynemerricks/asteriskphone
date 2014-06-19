package com.thevoiceasia.phonebox.callinput;

import javax.swing.table.AbstractTableModel;

import com.thevoiceasia.phonebox.records.Person;

import java.util.Vector;

public class ChangePersonModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private static final int COLUMN_COUNT = 3;
	
	private Vector<Person> data = null;
    private String[] columnNames = null;
    
	public ChangePersonModel(Vector<Person> data, String[] columnNames) {
		
		this.data = data;
		this.columnNames = columnNames;
		
	}
	
	public void addRow(Person person){
		
		data.add(person);
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
		
		Person person = data.get(rowIndex);
		
		switch(columnIndex){
		
			case 0:
				value = person.name;
				break;
			case 1:
				value = person.location;
				break;
			case 2:
				value = person.number;
				break;
		
		}
		
		return value;
		
	}

	/**
	 * Changes the value of the given channel
	 * @param id person record ID
	 * @param field Field name
	 * @param value Value to change to
	 */
	public void changeRow(int id, String field, String value) {
		
		boolean done = false;
		int i = 0;
		
		while(!done && i < data.size()){
			
			if(data.get(i).id == id){
				
				//We found the record so lets change the value
				done = true;
				
				if(field.equals("name")) //$NON-NLS-1$
					data.get(i).name = value;
				else if(field.equals("location")) //$NON-NLS-1$
					data.get(i).location = value;
				else if(field.equals("number")) //$NON-NLS-1$
					data.get(i).number = value;
				
				//fire data changed on row
				this.fireTableRowsUpdated(i, i);
				
			}
			
			i++;
			
		}
		
	}

}
