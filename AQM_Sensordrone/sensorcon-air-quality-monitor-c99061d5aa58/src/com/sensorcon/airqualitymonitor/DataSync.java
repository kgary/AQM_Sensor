package com.sensorcon.airqualitymonitor;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;
import android.widget.Toast;

import com.sensorcon.airqualitymonitor.database.*;
import com.sensorcon.sensordrone.DroneEventHandler;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneEventObject.droneEventType;
import com.sensorcon.sensordrone.android.Drone;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataSync extends AsyncTask<Void, Void, Void> {

	Drone myDrone;
	String sdMC = "";
	AirQualityMonitor runningApp = null;
	double locLat, locLong;
	String locMethod;
	
	Context context;

	NotificationManager notifier;
	NotificationCompat.Builder notifyLowBattery;
	NotificationCompat.Builder notiftyAirQualityModerate;
	NotificationCompat.Builder notiftyAirQualityBad;
	

	boolean measurementTimeout;
	boolean connectFailed;

	public void setContext(Context context) {
		this.context = context;
	}


	public void setSdMC(String sdMC) {
		this.sdMC = sdMC;
	}

	public DataSync(Context context) {
		this.context = context;
	}

	public DataSync(Context context, AirQualityMonitor currentApp) {
		this.context = context;
		this.runningApp = currentApp;
	}
	public void setLocation (double locLat, double locLong, String locMethod){
		this.locLat = locLat;
		this.locLong = locLong;
		this.locMethod = locMethod;
	}

	DBDateTime dateTime;
	DBCO coData;
	DBCO2 co2Data;
	DBTemperature tempData;
	DBHumidity humidityData;
	DBPressure presureData;
	String co2DevID;
	int intCoData, intCo2Data, intTemp, intHumidityData, intPressureData;

	DBDataHandler dbHandler;

	SharedPreferences myPreferences;
	Editor prefEditor;
	
	//ASU
	
	//ASU_end

	private Object lock = new Object();

	@Override
	protected Void doInBackground(Void... params) {

		if (sdMC == "") {
			return null;
		}


		notifier = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				 
		DroneEventHandler myHandler = new DroneEventHandler() {

			@Override
			public void parseEvent(DroneEventObject arg0) {

				// Program it in order...
				if (arg0.matches(droneEventType.CONNECTED)) {

					publishProgress(null);

					SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
					String currentDateandTime = sdf.format(new Date());
					dateTime = new DBDateTime();
					dateTime.setValue(currentDateandTime);

					myDrone.uartWrite("K 1\r\n".getBytes());
					myDrone.setLEDs(0, 0, 126);
					myDrone.measureBatteryVoltage();
				}

				if (arg0.matches(droneEventType.BATTERY_VOLTAGE_MEASURED)) {
					// Make a notification if the Drone's battery is low
					if (myDrone.batteryVoltage_Volts < Constants.LOW_BATTERY_NOTIFY) {
						notifyLowBattery = new Builder(context);
						notifyLowBattery.setContentTitle("Low Battery!");
						notifyLowBattery.setContentText("Your Sensordrones battery is getting low! Please charge it up.");
						notifyLowBattery.setSmallIcon(R.drawable.ic_launcher);
						notifyLowBattery.setContentIntent(emptyIntent());
						notifier.notify(Constants.NOTIFY_LOW_BATTERY, notifyLowBattery.build());
					}
					myDrone.enableTemperature();
				}

				if (arg0.matches(droneEventType.TEMPERATURE_ENABLED)) {

					myDrone.measureTemperature();
				}

				if (arg0.matches(droneEventType.TEMPERATURE_MEASURED)) {
					publishProgress(null);

					tempData = new DBTemperature();
					tempData.setValue((long) myDrone.temperature_Celsius);

					myDrone.enableHumidity();
				}

				if (arg0.matches(droneEventType.HUMIDITY_ENABLED)) {

					myDrone.measureHumidity();
				}

				if (arg0.matches(droneEventType.HUMIDITY_MEASURED)) {
					publishProgress(null);

					humidityData = new DBHumidity();
					humidityData.setValue((long) myDrone.humidity_Percent);


					myDrone.enablePressure();
				}

				if (arg0.matches(droneEventType.PRESSURE_ENABLED)) {

					// Give the Sensor a second to start up
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//
					}

					myDrone.measurePressure();
				}

				if (arg0.matches(droneEventType.PRESSURE_MEASURED)) {
					publishProgress(null);

					presureData = new DBPressure();
					presureData.setValue((long) myDrone.pressure_Pascals);


					myDrone.disablePressure();

				}

				if (arg0.matches(droneEventType.PRESSURE_DISABLED)) {

					myDrone.enablePrecisionGas();
				}

				if (arg0.matches(droneEventType.PRECISION_GAS_ENABLED)) {

					// Give the Sensor a second to start up
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						//
					}
					myDrone.measurePrecisionGas();
				}

				if (arg0.matches(droneEventType.PRECISION_GAS_MEASURED)) {
					publishProgress(null);

					coData = new DBCO();
					coData.setValue((long) myDrone.precisionGas_ppmCarbonMonoxide);


					myDrone.uartWrite("Z\r\n".getBytes());
					myDrone.uartRead();

				}

				if (arg0.matches(droneEventType.UART_READ)) {
					publishProgress(null);


					// We store -1, unless something is found
					String result = "-1";


					//					byte[] data = myDrone.uartReadBuffer.array();
					//					printPacket(data);
					//					// Clear our array in case we got multiple reads
					//					byte[] empty = {0x00};
					//					myDrone.uartReadBuffer = ByteBuffer.wrap(empty);
					//					printPacket(myDrone.uartReadBuffer.array());
					//					for (int i=0; i < data.length; i++) {
					//						if (data[i] == 0x5a && i < data.length-7) {
					//							byte[] value = {data[i+2], data[i+3], data[i+4], data[i+5], data [i+6]};
					//							result = new String(value);
					//							continue;
					//						}
					//					}

					try {
						int avail = myDrone.uartInputStream.available();
						boolean needData = true;
						for (int i=0; i < avail; i++) {

							if ((byte)myDrone.uartInputStream.read() == 0x5a && i < avail-7 && needData) {
								myDrone.uartInputStream.read();
								byte[] value = new byte[5];
								myDrone.uartInputStream.read(value);
								result = new String(value);
								i +=6;
								needData = false;
							}
						}
					} catch (IOException e1) {

					}




					myDrone.uartWrite("K 0\r\n".getBytes());
					myDrone.setLEDs(0, 0, 0);
					myDrone.disconnect();

					co2Data = new DBCO2();
					long value = Long.parseLong(result);
					co2Data.setValue(value);
					
					
					DBDataHandler myDBHandler = new DBDataHandler(context);
					myDBHandler.open();
					long id = myDBHandler.addData(dateTime, coData, co2Data, tempData, humidityData, presureData);
					//ASU
					
					intCoData = (int) coData.getValue();
					intCo2Data = (int) co2Data.getValue();
					intTemp = (int) tempData.getValue();
					intHumidityData = (int) humidityData.getValue();
					intPressureData = (int) presureData.getValue();
						
					co2DevID = "UNKNOWN";
					JSONArray json_data = new JSONArray();
					 	
						json_data.put(sdMC + id);
						json_data.put(dateTime);
						json_data.put(coData);
						json_data.put(co2Data);
						json_data.put(tempData);
						json_data.put(humidityData);
						json_data.put(presureData);
					
					JSONObject json_obj = new JSONObject();
					
					try {
						json_obj.put("deviceId", "SensorDrone" + sdMC);
						json_obj.put("geoLatitude", locLat);
						json_obj.put("geoLongitude", locLong);
						json_obj.put("geoMethod", locMethod);
						json_obj.put("dateTime", dateTime);
						json_obj.put("coData", intCoData);
						json_obj.put("co2Data", intCo2Data);
						json_obj.put("tempData", intTemp);
						json_obj.put("humidityData", intHumidityData);
						json_obj.put("presureData", intPressureData);
						json_obj.put("co2DeviceID", co2DevID);
						
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					
					Log.d("NguyenDebug","VaLues JSON hash: " + json_obj.toString());
					//Log.d("NguyenDebug","VaLues JSON array: " + json_data.toString());
					try {
						StringEntity params = new StringEntity(json_data.toString());
						//Log.d("NguyenDebug","VaLues JSON: " + params);
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					myPreferences = PreferenceManager
							.getDefaultSharedPreferences(context);
					prefEditor = myPreferences.edit();
					String post_url = myPreferences.getString("database_url", "");
					Log.d("NguyenDebug","HTTP URL: " + post_url);
					HttpClient httpClient = new DefaultHttpClient();
					
					// TODO blockout for url testing
					try {
					    //HttpPost request = new HttpPost("http://lead2.poly.asu.edu:8090/AQMEcho/aqmecho");
						HttpPost request = new HttpPost(post_url);
						StringEntity params = new StringEntity(json_obj.toString());
					    request.addHeader("content-type", "application/x-www-form-urlencoded");
					    request.setEntity(params);
					    httpClient.execute(request);
					// handle response here...
					}catch (Exception ex) {
					    // handle exception here
						Log.d("NguyenDebug","Something wrong with the web posting on: " + id);
					}finally {
						httpClient.getConnectionManager().shutdown();
					}
					
					
					//ASU_end
					/*
					Log.d("NguyenDebug","VaLues loaded into database id: " + id);
					Log.d("NguyenDebug","VaLues loaded into database dateTime: " + dateTime);
					Log.d("NguyenDebug","VaLues loaded into database coData: " + coData);
					Log.d("NguyenDebug","VaLues loaded into database co2Data: " + co2Data);
					Log.d("NguyenDebug","VaLues loaded into database tempData: " + tempData);
					Log.d("NguyenDebug","VaLues loaded into database humidityData: " + humidityData);
					Log.d("NguyenDebug","VaLues loaded into database presureData: " + presureData);
					*/
					myDBHandler.close();

					// Make a blob to get a quality assessment
					DBDataBlob statusBlob = new DBDataBlob(dateTime, coData, co2Data, humidityData, tempData, presureData);

					// Don't throw a notification if the user is measuring from main UI
					if (runningApp == null) {
						if (statusBlob.getStatus() == Constants.STATUS_GOOD) {
							// Clear notification if the reading has returned to normal
							notifier.cancel(Constants.NOTIFY_AQ_STATUS);
						} else if (statusBlob.getStatus() == Constants.STATUS_MODERATE) {
							notiftyAirQualityModerate = new Builder(context);
							notiftyAirQualityModerate.setContentTitle("Air Quality Alert!");
							notiftyAirQualityModerate.setContentText("Your air quality is Moderate!");
							notiftyAirQualityModerate.setSmallIcon(R.drawable.ic_launcher);
							notiftyAirQualityModerate.setContentIntent(emptyIntent());
							notifier.notify(Constants.NOTIFY_AQ_STATUS, notiftyAirQualityModerate.build());

						} else if (statusBlob.getStatus() == Constants.STATUS_BAD) {
							notiftyAirQualityBad = new Builder(context);
							notiftyAirQualityBad.setContentTitle("Air Quality Alert!");
							notiftyAirQualityBad.setContentText("Your air quality is Bad!");
							notiftyAirQualityBad.setSmallIcon(R.drawable.ic_launcher);
							notiftyAirQualityBad.setContentIntent(emptyIntent());
							notifier.notify(Constants.NOTIFY_AQ_STATUS, notiftyAirQualityBad.build());
						}
					}


					synchronized (lock) {
						measurementTimeout = false;
						lock.notify();
					}
				}

			}
		};

		myDrone.registerDroneListener(myHandler);

		if (myDrone.btConnect(sdMC)) {
			try {
				synchronized (lock) {
					measurementTimeout = true;
					// We'll give it 10 seconds
					// reset measurementTimeout at notify() statement 
					lock.wait(10000);
				}
			} catch (InterruptedException e) {
				return null;
			}
		} else {
			// We didn't even connect!
			connectFailed = true;
		}


		return null;
	}

	public void printPacket(byte[] packet) {
		String data = "";
		for (int i=0; i < packet.length; i++) {
			data += Integer.toHexString(packet[i] &0xff) + ":";
		}
		Log.d("AQM", data);
	}
	@Override
	protected void onPostExecute(Void result) {
		if (runningApp != null) {
			runningApp.updateDisplay();
			
			if (connectFailed) {
				Toast.makeText(runningApp.getApplicationContext(), "Connection not successful!\n\nIs your Sensordrone in range and charged up?", Toast.LENGTH_LONG).show();
			} else if (measurementTimeout) {
				Toast.makeText(runningApp.getApplicationContext(), "Measurement timed out!\n\nPerhaps your Sensordrone battery is low or you moved out of range.", Toast.LENGTH_LONG).show();
			}
			
			runningApp.setIsMeasuring(false);
		}
	}

	@Override
	protected void onPreExecute() {
		connectFailed = false;
		measurementTimeout = false;
		if (runningApp != null) {
			runningApp.setIsMeasuring(true);
		}
		// myDrone needs to be set up in a UI thread, or else you'll need to manager Looper in the doInBackground
		myDrone = new Drone();
	}
	
	@Override
	protected void onProgressUpdate(Void... values) {
		if (runningApp != null) {
			runningApp.animateFace();
		}
	}
	
	public PendingIntent emptyIntent() {
		PendingIntent empty;
		empty = PendingIntent.getActivity(context, 0, new Intent(), Intent.FLAG_ACTIVITY_NEW_TASK);
		return empty;
	}
}
