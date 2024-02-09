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
	
	/**
	 * Used by Clients to create a call record based on given details
	 * @param database
	 * @param callerID
	 * @param channelID
	 */
	public PhoneCall(DatabaseManager database, String callerID, String channelID) {
		
		this.database = database;
		this.channelID = channelID;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		
		populatePersonDetails();
		
	}
	
	/**
	 * Used by Clients to create a call record with specified details
	 * @param database
	 * @param callerID
	 * @param channelID
	 * @param callLocation
	 */
	public PhoneCall(DatabaseManager database, String callerID, String channelID, 
			String callLocation){
		
		this.callLocation = callLocation;
		this.database = database;
		this.channelID = channelID;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		//TODO When a call is queued without a conversation it might have been from
		//a call we initiated, the conversation goes because of a channel switch
		//from originator to receiver once the call is put on hold :|
		populatePersonDetails();
		lookupCallType(channelID);
		
	}

	/**
	 * Used by the Server to create a call record
	 * @param database
	 * @param callerID
	 * @param channelID
	 * @param asteriskManager
	 * @param mode
	 * @param from
	 */
	public PhoneCall(DatabaseManager database, String callerID, String channelID, 
			AsteriskManager asteriskManager, char mode, String from){
	
		this.database = database;
		this.channelID = channelID;
		this.asteriskManager = asteriskManager;
		headless = true;
		threadMode = mode;
		threadOperator = from;
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerID);
		
	}
	
	/**
	 * Used by Server to create a call when the Queued message is received
	 * @param database
	 * @param queueEntry
	 * @param asteriskManager
	 * @param callerIDSubstitute If not null, use this as the callerid, not the queue entry record
	 */
	public PhoneCall(DatabaseManager database, AsteriskQueueEntry queueEntry, 
			AsteriskManager asteriskManager, String callerIDSubstitute) {
		
		this.database = database;
		this.queueEntry = queueEntry;
		this.asteriskManager = asteriskManager;
		this.channelID = queueEntry.getChannel().getId();
		threadMode = 'Q';
		threadOperator = "NA"; 
		headless = true;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		if(callerIDSubstitute != null)
			this.callerID = callerIDSubstitute;
		else
			this.callerID =  checkNumberWithHeld(
					queueEntry.getChannel().getCallerId());
		
	}
	
	/**
	 * Used by the server to create a skeleton of a call with a particular mode
	 * At the moment only using it for manual calls (M mode)
	 * 
	 * @param callerId Caller ID to add to the manual call (probably UNKNOWN)
	 * @param channel Channel to assign to manual call (this is a date.getTime variant as its not a real channel)
	 * @param mode Mode to set (usually M)
	 * @param database Reference to Database
	 */
	public PhoneCall(String callerId, String channel, char mode, String operator, DatabaseManager database){
		
		//TODO Use this to have a manual call, need to edit callmanager.addmanual
		headless = true;
		this.channelID = channel;
		this.database = database;
		this.threadMode = mode;
		this.threadOperator = operator;
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerId);
		
	}
	
	/**
	 * Used by the server to create skeleton for new calls (if necessary)
	 * Usually here when calls are in the ringing state.
	 * @param callerId Caller ID aka Number of the call (can be unknown)
	 * @param channel Channel that the call has (used for tracking)
	 * @param database Database link to use for queries
	 */
	public PhoneCall(String callerId, String channel, DatabaseManager database){
		
		headless = true;
		this.channelID = channel;
		this.database = database;
		this.threadMode = 'R';
		
		xStrings = new I18NStrings(database.getUserSettings().get("language"),  
				database.getUserSettings().get("country")); 
		
		/* BUG FIX CALLERID WITHHELD */
		this.callerID =  checkNumberWithHeld(callerId);
	
	}
	
	/**
	 * Returns the callerid of this record
	 * @return
	 */
	public String getNumber() {
		
		return callerID;
		
	}
	
	/**
	 * Helper method to replace with held number with a suitable string
	 * @param id callerid to check
	 * @return caller id number or suitable string if withheld/null
	 */
	private String checkNumberWithHeld(CallerId id){
		
		String callerid = null;
		
		if(id != null && id.getNumber() != null && id.getNumber().trim().length() > 0 &&
				!id.getNumber().equalsIgnoreCase("null")) 
			callerid = id.getNumber();
		else
			callerid = xStrings.getString("PhoneCall.withHeldNumber"); 
		
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
				!id.equalsIgnoreCase("null")) 
			callerid = id;
		else
			callerid = xStrings.getString("PhoneCall.withHeldNumber"); 
		
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
			
			String SQL = "SELECT type FROM callhistory WHERE callchannel = \"" + channel +  
					"\" AND type != 'NA' ORDER BY callhistory_id DESC LIMIT 1"; 
			
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next())
		    	calltype = resultSet.getString("type"); 
		    
		}catch (SQLException e){
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel) VALUES("  
				+ "'" + callerID + "', 'R', "   
				+ channelID + ")"; 
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingRingState")  
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
	public void createCallSkeleton(String mode, String operator){
		
		// If operator = null set it to NA
		if(operator == null)
			operator = "NA"; 
		
		// Lookup phone number if its already there get person id
		Statement statement = null, writeStatement = null;
		ResultSet resultSet = null, personResult = null, historyResult = null;
		
		String SQL = null;
		
		try{
			
			SQL = "SELECT person_id FROM phonenumbers WHERE phone_number = '" + callerID  
					+ "' ORDER BY lastUpdate DESC LIMIT 1"; 
			
			statement = database.getReadConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    int personID = -1;
		    
		    while(resultSet.next())
		    	personID = resultSet.getInt("person_id"); 
		    
		    if(personID == -1){
		    	
		    	// If not there create blank new person record + phonenumber and get person id
				writeStatement = database.getWriteConnection().createStatement();
				SQL = "INSERT INTO person VALUES()"; 
				
				writeStatement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
				personResult = writeStatement.getGeneratedKeys();
				
				if(personResult.next())
					personID = personResult.getInt(1);
				
				SQL = "INSERT INTO phonenumbers (phone_number, person_id) VALUES('"  
						+ callerID + "', '" + personID + "')";  
		    	
				writeStatement.executeUpdate(SQL);
				
		    }
		    
		    if(personID != -1){ //Should never be -1 if got this far
		    	
			    // Use person id to add an R Log with channel to callhistory
			    SQL = "INSERT INTO callhistory(`phonenumber`, `state`, `callchannel`, `activePerson`, `operator`) " 
			    		+ "VALUES ('" + callerID + "', '" + mode + "', '" + channelID + "', '"      
			    		+ personID + "', '" + operator + "')";   
			    
			    if(writeStatement == null)
			    	writeStatement = database.getWriteConnection().createStatement();
			    
			    writeStatement.executeUpdate(SQL);

		    }
			
		}catch (SQLException e){
			showError(e, xStrings.getString("PhoneCall.databaseSQLError") + " " + SQL);  
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel, operator) VALUES("  
				+ "'" + callerID + "', 'Q', "   
				+ channelID + ", '" + operator + "')";  
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL, Statement.RETURN_GENERATED_KEYS);
			
			ResultSet rs = statement.getGeneratedKeys();
			int rowID = -1;
			
			if(rs.next())
				rowID = rs.getInt(1);
			
			// If operator is NA then add the active person to this entry
			if(operator.equals("NA")){ 
				
				SQL = "UPDATE callhistory SET activePerson = " + getPersonFromNumber(callerID) +  
						" WHERE callhistory_id = " + rowID; 
				
				updateStatement = database.getWriteConnection().createStatement();
				updateStatement.executeUpdate(SQL);
				
			}
			
			retryCount = -1;
			
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingRingState")  
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
			
			String SQL = "SELECT person_id FROM phonenumbers WHERE phone_number = '"  
					+ number  + "' ORDER BY lastUpdate DESC LIMIT 1"; 
			
			LOGGER.info(xStrings.getString("PhoneCall.getPersonFromNumber") + SQL); 
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next())
		    	id = resultSet.getString("person_id"); 
		    
		}catch (SQLException e){
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  
				+ "'" + callerID + "', 'A', '" + answeredBy + "', "   
				+ channelID + ")"; 
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingAnsweredState")  
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  
				+ "'" + callerID + "', 'P', '" + parkedBy + "', "   
				+ channelID + ")"; 
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingParkedState")  
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
		
		String SQL = "INSERT INTO callhistory(phonenumber, state, operator, callchannel) VALUES("  
				+ "'" + callerID + "', 'H', '" + hangupBy + "', \""   
				+ channelID + "\")";  
		
		try{
			
			statement = database.getWriteConnection().createStatement();
			statement.executeUpdate(SQL);
	        
		}catch(SQLException e){
        	
        	showError(e, xStrings.getString("PhoneCall.errorTrackingHangupState")  
        			+ callerID);
        	
        }finally{
            if(statement != null)
            	try{
            		statement.close();
            	}catch(Exception e){}
        }
		
	}
	
	/** 
	 * Adds the given person to the record and sets it as the active person
	 * @param id id of person to set as new active
	 */
	public void addActivePerson(Person newActive){
		
		people.add(newActive);
		setActivePerson(newActive.id);
		
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
			String SQL = "SELECT numbers_id, person_id FROM phonenumbers WHERE phone_number='"  
					+ callerID + "'"; 
			statement = database.getConnection().createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    boolean first = true;
		    
		    while(resultSet.next()){
		    	
		    	numberIDs.add(resultSet.getInt("numbers_id")); 
		    	
		    	if(first){
		    		SQL = "" + resultSet.getInt("person_id");  
		    		first = false;
		    	}else
		    		SQL += " OR person_id = " + resultSet.getInt("person_id");  
		    	
		    }
		    
		    if(!first){
		    	
		    	//We got some results so lets look them up
		    	SQL = "SELECT * FROM person WHERE person_id = " + SQL; 
		    	personStatement = database.getConnection().createStatement();
		    	personResultSet = personStatement.executeQuery(SQL);
		    	
		    	while(personResultSet.next()){
		    		
		    		Person person = new Person(personResultSet.getInt("person_id"), 
		    				database.getUserSettings().get("language"),  
		    				database.getUserSettings().get("country")); 
		    		
		    		//Alert level e.g. person banned or warning because they're awkward
		    		person.alert = personResultSet.getString("alert"); 
		    		
		    		if(person.alert.equals("N")) 
		    			person.alert = xStrings.getString("PhoneCall.alertNormal"); 
		    		else if(person.alert.equals("W")) 
		    			person.alert = xStrings.getString("PhoneCall.alertWarning"); 
		    		else if(person.alert.equals("B")) 
		    			person.alert = xStrings.getString("PhoneCall.alertBanned"); 
		    		else if(person.alert.equals("F")) 
		    			person.alert = xStrings.getString("PhoneCall.alertFavourite"); 
		    		
		    		//Name
		    		person.name = personResultSet.getString("name"); 
		    		
		    		if(person.name == null || person.name.equals("null")) 
		    			person.name = xStrings.getString("PhoneCall.unknownCaller"); 
		    		
		    		//Gender
		    		person.gender = personResultSet.getString("gender"); 
		    		
		    		if(person.gender.equals("U")) 
		    			person.gender = xStrings.getString("PhoneCall.genderUnknown"); 
		    		else if(person.gender.equals("M")) 
		    			person.gender = xStrings.getString("PhoneCall.genderMale"); 
		    		else if(person.gender.equals("F")) 
		    			person.gender = xStrings.getString("PhoneCall.genderFemale"); 
		    		
		    		//Location
		    		person.location = personResultSet.getString("location"); 
		    		
		    		if(person.location == null || person.location.equals("null")){
		    			
		    			if(callLocation != null)
		    				person.location = callLocation;
		    			else
		    				person.location = xStrings.getString("PhoneCall.locationUnknown"); 
		    			
		    		}
		    		
		    		//Postal Address
		    		person.postalAddress = personResultSet.getString("address"); 
		    		
		    		if(person.postalAddress == null || person.postalAddress.equals("null")) 
		    			person.postalAddress = ""; 
		    		
		    		//Post Code
		    		person.postCode = personResultSet.getString("postcode"); 
		    		
		    		if(person.postCode == null || person.postCode.equals("null")) 
		    			person.postCode = ""; 
		    		
		    		//Email Address
		    		person.email = personResultSet.getString("email"); 
		    		
		    		if(person.email == null || person.email.equals("null")) 
		    			person.email = ""; 
		    		
		    		//Language
		    		person.language = personResultSet.getString("language"); 
		    		
		    		if(person.language == null || person.language.equals("null")) 
		    			person.language = ""; 
		    		
		    		//Religion
		    		person.religion = personResultSet.getString("religion"); 
		    		
		    		if(person.religion == null || person.religion.equals("null")) 
		    			person.religion = ""; 
		    		
		    		//Journey
		    		person.journey = personResultSet.getString("journey"); 
		    		
		    		if(person.journey == null || person.journey.equals("null")) 
		    			person.journey = ""; 
		    		
		    		//Notes
		    		person.notes = personResultSet.getString("notes"); 
		    		
		    		if(person.notes == null || person.notes.equals("null")) 
		    			person.notes = ""; 
		    		
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
			showError(e, xStrings.getString("PhoneCall.databaseSQLError")); 
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
			LOGGER.severe(xStrings.getString("PhoneCall.noRecordTimeOut")); 
		
	}
	
	/**
	 * Reads any conversation history this person has from the DB
	 * @param person
	 */
	private void getConversationHistory(Person person) {
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		String SQL = "SELECT time, channel, conversation FROM conversations WHERE person_id = " + 
						person.id; 
		
		try{
			
			statement = database.getReadConnection().createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				String dbChannel = resultSet.getString("channel"); 
				
				if(dbChannel.equals(channelID)){
					person.currentConversation = resultSet.getString("conversation"); 
				}else{

					Date time = resultSet.getTimestamp("time"); 
					person.addConversation(new Conversation(time, 
							resultSet.getString("conversation"))); 
					
				}
				
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
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("PhoneCall.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		
		if(!headless)
			JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("PhoneCall.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
		
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
			asteriskManager.sendNewQueueEntryMessage(queueEntry, callerID);
		}else if(threadMode == 'H'){
			//Automated Hang up, user hang ups will bypass this
			trackHangup(threadOperator); 
		}else if(threadMode == 'A'){
			createCallSkeleton("A", threadOperator); 
			//trackAnswered(threadOperator);
		}else if(threadMode == 'M'){
			createCallSkeleton("M", threadOperator); 
			//trackAnswered(threadOperator);
		}else if(threadMode == 'R')
			createCallSkeleton("R", null); 
		
	}
	
	/**
	 * Set the value of the given field, background work for custom fields in the future
	 * @param fieldName use the mapping value to get the correct field
	 * @param value
	 */
	public void setField(String fieldName, String value){
		
		if(fieldName.equals("name")) 
			getActivePerson().name = value;
		else if(fieldName.equals("location")) 
			getActivePerson().location = value;
		else if(fieldName.equals("email")) 
			getActivePerson().email = value;
		else if(fieldName.equals("postcode")) 
			getActivePerson().postCode = value;
		else if(fieldName.equals("address")) 
			getActivePerson().postalAddress = value;
		else if(fieldName.equals("notes")) 
			getActivePerson().notes = value;
		else if(fieldName.equals("alert")) 
			getActivePerson().alert = value;
		else if(fieldName.equals("journey")) 
			getActivePerson().journey = value;
		else if(fieldName.equals("religion")) 
			getActivePerson().religion = value;
		else if(fieldName.equals("language")) 
			getActivePerson().language = value;
		else if(fieldName.equals("gender")) 
			getActivePerson().gender = value;
		else if(fieldName.equals("conversation")) 
			addConversation(value);
		else if(fieldName.equals("calltype")) 
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
		
		String SQL = "SELECT options FROM callinputfields WHERE language = '"  
				+ xStrings.getLocale() + "' AND mapping = 'calltype'"; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			statement = database.getReadConnection().createStatement();
			resultSet = statement.executeQuery(SQL);
	    
			while(resultSet.next()){
				
				
				String[] types = resultSet.getString("options").split(","); 
				
				boolean found = false;
				int i = 0;
				
				while(i < types.length && !found){
					
					String[] field = types[i].split("=>"); 
					
					if(field[0].equals(calltype)){
						
						found = true;
						iconPath = field[1];
						
					}
					
					i++;
					
				}
				
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
			
			String SQL = "INSERT INTO callhistory(phonenumber, state, callchannel, type) VALUES("  
					+ "'" + callerID + "', 'T', "   
					+ channelID + ", '" + type + "')";  
			
			try{
				
				statement = database.getWriteConnection().createStatement();
				statement.executeUpdate(SQL);
		        
			}catch(SQLException e){
	        	
	        	showError(e, xStrings.getString("PhoneCall.errorChangingCallType")  
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
