package com.github.waynemerricks.asteriskphone.calls;

import java.awt.Font;

import javax.swing.Icon;

public class BoldLabel extends TransparentLabel {

	/** STATICS */
	private static final long serialVersionUID = 1L;

	
	public BoldLabel() {
		setBold();
	}

	public BoldLabel(String text) {
		super(text);
		setBold();
	}

	public BoldLabel(Icon image) {
		super(image);
		setBold();
	}

	public BoldLabel(String text, int horizontalAlignment) {
		super(text, horizontalAlignment);
		setBold();
	}

	public BoldLabel(Icon image, int horizontalAlignment) {
		super(image, horizontalAlignment);
		setBold();
	}

	public BoldLabel(String text, Icon icon, int horizontalAlignment) {
		super(text, icon, horizontalAlignment);
		setBold();
	}
	
	private void setBold(){
		
		this.setFont(this.getFont().deriveFont(Font.BOLD));
		
	}

}
