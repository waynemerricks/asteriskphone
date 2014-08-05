package com.thevoiceasia.phonebox.calls;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.MultiUserChat;

import com.thevoiceasia.phonebox.callinput.CallerUpdater;
import com.thevoiceasia.phonebox.records.Person;
import com.thevoiceasia.phonebox.records.PhoneCall;

public class CallInfoPanel extends JPanel implements MouseListener{

	/** STATICS **/
	private static final long serialVersionUID = 1L;
	public static final int ALERT_OK = 0;
	public static final int ALERT_INFORMATION = 1;
	public static final int ALERT_WARNING = 2;
	public static final int ALERT_BANNED = 3;
	public static final String ALERT_FAVOURITE = "images/favourite.png"; //$NON-NLS-1$
	
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
	public static final int MODE_RINGING_ME = 6;
	public static final int MODE_QUEUED_ME = 7;
	public static final int MODE_ANSWERED_ME = 8;
	public static final int MODE_CLICKED = -1;
	public static final int MODE_ON_AIR_ME = 9;
	
	//Colours of the different modes
	private static final Color RINGING_COLOUR = new Color(99, 169, 219);//Blue
	private static final Color RINGING_ME_COLOUR = new Color(29, 110, 167);//Dark Blue
	private static final Color ANSWERED_COLOUR = new Color(237, 171, 122);//Orange
	//private static final Color ANSWERED_ME_COLOUR= new Color(183, 93, 27);//Dark Orange
	private static final Color ANSWERED_ELSEWHERE_COLOUR = new Color(164, 164, 164);//Grey
	private static final Color QUEUED_COLOUR = new Color(111, 212, 127);//Green
	private static final Color QUEUED_ME_COLOUR = new Color(40, 130, 55);//Dark Green
	private static final Color ON_AIR_COLOUR = new Color(227, 91, 91);//Red ish
	private static final Color ON_AIR_ME_COLOUR = new Color(152, 27, 27);//Dark Red ish
	private static final Color CLICKED_COLOUR = new Color(234, 223, 39); //Yellow
	
	private static final Logger LOGGER = Logger.getLogger(CallInfoPanel.class.getName());//Logger
	
	/** CLASS VARS **/
	private I18NStrings xStrings;
	private int mode, modeWhenClicked;
	private Color defaultColour;
	private Timer ringingTimer;
	private TimerTask ringingTask;
	private MultiUserChat controlRoom;
	private String channelID, myExtension, myNickName, originator; 
	private boolean hangupActive, canTakeCall;
	private PhoneCall phoneCallRecord;
	private Vector<ManualHangupListener> hangupListeners = new Vector<ManualHangupListener>();
	private CallerUpdater updateThread = null;
	
	/** GUI SPECIFIC **/
	private TransparentLabel connectedToLabel, conversationLabel;//alertIcon
	private CallIconPanel alertIcon;
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
	 * @param controlRoom room to send control messages to
	 * @param myExtension The current users extension number (or null)
	 * @param myNickName The current users chat name
	 * @param hourOffSet Offset for time keeping purposes so that timer starts at 0
	 * @param badgeIconPath path to the icon to use as a badge
	 */
	public CallInfoPanel(String language, String country, String callerName, 
			String callerLocation, String conversation,	int alertLevel,
			String channelID, boolean hangupActive, boolean canTakeCall,
			MultiUserChat controlRoom, String myExtension, String myNickName,
			int hourOffset,	String badgeIconPath){
	
		xStrings = new I18NStrings(language, country);
		
		//Don't add a mouse listener if we have no extension, saves on some cpu time
		if(myExtension != null && !myExtension.equals("null") && myExtension.length() > 0) //$NON-NLS-1$
			this.addMouseListener(this);
		
		defaultColour = this.getBackground();
		ringingTimer = new Timer("ringingTimer:" + channelID); //$NON-NLS-1$
		this.channelID = channelID;
		this.hangupActive = hangupActive;
		this.canTakeCall = canTakeCall;
		this.controlRoom = controlRoom;
		this.myExtension = myExtension;
		this.myNickName = myNickName;
		
		this.setLayout(new MigLayout("insets 0, gap 0, fillx")); //$NON-NLS-1$
		
		//Alert Icon
		alertIcon = new CallIconPanel(UIManager.getIcon(ALERT_ICONS[alertLevel]),
				createImageIcon(badgeIconPath, badgeIconPath), language, country);
		alertIcon.setMinimumSize(new Dimension(100, 64));
		
		//Timer Label 1 row/column
		timeLabel = new TimerLabel(xStrings.getString("CallInfoPanel.callTimeInit"), //$NON-NLS-1$
				TransparentLabel.CENTER, hourOffset); 
		//timeLabel.setPreferredSize(new Dimension(100, 30));
		timeLabel.setMinimumSize(new Dimension(100, 30));
		
		this.add(timeLabel, "growx 25, split"); //$NON-NLS-1$
		
		//Connected To Label, will be blank by default, fill row
		connectedToLabel = new TransparentLabel(" ", TransparentLabel.CENTER); //$NON-NLS-1$
		
		connectedToLabel.setPreferredSize(new Dimension(150, 30));
		connectedToLabel.setMinimumSize(new Dimension(150, 30));
		
		this.add(connectedToLabel, "growx, gap 0, wrap"); //$NON-NLS-1$
		
		//Name Label, 2nd Row, fill horizontal
		nameLabel = new BoldLabel(callerName, TransparentLabel.CENTER);
		
		nameLabel.setPreferredSize(new Dimension(250, 30));
		nameLabel.setMinimumSize(new Dimension(250, 30));
		
		this.add(nameLabel, "growx, span 3, wrap"); //$NON-NLS-1$
		
		//Location Label, 3rd Row, fill horizontal
		locationLabel = new BoldLabel(callerLocation, TransparentLabel.CENTER);
		
		locationLabel.setPreferredSize(new Dimension(250, 30));
		locationLabel.setMinimumSize(new Dimension(250, 30));
		
		this.add(locationLabel, "growx, span 3, wrap"); //$NON-NLS-1$
		
		//Conversation Label, 4th Row, fill height/horiz
		conversationLabel = new TransparentLabel(conversation);
		conversationLabel.setFont(new Font(conversationLabel.getFont().getName(), 
				Font.PLAIN, 16));
		
		conversationLabel.setPreferredSize(new Dimension(350, 60));
		conversationLabel.setMinimumSize(new Dimension(350, 30));
		
		this.add(conversationLabel, "south"); //$NON-NLS-1$
		this.add(alertIcon, "west"); //Add Alert to Panel, west //$NON-NLS-1$
		
		this.setPreferredSize(new Dimension(350, 150));
		this.setMinimumSize(new Dimension(350, 150));
		
	}
	
	/**
	 * Set where this panel came from, used when something is dialled or
	 * a call comes in.  This way we can get around the problem whereby
	 * callerIDs may change to the external ID of the company and then you lose
	 * track of if this is a channel belonging to your phone or someone else
	 * @param originator
	 */
	public void setOriginator(String originator){
		
		this.originator = originator;
		
	}
	
	/**
	 * Sets the updater thread for this panel so that you can send info to other
	 * clients when names change etc
	 * @param thread
	 */
	public void setUpdaterThread(CallerUpdater thread){
		
		updateThread = thread;
		
	}
	
	/**
	 * Returns the creation number of this panel
	 * Can be null!
	 * @return
	 */
	public String getOriginator() {
		
		return originator;
		
	}
	
	/**
	 * Gets the icon panel associated with this info panel
	 * @return
	 */
	public CallIconPanel getIconPanel(){
		
		return alertIcon;
		
	}
	
	
	/**
	 * Helper method to return who this panel is connectedTo
	 * @return
	 */
	public String getConnectedTo(){
		
		return connectedToLabel.getText();
		
	}
	
	/**
	 * Adds a manual hang up listener to this object
	 * @param listener
	 */
	public void addManualHangupListener(ManualHangupListener listener){
		
		hangupListeners.add(listener);
		
	}
	
	/**
	 * Gets the mode of this panel
	 * @return
	 */
	public int getMode(){
		
		return mode;
		
	}
	
	/**
	 * Gets the creation time for this call as a long (equiv to Date().getTime())
	 * @return
	 */
	public long getCallCreationTime(){
		
		return timeLabel.getCreationTime();
		
	}
	
	/**
	 * Sets the callCreation time to the given long (in the form of Date().getTime()
	 * @param time
	 */
	public void setCallCreationTime(long time){
		
		timeLabel.setCreationTime(time);
		
	}
	
	/**
	 * Helper method, used for LOGGER reporting
	 * @return
	 */
	public String getChannelID(){
		
		return channelID;
		
	}
	
	/**
	 * Alert our listeners that they can cancel the hang up mode
	 * this occurs if we're initiating a hang up or if it is someone elses call
	 * and we clicked No, it will also cancel hang up mode
	 */
	private void notifyManualHangupListeners(){
		
		for(int i = 0; i < hangupListeners.size(); i++){
			
			hangupListeners.get(i).hangupClicked(channelID);
			
		}
		
	}
	
	/**
	 * Sets the GUI label for connectedTo
	 * Will reflect the changes on the PhoneCall record associated with this panel
	 * @param to
	 */
	public void setConnectedTo(String to){
		
		phoneCallRecord.setAnsweredBy(to);
		
		final String connTo = to;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
		
				LOGGER.info(connTo);
				connectedToLabel.setText(connTo);
				
			}
		});
		
	}
	/**
	 * Helper method to set the control room this panel reports to
	 * @param controlRoom
	 */
	public void setControlRoom(MultiUserChat controlRoom){
		
		this.controlRoom = controlRoom;
		
	}
	
	/**
	 * Sets ringing mode
	 */
	public void setRingingMe(boolean reset){
		
		if(mode != MODE_RINGING_ME){
			
			if(reset)
				timeLabel.resetStageTime(); //Reset Stage Time
			
			ringingTask = new TimerTask(){//Setup new ringing animation
				
				public void run(){
					
					setRingingMe(false);
					
				}
				
			};
		
			ringingTimer.schedule(ringingTask, 750, 750);
			
		}
		
		mode = MODE_RINGING_ME;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				if(getBackground() == defaultColour)
					setBackground(RINGING_ME_COLOUR);
				else
					setBackground(defaultColour);
				
			}
			
		});
		
	}
	
	/**
	 * Sets this panel to ringing mode
	 */
	public void setRinging(String connectedTo, boolean reset){
		
		if(mode != MODE_RINGING){
			
			if(reset)
				timeLabel.resetStageTime(); //Reset Stage Time
			
			final String connected = connectedTo;
			ringingTask = new TimerTask(){//Setup new ringing animation
				
				public void run(){
					
					setRinging(null, false);
					
					if(connected != null)
						connectedToLabel.setText(connected);
					
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
		
		modeWhenClicked = mode;
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_CLICKED;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(CLICKED_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Used to alert this panel that hangup is active (or has been deactivated)
	 * @param active true if active, false if not
	 */
	public void setHangupActive(boolean active){
		
		hangupActive = active;
		
	}
	
	/**
	 * Sets the panel to answered mode
	 */
	public void setAnswered(boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_ANSWERED;
		
		if(reset)
			timeLabel.resetStageTime();
		
		if(phoneCallRecord != null)
			phoneCallRecord.setAnsweredBy(myNickName);
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ANSWERED_COLOUR);
				connectedToLabel.setText(myNickName);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered me mode, signifies I've made a call that is answered
	 */
	public void setAnsweredMe(String answeredBy, boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_ANSWERED_ME;
		
		if(reset)
			timeLabel.resetStageTime();
		
		if(phoneCallRecord != null && answeredBy != null)
			phoneCallRecord.setAnsweredBy(answeredBy);
		
		final String answered = answeredBy;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ANSWERED_COLOUR);
				
				if(answered != null)
					connectedToLabel.setText(answered);
				
			}
			
		});
		
	}
	
	
	/**
	 * Internal answeredElseWhere used when user clicks no to take call
	 */
	private void setAnsweredElseWhere(){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		/*
		 * Only reset to AnsweredElseWhere if we're still in the clicked state
		 * Call may have gone on air by the time the user clicks yes.
		 * 
		 * Still a possibility of race condition here but its not worth the synching
		 * hassle
		 */
		if(mode == MODE_CLICKED || mode == MODE_RINGING_ME){
			
			mode = MODE_ANSWERED_ELSEWHERE;
			
			SwingUtilities.invokeLater(new Runnable(){
				
				public void run(){
					
					setBackground(ANSWERED_ELSEWHERE_COLOUR);
					
				}
				
			});
			
		}
	}
	
	/**
	 * Sets the panel to answered by someone else mode
	 */
	public void setAnsweredElseWhere(String answeredBy, boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_ANSWERED_ELSEWHERE;
		
		if(phoneCallRecord != null)
			phoneCallRecord.setAnsweredBy(answeredBy);
		
		if(reset)
			timeLabel.resetStageTime();
		
		final String answered = answeredBy;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ANSWERED_ELSEWHERE_COLOUR);
				
				if(answered != null)
					connectedToLabel.setText(answered);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to queued mode
	 */
	public void setQueued(boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_QUEUED;
		
		if(reset)
			timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(QUEUED_COLOUR);
				connectedToLabel.setText(""); //$NON-NLS-1$
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to answered by someone else mode
	 */
	public void setQueuedMe(boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_QUEUED_ME;
		
		if(reset)
			timeLabel.resetStageTime();
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(QUEUED_ME_COLOUR);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to on air mode
	 */
	public void setOnAir(String studioName){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_ON_AIR;
		timeLabel.resetStageTime();
		
		final String studio = studioName;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ON_AIR_COLOUR);
				
				if(studio != null)
					connectedToLabel.setText(studio);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the panel to on air me mode
	 */
	public void setOnAirMe(String studioName, boolean reset){
		
		if(mode == MODE_RINGING || mode == MODE_RINGING_ME)
			ringingTask.cancel();
		
		mode = MODE_ON_AIR_ME;
		
		if(reset)
			timeLabel.resetStageTime();
		
		final String studio = studioName;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				setBackground(ON_AIR_ME_COLOUR);
				
				if(studio != null)
					connectedToLabel.setText(studio);
				
			}
			
		});
		
	}
	
	/**
	 * Sets the icon for the call info panel to the given alert level
	 * @param level
	 */
	public void setAlertLevel(String alertText, int level, boolean updateOthers){
		
		final int slevel = level;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				//alertIcon.setIcon(UIManager.getIcon(ALERT_ICONS[slevel]));
				alertIcon.setMainIcon(UIManager.getIcon(ALERT_ICONS[slevel]));
				
			}
			
		});
		
		//Update the record held by this panel
		this.getPhoneCallRecord().getActivePerson().alert = "" + level; //$NON-NLS-1$
		
		//If we need to notify others then do so
		if(updateOthers)
			sendCallerUpdated("alert", alertText + "@@" + level); //$NON-NLS-1$ //$NON-NLS-2$
		
			
	}
	
	/**
	 * Set a custom image as the alert icon
	 * Recommend 64x64 image files (png, jpg should work probably others but ymmv)
	 * @param pathToImage relative path to image
	 * @param updateOthers true if we need to update other clients that this has changed
	 */
	public void setAlertLevel(String alertText, String pathToImage, boolean updateOthers){
		
		final String apath = pathToImage;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
				
				//alertIcon.setIcon(createImageIcon(apath, apath));
				alertIcon.setMainIcon(createImageIcon(apath, apath));
				
			}
			
		});
		
		if(updateOthers){
			sendCallerUpdated("alert", alertText + "@@" + pathToImage.replace("/", "+")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			//We can also save to DB here
			this.getPhoneCallRecord().getActivePerson().alert = alertText;
		}
	}
	
	/**
	 * Sets a custom image as the badge icon
	 * Recommend 32x32 images 
	 * @param pathToImage
	 * @param updateOthers true if we need to update other clients that this has changed
	 */
	public void setBadgeIcon(String badgeText, String pathToImage, boolean updateOthers){
		
		getIconPanel().setBadgeIcon(pathToImage);
		
		if(updateOthers)
			sendCallerUpdated("calltype", badgeText + "@@" + pathToImage.replace("/", "+")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
	}
	
	/**
	 * Gets the image from a relative path and creates an icon for use with buttons
	 * @param path path where image resides
	 * @param description identifier for this image, for internal use
	 * @return the image loaded as a Java Icon
	 */
	private ImageIcon createImageIcon(String path, String description){
		
		ImageIcon icon = null;
		
		if(path != null){
			
			URL imgURL = getClass().getResource(path);
			
			if(imgURL != null)
				icon = new ImageIcon(imgURL, description);
			else{
				
				LOGGER.warning(xStrings.getString("CallInfoPanel.logLoadIconError") + path); //$NON-NLS-1$
				
			}
		
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
					notifyManualHangupListeners();
					
				}else{
					message = xStrings.getString("calls.transfer") + "/" + channelID + "/"  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									+ myExtension;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestTransferCall") //$NON-NLS-1$
							+ channelID + "/" + myExtension); //$NON-NLS-1$
				}
			}else if(messageMode == MODE_RINGING_ME || messageMode == MODE_QUEUED_ME 
					|| messageMode == MODE_ON_AIR_ME || messageMode == MODE_ANSWERED_ME){
				
				if(hangupActive){
					
					message = xStrings.getString("calls.hangup") + "/" + channelID;  //$NON-NLS-1$//$NON-NLS-2$
					hangupActive = false;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCall")  //$NON-NLS-1$
							+ channelID);
					notifyManualHangupListeners();
					
				}else{
					
					/* We can't do anything to our own call unless we are in the ANSWERED_ME
					 * state so ignore it if we're not hanging up, but we do need to reset 
					 * it back to state 
					 * 
					 * If we are in ANSWERED_ME send a request to transfer the other side
					 * of the call to the on air queue */
					switch(messageMode){
					
						case MODE_RINGING_ME:
							setRingingMe(false);
							break;
						case MODE_QUEUED_ME:
							setQueuedMe(false);
							break;
						case MODE_ON_AIR_ME:
							setOnAirMe(null, false);
							break;
						case MODE_ANSWERED_ME:
							//If this is our call we can transfer the end point
							message = xStrings.getString("calls.transferEndPoint") + "/" + channelID;  //$NON-NLS-1$//$NON-NLS-2$
							LOGGER.info(xStrings.getString(
									"CallInfoPanel.requestTransferEndpoint")  //$NON-NLS-1$
									+ channelID);
							break;
					}
					
				}
				
			}else if(messageMode == MODE_ANSWERED){
			
				
				if(hangupActive){
					
					message = xStrings.getString("calls.hangup") + "/" + channelID; //$NON-NLS-1$ //$NON-NLS-2$
					hangupActive = false;
					LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCall")  //$NON-NLS-1$
							+ channelID);
					notifyManualHangupListeners();
					
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
						notifyManualHangupListeners();
						
					}else{
						message = xStrings.getString("calls.transfer") + "/" + channelID //$NON-NLS-1$ //$NON-NLS-2$
									+ "/" + myExtension; //$NON-NLS-1$
						LOGGER.info(xStrings.getString(
								"CallInfoPanel.requestTransferCallOther") //$NON-NLS-1$
								+ channelID + "/" + myExtension); //$NON-NLS-1$
					}
					
				}else{
					
					//Set call back to answered elsewhere don't reset timer
					
					if(messageMode == MODE_ANSWERED_ELSEWHERE)
						setAnsweredElseWhere();
					else
						setOnAir(null);
					
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
					xStrings.getString("CallInfoPanel.takeCallTitle")//$NON-NLS-1$
						.replace("\\n", "\n").replace("\\t", "\t"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
			
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
			
			SwingUtilities.invokeLater(new Runnable(){
				
				public void run(){
					
					nameLabel.setText(p.name);
					locationLabel.setText(p.location);
					
					if(p.getShortAlertLevel() == 'W')
						setAlertLevel(p.alert, ALERT_WARNING, false);
					else if(p.getShortAlertLevel() == 'B')
						setAlertLevel(p.alert, ALERT_BANNED, false);
					else if(p.getShortAlertLevel() == 'F')
						setAlertLevel(p.alert, ALERT_FAVOURITE, false);
					
					if(phoneCallRecord.getCallType() != null)
						setBadgeIcon(phoneCallRecord.getCallType(), 
								phoneCallRecord.getCallTypeIconPath(), false);
						
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
	 * Answers this panel (called outside via CallManagerPanel.answerNext())
	 */
	public void answer(){
		
		if(mode != MODE_CLICKED){
			int modeWhenClicked = mode;
			setClicked();
			sendControlMessage(modeWhenClicked);
		}
		
	}
	
	/**
	 * Changes the active person for this panel to a new person given by the id
	 * @param person Person record to change to
	 */
	public void changeActivePerson(Person person) {
		
		// TODO Set New Active Person and update the labels
		this.getPhoneCallRecord().addActivePerson(person);
		updateLabels();
		
	}
	
	/**
	 * Hangs up this panel (called outside via CallManagerPanel)
	 */
	public void hangup(){
	
		String message = xStrings.getString("calls.hangup") + "/" + channelID; //$NON-NLS-1$ //$NON-NLS-2$
		hangupActive = false;
		LOGGER.info(xStrings.getString("CallInfoPanel.requestHangupCall")  //$NON-NLS-1$
				+ channelID);
		notifyManualHangupListeners();
		
		try {
			controlRoom.sendMessage(message);
		} catch (XMPPException e) {
			LOGGER.severe(xStrings.getString("CallInfoPanel.errorSendingControlMessage")); //$NON-NLS-1$
		}
		
	}
	
	/**
	 * Method to update a custom field that is not visible on the panel but is tied
	 * to the PhoneCallRecord
	 * @param fieldName mapping to update
	 * @param value value to update to
	 * @param updateOthers whether we need to send an XMPP update message or not
	 */
	public void setPhoneCallField(String fieldName, String value, boolean updateOthers){
		
		getPhoneCallRecord().setField(fieldName, value);
		
		if(updateOthers)
			sendCallerUpdated(fieldName, value);
		
	}
	/**
	 * Returns the PhoneCall object associated with this panel
	 * @return Can be null if no record attached
	 */
	public PhoneCall getPhoneCallRecord(){
		
		return phoneCallRecord;
		
	}
	
	/**
	 * Sets the conversation field on this panel to the given text
	 * @param text text to set the conversation to
	 * @param updateOthers true if we should update other clients with this information
	 * Should only be true when called from CallInputPanel (as its our call in theory)
	 */
	public void setConversation(String text, boolean updateOthers) {
		
		conversationLabel.setText(text);
		
		if(text.contains("/")) //$NON-NLS-1$
			text = text.replaceAll("/", "^^%%$$");  //$NON-NLS-1$//$NON-NLS-2$
		
		//Update Others
		if(updateOthers)
			sendCallerUpdated("conversation", text); //$NON-NLS-1$
		
	}
	
	/**
	 * Sets the caller name field on this panel to the given text
	 * @param text
	 * @param updateOthers true if we should update other clients with this information
	 * Should only be true when called from CallInputPanel (as its our call in theory)
	 */
	public void setCallerName(String text, boolean updateOthers) {
		
		nameLabel.setText(text);
		
		//Update Others
		if(updateOthers)
			sendCallerUpdated("name", text); //$NON-NLS-1$
		
	}
	
	/**
	 * Sets the caller location field on this panel to the given text
	 * @param text
	 * @param updateOthers true if we should update other clients with this information
	 * Should only be true when called from CallInputPanel (as its our call in theory)
	 */
	public void setCallerLocation(String text, boolean updateOthers) {
		
		locationLabel.setText(text);
		
		//Update Others
		if(updateOthers)
			sendCallerUpdated("location", text); //$NON-NLS-1$
		
	}
	
	/**
	 * Sends a message to the client updater thread to show that a field has been
	 * updated with new information.  This is to keep updates looking realtime
	 * while cutting down on excessive DB requests
	 * @param field field to update
	 * @param value value to set it to
	 */
	private void sendCallerUpdated(String field, String value){
		
		/*
		 * Create a queue for updates, send max every 2 seconds
		 * If field already exists in queue then overwrite it
		 */
		if(updateThread != null)
			updateThread.addUpdate(channelID, field, value);
		
	}
	
	/**
	 * Resets this panel to the mode it was in before being clicked
	 */
	public void reset() {
		
		mode = modeWhenClicked;
		
		switch(mode){
		
			case MODE_RINGING:
				setRinging(null, false);
				break;
			case MODE_ANSWERED:
				setAnswered(false);
				break;
			case MODE_ANSWERED_ELSEWHERE:
				setAnsweredElseWhere(null, false);
				break;
			case MODE_QUEUED:
				setQueued(false);
				break;
			case MODE_ON_AIR:
				setQueuedMe(false);
				break;
			case MODE_RINGING_ME:
				setRingingMe(false);
				break;
			case MODE_QUEUED_ME:
				setQueuedMe(false);
				break;
			case MODE_ANSWERED_ME:
				setAnsweredMe(null, false);
				break;
			case MODE_ON_AIR_ME:
				setOnAirMe(null, false);
				break;
				
		}
		
	}
	
	/* MOUSE LISTENER METHODS */
	@Override
	public void mouseClicked(MouseEvent evt) {
		
		if(myExtension != null){
			
			//This will forward the clicks onto its parent if its a CallManagerPanel
			if(this.getParent() != null && this.getParent() instanceof CallManagerPanel){
				
				CallManagerPanel cmp = (CallManagerPanel)this.getParent();
				
				MouseListener[] mouseListeners = cmp.getMouseListeners();
				
				for(MouseListener listener : mouseListeners)
					listener.mouseClicked(evt);
				
			}
			
			if(mode != MODE_CLICKED){
				int modeWhenClicked = new Integer(mode);
				setClicked();
				sendControlMessage(modeWhenClicked);
			}
			
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
