package com.sensorcon.airqualitymonitor;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DBUrlSetupActivity extends Activity{
	SharedPreferences myPreferences;
	Editor prefEditor;
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setContentView(R.layout.set_database_url_activity);		
			final EditText url_text = (EditText) findViewById (R.id.url);
			final Button btnSetUrl = (Button) findViewById(R.id.set_url);
			myPreferences = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			prefEditor = myPreferences.edit();	
			url_text.setText(myPreferences.getString("database_url", ""));
			btnSetUrl.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (url_text.getText().toString().matches("^\\s*$")) {
						Toast.makeText(getApplicationContext(), "Please enter a URL", Toast.LENGTH_LONG).show();
						return;
					}
					
					prefEditor.putString("database_url", url_text.getText().toString());
					prefEditor.commit();
					finish();
				}
			});
			
			
	}
}
