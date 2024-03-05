package com.github.waynemerricks.asteriskphone.misc;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URL;

public class AlertSounder {

	/**
	 * VARS
	 */
	private boolean ready = false; 
    private Clip alert = null;
    
    /**
     * Creates a class that will loop a given sound clip
     * WAV format only sadly
     * @param pathToSoundClip 
     */
	public AlertSounder(String pathToSoundClip){
		
		AudioInputStream in = null;
			
		try{
			
			in = AudioSystem.getAudioInputStream(getAudioURL(pathToSoundClip));
			DataLine.Info info = new DataLine.Info(Clip.class, in.getFormat());
			alert = (Clip)AudioSystem.getLine(info);
			alert.open(in);
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
	 * Gets the audio from a relative path and returns a URL for reference
	 * @param path path where audio resides
	 * @return URL pointing to the audio
	 */
	private URL getAudioURL(String path){

		URL audioURL = null;

		if(path != null){

			audioURL = getClass().getResource(path);

		}

		return audioURL;

	}
	
	/**
	 * Loops the clip this class was instantiated with
	 */
	public void play(){
		
		if(ready && !alert.isActive())
			alert.start();
		
	}
	
	/**
	 * Stops playing the clip
	 */
	public void stop(){
	
		if(ready && alert.isActive())
			alert.stop();
		
	}
	
}
