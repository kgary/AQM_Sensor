package edu.asupoly.heal.aqm.dylos.monitorservice;

import java.util.Properties;
import java.util.TimerTask;

public abstract class AQMTimerTask extends TimerTask {
	protected boolean _isInitialized;
	
	protected AQMTimerTask() {
        super();
        _isInitialized = false;
    }
	
	// Init should read whatever properties it expects in the subclass
    public abstract boolean init(Properties props);
}
