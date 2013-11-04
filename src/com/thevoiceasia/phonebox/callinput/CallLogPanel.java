package com.thevoiceasia.phonebox.callinput;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.thevoiceasia.phonebox.records.CallLog;
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
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(CallLogPanel.class.getName());//Logger
		
	public CallLogPanel(Connection readConnection, long recordAge, String language, String country) {
		
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
		
		getCallLog(readConnection, recordAge);
		
	}

	/**
	 * Read the call log from the DB
	 * @param readConnection read connection to use
	 * @param recordAge max age of record to retrieve
	 */
	private void getCallLog(Connection readConnection, long recordAge){
		
		Date oldestRecord = new Date(new Date().getTime() - recordAge);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"); //$NON-NLS-1$
		String date = sdf.format(oldestRecord);
		
		//Get the records from callhistory
		String SQL = "SELECT callchannel FROM callhistory WHERE time > "  //$NON-NLS-1$
						+ date + " AND (state = 'Q' OR state = 'A') GROUP BY callchannel " //$NON-NLS-1$
						+ "ORDER BY time DESC"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		Vector<CallLog> callLog = new Vector<CallLog>();
		
		LOGGER.info(xStrings.getString("CallLogPanel.gettingCallHistory")); //$NON-NLS-1$
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	CallLog log = new CallLog(resultSet.getString("callchannel"),  //$NON-NLS-1$
		    			readConnection);
		    	
		    	if(log.isComplete())
		    		callLog.add(log);
		    	
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("CallLogPanel.getLogSQLError")); //$NON-NLS-1$
		}finally {
		    
			if (resultSet != null) {
		        try {
		        	resultSet.close();
		        } catch (SQLException sqlEx) { } // ignore
		        resultSet = null;
		    }
			
		    if (statement != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        statement = null;
		    }
		    
		}
		
	}
	
	public void addCallLog(CallLog log){
		
		//TODO
		
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
		columnNames.add(xStrings.getString("CallLogPanel.locationField")); //$NON-NLS-1$
		columnNames.add(xStrings.getString("CallLogPanel.conversationField")); //$NON-NLS-1$
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}

}
