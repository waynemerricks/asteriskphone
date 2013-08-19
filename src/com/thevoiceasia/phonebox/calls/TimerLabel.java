package com.thevoiceasia.phonebox.calls;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;

public class TimerLabel extends TransparentLabel {

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	/** CLASS VARS **/
	private long creationTimer, stageTimer, hourOffset;
	private Timer timeUpdateTimer = new Timer("time updater"); //$NON-NLS-1$
	
	public TimerLabel() {
		
		hourOffset = 0;
		setupTimer(null);
		
	}
	
	public TimerLabel(int minutes){
		
		hourOffset = minutes * 60000;
		setupTimer(null);
		
	}
	
	public TimerLabel(Date creationTime, int minutes){
		
		hourOffset = minutes * 60000;
		setupTimer(creationTime);
		
	}
	
	public TimerLabel(Date creationTime){
		
		hourOffset = 0;
		setupTimer(creationTime);
		
	}

	public TimerLabel(String text) {
		
		super(text);
		hourOffset = 0;
		setupTimer(null);
		
	}

	public TimerLabel(String text, int horizontalAlignment, int minutes){
		
		super(text, horizontalAlignment);
		this.hourOffset = minutes * 60000;
		setupTimer(null);
		
	}
	
	public TimerLabel(String text, int horizontalAlignment) {
		
		super(text, horizontalAlignment);
		hourOffset = 0;
		setupTimer(null);
		
	}

	/**
	 * Sets up the time lapsed on this object and spawns a thread to update it
	 * @param creationTimer null to set the time to now
	 */
	private void setupTimer(Date creationTimer){
		
		long time = new Date().getTime();
		
		if(creationTimer != null)
			this.creationTimer = creationTimer.getTime();
		else
			this.creationTimer = time;
		
		stageTimer = time;
		
		/* Set Label Text based on time format: 
		 * creation/stage 
		 * mm:ss / mm:ss
		 */
		setTimerText();
		
		//Start the timer going
		timeUpdateTimer.schedule(new TimerTask(){
			
			public void run(){
				
				setTimerText();
				
			}
			
		}, 1000, 1000);
		
	}
	
	/**
	 * Sets the text of this label to the time lapsed (in mm:ss) since the creation and stage
	 * time
	 */
	public void setTimerText(){
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
			
				long now = new Date().getTime();
				Date createTime = new Date(now - creationTimer - hourOffset);
				Date stageTime = new Date(now - stageTimer - hourOffset);
				
				SimpleDateFormat sdf = new SimpleDateFormat("mm:ss"); //$NON-NLS-1$
				
				String labelText = sdf.format(createTime) + " / " + sdf.format(stageTime); //$NON-NLS-1$
				setText(labelText);
				
			}
			
		});
		
	}
	
	/**
	 * Resets the time lapsed for the current staged time
	 */
	public void resetStageTime(){
		
		stageTimer = new Date().getTime();
		
	}

}
