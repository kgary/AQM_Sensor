package edu.asupoly.heal.aqm.dylos.monitorservice;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.json.simple.JSONArray;

import edu.asupoly.heal.aqm.dylos.AQMSettings;
import edu.asupoly.heal.aqm.dmp.AQMDAOFactory;
import edu.asupoly.heal.aqm.dmp.IAQMDAO;
import edu.asupoly.heal.aqm.model.ServerPushEvent;


public class ServerPushTask extends AQMTimerTask {
	public static final int PUSH_UNSET = 0;
	public static final int PUSH_BAD_RESPONSE_CODE = -101;
	public static final int PUSH_MALFORMED_URL = -102;
    public static final int PUSH_UNABLE_TO_CONNECT = -100;
	public static final int SERVER_DYLOS_IMPORT_FAILED = -20;
	public static final int SERVER_SENSORDRONE_IMPORT_FAILED = -30;
	public static final int SERVER_BAD_OBJECT_TYPE = -2;
	public static final int SERVER_STREAM_ERROR = -1;
	public static final int SERVER_STREAM_CORRUPTED_EXCEPTION = -10;
	public static final int SERVER_IO_EXCEPTION = -11;
	public static final int SERVER_SECURITY_EXCEPTION = -12;
	public static final int SERVER_NULL_POINTER_EXCEPTION = -13;
	public static final int SERVER_UNKNOWN_ERROR = -99;
	
	public static final int AIR_QUALITY_READINGS_TYPE = 1;
    private Properties __props;
    private String __pushURL;
    protected Date _lastReadTime;
    
    public ServerPushTask() {
        super();
    }  
    
	@Override
	public boolean init(Properties props) {
        boolean rval = true;
        
        __props = new Properties();  // need this even if not using here
        String userId = AQMSettings.getUserId();
		__pushURL = props.getProperty("push.url");
		
        if (userId != null && __pushURL != null || __pushURL.length() >= 12) { // must be at least http://x.yyy
            __props.setProperty("userId", userId);
        } else {
            rval = false;
        }
        _isInitialized = rval;
        
        // This section tries to initialize the last reading date
        Date lastReadTime = new Date(0L);  // Jan 1 1970, 00:00:00
        _lastReadTime = new Date();
        try {
        	IAQMDAO dao = AQMDAOFactory.getDAO();
            ServerPushEvent spe = dao.getLastServerPush();
            if (spe != null) {
            	String dateTime = spe.getEventTime();
            	_lastReadTime = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy",
						Locale.US).parse(dateTime);
                System.out.println("Last server push " + _lastReadTime.toString());
            } else {
            	_lastReadTime = lastReadTime;
                System.out.println("Last server push unknown, using " + lastReadTime.toString());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.out.println("Unable to get last server push time, using " + lastReadTime.toString());
        }
  
        return _isInitialized;
	}

	@Override
	public void run() {
        if (_isInitialized) {
            System.out.println("MonitorService: executing AQMonitor ServerPushTask");
            Date d = new Date(System.currentTimeMillis());

            try {
            	IAQMDAO dao = AQMDAOFactory.getDAO();
                System.out.println("Checking for air quality readings between " + 
                		_lastReadTime.toString() + " and " + d.toString());
                JSONArray toImport = new JSONArray();
                toImport = dao.findDylosReadingsForUserBetween(__props.getProperty("userId"),
                		_lastReadTime, d);

                int rval = 0;
                if (__pushURL != null) {
                    if (toImport != null && toImport.size() > 0) {                                
                    	StringWriter json = new StringWriter();
                    	toImport.writeJSONString(json);
                    	rval = __pushToServer(json.toString(), "dylos");
                    	__recordResult(dao, d, rval, AIR_QUALITY_READINGS_TYPE, "DylosReadings");
                    } else {
                    	System.out.println(" No Air Quality Readings to push");
                    }
                }
            } catch (Throwable t) {
            	System.out.println("Error pushing to the server " + t.getMessage());
            }
        }
		
	}
	
	private void __recordResult(IAQMDAO dao, Date d, int rval, int type, String label) {
        String msg = "";
        if (rval >= 0) {
            msg = "Pushed " + rval + " " + label + " to the server";            
        } else {
            msg = "Unable to push " + label + " to the server";
        }
        System.out.println(msg);

        _lastReadTime = d;    // whether we are successful or not we update the date.
        					// otherwise this could happen over and over (say, UNIQUE constraint)
        try {
            dao.addPushEvent(new ServerPushEvent(d.toString(), rval, type, msg));
        } catch (Throwable ts) {
        	ts.printStackTrace();
        	System.out.println("Unable to record " + label + " push event");
        }
    }
		
	private int __pushToServer(String jsonString, String type) throws Exception {
        HttpURLConnection urlConn = null;
        DataOutputStream output = null;
        BufferedReader br = null;
        
        String url = __pushURL+"?type="+type;
		//http://localhost:8080/AQM/AQMImport?type=dylos
		//String url = "http://localhost:8081/AQM/AQMImport?type=dylos";// for TCP/TP Monitor test
		
		//*******for sensordrone json string test
     // {"deviceId":"SensorDroneB8:FF:FE:B9:D9:A0","dateTime":"20140313_195444",
    	// "co2DeviceID":"UNKNOWN","coData":-2,"co2Data":-1,
    	// "presureData":96128,"tempData":27,"humidityData":42,
    	// "geoLatitude":33.2830173,"geoLongitude":-111.7627723,"geoMethod":"Network"}
/*		url = __pushURL+"?type="+"sensordrone";
		JSONObject jsensordrone = new JSONObject();
		jsensordrone.put("deviceId", "SensorDroneB8:FF:FE:B9:D9:A0");
		jsensordrone.put("geoLongitude", new Double(111.7627723));
		jsensordrone.put("coData", new Integer(-2));
		jsensordrone.put("dateTime", "20140307_114406");
		jsensordrone.put("geoMethod", "Network");
		jsensordrone.put("co2Data", new Integer(-1));
		jsensordrone.put("co2DeviceID", "UNKNOWN");
		jsensordrone.put("presureData", new Integer(96332));
		jsensordrone.put("tempData", new Integer(22));
		jsensordrone.put("humidityData", new Integer(43));
		jsensordrone.put("geoLatitude", new Double(33.2830173));*/
		//*********************************
        

        int rval = 0;
        try {
            System.out.println("Pushing to server" + url);
            urlConn = (HttpURLConnection) new URL(url).openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestMethod("POST");
			urlConn.setRequestProperty("Content-Type", "application/json;charset=utf-8");//tell the server to expect a JSON Object
            urlConn.connect();
			
            output = new DataOutputStream(urlConn.getOutputStream());
            
            output.writeBytes(jsonString);
            //output.writeBytes(jsensordrone.toString());// sensordrong test!
            
            output.flush();
            output.close();
            System.out.println("Push complete " + url);
            
            // Process the response
            if (urlConn.getResponseCode() != 200) {
                throw new Exception("Did not receive OK from server for request");
            } else {
                // Get the return value, the response of doPost()
                br = new BufferedReader(new InputStreamReader(new DataInputStream (urlConn.getInputStream())));
                String str = br.readLine();
                try {
                    rval = Integer.parseInt(str);
                } catch (NumberFormatException nfe) {
                	nfe.printStackTrace();
                	System.out.println("Unable to convert server response to return code");
                    rval = PUSH_BAD_RESPONSE_CODE;
                }
            }
        } catch (MalformedURLException mue) {
            System.out.println("Malformed URL " + url);
            mue.printStackTrace();
            rval = PUSH_MALFORMED_URL;
        } catch (Throwable t) {
            System.out.println("Error trying to connect to push server");
            t.printStackTrace();
            rval = PUSH_UNABLE_TO_CONNECT;
        } finally {
            try {
                if (br != null) br.close();
                if (output != null) output.close();
            } catch (Throwable t2) {
            	t2.printStackTrace();
                System.out.println("Unable to close Object Output Stream");
            }
        }
        __logReturnValue(rval);
        return rval;   
	}
	
	private void __logReturnValue(int rval) {
        // This is a total hack right now
		System.out.println("Return code from server push: " + rval);
        if (rval > 0) System.out.println("This is the number of elements pushed successfully");
        else if (rval == 0) System.out.println("Server did not think there was anything to push");
        else if (rval <= -100) System.out.println("Some error on the client prevented server push round trip");
        else if (rval <= -90) System.out.println("Server side servlet error");
        else if (rval <= -30) System.out.println("Could not push Sensordrone Readings");
        else if (rval <= -20) System.out.println("Could not push Dylos Readings");
        else if (rval <= -10) System.out.println("Server encountered an exception");
        else if (rval < 0) System.out.println("Server encountered parameters it did not understand");
	}

}
