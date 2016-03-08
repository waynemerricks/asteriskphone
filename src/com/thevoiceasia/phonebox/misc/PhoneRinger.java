package com.thevoiceasia.phonebox.misc;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;

public class PhoneRinger {

	/**
	 * VARS
	 */
	private boolean ringing = false, ready = false; 
    private Clip phone = null;
    
    /**
     * Creates a class that will loop a given sound clip
     * WAV format only sadly
     * @param pathToSoundClip 
     */
	public PhoneRinger(URL pathToSoundClip){
		
		AudioInputStream in = null;
				
		try{
			
			in = AudioSystem.getAudioInputStream(pathToSoundClip);
			DataLine.Info info = new DataLine.Info(Clip.class, in.getFormat());
			phone = (Clip)AudioSystem.getLine(info);
			phone.open(in);
			ready = true;
			
		}catch(LineUnavailableException e){
			
			System.err.println("Error getting output for clip");
			e.printStackTrace();
			
		}catch(UnsupportedAudioFileException e){
			
			System.err.println("Can't play file, unsupported type");
			e.printStackTrace();
			
		}catch(IOException e){
			
			System.err.println("IO Error with sound file");
			e.printStackTrace();
			
		}
		
	}
	
	/**
	 * Loops the clip this class was instantiated with
	 */
	public void ring(){
		
		if(!ringing){
			
			ringing = true;
			
			if(ready)
				phone.loop(-1);
			
		}
		
	}
	
	/**
	 * Stops playing the clip
	 */
	public void stop(){
	
		if(ringing){
			
			ringing = false;
			phone.stop();
			
		}
		
	}
	
	/**
	 * Helper to determine if this object is already in the ringing state
	 * @return true if ringing, false if not
	 */
	public boolean isRinging(){
		
		return ringing;
		
	}
	
}
