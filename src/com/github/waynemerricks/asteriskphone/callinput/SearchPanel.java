package com.github.waynemerricks.asteriskphone.callinput;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import com.github.waynemerricks.asteriskphone.records.Person;

import net.miginfocom.swing.MigLayout;

public class SearchPanel extends JDialog implements ActionListener, KeyListener {

	private static final long serialVersionUID = 1L;
	
	private Person selectedPerson = null;
	private I18NStrings xStrings;
	private JTextField name, number;
	private ChangePersonModel tableModel;
	private JTable people;
	//ArrayList is better but table uses Vectors so pointless having two identical sets of data
	private Vector<Person> records = new Vector<Person>();
	private String country, language;
	private String[] columnNames = null;
	private Connection readConnection;
	private SearchPersonThread searchPersonThread = null;
	private SearchTerm searchTerm = null;
	private ArrayList<PersonChangedListener> notifyMe = new ArrayList<PersonChangedListener>();
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(SearchPanel.class.getName());//Logger
	
	/**
	 * Creates a dialog that allows you to search for people and return the selected person
	 * @param owner Component that owns this dialog
	 * @param title Title of this dialog
	 * @param language I18N language e.g. en
	 * @param country I18N country e.g. GB
	 * @param readConnection Read Connection to the database
	 * @param writeConnection Write Connection to the database
	 * @param numberToSearch Number this panel will search for on creation
	 */
	public SearchPanel(JFrame owner, String title, String language, String country, 
			Connection readConnection, String numberToSearch){
		
		super(owner, true);//Set owner and modal
		
		xStrings = new I18NStrings(language, country);
		this.language = language;
		this.country = country;
		this.readConnection = readConnection;
		this.setSize(400, 320);
		this.setTitle(title);
		
		LOGGER.info(xStrings.getString("SearchPanel.gettingPeopleFromNumber") + " " + numberToSearch);  
		addNewPersonRecord();
		getPeopleFromNumber(numberToSearch, false);
		
		LOGGER.info(xStrings.getString("SearchPanel.creatingSearchPanel")); 
		this.setLayout(new MigLayout("fillx, insets 0 5 0 5")); 
		
		//** NAME FIELD **//
		name = new JTextField(""); 
		name.setToolTipText(xStrings.getString("SearchPanel.searchByName")); 
		name.addKeyListener(this);
		
		JLabel lbl = new JLabel(xStrings.getString("SearchPanel.nameField")); 
		lbl.setLabelFor(name);
		
		this.add(lbl);
		this.add(name, "growx, pushx, wrap"); 
		
		number = new JTextField(""); 
		number.setToolTipText(xStrings.getString("SearchPanel.searchByNumber")); 
		number.addKeyListener(this);
		
		lbl = new JLabel(xStrings.getString("SearchPanel.numberField")); 
		lbl.setLabelFor(number);
		
		this.add(lbl);
		this.add(number, "growx, pushx, wrap"); 
		
		//Create the Table
		buildTableColumns();
		tableModel = new ChangePersonModel(records, columnNames);
		
		people = new JTable(tableModel){
			
			private static final long serialVersionUID = 1L;

			public Component prepareRenderer(TableCellRenderer renderer, 
					int row, int column){
				
				Component c = super.prepareRenderer(renderer, row, column);
				
				Color backgroundColour = null;
				
				if(row % 2 == 0){//if we're an odd row go green
					if(people.isRowSelected(row))
						backgroundColour = Color.BLUE;
					else
						backgroundColour = Color.WHITE;	
				}else{
					if(people.isRowSelected(row))
						backgroundColour = Color.BLUE;
					else
						backgroundColour = new Color(189, 224, 194);
				}
				
				JLabel l = (JLabel)c;
				l.setHorizontalAlignment(JLabel.CENTER);
					
				c.setBackground(backgroundColour);
				
				return c;
				
			}
			
		};
		
		people.setRowSelectionAllowed(true);
		people.setAutoCreateRowSorter(true);
		
		this.add(people.getTableHeader(), "growx, span, wrap"); 
		this.add(new JScrollPane(people), "grow, pushy, span, wrap"); 
		
		this.add(new JLabel(" "), "push, growx");//blanking label to fill space  
		
		//Buttons
		JButton button = new JButton(xStrings.getString("SearchPanel.cancelButton")); 
		button.setMnemonic(xStrings.getString("SearchPanel.cancelButton").charAt(0)); 
		button.setToolTipText(xStrings.getString("SearchPanel.cancelToolTip")); 
		button.setActionCommand("cancel"); 
		button.addActionListener(this);
		
		this.add(button);
		
		button = new JButton(xStrings.getString("SearchPanel.okButton")); 
		button.setMnemonic(xStrings.getString("SearchPanel.okButton").charAt(0)); 
		button.setToolTipText(xStrings.getString("SearchPanel.okToolTip")); 
		button.setActionCommand("ok"); 
		button.addActionListener(this);
		
		this.add(button, "wrap"); 
		
		//Spawn search thread
		searchPersonThread = new SearchPersonThread(this);
		searchPersonThread.start();
		
	}
	
	/**
	 * Gets Person records from a DB read connection matching the number given
	 * @param number Number to search
	 * @param partialMatch if true will do an SQL query as LIKE blah% instead of exact match
	 */
	private void getPeopleFromNumber(String number, boolean partialMatch){
		
		//TODO Rework for custom fields (one day)
		//Get the records cross referenced from person and phonenumbers
		String SQL = "SELECT phone_number, person.* FROM phonenumbers INNER JOIN person ON " + 
				"phonenumbers.person_id = person.person_id " + 
				"WHERE phone_number "; 
		
		if(partialMatch)
			SQL += "LIKE '" + number + "%' ";  
		else
			SQL += "= '" + number + "' ";  
		
		SQL += "ORDER BY person.name ASC"; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("SearchPanel.lookingUpNumber")); 
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	Person person = new Person(resultSet.getInt("person_id"), language, country); 
		    	person.alert = resultSet.getString("alert"); 
		    	person.name = resultSet.getString("name"); 
		    	person.gender = resultSet.getString("gender"); 
		    	person.location = resultSet.getString("location"); 
		    	person.postalAddress = resultSet.getString("address"); 
		    	person.postCode = resultSet.getString("postcode"); 
		    	person.email = resultSet.getString("email"); 
		    	person.language = resultSet.getString("language"); 
		    	person.religion = resultSet.getString("religion"); 
		    	person.journey = resultSet.getString("journey"); 
		    	person.notes = resultSet.getString("notes"); 
		    	person.number = resultSet.getString("phone_number"); 
		    	
		    	records.add(person);

		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("SearchPanel.getLogSQLError")); 
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
	 * Gets Person records from a DB read connection matching the name given
	 * @param number Number to search
	 * @param partialMatch if true will do an SQL query as LIKE blah% instead of exact match
	 */
	private void getPeopleFromName(String name, boolean partialMatch){
		
		//TODO Rework for custom fields (one day)
		//Get the records cross referenced from person and phonenumbers
		String SQL = "SELECT person.*, phonenumbers.phone_number FROM person INNER JOIN phonenumbers ON " + 
				"person.person_id = phonenumbers.person_id " + 
				"WHERE person.name "; 
		
		if(partialMatch)
			SQL += "LIKE '" + name + "%' ";  
		else
			SQL += "= '" + name + "' ";  
		
		SQL += "ORDER BY person.name ASC LIMIT 10"; 
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("SearchPanel.lookingUpName")); 
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	Person person = new Person(resultSet.getInt("person_id"), language, country); 
		    	person.alert = resultSet.getString("alert"); 
		    	person.name = resultSet.getString("name"); 
		    	person.gender = resultSet.getString("gender"); 
		    	person.location = resultSet.getString("location"); 
		    	person.postalAddress = resultSet.getString("address"); 
		    	person.postCode = resultSet.getString("postcode"); 
		    	person.email = resultSet.getString("email"); 
		    	person.language = resultSet.getString("language"); 
		    	person.religion = resultSet.getString("religion"); 
		    	person.journey = resultSet.getString("journey"); 
		    	person.notes = resultSet.getString("notes"); 
		    	person.number = resultSet.getString("phone_number"); 
		    	
		    	records.add(person);

		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("SearchPanel.getLogSQLError")); 
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
	 * Adds a placeholder for selecting a new person in the table of records
	 */
	private void addNewPersonRecord(){
		
		Person person = new Person(-1, language, country);
    	person.name = xStrings.getString("SearchPanel.newPersonName"); 
    	person.location = xStrings.getString("SearchPanel.newPersonLocation"); 
    	person.id = -1; //-1 signifies this should create a record
    	person.number = ""; 

    	records.add(person);

	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	private void buildTableColumns(){
		
		//TODO read this from db instead of hard coding
		columnNames = new String[3];
		
		columnNames[0] = xStrings.getString("SearchPanel.nameField"); 
		columnNames[1] = xStrings.getString("SearchPanel.locationField"); 
		columnNames[2] = xStrings.getString("SearchPanel.numberField"); 
		
	}
	
	/**
	 * Set the number of this class that is used to lookup records.
	 * This allows you to set outside of the constructor and still have a
	 * search performed
	 * @param phoneNumber
	 */
	public void setNumber(String phoneNumber) {
		
		getPeopleFromNumber(phoneNumber, false);
		
	}
	
	/**
	 * Adds an object that this class will notify when a person has been changed
	 * @param pcl
	 */
	public void addPersonChangedListener(PersonChangedListener pcl){
		
		//This should notify the object that the person has changed as it will
		//usually be a call info panel so needs to update data accordingly and send
		//a PERSONCHANGED message via chat
		notifyMe.add(pcl);
		
	}
	
	/**
	 * Returns the person that was selected prior to this dialog closing
	 * @return
	 */
	public Person getSelectedPerson(){
		
		return selectedPerson;
		
	}
	
	/**
	 * Looks at the state of the SearchTerm object and updates accordingly
	 */
	public void checkDataNeedsUpdating(){
		
		//Query DB if there is a search term to use
		if(searchTerm != null && searchTerm.getSearchTerm() != null){
			
			LOGGER.info(xStrings.getString("SearchPanel.dataUpdating")); 
			searchTerm.setLocked(true);
			
			updateData(searchTerm.isNumber(), searchTerm.getSearchTerm());
			searchTerm.clear();
			
			searchTerm.setLocked(false);
			LOGGER.info(xStrings.getString("SearchPanel.dataUpdated")); 
			
		}
		
	}
	
	/**
	 * Updates the data given the mode and search term
	 * @param numberMode true = search by number, false = by name
	 * @param search term to search for
	 */
	private void updateData(boolean numberMode, String search){
		
		//Create a thread to search by name/number
		records.removeAllElements();
		
		//Add the placeholder for the new person record
		addNewPersonRecord();
		
		if(numberMode)
			getPeopleFromNumber(search, true);
		else
			getPeopleFromName(search, true);
		
		tableModel.fireTableDataChanged();
		
	}

	/**
	 * Spawns a thread to create a new Person record and associate it
	 * with owner panel/call input
	 */
	private void createNewPerson(){
		
		/* Not sending null as null numbers upset clients
		 * Instead will add a "new" field to Person class and check that
		 * 
		 * If we can, use the number typed in here for the record as well
		 */
		//Get the number from the panel if they've typed one in
		Person person = new Person(-1, language, country);
    	person.name = xStrings.getString("SearchPanel.newPersonName"); 
    	person.location = xStrings.getString("SearchPanel.newPersonLocation"); 
    	person.id = -1; //-1 signifies this should create a record
    	person.number = ""; 

    	person.number = number.getText().trim();
    	
    	if(person.number.length() < 1)
    		person.number = xStrings.getString("SearchPanel.unknown");
    	
    	person.setNewPerson(true);
    	
		/* All writes must happen server side */
		for(int i = 0; i < notifyMe.size(); i++)
			notifyMe.get(i).personChanged(person);
		
	}
	
	/**
	 * Spawns a thread to associate owner panel/call input with a given Person
	 * record
	 * @param selectedID id of the record to associate with
	 */
	private void setExistingPerson(int selectedID) {
		
		/* Set SelectedPerson to the one with an id match
		 * In theory this should change depending on the table sort
		 * so we can't just do a row 1 is selected so it should be person 1
		 * I'm unclear how the model lookups map back to views and vice
		 * versa so this may be unnecessary
		 */
		int found = -1;
		int i = 0;
		
		while(found == -1 && i < records.size()){
			
			if(records.get(i).id == selectedID)
				found = i;
			
			i++;
			
		}
		
		selectedPerson = records.get(found);
			
		for(int j = 0; j < notifyMe.size(); j++)
			notifyMe.get(j).personChanged(getSelectedPerson());
		
	}
	
	/**
	 * Removes all the objects that need to be notified of call changes
	 */
	public void removePersonChangedListeners(){
		
		while(notifyMe.size() > 0)
			notifyMe.remove(0);
		
	}
	
	/**
	 * Internally called when OK is clicked
	 */
	private void ok(){
		
		//OK button pressed, get selected person and update as necessary
		LOGGER.info(xStrings.getString("SearchPanel.okPressed")); 
		
		if(people.getSelectedRow() != -1){
			
			this.setVisible(false);
			
			//BUG FIX: Need to get value from the model.  Means you have to convert
			//Selected Row to model row and then call model.getValueAt(row, 99) for id
			int selectedRow =  people.convertRowIndexToModel(people.getSelectedRow());
			int selectedID = Integer.parseInt((String)tableModel.getValueAt(selectedRow, 99));
			
			if(selectedID == -1)
				createNewPerson();
			else
				setExistingPerson(selectedID);
			
			removePersonChangedListeners();
			
		}else{
			
			//Prompt user about not selecting a person?
			int confirm = JOptionPane.showConfirmDialog(this,
					xStrings.getString("SearchPanel.noPersonSelected"), 
					xStrings.getString("SearchPanel.noPersonSelectedTitle"), 
					JOptionPane.QUESTION_MESSAGE,
					JOptionPane.YES_NO_OPTION); 
			
			if(confirm == JOptionPane.YES_OPTION)//Do Nothing except hide me
				this.setVisible(false);
			
		}	
		
	}
	
	

	/**
	 * Internally called when Cancel is clicked
	 */
	private void cancel(){
		
		//Cancel button pressed
		LOGGER.info(xStrings.getString("SearchPanel.cancelPressed")); 
		this.setVisible(false);
		
	}
	
	/**
	 * ActionEvent listener
	 * @param e from the ok or cancel buttons
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("ok")) 
			ok();
		else if(e.getActionCommand().equals("cancel")) 
			cancel();
		
	}

	/**
	 * KeyListener used on the name and number fields, this will invoke a search
	 * on the db person/phone number records
	 * @param e
	 */
	@Override
	public void keyReleased(KeyEvent e) {
		
		//Get the term and flag whether this is a number or name search
		String search = ""; 
		boolean isNumber = false;
		
		if(e.getSource() instanceof JTextField)
			if(e.getSource() == number){
				
				search = number.getText();
				isNumber = true;
				
			}else if(e.getSource() == name)
				search = name.getText();
		
		if(searchTerm == null)//if null first run so create a term
			searchTerm = new SearchTerm(isNumber, search);
		else if(!searchTerm.isLocked())//not locked = we're doing nothing so update
			searchTerm.updateSearch(isNumber, search);
		else if(searchTerm.isLocked())//locked = we're running a query so queue this search
			searchTerm.queueSearch(isNumber, search);
		
	}

	/* KeyListener Events that we don't need to monitor */
	@Override
	public void keyTyped(KeyEvent e) {}

	@Override
	public void keyPressed(KeyEvent e) {}

}
