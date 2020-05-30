package edu.asupoly.heal.aqm.dylos.monitorservice;

import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import org.json.simple.JSONArray;

import edu.asupoly.heal.aqm.dylos.AQMSettings;
import edu.asupoly.heal.aqm.dmp.AQMDAOFactory;
import edu.asupoly.heal.aqm.dmp.IAQMDAO;
import edu.asupoly.heal.aqm.model.DylosReading;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;


public class AQMSerialPortTask extends AQMTimerTask {
    private static final int BAUD_RATE = SerialPort.BAUDRATE_9600;
    private static final int DATA_BITS = SerialPort.DATABITS_8;
    private static final int STOP_BITS = SerialPort.STOPBITS_1;
    private static final int PARITY    = SerialPort.PARITY_NONE;
    private static final int MASK      = 
            SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;
    private static final int TIMEOUT   = 500; // 1/2 sec timeout on serial port read
    
    private SerialPort    __serialPort;
    private Properties    __props;
    private JSONArray     __aqReadings = new JSONArray();
    private String        __deviceId;
    private String        __userId;
    private String 		  __geoLatitude;
    private String		  __geoLongitude;
    private String		  __geoMethod;
    
    public AQMSerialPortTask() {
        super();
    }
    
	@Override
	public boolean init(Properties props) {
		boolean rval = true;
		__props = new Properties();
		
        String deviceId     = AQMSettings.getDeviceId();
        String userId    = AQMSettings.getUserId();
        String geoLatitude = AQMSettings.getGeoLatitude();
        String geoLongitude = AQMSettings.getGeoLongitude();
        String geoMethod = AQMSettings.getGeoMethod();
        
        String serialPort   = props.getProperty("aqmSerialPort");
        
        if (deviceId != null && userId != null && serialPort != null && geoLatitude != null && geoLongitude != null) {
            __deviceId  = deviceId;
            __userId = userId;
            __geoLatitude = geoLatitude;
            __geoLongitude = geoLongitude;
            __geoMethod = geoMethod;
            __props.setProperty("deviceid", deviceId);
            __props.setProperty("userid", userId);
            __props.setProperty("aqmSerialPort", serialPort);
            
            try {
            	// this section initializes the serialPort
            	__serialPort = new SerialPort(serialPort);
            	rval = __serialPort.openPort();
            	if(rval) {
                    __serialPort.setParams(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY);
                    __serialPort.setEventsMask(MASK);
                    // we purge initially to try and clear buffers
                    __serialPort.purgePort(SerialPort.PURGE_RXCLEAR | SerialPort.PURGE_TXCLEAR);
                    __serialPort.addEventListener(new AQMDylosSerialEventListener());
            	}
            } catch (SerialPortException spe) {
            	spe.printStackTrace();
            	rval = false;
            }

        } else {
        	rval = false;
        }
        _isInitialized = rval;
	
		return rval;
	}

	@Override
	public void run() {
        if (_isInitialized) {
            System.out.println("MonitorService: executing AQMonitor Serial Port Reading Task");
            synchronized(__aqReadings) {
                if (__aqReadings.size() > 0) {
                	IAQMDAO dao = AQMDAOFactory.getDAO();
                    try {
                    	StringWriter json = new StringWriter();
                    	__aqReadings.writeJSONString(json);
                        if (dao.importDylosReading(json.toString())) {
                            System.out.println("Imported AQ Readings " + __aqReadings.size());
                            // clear out __aqReadings by creating a new one
                            __aqReadings = new JSONArray();
                        } else { // If the DAO didn't work we keep the air quality readings for the next try
                        	System.out.println("Failed to Import AQ Readings in DAO");
                        }                     
                    } catch (Exception e) {
                    	System.out.println("Exception writing serial ParticleReadings to database");
                    }                    
                }
            }
        } else {
        	System.out.println("MonitorService: trying to execute uninitialized AQMonitor Serial Port Reading Task");
        }
		
	}
	
    @Override
    public void finalize() {
        try {
            if (__serialPort.isOpened()) {
                if (__serialPort.closePort()) System.out.println("CLOSED SERIAL PORT");
                else System.out.println("Serial port already closed!");
            }
        } catch (Throwable t) {
            // silently discard
        }
    }
	
    // nested class
    class AQMDylosSerialEventListener implements SerialPortEventListener {
        private StringBuffer __serialBuffer = new StringBuffer();
        @SuppressWarnings("unchecked")
        @Override
        public void serialEvent(SerialPortEvent event) {
            if(event.isRXCHAR()){//If data is available
                if(event.getEventValue() > 0){//Check bytes count in the input buffer
                    //Read data 
                    try {
                        byte buffer[] = __serialPort.readBytes(event.getEventValue(), TIMEOUT);
                        
                        // Need to convert what is in our buffer to a ParticleReading
                        // each reading format is <small>,<large>CRLF  (CRLF = \r\n)
                        // 4 bytes = 1 int in Java, char is 2 bytes, so we'll scan 2 bytes at a time

                        // This is not the most efficient conversion but it is the simplest
                        // We convert the byte buffer to a StringBuffer
                        String toProcess = null;
                        __serialBuffer.append(new String(buffer));
                        int lastIndexOf = __serialBuffer.lastIndexOf("\r\n");
                        if (lastIndexOf != -1) {
                            toProcess = __serialBuffer.substring(0, lastIndexOf);
                            __serialBuffer.delete(0, lastIndexOf+1);
                        }
                        // then we process the StringBuffer up to the last CRLF
                        if (toProcess != null) {
                            StringTokenizer st  = new StringTokenizer(toProcess, "\r\n");
                            StringTokenizer st2 = null;
                            synchronized (__aqReadings) {
                                while (st.hasMoreTokens()) {
                                    st2 = new StringTokenizer(st.nextToken(), ",");
                                    if (st2.countTokens() == 2) { // should always be the case
                                    	DylosReading pr = new DylosReading(__deviceId, __userId, new Date().toString(), 
                                                Integer.parseInt(st2.nextToken()), Integer.parseInt(st2.nextToken()), Double.parseDouble(__geoLatitude), Double.parseDouble(__geoLongitude), __geoMethod);
                                        __aqReadings.add(pr);
                                        System.out.println("Importing ParticleReading " + pr.toString());
                                    } else {
                                        System.out.println("Unable to process serial port event in AQM Timer Task");
                                    }
                                }
                            } //synch on __aqReadings
                        }
                    }
                    catch (SerialPortException ex) {
                        ex.printStackTrace();
                    }
                    catch (SerialPortTimeoutException spte) {
                    	spte.printStackTrace();
                    }
                    catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            }
        }
    }  // end nested class

}
