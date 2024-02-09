package com.thevoiceasia.phonebox.records;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class Person {

	/** CLASS VARS **/
	public int id;
	public String alert, gender, name, location, postalAddress, number,
		postCode, email, language, religion, currentConversation, journey, notes;
	private Vector<Conversation> conversationHistory = new Vector<Conversation>();
	private I18NStrings xStrings; //Link to external string resources
	private boolean newPerson = false;
	
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(PhoneCall.class.getName());//Logger
	
	public Person(String language, String country){
		xStrings = new I18NStrings(language, country);
	}
	
	public Person(int id, String language, String country){
		
		this.id = id;
		xStrings = new I18NStrings(language, country);
		
	}
	
	/**
	 * Initialises this object by looking up the given id in the database
	 * @param id id to lookup
	 * @param language language for I18N
	 * @param country country for I18N
	 * @param readConnection Read Connection to person table
	 */
	public Person(int id, String language, String country, 
			Connection readConnection){
		
		this.id = id;
		xStrings = new I18NStrings(language, country);
		
		String SQL = "SELECT * FROM `person` WHERE `person_id` = " + id; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
			    //Alert level e.g. person banned or warning because they're awkward
	    		alert = resultSet.getString("alert"); 
	    		
	    		if(alert.equals("N")) 
	    			alert = xStrings.getString("PhoneCall.alertNormal"); 
	    		else if(alert.equals("W")) 
	    			alert = xStrings.getString("PhoneCall.alertWarning"); 
	    		else if(alert.equals("B")) 
	    			alert = xStrings.getString("PhoneCall.alertBanned"); 
	    		else if(alert.equals("F")) 
	    			alert = xStrings.getString("PhoneCall.alertFavourite"); 
	    		
	    		//Name
	    		name = resultSet.getString("name"); 
	    		
	    		if(name == null || name.equals("null")) 
	    			name = xStrings.getString("PhoneCall.unknownCaller"); 
	    		
	    		//Gender
	    		gender = resultSet.getString("gender"); 
	    		
	    		if(gender.equals("U")) 
	    			gender = xStrings.getString("PhoneCall.genderUnknown"); 
	    		else if(gender.equals("M")) 
	    			gender = xStrings.getString("PhoneCall.genderMale"); 
	    		else if(gender.equals("F")) 
	    			gender = xStrings.getString("PhoneCall.genderFemale"); 
	    		
	    		//Location
	    		location = resultSet.getString("location"); 
	    		
	    		if(location == null || location.equals("null"))
	    			location = xStrings.getString("PhoneCall.locationUnknown"); 
	    		
	    		//Postal Address
	    		postalAddress = resultSet.getString("address"); 
	    		
	    		if(postalAddress == null || postalAddress.equals("null")) 
	    			postalAddress = ""; 
	    		
	    		//Post Code
	    		postCode = resultSet.getString("postcode"); 
	    		
	    		if(postCode == null || postCode.equals("null")) 
	    			postCode = ""; 
	    		
	    		//Email Address
	    		email = resultSet.getString("email"); 
	    		
	    		if(email == null || email.equals("null")) 
	    			email = ""; 
	    		
	    		//Language
	    		language = resultSet.getString("language"); 
	    		
	    		if(language == null || language.equals("null")) 
	    			language = ""; 
	    		
	    		//Religion
	    		religion = resultSet.getString("religion"); 
	    		
	    		if(religion == null || religion.equals("null")) 
	    			religion = ""; 
	    		
	    		//Journey
	    		journey = resultSet.getString("journey"); 
	    		
	    		if(journey == null || journey.equals("null")) 
	    			journey = ""; 
	    		
	    		//Notes
	    		notes = resultSet.getString("notes"); 
	    		
	    		if(notes == null || notes.equals("null")) 
	    			notes = ""; 
	    		
	    		//Get the conversation history for this person
	    		populateConversationHistory(readConnection);
	    		
		    }
		   
		}catch (SQLException e){
			showError(e, xStrings.getString("Person.databaseSQLError")); 
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
	 * Reads any conversation history this person has from the DB
	 */
	private void populateConversationHistory(Connection readConnection) {
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		String SQL = "SELECT time, channel, conversation FROM conversations " + 
				"WHERE person_id = " + id;  
		
		try{
			
			statement = readConnection.createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				Date time = resultSet.getTimestamp("time"); 
				addConversation(new Conversation(time, 
							resultSet.getString("conversation"))); 
				
			}
			
		}catch(SQLException e){
			
			showError(e, xStrings.getString("PhoneCall.databaseSQLError")); 
			
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
	 * Returns char representation of alert level 
	 * @return N = Normal, W = Warning, B = Banned
	 */
	public char getShortAlertLevel(){
		
		char shortAlert = 'N';
		
		if(alert.equals(xStrings.getString("PhoneCall.alertWarning"))) 
			shortAlert = 'W';
		else if(alert.equals(xStrings.getString("PhoneCall.alertBanned"))) 
			shortAlert = 'B';
		else if(alert.equals(xStrings.getString("PhoneCall.alertFavourite"))) 
			shortAlert = 'F';
		
		return shortAlert;
		
	}
	
	/**
	 * Returns char representation of gender
	 * @return U = Unknown, M = Male, F = Female
	 */
	private char getShortGender(){
	
		char shortGender = 'U';
		if(gender.equals(xStrings.getString("PhoneCall.genderMale"))) 
			shortGender = 'M';
		else if(gender.equals(xStrings.getString("PhoneCall.genderFemale"))) 
			shortGender = 'F';
			
		return shortGender;
		
	}
	
	private boolean saveConversationToDB(Connection dbConnection){
	
		boolean saved = false;
		
		PreparedStatement statement = null;
		
		String SQL = "INSERT INTO conversations(person_id, conversation) VALUES (?, ?)"; 
		
		try{
			
			statement = dbConnection.prepareStatement(SQL);
			statement.setInt(1, id);
			statement.setString(2, currentConversation);
			
			statement.executeUpdate(SQL);
	        saved = true;
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("Person.DBSQLError") + id);  
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
		return saved;
		
	}
	
	
	/**
	 * Saves this persons record to the DB
	 * @param dbConnection connection to use
	 * @return true if successful
	 */
	public boolean saveToDB(Connection dbConnection){
		
		boolean saved = false;
	
		PreparedStatement statement = null;
		
		String SQL = "UPDATE person SET alert = ?, name = ?, gender = ?, location = ?, " + 
				"address = ?, postcode = ?, email = ?, language = ?, " + 
				"religion = ?, notes = ? WHERE person_id = ?";  
		
		try{
			
			//Save Person
			statement = dbConnection.prepareStatement(SQL);
			statement.setString(1, "" + getShortAlertLevel()); 
			statement.setString(2, name);
			statement.setString(3, "" + getShortGender()); 
			statement.setString(4, location);
			statement.setString(5, postalAddress);
			statement.setString(6, postCode);
			statement.setString(7, email);
			statement.setString(8, language);
			statement.setString(9, religion);
			statement.setString(10, notes);
			statement.setInt(11, id);
			
			statement.executeUpdate();
	        
			//Save Conversation
			saved = saveConversationToDB(dbConnection);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("Person.DBSQLError") + id);  
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
		return saved;
		
	}
	
	/**
	 * Adds the given Conversation record to the history of conversations
	 * @param conversation
	 */
	public void addConversation(Conversation conversation){
		
		conversationHistory.add(conversation);
		
	}
	
	public Vector<Conversation> getConversationHistory(){
		
		return conversationHistory;
		
	}
	
	public String getCurrentConversation(){
		
		return currentConversation;
		
	}
	
	/**
	 * Appends given string to the local currentConversation variable
	 * @param conversation
	 */
	public void addCurrentConversation(String conversation){
		
		currentConversation = conversation;
		
	}
	
	
	/**
	 * Creates an empty Person record in the DB and returns the auto_increment id for this
	 * record
	 * @param databaseConnection preset DB write connection to use
	 * @return ID of record inserted
	 */
	public int createNewDBEntry(Connection databaseConnection){
		
		int id = -1;
		
		Statement statement = null;
		ResultSet rs = null;
		
		String SQL = "INSERT INTO person VALUES()";  
		
		try{
			
			statement = databaseConnection.createStatement();
			int rows = statement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
			
			if(rows > 0){
				rs = statement.getGeneratedKeys();
				rs.next();
				id = rs.getInt(1);
				this.id = id;//Set ID for this object now that we have an ID
			}
			
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("Person.errorCreatingNewPerson")); 
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
		return id;
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Person.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("Person.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	/**
	 * Sets the flag that indicates this is a new, non-existing person record
	 * @param isNew true if new
	 */
	public void setNewPerson(boolean isNew){
		
		newPerson = isNew;
		
	}
	
	/**
	 * Returns the is new record flag (one thats not in the db already)
	 * @return true if not in the db
	 */
	public boolean isNewPerson(){
		
		return newPerson;
		
	}
	
}
