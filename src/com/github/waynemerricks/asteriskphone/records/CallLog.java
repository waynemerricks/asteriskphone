package com.github.waynemerricks.asteriskphone.records;

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
	
	/**
	 * Creates a CallLog for the given channel by looking up info in the callhistory
	 * table
	 * @param language I18N Language e.g. en
	 * @param country I18N country e.g. GB
	 * @param channel Channel to lookup
	 * @param readConnection Connection to use for database reads
	 * @param history set to whatever, only necessary to distinguish from other constructor
	 */
	public CallLog(String language, String country, String channel, 
			Connection readConnection, boolean history) {
		
		this.channel = channel;
		
		xStrings = new I18NStrings(language, country);
		
		//Get the skeleton of the record, number -> person
		String SQL = "SELECT callhistory.time, person.name, person.location FROM " 
				+ "callhistory INNER JOIN phonenumbers ON callhistory.phonenumber = "  
				+ "phonenumbers.phone_number INNER JOIN person ON phonenumbers.person_id = " 
				+ "person.person_id WHERE callhistory.callchannel = " + channel  
				+ " AND callhistory.state = 'R'"; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); 
		    
		    while(resultSet.next()){
		    	
		    	this.time = sdf.format(new Date(
		    			resultSet.getTimestamp("time").getTime())); 
		    	this.location = resultSet.getString("location"); 
		    	this.name = resultSet.getString("name"); 
		    	this.conversation = null;
		    	
		    }
		    
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("CallLog.callhistorySQLError") + SQL); 
			
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
	 * Creates a CallLog entry based on the given channel in the conversations table
	 * @param language for I18N strings e.g. en
	 * @param country for I18N strings e.g. GB
	 * @param channel Channel to lookup in conversation table
	 * @param readConnection A MySQL Read connection
	 */
	public CallLog(String language, String country, String channel, 
			Connection readConnection) {
		
		this.channel = channel;
		
		xStrings = new I18NStrings(language, country);
		
		//Get the conversation part of the record
		String SQL = "SELECT person_id, time, conversation FROM conversations WHERE channel = "  
				+ channel;

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); 
		    
		    while(resultSet.next()){
		    	
		    	this.id = resultSet.getString("person_id"); 
		    	this.conversation = resultSet.getString("conversation"); 
		    	this.time = sdf.format(new Date(
		    			resultSet.getTimestamp("time").getTime())); 
		    	
		    	lookupPerson(readConnection);
		    	
		    }
		    
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("CallLog.conversationSQLError") + SQL); 
			
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
		String SQL = "SELECT name, location FROM person WHERE person_id = " + id;  

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	this.name = resultSet.getString("name"); 
		    	this.location = resultSet.getString("location"); 
		    	
		    }
		    
		}catch (SQLException e){
			
			LOGGER.severe(xStrings.getString("CallLog.LookupPersonSQLError") + SQL); 
			
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
			tmpConv = xStrings.getString("CallLog.NoConversation"); 
		
		return tmpConv;
		
	}
	
	public void setConversation(String conversation) {
		
		if(conversation != null)
			this.conversation = conversation;
		
	}
	
	/**
	 * Returns the location or default value if null
	 * @return
	 */
	public String getLocation(){
		
		String tmpLoc = location;
		
		if(tmpLoc == null)
			tmpLoc = xStrings.getString("CallLog.UnknownLocation"); 
		
		return tmpLoc;
		
	}
	
	public void setLocation(String location) {
		
		if(location != null)
			this.location = location;
		
	}
	
	public void setName(String name){
		
		if(name != null)
			this.name = name;
			
	}
	
	/**
	 * Returns the name or default value if null
	 * @return
	 */
	public String getName(){
		
		String tmpName = name;
		
		if(tmpName == null)
			tmpName = xStrings.getString("CallLog.UnknownName"); 
		
		return tmpName;
		
	}
	
	/**
	 * Returns the time on the record or current time if its null
	 * @return
	 */
	public String getTime(){
		
		String tmpTime = time;
		
		if(tmpTime == null){
			
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss"); 
			tmpTime = sdf.format(new Date());
			time = tmpTime;
					
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

	public boolean isValid() {
		
		boolean valid = false;
		
		if(time != null)
			valid = true;
		
		return valid;
		
	}

	/**
	 * Need to be able to set the channel due to channel Swaps
	 * @param toChannel
	 */
	public void setChannel(String toChannel) {
		
		channel = toChannel;
		
	}
	
}
