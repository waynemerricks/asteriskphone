package com.thevoiceasia.phonebox.records;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

public class Person {

	/** CLASS VARS **/
	public int id;
	public String alert, gender, name, location, postalAddress, 
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
	 * @return true if succcessful
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
