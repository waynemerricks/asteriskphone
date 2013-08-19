package com.thevoiceasia.phonebox.calls;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.phonebox.records.Person;
import com.thevoiceasia.phonebox.records.PhoneCall;

public class CallInfoPanel extends JPanel implements MouseListener{

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	public static final int ALERT_OK = 0;
	public static final int ALERT_INFORMATION = 1;
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
	public static final int MODE_CLICKED = -1;
	
	//Colours of the different modes
	private static final Color RINGING_COLOUR = new Color(99, 169, 219);//Blue
	private static final Color ANSWERED_COLOUR = new Color(237, 171, 122);//Orange
	private static final Color ANSWERED_ELSEWHERE_COLOUR = new Color(164, 164, 164);//Grey
	private static final Color QUEUED_COLOUR = new Color(111, 212, 127);//Green
	private static final Color ON_AIR_COLOUR = new Color(227, 91, 91);//Red ish
	private static final Color CLICKED_COLOUR = new Color(234, 223, 39); //Yellow
	
	private static final Logger LOGGER = Logger.getLogger(CallInfoPanel.class.getName());//Logger
	
	/** CLASS VARS **/
	private I18NStrings xStrings;
	private int mode;
	private Color defaultColour;
	private Timer ringingTimer;
	private TimerTask ringingTask;
	private MultiUserChat controlRoom;
	private String channelID, myExtension; 
	private boolean hangupActive, canTakeCall;
	private PhoneCall phoneCallRecord;
	
	/** GUI SPECIFIC **/
	private TransparentLabel alertIcon, connectedToLabel, conversationLabel;
	private TimerLabel timeLabel;
	private BoldLabel nameLabel, locationLabel;
	
	/**
	 * Sets up a callinfopanel that has info about the caller
	 * @param language language for strings I18N
	 * @param country country for strings I18N
	 * @param callerName name of the person who is calling
	 * @param callerLocation location of the person who is calling
	 * @param conversation current conversation details (typed by handlers)
	 * @param alertLevel friendly icon to use along with call
	 * @param channelID id of the asterisk channel this panel applies to
	 * @param hangupActive indicates whether hangup mode is active at time of creation
	 * @param canTakeCall indicates whether this user can steal calls from others
	 */
	public CallInfoPanel(String language, String country, String callerName, 
			String callerLocation, String conversation,	int alertLevel,
			String channelID, boolean hangupActive, boolean canTakeCall,
			MultiUserChat controlRoom, String myExtension){
	
		xStrings = new I18NStrings(language, country);
		this.addMouseListener(this);
		defaultColour = this.getBackground();
		ringingTimer = new Timer("ringingTimer:" + channelID); //$NON-NLS-1$
		this.channelID = channelID;
		this.hangupActive = hangupActive;
		this.canTakeCall = canTakeCall;
		this.controlRoom = controlRoom;
		this.myExtension = myExtension;
		
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
		timeLabel = new TimerLabel(xStrings.getString("CallInfoPanel.callTimeInit"), //$NON-NLS-1$
				TransparentLabel.CENTER); 
		
		c.weightx = 0.25;
		c.gridx = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.BOTH;

		this.add(timeLabel, c);
		
		//Connected To Label, will be blank by default, fill row
		connectedToLabel = new TransparentLabel(" ", TransparentLabel.CENTER); //$NON-NLS-1$
		
		c.weightx = 1;
		c.gridx = 2;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH;
		
		this.add(connectedToLabel, c);
		
		//Name Label, 2nd Row, fill horizontal
		nameLabel = new BoldLabel(callerName, TransparentLabel.CENTER);
		
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 4;
		
		this.add(nameLabel, c);
		
		//Location Label, 3rd Row, fill horizontal
		locationLabel = new BoldLabel(callerLocation, TransparentLabel.CENTER);
		
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
		
		this.setPreferredSize(new Dimension(600, 150));
		this.setMinimumSize(new Dimension(400, 150));
		//this.validate();
		
	}
	
	public void setControlRoom(MultiUserChat controlRoom){
		
		this.controlRoom = controlRoom;
		
	}
	
	/**
	 * Sets this panel to ringing mode
	 */
	public void setRinging(){
		
		if(mode != MODE_RINGING){
			
			timeLabel.resetStageTime(); //Reset Stage Time
			
			ringingTask = new TimerTask(){//Setup new ringing animation
				
				public void run(){
					
					setRinging();
					
				}
				
			};
		
			ringingTimer.schedule(ringingTask, 750, 750);
		
		}
		
		mode = MODE_RINGING;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				if(getBackground() == defaultColour)
					setBackground(RINGING_COLOUR);
				else
					setBackground(defaultColour);
				
			}
			
		});
		
	}
	
	/**
	 * Sets this panel to clicked mode
	 * This indicates to the user that something is happening
	 * and we're waiting for server confirmation
	 * 
	 * This should arrive in the form of asterisk control messages such as
	 * call connected to your phone or whatever and will trigger setAnswered etc
	 */
	public void setClicked(){
		
		if(mode == MODE_RINGING)
			ringingTask.cancel();
		
		mode = MODE_CLICKED;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(CLICKED_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered mode
	 */
	public void setAnswered(){
		
		mode = MODE_ANSWERED;
		timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ANSWERED_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered by someone else mode
	 */
	public void setAnsweredElseWhere(){
		
		mode = MODE_ANSWERED_ELSEWHERE;
		timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ANSWERED_ELSEWHERE_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered by someone else mode
	 */
	public void setQueued(){
		
		if(mode == MODE_RINGING)
			ringingTask.cancel();
		
		mode = MODE_QUEUED;
		timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(QUEUED_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered by someone else mode
	 */
	public void setOnAir(){
		
		if(mode == MODE_RINGING)
			ringingTask.cancel();
		
		mode = MODE_ON_AIR;
		timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ON_AIR_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the icon for the call info panel to the given alert level
	 * @param level
	 */
	public void setAlertLevel(int level){
		
		final int slevel = level;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				alertIcon.setIcon(UIManager.getIcon(ALERT_ICONS[slevel]));
				
			}
			
		});
		
	}
	
	/**
	 * Set a custom image as the alert icon
	 * Recommend 48x48 image files (png, jpg should work probably others but ymmv)
	 * @param pathToImage relative path to image
	 */
	public void setAlertLevel(String pathToImage){
		
		final String apath = pathToImage;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				alertIcon.setIcon(createImageIcon(apath, apath));
				
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
			
			LOGGER.warning(xStrings.getString("CallInfoPanel.logLoadIconError") + path); //$NON-NLS-1$
			
		}
		
		return icon;
		
	}
	
	

	/**
	 * Sends a message to control room via XMPP
	 */
	private void sendControlMessage(int messageMode){
		
		LOGGER.info(xStrings.getString("CallInfoPanel.sendingControlMessage")); //$NON-NLS-1$
		
		if(controlRoom != null){
			
			String message = null;
			
			if(messageMode == MODE_RINGING || messageMode == MODE_QUEUED){
				
				if(hangupActive){
					message = xStrings.getString("calls.hangup") + "/" + channelID;  //$NON-NLS-1$//$NON-NLS-2$
					hangupActive = false;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCall")  //$NON-NLS-1$
							+ channelID);
				}else{
					message = xStrings.getString("calls.transfer") + "/" + channelID + "/"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									+ myExtension;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestTransferCall") //$NON-NLS-1$
							+ channelID + "/" + myExtension); //$NON-NLS-1$
				}
			}else if(messageMode == MODE_ANSWERED){
				
				if(hangupActive){
					message = xStrings.getString("calls.hangup") + "/" + channelID; //$NON-NLS-1$ //$NON-NLS-2$
					hangupActive = false;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCall")  //$NON-NLS-1$
							+ channelID);
				}else{
					message = xStrings.getString("calls.queue") + "/" + channelID; //$NON-NLS-1$ //$NON-NLS-2$
					LOGGER.info(xStrings.getString("CallInfoPanel.requestQueueCall") //$NON-NLS-1$
							+ channelID + "/" + myExtension); //$NON-NLS-1$
				}
			}else if(messageMode == MODE_ANSWERED_ELSEWHERE || messageMode == MODE_ON_AIR){
				
				//Taking a call from another user
				if(takeCall()){
					
					if(hangupActive){
						message = xStrings.getString("calls.hangup") + "/" + channelID; //$NON-NLS-1$ //$NON-NLS-2$ 
						hangupActive = false;
						LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCallOther")  //$NON-NLS-1$
								+ channelID);
					}else{
						message = xStrings.getString("calls.transfer") + "/" + channelID //$NON-NLS-1$ //$NON-NLS-2$
									+ myExtension;
						LOGGER.info(xStrings.getString(
								"CallInfoPanel.requestTransferCallOther") //$NON-NLS-1$
								+ channelID + "/" + myExtension); //$NON-NLS-1$
					}
					
				}
					
			}
			
			if(message != null)
				try {
					controlRoom.sendMessage(message);
				} catch (XMPPException e) {
					LOGGER.severe(xStrings.getString("CallInfoPanel.errorSendingControlMessage")); //$NON-NLS-1$
				}
			
		}else
			LOGGER.severe(xStrings.getString("CallInfoPanel.noControlRoomSet")); //$NON-NLS-1$
		
		//TODO Mode for hangup need to think about how it interacts with gui
		//E.g. hangup clicked should now cancel hangup mode elsewhere
		
	}
	
	/**
	 * Shows a confirm box when you request to take the call from someone else
	 * @return
	 */
	private boolean takeCall(){
	
		boolean takeIt = false;
		
		if(canTakeCall){
			
			int option = JOptionPane.showConfirmDialog(this, 
					xStrings.getString("CallInfoPanel.takeCall"),  //$NON-NLS-1$
					xStrings.getString("CallInfoPanel.takeCallTitle"), //$NON-NLS-1$
					JOptionPane.QUESTION_MESSAGE);
			
			if(option == JOptionPane.YES_OPTION)
				takeIt = true;
			
		}
		
		return takeIt;
		
	}
	
	/**
	 * Internal method to update the labels by grabbing info from the PhoneCall 
	 * object associated with this panel
	 */
	private void updateLabels(){
		
		if(phoneCallRecord != null){
			
			final Person p = phoneCallRecord.getActivePerson();
			final String answeredBy = phoneCallRecord.getAnsweredBy();
			
			SwingUtilities.invokeLater(new Runnable(){
				
				public void run(){
					
					nameLabel.setText(p.name);
					locationLabel.setText(p.location);
					
					if(p.getShortAlertLevel() == 'W')
						setAlertLevel(ALERT_WARNING);
					else if(p.getShortAlertLevel() == 'B')
						setAlertLevel(ALERT_BANNED);
					
					if(answeredBy != null)
						connectedToLabel.setText(answeredBy);
					
					if(p.currentConversation != null && p.currentConversation.length() > 0)
						conversationLabel.setText(p.currentConversation);
					
				}
			});
			
		}
		
	}
	
	/**
	 * Sets this panel to show info from the given PhoneCall
	 * @param phoneCall
	 */
	public void setPhoneCallRecord(PhoneCall phoneCall){
		
		phoneCallRecord = phoneCall;
		updateLabels();
		
	}
	
	/**
	 * Returns the PhoneCall object associated with this panel
	 * @return Can be null if no record attached
	 */
	public PhoneCall getPhoneCallRecord(){
		
		return phoneCallRecord;
		
	}
	
	/* MOUSE LISTENER METHODS */
	@Override
	public void mouseClicked(MouseEvent evt) {
		
		if(mode != MODE_CLICKED){
			int modeWhenClicked = new Integer(mode);
			setClicked();
			sendControlMessage(modeWhenClicked);
		}
		
	}

	/* UNUSED MOUSE LISTENER METHODS */
	@Override
	public void mouseEntered(MouseEvent evt) {}
	
	@Override
	public void mouseExited(MouseEvent evt) {}
	
	@Override
	public void mousePressed(MouseEvent evt) {}

	@Override
	public void mouseReleased(MouseEvent evt) {}
	
}
