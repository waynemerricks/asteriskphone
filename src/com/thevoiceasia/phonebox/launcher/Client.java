package com.thevoiceasia.phonebox.launcher;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import com.thevoiceasia.phonebox.callinput.CallInputPanel;
import com.thevoiceasia.phonebox.calls.CallManagerPanel;
import com.thevoiceasia.phonebox.calls.CallShortcutBar;
import com.thevoiceasia.phonebox.chat.ChatManager;
import com.thevoiceasia.phonebox.chat.ChatWindow;
import com.thevoiceasia.phonebox.database.DatabaseManager;
import com.thevoiceasia.phonebox.database.Settings;

import javax.swing.UIManager.*;

public class Client extends JFrame implements WindowListener{

	//Class Vars
	private ChatManager chatManager;
	private DatabaseManager databaseManager;
	
	private String language, country;
	private boolean hasErrors = false;
	private HashMap<String, String> userSettings;
	private Splash loadingSplash;
	
	//Statics
	private static Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.WARNING;
	private static Logger CHAT_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.chat"); 
	private static final Level CHAT_LOG_LEVEL = Level.WARNING;
	private static Logger DATABASE_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.database"); 
	private static final Level DATABASE_LOG_LEVEL = Level.WARNING;
	private static Logger RECORDS_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.records"); 
	private static final Level RECORDS_LOG_LEVEL = Level.WARNING;
	private static Logger CALL_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.calls"); 
	private static final Level CALL_LOG_LEVEL = Level.INFO;
	private static Logger CALL_INPUT_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.callinput"); 
	private static final Level CALL_INPUT_LEVEL = Level.WARNING;
	
	private static I18NStrings xStrings;
	
	public Client(String language, String country){
		
		super();
		
		//Set Locale
		this.language = language;
		this.country = country;
		
		/** Initialise component daemons **/
		xStrings = new I18NStrings(this.language, this.country);
		this.setIconImage(createImage("images/app.png")); 
		setupLogging();
		
		loadingSplash = new Splash(""); 
		loadingSplash.setVisible(true);
		loadingSplash.setStatus(xStrings.getString("Client.loading")); 
		
		setupManagementObjects();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		if(!hasErrors){
			
			LOGGER.info(xStrings.getString("Client.buildingGUI")); 
			
			/** Build GUI **/
			setLookandFeel();
			this.setSize(1024, 768);
			this.setLayout(new BorderLayout());
			this.setTitle(xStrings.getString("Client.appTitle")); 
			this.addWindowListener(this);
			
			LOGGER.info(xStrings.getString("Client.loadingChatModule")); 
			loadingSplash.setStatus(xStrings.getString("Client.loadingChatModule")); 
			
			//Chat Module
			ChatWindow chat = new ChatWindow(chatManager, this.language, this.country, userSettings.get("nickName"), 
					userSettings.get("isStudio")); 
			
			if(chatManager.hasErrors())
				hasErrors = true;
			else{
				chatManager.startIdleDetectThread();
				
				//GUI For Call Queue
				//CallManager interacts with control room
				LOGGER.info(xStrings.getString("Client.creatingCallManager")); 
				loadingSplash.setStatus(xStrings.getString("Client.creatingCallManager")); 
				
				CallManagerPanel callManagerPanel = new CallManagerPanel(
						databaseManager.getUserSettings(),
						chatManager.getControlChatRoom(), 
						databaseManager, chatManager.getConnection()); 
				
				chatManager.addActionTimeRecorder(callManagerPanel, 
						callManagerPanel.getClass().getName());
				
				JPanel callModule = new JPanel(new BorderLayout());
				callModule.add(new JScrollPane(callManagerPanel), BorderLayout.CENTER);
				CallShortcutBar callShortcuts = new CallShortcutBar(callManagerPanel, 
						this.language, this.country);
				callManagerPanel.addManualHangupListener(callShortcuts);
				callModule.add(callShortcuts, BorderLayout.NORTH);
				
				//this.add(callModule, BorderLayout.CENTER);
				
				callManagerPanel.sendUpdateRequest();
				
				CallInputPanel callInput = new CallInputPanel(
						databaseManager.getReadConnection(), 
						userSettings.get("maxRecordAge"), this.language, this.country, chatManager,  
						userSettings.get("incomingQueueNumber"), 
						userSettings.get("onAirQueueNumber")); 
				callManagerPanel.addAnswerListener(callInput);
				
				//Add CallManagerPanel ref to AddCall on callinputpanel
				callInput.setCallManagerPanel(callManagerPanel);
				
				//Add Client ref to ChangePerson on callinputpanel
				callInput.setClientWindow(this);
				
				JSplitPane chatSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chat, callInput);
				chatSplit.setOneTouchExpandable(true);
				chatSplit.setContinuousLayout(true);
		        //this.add(east, BorderLayout.EAST);
				
				//callModule SplitPane east
				JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, callModule, chatSplit);
				split.setOneTouchExpandable(true);
		        split.setContinuousLayout(true);
				this.add(split, BorderLayout.CENTER);
				loadingSplash.setStatus(xStrings.getString("Client.loadingComplete")); 
				
			}
			
		}
		
	}

	/**
	 * Gets the image from a relative path and creates an icon for use with buttons
	 * @param path path where image resides
	 * @return the image loaded as a Java Icon
	 */
	private Image createImage(String path){
		
		Image icon = null;
		
		URL imgURL = getClass().getResource(path);
		
		if(imgURL != null)
			icon = Toolkit.getDefaultToolkit().createImage(imgURL);
		else{
			
			LOGGER.warning(xStrings.getString("Client.logLoadIconError")); 
			
		}
		
		return icon;
		
	}
	
	/**
	 * Closes the splash screen
	 */
	public void closeLoadingSplash(){
		
		loadingSplash.setVisible(false);
		loadingSplash.close();
		
	}
	
	/**
	 * Internal setup for the manager objects
	 */
	private void setupManagementObjects(){
		
		//Database
		databaseManager = new DatabaseManager(new Settings(), language, country);
		
		if(databaseManager.hasErrors())
			hasErrors = true;
		else{
			
			LOGGER.info(xStrings.getString("Client.connectingToDatabase")); 
			loadingSplash.setStatus(xStrings.getString("Client.connectingToDatabase")); 

			if(databaseManager.connect()){
				
				boolean createUser = !databaseManager.populateUserSettings(null);
				userSettings = databaseManager.getUserSettings();
				
				//BUG FIX rely on DB for locale info where possible
				language = userSettings.get("language"); 
				country = userSettings.get("country"); 
				xStrings = new I18NStrings(language, country);//reset language to user settings
				databaseManager.setNewLocale(language, country);//Set db manager language to above
				
				//Database Keep Alive Thread
				databaseManager.spawnKeepAlive(language, country);
				
				//Chat Connection Manager
				chatManager = new ChatManager(userSettings.get("XMPPLogin") + "@" +   
						userSettings.get("XMPPDomain"),  
						userSettings.get("password"), 
						userSettings.get("nickName"), 
						userSettings.get("XMPPServer"), 
						userSettings.get("XMPPRoom"), 
						userSettings.get("XMPPControlRoom"), 
						userSettings.get("language"), 
						userSettings.get("country"), 
						Integer.parseInt(userSettings.get("idleTimeout"))); 
				
				if(chatManager.hasErrors())
					hasErrors = true;
				else{
					
					if(createUser){
						
						LOGGER.info(xStrings.getString("Client.creatingXMPPUser")); 
						loadingSplash.setStatus(xStrings.getString("Client.creatingXMPPUser")); 
						
						if(!chatManager.createUser())
							hasErrors = true;
						
					}
					
				}
				
			}else
				hasErrors = true;
			
		}
		
	}
	
	/**
	 * Sets the nimbus L&F for Client.lookAndFeel in strings.properties to specify 
	 * others
	 */
	private void setLookandFeel(){
		
		// Set preferred L&F
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if (xStrings.getString("Client.lookAndFeel").equals(info.getName())) { 
		            UIManager.setLookAndFeel(info.getClassName());
		            break;
		        }
		    }
		} catch (Exception e) {
		    // Will use default L&F at this point, don't really care which it is
		}
		
	}
	
	private boolean hasErrors(){
		
		return hasErrors;
		
	}
	
	public static void main(String[] args){
		
		if(args.length == 2){
			
			I18NStrings xStrings = new I18NStrings(args[0], args[1]);
			Client phonebox = new Client(args[0], args[1]);
		
			if(!phonebox.hasErrors()){
				phonebox.setVisible(true);
				phonebox.closeLoadingSplash();
			}else{
				
				Exception e = new Exception(xStrings.getString("Client.onLoadError")); 

				System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); 
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
				LOGGER.severe(e.getMessage());
				phonebox.closeLoadingSplash();
				
				System.exit(1);
				
			}
			
		}else if (args.length == 0){
			
			I18NStrings xStrings = new I18NStrings("en", "GB");  
			Client phonebox = new Client("en", "GB");  
		
			if(!phonebox.hasErrors()){
				phonebox.setVisible(true);
				phonebox.closeLoadingSplash();
			}else{
				
				Exception e = new Exception(xStrings.getString("Client.onLoadError")); 

				System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); 
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
				LOGGER.severe(e.getMessage());
				phonebox.closeLoadingSplash();
				
				System.exit(1);
				
			}
			
		}else{
			
			I18NStrings xStrings = new I18NStrings("en", "GB");  
			Exception e = new Exception(xStrings.getString("Client.usageError")); 

			System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); 
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); 
			LOGGER.severe(e.getMessage());
			
			System.exit(1);
			
		}
			
		
	}
	
	/**
	 * Set the Logger object
	 * 
	 */
	private void setupLogging(){
		
		LOGGER.setLevel(LOG_LEVEL);
		CHAT_LOGGER.setLevel(CHAT_LOG_LEVEL);
		DATABASE_LOGGER.setLevel(DATABASE_LOG_LEVEL);
		RECORDS_LOGGER.setLevel(RECORDS_LOG_LEVEL);
		CALL_LOGGER.setLevel(CALL_LOG_LEVEL);
		CALL_INPUT_LOGGER.setLevel(CALL_INPUT_LEVEL);
		
		LOGGER.info(xStrings.getString("Client.logSetupLogging")); 
		
		try{
			LOGGER.addHandler(new FileHandler("tvapb.log")); 
			CHAT_LOGGER = LOGGER;//.addHandler(new FileHandler("tvapb.log")); 
			DATABASE_LOGGER = LOGGER;//.addHandler(new FileHandler("tvapb.log")); 
			RECORDS_LOGGER = LOGGER;//.addHandler(new FileHandler("tvapb.log")); 
			CALL_LOGGER = LOGGER;//.addHandler(new FileHandler("tvapb.log")); 
			CALL_INPUT_LOGGER = LOGGER;//.addHandler(new FileHandler("tvapb.log")); 
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("Client.loggerCreateError")); 
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Client.logErrorPrefix") + friendlyErrorMessage); 
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("Client.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); 
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	@Override
	public void windowClosing(WindowEvent arg0) {
		
		LOGGER.info(xStrings.getString("Client.appTitle") + " " +   
				xStrings.getString("Client.logApplicationClosing")); 
		
		//Clean up and free connections/memory
		try{
			chatManager.disconnect();
			databaseManager.disconnect();
		}catch(IllegalStateException e){
			/*
			 * This only happens if program was logged in twice as the same user,
			 * in this case the older instance gets kicked from the server.
			 * 
			 * As we're closing we can ignore the error and continue to shutdown.
			 */
		}
		
		chatManager = null;
		databaseManager = null;
		
	}

	//Unneccessary Window Events
	@Override
	public void windowActivated(WindowEvent arg0) {}
	@Override
	public void windowClosed(WindowEvent arg0) {}
	@Override
	public void windowDeactivated(WindowEvent arg0) {}
	@Override
	public void windowDeiconified(WindowEvent arg0) {}
	@Override
	public void windowIconified(WindowEvent arg0) {}
	@Override
	public void windowOpened(WindowEvent arg0) {}
	
	private static final long serialVersionUID = 1L;
	
}