package com.thevoiceasia.phonebox.records;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.asteriskjava.live.AsteriskQueueEntry;
import org.asteriskjava.live.CallerId;

import com.thevoiceasia.phonebox.asterisk.AsteriskManager;
import com.thevoiceasia.phonebox.database.DatabaseManager;

public class PhoneCall implements Runnable{

	/** CLASS VARS **/
	private AsteriskQueueEntry queueEntry;
	private Vector<Person> people = new Vector<Person>();
	private int activePerson = 0;
	
	private DatabaseManager database;
	private AsteriskManager asteriskManager;
	private I18NStrings xStrings; //Link to external string resources
	private Vector<Integer> numberIDs = new Vector<Integer>(); 
	private char threadMode;
	private String threadOperator, answeredBy, callerID, channelID, callLocation, calltype;
	private boolean headless = false;
	private int retryCount = -1;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(PhoneCall.class.getName());//Logger
	
	public PhoneCall(DatabaseManager database, String callerID, String channelID) {
		
		this.database = database;
		this.channelID = channelID;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		
		populatePersonDetails();
		
	}
	
	public PhoneCall(DatabaseManager database, String callerID, String channelID, 
			String callLocation){
		
		this.callLocation = callLocation;
		this.database = database;
		this.channelID = channelID;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		//TODO When a call is queued without a conversation it might have been from
		//a call we initiated, the conversation goes because of a channel switch
		//from originator to receiver once the call is put on hold :|
		populatePersonDetails();
		lookupCallType(channelID);
		
	}

	public PhoneCall(DatabaseManager database, String callerID, String channelID, 
			AsteriskManager asteriskManager, char mode, String from){
	
		this.database = database;
		this.channelID = channelID;
		this.asteriskManager = asteriskManager;
		headless = true;
		threadMode = mode;
		threadOperator = from;
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		
	}
	
	public PhoneCall(DatabaseManager database, AsteriskQueueEntry queueEntry, 
			AsteriskManager asteriskManager) {
		
		this.database = database;
		this.queueEntry = queueEntry;
		this.asteriskManager = asteriskManager;
		this.channelID = queueEntry.getChannel().getId();
		threadMode = 'Q';
		threadOperator = "NA"; //$NON-NLS-1$
		headless = true;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(queueEntry.getChannel().getCallerId());
		
	}
	
	public PhoneCall(String callerId, String channel, DatabaseManager database){
		
		/* Used by server to create skeleton for new calls (if necessary)
		 * hits this when the calls are in the Ringing state 
		 */
		headless = true;
		this.channelID = channel;
		this.database = database;
		this.threadMode = 'R';
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  //$NON-NLS-1$
				database.getUserSettings().get("country")); //$NON-NLS-1$
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerId);
	
	}
	
	/**
	 * Helper method to replace with held number with a suitable string
	 * @param id callerid to check
	 * @return caller id number or suitable string if withheld/null
	 */
	private String checkNumberWithHeld(CallerId id){
		
		String callerid = null;
		
		if(id != null && id.getNumber() != null && id.getNumber().trim().length() > 0 &&
				!id.getNumber().equalsIgnoreCase("null")) //$NON-NLS-1$
			callerid = id.getNumber();
		else
			callerid = xStrings.getString("PhoneCall.withHeldNumber"); //$NON-NLS-1$
		
		return callerid;
		
	}
	
	/**
	 * Helper method to replace with held number with a suitable string
	 * @param id callerid to check
	 * @return caller id number or suitable string if withheld/null
	 */
	private String checkNumberWithHeld(String id){
		
		String callerid = null;
		
		if(id != null && id.trim().length() > 0 &&
				!id.equalsIgnoreCase("null")) //$NON-NLS-1$
			callerid = id;
		else
			callerid = xStrings.getString("PhoneCall.withHeldNumber"); //$NON-NLS-1$
		
		return callerid;
		
	}
	/**
	 * Checks to see if this channel already has an associated type and updates accordingly
	 * @param channel channel to lookup
	 */
	private void lookupCallType(String channel) {
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			String SQL = "SELECT type FROM callhistory WHERE callchannel = " + channel +  //$NON-NLS-1$
					" AND type != 'NA' ORDER BY callhistory_id DESC LIMIT 1"; //$NON-NLS-1$
			
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next())
		    	calltype = resultSet.getString("type"); //$NON-NLS-1$
		    
		}catch (SQLException e){
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
	 * Returns the person who answered this call or null if not answered
	 * @return
	 */
	public String getAnsweredBy(){
		
		return answeredBy;
		
	}
	
	/**
	 * Returns the type of this call
	 * @return
	 */
	public String getCallType(){
		
		return calltype;
		
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel) VALUES("  //$NON-NLS-1$
				+ "'" + callerID + "', 'R', " //$NON-NLS-1$ //$NON-NLS-2$ 
				+ channelID + ")"; //$NON-NLS-1$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingRingState") //$NON-NLS-1$ 
        			+ callerID);
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
	}
	
	/**
	 * Used by Server to create a skeleton record if this is a first time caller
	 */
	public void createCallSkeleton(){
		
		// Lookup phone number if its already there get person id
		Statement statement = null, writeStatement = null;
		ResultSet resultSet = null, personResult = null, historyResult = null;
		
		String SQL = null;
		
		try{
			
			SQL = "SELECT person_id FROM phonenumbers WHERE phone_number = '" + callerID  //$NON-NLS-1$
					+ "' ORDER BY lastUpdate DESC LIMIT 1"; //$NON-NLS-1$
			
			statement = database.getReadConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    int personID = -1;
		    
		    while(resultSet.next())
		    	personID = resultSet.getInt("person_id"); //$NON-NLS-1$
		    
		    if(personID == -1){
		    	
		    	// If not there create blank new person record + phonenumber and get person id
				writeStatement = database.getWriteConnection().createStatement();
				SQL = "INSERT INTO person VALUES()"; //$NON-NLS-1$
				
				writeStatement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
				personResult = writeStatement.getGeneratedKeys();
				
				if(personResult.next())
					personID = personResult.getInt(1);
				
				SQL = "INSERT INTO phonenumbers (phone_number, person_id) VALUES('"  //$NON-NLS-1$
						+ callerID + "', '" + personID + "')"; //$NON-NLS-1$ //$NON-NLS-2$
		    	
				writeStatement.executeUpdate(SQL);
				
		    }
		    
		    if(personID != -1){ //Should never be -1 if got this far
		    	
			    // Use person id to add an R Log with channel to callhistory
			    SQL = "INSERT INTO callhistory(phonenumber, state, callchannel, activePerson) " //$NON-NLS-1$
			    		+ "VALUES ('" + callerID + "', 'R', '" + channelID + "', '" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$  
			    		+ personID + "')"; //$NON-NLS-1$ 
			    
			    if(writeStatement == null)
			    	writeStatement = database.getWriteConnection().createStatement();
			    
			    writeStatement.executeUpdate(SQL);

		    }
			
		}catch (SQLException e){
			showError(e, xStrings.getString("PhoneCall.databaseSQLError") + " " + SQL); //$NON-NLS-1$ //$NON-NLS-2$
		}finally {
			
			if(historyResult != null){
				try{
					historyResult.close();
				}catch(SQLException e){}
				historyResult = null;
			}
			
			if(personResult != null){
				try{
					personResult.close();
				}catch(SQLException e){}
				personResult = null;
			}
			
			if (resultSet != null) {
		        try {
		        	resultSet.close();
		        } catch (SQLException sqlEx) { } // ignore
		        resultSet = null;
		    }
			
			if(writeStatement != null){
				try{
					writeStatement.close();
				}catch(SQLException e){}
				writeStatement = null;
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
	 * Adds a record to the DB to indicate that this PhoneCall is in the ringing state
	 */
	public void trackQueue(String operator){
		
		Statement statement = null, updateStatement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel, operator) VALUES("  //$NON-NLS-1$
				+ "'" + callerID + "', 'Q', " //$NON-NLS-1$ //$NON-NLS-2$ 
				+ channelID + ", '" + operator + "')"; //$NON-NLS-1$ //$NON-NLS-2$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
			
			ResultSet rs = statement.getGeneratedKeys();
			int rowID = -1;
			
			if(rs.next())
				rowID = rs.getInt(1);
			
			// If operator is NA then add the active person to this entry
			if(operator.equals("NA")){ //$NON-NLS-1$
				
				SQL = "UPDATE callhistory SET activePerson = " + getPersonFromNumber(callerID) +  //$NON-NLS-1$
						" WHERE callhistory_id = " + rowID; //$NON-NLS-1$
				
				updateStatement = database.getWriteConnection().createStatement();
				updateStatement.executeUpdate(SQL);
				
			}
			
			retryCount = -1;
			
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingRingState") //$NON-NLS-1$ 
        			+ callerID);
        	retryCount++;
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
            
            if(updateStatement != null)
            	try{
            		updateStatement.close();
            	}catch(Exception e){}
        }
		
		if(retryCount != -1 && retryCount < 2){
			
			try {
				Thread.sleep(500L);
				trackQueue(operator);
				retryCount = -1;//Reset retry count as we successfully completed
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	/**
	 * Gets the most recently updated person_id associated with the number parameter
	 * @param number number to search for
	 * @return person id
	 */
	private String getPersonFromNumber(String number) {
		
		//Get the most recently updated person id associated with this number
		String id = null;
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			String SQL = "SELECT person_id FROM phonenumbers WHERE phone_number = '"  //$NON-NLS-1$
					+ number  + "' ORDER BY lastUpdate DESC LIMIT 1"; //$NON-NLS-1$
			
			LOGGER.info(xStrings.getString("PhoneCall.getPersonFromNumber") + SQL); //$NON-NLS-1$
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next())
		    	id = resultSet.getString("person_id"); //$NON-NLS-1$
		    
		}catch (SQLException e){
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
		
		return id;
		
	}

	/**
	 * Sets answered by to the given string.  This DOES NOT save tracking to the DB
	 * Used by CallManagerPanel internally to keep client states correct without
	 * resaving to the DB (which will be done by the person who answered it)
	 * @param answeredBy
	 */
	public void setAnsweredBy(String answeredBy){
		
		this.answeredBy = answeredBy;
		
	}
	
	/**
	 * Adds a record to the DB to indicate that this PhoneCall is in the answered state
	 * @param answeredBy User Name of person who answered
	 */
	public void trackAnswered(String answeredBy){
		
		this.answeredBy = answeredBy;
		
		Statement statement = null;
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  //$NON-NLS-1$
				+ "'" + callerID + "', 'A', '" + answeredBy + "', " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ channelID + ")"; //$NON-NLS-1$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingAnsweredState") //$NON-NLS-1$ 
        			+ callerID);
        	
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  //$NON-NLS-1$
				+ "'" + callerID + "', 'P', '" + parkedBy + "', " //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ channelID + ")"; //$NON-NLS-1$
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingParkedState") //$NON-NLS-1$ 
        			+ callerID);
        	
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  //$NON-NLS-1$
				+ "'" + callerID + "', 'H', '" + hangupBy + "'," //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ channelID + ")"; //$NON-NLS-1$ 
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingHangupState") //$NON-NLS-1$ 
        			+ callerID);
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
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
					+ callerID + "'"; //$NON-NLS-1$
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
		    		person.alert = personResultSet.getString("alert"); //$NON-NLS-1$
		    		
		    		if(person.alert.equals("N")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertNormal"); //$NON-NLS-1$
		    		else if(person.alert.equals("W")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertWarning"); //$NON-NLS-1$
		    		else if(person.alert.equals("B")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertBanned"); //$NON-NLS-1$
		    		else if(person.alert.equals("F")) //$NON-NLS-1$
		    			person.alert = xStrings.getString("PhoneCall.alertFavourite"); //$NON-NLS-1$
		    		
		    		//Name
		    		person.name = personResultSet.getString("name"); //$NON-NLS-1$
		    		
		    		if(person.name == null || person.name.equals("null")) //$NON-NLS-1$
		    			person.name = xStrings.getString("PhoneCall.unknownCaller"); //$NON-NLS-1$
		    		
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
		    		
		    		if(person.location == null || person.location.equals("null")){//$NON-NLS-1$
		    			
		    			if(callLocation != null)
		    				person.location = callLocation;
		    			else
		    				person.location = xStrings.getString("PhoneCall.locationUnknown"); //$NON-NLS-1$
		    			
		    		}
		    		
		    		//Postal Address
		    		person.postalAddress = personResultSet.getString("address"); //$NON-NLS-1$
		    		
		    		if(person.postalAddress == null || person.postalAddress.equals("null")) //$NON-NLS-1$
		    			person.postalAddress = ""; //$NON-NLS-1$
		    		
		    		//Post Code
		    		person.postCode = personResultSet.getString("postcode"); //$NON-NLS-1$
		    		
		    		if(person.postCode == null || person.postCode.equals("null")) //$NON-NLS-1$
		    			person.postCode = ""; //$NON-NLS-1$
		    		
		    		//Email Address
		    		person.email = personResultSet.getString("email"); //$NON-NLS-1$
		    		
		    		if(person.email == null || person.email.equals("null")) //$NON-NLS-1$
		    			person.email = ""; //$NON-NLS-1$
		    		
		    		//Language
		    		person.language = personResultSet.getString("language"); //$NON-NLS-1$
		    		
		    		if(person.language == null || person.language.equals("null")) //$NON-NLS-1$
		    			person.language = ""; //$NON-NLS-1$
		    		
		    		//Religion
		    		person.religion = personResultSet.getString("religion"); //$NON-NLS-1$
		    		
		    		if(person.religion == null || person.religion.equals("null")) //$NON-NLS-1$
		    			person.religion = ""; //$NON-NLS-1$
		    		
		    		//Journey
		    		person.journey = personResultSet.getString("journey"); //$NON-NLS-1$
		    		
		    		if(person.journey == null || person.journey.equals("null")) //$NON-NLS-1$
		    			person.journey = ""; //$NON-NLS-1$
		    		
		    		//Notes
		    		person.notes = personResultSet.getString("notes"); //$NON-NLS-1$
		    		
		    		if(person.notes == null || person.notes.equals("null")) //$NON-NLS-1$
		    			person.notes = ""; //$NON-NLS-1$
		    		
		    		//Get the conversation history for this person
		    		getConversationHistory(person);
		    		
		    		//Add to Vector
		    		people.add(person);
		    		
		    		retryCount = -1;
		    		
		    	}
		    	
		    	if(people.size() < 1){
		    		
		    		/* No person exists yet so lets wait 1 second and try again? */
		    		retryCount++;
		    		
		    	}
		    	
		    }else{
		    	
		    	retryCount++;
	    		
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
		
		if(retryCount > -1 && retryCount < 5){
			
			try {
				Thread.sleep(1000L);
				populatePersonDetails();
				retryCount = -1;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}else if(retryCount > 4)
			LOGGER.severe(xStrings.getString("PhoneCall.noRecordTimeOut")); //$NON-NLS-1$
		
	}
	
	/**
	 * Reads any conversation history this person has from the DB
	 * @param person
	 */
	private void getConversationHistory(Person person) {
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		String SQL = "SELECT time, channel, conversation FROM conversations WHERE person_id = " +//$NON-NLS-1$ 
						person.id; 
		
		try{
			
			statement = database.getReadConnection().createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				double dbChannel = Double.parseDouble(resultSet.getString("channel")); //$NON-NLS-1$
				double localChannel = Double.parseDouble(channelID);
				
				if(dbChannel == localChannel){
					person.currentConversation = resultSet.getString("conversation"); //$NON-NLS-1$
				}else{

					Date time = resultSet.getTimestamp("time"); //$NON-NLS-1$
					person.addConversation(new Conversation(time, 
							resultSet.getString("conversation"))); //$NON-NLS-1$
					
				}
				
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
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("PhoneCall.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		
		if(!headless)
			JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("PhoneCall.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		
		LOGGER.severe(friendlyErrorMessage);
		
	}

	/**
	 * Called via a thread in AsteriskManager to trackRinging without blocking event processing
	 */
	@Override
	public void run() {
		
		if(threadMode == 'Q'){
			//Will be Q or R when it first comes in, we use R to setup call skeleton
			trackQueue(threadOperator);
			asteriskManager.sendNewQueueEntryMessage(queueEntry);
		}else if(threadMode == 'H'){
			//Automated Hang up, user hang ups will bypass this
			trackHangup(threadOperator); 
		}else if(threadMode == 'A'){
			trackAnswered(threadOperator);
		}else if(threadMode == 'R')
			createCallSkeleton();
		
	}
	
	/**
	 * Set the value of the given field, background work for custom fields in the future
	 * @param fieldName use the mapping value to get the correct field
	 * @param value
	 */
	public void setField(String fieldName, String value){
		
		if(fieldName.equals("name")) //$NON-NLS-1$
			getActivePerson().name = value;
		else if(fieldName.equals("location")) //$NON-NLS-1$
			getActivePerson().location = value;
		else if(fieldName.equals("email")) //$NON-NLS-1$
			getActivePerson().email = value;
		else if(fieldName.equals("postcode")) //$NON-NLS-1$
			getActivePerson().postCode = value;
		else if(fieldName.equals("address")) //$NON-NLS-1$
			getActivePerson().postalAddress = value;
		else if(fieldName.equals("notes")) //$NON-NLS-1$
			getActivePerson().notes = value;
		else if(fieldName.equals("alert")) //$NON-NLS-1$
			getActivePerson().alert = value;
		else if(fieldName.equals("journey")) //$NON-NLS-1$
			getActivePerson().journey = value;
		else if(fieldName.equals("religion")) //$NON-NLS-1$
			getActivePerson().religion = value;
		else if(fieldName.equals("language")) //$NON-NLS-1$
			getActivePerson().language = value;
		else if(fieldName.equals("gender")) //$NON-NLS-1$
			getActivePerson().gender = value;
		else if(fieldName.equals("conversation")) //$NON-NLS-1$
			addConversation(value);
		else if(fieldName.equals("calltype")) //$NON-NLS-1$
			setCallType(value, false);
		else{
			
			//TODO some sort of custom field
			
		}
		
	}
	
	/**
	 * Returns the CallType expressed as a path to the badge icon
	 * @return path to the badge icon usually images/blah.png
	 */
	public String getCallTypeIconPath(){
		
		String iconPath = null;
		
		String SQL = "SELECT options FROM callinputfields WHERE language = '"  //$NON-NLS-1$
				+ xStrings.getLocale() + "' AND mapping = 'calltype'"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			statement = database.getReadConnection().createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				
				String[] types = resultSet.getString("options").split(",");//$NON-NLS-1$ //$NON-NLS-2$
				
				boolean found = false;
				int i = 0;
				
				while(i < types.length && !found){
					
					String[] field = types[i].split("=>"); //$NON-NLS-1$
					
					if(field[0].equals(calltype)){
						
						found = true;
						iconPath = field[1];
						
					}
					
					i++;
					
				}
				
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
		
		return iconPath;
		
	}
	
	
	/**
	 * Internal method to set this phone calls type
	 * @param type
	 * @param updateDB flag to indicate whether we need to update the DB with this value
	 */
	private void setCallType(String type, boolean updateDB){
		
		calltype = type;
		
		if(updateDB){
			
			Statement statement = null;
			
			String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel, type) VALUES("  //$NON-NLS-1$
					+ "'" + callerID + "', 'T', " //$NON-NLS-1$ //$NON-NLS-2$ 
					+ channelID + ", '" + type + "')"; //$NON-NLS-1$ //$NON-NLS-2$
			
			try{
				
				statement = database.getWriteConnection().createStatement();
				statement.executeUpdate(SQL);
		        
			}catch(SQLException e){
	        	
	        	showError(e, xStrings.getString("PhoneCall.errorChangingCallType") //$NON-NLS-1$ 
	        			+ callerID);
	        	
	        }finally{
	            if(statement != null)
	            	try{
	            		statement.close();
	            	}catch(Exception e){}
	        }
			
		}
		
	}
	
}
