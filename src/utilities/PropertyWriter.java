/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

/**
 * This class writes properties (configurations) into a property file
 * 
 * Help from: http://www.mkyong.com/java/java-properties-file-examples/ 
 * 
 * @author Aqeel
 */
public class PropertyWriter {
    
    // general configurations 
    private static final String PROP_FILE = "temp_config.properties";
    
    // Database related configurations:
    private static final String DB_PROP_FILE = "temp_config_db.properties";
    
    //------------------------------------------------------------------------
    
    private static void initializePropertyFile()
    {
        Properties prop = new Properties();
 
    	try {
    		//set the properties value
    		prop.setProperty("prop", "value");
 
                
                //save properties
    		prop.store(new FileOutputStream(PROP_FILE), null);
 
    	}
        
        catch (IOException ex) {
    		ex.printStackTrace();
        }
    }
    
    private static void initializeDBPropertyFile()
    {
        Properties prop = new Properties();
 
    	try {
    		//set the properties value
    		prop.setProperty("prop", "value");
 
    		//save properties
    		prop.store(new FileOutputStream(DB_PROP_FILE), null);
 
    	}
        
        catch (IOException ex) {
    		ex.printStackTrace();
        }
    }
    
    //------------------------------------------------------------------------
    
    public static void main( String[] args )
    {
        // write default properties:
        PropertyWriter.initializePropertyFile();
        
        // write DB properties:
        PropertyWriter.initializeDBPropertyFile();
               
    }
    
}
