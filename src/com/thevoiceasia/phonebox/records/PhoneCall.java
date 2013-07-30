package com.thevoiceasia.phonebox.records;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.asteriskjava.live.AsteriskChannel;
import org.asteriskjava.live.CallerId;

import com.thevoiceasia.phonebox.chat.I18NStrings;
import com.thevoiceasia.phonebox.database.DatabaseManager;

public class PhoneCall {

	/** CLASS VARS **/
	private AsteriskChannel channel;
	private Vector<Person> people = new Vector<Person>();
	private int activePerson = 0;
	
	private DatabaseManager database;
	private I18NStrings xStrings; //Link to external string resources
	private Vector<Integer> numberIDs = new Vector<Integer>(); 
	
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
	 * Saves this object to the DB.  All we're doing is saving the Person 
	 * details attached to the call as there is no state to save for
	 * PhoneCall itself that isn't tracked elsewhere.
	 * 
	 * @return true if save succeeded
	 */
	public boolean saveToDB(){
		
		return getActivePerson().saveToDB(database.getWriteConnection());
		
	}
	
	
	/**
	 * Returns the active person this object is related to
	 * This is because there could be 5 people with the same phone number
	 * if there is a local call box or even a shared phone in the family
	 * @return The Active Person Object (the first object is active by default)
	 */
	public Person getActivePerson(){
		
		return people.get(activePerson);
		
	}
	
	/**
	 * Sets the active person associated with this call from the list of people
	 * pulled out of the DB with this same callerid number.
	 * @param personID personID to set active
	 * @return true if we found a match/false if this id didn't exist in the list
	 */
	public boolean setActivePerson(int personID){
		
		boolean found = false;
		int i = 0;
		
		while(i < people.size() && !found){
			
			if(people.get(i).id == personID){
				
				found = true;
				activePerson = i;
				
			}
			
			i++;
			
		}
		
		return found;
		
	}
	
	/**
	 * Adds given Conversation to the activePersons history
	 * @param conversation
	 */
	public void addConversationHistory(Conversation conversation){
		
		getActivePerson().addConversation(conversation);
		
	}
	
	/**
	 * Adds given String to the active persons current conversation
	 * @param conversation
	 */
	public void addConversation(String conversation){
		
		getActivePerson().addCurrentConversation(conversation);
		
	}
	
	/**
	 * Adds a record to the DB to indicate that this PhoneCall is in the ringing state
	 */
	public void trackRinging(){
		
		Statement statement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state) VALUES("  //$NON-NLS-1$
				+ channel.getCallerId().getNumber() + ", 'R')"; //$NON-NLS-1$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingRingState") //$NON-NLS-1$ 
        			+ channel.getCallerId().getNumber());
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
	}
	
	/**
	 * Adds a record to the DB to indicate that this PhoneCall is in the answered state
	 * @param answeredBy User Name of person who answered
	 */
	public void trackAnswered(String answeredBy){
		
		Statement statement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator) VALUES("  //$NON-NLS-1$
				+ channel.getCallerId().getNumber() + ", 'A', " + answeredBy + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingAnsweredState") //$NON-NLS-1$ 
        			+ channel.getCallerId().getNumber());
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
	}
	
	/**
	 * Adds a record to the DB to indicate that this PhoneCall is in the parked state
	 * @param parkedBy User Name of person who parked call
	 */
	public void trackParked(String parkedBy){
		
		Statement statement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator) VALUES("  //$NON-NLS-1$
				+ channel.getCallerId().getNumber() + ", 'P', " + parkedBy + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingParkedState") //$NON-NLS-1$ 
        			+ channel.getCallerId().getNumber());
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
	}
	
	/**
	 * Adds a record to the DB to indicate that this PhoneCall is in the hung up/ended state
	 * @param hangupBy User Name of person who hung up the call
	 */
	public void trackHangup(String hangupBy){
		
		Statement statement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator) VALUES("  //$NON-NLS-1$
				+ channel.getCallerId().getNumber() + ", 'H', " + hangupBy + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingHangupState") //$NON-NLS-1$ 
        			+ channel.getCallerId().getNumber());
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
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
	
	/**
	 * Gets the details for the person associated with this call
	 */
	private void populatePersonDetails(){
		
		Statement statement = null, personStatement = null;
		ResultSet resultSet = null, personResultSet = null;
		
		try{
			//TODO Think about one number for multiple people
			/*
			 * May need to create multiples if number already has a person ID that isn't this one
			 * Incorporate a TIMESTAMP to figure out latest called from person.
			 */
			String SQL = "SELECT numbers_id, person_id FROM phonenumbers WHERE phone_number='"  //$NON-NLS-1$
					+ channel.getCallerId() + "'"; //$NON-NLS-1$
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    boolean first = true;
		    
		    while(resultSet.next()){
		    	
		    	numberIDs.add(resultSet.getInt("numbers_id")); //$NON-NLS-1$
		    	
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
		    		
		    		Person person = new Person(personResultSet.getInt("person_id"), //$NON-NLS-1$
		    				database.getUserSettings().get("language"),  //$NON-NLS-1$
		    				database.getUserSettings().get("country")); //$NON-NLS-1$
		    		
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
		    		
		    		//Get the conversation history for this person
		    		getConversationHistory(person);
		    		
		    		//Add to Vector
		    		people.add(person);
		    		
		    	}
		    	
		    	if(people.size() < 1){
		    		
		    		/* No person exists so this is a new caller, need to create an entry in person
		    		 * and update entry in phonenumbers
		    		 */
		    		Person newPerson = new Person(database.getUserSettings().get("language"),  //$NON-NLS-1$
		    				database.getUserSettings().get("country")); //$NON-NLS-1$
			    	
			    	newPerson.createNewDBEntry(database.getWriteConnection());
		    		newPerson = populatePersonWithDefaults(newPerson);
		    		
		    		//Attach this person to the given number records
		    		for(int i = 0; i < numberIDs.size(); i++)
		    			attachNumberToPerson(numberIDs.get(i), newPerson.id);
		    		
		    		//Add this person to the people Vector
		    		people.add(newPerson);
		    		
		    	}
		    	
		    }else{
		    	
		    	//Need to create new Person and PhoneNumber record as neither exist
		    	Person newPerson = new Person(database.getUserSettings().get("language"),  //$NON-NLS-1$
	    				database.getUserSettings().get("country")); //$NON-NLS-1$
		    	
		    	newPerson.createNewDBEntry(database.getWriteConnection());
	    		newPerson = populatePersonWithDefaults(newPerson);
	    		
	    		createNumberRecord(channel.getCallerId(), newPerson.id);
	    		
	    		//Add this person to the people Vector
	    		people.add(newPerson);
	    		
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("PhoneCall.databaseSQLError")); //$NON-NLS-1$
		}finally {
			
			if (personResultSet != null) {
		        try {
		        	personResultSet.close();
		        } catch (SQLException sqlEx) { } // ignore
		        personResultSet = null;
		    }
		    
			if (personStatement != null) {
		        try {
		        	personStatement.close();
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
	 * Reads any conversation history this person has from the DB
	 * @param person
	 */
	private void getConversationHistory(Person person) {
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		String SQL = "SELECT time, conversation WHERE person_id = " + person.id; //$NON-NLS-1$
		
		try{
			
			statement = database.getReadConnection().createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				Date time = resultSet.getTimestamp("time"); //$NON-NLS-1$
				person.addConversation(new Conversation(time, 
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
	 * Creates a new record in the phonenumbers table with the given number
	 * and person id attached
	 * @param callerId uses callerId.getNumber() to return phone number of caller
	 * @param personID personID in the person table to attach to this record
	 * @return
	 */
	private int createNumberRecord(CallerId callerId, int personID) {
		
		int id = -1;
		
		Statement statement = null;
		
		String SQL = "INSERT INTO phonenumbers(phone_number, person_id) VALUES("  //$NON-NLS-1$
				+ callerId.getNumber() + ", " + personID + ")";  //$NON-NLS-1$ //$NON-NLS-2$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			id = statement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
	        numberIDs.add(id);
	        
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
	 * Puts the given personID into the number record for the given numberID 
	 * @param numberID numbers_id in phonenumbers table
	 * @param personID person_id in person table
	 */
	private void attachNumberToPerson(Integer numberID, int personID) {
		//TODO Think about one number for multiple people
		/*
		 * May need to create multiples if number already has a person ID that isn't this one
		 * Incorporate a TIMESTAMP to figure out latest called from person.
		 */
		String SQL = "UPDATE phonenumbers SET person_id = " + personID +  //$NON-NLS-1$
				" WHERE numbers_id = " + numberID; //$NON-NLS-1$
		
		Statement query = null;
		try{	
			
			query = database.getWriteConnection().createStatement();
			query.executeUpdate(SQL);
	        
	    }catch(SQLException e){
	    	
	    	showError(e, xStrings.getString("PhoneCall.errorAttachingNumber" + numberID + ":" + personID)); //$NON-NLS-1$ //$NON-NLS-2$
	    	
	    }finally{
	        if(query != null)
	        	try{
	        		query.close();
	        	}catch(Exception e){}
	    }
		
	}

	
	/**
	 * Sets all the internal Person attributes to their defaults
	 * Used when creating a new Person
	 * @param person 
	 * @return Person object with default values
	 */
	private Person populatePersonWithDefaults(Person person){
		
		person.alert = xStrings.getString("PhoneCall.alertNormal"); //$NON-NLS-1$
		person.name.equals(xStrings.getString("PhoneCall.unknownCaller")); //$NON-NLS-1$
		person.gender = xStrings.getString("PhoneCall.genderUnknown"); //$NON-NLS-1$
		person.location = xStrings.getString("PhoneCall.locationUnknown"); //$NON-NLS-1$
		person.postalAddress = ""; //$NON-NLS-1$
		person.postCode = ""; //$NON-NLS-1$
		person.email = ""; //$NON-NLS-1$
		person.language = ""; //$NON-NLS-1$
		person.religion = ""; //$NON-NLS-1$
		
		return person;
		
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