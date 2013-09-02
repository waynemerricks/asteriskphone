package com.thevoiceasia.phonebox.callinput;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

public class CallInputPanel extends JTabbedPane {

	/* CLASS VARS */
	private String language, country;
	private Connection databaseConnection;
	private Vector<CallInputField> components = new Vector<CallInputField>();
	private I18NStrings xStrings;
	private boolean hasErrors = false;
	
	/** STATICS **/
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallInputPanel.class.getName());//Logger
	
	public CallInputPanel(Connection readConnection, String language, String country) {
		
		super(JTabbedPane.RIGHT);
		
		xStrings = new I18NStrings(language, country);
		
		//Read components from DB
		if(getComponentDetails())
			//Create the JTabbedPane
			createTabbedPane();
		else
			hasErrors = true;
		
	}
	
	/**
	 * Flag to show
	 * @return
	 */
	public boolean hasErrors(){
		
		return hasErrors;
		
	}
	
	/**
	 * Creates the actual GUI
	 */
	private void createTabbedPane(){
		
		//TODO
		
		//Find the parent tabs
		for(int i = 0; i < components.size(); i++){
		
			if(components.get(i).isTab())
			
		}
		
	}
	
	/**
	 * Reads the component info from the DB
	 * @return
	 */
	private boolean getComponentDetails(){
		
		Statement statement = null;
		ResultSet resultSet = null;
		boolean gotSettings = false;
		
		try{
			
			String SQL = "SELECT * FROM callinputfields WHERE language = '" + language +  //$NON-NLS-1$
					"," + country + "' ORDER BY order ASC"; //$NON-NLS-1$ //$NON-NLS-2$
			
			statement = databaseConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	//int id, String name, String type, String tooltip, int order, 
				//int parent, String options
		    	components.add(new CallInputField(resultSet.getInt("id"), //$NON-NLS-1$
		    			resultSet.getString("name"), //$NON-NLS-1$
		    			resultSet.getString("type"), //$NON-NLS-1$
		    			resultSet.getString("tooltip"), //$NON-NLS-1$
		    			resultSet.getInt("order"), //$NON-NLS-1$
		    			resultSet.getInt("parent"), //$NON-NLS-1$
		    			resultSet.getString("options"))); //$NON-NLS-1$
		    	
		    	gotSettings = true;
		    	
		    }
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("CallInputPanel.getComponentsSQLError")); //$NON-NLS-1$
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
		
		return gotSettings;
		
	}
	
	/**
	 * Logs an error message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showError(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("CallInputPanel.logErrorPrefix") + //$NON-NLS-1$
				friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, 
				xStrings.getString("CallInputPanel.errorBoxTitle"), //$NON-NLS-1$
				JOptionPane.ERROR_MESSAGE); 
		LOGGER.severe(friendlyErrorMessage);
		
	}

}
