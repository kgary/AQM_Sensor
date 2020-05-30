package com.sensorcon.airqualitymonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.concurrent.ExecutionException;


public class DroneAlarm extends BroadcastReceiver {

	SharedPreferences myPreferences;

	AlarmManager myAlarmManager;
	Intent myIntent;
	PendingIntent myPendingIntent;
	double locLat, locLong;
	String locMethod;

	public DroneAlarm() {

	}

	@Override
	public void onReceive(Context context, Intent intent) {

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
		wl.acquire();

		myPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		String MAC = myPreferences.getString(Constants.SD_MAC, "");
		if (MAC.equals("")) {
			if (wl.isHeld()) {
				wl.release();
			}
			return;
		}


		DataSync droneTask = new DataSync(context);
		// Nguyen Added
		GeoLocation geoData = new GeoLocation(context);
		geoData.getLocation();

		if (geoData.canGetLocation()) {
			Log.d("NguyenDebug", "geoData is enabled");
			locLat = geoData.getLatitude();
			locLong = geoData.getLongitude();
			locMethod = geoData.getGeoMethod();
			Log.d("NguyenDebug", "Activity nLatitude is " + locLat);
			Log.d("NguyenDebug", "Activity Longitude is " + locLong);
			Log.d("NguyenDebug", "Activity LocMethod is " + locMethod);
		} else {Log.d("NguyenDebug", "geoData is DISABLED");}
		droneTask.setLocation(locLat, locLong, locMethod);
		// Nguyen
		droneTask.setSdMC(MAC);
		droneTask.execute();

		// Block for result
		try {
			droneTask.get();

            // Store the last time we measured data
            SharedPreferences.Editor myEditor = myPreferences.edit();
            long currentTime = System.currentTimeMillis();
            myEditor.putLong(Constants.LAST_MEASURE,currentTime);
            myEditor.commit();
            Log.d("XXX", "TIMESTAMPED! " + currentTime);

		} catch (InterruptedException e) {
			if (wl.isHeld()) {
				wl.release();
			}		
		} catch (ExecutionException e) {
			if (wl.isHeld()) {
				wl.release();
			}	
		}

		// Release the WakeLock
		if (wl.isHeld()) {
			wl.release();
		}


	}

	public void setAlarm(Context context) {
		AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(context, DroneAlarm.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		int minutes = myPreferences.getInt(Constants.TIME_INTERVAL, 60) *  1000 * 60;
		am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), minutes, pi); // Millisec * Second * Minute		
	}

	public void CancelAlarm(Context context) {
		Intent intent = new Intent(context, DroneAlarm.class);
		PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(sender);
        // Update our shared preferences
        myPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor myEditor = myPreferences.edit();
        myEditor.putLong(Constants.LAST_MEASURE, 0);
        myEditor.commit();
	}

}
