package edu.asupoly.heal.aqm.dylos;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.GroupLayout.Alignment;

import edu.asupoly.heal.aqm.dylos.monitorservice.MonitoringService;

@SuppressWarnings("serial")
public class AQMGUI extends javax.swing.JFrame {
    public AQMGUI(String title){
        super(title);

        //setAlwaysOnTop(true);
        initComponents();
    }
    
    public class AspiraGUICloseListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            if (JOptionPane.showConfirmDialog(AQMGUI.this,
                    "Are you sure you want to exit? Any unsaved changes will be lost!",
                    "Confirm Exit", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)
                    != JOptionPane.YES_OPTION) {
                return;
            }
            AQMGUI.thisFrame.dispose();
        }
    }
    
    private void initComponents() {
    	try {
    		//this.addWindowListener(new AspiraGUICloseListener());
    		//setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    		
    		jTabbedPane1 = new JTabbedPane();
    		jTabbedPane1.setMaximumSize(new Dimension(33000, 30000));
    		startButton = new JButton("Start");
    		stopButton = new JButton("Exit");
    		stopButton.setEnabled(false);
    		
    		worker = new AQMStartWorker();
    		
    		startButton.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
					worker.execute();
					startButton.setEnabled(false);
					stopButton.setEnabled(true);
    			}
    		});

    		stopButton.addActionListener(new ActionListener() {
    			public void actionPerformed(ActionEvent e) {
    				worker.cancel(true);
    				timer.stop();
    			}
    		});
    		
    		GroupLayout layout= new GroupLayout(getContentPane());
            layout.setHorizontalGroup(
                    layout.createParallelGroup(Alignment.LEADING)
                    	.addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jTabbedPane1, GroupLayout.PREFERRED_SIZE, 782, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(19, Short.MAX_VALUE))
                    	.addGroup(layout.createSequentialGroup()
                    		.addGap(350)
                            .addComponent(startButton)
                            .addGap(18)
                            .addComponent(stopButton))           	
                    );
            layout.setVerticalGroup(
                    //layout.createParallelGroup(Alignment.LEADING)
            		layout.createSequentialGroup()
                    	.addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jTabbedPane1, GroupLayout.DEFAULT_SIZE, 506, Short.MAX_VALUE)
                            .addContainerGap())
                        .addGroup(layout.createParallelGroup(Alignment.LEADING)
                        	.addComponent(startButton)
                        	.addComponent(stopButton)) 
                        .addContainerGap()
                    );
            getContentPane().setLayout(layout);   		
    		
            jTabbedPane1.addTab("Aspira", __createPropsPanel("properties/aqm.properties"));
            jTabbedPane1.addTab("Monitoring", __createPropsPanel("properties/monitoringservice.properties"));
    		
            pack();
    		
    		
    	} catch (Throwable tall) {
    		tall.printStackTrace();
    		System.out.println("Error initializing Admin application in initComponents, exiting");
    		System.exit(0);
    	}
    }
    
    private javax.swing.JPanel __createPropsPanel(String filename) {
        final String fname = filename;
        javax.swing.JPanel propsPanel = new javax.swing.JPanel();
        final Properties props = readProps(fname);
        final List<PropertyFileData> propData = __convertPropertiesToList(props);
        propsPanel.setLayout(new GridLayout(0, 2)); 
        Iterator<PropertyFileData> iter = propData.iterator();
        while (iter.hasNext()) {
            PropertyFileData next = iter.next();
            propsPanel.add(new JLabel(next.propertyName));
            propsPanel.add(next.propertyValue);
        }
        propsPanel.add(new JLabel(""));
        propsPanel.add(new JLabel(""));
        propsPanel.add(new JLabel(""));
        JButton sButton = new JButton("Save");
        propsPanel.add(sButton);
        sButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                PrintWriter pw = null;
                try {
                    URL url = this.getClass().getClassLoader().getResource(fname);
                    File file = new File(url.toURI().getPath());
                    pw = new PrintWriter(new FileOutputStream(file));
                    Iterator<PropertyFileData> iter = propData.iterator();
                    while (iter.hasNext()) {
                        PropertyFileData entry = iter.next();
                        pw.println(entry.propertyName+"="+entry.propertyValue.getText());
                    }
                    JOptionPane.showMessageDialog(AQMGUI.this, 
                            "Properties saved, restart Aspira services", "Properties saved", 
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (Throwable t) {
                    //LOGGER.log(Level.SEVERE, "Unable to write properties file " + fname);
                	t.printStackTrace();
                    JOptionPane.showMessageDialog(AQMGUI.this, 
                            "Unable to write properties file " + fname, "Properties not saved", 
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    try {
                        if (pw != null) pw.close();                        
                    } catch (Throwable t) {
                        //LOGGER.log(Level.WARNING, "Unable to close output stream " + fname);
                    	t.printStackTrace();
                    }
                }
            }
        }
        );
 
        return propsPanel;
    }   
    
    private Properties readProps(String filename) {
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(filename));
			Properties p = new Properties();
			p.load(isr);
			return p;
		} catch (Throwable t) {
			// LOGGER.log(Level.SEVERE, "Unable to load properties from filename);
			t.printStackTrace();
			return null;
		}
	}
    
    private class PropertyFileData {
        String propertyName;
        JTextField propertyValue;
        PropertyFileData(String key, JTextField value) {
            propertyName  = key;
            propertyValue = value;
        }        
    }    
    
    private List<PropertyFileData> __convertPropertiesToList(Properties p) {
        if (p == null) return new ArrayList<PropertyFileData>();
        
        List<PropertyFileData> rval = new ArrayList<PropertyFileData>();
        Set<Map.Entry<Object, Object>> plist = p.entrySet();
        Iterator<Map.Entry<Object, Object>> iter = plist.iterator();
        while (iter.hasNext()) {
            try {
                Map.Entry<Object, Object> entry = iter.next();
                rval.add(new PropertyFileData((String)entry.getKey(),
                        new JTextField((String)entry.getValue())));
            } catch (Throwable t2) {
                //LOGGER.log(Level.INFO, "Cannot parse a property in file");
            	t2.printStackTrace();
            }
        }
        return rval;
    }   
    
	public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	thisFrame = new AQMGUI("AQM");
            	thisFrame.setVisible(true);
            }
        });
        
      //timer = new Timer(1000, (ActionListener) AspiraGUI.this);
	}
	
	class AQMStartWorker extends SwingWorker<String, Void> {
		MonitoringService theService;
		@Override
		protected String doInBackground() throws Exception {
			int delay = 1000; // milliseconds
			timer = new Timer(delay, null);
			timer.start();

			startAQMService();
			//return "start now";
			return null;
		}
		
		private void startAQMService() {
			theService = null;
			System.out.println("Started AQM at " + new Date(__startTime));
			try {
				theService = MonitoringService.getMonitoringService();
				if (theService == null) {
					System.out.println("EXITING: Cannot initialize the Monitoring Service");
				}
				while(!isCancelled()) {
				}
			} catch (Throwable t) {
				t.printStackTrace();
				System.exit(0);
			} finally {
				if (theService != null) {
					try {
						theService.shutdownService();
					} catch (Throwable te) {
						te.printStackTrace();
						System.out.println("Could not shutdown Aspira Monitoring Service cleanly");
					}
				}
			}
		}
				
		protected void done() {
			try {
				//JOptionPane.showMessageDialog(AspiraGUI.this, get());
				if (theService != null) theService.shutdownService();
				if (isCancelled()) {
					System.out.println("SwingWorker - isCancelled");
				}
				System.exit(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}	
	
	private static JFrame thisFrame;
	private JTabbedPane jTabbedPane1;
	private JButton startButton;
	private JButton stopButton;
	private static Timer timer;
	private AQMStartWorker worker;
	private static final long __startTime = System.currentTimeMillis();

}
