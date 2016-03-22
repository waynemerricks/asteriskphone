package com.thevoiceasia.phonebox.calls;

public interface AnswerListener {

	public void callAnswered(CallInfoPanel call);
	
	public void manualCallEnded();
	
}
