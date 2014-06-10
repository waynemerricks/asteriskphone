package com.thevoiceasia.phonebox.callinput;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import net.miginfocom.swing.MigLayout;

import com.thevoiceasia.phonebox.records.CallLog;
import com.thevoiceasia.phonebox.records.Person;

public class SearchPanel extends JDialog {

	private static final long serialVersionUID = 1L;
	
	private Person selectedPerson = null;
	private I18NStrings xStrings;
	private JTextField name, number;
	private ChangePersonModel tableModel;
	private JTable people;
	private HashMap<String, Person> records = new HashMap<String, Person>();
	private String country, language;
	private String[] columnNames = null;
	
	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(SearchPanel.class.getName());//Logger
	
	public SearchPanel(Component owner, String title, String language, String country, 
			Connection readConnection, Connection writeConnection, String numberToSearch){
		
		xStrings = new I18NStrings(language, country);
		
		this.setSize(400, 200);
		this.setTitle(title);
		
		
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
				
				//TODO
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
				
				// TODO Auto-generated method stub
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
				
		tableModel = new ChangePersonModel(tableData, columnNames);
		
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
		    	
		    	CallLog log = new CallLog(language, country,
		    			resultSet.getString("callchannel"),  //$NON-NLS-1$
		    			readConnection);
		    	
		    	if(log.isComplete())
		    		records.put(log.getChannel(), log);
		    	else{
		    		
		    		log = new CallLog(language, country, 
		    				resultSet.getString("callchannel"),  //$NON-NLS-1$
		    				readConnection, true);
		    		
		    		/* BUG FIX: Can get into a situation where a person does not exist
		    		 * in any records, this is usually when its an internal phone
		    		 * calling somewhere and its originator channel ends up in the
		    		 * call history.
		    		 * 
		    		 * These internal phones may never have been dealt with in the phone
		    		 * system so won't have a person/location attached.
		    		 * 
		    		 * To check this, see if we have a valid log time otherwise discard
		    		 * 
		    		 * If the time is null as part of error checking we gen from
		    		 * the current time which means you end up with phantom records
		    		 * in the top of the log.  So we need to discard invalid times!
		    		 */
		    		if(log.isValid())
		    			records.put(log.getChannel(), log);
		    		
		    	}
		    		
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("CallLogPanel.getLogSQLError")); //$NON-NLS-1$
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
		new SearchPanel(null, "title", "en", "GB", null, null).setVisible(true);
	}

}
