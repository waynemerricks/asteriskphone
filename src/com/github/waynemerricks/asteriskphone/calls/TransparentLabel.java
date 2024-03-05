package com.github.waynemerricks.asteriskphone.calls;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.LineBorder;

public class TransparentLabel extends JLabel {

	/** STATICS **/
	private static final long serialVersionUID = 1L;

	public TransparentLabel() {
		setTransparent();
	}

	public TransparentLabel(String text) {
		super(text);
		setTransparent();
	}

	public TransparentLabel(Icon image) {
		super(image);
		setTransparent();
	}

	public TransparentLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
		setTransparent();
	}

	public TransparentLabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
		setTransparent();
	}

	public TransparentLabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
		setTransparent();
	}
	
	private void setTransparent(){
		
		this.setOpaque(false);
		this.setBorder(new LineBorder(Color.BLACK));
		
	}

}
