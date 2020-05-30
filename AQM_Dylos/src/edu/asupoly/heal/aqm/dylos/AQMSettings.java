package edu.asupoly.heal.aqm.dylos;

import java.io.InputStreamReader;
import java.util.Properties;


public final class AQMSettings {
	private static final String PROPERTY_FILENAME = "properties/aqm.properties";
	private Properties __globalProperties = null;
	
	// Singleton pattern. because sometimes we don't go through main
	private static AQMSettings __appSettings = null;
	
	// We could get real clever and make sure our patient and devices are in the DB.
    // Now accepting on faith
	private AQMSettings() throws Exception {
		InputStreamReader isr = null;
		try{
			isr = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(PROPERTY_FILENAME));
			__globalProperties = new Properties();
			__globalProperties.load(isr);		
		} catch (Throwable t1) {
			System.out.println("Unable to initialize AQM Service, exiting");
			t1.printStackTrace();
			System.exit(0);
		} finally {
			try{
				isr.close();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static AQMSettings getAQMSettings() {
		if(__appSettings == null) {
			try {
				__appSettings = new AQMSettings();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return __appSettings;
	}
	
	private static String getAQMProperty(String key) {
		return AQMSettings.getAQMSettings().getGlobalProperty(key);
	}

	private String getGlobalProperty(String key) {
		return __globalProperties.getProperty(key);
	}
	
	public static String getDeviceId() {
		return getAQMProperty("device.id");
	}
	
	public static String getUserId() {
		return getAQMProperty("user.id");
	}
	
	public static String getGeoLatitude() {
		return getAQMProperty("geocoordinates.latitude");
	}	
	
	public static String getGeoLongitude() {
		return getAQMProperty("geocoordinates.longitude");
	}
	
	public static String getGeoMethod() {
		return getAQMProperty("geocoordinates.method");
	}
	
}
