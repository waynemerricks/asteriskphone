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
		
		xStrings = new I18NStrings(language, country);
		
		String SQL = "SELECT * FROM `person` WHERE `person_id` = " + id; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
			    //Alert level e.g. person banned or warning because they're awkward
	    		alert = resultSet.getString("alert"); //$NON-NLS-1$
	    		
	    		if(alert.equals("N")) //$NON-NLS-1$
	    			alert = xStrings.getString("PhoneCall.alertNormal"); //$NON-NLS-1$
	    		else if(alert.equals("W")) //$NON-NLS-1$
	    			alert = xStrings.getString("PhoneCall.alertWarning"); //$NON-NLS-1$
	    		else if(alert.equals("B")) //$NON-NLS-1$
	    			alert = xStrings.getString("PhoneCall.alertBanned"); //$NON-NLS-1$
	    		else if(alert.equals("F")) //$NON-NLS-1$
	    			alert = xStrings.getString("PhoneCall.alertFavourite"); //$NON-NLS-1$
	    		
	    		//Name
	    		name = resultSet.getString("name"); //$NON-NLS-1$
	    		
	    		if(name == null || name.equals("null")) //$NON-NLS-1$
	    			name = xStrings.getString("PhoneCall.unknownCaller"); //$NON-NLS-1$
	    		
	    		//Gender
	    		gender = resultSet.getString("gender"); //$NON-NLS-1$
	    		
	    		if(gender.equals("U")) //$NON-NLS-1$
	    			gender = xStrings.getString("PhoneCall.genderUnknown"); //$NON-NLS-1$
	    		else if(gender.equals("M")) //$NON-NLS-1$
	    			gender = xStrings.getString("PhoneCall.genderMale"); //$NON-NLS-1$
	    		else if(gender.equals("F")) //$NON-NLS-1$
	    			gender = xStrings.getString("PhoneCall.genderFemale"); //$NON-NLS-1$
	    		
	    		//Location
	    		location = resultSet.getString("location"); //$NON-NLS-1$
	    		
	    		if(location == null || location.equals("null"))//$NON-NLS-1$
	    			location = xStrings.getString("PhoneCall.locationUnknown"); //$NON-NLS-1$
	    		
	    		//Postal Address
	    		postalAddress = resultSet.getString("address"); //$NON-NLS-1$
	    		
	    		if(postalAddress == null || postalAddress.equals("null")) //$NON-NLS-1$
	    			postalAddress = ""; //$NON-NLS-1$
	    		
	    		//Post Code
	    		postCode = resultSet.getString("postcode"); //$NON-NLS-1$
	    		
	    		if(postCode == null || postCode.equals("null")) //$NON-NLS-1$
	    			postCode = ""; //$NON-NLS-1$
	    		
	    		//Email Address
	    		email = resultSet.getString("email"); //$NON-NLS-1$
	    		
	    		if(email == null || email.equals("null")) //$NON-NLS-1$
	    			email = ""; //$NON-NLS-1$
	    		
	    		//Language
	    		language = resultSet.getString("language"); //$NON-NLS-1$
	    		
	    		if(language == null || language.equals("null")) //$NON-NLS-1$
	    			language = ""; //$NON-NLS-1$
	    		
	    		//Religion
	    		religion = resultSet.getString("religion"); //$NON-NLS-1$
	    		
	    		if(religion == null || religion.equals("null")) //$NON-NLS-1$
	    			religion = ""; //$NON-NLS-1$
	    		
	    		//Journey
	    		journey = resultSet.getString("journey"); //$NON-NLS-1$
	    		
	    		if(journey == null || journey.equals("null")) //$NON-NLS-1$
	    			journey = ""; //$NON-NLS-1$
	    		
	    		//Notes
	    		notes = resultSet.getString("notes"); //$NON-NLS-1$
	    		
	    		if(notes == null || notes.equals("null")) //$NON-NLS-1$
	    			notes = ""; //$NON-NLS-1$
	    		
	    		//Get the conversation history for this person
	    		populateConversationHistory(readConnection);
	    		
		    }
		   
		}catch (SQLException e){
			showError(e, xStrings.getString("Person.databaseSQLError")); //$NON-NLS-1$
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
		
		String SQL = "SELECT time, channel, conversation FROM conversations " + //$NON-NLS-1$
				"WHERE person_id = " + id;  //$NON-NLS-1$
		
		try{
			
			statement = readConnection.createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				Date time = resultSet.getTimestamp("time"); //$NON-NLS-1$
				addConversation(new Conversation(time, 
							resultSet.getString("conversation"))); //$NON-NLS-1$
				
			}
			
		}catch(SQLException e){
			
			showError(e, xStrings.getString("PhoneCall.databaseSQLError")); //$NON-NLS-1$
			
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
		
		if(alert.equals(xStrings.getString("PhoneCall.alertWarning"))) //$NON-NLS-1$
			shortAlert = 'W';
		else if(alert.equals(xStrings.getString("PhoneCall.alertBanned"))) //$NON-NLS-1$
			shortAlert = 'B';
		else if(alert.equals(xStrings.getString("PhoneCall.alertFavourite"))) //$NON-NLS-1$
			shortAlert = 'F';
		
		return shortAlert;
		
	}
	
	/**
	 * Returns char representation of gender
	 * @return U = Unknown, M = Male, F = Female
	 */
	private char getShortGender(){
	
		char shortGender = 'U';
		if(gender.equals(xStrings.getString("PhoneCall.genderMale"))) //$NON-NLS-1$
			shortGender = 'M';
		else if(gender.equals(xStrings.getString("PhoneCall.genderFemale"))) //$NON-NLS-1$
			shortGender = 'F';
			
		return shortGender;
		
	}
	
	private boolean saveConversationToDB(Connection dbConnection){
	
		boolean saved = false;
		
		PreparedStatement statement = null;
		
		String SQL = "INSERT INTO conversations(person_id, conversation) VALUES (?, ?)"; //$NON-NLS-1$
		
		try{
			
			statement = dbConnection.prepareStatement(SQL);
			statement.setInt(1, id);
			statement.setString(2, currentConversation);
			
			statement.executeUpdate(SQL);
	        saved = true;
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("Person.DBSQLError") + id); //$NON-NLS-1$ 
        	
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
		
		String SQL = "UPDATE person SET alert = ?, name = ?, gender = ?, location = ?, " + //$NON-NLS-1$
				"address = ?, postcode = ?, email = ?, language = ?, " + //$NON-NLS-1$
				"religion = ?, notes = ? WHERE person_id = ?";  //$NON-NLS-1$
		
		try{
			
			//Save Person
			statement = dbConnection.prepareStatement(SQL);
			statement.setString(1, "" + getShortAlertLevel()); //$NON-NLS-1$
			statement.setString(2, name);
			statement.setString(3, "" + getShortGender()); //$NON-NLS-1$
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
        	
        	showError(e, xStrings.getString("Person.DBSQLError") + id); //$NON-NLS-1$ 
        	
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
		
		String SQL = "INSERT INTO person VALUES()";  //$NON-NLS-1$
		
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
        	
        	showError(e, xStrings.getString("Person.errorCreatingNewPerson")); //$NON-NLS-1$
        	
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
		
		System.err.println(xStrings.getString("Person.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("Person.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
}
