package com.thevoiceasia.phonebox.records;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

public class CallLog {

	private String name, location, conversation, id;
	
	public CallLog(String channel, Connection readConnection) {
		
		//SELECT person_id, conversation FROM conversations WHERE channel = 1383575074.680
		String SQL = "SELECT person_id, conversation FROM conversations WHERE channel = "  //$NON-NLS-1$
				+ channel;

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	//TODO
		    	this.id = resultSet.getString("person_id"); //$NON-NLS-1$
		    	this.conversation = resultSet.getString("conversation"); //$NON-NLS-1$
		    	
		    	lookupPerson(readConnection);
		    	
		    }
		    
		}catch (SQLException e){
			
			//TODO
			LOGGER.severe(xStrings.getString("CallLog.conversationSQLError")); //$NON-NLS-1$
			
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
		// TODO Auto-generated method stub
		//SELECT name, location FROM person WHERE person_id = 12
		String SQL = "SELECT name,location FROM person WHERE person_id = " + id;  //$NON-NLS-1$

		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	//TODO
		    	this.name = resultSet.getString("name"); //$NON-NLS-1$
		    	this.location = resultSet.getString("location"); //$NON-NLS-1$
		    	
		    }
		    
		}catch (SQLException e){
			
			//TODO
			LOGGER.severe(xStrings.getString("CallLog.LookupPersonSQLError")); //$NON-NLS-1$
			
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
	 * @return
	 */
	public boolean isComplete(){
		//TODO complete = has person and conversation
		return true;
		
	}
	
	/**
	 * Returns this record as a Vector<String> suitable for a JTable
	 * @return
	 */
	public Vector<String> getTableFormattedData(){
		//TODO time, name, conversation, location? WHERE callchannel has a conversation at least
		return null;
	}
	
}
