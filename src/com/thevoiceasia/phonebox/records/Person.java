package com.thevoiceasia.phonebox.records;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.thevoiceasia.phonebox.chat.I18NStrings;
//TODO SAVE ME TO DB
public class Person {

	/** CLASS VARS **/
	public int id;
	public String alert, gender, name, location, postalAddress, 
		postCode, email, language, religion, currentConversation;
	
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
	
	public boolean addConversation(String conversation){
		
		//TODO
		/*
		 * Store locally, separate private method to commit to DB on a timer
		 */
		
		return true;
		
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
