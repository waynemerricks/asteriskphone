package com.thevoiceasia.phonebox.calls;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import java.util.logging.Logger;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;


public class CallIconPanel extends JLayeredPane implements ComponentListener{

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
	public CallIconPanel(Icon mainIcon, Icon badgeIcon, String language, 
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
	private void setupPanel(Icon mainIcon, Icon mainBadge, String language, 
			String country){
		
		xStrings = new I18NStrings(language, country);
		this.setBorder(new LineBorder(Color.BLACK));
		main = new JLabel(mainIcon, JLabel.CENTER);
		main.setOpaque(false);
		main.setBounds(0, 0, 64, 64);
		
		badge = new JLabel(mainBadge, JLabel.CENTER);
		badge.setOpaque(false);
		badge.setBounds(32, 32, 32, 32);
		
		this.addComponentListener(this);
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
	public void setMainIcon(Icon mainIcon){
		
		final Icon icon = mainIcon;
		
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
	public void setBadgeIcon(Icon badgeIcon){
		
		final Icon icon = badgeIcon;
		
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

	/* COMPONENT LISTENER */
	@Override
	public void componentResized(ComponentEvent evt) {
		
		if(evt.getSource() instanceof Component){
			
			Component c = (Component)evt.getSource();
			
			int width = c.getSize().width;
			int height = c.getSize().height;
			
			main.setBounds(width / 2 - 32, height / 2 - 32, 64, 64);
			badge.setBounds(width / 2, height / 2, 32, 32);
			
		}
		
	}

	/* UNUSED ComponentListener events */
	@Override
	public void componentShown(ComponentEvent arg0) {}

	@Override
	public void componentHidden(ComponentEvent evt) {}

	@Override
	public void componentMoved(ComponentEvent evt) {}

}
