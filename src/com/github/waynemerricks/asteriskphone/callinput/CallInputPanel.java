package com.github.waynemerricks.asteriskphone.callinput;

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

import com.github.waynemerricks.asteriskphone.calls.AnswerListener;
import com.github.waynemerricks.asteriskphone.calls.CallInfoPanel;
import com.github.waynemerricks.asteriskphone.calls.CallManagerPanel;
import com.github.waynemerricks.asteriskphone.chat.ChatManager;
import com.github.waynemerricks.asteriskphone.launcher.Client;
import com.github.waynemerricks.asteriskphone.records.Person;

import net.miginfocom.swing.MigLayout;

public class CallInputPanel extends JTabbedPane implements AnswerListener, PersonChangedListener{

	/* CLASS VARS */
	private String language, country;
	private Connection databaseReadConnection;
	private Vector<CallInputField> components = new Vector<CallInputField>();
	private I18NStrings xStrings;
	private boolean hasErrors = false;
	private CallInfoPanel currentPanel = null;
	private CallerHistoryPanel historyPanel = null;
	private CallLogPanel callLogPanel = null;
	private long maxRecordAge = 3600000L;
	private String incomingQueue = null, onairQueue = null;
	private SearchPanel searchPanel = null;
	private ChatManager chat = null;
	private JButton changePerson = null, addCall = null;
	
	/** STATICS **/
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallInputPanel.class.getName());//Logger
	
	public CallInputPanel(Connection readConnection,  
			String maxRecordAge, String language, String country, ChatManager manager, 
			String incomingQueue, String onairQueue) {
		
		super(JTabbedPane.BOTTOM);

		this.chat = manager;
		this.language = language;
		this.country = country;
		this.incomingQueue = incomingQueue;
		this.onairQueue = onairQueue;
		
		xStrings = new I18NStrings(language, country);
		
		try{
			
			this.maxRecordAge = Long.parseLong(maxRecordAge);
			
		}catch(NumberFormatException e){

			this.maxRecordAge = 360000L;
			LOGGER.warning(xStrings.getString("CallInputPanel.maxRecordAgeInvalid")); 
			
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
						new MigLayout("fillx"), components.get(i).mapping); 
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
							"growx, spanx, wrap"); 
					
				}else if(components.get(i).isCombo()){// || components.get(i).isTextField()
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); 
					
					//Checks for special components e.g. alert box updater etc
					if(components.get(i).mapping != null &&
							components.get(i).mapping.equals("alert")){ 
						
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
							components.get(i).mapping.equals("calltype")){ 
					
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
											((String)evt.getItem()).replace("/", "^^%%$$"), 
											true);
									
								}
								
							}
							
						});
						
					}
					
				}else if(components.get(i).isTextArea()){
					
					if(components.get(i).getLabel() != null)
						tabHash.get(components.get(i).parent).add(components.get(i).getLabel());
					
					tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); 
					//Conversation
					if(components.get(i).mapping != null &&
							components.get(i).mapping.equals("conversation")){ 
						
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
											txt.getText().replace("/", "^^%%$$"), 
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
					
					if(components.get(i).mapping != null && !components.get(i).mapping.equals("name")) 
						tabHash.get(components.get(i).parent).add(components.get(i).getComponent(),
							"growx, spanx, wrap"); 
					
					//Name Field
					if(components.get(i).mapping != null && 
							components.get(i).mapping.equals("name")){ 
						
						//Add the text field without the wrap and then add a button
						tabHash.get(components.get(i).parent).add(components.get(i).getComponent(), 
								"spanx, growx, split 3"); 
						
						//Button to change the person on the call usually for withheld numbers or
						//one number in use by more than one person
						changePerson = new JButton(
								xStrings.getString("CallInputPanel.changePerson")); 
						tabHash.get(components.get(i).parent).add(changePerson);  
						
						//Button to add a manual Call
						addCall = new JButton(xStrings.getString("CallInputPanel.addCall"));
						//addCall.setEnabled(false);
						tabHash.get(components.get(i).parent).add(addCall, "wrap");  
						
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
							components.get(i).mapping.equals("location")){ 
						
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
											txt.getText().replace("/", "^^%%$$"),   
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
			
			if(tab.mapping != null && tab.mapping.equals("history")){ 
				
				//Special case for the history tab so create caller history here
				historyPanel = new CallerHistoryPanel(language, country);
				tab.add(historyPanel.getTable().getTableHeader(), "growx, spanx, wrap"); 
				tab.add(historyPanel.getTable(), "growx, spanx, wrap"); 
				
			}else if(tab.mapping != null && tab.mapping.equals("calllog")){ 
				
				LOGGER.info(xStrings.getString("CallInputPanel.creatingCallLogPanel")); 
				//Special case for the call log tab
				callLogPanel = new CallLogPanel(databaseReadConnection, 
						this.maxRecordAge, language, country, manager, incomingQueue, onairQueue);
				tab.add(callLogPanel.getTable().getTableHeader(), "growx, spanx, wrap"); 
				tab.add(callLogPanel.getTable(), "growx, spanx, wrap"); 
				
			}
			
			this.addTab(tab.name, new JScrollPane(tab, 
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));	
			
		}
		
		
	}
	
	/**
	 * Gives this input panel a client window to work with.  Used to attach
	 * dialogs to the parent window
	 * @param client
	 */
	public void setClientWindow(Client client){
		
		final Client ownerFinal = client;
		final CallInputPanel cip = this;
		
		changePerson.addActionListener(new ActionListener(){
			
			public void actionPerformed(ActionEvent evt){
				
				if(currentPanel != null)
					if(searchPanel == null){
					
						//Get the Parent JFrame so that SearchPanel can have that as the owner
						searchPanel = new SearchPanel(ownerFinal, 
								xStrings.getString("SearchPanel.title"),  
								language, country, 
								databaseReadConnection, 
								getPhoneNumber(currentPanel.getChannelID()));
						
						searchPanel.addPersonChangedListener(cip);
						searchPanel.setVisible(true);
						
						
					}else{
						//Reuse existing panel but reset number and panel
						//we need to notify when theres a change
						searchPanel.setNumber(
								getPhoneNumber(
										currentPanel.getChannelID()));
						searchPanel.addPersonChangedListener(cip);
						searchPanel.setVisible(true);
					}
				
			}

		});
		
	}
	
	/**
	 * Sets an active CallManagerPanel for this object
	 * This is required by AddCall in order to create a call panel
	 * There may be other uses in the future.
	 * @param manager
	 */
	public void setCallManagerPanel(CallManagerPanel manager){
		
		final CallManagerPanel finalManager = manager;
		
		addCall.addActionListener(new ActionListener(){
			
			public void actionPerformed(ActionEvent evt){
				
				finalManager.addManualCall();
				addCall.setEnabled(false);
				
			}

		});
	}
	
	/**
	 * Gets the phone number from the given channel id
	 * @param channelID
	 * @return
	 */
	private String getPhoneNumber(String channelID) {
		
		String number = null;
		Statement statement = null;
		ResultSet resultSet = null;
		
		try{
			
			String SQL = "SELECT phonenumber FROM callhistory WHERE callchannel = '" + channelID + "' LIMIT 1";  
			statement = databaseReadConnection.createStatement();
			resultSet = statement.executeQuery(SQL);
			
			while(resultSet.next())
				number = resultSet.getString("phonenumber"); 
			
		}catch(SQLException e){
			showError(e, xStrings.getString("CallInputPanel.getNumberSQLError")); 
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
		
		return number;
		
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
			
			String SQL = "SELECT * FROM callinputfields WHERE language = '" + language +  
					"," + country + "' ORDER BY `order` ASC";  
			
			statement = databaseReadConnection.createStatement();
		    resultSet = statement.executeQuery(SQL);
		    
		    while(resultSet.next()){
		    	
		    	//int id, String name, String type, String tooltip, int order, 
				//int parent, String options
		    	components.add(new CallInputField(resultSet.getInt("id"), 
		    			resultSet.getString("name"), 
		    			resultSet.getString("type"), 
		    			resultSet.getString("tooltip"), 
		    			resultSet.getInt("order"), 
		    			resultSet.getInt("parent"), 
		    			resultSet.getString("options"), 
		    			resultSet.getString("mapping"))); 
		    	
		    }
		    
		    gotSettings = true;
		    
		}catch (SQLException e){
			showError(e, xStrings.getString("CallInputPanel.getComponentsSQLError")); 
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
		
		System.err.println(xStrings.getString("CallInputPanel.logErrorPrefix") + 
				friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, 
				xStrings.getString("CallInputPanel.errorBoxTitle"), 
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
	public void manualCallEnded() {
		
	  addCall.setEnabled(true);	
		
	}
	
	
	@Override
	public void callAnswered(CallInfoPanel call) {
		
		LOGGER.info(xStrings.getString("CallInputPanel.answeredCall") + ": " +  
				call.getPhoneCallRecord().getActivePerson().name); 
		
		/* This panel has been answered so we need to update the fields as necessary */
		if(call != null){
			
			currentPanel = null; //Set to null so update listeners won't activate on an 
			//old panel
			setFieldValue("alert", call.getPhoneCallRecord().getActivePerson().alert); 
			setFieldValue("calltype", call.getPhoneCallRecord().getCallType()); 
			setFieldValue("gender", call.getPhoneCallRecord().getActivePerson().gender); 
			setFieldValue("name", call.getPhoneCallRecord().getActivePerson().name); 
			setFieldValue("location", call.getPhoneCallRecord().getActivePerson().location); 
			setFieldValue("address", 
					call.getPhoneCallRecord().getActivePerson().postalAddress); 
			setFieldValue("postcode", call.getPhoneCallRecord().getActivePerson().postCode); 
			setFieldValue("email", call.getPhoneCallRecord().getActivePerson().email); 
			setFieldValue("language", call.getPhoneCallRecord().getActivePerson().language); 
			setFieldValue("religion", call.getPhoneCallRecord().getActivePerson().religion); 
			setFieldValue("journey", call.getPhoneCallRecord().getActivePerson().journey); 
			setFieldValue("conversation", 
					call.getPhoneCallRecord().getActivePerson().currentConversation); 
			setFieldValue("notes", call.getPhoneCallRecord().getActivePerson().notes); 
			
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
		
		/* Send XMPP Requesting person change
		 * null = new person otherwise change to existing
		 */
		
		if(changedTo.isNewPerson())
			chat.sendMessage(xStrings.getString("CallInputPanel.ChangeToNewPerson") + 
					"/" + currentPanel.getChannelID() + "/" + changedTo.number, 
					true); 
		else
			chat.sendMessage(xStrings.getString("CallInputPanel.ChangeToExistingPerson") + 
					"/" + currentPanel.getChannelID() + 
					"/" + changedTo.id, true); 
		
	}

}
