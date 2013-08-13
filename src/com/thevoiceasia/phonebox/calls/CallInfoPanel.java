package com.thevoiceasia.phonebox.calls;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

public class CallInfoPanel extends JPanel{

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	public static final int ALERT_OK = 0;
	public static final int ALERT_FAVOURITE = 1;
	public static final int ALERT_WARNING = 2;
	public static final int ALERT_BANNED = 3;
	private static final String[] ALERT_ICONS = {"OptionPane.questionIcon", //$NON-NLS-1$
		"OptionPane.informationIcon", "OptionPane.warningIcon", "OptionPane.errorIcon"};  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	/* Modes:
	 * 1 = Ringing
	 * 2 = Answered
	 * 3 = Answered Elsewhere
	 * 4 = Queued
	 * 5 = On Air
	 */
	public static final int MODE_RINGING = 1;
	public static final int MODE_ANSWERED = 2;
	public static final int MODE_ANSWERED_ELSEWHERE = 3;
	public static final int MODE_QUEUED = 4;
	public static final int MODE_ON_AIR = 5;
	
	private static final Logger LOGGER = Logger.getLogger(CallInfoPanel.class.getName());//Logger
	
	/** CLASS VARS **/
	private I18NStrings xStrings;
	private int mode = MODE_RINGING;
	
	/** GUI SPECIFIC **/
	private TransparentLabel alertIcon, timeLabel, connectedToLabel, nameLabel, locationLabel, 
		conversationLabel;
	
	public CallInfoPanel(String language, String country, String callerName, 
			String callerLocation, String conversation,	int alertLevel){
	
		xStrings = new I18NStrings(language, country);
		
		//Argh the horror!
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		//Alert Icon, 3 rows high
		alertIcon = new TransparentLabel(UIManager.getIcon(ALERT_ICONS[alertLevel]));
		
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 3;
		c.weightx = 0.3;
		c.weighty = 0.25;
		c.fill = GridBagConstraints.BOTH;
		
		this.add(alertIcon, c); //Add Alert to Panel, 3 rows high
		
		//Timer Label 1 row/column
		timeLabel = new TransparentLabel(xStrings.getString("CallInfoPanel.callTimeInit")); //$NON-NLS-1$
		
		c.weightx = 0.25;
		c.gridx = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;
		//TODO Subclass to update time
		this.add(timeLabel, c);
		
		//Connected To Label, will be blank by default, fill row
		connectedToLabel = new TransparentLabel(" ", TransparentLabel.CENTER); //$NON-NLS-1$
		
		c.weightx = 1;
		c.gridx = 2;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH;
		
		this.add(connectedToLabel, c);
		
		//Name Label, 2nd Row, fill horizontal
		nameLabel = new TransparentLabel(callerName, TransparentLabel.CENTER);
		
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 4;
		
		this.add(nameLabel, c);
		
		//Location Label, 3rd Row, fill horizontal
		locationLabel = new TransparentLabel(callerLocation, TransparentLabel.CENTER);
		
		c.gridx = 1;
		c.gridy = 2;
		
		this.add(locationLabel, c);
		
		//Conversation Label, 4th Row, fill height/horiz
		conversationLabel = new TransparentLabel(conversation);
		
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 1;
		c.gridwidth = 6;
		c.fill = GridBagConstraints.BOTH;
		
		this.add(conversationLabel, c);
		
		this.setBackground(Color.BLUE);
		//TODO Ringing thread flash
		//TODO When this is clicked it should reset active timer and send control message
		//TODO Mode for click operation answer, hangup, queue etc
		//
		
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
			
			LOGGER.warning(xStrings.getString("ChatShortcutBar.logLoadIconError")); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}
	
	public static void main(String[] args){
		
		JFrame win = new JFrame("Test");
		win.setSize(450, 200);
		win.setLayout(new BorderLayout());
		win.add(new CallInfoPanel("en", "GB", "Wayne Merricks", 
				"United Kingdom", "Pray for his goat",	ALERT_OK));
		win.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		win.setVisible(true);
		
	}
	
}
