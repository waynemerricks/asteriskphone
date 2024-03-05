package com.github.waynemerricks.asteriskphone.calls;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.github.waynemerricks.asteriskphone.launcher.Client;
import com.github.waynemerricks.asteriskphone.misc.LastActionTimer;

public class CallShortcutBar extends JPanel implements ActionListener, LastActionTimer,
														ManualHangupListener{

	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(CallShortcutBar.class.getName());//Logger
	private static final long serialVersionUID = 1L;
	
	/** CLASS VARS **/
	private I18NStrings xStrings; //Link to external string resources
	private CallManagerPanel callManager;
	private long lastActionTime = new Date().getTime();
	private Cursor dropCursor;
	private boolean dropMode = false;
	
	public CallShortcutBar(CallManagerPanel callManager, String language, String country) {
		
		xStrings = new I18NStrings(language, country);
		
		this.callManager = callManager;
		this.setLayout(new GridLayout(1, 5, 5, 5));
		
		JButton button = new JButton(createImageIcon("images/answer.png", "answer"));  
		button.addActionListener(this);
		button.setActionCommand("answer"); 
		button.setToolTipText(xStrings.getString("CallShortcutBar.answerNextToolTip")); 
		
		this.add(button);
		
		button = new JButton(createImageIcon("images/answerrandom.png", "answerrandom"));  
		button.addActionListener(this);
		button.setActionCommand("answerrandom"); 
		button.setToolTipText(xStrings.getString("CallShortcutBar.answerRandomToolTip")); 
		
		this.add(button);
		
		button = new JButton(createImageIcon("images/drop.png", "drop"));  
		button.addActionListener(this);
		button.setActionCommand("drop"); 
		button.setToolTipText(xStrings.getString("CallShortcutBar.dropToolTip")); 
		
		this.add(button);
		
		this.add(new JLabel(" ")); //SPACER
		
		button = new JButton(createImageIcon("images/dial.png", "dial"));  
		button.addActionListener(this);
		button.setActionCommand("dial"); 
		button.setToolTipText(xStrings.getString("CallShortcutBar.dialToolTip")); 
		
		this.add(button);
		
		loadDropCursor();
		
	}

	/**
	 * Gets the image from a relative path and creates an icon for use with buttons
	 * @param path path where image resides
	 * @param description identifier for this image, for internal use
	 * @return the image loaded as a Java Icon
	 */
	private ImageIcon createImageIcon(String path, String description){
		
		ImageIcon icon = null;
		
		URL imgURL = getClass().getResource(path);
		
		if(imgURL != null)
			icon = new ImageIcon(imgURL, description);
		else{
			
			LOGGER.warning(xStrings.getString("CallShortcutBar.logLoadIconError")); 
			
		}
		
		return icon;
		
	}
	
	/**
	 * Sets the cursor to the drop icon and informs CallManagerPanel
	 */
	private void drop(){
	
		if(!dropMode){
			boolean client = false;
		
			Component parent = getParent();
			
			while(!client){
				
				if(parent instanceof Client)
					client = true;
				else
					parent = parent.getParent();
				
			}
			
			this.setCursor(dropCursor);
			
			if(client)
				parent.setCursor(dropCursor);
			
			callManager.setDropMode(true);
			dropMode = true;
		}else
			setNormalCursor();
		
	}
	
	/**
	 * Sets the cursor back to normal
	 */
	public void setNormalCursor(){
		
		if(dropMode){
			boolean client = false;
			Component parent = getParent();
			
			while(!client){
				
				if(parent instanceof Client)
					client = true;
				else
					parent = parent.getParent();
				
			}
			
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			
			if(client)
				parent.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			
			callManager.setDropMode(false);
			dropMode = false;
			
		}
		
	}
	
	/**
	 * Preloads custom drop cursor and stores for later use
	 */
	private void loadDropCursor(){
		
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		dropCursor = toolkit.createCustomCursor(
				createImageIcon("images/dropcursor.gif", "dropcursor").getImage(),  
				new Point(0, 0),
				"dropCursor");  
		
	}
	
	@Override
	public long getLastActionTime() {
		
		return lastActionTime;
		
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		
		lastActionTime = new Date().getTime();
		
		if(evt.getActionCommand().equals("answer")){ 
			if(dropMode)
				setNormalCursor();
			callManager.answerNext();
		}else if(evt.getActionCommand().equals("answerrandom")){ 
			if(dropMode)
				setNormalCursor();
			callManager.answerRandom();
		}else if(evt.getActionCommand().equals("drop")){ 
			drop();
		}else if(evt.getActionCommand().equals("dial")){ 
			if(dropMode)
				setNormalCursor();
			callManager.dial();
		}
		
	}

	@Override
	public void hangupClicked(String channelID) {
		
		setNormalCursor();
		
	}

}
