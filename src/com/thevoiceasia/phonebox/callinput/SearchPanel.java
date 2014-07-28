package com.thevoiceasia.phonebox.callinput;

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

import net.miginfocom.swing.MigLayout;

import com.thevoiceasia.phonebox.records.Person;

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
		
		LOGGER.info(xStrings.getString("SearchPanel.gettingPeopleFromNumber") + " " + numberToSearch); //$NON-NLS-1$ //$NON-NLS-2$
		addNewPersonRecord();
		getPeopleFromNumber(numberToSearch, false);
		
		LOGGER.info(xStrings.getString("SearchPanel.creatingSearchPanel")); //$NON-NLS-1$
		this.setLayout(new MigLayout("fillx, insets 0 5 0 5")); //$NON-NLS-1$
		
		//** NAME FIELD **//
		name = new JTextField(""); //$NON-NLS-1$
		name.setToolTipText(xStrings.getString("SearchPanel.searchByName")); //$NON-NLS-1$
		name.addKeyListener(this);
		
		JLabel lbl = new JLabel(xStrings.getString("SearchPanel.nameField")); //$NON-NLS-1$
		lbl.setLabelFor(name);
		
		this.add(lbl);
		this.add(name, "growx, pushx, wrap"); //$NON-NLS-1$
		
		number = new JTextField(""); //$NON-NLS-1$
		number.setToolTipText(xStrings.getString("SearchPanel.searchByNumber")); //$NON-NLS-1$
		number.addKeyListener(this);
		
		lbl = new JLabel(xStrings.getString("SearchPanel.numberField")); //$NON-NLS-1$
		lbl.setLabelFor(number);
		
		this.add(lbl);
		this.add(number, "growx, pushx, wrap"); //$NON-NLS-1$
		
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
		
		this.add(people.getTableHeader(), "growx, span, wrap"); //$NON-NLS-1$
		this.add(new JScrollPane(people), "grow, pushy, span, wrap"); //$NON-NLS-1$
		
		this.add(new JLabel(" "), "push, growx");//blanking label to fill space  //$NON-NLS-1$//$NON-NLS-2$
		
		//Buttons
		JButton button = new JButton(xStrings.getString("SearchPanel.cancelButton")); //$NON-NLS-1$
		button.setMnemonic(xStrings.getString("SearchPanel.cancelButton").charAt(0)); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("SearchPanel.cancelToolTip")); //$NON-NLS-1$
		button.setActionCommand("cancel"); //$NON-NLS-1$
		button.addActionListener(this);
		
		this.add(button);
		
		button = new JButton(xStrings.getString("SearchPanel.okButton")); //$NON-NLS-1$
		button.setMnemonic(xStrings.getString("SearchPanel.okButton").charAt(0)); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("SearchPanel.okToolTip")); //$NON-NLS-1$
		button.setActionCommand("ok"); //$NON-NLS-1$
		button.addActionListener(this);
		
		this.add(button, "wrap"); //$NON-NLS-1$
		
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
		String SQL = "SELECT phone_number, person.* FROM phonenumbers INNER JOIN person ON " + //$NON-NLS-1$
				"phonenumbers.person_id = person.person_id " + //$NON-NLS-1$
				"WHERE phone_number "; //$NON-NLS-1$
		
		if(partialMatch)
			SQL += "LIKE '" + number + "%' ";  //$NON-NLS-1$//$NON-NLS-2$
		else
			SQL += "= '" + number + "' ";  //$NON-NLS-1$//$NON-NLS-2$
		
		SQL += "ORDER BY person.name ASC"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("SearchPanel.lookingUpNumber")); //$NON-NLS-1$
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	Person person = new Person(resultSet.getInt("person_id"), language, country); //$NON-NLS-1$
		    	person.alert = resultSet.getString("alert"); //$NON-NLS-1$
		    	person.name = resultSet.getString("name"); //$NON-NLS-1$
		    	person.gender = resultSet.getString("gender"); //$NON-NLS-1$
		    	person.location = resultSet.getString("location"); //$NON-NLS-1$
		    	person.postalAddress = resultSet.getString("address"); //$NON-NLS-1$
		    	person.postCode = resultSet.getString("postcode"); //$NON-NLS-1$
		    	person.email = resultSet.getString("email"); //$NON-NLS-1$
		    	person.language = resultSet.getString("language"); //$NON-NLS-1$
		    	person.religion = resultSet.getString("religion"); //$NON-NLS-1$
		    	person.journey = resultSet.getString("journey"); //$NON-NLS-1$
		    	person.notes = resultSet.getString("notes"); //$NON-NLS-1$
		    	person.number = resultSet.getString("phone_number"); //$NON-NLS-1$
		    	
		    	records.add(person);

		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("SearchPanel.getLogSQLError")); //$NON-NLS-1$
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
		String SQL = "SELECT person.*, phonenumbers.phone_number FROM person INNER JOIN phonenumbers ON " + //$NON-NLS-1$
				"person.person_id = phonenumbers.person_id " + //$NON-NLS-1$
				"WHERE person.name "; //$NON-NLS-1$
		
		if(partialMatch)
			SQL += "LIKE '" + name + "%' ";  //$NON-NLS-1$//$NON-NLS-2$
		else
			SQL += "= '" + name + "' ";  //$NON-NLS-1$//$NON-NLS-2$
		
		SQL += "ORDER BY person.name ASC LIMIT 10"; //$NON-NLS-1$
		
		Statement statement = null;
		ResultSet resultSet = null;
		
		LOGGER.info(xStrings.getString("SearchPanel.lookingUpName")); //$NON-NLS-1$
		
		try{
			statement = readConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	Person person = new Person(resultSet.getInt("person_id"), language, country); //$NON-NLS-1$
		    	person.alert = resultSet.getString("alert"); //$NON-NLS-1$
		    	person.name = resultSet.getString("name"); //$NON-NLS-1$
		    	person.gender = resultSet.getString("gender"); //$NON-NLS-1$
		    	person.location = resultSet.getString("location"); //$NON-NLS-1$
		    	person.postalAddress = resultSet.getString("address"); //$NON-NLS-1$
		    	person.postCode = resultSet.getString("postcode"); //$NON-NLS-1$
		    	person.email = resultSet.getString("email"); //$NON-NLS-1$
		    	person.language = resultSet.getString("language"); //$NON-NLS-1$
		    	person.religion = resultSet.getString("religion"); //$NON-NLS-1$
		    	person.journey = resultSet.getString("journey"); //$NON-NLS-1$
		    	person.notes = resultSet.getString("notes"); //$NON-NLS-1$
		    	person.number = resultSet.getString("phone_number"); //$NON-NLS-1$
		    	
		    	records.add(person);

		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("SearchPanel.getLogSQLError")); //$NON-NLS-1$
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
    	person.name = xStrings.getString("SearchPanel.newPersonName"); //$NON-NLS-1$
    	person.location = xStrings.getString("SearchPanel.newPersonLocation"); //$NON-NLS-1$
    	person.id = -1; //-1 signifies this should create a record
    	person.number = ""; //$NON-NLS-1$
    	
    	records.add(person);
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("DatabaseManager.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("DatabaseManager.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
		LOGGER.severe(friendlyErrorMessage);
		
	}
	
	private void buildTableColumns(){
		
		//TODO read this from db instead of hard coding
		columnNames = new String[3];
		
		columnNames[0] = xStrings.getString("SearchPanel.nameField"); //$NON-NLS-1$
		columnNames[1] = xStrings.getString("SearchPanel.locationField"); //$NON-NLS-1$
		columnNames[2] = xStrings.getString("SearchPanel.numberField"); //$NON-NLS-1$
		
	}
	
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
			
			LOGGER.info(xStrings.getString("SearchPanel.dataUpdating")); //$NON-NLS-1$
			searchTerm.setLocked(true);
			
			updateData(searchTerm.isNumber(), searchTerm.getSearchTerm());
			searchTerm.clear();
			
			searchTerm.setLocked(false);
			LOGGER.info(xStrings.getString("SearchPanel.dataUpdated")); //$NON-NLS-1$
			
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
		
		/* Its better to keep writes on the server side for remote offices
		 * TODO Test how much of a delay there is with swapping the person
		 */
		for(int i = 0; i < notifyMe.size(); i++)
			notifyMe.get(i).personChanged(null);
		
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
		LOGGER.info(xStrings.getString("SearchPanel.okPressed")); //$NON-NLS-1$
		
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
					xStrings.getString("SearchPanel.noPersonSelected"), //$NON-NLS-1$
					xStrings.getString("SearchPanel.noPersonSelectedTitle"), //$NON-NLS-1$
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
		LOGGER.info(xStrings.getString("SearchPanel.cancelPressed")); //$NON-NLS-1$
		this.setVisible(false);
		
	}
	
	/**
	 * ActionEvent listener
	 * @param e from the ok or cancel buttons
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if(e.getActionCommand().equals("ok")) //$NON-NLS-1$
			ok();
		else if(e.getActionCommand().equals("cancel")) //$NON-NLS-1$
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
		String search = ""; //$NON-NLS-1$
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
