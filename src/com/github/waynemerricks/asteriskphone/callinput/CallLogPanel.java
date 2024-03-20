package com.github.waynemerricks.asteriskphone.callinput;

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

import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jxmpp.jid.EntityBareJid;

import com.github.waynemerricks.asteriskphone.chat.ChatManager;
import com.github.waynemerricks.asteriskphone.records.CallLog;

/**
 * Show a table with the call log for the last time period (undecided)
 * @author waynemerricks
 *
 */
public class CallLogPanel implements MessageListener, IncomingChatMessageListener {

	/* CLASS VARS */
	private I18NStrings xStrings;
	private String[] columnNames = null;
	private CallLogModel tableModel;
	private JTable history;
	private String language, country;
	private HashMap<String, CallLog> records = new HashMap<String, CallLog>();
	private long maxRecordAge = 3600000L;
	private Connection readConnection = null;
	private String incomingQueue = null, onairQueue = null;
	private HashMap<String, String> channelSwapList = new HashMap<String, String>();
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(CallLogPanel.class.getName());//Logger
		
	public CallLogPanel(Connection readConnection, long maxRecordAge, 
			String language, String country, ChatManager manager, String incomingQueue, 
			String onairQueue) {

		this.language = language;
		this.country = country;
		xStrings = new I18NStrings(language, country);
		this.readConnection = readConnection;
		this.maxRecordAge = maxRecordAge;
		this.incomingQueue = incomingQueue;
		this.onairQueue = onairQueue;
		
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
					textPane.setContentType("text/html"); 
					
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
		
		//Set the conversation tab as multiline
		history.getColumnModel().getColumn(1).setCellRenderer(new MultiLineCellRenderer());
		history.getColumnModel().getColumn(3).setPreferredWidth(5);
		
		//Add a listener to the chat room to listen for new calls and field updates
		manager.getControlChatRoom().addMessageListener(this);
		
		//Add Private Chat Listener
		org.jivesoftware.smack.chat2.ChatManager.getInstanceFor(manager.getConnection()).addIncomingListener(this);
		
	}

	/**
	 * Read the call log from the DB, uses LogMaxAge in settings db
	 * to determine how far back to grab records 
	 * @param readConnection read connection to use
	 */
	private void getCallLog(Connection readConnection){
		
		Date oldestRecord = new Date(new Date().getTime() - maxRecordAge);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss"); 
		String date = sdf.format(oldestRecord);
		
		//Get the records from callhistory
		String SQL = "SELECT callchannel FROM callhistory WHERE time > "  
						+ date + " AND (state = 'Q' OR state = 'A') GROUP BY callchannel " 
						+ "ORDER BY time DESC"; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("CallLogPanel.gettingCallHistory")); 
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	CallLog log = new CallLog(language, country,
		    			resultSet.getString("callchannel"),  
		    			readConnection);
		    	
		    	if(log.isComplete())
		    		records.put(log.getChannel(), log);
		    	else{
		    		
		    		log = new CallLog(language, country, 
		    				resultSet.getString("callchannel"),  
		    				readConnection, true);
		    		
		    		/* BUG FIX: Can get into a situation where a person does not exist
		    		 * in any records, this is usually when its an internal phone
		    		 * calling somewhere and its originator channel ends up in the
		    		 * call history.
		    		 * 
		    		 * These internal phones may never have been dealt with in the phone
		    		 * system so won't have a person/location attached.
		    		 * 
		    		 * To check this, see if we have a valid log time otherwise discard
		    		 * 
		    		 * If the time is null as part of error checking we gen from
		    		 * the current time which means you end up with phantom records
		    		 * in the top of the log.  So we need to discard invalid times!
		    		 */
		    		if(log.isValid())
		    			records.put(log.getChannel(), log);
		    		
		    	}
		    		
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("CallLogPanel.getLogSQLError")); 
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
	 * Gets the name and location for a person given their record ID
	 * @param readConnection Read connection to DB to use for query
	 * @param id ID of the person you are looking up
	 * @return Array with 2 elements, these will be CallLogPanel.nameNotFound 
	 * and CallLogPanel.locationNotFound if the record doesn't exist
	 */
	private String[] getPerson(Connection readConnection, int id){
		
		String[] person = new String[2];
		person[0] = xStrings.getString("CallLogPanel.nameNotFound"); 
		person[1] = xStrings.getString("CallLogPanel.locationNotFound"); 
		
		//Get the records from callhistory
		String SQL = "SELECT `name`, `location` FROM `person` " + 
				"WHERE `person_id` = " + id; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("CallLogPanel.getPerson") + id); 
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	person[0] = resultSet.getString("name"); 
		    	person[1] = resultSet.getString("location"); 
		    		
		    }
		    
		}catch (SQLException e){
			
			showError(e, xStrings.getString(
					"CallLogPanel.getPersonSQLError") + id); 
			
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
		
		return person;
		
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
	 * Gets the table encapsulated in this class
	 * @return
	 */
	public JTable getTable(){
		return history;
	}
	
	private void buildTableColumns(){
		
		//TODO read this from db instead of hard coding
		columnNames = new String[4];
		
		columnNames[0] = xStrings.getString("CallLogPanel.nameField"); 
		columnNames[1] = xStrings.getString("CallLogPanel.conversationField"); 
		columnNames[2] = xStrings.getString("CallLogPanel.locationField"); 
		columnNames[3] = xStrings.getString("CallerHistoryPanel.timeField"); 
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("CallLogPanel.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("CallLogPanel.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
		LOGGER.severe(friendlyErrorMessage);
		
	}

	@Override
	public void processMessage(Message message) {
		
		//React to commands thread all of this if performance is a problem
		LOGGER.info(xStrings.getString("CallLogPanel.receivedMessage") + 
					message.getBody());
			
		String[] command = message.getBody().split("/"); 
		
		//UPDATEFIELD
		if(command.length == 4 && command[0].equals(
				xStrings.getString("CallLogPanel.commandUpdateField"))){ 
			
			/* Only care about name, conversation, location
			 * TODO Change this to read fields wanted from DB
			 */
			//Make sure channel exists in list before updating
			if(!records.containsKey(command[2])){
				
				//Create a new one based on this
				/* TODO BUG fix dial channel swap
				 * When we dial, the channel in command[2] will only show you
				 * the person who called.  We need to find out who we're calling
				 * and use that for the channel vs person lookup					
				 */
				//Add to call log table
				LOGGER.info(xStrings.getString("CallLogPanel.logNewUpdate") + " " + command[2]);  
				
				CallLog log = new CallLog(language, country,
		    			command[2],
		    			readConnection, true);
		    	//TODO DEBUG change back to info
				LOGGER.severe(
						xStrings.getString("CallLogPanel.addingChannelToLog") + 
						log.getChannel()); 
				
		    	records.put(log.getChannel(), log);
		    	addCallLog(log);
				
			}
			
			if(command[1].equals(xStrings.getString("CallLogPanel.name"))){ 
				
				LOGGER.info(xStrings.getString("CallLogPanel.logNameUpdate")); 
				
				//Set Internal Record
				records.get(command[2]).setName(command[3]);
				
				//Set table row value
				changeCallLog(command[2], "name", command[3]); 
				
			}else if(command[1].equals(xStrings.getString("CallLogPanel.conversation"))){ 
				
				LOGGER.info(xStrings.getString("CallLogPanel.logConversationUpdate")); 
				
				//Set Internal Record
				records.get(command[2]).setConversation(command[3]);
				
				//Set table row value
				changeCallLog(command[2], "conversation", command[3]); 
				
			}else if(command[1].equals(xStrings.getString("CallLogPanel.location"))){ 
				
				LOGGER.info(xStrings.getString("CallLogPanel.logLocationUpdate")); 
				
				//Set Internal Record
				records.get(command[2]).setLocation(command[3]);
				
				//Set table row value
				changeCallLog(command[2], "location", command[3]); 
				
			}
		
		}else if(command.length == 4 && command[0].equals(
				xStrings.getString("CallLogPanel.commandQueue")) && 
				command[1].equals(incomingQueue)){//QUEUE INCOMING
			
			//Add to call log table
			LOGGER.info(xStrings.getString("CallLogPanel.logQUEUE")); 
			
			CallLog log = new CallLog(language, country,
	    			command[3],
	    			readConnection, true);
	    	//TODO DEBUG change back to info
			LOGGER.severe(
					xStrings.getString("CallLogPanel.addingChannelToLog") + 
					log.getChannel()); 
			
	    	records.put(log.getChannel(), log);
	    	addCallLog(log);
	    		
	    }else if(command.length == 4 && command[0].equals(
	    		xStrings.getString("CallLogPanel.commandEndPoint"))){ 
	    	//TODO DEBUG change back to info
	    	//Store as we'll expect a channel swap on this
	    	LOGGER.severe(xStrings.getString("CallLogPanel.logEndPoint") + " " +   
	    			"Number: " + command[3] + " Channel: " + command[1]);   
	    	channelSwapList.put(command[3], command[1]);//key = number, value = channel
	    	
	    }else if(command.length == 4 && command[0].equals(
	    		xStrings.getString("CallLogPanel.commandQueue")) && 
	    		command[1].equals(onairQueue)){ //QUEUE ON AIR
	    	
	    	//Check for channel swap
	    	if(channelSwapList.size() > 0){
	    		
	    		if(channelSwapList.containsKey(command[2])){
	    			
	    			LOGGER.info(xStrings.getString("CallLogPanel.receivedSwapOnAirQueue")); 
		    		swapChannel(channelSwapList.get(command[2]), command[3]);
	    			channelSwapList.remove(command[2]);
	    			
	    		}
	    		
	    	}
	    	
	    }else if(command.length == 3 && command[0].equals(
				xStrings.getString("CallLogPanel.changed"))){ 
			
			/* CHANGED
			 * Update log record for a given channel to another person only 
			 * need to change name and location as the rest of the call
			 * follows the channelID
	    	 */
			//CHANGED/channelID/personID
	    	
	    	String[] person = getPerson(readConnection, 
	    			Integer.parseInt(command[2]));
	    	
	    	changeCallLog(command[1], "name", person[0]); 
	    	changeCallLog(command[1], "location", person[1]); 
	    	
		}else if(command.length == 3 && command[0].equals(
				xStrings.getString("CallLogPanel.manual"))){
			
			//Add to call log table
			LOGGER.info(xStrings.getString("CallLogPanel.logManual")); 
			
			CallLog log = new CallLog(language, country,
	    			command[1],
	    			readConnection, true);
	    	
			LOGGER.info(
					xStrings.getString("CallLogPanel.addingChannelToLog") + 
					log.getChannel()); 
			
	    	records.put(log.getChannel(), log);
	    	addCallLog(log);
			
		}

	}

	/**
	 * Swaps the channel of an entry in the call log to this new channel
	 * @param fromChannel Original channel to swap
	 * @param toChannel new value to swap it to
	 */
	private void swapChannel(String fromChannel, String toChannel) {
		//TODO DEBUG change to info
		LOGGER.severe(xStrings.getString("CallLogPanel.logChannelUpdate") +  
				fromChannel + "/" + toChannel); 
		
		//Set Internal Record
		if(!records.containsKey(fromChannel)){
			
			//The log didn't have this call so lets add it usually only happens
			//if its a call that has been dialled
			CallLog log = new CallLog(language, country,
	    			toChannel,
	    			readConnection, true);
			//TODO DEBUG change to info TODO xStrings Swap Adding New Log
			LOGGER.severe("Swap Adding New Log: " + toChannel); 
			records.put(toChannel, log);
			
			addCallLog(log);
	    	
		}else{
			
			CallLog log = records.get(fromChannel);
			log.setChannel(toChannel);
			records.remove(fromChannel);
			records.put(toChannel, log);
			
			//Set table row value
			changeCallLog(fromChannel, "channel", toChannel); 
			
		}
		
	}

	/**
	 * Changes the given field in the call log
	 * @param channel Channel to change
	 * @param field field to change
	 * @param value value to set it to
	 */
	private void changeCallLog(String channel, String field, String value) {
		
		tableModel.changeRow(channel, field, value);
		
	}

	/* MESSAGE LISTENER -> IncomingChatMessageListener */
	@Override
	public void newIncomingMessage(EntityBareJid jid, Message message, Chat chat) {
		
		//Can pass this on to the processPacket method as part of normal message handling
		LOGGER.info(xStrings.getString("CallLogPanel.receivedPrivateMessage") 
				+ message.getBody()); 
		
		String[] command = message.getBody().split("/"); 
		
		if(command.length == 5 && command[0].equals(
				xStrings.getString("CallLogPanel.commandCall")) && 
				command[2].equals(incomingQueue)){ 
		
			//This will be a call still ringing so add it to our call history for later
			
			//Calls that aren't in the incoming queue will already be in the conversation
			//list (in theory but needs testing)
			//TODO DEBUG info & xStrings
			LOGGER.severe("Private Add New Log: " + command[3]); 
			CallLog log = new CallLog(language, country,
	    			command[3],
	    			readConnection, true);
	    	
	    	records.put(log.getChannel(), log);
	    	addCallLog(log);
			
		}	
		
	}

}
