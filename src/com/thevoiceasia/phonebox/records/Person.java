package com.thevoiceasia.phonebox.records;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.thevoiceasia.phonebox.chat.I18NStrings;

public class Person {

	/** CLASS VARS **/
	public int id;
	public String alert, gender, name, location, postalAddress, 
		postCode, email, language, religion, currentConversation;
	private Vector<Conversation> conversationHistory = new Vector<Conversation>();
	private I18NStrings xStrings; //Link to external string resources
	
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(PhoneCall.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.WARNING;
	
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
	private char getShortAlertLevel(){
		
		char shortAlert = 'N';
		
		if(alert.equals(xStrings.getString("PhoneCall.alertWarning"))) //$NON-NLS-1$
			shortAlert = 'W';
		else if(alert.equals(xStrings.getString("PhoneCall.alertBanned"))) //$NON-NLS-1$
			shortAlert = 'B';
		
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
		
		String SQL = "UPDATE person SET alert_level = ?, name = ?, gender = ?, location = ?, " + //$NON-NLS-1$
				"postal_address = ?, post_code = ?, email_address = ?, language = ?, " + //$NON-NLS-1$
				"religion = ? WHERE person_id = ?";  //$NON-NLS-1$
		
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
			statement.setInt(10, id);
			
			statement.executeUpdate(SQL);
	        
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
	 * Set the Logger object
	 */
	public void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.info(xStrings.getString("Person.logSetupLogging")); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("phonecall.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("Person.loggerCreateError")); //$NON-NLS-1$
			
		}
		
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
		
		currentConversation += conversation + " "; //$NON-NLS-1$
		
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
		
		String SQL = "INSERT INTO person VALUES()";  //$NON-NLS-1$
		
		try{
			
			statement = databaseConnection.createStatement();
			id = statement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
	        this.id = id; //Set ID for this object now that we have an ID
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
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Person.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("Person.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
}
