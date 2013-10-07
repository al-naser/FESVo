/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class reads properties (configurations) from a property file
 * 
 * Help from: http://www.mkyong.com/java/java-properties-file-examples/ 
 * 
 * @author Aqeel
 */
public class PropertyReader {
    
    // property folder:
    // ----------------
    private static final String PROP_DIR = "config";
    
    // property files:
    // ---------------
    private static final String PROP_FILE = PROP_DIR + "/config.properties"; // general properties 
    private static final String DB_PROP_FILE = PROP_DIR + "/config_db.properties"; // DB related
    
    
    // properties objects:
    // -------------------
    private static final Properties PROP = new Properties();
    private static final Properties DB_PROP = new Properties(); // DB related
    
    
    // load properties files:
    // ----------------------
    static
    {
        try{
            PROP.load(new FileInputStream(PROP_FILE));
            DB_PROP.load(new FileInputStream(DB_PROP_FILE));
            
    	}//try
        
        catch (IOException ex) {
    		ex.printStackTrace();
        }
        
    }//static
    
    
    //------------------------------------------------------------------------

    /**
     * returns the property value associated to the given key in the general property file 
     * @param key
     * @return value
     */
    public static String getProperty(String key){
        return PROP.getProperty(key);
    }
    
    /**
     * returns the property value associated to the given key in the general property file, otherwise returns the default value argument if the property is not found.
     * @param key
     * @return value
     */
    public static String getProperty(String key, String defaultValue){
        return PROP.getProperty(key, defaultValue);
    }
    
    /**
     * returns the property value associated to the given key in the DB property file 
     * @param key
     * @return value
     */
    public static String getDBProperty(String key){
        return DB_PROP.getProperty(key);
    }
    
    /**
     * returns the property value associated to the given key in the DB property file, otherwise returns the default value argument if the property is not found.
     * @param key
     * @return value
     */
    public static String getDBProperty(String key, String defaultValue){
        return DB_PROP.getProperty(key, defaultValue);
    }
    
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main( String[] args )
    {
        System.out.println(PropertyReader.getDBProperty("DATABASE"));
    }
}
