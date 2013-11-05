package com.thevoiceasia.phonebox.records;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

public class CallLog {

	/* CLASS VARS */
	private I18NStrings xStrings;
	private String name, location, conversation, id, time, channel;
	
	//STATICS
	private static final Logger LOGGER = Logger.getLogger(CallLog.class.getName());//Logger
		
	public CallLog(String language, String country, String channel, 
			Connection readConnection) {
		
		this.channel = channel;
		
		xStrings = new I18NStrings(language, country);
		
		//Get the conversation part of the record
		String SQL = "SELECT person_id, time, conversation FROM conversations WHERE channel = "  //$NON-NLS-1$
				+ channel;

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$
		    
		    while(resultSet.next()){
		    	
		    	this.id = resultSet.getString("person_id"); //$NON-NLS-1$
		    	this.conversation = resultSet.getString("conversation"); //$NON-NLS-1$
		    	this.time = sdf.format(new Date(
		    			resultSet.getTimestamp("time").getTime())); //$NON-NLS-1$
		    	
		    	lookupPerson(readConnection);
		    	
		    }
		    
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("CallLog.conversationSQLError") + SQL); //$NON-NLS-1$
			
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
	 * 
	 * @param readConnection
	 */
	private void lookupPerson(Connection readConnection) {
		
		//Get the persons details associated with this record
		String SQL = "SELECT name,location FROM person WHERE person_id = " + id;  //$NON-NLS-1$

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	this.name = resultSet.getString("name"); //$NON-NLS-1$
		    	this.location = resultSet.getString("location"); //$NON-NLS-1$
		    	
		    }
		    
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("CallLog.LookupPersonSQLError") + SQL); //$NON-NLS-1$
			
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
	 * Returns true if this record has a person and conversation
	 * @return
	 */
	public boolean isComplete(){
		
		boolean complete = false;
		
		if(name != null && name.length() > 0 
				&& conversation != null && conversation.length() > 0)
			complete = true;
		
		return complete;
		
	}
	
	/**
	 * Returns this record as a Vector<String> suitable for a JTable
	 * @return
	 */
	public Vector<String> getTableFormattedData(){
		
		Vector<String> record = new Vector<String>();
		record.add(getName());
		record.add(getConversation());
		record.add(getLocation());
		record.add(getTime());
		
		return record;
		
	}
	
	/**
	 * Returns the conversation or default value if null
	 * @return
	 */
	public String getConversation(){
		
		String tmpConv = conversation;
		
		if(tmpConv == null)
			tmpConv = xStrings.getString("CallLog.NoConversation"); //$NON-NLS-1$
		
		return tmpConv;
		
	}
	
	/**
	 * Returns the location or default value if null
	 * @return
	 */
	public String getLocation(){
		
		String tmpLoc = location;
		
		if(tmpLoc == null)
			tmpLoc = xStrings.getString("CallLog.UnknownLocation"); //$NON-NLS-1$
		
		return tmpLoc;
		
	}
	
	/**
	 * Returns the name or default value if null
	 * @return
	 */
	public String getName(){
		
		String tmpName = name;
		
		if(tmpName == null)
			tmpName = xStrings.getString("CallLog.UnknownName"); //$NON-NLS-1$
		
		return tmpName;
		
	}
	
	/**
	 * Returns the time on the record or current time if its null
	 * @return
	 */
	public String getTime(){
		
		String tmpTime = time;
		
		if(tmpTime == null){
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); //$NON-NLS-1$
			tmpTime = sdf.format(new Date());
					
		}
		
		return tmpTime;
		
	}
	
	/**
	 * Returns the channel associated with this log
	 * @return
	 */
	public String getChannel(){
		
		return channel;
		
	}
	
}
