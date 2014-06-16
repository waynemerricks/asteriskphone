package com.thevoiceasia.phonebox.callinput;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import com.thevoiceasia.phonebox.records.Person;

public class SearchPanel extends JDialog {

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
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(SearchPanel.class.getName());//Logger
	
	public SearchPanel(Component owner, String title, String language, String country, 
			Connection readConnection, Connection writeConnection, String numberToSearch){
		
		xStrings = new I18NStrings(language, country);
		
		this.setSize(400, 200);
		this.setTitle(title);
		
		LOGGER.info(xStrings.getString("SearchPanel.gettingPeopleFromNumber") + " " + numberToSearch); //$NON-NLS-1$ //$NON-NLS-2$
		getPeopleFromNumber(numberToSearch, readConnection);
		
		LOGGER.info(xStrings.getString("SearchPanel.creatingSearchPanel")); //$NON-NLS-1$
		this.setLayout(new MigLayout("fillx")); //$NON-NLS-1$
		
		//** NAME FIELD **//
		name = new JTextField(""); //$NON-NLS-1$
		name.setToolTipText(xStrings.getString("SearchPanel.searchByName")); //$NON-NLS-1$
		
		name.addKeyListener(new KeyListener(){

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}

			@Override
			public void keyReleased(KeyEvent e) {
				
				//TODO create a thread to search by name
				System.out.println("released: " + e.getKeyCode());
				System.out.println("released text: " + ((JTextField)e.getSource()).getText());
				
			}
			
		});
		
		JLabel lbl = new JLabel(xStrings.getString("SearchPanel.nameField")); //$NON-NLS-1$
		lbl.setLabelFor(name);
		
		this.add(lbl);
		this.add(name, "growx, pushx, wrap"); //$NON-NLS-1$
		
		
		number = new JTextField(""); //$NON-NLS-1$
		number.setToolTipText(xStrings.getString("SearchPanel.searchByNumber")); //$NON-NLS-1$

		number.addKeyListener(new KeyListener(){

			@Override
			public void keyTyped(KeyEvent e) {}

			@Override
			public void keyPressed(KeyEvent e) {}
			
			@Override
			public void keyReleased(KeyEvent e) {
				
				// TODO Create a thread to search by number
				System.out.println("released: " + e.getKeyCode());
				System.out.println("released text: " + ((JTextField)e.getSource()).getText());
				
			}
			
		});
		
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
				
				if(row % 2 == 0)//if we're an odd row go green
					backgroundColour = Color.WHITE;	
				else
					backgroundColour = new Color(189, 224, 194);
				
				JLabel l = (JLabel)c;
				l.setHorizontalAlignment(JLabel.CENTER);
					
				c.setBackground(backgroundColour);
				
				return c;
				
			}
			
		};
		
	}
	
	private void getPeopleFromNumber(String number, Connection readConnection){
		
		//TODO
		//Get the records cross referenced from person and phonenumbers
		String SQL = "SELECT phone_number, person.* FROM phonenumbers INNER JOIN person ON " + //$NON-NLS-1$
				"phonenumbers.person_id = person.person_id " + //$NON-NLS-1$
				"WHERE phone_number = '" + number + "' ORDER BY person.name ASC"; //$NON-NLS-1$ //$NON-NLS-2$
		
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
		
		//TODO
		
		
	}
	
	public Person getSelectedPerson(){
		
		return selectedPerson;
		
	}
	
	public static void main(String[] args){
		//Component owner, String title, String language, String country, 
		//Connection readConnection, Connection writeConnection
		new SearchPanel(null, "title", "en", "GB", null, null, "").setVisible(true);
	}

}
