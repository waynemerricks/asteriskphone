package com.thevoiceasia.phonebox.callinput;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.thevoiceasia.phonebox.records.Conversation;

/**
 * Show a table with the call log for the last time period (undecided)
 * @author waynemerricks
 *
 */
public class CallLogPanel {

	/* CLASS VARS */
	private I18NStrings xStrings;
	private Vector<String> columnNames = new Vector<String>();
	private DefaultTableModel tableModel;
	private JTable history;
	
	public CallLogPanel(String language, String country) {
		
		xStrings = new I18NStrings(language, country);
		
		//Create the Table
		buildTableColumns();
		
		tableModel = new DefaultTableModel(){
			
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int col){
				return false;
			}
			
			public Class<String> getColumnClass(int column){
				
				return String.class;
				
			}
			
		};
		
		history = new JTable(tableModel);
		history.setRowSelectionAllowed(true);
		history.setAutoCreateRowSorter(true);
		
	}
	
	/**
	 * Gets the table encapsulated in this class
	 * @return
	 */
	public JTable getTable(){
		return history;
	}
	
	/**
	 * Method to set the data of the history tab
	 * @param conversations
	 */
	public void setConversationHistory(Vector<Conversation> conversations){
		
		tableModel.setDataVector(getTableData(conversations), columnNames);
		
		history.getColumnModel().getColumn(0).setPreferredWidth(10);
		history.getColumnModel().getColumn(1).setCellRenderer(new MultiLineCellRenderer());
		
	}
	
	/**
	 * Converts Vector<Conversation> into Vector<Vector<String>> suitable for use
	 * with table model
	 * @param conversations
	 * @return
	 */
	private Vector<Vector<String>> getTableData(Vector<Conversation> conversations){
		
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		
		Iterator<Conversation> iterator = conversations.iterator();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM dd HH:mm:ss"); //$NON-NLS-1$
		
		while(iterator.hasNext()){
			
			Conversation c = iterator.next();
			
			Vector<String> row = new Vector<String>();
			row.add(sdf.format(c.getTime()));
			row.add(c.getConversation());
			
			data.add(row);
			
		}
		
		return data;
		
	}
	
	private void buildTableColumns(){
		
		columnNames.add(xStrings.getString("CallerHistoryPanel.timeField")); //$NON-NLS-1$
		columnNames.add(xStrings.getString("CallLogPanel.nameField")); //$NON-NLS-1$
		columnNames.add(xStrings.getString("CallLogPanel.locaionField")); //$NON-NLS-1$
		columnNames.add(xStrings.getString("CallLogPanel.conversationField")); //$NON-NLS-1$
		
	}

}
