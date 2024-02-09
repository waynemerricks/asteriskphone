package com.thevoiceasia.phonebox.launcher;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.thevoiceasia.phonebox.calls.TransparentLabel;

public class Splash extends JFrame implements SplashControl{

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	/** CLASS VARS **/
	private TransparentLabel statusLabel;
	
	public Splash(String title) throws HeadlessException {
		super(title);
		
		this.setLayout(new BorderLayout());
		this.setSize(320, 280);
		
		GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getScreenDevices();
		
		int height = screens[0].getDisplayMode().getHeight();
		int width = screens[0].getDisplayMode().getWidth();
		
		this.setLocation(width / 2 - this.getWidth() / 2, 
				height / 2 - this.getHeight() / 2);
		
		this.setUndecorated(true);
		
		this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		
		JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.setBorder(new LineBorder(Color.BLACK));
		center.setBackground(Color.WHITE);
		
		TransparentLabel image = new TransparentLabel(
				createImageIcon("images/splash.png", "splash"));  
		image.setBorder(null);
		
		center.add(image, BorderLayout.CENTER);
		
		JPanel text = new JPanel(new GridLayout(1, 1, 5, 5));
		text.setBorder(new EmptyBorder(5, 5, 5, 5));
		text.setOpaque(false);
		
		statusLabel = new TransparentLabel("Reticulating Splines", TransparentLabel.CENTER); 
		statusLabel.setBorder(null);
		text.add(statusLabel);
				
		center.add(text, BorderLayout.SOUTH);
		
		this.add(center, BorderLayout.CENTER);
			
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
			
			System.exit(1);
			
		}
		
		return icon;
		
	}
	
	@Override
	public void setStatus(String status) {
		
		final String s = status;
		
		SwingUtilities.invokeLater(new Runnable(){
			
			public void run(){
		
				statusLabel.setText(s);
				
			}
		});
		
	}

	@Override
	public void close() {
		
		this.setVisible(false);
		this.dispose();
		
	}

}
