package com.thevoiceasia.phonebox.callinput;

import java.awt.Dimension;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;

import net.miginfocom.swing.MigLayout;

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
		
		super(JTabbedPane.BOTTOM);
		
		this.language = language;
		this.country = country;
		
		xStrings = new I18NStrings(language, country);
		databaseConnection = readConnection;
		
		//Read components from DB
		if(getComponentDetails())
			//Create the JTabbedPane
			createTabbedPane();
		else
			hasErrors = true;
		
		this.setPreferredSize(new Dimension(500, 350));
		this.setMinimumSize(new Dimension(300, 350));
		this.setMaximumSize(new Dimension(500, 350));
		
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
		
		Vector<TabPanel> tabs = new Vector<TabPanel>();
		HashMap<Integer, TabPanel> tabHash = new HashMap<Integer, TabPanel>();
		
		//Find the parent tabs
		for(int i = 0; i < components.size(); i++){
		
			if(components.get(i).isTab()){
				
				TabPanel tab = new TabPanel(components.get(i).id, components.get(i).name, 
						new MigLayout("fillx")); //$NON-NLS-1$
				tab.setPreferredSize(new Dimension(400, 350));
				tab.setMinimumSize(new Dimension(200, 350));
				tab.setMaximumSize(new Dimension(400, 350));
				
				tabs.add(tab);
				tabHash.put(tab.id, tab);
				
			}
			
		}
		
		//Place the components
		for(int i = 0; i < components.size(); i++){
			
			if(!components.get(i).isTab()){
				
				if(components.get(i).isLabel()){
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					
				}else if(components.get(i).isCombo() || components.get(i).isTextField()){
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					
				}else if(components.get(i).isTextArea()){
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					
				}
				
			}
			
		}
		
		//Add the tabs to the panel
		for(int i = 0; i < tabs.size(); i++){
			
			TabPanel tab = tabs.get(i);
			
			this.addTab(tab.name, new JScrollPane(tab, 
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));	
			
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
					"," + country + "' ORDER BY `order` ASC"; //$NON-NLS-1$ //$NON-NLS-2$
			
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
		    	
		    }
		    
		    gotSettings = true;
		    
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
