package com.thevoiceasia.phonebox.calls;
import java.awt.Dimension;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;


public class CallIconPanel extends JLayeredPane{

	/** STATICS */
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = Logger.getLogger(CallIconPanel.class.getName());//Logger
	
	/** CLASS VARS */
	private JLabel main, badge;
	private I18NStrings xStrings; //Link to external string resources
	
	/**
	 * Creates a CallIconPanel with the given icons
	 * @param mainIcon
	 * @param badgeIcon
	 * @param language
	 * @param country
	 */
	public CallIconPanel(ImageIcon mainIcon, ImageIcon badgeIcon, String language, 
			String country) {
		
		super();
		
		setupPanel(mainIcon, badgeIcon, language, country);
		
	}
	
	/**
	 * Creates a CallIconPanel, will attempt to load the icons given by the relative
	 * path
	 * @param mainIconPath main Icon
	 * @param badgeIconPath smaller badge Icon
	 * @param language language for I18N
	 * @param country country for I18N
	 */
	public CallIconPanel(String mainIconPath, String badgeIconPath, String language,
			String country){
		
		super();
		
		setupPanel(createImageIcon(mainIconPath, "mainIcon"),  //$NON-NLS-1$
				createImageIcon(badgeIconPath, "badgeIcon"), language, country); //$NON-NLS-1$
		
	}
	
	/**
	 * Creates the panel, called by constructors
	 * @param mainIcon
	 * @param mainBadge
	 * @param language
	 * @param country
	 */
	private void setupPanel(ImageIcon mainIcon, ImageIcon mainBadge, String language, 
			String country){
		
		xStrings = new I18NStrings(language, country);
		
		this.setPreferredSize(new Dimension(100, 100));
		
		main = new JLabel(mainIcon);
		main.setBounds(18, 18, 64, 64);
		
		badge = new JLabel(mainBadge);
		badge.setBounds(50, 50, 32, 32);
		
		this.add(this.badge, 2);
		this.add(this.main, 1);
		
	}
	
	/**
	 * Sets the main icon to the icon described by the relative path
	 * @param iconPath
	 */
	public void setMainIcon(String iconPath){
		
		setMainIcon(createImageIcon(iconPath, iconPath));
		
	}
	
	/**
	 * Sets the main icon to the given icon
	 * @param mainIcon
	 */
	public void setMainIcon(ImageIcon mainIcon){
		
		final ImageIcon icon = mainIcon;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				main.setIcon(icon);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the badge icon to the icon described by the relative path
	 * @param iconPath
	 */
	public void setBadgeIcon(String iconPath){
		
		setMainIcon(createImageIcon(iconPath, iconPath));
		
	}
	
	/**
	 * Sets the badge icon to the given icon
	 * @param badgeIcon
	 */
	public void setBadgeIcon(ImageIcon badgeIcon){
		
		final ImageIcon icon = badgeIcon;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				badge.setIcon(icon);
				
			}
			
		});
		
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
			
			LOGGER.warning(xStrings.getString("CallIconPanel.logLoadIconError")); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}

}
