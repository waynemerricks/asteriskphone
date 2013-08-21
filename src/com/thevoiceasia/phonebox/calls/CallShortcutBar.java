package com.thevoiceasia.phonebox.calls;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.thevoiceasia.phonebox.chat.I18NStrings;
import com.thevoiceasia.phonebox.launcher.Client;
import com.thevoiceasia.phonebox.misc.LastActionTimer;

public class CallShortcutBar extends JPanel implements ActionListener, LastActionTimer{

	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(CallShortcutBar.class.getName());//Logger
	private static final long serialVersionUID = 1L;
	
	/** CLASS VARS **/
	private I18NStrings xStrings; //Link to external string resources
	private CallManagerPanel callManager;
	private long lastActionTime = new Date().getTime();
	
	public CallShortcutBar(CallManagerPanel callManager, String language, String country) {
		// TODO Auto-generated constructor stub
		xStrings = new I18NStrings(language, country);
		
		this.callManager = callManager;
		this.setLayout(new GridLayout(1, 5, 5, 5));
		
		JButton button = new JButton(xStrings.getString("CallShortcutBar.answerNext"),  //$NON-NLS-1$
				createImageIcon("images/answer.png", "answer")); //$NON-NLS-1$ //$NON-NLS-2$
		button.addActionListener(this);
		button.setActionCommand("answer"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("CallShortcutBar.answerNextToolTip")); //$NON-NLS-1$
		
		this.add(button);
		
		button = new JButton(xStrings.getString("CallShortcutBar.answerRandom"),  //$NON-NLS-1$
				createImageIcon("images/answerRandom.png", "answerrandom")); //$NON-NLS-1$ //$NON-NLS-2$
		button.addActionListener(this);
		button.setActionCommand("answerrandom"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("CallShortcutBar.answerRandomToolTip")); //$NON-NLS-1$
		
		this.add(button);
		
		button = new JButton(xStrings.getString("CallShortcutBar.drop"),  //$NON-NLS-1$
				createImageIcon("images/drop.png", "drop")); //$NON-NLS-1$ //$NON-NLS-2$
		button.addActionListener(this);
		button.setActionCommand("drop"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("CallShortcutBar.dropToolTip")); //$NON-NLS-1$
		
		this.add(button);
		
		this.add(new JLabel(" ")); //$NON-NLS-1$ SPACER
		
		button = new JButton(xStrings.getString("CallShortcutBar.dial"),  //$NON-NLS-1$
				createImageIcon("images/dial.png", "dial")); //$NON-NLS-1$ //$NON-NLS-2$
		button.addActionListener(this);
		button.setActionCommand("dial"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("CallShortcutBar.dialToolTip")); //$NON-NLS-1$
		
		this.add(button);
		
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
			
			LOGGER.warning(xStrings.getString("CallShortcutBar.logLoadIconError")); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}
	
	private void drop(){
	
		/* TODO Set cursor to drop icon
		 * also tell callmanager to set drop too
		 */
		boolean client = false;
		Component parent = getParent();
		
		while(!client){
			
			if(parent instanceof Client)
				client = true;
			else
				parent = parent.getParent();
			
		}
		this.setCursor(cursor)
		callManager.setDropMode();
		
	}
	
	@Override
	public long getLastActionTime() {
		
		return lastActionTime;
		
	}

	@Override
	public void actionPerformed(ActionEvent evt) {
		
		lastActionTime = new Date().getTime();
		
		if(evt.getActionCommand().equals("answer")){ //$NON-NLS-1$
			callManager.answerNext();
		}else if(evt.getActionCommand().equals("answerrandom")){ //$NON-NLS-1$
			callManager.answerRandom();
		}else if(evt.getActionCommand().equals("drop")){ //$NON-NLS-1$
			drop();
		}else if(evt.getActionCommand().equals("dial")){ //$NON-NLS-1$
			callManager.dial();
		}
		
	}

}
