package com.github.waynemerricks.asteriskphone.calls;

public interface AnswerListener {

	public void callAnswered(CallInfoPanel call);
	
	public void manualCallEnded();
	
}
