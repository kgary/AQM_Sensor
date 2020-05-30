package edu.asupoly.heal.aqm.dylos.monitorservice;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public final class MonitoringService {
	private static final int DEFAULT_MAX_TIMER_TASKS = 10;
	private static final int DEFAULT_INTERVAL = 10;  // in seconds
	private static final String TASK_KEY_PREFIX = "MonitorTask";
	private static final String TASKINTERVAL_KEY_PREFIX = "TaskInterval";
	private static final String PROPERTY_FILENAME = "properties/monitoringservice.properties";
	private Properties __monitorProperties = null;
	private Timer __timer;
	private HashMap<String, TimerTask> __tasks;
	
	// Singleton pattern. because sometimes we don't go through main
	private static MonitoringService __theMonitoringService = null;
	
	private MonitoringService() throws Exception {
		__timer = new Timer();
		__tasks = new HashMap<String, TimerTask>();
		int maxTimerTasks   = DEFAULT_MAX_TIMER_TASKS;
		int defaultInterval = DEFAULT_INTERVAL; // all intervals in seconds
		__monitorProperties = new Properties();
		InputStreamReader  isr = null;
		try {
			isr = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(PROPERTY_FILENAME));
			__monitorProperties.load(isr);
			maxTimerTasks = Integer.parseInt(__monitorProperties.getProperty("MaxTimerTasks"));
			defaultInterval = Integer.parseInt(__monitorProperties.getProperty("DefaultTaskInterval"));			
		} catch (NumberFormatException nfe) {
			defaultInterval = DEFAULT_INTERVAL;
			maxTimerTasks   = DEFAULT_MAX_TIMER_TASKS;
		} catch (NullPointerException npe) {
			if (__monitorProperties == null) {
				npe.printStackTrace();
			}
			defaultInterval = DEFAULT_INTERVAL;
			maxTimerTasks   = DEFAULT_MAX_TIMER_TASKS;
		} catch (Throwable t1) {
			t1.printStackTrace();
		} finally {
			try {
				if (isr != null) {
					isr.close();
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		
		// for each TimerTask indicated in the property, schedule it
		int i = 1;
		int interval = defaultInterval;
		String intervalProp = null;
		String taskClassName = __monitorProperties.getProperty(TASK_KEY_PREFIX+i);
		AQMTimerTask nextTask = null;
		while (i <= maxTimerTasks && taskClassName != null) {
			try {
				intervalProp = __monitorProperties.getProperty(TASKINTERVAL_KEY_PREFIX+i);
				if (intervalProp != null) {
					try {
						interval = Integer.parseInt(intervalProp);
					} catch (NumberFormatException nfe) {
						interval = defaultInterval;
					}
				} else {// no interval specified, use default
					interval = defaultInterval;
				}
				
				// let's create a TimerTask of that class and start it
				 Class<?> taskClass = Class.forName(taskClassName);
				 nextTask = (AQMTimerTask)taskClass.newInstance();
				 
	             if (nextTask.init(__monitorProperties)) {
	            	 __tasks.put(TASK_KEY_PREFIX+i, nextTask);
	            	 // fire up the task 1 second from now and execute at fixed delay
	                 __timer.schedule(nextTask, 1000L, interval*1000L); // repeat task in seconds
	            	 System.out.println("Created timer task " + (TASK_KEY_PREFIX+i) + " for task class " + taskClassName);
	                } else {
	                System.out.println("1. Unable to initialize MonitorService task from properties, skipping " + taskClassName);
	                }
			} catch (Throwable t) {
				t.printStackTrace();
			}
			i++;
			taskClassName = __monitorProperties.getProperty(TASK_KEY_PREFIX+i);
		}
		
	}
	
	public static MonitoringService getMonitoringService() {
		if (__theMonitoringService == null) {
			try{
				__theMonitoringService = new MonitoringService();
			} catch (Throwable t) {
				t.printStackTrace();
			}			
		}
		return __theMonitoringService;
	}
	
	public void shutdownService() throws Exception{
        // Canceling the Timer gets rid of all tasks, allowing
        // the current one to complete.
        __timer.cancel();
        // If the singleton accessor is called again it will fire up another Timer
        MonitoringService.__theMonitoringService = null;
        //System.out.println("Shutting down MonitorService");
	}
	
}
