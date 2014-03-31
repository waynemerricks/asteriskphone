package com.thevoiceasia.phonebox.callinput;

import java.awt.Color;
import java.awt.Component;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import com.thevoiceasia.phonebox.chat.ChatManager;
import com.thevoiceasia.phonebox.records.CallLog;

/**
 * Show a table with the call log for the last time period (undecided)
 * @author waynemerricks
 *
 */
public class CallLogPanel implements PacketListener {

	/* CLASS VARS */
	private I18NStrings xStrings;
	private String[] columnNames = null;
	private CallLogModel tableModel;
	private JTable history;
	private String language, country;
	private HashMap<String, CallLog> records = new HashMap<String, CallLog>();
	private long maxRecordAge = 3600000L;
	private Connection readConnection = null;
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(CallLogPanel.class.getName());//Logger
		
	public CallLogPanel(Connection readConnection, long maxRecordAge, 
			String language, String country, ChatManager manager) {
	
		this.language = language;
		this.country = country;
		xStrings = new I18NStrings(language, country);
		this.readConnection = readConnection;
		this.maxRecordAge = maxRecordAge;
		
		//Create the Table
		buildTableColumns();
		
		getCallLog(readConnection);
		
		Iterator<String> keys = records.keySet().iterator();
		Vector<CallLog> tableData = new Vector<CallLog>();
		
		while(keys.hasNext()){
			
			String key = keys.next();
			tableData.add(records.get(key));
			
		}
		
		tableModel = new CallLogModel(tableData, columnNames);
		
		history = new JTable(tableModel){
			
			private static final long serialVersionUID = 1L;

			public Component prepareRenderer(TableCellRenderer renderer, 
					int row, int column){
				
				Component c = super.prepareRenderer(renderer, row, column);
				
				Color backgroundColour = null;
				
				if(row % 2 == 0)//if we're an odd row go green
					backgroundColour = Color.WHITE;	
				else
					backgroundColour = new Color(189, 224, 194);
				
				if(c instanceof MultiLineCellRenderer){
					
					//Setup an attribute Set with the wanted background colour
					SimpleAttributeSet bgAttributes = new SimpleAttributeSet();
					StyleConstants.setBackground(bgAttributes, backgroundColour);
					
					//Convert the component to a MultiLineCellRenderer so we can set the Document
					MultiLineCellRenderer textPane = (MultiLineCellRenderer)c;
					textPane.setContentType("text/html"); //$NON-NLS-1$
					
					//Set the document to our wanted attributes
					textPane.getStyledDocument().setParagraphAttributes(0, 
								textPane.getDocument().getLength(), bgAttributes, false);
					
					//Make sure the inner border is the same colour so it doesn't look weird
					textPane.setBorder(BorderFactory.createEmptyBorder());
					
					//Set the Altered TextPane to return
					c = textPane;
					
				}else{
					
					JLabel l = (JLabel)c;
					l.setHorizontalAlignment(JLabel.CENTER);
					
					c.setBackground(backgroundColour);
				
				}
				
				return c;
				
			}
			
		};
		
		history.setRowSelectionAllowed(false);
		history.setAutoCreateRowSorter(true);
		
		//Set to Date DESC by default
		history.getRowSorter().toggleSortOrder(3);
		history.getRowSorter().toggleSortOrder(3);
		
		//Add a listener to the chat room to listen for new calls and field updates
		manager.getControlChatRoom().addMessageListener(this);
		
	}

	/**
	 * Read the call log from the DB, uses LogMaxAge in settings db
	 * to determine how far back to grab records 
	 * @param readConnection read connection to use
	 */
	private void getCallLog(Connection readConnection){
		
		Date oldestRecord = new Date(new Date().getTime() - maxRecordAge);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"); //$NON-NLS-1$
		String date = sdf.format(oldestRecord);
		
		//Get the records from callhistory
		String SQL = "SELECT callchannel FROM callhistory WHERE time > "  //$NON-NLS-1$
						+ date + " AND (state = 'Q' OR state = 'A') GROUP BY callchannel " //$NON-NLS-1$
						+ "ORDER BY time DESC"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("CallLogPanel.gettingCallHistory")); //$NON-NLS-1$
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	CallLog log = new CallLog(language, country,
		    			resultSet.getString("callchannel"),  //$NON-NLS-1$
		    			readConnection);
		    	
		    	if(log.isComplete())
		    		records.put(log.getChannel(), log);
		    	else{
		    		
		    		log = new CallLog(language, country, 
		    				resultSet.getString("callchannel"),  //$NON-NLS-1$
		    				readConnection, true);
		    		records.put(log.getChannel(), log);
		    		
		    	}
		    		
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
	
	/**
	 * Adds a CallLog to the table
	 * @param log
	 */
	public void addCallLog(CallLog log){
		
		//Add to record log
		records.put(log.getChannel(), log);
		
		//Add to table model
		tableModel.addRow(log);
		
	}
	
	/**
	 * Updates the call log table with new data
	 * @param log
	 */
	public void amendCallLog(CallLog log){
		
		records.put(log.getChannel(), log);
		
		tableModel.addRow(log);
		
	}
	
	/**
	 * Gets the table encapsulated in this class
	 * @return
	 */
	public JTable getTable(){
		return history;
	}
	
	private void buildTableColumns(){
		
		//TODO read this from db instead of hard coding
		columnNames = new String[4];
		
		columnNames[0] = xStrings.getString("CallLogPanel.nameField"); //$NON-NLS-1$
		columnNames[1] = xStrings.getString("CallLogPanel.conversationField"); //$NON-NLS-1$
		columnNames[2] = xStrings.getString("CallLogPanel.locationField"); //$NON-NLS-1$
		columnNames[3] = xStrings.getString("CallerHistoryPanel.timeField"); //$NON-NLS-1$
		
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

	@Override
	public void processPacket(Packet XMPPPacket) {
		
		if(XMPPPacket instanceof Message){
			
			Message message = (Message)XMPPPacket;
			
			//React to commands thread all of this if performance is a problem
			LOGGER.info(xStrings.getString("CallLogPanel.receivedMessage") + //$NON-NLS-1$
						message.getBody());
				
			String[] command = message.getBody().split("/"); //$NON-NLS-1$
			
			//UPDATEFIELD
			if(command.length == 4 && command[0].equals(
					xStrings.getString("CallLogPanel.commandUpdateField"))){ //$NON-NLS-1$
				
				/* Only care about name, conversation, location
				 * TODO Change this to read fields wanted from DB
				 */
				if(command[1].equals(xStrings.getString("CallLogPanel.name"))){ //$NON-NLS-1$
					
					//TODO
					LOGGER.info(xStrings.getString("CallLogPanel.logNameUpdate")); //$NON-NLS-1$
					
				}else if(command[1].equals(xStrings.getString("CallLogPanel.conversation"))){ //$NON-NLS-1$
					
					//TODO
					LOGGER.info(xStrings.getString("CallLogPanel.logConversationUpdate")); //$NON-NLS-1$
					
				}else if(command[1].equals(xStrings.getString("CallLogPanel.location"))){ //$NON-NLS-1$
					
					//TODO
					LOGGER.info(xStrings.getString("CallLogPanel.logLocationUpdate")); //$NON-NLS-1$
					
				}
			
			}else if(command.length == 4 && command[0].equals(
					xStrings.getString("CallLogPanel.commandQueue"))){//CALL //$NON-NLS-1$ 
				
				//Add to call log table
				LOGGER.info(xStrings.getString("CallLogPanel.logQUEUE")); //$NON-NLS-1$
				
				CallLog log = new CallLog(language, country,
		    			command[3],
		    			readConnection, true);
		    	
		    	records.put(log.getChannel(), log);
		    	amendCallLog(log);
		    		
		    }
			
		}	

	}

}
