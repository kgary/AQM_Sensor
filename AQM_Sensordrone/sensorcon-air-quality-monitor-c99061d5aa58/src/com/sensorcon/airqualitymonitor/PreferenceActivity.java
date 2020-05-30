package com.sensorcon.airqualitymonitor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.sensorcon.sensordrone.DroneEventHandler;
import com.sensorcon.sensordrone.DroneEventObject;
import com.sensorcon.sensordrone.DroneEventObject.droneEventType;
import com.sensorcon.sensordrone.android.Drone;
import com.sensorcon.sensordrone.android.tools.DroneConnectionHelper;

public class PreferenceActivity extends Activity{

	Drone myDrone;
	DroneConnectionHelper myDroneHelper;
	DroneEventHandler myHandler;
	String storedDrone;
	
	SharedPreferences myPreferences;
	Editor prefEditor;
	Intent aqmService;
	boolean serviceStatus;

	
	Activity myActivity;
	Context myContext;
	
	RadioButton rF;
	RadioButton rC;
	int tempUnit;
	
	RadioButton rPa;
	RadioButton rhPa;
	RadioButton rkPa;
	RadioButton rAtm;
	RadioButton rmmHg;
	RadioButton rinHg;
	int pUnit;
	
	RadioButton rOff;
	RadioButton r1;
	RadioButton r5;
	RadioButton r15;
	RadioButton r30;
	RadioButton r60;
	int tUnit;

    long lastMeasure;

    Button co2BaselineButton;


	TextView tvServiceStatus;
	TextView tvStoredDrone;
    String statusMessage = "Last background measurement: ";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preference_activity);

        co2BaselineButton = (Button)findViewById(R.id.btnCOO);
        co2BaselineButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(PreferenceActivity.this);
                builder.setIcon(R.drawable.ic_launcher);
                builder.setTitle("Re-Baseline the CO2 Module");
                String msg = "Use this only if you want to re-baseline your CO2 module!\n\n" +
                        "This will re-baseline your CO2 sensor module, and set the current level to " +
                        "be around 420 ppm, which is a typical outdoor/fresh-air value.\n\n" +
                        "This calibration stored to the CO2 sensor itself, not in the app.\n\n" +
                        "Be sure your CO2 modules is plugged in, and your Sensordrone is powered up " +
                        "before continuing!";

                builder.setMessage(msg);
                builder.setPositiveButton("Re-zero CO2 module", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String MAC = myPreferences.getString(Constants.SD_MAC, "");
                        if (MAC.equals("")) {
                            AlertDialog.Builder noDrone = new AlertDialog.Builder(PreferenceActivity.this);
                            noDrone.setIcon(R.drawable.ic_launcher);
                            noDrone.setTitle("Drone not set up!");
                            noDrone.setMessage("It seems you haven't set you Sensordrone up yet! Please do that " +
                                    "first");
                            noDrone.create().show();
                        }
                        Drone aDrone = new Drone();
                        if (aDrone.btConnect(MAC)) {
                            aDrone.setLEDs(0,0,125);
                            aDrone.uartWriteForRead("K 1\r\n".getBytes(), 250);
                            byte[] response = aDrone.uartWriteForRead("G\r\n".getBytes(), 500);
                            aDrone.uartWriteForRead("K 0\r\n".getBytes(), 250);
                            String moduleResponse = new String(response);

                            if (moduleResponse.contains("locked") || !moduleResponse.contains("G")) {
                                AlertDialog.Builder noDrone = new AlertDialog.Builder(PreferenceActivity.this);
                                noDrone.setIcon(R.drawable.ic_launcher);
                                noDrone.setTitle("Error!");
                                noDrone.setMessage("There was an error communicating with the CO2 sensor! Please " +
                                        "make sure it is plugged in, and try again.");
                                noDrone.create().show();
                            } else {
                                AlertDialog.Builder noDrone = new AlertDialog.Builder(PreferenceActivity.this);
                                noDrone.setIcon(R.drawable.ic_launcher);
                                noDrone.setTitle("Success!");
                                noDrone.setMessage("Your CO2 module should now be re-baselined");
                                noDrone.create().show();
                            }

                            aDrone.setLEDs(0,0,0);
                            aDrone.disconnect();
                        }
                        else {
                            AlertDialog.Builder noDrone = new AlertDialog.Builder(PreferenceActivity.this);
                            noDrone.setIcon(R.drawable.ic_launcher);
                            noDrone.setTitle("Couldn't Connect!");
                            noDrone.setMessage("Could not connect to your Sensordrone. Please try again!");
                            noDrone.create().show();
                        }

                        dialog.dismiss();
                    }
                });

                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialog = builder.create();
                dialog.show();
            }
        });
		
		tvStoredDrone = (TextView)findViewById(R.id.tvServiceDrone);
		tvServiceStatus = (TextView)findViewById(R.id.tvServiceStatus);
		
		// Handy to have
		myActivity = this;
		myContext = this;
		
		// Preferences
		myPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefEditor = myPreferences.edit();
		
		getPreferences();
		
		//ASU
		final Button btnSetUrl = (Button) findViewById(R.id.btnSetUrl);
		btnSetUrl.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//Launch set_database_url_activity
				Intent setup_db_url = new Intent(getApplicationContext(), DBUrlSetupActivity.class);
				startActivity(setup_db_url);
			}
		});
		//ASU_end
		
		// Our background monitoring service
		aqmService = new Intent(getApplicationContext(), DroneAQMService.class);

		// Were we started with the intention of setting up the drone?
		Intent myIntent = getIntent();
		if (myIntent != null) {
			boolean needsSetup = myIntent.getBooleanExtra(Constants.NEEDS_SETUP, false);
			if (needsSetup) {
				setupDrone(true);
			}
		}// End of Intent Checking
		
		// Temperature Radios
		rF = (RadioButton)findViewById(R.id.radioF);
		rF.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tempUnit = Constants.FAHRENHEIT;
				prefEditor.putInt(Constants.TEMPERATURE_UNIT, tempUnit);
				prefEditor.commit();
			}
		});
		rC = (RadioButton)findViewById(R.id.radioC);
		rC.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tempUnit = Constants.CELCIUS;
				prefEditor.putInt(Constants.TEMPERATURE_UNIT, tempUnit);
				prefEditor.commit();
			}
		});
		
		if (tempUnit == Constants.FAHRENHEIT) {
			rF.setChecked(true);
		} else {
			rC.setChecked(true);
		}
		
		// Pressure radios
		rPa = (RadioButton)findViewById(R.id.radioPa);
		rPa.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.PASCAL;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		rhPa = (RadioButton)findViewById(R.id.radiohPa);
		rhPa.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.HECTOPASCAL;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		rkPa = (RadioButton)findViewById(R.id.radiokPa);
		rkPa.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.KILOPASCAL;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		rAtm = (RadioButton)findViewById(R.id.radioAtm);
		rAtm.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.ATMOSPHERE;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		rmmHg = (RadioButton)findViewById(R.id.radiommHg);
		rmmHg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.MMHG;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		rinHg = (RadioButton)findViewById(R.id.radioinHg);
		rinHg.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				pUnit = Constants.INHG;
				prefEditor.putInt(Constants.PRESSURE_UNIT, pUnit);
				prefEditor.commit();
			}
		});
		
		if (pUnit == Constants.HECTOPASCAL) {
			rhPa.setChecked(true);
		} else if (pUnit == Constants.KILOPASCAL) {
			rkPa.setChecked(true);
		} else if (pUnit == Constants.ATMOSPHERE) {
			rAtm.setChecked(true);
		} else if (pUnit == Constants.MMHG) {
			rmmHg.setChecked(true);
		} else if (pUnit == Constants.INHG) {
			rinHg.setChecked(true);
		} else {
			rPa.setChecked(true);
		}
		
		// Time Radios
		rOff = (RadioButton)findViewById(R.id.radioIOff);
		rOff.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                tUnit = 0;
                prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
                prefEditor.commit();
//				stopService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_OFF);
                alarmOff();
                updateServiceMessage();
				Toast.makeText(myContext, "Monitoring service has been stopped", Toast.LENGTH_LONG).show();
			}
		});
		r1 = (RadioButton)findViewById(R.id.radioI1);
		r1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tUnit = Constants.MINUTES_1;
				prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
				prefEditor.commit();
//				stopService(aqmService);
//				startService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_ON);
                alarmReset();
                updateServiceMessageNow();
				Toast.makeText(myContext, "Monitoring service has been started", Toast.LENGTH_LONG).show();
			}
		});
		r5 = (RadioButton)findViewById(R.id.radioI5);
		r5.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tUnit = Constants.MINUTES_5;
				prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
				prefEditor.commit();
//				stopService(aqmService);
//				startService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_ON);
                alarmReset();
                updateServiceMessageNow();
				Toast.makeText(myContext, "Monitoring service has been started", Toast.LENGTH_LONG).show();
			}
		});
		r15 = (RadioButton)findViewById(R.id.radioI15);
		r15.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tUnit = Constants.MINUTES_15;
				prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
				prefEditor.commit();
//				stopService(aqmService);
//				startService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_ON);
                alarmReset();
                updateServiceMessageNow();
				Toast.makeText(myContext, "Monitoring service has been started", Toast.LENGTH_LONG).show();
			}
		});
		r30 = (RadioButton)findViewById(R.id.radioI30);
		r30.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tUnit = Constants.MINUTES_30;
				prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
				prefEditor.commit();
//				stopService(aqmService);
//				startService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_ON);
                alarmReset();
                updateServiceMessageNow();
				Toast.makeText(myContext, "Monitoring service has been started", Toast.LENGTH_LONG).show();
			}
		});
		r60 = (RadioButton)findViewById(R.id.radioI60);
		r60.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				tUnit = Constants.MINUTES_60;
				prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
				prefEditor.commit();
//				stopService(aqmService);
//				startService(aqmService);
//				tvServiceStatus.setText(Constants.SERVICE_ON);
                alarmReset();
                updateServiceMessageNow();
				Toast.makeText(myContext, "Monitoring service has been started", Toast.LENGTH_LONG).show();
			}
		});

        if (tUnit == Constants.MINUTES_1) {
            r1.setChecked(true);
        }
        else if (tUnit == Constants.MINUTES_5) {
            r5.setChecked(true);
        }
        else if (tUnit == Constants.MINUTES_15) {
            r15.setChecked(true);
        }
        else if (tUnit == Constants.MINUTES_30) {
            r30.setChecked(true);
        }
        else if (tUnit == Constants.MINUTES_60) {
            r60.setChecked(true);
        } else {
            rOff.setChecked(true);
        }

		
//		// Is the service running?
//		if (serviceStatus) {
//			tvServiceStatus.setText(Constants.SERVICE_ON);
//		} else {
//			tvServiceStatus.setText(Constants.SERVICE_OFF);
//		}



        updateServiceMessage();


        // What Sensordrone is stored?
		tvStoredDrone.setText(storedDrone);
	}
	
	public void getPreferences() {
		tempUnit = myPreferences.getInt(Constants.TEMPERATURE_UNIT, 1); // Default C
		pUnit = myPreferences.getInt(Constants.PRESSURE_UNIT, 0); // Default Pa
		tUnit = myPreferences.getInt(Constants.TIME_INTERVAL, 60); // Default 60 minutes
		serviceStatus = myPreferences.getBoolean(Constants.SERVICE_STATUS, false); 
		storedDrone = myPreferences.getString(Constants.SD_MAC, "Not Paired");
        lastMeasure = myPreferences.getLong(Constants.LAST_MEASURE, 0);

    }
	
	public void setupDrone(final boolean fromIntent) {
		myDrone = new Drone();
		myDroneHelper = new DroneConnectionHelper();
		myHandler = new DroneEventHandler() {
			
			@Override
			public void parseEvent(DroneEventObject arg0) {
				if (arg0.matches(droneEventType.CONNECTED)) {
					prefEditor.putString(Constants.SD_MAC, myDrone.lastMAC);
					prefEditor.commit();
					myDrone.disconnect();
					Toast.makeText(getApplicationContext(), "Sensordrone Saved!", Toast.LENGTH_SHORT).show();
					tvStoredDrone.setText(myDrone.lastMAC);
					myDrone.unregisterDroneListener(myHandler);
					// If we were set up from an intent, trigger a measurement
					if (fromIntent) {
						DataSync getData = new DataSync(getApplicationContext(), null);
						getData.setSdMC(myDrone.lastMAC);
						getData.execute();
					}
				}
			}
		};
		myDrone.registerDroneListener(myHandler);
        myDroneHelper.connectFromPairedDevices(myDrone, this);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.pref_menu, menu);
		return true;
	}
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.stupDrn:
			setupDrone(false);
			break;
		case R.id.prefHelp:
			TxtReader help = new TxtReader(myContext);
			help.displayTxtAlert("Settings", R.raw.settings_help);
			break;
		}
		return true;
	}

    private void alarmReset() {
        DroneAlarm droneTask = new DroneAlarm();
        // cancel any old one
        droneTask.CancelAlarm(getApplicationContext());
        // start a new one
        droneTask.setAlarm(getApplicationContext());
    }

    private void alarmOff() {
        DroneAlarm droneTask = new DroneAlarm();
        // cancel any old one
        droneTask.CancelAlarm(getApplicationContext());
    }

    private void updateServiceMessageNow() {
        tvServiceStatus.setText(statusMessage + " Just now.");
    }

    private void updateServiceMessage() {
        getPreferences();
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastMeasure;
        long fiveMinutes = 300000;
        if (lastMeasure == 0) {
            tvServiceStatus.setText(statusMessage + "Off");
        }
        else if (deltaTime > tUnit + fiveMinutes) {
            // Assume the worst!
            alarmOff();
            tvServiceStatus.setText(statusMessage + "Off");
            if (rOff != null) {
                rOff.setChecked(true);
            }
            tUnit = 0;
            prefEditor.putInt(Constants.TIME_INTERVAL, tUnit);
            prefEditor.commit();
        }
        else {
            tvServiceStatus.setText(statusMessage + String.format(" %.0f minute(s) ago", deltaTime/1000.0/60.));
        }
    }
}
