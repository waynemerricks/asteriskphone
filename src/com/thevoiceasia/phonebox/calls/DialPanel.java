package com.thevoiceasia.phonebox.calls;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class DialPanel extends JDialog implements ActionListener, KeyListener{

	/** STATICS **/
	private static final Logger LOGGER = Logger.getLogger(DialPanel.class.getName());//Logger
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("nls")
	private static final String[] buttons = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "*",  
		"0" ,"#"};
	
	/* CLASS VARS */
	private JTextField number;
	private I18NStrings xStrings;
	private String dialledNumber;
	private Vector<DialListener> listeners = new Vector<DialListener>();
	
	public DialPanel(Frame owner, String title, String language, 
			String country) {
		
		super(owner, title, false);
		xStrings = new I18NStrings(language, country);
		
		this.setLayout(new BorderLayout());
		this.setSize(280, 480);
		
		//Input Panel for the number
		number = new JTextField();
		number.setFont(new Font(number.getFont().getName(), Font.BOLD, 24));
		number.setHorizontalAlignment(JTextField.CENTER);
		number.addKeyListener(this);
		
		this.add(number, BorderLayout.NORTH);
		
		//Numbers + * + #
		JPanel numberGrid = new JPanel(new GridLayout(4, 3, 5, 5));
		
		for(int i = 0; i < buttons.length; i++){
			
			JButton button = new JButton(buttons[i]);
			button.setActionCommand(buttons[i]);
			button.setToolTipText(xStrings.getString("DialPanel.dialToolTip") + " "   //$NON-NLS-1$//$NON-NLS-2$
					+ buttons[i]);
			button.addActionListener(this);
			numberGrid.add(button);
			
		}
		
		this.add(numberGrid, BorderLayout.CENTER);
		
		JPanel south = new JPanel(new GridLayout(1, 2, 5, 5));
		JButton button = new JButton(createImageIcon("images/answer.png", "dial"));  //$NON-NLS-1$//$NON-NLS-2$
		button.setActionCommand("dial"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("DialPanel.dialButtonToolTip")); //$NON-NLS-1$
		button.addActionListener(this);
		south.add(button);
		
		button = new JButton(createImageIcon("images/drop.png", "clear"));  //$NON-NLS-1$//$NON-NLS-2$
		button.setActionCommand("clear"); //$NON-NLS-1$
		button.setToolTipText(xStrings.getString("DialPanel.clearButtonToolTip")); //$NON-NLS-1$
		button.addActionListener(this);
		south.add(button);
		
		this.add(south, BorderLayout.SOUTH);
		 
	}
	
	/**
	 * Add a listener to this panel
	 * @param listener
	 */
	public void addDialListener(DialListener listener){
		
		listeners.add(listener);
		
	}
	
	/**
	 * Notify anyone listening to this dialog
	 */
	private void notifyListeners(){
		
		for(int i = 0; i < listeners.size(); i++)
			listeners.get(i).dial(dialledNumber);
		
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

	/**
	 * Dials the number shown in the text box, makes this box disappear
	 * and call originates in CallManagerPanel (if all goes well via Asterisk)
	 */
	private void dial(){
		
		dialledNumber = number.getText();
		this.setVisible(false);
		notifyListeners();
		
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		
		if(evt.getActionCommand().equals("dial")){ //$NON-NLS-1$
			dial();
		}else if(evt.getActionCommand().equals("clear")){ //$NON-NLS-1$
			
			if(number.getText().length() > 0)
				number.setText(""); //$NON-NLS-1$
			else{
				
				this.setVisible(false);
				
			}
			
		}else
			number.setText(number.getText() + evt.getActionCommand());
		
		number.requestFocus();
		
	}
	
	/** Key Listener **/
	@Override
	public void keyReleased(KeyEvent evt) {
		
		if(evt.getKeyCode() == KeyEvent.VK_ENTER)
			dial();
		
	}

	/** Unnecessary Key Listener Methods **/
	@Override
	public void keyPressed(KeyEvent evt) {}

	@Override
	public void keyTyped(KeyEvent evt) {}


}
