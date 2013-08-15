package com.thevoiceasia.phonebox.launcher;

import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import com.thevoiceasia.phonebox.calls.CallManagerPanel;
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
	
	//Statics
	private static final Logger LOGGER = Logger.getLogger(Client.class.getName());//Logger
	private static final Level LOG_LEVEL = Level.INFO;
	private static final Logger CHAT_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.chat"); //$NON-NLS-1$
	private static final Level CHAT_LOG_LEVEL = Level.WARNING;
	private static final Logger DATABASE_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.database"); //$NON-NLS-1$
	private static final Level DATABASE_LOG_LEVEL = Level.WARNING;
	private static final Logger RECORDS_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.records"); //$NON-NLS-1$
	private static final Level RECORDS_LOG_LEVEL = Level.INFO;
	private static final Logger CALL_LOGGER = Logger.getLogger("com.thevoiceasia.phonebox.calls"); //$NON-NLS-1$
	private static final Level CALL_LOG_LEVEL = Level.INFO;
	
	private static I18NStrings xStrings;
	
	public Client(String language, String country){
		
		super();
		
		//Set Locale
		this.language = language;
		this.country = country;
		
		/** Initialise component daemons **/
		xStrings = new I18NStrings(language, country);
		setupLogging();
		setupManagementObjects();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		if(!hasErrors){
			
			LOGGER.info(xStrings.getString("Client.buildingGUI")); //$NON-NLS-1$
			
			/** Build GUI **/
			setLookandFeel();
			this.setSize(1024, 768);
			this.setLayout(new BorderLayout());
			this.setTitle(xStrings.getString("Client.appTitle")); //$NON-NLS-1$
			this.addWindowListener(this);
			
			LOGGER.info(xStrings.getString("Client.loadingChatModule")); //$NON-NLS-1$
			
			//Chat Module
			this.add(new ChatWindow(chatManager, language, country, userSettings.get("nickName"), //$NON-NLS-1$
					userSettings.get("isStudio")), BorderLayout.EAST); //$NON-NLS-1$ 
			
			if(chatManager.hasErrors())
				hasErrors = true;
			else{
				chatManager.startIdleDetectThread();
				
				//GUI For Call Queue
				//TODO
				//CallManager interacts with control room
				LOGGER.info(xStrings.getString("Client.creatingCallManager")); //$NON-NLS-1$
				
				this.add(new CallManagerPanel(language, country,
						chatManager.getControlChatRoom()), BorderLayout.CENTER);
				
				//GUI For Call Input
				//TODO
				
			}
			
		}
		
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
			
			LOGGER.info(xStrings.getString("Client.connectingToDatabase")); //$NON-NLS-1$
			if(databaseManager.connect()){
				
				boolean createUser = !databaseManager.populateUserSettings();
				userSettings = databaseManager.getUserSettings();
				
				//Chat Connection Manager
				chatManager = new ChatManager(userSettings.get("XMPPLogin") + "@" +  //$NON-NLS-1$ //$NON-NLS-2$
						userSettings.get("XMPPDomain"), //$NON-NLS-1$ 
						userSettings.get("password"), //$NON-NLS-1$
						userSettings.get("nickName"), //$NON-NLS-1$
						userSettings.get("XMPPServer"), //$NON-NLS-1$
						userSettings.get("XMPPRoom"), //$NON-NLS-1$
						userSettings.get("XMPPControlRoom"), //$NON-NLS-1$
						userSettings.get("language"), //$NON-NLS-1$
						userSettings.get("country"), //$NON-NLS-1$
						Integer.parseInt(userSettings.get("idleTimeout"))); //$NON-NLS-1$
				
				if(chatManager.hasErrors())
					hasErrors = true;
				else{
					
					if(createUser){
						
						LOGGER.info(xStrings.getString("Client.creatingXMPPUser")); //$NON-NLS-1$
						
						if(!chatManager.createUser())
							hasErrors = true;
						
					}
					
				}
				
			}else
				hasErrors = true;
			
		}
		
	}
	
	private void setLookandFeel(){
		
		// Set preferred L&F
		try {
		    for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
		        if (xStrings.getString("Client.lookAndFeel").equals(info.getName())) { //$NON-NLS-1$
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
		
			if(!phonebox.hasErrors())
				phonebox.setVisible(true);
			else{
				
				Exception e = new Exception(xStrings.getString("Client.onLoadError")); //$NON-NLS-1$

				System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				LOGGER.severe(e.getMessage());
				
				System.exit(1);
				
			}
			
		}else if (args.length == 0){
			
			I18NStrings xStrings = new I18NStrings("en", "GB");  //$NON-NLS-1$//$NON-NLS-2$
			Client phonebox = new Client("en", "GB"); //$NON-NLS-1$ //$NON-NLS-2$
		
			if(!phonebox.hasErrors())
				phonebox.setVisible(true);
			else{
				
				Exception e = new Exception(xStrings.getString("Client.onLoadError")); //$NON-NLS-1$

				System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); //$NON-NLS-1$
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
				LOGGER.severe(e.getMessage());
				
				System.exit(1);
				
			}
			
		}else{
			
			I18NStrings xStrings = new I18NStrings("en", "GB");  //$NON-NLS-1$//$NON-NLS-2$
			Exception e = new Exception(xStrings.getString("Client.usageError")); //$NON-NLS-1$

			System.err.println(xStrings.getString("Client.logErrorPrefix") + e.getMessage()); //$NON-NLS-1$
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, e.getMessage(), xStrings.getString("Client.errorBoxTitle"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
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
		
		LOGGER.info(xStrings.getString("Client.logSetupLogging")); //$NON-NLS-1$
		
		try{
			LOGGER.addHandler(new FileHandler("clientLog.log")); //$NON-NLS-1$
			CHAT_LOGGER.addHandler(new FileHandler("chatLog.log")); //$NON-NLS-1$
			DATABASE_LOGGER.addHandler(new FileHandler("databaseLog.log")); //$NON-NLS-1$
			RECORDS_LOGGER.addHandler(new FileHandler("recordsLog.log")); //$NON-NLS-1$
			CALL_LOGGER.addHandler(new FileHandler("callLog.log")); //$NON-NLS-1$
		}catch(IOException e){
			
			e.printStackTrace();
			showWarning(e, xStrings.getString("Client.loggerCreateError")); //$NON-NLS-1$
			
		}
		
	}
	
	/**
	 * Logs a warning message and displays friendly message to user
	 * @param e
	 * @param friendlyErrorMessage
	 */
	private void showWarning(Exception e, String friendlyErrorMessage){
		
		System.err.println(xStrings.getString("Client.logErrorPrefix") + friendlyErrorMessage); //$NON-NLS-1$
		e.printStackTrace();
		JOptionPane.showMessageDialog(null, friendlyErrorMessage, xStrings.getString("Client.errorBoxTitle"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
		LOGGER.warning(friendlyErrorMessage);
		
	}
	
	@Override
	public void windowClosing(WindowEvent arg0) {
		
		LOGGER.info(xStrings.getString("Client.appTitle") + " " +  //$NON-NLS-1$ //$NON-NLS-2$
				xStrings.getString("Client.logApplicationClosing")); //$NON-NLS-1$
		
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