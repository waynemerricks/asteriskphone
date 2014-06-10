package com.thevoiceasia.phonebox.callinput;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import com.thevoiceasia.phonebox.calls.AnswerListener;
import com.thevoiceasia.phonebox.calls.CallInfoPanel;
import com.thevoiceasia.phonebox.chat.ChatManager;
import com.thevoiceasia.phonebox.records.Person;

import net.miginfocom.swing.MigLayout;

public class CallInputPanel extends JTabbedPane implements AnswerListener, PersonChangedListener{

	/* CLASS VARS */
	private String language, country;
	private Connection databaseReadConnection, databaseWriteConnection;
	private Vector<CallInputField> components = new Vector<CallInputField>();
	private I18NStrings xStrings;
	private boolean hasErrors = false;
	private CallInfoPanel currentPanel = null;
	private CallerHistoryPanel historyPanel = null;
	private CallLogPanel callLogPanel = null;
	private long maxRecordAge = 3600000L;
	private String incomingQueue = null, onairQueue = null;
	private SearchPanel searchPanel = null;
	
	/** STATICS **/
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallInputPanel.class.getName());//Logger
	
	public CallInputPanel(Connection readConnection,  
			String maxRecordAge, String language, String country, ChatManager manager, 
			String incomingQueue, String onairQueue) {
		
		super(JTabbedPane.BOTTOM);
		
		this.language = language;
		this.country = country;
		this.incomingQueue = incomingQueue;
		this.onairQueue = onairQueue;
		
		xStrings = new I18NStrings(language, country);
		
		try{
			
			this.maxRecordAge = Long.parseLong(maxRecordAge);
			
		}catch(NumberFormatException e){

			this.maxRecordAge = 360000L;
			LOGGER.warning(xStrings.getString("CallInputPanel.maxRecordAgeInvalid")); //$NON-NLS-1$
			
		}
		
		
		databaseReadConnection = readConnection;
		
		//Read components from DB
		if(getComponentDetails())
			//Create the JTabbedPane
			createTabbedPane(manager);
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
	 * @param manager 
	 */
	private void createTabbedPane(ChatManager manager){
		
		Vector<TabPanel> tabs = new Vector<TabPanel>();
		HashMap<Integer, TabPanel> tabHash = new HashMap<Integer, TabPanel>();
		
		//Find the parent tabs
		for(int i = 0; i < components.size(); i++){
		
			if(components.get(i).isTab()){
				
				TabPanel tab = new TabPanel(components.get(i).id, components.get(i).name, 
						new MigLayout("fillx"), components.get(i).mapping); //$NON-NLS-1$
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
					
				}else if(components.get(i).isCombo()){// || components.get(i).isTextField()
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					
					//Checks for special components e.g. alert box updater etc
					if(components.get(i).mapping != null &&
							components.get(i).mapping.equals("alert")){ //$NON-NLS-1$
						
						//Add a Listener to update the alert levels
						components.get(i).addItemListener(new ItemListener(){

							@Override
							public void itemStateChanged(ItemEvent evt) {
								
								//Update the alert icon as appropriate
								if(currentPanel != null){
									
									ComboField field = (ComboField)evt.getSource();
									
									try{
										
										int alert = Integer.parseInt(
												field.getItemMapping((String)evt.getItem()));
										
										currentPanel.setAlertLevel((String)evt.getItem(), 
												alert, true);
										
									}catch(NumberFormatException e){
										
										//Not an int so must be a custom image
										currentPanel.setAlertLevel((String)evt.getItem(),
												field.getItemMapping(
												(String)evt.getItem()), true);
										
									}
									
								}
								
							}
							
						});
					//Call Type e.g. On Air/Off Air
					}else if(components.get(i).mapping != null && 
							components.get(i).mapping.equals("calltype")){ //$NON-NLS-1$
					
						//Add a Listener to update the alert levels
						components.get(i).addItemListener(new ItemListener(){

							@Override
							public void itemStateChanged(ItemEvent evt) {
								
								//Update the alert icon as appropriate
								if(currentPanel != null){
									
									ComboField field = (ComboField)evt.getSource();
									
									String map = field.getItemMapping((String)evt.getItem());
									currentPanel.setBadgeIcon((String)evt.getItem(), map, true);
									
								}
								
							}
							
						});
						
					}else if(components.get(i).mapping != null){
						
						//Custom Field
						//Add a Listener to update the custom field
						components.get(i).addItemListener(new ItemListener(){

							@Override
							public void itemStateChanged(ItemEvent evt) {
								
								//Update the alert icon as appropriate
								if(currentPanel != null){
									
									ComboField field = (ComboField)evt.getSource();
									
									currentPanel.setPhoneCallField(
											field.getFieldMapping(), 
											((String)evt.getItem()).replace("/", "^^%%$$"),  //$NON-NLS-1$ //$NON-NLS-2$, 
											true);
									
								}
								
							}
							
						});
						
					}
					
				}else if(components.get(i).isTextArea()){
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					//Conversation
					if(components.get(i).mapping != null &&
							components.get(i).mapping.equals("conversation")){ //$NON-NLS-1$
						
						components.get(i).addKeyListener(new KeyListener(){

							@Override
							public void keyReleased(KeyEvent evt) {
								
								if(currentPanel != null){
									
									//Get the text area inside the JScrollPane
									JTextArea txt = (JTextArea)evt.getSource();
									currentPanel.setConversation(txt.getText(), true);
									
								}
								
							}
							
							@Override
							public void keyTyped(KeyEvent evt) {}
							@Override
							public void keyPressed(KeyEvent evt) {}
							
						});
						
					}else if(components.get(i).mapping != null){
						
						//Custom Field
						components.get(i).addKeyListener(new KeyListener(){
							
							@Override
							public void keyReleased(KeyEvent evt) {
								
								if(currentPanel != null){
									
									//Get the text area
									AreaField txt = (AreaField)evt.getSource();
									
									currentPanel.setPhoneCallField(
											txt.getFieldMapping(), 
											txt.getText().replace("/", "^^%%$$"),  //$NON-NLS-1$ //$NON-NLS-2$, 
											true);
									
								}
								
							}
							
							@Override
							public void keyTyped(KeyEvent evt) {}
							@Override
							public void keyPressed(KeyEvent evt) {}
							
						});
						
					}
					
				}else if(components.get(i).isTextField()){
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					if(components.get(i).mapping != null && !components.get(i).mapping.equals("name")) //$NON-NLS-1$
						tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); //$NON-NLS-1$
					
					//Name Field
					if(components.get(i).mapping != null && 
							components.get(i).mapping.equals("name")){ //$NON-NLS-1$
						
						//Add the text field without the wrap and then add a button
						tabHash.get(components.get(i).parent).add(components.get(i).getComponent(), 
								"spanx, growx, split 2"); //$NON-NLS-1$
						
						//Button to change the person on the call usually for withheld numbers or
						//one number in use by more than one person
						JButton changePerson = new JButton(
								xStrings.getString("CallInputPanel.changePerson")); //$NON-NLS-1$
						
						final Component owner = this.getParent();
						final CallInputPanel cip = this;
						
						changePerson.addActionListener(new ActionListener(){
							
							public void actionPerformed(ActionEvent evt){
								
								if(searchPanel == null){
									
									searchPanel = new SearchPanel(owner, 
											xStrings.getString("SearchPanel.title"),  //$NON-NLS-1$
											language, country, 
											databaseReadConnection, databaseWriteConnection, null);//TODO Get number we're using to search with
									searchPanel.getSelectedPerson();
									searchPanel.addPersonChangedListener(cip);
									searchPanel.setVisible(true);
									
								}
								
							}
							
						});
						
						tabHash.get(components.get(i).parent).add(changePerson, "wrap");  //$NON-NLS-1$
						
						components.get(i).addKeyListener(new KeyListener(){

							@Override
							public void keyReleased(KeyEvent evt) {
								
								if(currentPanel != null){
									
									//Get the text area inside the JScrollPane
									JTextField txt = (JTextField)evt.getSource();
									currentPanel.setCallerName(txt.getText(), true);
									
								}
								
							}
							
							@Override
							public void keyTyped(KeyEvent evt) {}
							@Override
							public void keyPressed(KeyEvent evt) {}
							
						});
						
					}else if(components.get(i).mapping != null && 
							components.get(i).mapping.equals("location")){ //$NON-NLS-1$
						
						components.get(i).addKeyListener(new KeyListener(){

							@Override
							public void keyReleased(KeyEvent evt) {
								
								if(currentPanel != null){
									
									//Get the text area inside the JScrollPane
									JTextField txt = (JTextField)evt.getSource();
									currentPanel.setCallerLocation(txt.getText(), true);
									
								}
								
							}
							
							@Override
							public void keyTyped(KeyEvent evt) {}
							@Override
							public void keyPressed(KeyEvent evt) {}
							
						});
						
					}else if(components.get(i).mapping != null){
						
						//Custom TextField
						components.get(i).addKeyListener(new KeyListener(){

							@Override
							public void keyReleased(KeyEvent evt) {
								
								if(currentPanel != null){
									
									//Get the text field
									TextField txt = (TextField)evt.getSource();
									currentPanel.setPhoneCallField(
											txt.getFieldMapping(), 
											txt.getText().replace("/", "^^%%$$"),  //$NON-NLS-1$ //$NON-NLS-2$
											true);
									
								}
								
							}
							
							@Override
							public void keyTyped(KeyEvent evt) {}
							@Override
							public void keyPressed(KeyEvent evt) {}
							
						});
						
					}
					
					
				}
				
			}
			
		}
		
		//Add the tabs to the panel
		for(int i = 0; i < tabs.size(); i++){
			
			TabPanel tab = tabs.get(i);
			
			if(tab.mapping != null && tab.mapping.equals("history")){ //$NON-NLS-1$
				
				//Special case for the history tab so create caller history here
				historyPanel = new CallerHistoryPanel(language, country);
				tab.add(historyPanel.getTable().getTableHeader(), "growx, spanx, wrap"); //$NON-NLS-1$
				tab.add(historyPanel.getTable(), "growx, spanx, wrap"); //$NON-NLS-1$
				
			}else if(tab.mapping != null && tab.mapping.equals("calllog")){ //$NON-NLS-1$
				
				LOGGER.info(xStrings.getString("CallInputPanel.creatingCallLogPanel")); //$NON-NLS-1$
				//Special case for the call log tab
				callLogPanel = new CallLogPanel(databaseReadConnection, 
						this.maxRecordAge, language, country, manager, incomingQueue, onairQueue);
				tab.add(callLogPanel.getTable().getTableHeader(), "growx, spanx, wrap"); //$NON-NLS-1$
				tab.add(callLogPanel.getTable(), "growx, spanx, wrap"); //$NON-NLS-1$
				
			}
			
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
			
			statement = databaseReadConnection.createStatement();
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
		    			resultSet.getString("options"), //$NON-NLS-1$
		    			resultSet.getString("mapping"))); //$NON-NLS-1$
		    	
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
	
	/**
	 * Sets the value of the given field, only works with combo, text area and text field
	 * @param fieldName name of the field to set
	 * @param value value to set it to
	 * @return true if successful or false if field not found
	 */
	private boolean setFieldValue(String fieldName, String value){
		
		boolean set = false;
		
		int i = 0;
		
		while(!set && i < components.size()){
			
			if(components.get(i).getMapping() != null 
					&& components.get(i).getMapping().equals(fieldName)){
				
				if(components.get(i).isCombo()){
					if(value == null || value.trim().length() < 1)
						components.get(i).setSelected(0);
					else
						components.get(i).setSelected(value);
				}else if(components.get(i).isTextArea() || components.get(i).isTextField())
					components.get(i).setText(value);
				
				set = true;
				
			}
			
			i++;
				
		}
		
		return set;
		
	}

	@Override
	public void callAnswered(CallInfoPanel call) {
		
		LOGGER.info(xStrings.getString("CallInputPanel.answeredCall") + //$NON-NLS-1$
				call.getPhoneCallRecord().getActivePerson().name); 
		
		/* This panel has been answered so we need to update the fields as necessary */
		if(call != null){
			
			currentPanel = null; //Set to null so update listeners won't activate on an 
			//old panel
			setFieldValue("alert", call.getPhoneCallRecord().getActivePerson().alert); //$NON-NLS-1$
			setFieldValue("calltype", call.getPhoneCallRecord().getCallType()); //$NON-NLS-1$
			setFieldValue("gender", call.getPhoneCallRecord().getActivePerson().gender); //$NON-NLS-1$
			setFieldValue("name", call.getPhoneCallRecord().getActivePerson().name); //$NON-NLS-1$
			setFieldValue("location", call.getPhoneCallRecord().getActivePerson().location); //$NON-NLS-1$
			setFieldValue("address", //$NON-NLS-1$
					call.getPhoneCallRecord().getActivePerson().postalAddress); 
			setFieldValue("postcode", call.getPhoneCallRecord().getActivePerson().postCode); //$NON-NLS-1$
			setFieldValue("email", call.getPhoneCallRecord().getActivePerson().email); //$NON-NLS-1$
			setFieldValue("language", call.getPhoneCallRecord().getActivePerson().language); //$NON-NLS-1$
			setFieldValue("religion", call.getPhoneCallRecord().getActivePerson().religion); //$NON-NLS-1$
			setFieldValue("journey", call.getPhoneCallRecord().getActivePerson().journey); //$NON-NLS-1$
			setFieldValue("conversation", //$NON-NLS-1$
					call.getPhoneCallRecord().getActivePerson().currentConversation); 
			setFieldValue("notes", call.getPhoneCallRecord().getActivePerson().notes); //$NON-NLS-1$
			
			//TODO Custom Fields
			
			//Set Conversation History
			historyPanel.setConversationHistory(call.getPhoneCallRecord().getActivePerson()
					.getConversationHistory());
			
			//Add to current panel so we can update things
			currentPanel = call;
			
		}
		
	}

	@Override
	public void personChanged(Person changedTo) {
		// TODO Auto-generated method stub
		
	}

}
