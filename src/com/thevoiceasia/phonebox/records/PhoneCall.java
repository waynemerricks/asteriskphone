package com.thevoiceasia.phonebox.records;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.asteriskjava.live.AsteriskChannel;

import com.thevoiceasia.phonebox.chat.I18NStrings;
import com.thevoiceasia.phonebox.database.DatabaseManager;

public class PhoneCall {

	/** CLASS VARS **/
	private AsteriskChannel channel;
	private Vector<Person> people = new Vector<Person>();
	private DatabaseManager database;
	private I18NStrings xStrings; //Link to external string resources
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(PhoneCall.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.WARNING;
	
	public PhoneCall(DatabaseManager database, AsteriskChannel channel){
		
		this.database = database;
		this.channel = channel;
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		setupLogging();
		populatePersonDetails();
		
	}
	
	/**
	 * Set the Logger object
	 */
	public void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		LOGGER.info(xStrings.getString("PhoneCall.logSetupLogging")); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("phonecall.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("PhoneCall.loggerCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	private void populatePersonDetails(){
		
		Statement statement = null, personStatement = null;
		ResultSet resultSet = null, personResultSet = null;
		
		try{
			
			String SQL = "SELECT person_id FROM phonenumbers WHERE phone_number='"  //$NON-NLS-1$
					+ channel.getCallerId() + "'"; //$NON-NLS-1$
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    boolean first = true;
		    
		    while(resultSet.next()){
		    	
		    	if(first){
		    		SQL = "" + resultSet.getInt("person_id"); //$NON-NLS-1$ //$NON-NLS-2$
		    		first = false;
		    	}else
		    		SQL += " OR person_id = " + resultSet.getInt("person_id"); //$NON-NLS-1$ //$NON-NLS-2$
		    	
		    }
		    
		    if(!first){
		    	
		    	//We got some results so lets look them up
		    	SQL = "SELECT * FROM person WHERE person_id = " + SQL; //$NON-NLS-1$
		    	personStatement = database.getConnection().createStatement();
		    	personResultSet = personStatement.executeQuery(SQL);
		    	
		    	while(personResultSet.next()){
		    		
		    		Person person = new Person(personResultSet.getInt("person_id")); //$NON-NLS-1$
		    		
		    		//Alert level e.g. person banned or warning because they're awkward
		    		person.alert = personResultSet.getString("alert_level"); //$NON-NLS-1$
		    		
		    		if(person.alert.equals("N")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertNormal"); //$NON-NLS-1$
		    		else if(person.alert.equals("W")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertWarning"); //$NON-NLS-1$
		    		else if(person.alert.equals("B")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertBanned"); //$NON-NLS-1$
		    		
		    		//Name
		    		person.name = personResultSet.getString("name"); //$NON-NLS-1$
		    		
		    		if(person.name.equals("null")) //$NON-NLS-1$
		    			person.name.equals(xStrings.getString("PhoneCall.unknownCaller")); //$NON-NLS-1$
		    		
		    		//Gender
		    		person.gender = personResultSet.getString("gender"); //$NON-NLS-1$
		    		
		    		if(person.gender.equals("U")) //$NON-NLS-1$
		    			person.gender = xStrings.getString("PhoneCall.genderUnknown"); //$NON-NLS-1$
		    		else if(person.gender.equals("M")) //$NON-NLS-1$
		    			person.gender = xStrings.getString("PhoneCall.genderMale"); //$NON-NLS-1$
		    		else if(person.gender.equals("F")) //$NON-NLS-1$
		    			person.gender = xStrings.getString("PhoneCall.genderFemale"); //$NON-NLS-1$
		    		
		    		//Location
		    		person.location = personResultSet.getString("location"); //$NON-NLS-1$
		    		
		    		if(person.location.equals("null")) //$NON-NLS-1$
		    			person.location = xStrings.getString("PhoneCall.locationUnknown"); //$NON-NLS-1$
		    		
		    		//Postal Address
		    		person.postalAddress = personResultSet.getString("postal_address"); //$NON-NLS-1$
		    		
		    		if(person.postalAddress.equals("null")) //$NON-NLS-1$
		    			person.postalAddress = ""; //$NON-NLS-1$
		    		
		    		//Post Code
		    		person.postCode = personResultSet.getString("post_code"); //$NON-NLS-1$
		    		
		    		if(person.postCode.equals("null")) //$NON-NLS-1$
		    			person.postCode = ""; //$NON-NLS-1$
		    		
		    		//Email Address
		    		person.email = personResultSet.getString("email_address"); //$NON-NLS-1$
		    		
		    		if(person.email.equals("null")) //$NON-NLS-1$
		    			person.email = ""; //$NON-NLS-1$
		    		
		    		//Language
		    		person.language = personResultSet.getString("language"); //$NON-NLS-1$
		    		
		    		if(person.language.equals("null")) //$NON-NLS-1$
		    			person.language = ""; //$NON-NLS-1$
		    		
		    		//Religion
		    		person.religion = personResultSet.getString("religion"); //$NON-NLS-1$
		    		
		    		if(person.religion.equals("null")) //$NON-NLS-1$
		    			person.religion = ""; //$NON-NLS-1$
		    		
		    		//Add to Vector
		    		people.add(person);
		    		
		    	}
		    	
		    	if(people.size() < 1){
		    		
		    		/* No person exists so this is a new caller, need to create an entry in person
		    		 * and update entry in phonenumbers
		    		 */
		    		//TODO
		    		
		    	}
		    	
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("PhoneCall.databaseSQLError")); //$NON-NLS-1$
		}finally {
			
			if (personResultSet != null) {
		        try {
		        	statement.close();
		        } catch (SQLException sqlEx) { } // ignore
		        personResultSet = null;
		    }
		    
			if (personStatement != null) {
		        try {
		        	resultSet.close();
		        } catch (SQLException sqlEx) { } // ignore
		        personStatement = null;
		    }
			
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
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("PhoneCall.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("PhoneCall.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("PhoneCall.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("PhoneCall.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
}
