/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

// NOTE: NOT all methods in this class are functional, mainly with the new SRDS database,
//      only the followings are used in this project:
//          1. getTrace (int x, int y, int sourceID) - added on 24/10/2011


package utilities;

import java.io.IOException;
import java.sql.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.*;
import java.util.*;
import seismic.*;

/**
 *
 * @author Aqeel
 */
public class DatabaseConnector {
    
    /**
     * [8.2.2013] using class PropertyReader to get configurations from a property file
     */

    // Database Driver:
    //--------------
    private final static String DB_DRIVER = PropertyReader.getDBProperty("DB_DRIVER");
    
    // Database URL:
    //--------------
    private final static String URL = PropertyReader.getDBProperty("URL");

    // Username and password:
    //-----------------------
    private final static String USERNAME = PropertyReader.getDBProperty("USERNAME");
    private final static String PASSWORD = PropertyReader.getDBProperty("PASSWORD");

    // Required Database, Tables and Columns Names as appears in the database:
    //------------------------------------------------------------------------
    private final static String DATABASE = PropertyReader.getDBProperty("DATABASE");
    private final static String TRACE_TABLE = PropertyReader.getDBProperty("TRACE_TABLE");
    private final static String FEATURE_TABLE = PropertyReader.getDBProperty("FEATURE_TABLE");
    private final static String X_COLUMN = PropertyReader.getDBProperty("X_COLUMN");
    private final static String Y_COLUMN = PropertyReader.getDBProperty("Y_COLUMN");
    private final static String Z_COLUMN = PropertyReader.getDBProperty("Z_COLUMN");
    private final static String PROPERTY_ID_COLUMN = PropertyReader.getDBProperty("PROPERTY_ID_COLUMN");
    private final static String PROPERTY_VALUE_COLUMN = PropertyReader.getDBProperty("PROPERTY_VALUE_COLUMN");
    private final static String SOURCE_ID_COLUMN = PropertyReader.getDBProperty("SOURCE_ID_COLUMN");
    private final static String TWO_WAY_TRAVEL_TIME_COLUMN = PropertyReader.getDBProperty("TWO_WAY_TRAVEL_TIME_COLUMN");
    private final static String TIMESTAMP_COLUMN = PropertyReader.getDBProperty("TIMESTAMP_COLUMN");
    
    
    // the followings were added on [26/4/2012]:
    private final static String METADATA_TABLE = PropertyReader.getDBProperty("METADATA_TABLE");
    private final static String SOURCE_DESCRIPTION_COLUMN = PropertyReader.getDBProperty("SOURCE_DESCRIPTION_COLUMN");
    private final static String SOURCE_FILE_NM_COLUMN = PropertyReader.getDBProperty("SOURCE_FILE_NM_COLUMN");
    private final static String FIRST_INLINE_COLUMN = PropertyReader.getDBProperty("FIRST_INLINE_COLUMN");
    private final static String LAST_INLINE_COLUMN = PropertyReader.getDBProperty("LAST_INLINE_COLUMN");
    private final static String FIRST_XLINE_COLUMN = PropertyReader.getDBProperty("FIRST_XLINE_COLUMN");
    private final static String LAST_XLINE_COLUMN = PropertyReader.getDBProperty("LAST_XLINE_COLUMN");
    private final static String X_MIN_COLUMN = PropertyReader.getDBProperty("X_MIN_COLUMN");
    private final static String X_MAX_COLUMN = PropertyReader.getDBProperty("X_MAX_COLUMN");
    private final static String X_DELTA_COLUMN = PropertyReader.getDBProperty("X_DELTA_COLUMN");
    private final static String Y_MIN_COLUMN = PropertyReader.getDBProperty("Y_MIN_COLUMN");
    private final static String Y_MAX_COLUMN = PropertyReader.getDBProperty("Y_MAX_COLUMN");
    private final static String Y_DELTA_COLUMN = PropertyReader.getDBProperty("Y_DELTA_COLUMN");
    private final static String TIME_MIN_COLUMN = PropertyReader.getDBProperty("TIME_MIN_COLUMN");
    private final static String TIME_MAX_COLUMN = PropertyReader.getDBProperty("TIME_MAX_COLUMN");
    private final static String TIME_DELTA_COLUMN = PropertyReader.getDBProperty("TIME_DELTA_COLUMN");
    private final static String AMPLITUDE_MIN_COLUMN = PropertyReader.getDBProperty("AMPLITUDE_MIN_COLUMN");
    private final static String AMPLITUDE_MAX_COLUMN = PropertyReader.getDBProperty("AMPLITUDE_MAX_COLUMN");
    private final static String AMPLITUDE_DELTA_COLUMN = PropertyReader.getDBProperty("AMPLITUDE_DELTA_COLUMN");
    private final static String SAMPLE_INTERVAL_COLUMN = PropertyReader.getDBProperty("SAMPLE_INTERVAL_COLUMN");
    private final static String SAMPLES_PER_TRACE_COLUMN = PropertyReader.getDBProperty("SAMPLES_PER_TRACE_COLUMN");
    private final static String POINT_A_X_COLUMN = PropertyReader.getDBProperty("POINT_A_X_COLUMN");
    private final static String POINT_A_Y_COLUMN = PropertyReader.getDBProperty("POINT_A_Y_COLUMN");
    private final static String POINT_B_X_COLUMN = PropertyReader.getDBProperty("POINT_B_X_COLUMN");
    private final static String POINT_B_Y_COLUMN = PropertyReader.getDBProperty("POINT_B_Y_COLUMN");
    private final static String POINT_C_X_COLUMN = PropertyReader.getDBProperty("POINT_C_X_COLUMN");
    private final static String POINT_C_Y_COLUMN = PropertyReader.getDBProperty("POINT_C_Y_COLUMN");
    private final static String POINT_D_X_COLUMN = PropertyReader.getDBProperty("POINT_D_X_COLUMN");
    private final static String POINT_D_Y_COLUMN = PropertyReader.getDBProperty("POINT_D_Y_COLUMN");
    private final static String UTM_ZONE_COLUMN = PropertyReader.getDBProperty("UTM_ZONE_COLUMN");
    
    
    
    // Source grouping table, columns, and codes:
    // ------------------------------------------
    
    private final static String SOURCE_GROUPING_TABLE = PropertyReader.getDBProperty("SOURCE_GROUPING_TABLE");

    private final static String GROUP_ID_COLUMN = PropertyReader.getDBProperty("GROUP_ID_COLUMN");
    private final static String GROUPING_RELATION_CODE_COLUMN = PropertyReader.getDBProperty("GROUPING_RELATION_CODE_COLUMN");
    private final static String GROUPING_EFFECTIVE_TIME_COLUMN = PropertyReader.getDBProperty("GROUPING_EFFECTIVE_TIME_COLUMN");
    private final static String GROUPING_END_TIME_COLUMN = PropertyReader.getDBProperty("GROUPING_END_TIME_COLUMN");

    private final static int BASELINE_RELATION_CODE = Integer.parseInt(PropertyReader.getDBProperty("BASELINE_RELATION_CODE"));
    private final static int INSERTION_RELATION_CODE = Integer.parseInt(PropertyReader.getDBProperty("INSERTION_RELATION_CODE"));
    private final static int DELETION_RELATION_CODE = Integer.parseInt(PropertyReader.getDBProperty("DELETION_RELATION_CODE"));

    private final static String CURRENT_TIMESTAMP = PropertyReader.getDBProperty("CURRENT_TIMESTAMP");
    
    
    // The source type id column name and values of 'original' & 'user interpretation':
    // --------------------------------------------------------------------------------
    private final static String SOURCE_TYPE_ID_COLUMN = PropertyReader.getDBProperty("SOURCE_TYPE_ID_COLUMN");
    private final static int ORIGINAL_SOURCE_TYPE_ID = Integer.parseInt(PropertyReader.getDBProperty("ORIGINAL_SOURCE_TYPE_ID"));
    private final static int USER_SOURCE_TYPE_ID = Integer.parseInt(PropertyReader.getDBProperty("USER_SOURCE_TYPE_ID"));
    
    
    //------------------------------------------------------------------------
    
    // The property id of seismic amplitude (trace) and horizon as recorded in the database:
    private final static int AMPLITUDE_PROPERTY_ID = Integer.parseInt(PropertyReader.getDBProperty("AMPLITUDE_PROPERTY_ID"));
    private final static int HORIZON_PROPERTY_ID = Integer.parseInt(PropertyReader.getDBProperty("HORIZON_PROPERTY_ID"));
    private final static int FAULT_PROPERTY_ID = Integer.parseInt(PropertyReader.getDBProperty("FAULT_PROPERTY_ID"));

    // Macro query for prepared statement to get a trace:
    // parameter 1: x
    // parameter 2: y
    // parameter 3: source id (or survey id)
    //private final static String MACRO_QUERY = "exec " + DATABASE + ".FETCH_AMP_TRACE (?, ?, ?);";
    //private final static String MACRO_QUERY = "exec FETCH_AMP_TRACE (?, ?, ?);";
    private final static String MACRO_QUERY = "exec SELECT_TRACE (?, ?, ?);";


    // Database Connection:
    Connection con;
    
    // PreparedStatement for the current session:
    PreparedStatement pStatement = null; // for traces
    PreparedStatement hPStatement = null; // for horizons [24/6/2012]
    
    // a threshold of a searching process:
    private int threshold;
    
    // width and depth of a searching process: [14/2/2012]
    // this should equals to FESVo LOD Unit Width and Depth,
    // but make it general for, eg, testing
    private int width, depth;
    
    // multiStatementLength [24/2/2012]
    private int multiStatementLength;
    
    // number of requested horizon objects: [24/6/2012]
    private int numOfHorizons;
    
    // [18/2/2012] trace header size
    // trace header should be removed to have pure samples:
    private final static int TRACE_HEADER_SIZE = 16; // 16 bytes (4 integers)
    

    // [TEST] number of traces to print:
    private int tracesToPrint = 0;
    
    // contructor
    public DatabaseConnector(){

        // connect to database:
        this.connect();

    }//constructor

    // connect to the database
    private void connect()
    {

        try
        {
            // load the Teradata Driver:
            System.out.println(" Looking for the Teradata JDBC driver... ");
            Class.forName(DB_DRIVER);
            System.out.println(" JDBC driver loaded. \n");

            // connect to the required database:
            System.out.println("Connecting to " + URL);
            con = DriverManager.getConnection (URL, USERNAME, PASSWORD);
            System.out.println("Established successful connection.\n");

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }

        catch(ClassNotFoundException ex)
        {
            System.out.println("*** ClassNotFoundException caught ***");
            //ex.printStackTrace();
        }
        
    }//connect
    
    // ------------------------------------------------------------------------

    // disconnect from database
    public void disconnect()
    {

        // if not connected already, exit method:
        if (con == null)
        {
            System.out.println("Not connected to database!\n");
            return;
        }

        try
        {
            // diconnect from the database:
            con.close();
            System.out.println("Disconnected from database \n");

        }//try


        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }

    }//disconnect
    
    // ------------------------------------------------------------------------
    
    // obtain a trace data for a given x,y location
    // a trace data is returned as an array of sampled frequencies
    public float[] getTrace_old(int x, int y)
    {
        // Query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        String sQuery = "SELECT w.DESCRIBE() FROM stg.WAVELETS "
                         + "WHERE x=" + x + " and y=" + y;
        
        // the returned queried value from database:
        String stringData = "";

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            System.out.println("Statement object created. \n");

            try
            {
                // Performing the query
                ResultSet resultSet = statement.executeQuery(sQuery);
                

                // Retrieve the properties of the ResultSet object
                // ResultSetMetaData resultMD = resultSet.getMetaData();

                // display metadata of this result set:
                // displayRSMetaData(resultSet);
                

                // get the value of 'w' column:
                resultSet.next(); // go to first row
                stringData = resultSet.getString(1);
                //System.out.println(stringData);
                System.out.println("Result is obtained.\n");

                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }
        
        // split the obtained result into an array:
        // execluding the first and last char from stringData, i.e. '(' and ')'
        String [] stringArray = (stringData.substring(1, (stringData.length()-1)).split(","));
        System.out.println("Result was split into an array of size " + stringArray.length + "\n");
        
        // convert each string into a float:
        float [] sample = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++)
        {
            sample[i] = Float.parseFloat(stringArray[i]);

            // TESTING: clamp each sample to range [0,1]
            sample[i] = ((sample[i] + 453865.88f) /  6894665.88f);

            // TESTING:
            //sample[i] = 0.5f;

        }//for
        
        System.out.println("A float type array holding all samples was created.\n");

        return sample;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain a trace data for a given inline, xline, and survey id
    // a trace data is returned as an array of sampled frequencies
    public float[] getTrace_old(int inline, int xline, int surveyID)
    {
        // Query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        String sQuery2 = "select WV.w.DESCRIBE() "
                      + "from stg.TRACE_HEADER TH, stg.WAVELETS WV "
                      + "where TH.TRACE_SEQ_WITHIN_SEGY=" + inline
                      + " and TH.ENSEMBLE_NUM=" + xline
                      + " and TH.SURVEY_ID=" + surveyID
                      + " and TH.SRCE_COORDINATE_X = WV.x and TH.SRCE_COORDINATE_Y = WV.y";

        // more efficient query
        String sQuery = "select WV.w.DESCRIBE() "
                       + "from stg.WAVELETS WV "
                       + "WHERE (WV.x,WV.y) in "
                            + "(select SRCE_COORDINATE_X, SRCE_COORDINATE_Y from stg.TRACE_HEADER"
                            + " where TRACE_SEQ_WITHIN_SEGY=" + inline
                            + " and TRACE_SEQ_WITHIN_LINE=" + xline
                            + " and SURVEY_ID=" + surveyID
                            + " )";

        // the returned queried value from database:
        String stringData = "";

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            System.out.println("Statement object created. \n");

            try
            {
                // Performing the query
                ResultSet resultSet = statement.executeQuery(sQuery);


                // Retrieve the properties of the ResultSet object
                // ResultSetMetaData resultMD = resultSet.getMetaData();

                // display metadata of this result set:
                // displayRSMetaData(resultSet);


                // get the value of 'w' column:
                resultSet.next(); // go to first row
                stringData = resultSet.getString(1);
                //System.out.println(stringData);
                System.out.println("Result is obtained.\n");


            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }

        // split the obtained result into an array:
        // execluding the first and last char from stringData, i.e. '(' and ')'
        String [] stringArray = (stringData.substring(1, (stringData.length()-1)).split(","));
        System.out.println("Result was split into an array of size " + stringArray.length + "\n");

        // convert each string into a float:
        float [] sample = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++)
        {
            sample[i] = Float.parseFloat(stringArray[i]);

            // TESTING: clamp each sample to range [0,1]
            //sample[i] = ((sample[i] + 453865.88f) /  6894665.88f);

            // TESTING:
            //sample[i] = 0.5f;

        }//for

        System.out.println("A float type array holding all samples was created.\n");

        return sample;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain a trace data for a given inline, xline, and survey id
    // and a min and max values for clamping into range [0,1]
    // a trace data is returned as an array of sampled frequencies
    public float[] getTrace_old(int inline, int xline, int surveyID, float min, float max)
    {
        // Query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        String sQuery2 = "select WV.w.DESCRIBE() "
                      + "from stg.TRACE_HEADER TH, stg.WAVELETS WV "
                      + "where TH.TRACE_SEQ_WITHIN_SEGY=" + inline
                      + " and TH.ENSEMBLE_NUM=" + xline
                      + " and TH.SURVEY_ID=" + surveyID
                      + " and TH.SRCE_COORDINATE_X = WV.x and TH.SRCE_COORDINATE_Y = WV.y";

        // more efficient query
        // this query relies on the TRACE_SEQ_WITHIN_LINE for xline which always starts from 1;
        String sQuery = "select WV.w.DESCRIBE() "
                       + "from stg.WAVELETS WV "
                       + "WHERE (WV.x,WV.y) in "
                            + "(select SRCE_COORDINATE_X, SRCE_COORDINATE_Y from stg.TRACE_HEADER"
                            + " where TRACE_SEQ_WITHIN_SEGY=" + inline
                            + " and TRACE_SEQ_WITHIN_LINE=" + xline
                            + " and SURVEY_ID=" + surveyID
                            + " )";

        // the returned queried value from database:
        String stringData = "";

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            //System.out.println("Statement object created. \n");

            try
            {
                // Performing the query
                ResultSet resultSet = statement.executeQuery(sQuery);


                // Retrieve the properties of the ResultSet object
                // ResultSetMetaData resultMD = resultSet.getMetaData();

                // display metadata of this result set:
                // displayRSMetaData(resultSet);


                // get the value of 'w' column:
                resultSet.next(); // go to first row
                stringData = resultSet.getString(1);
                //System.out.println(stringData);
                //System.out.println("Result is obtained.\n");


            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }

        // split the obtained result into an array:
        // execluding the first and last char from stringData, i.e. '(' and ')'
        String [] stringArray = (stringData.substring(1, (stringData.length()-1)).split(","));
        //System.out.println("Result was split into an array of size " + stringArray.length + "\n");

        // convert each string into a float:
        float [] sample = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++)
        {
            sample[i] = Float.parseFloat(stringArray[i]);

            // clamp each sample to range [0,1]
            sample[i] = ((sample[i] - min) /  (max-min));

        }//for

        //System.out.println("A float type array holding all samples was created.\n");

        return sample;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain a trace data for a given inline, xline, and survey id
    // and a min and max values for clamping into range [0,1]
    // a trace data is returned as an array of sampled frequencies
    public float[] getTraceBinary_old(int inline, int xline, int surveyID, float min, float max)
    {
        // Query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        // more efficient query
        // this query relies on the TRACE_SEQ_WITHIN_LINE for xline which always starts from 1;
        String sQuery = "select WV.w "
                       + "from stg.WAVELETS WV "
                       + "WHERE (WV.x,WV.y) in "
                            + "(select SRCE_COORDINATE_X, SRCE_COORDINATE_Y from stg.TRACE_HEADER"
                            + " where TRACE_SEQ_WITHIN_SEGY=" + inline
                            + " and TRACE_SEQ_WITHIN_LINE=" + xline
                            + " and SURVEY_ID=" + surveyID
                            + " )";

        // the returned queried value from database:
        InputStream inputStream;
        byte [] resultB;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            //System.out.println("Statement object created. \n");

            try
            {
                // Performing the query
                ResultSet resultSet = statement.executeQuery(sQuery);


                // get the value of 'w' column:
                resultSet.next(); // go to first row
                //inputStream = resultSet.getBinaryStream(1);
                resultB = resultSet.getBytes(1);
                System.out.println("Result is obtained.\n");


            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }

        byte [] sampleB = new byte[4];

        ByteBuffer bufferB = ByteBuffer.wrap(resultB);
        bufferB.rewind();

        //testing:
        bufferB.order(ByteOrder.LITTLE_ENDIAN);

        FloatBuffer bufferF = bufferB.asFloatBuffer();

        float float0 = bufferF.get();
        float float1 = bufferF.get();
        float float2 = bufferF.get();
        float float3 = bufferF.get();
        
        /*
        try {
            inputStream.parameterIndex(4*2);
            int noOfByte = inputStream.read(sampleB);
        } catch (IOException ex) {
            Logger.getLogger(DatabaseConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
         *
         */

        

        /*
        // split the obtained result into an array:
        // execluding the first and last char from stringData, i.e. '(' and ')'
        String [] stringArray = (stringData.substring(1, (stringData.length()-1)).split(","));
        //System.out.println("Result was split into an array of size " + stringArray.length + "\n");

        // convert each string into a float:
        float [] sample = new float[stringArray.length];
        for (int i = 0; i < stringArray.length; i++)
        {
            sample[i] = Float.parseFloat(stringArray[i]);

            // clamp each sample to range [0,1]
            sample[i] = ((sample[i] - min) /  (max-min));

        }//for

        //System.out.println("A float type array holding all samples was created.\n");

        return sample;
        */

        // temp solution:
        float [] sample = new float[resultB.length/4];
        bufferF.rewind();
        for (int i=0; i < sample.length; i++){
            sample[i] = bufferF.get();
        }

        return sample;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain a trace data for a given x, y, and source id
    // if more than one trace exists at this location, return one only
    // a trace data is returned as an array of bytes
    public byte[] getTrace(int x, int y, int sourceID)
    {
        // Query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        String sQuery = "SELECT " + PROPERTY_VALUE_COLUMN
                        + " FROM " + DATABASE + "." + TRACE_TABLE
                        + " WHERE " + X_COLUMN + " = " + x
                          + " and " + Y_COLUMN + " = " + y
                          + " and " + SOURCE_ID_COLUMN + " = " + sourceID;

        String macroQuery = "exec EDW.FETCH_AMP_TRACE ("
                            + x + ", " + y + ", " + sourceID + ")";

        // [TEST] print query:
        //System.out.println("Query: " + query);

        // the returned queried value from database:
        byte [] samples = null;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            //System.out.println("Statement object created. \n");

            try
            {
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // Performing the query
                ResultSet resultSet = statement.executeQuery(macroQuery);
        
                long endTime = System.currentTimeMillis(); System.out.println("Time: " + (endTime - startTime) + " ms.");


                // get the value of the property value column:
                // if no row is available, return null
                if (resultSet.next()){
                    samples = resultSet.getBytes(1);
                    //System.out.println("Result is obtained.\n"); // [TEST]
                }

            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return the retrieved samples as a byte array
        return samples;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain the first trace object found in an area
    // given xStart, yStart, source id, and threshold of this area
    // using macros, and a single PreparedStatement which execute a single query at each run
    public Trace getTrace(int xStart, int yStart, int sourceID, int threshold)
    {
        // the returned queried value from database:
        byte [] samples = null;
        
        // the returned Trace object:
        Trace trace = null;

        // ResultSet objects:
        ResultSet resultSet;
        
        try
        {
            // Creating a PreparedStatement object
            PreparedStatement pStatement = con.prepareStatement(MACRO_QUERY);

            //System.out.println("PreparedStatement object created. \n");
            
            try
            {
                // run the query over the required area using threshold value:
                loop: for (int i=0; i < threshold; i++)
                {
                    for (int j=0; j < threshold; j++)
                    {
                        // set parameters of the statement:
                        pStatement.setInt(1, (xStart+i));
                        pStatement.setInt(2, (yStart+j));
                        pStatement.setInt(3, sourceID);

                        // [TEST] measure execution time:
                        long startTime = System.currentTimeMillis();

                        // Performing the query
                        resultSet = pStatement.executeQuery();

                        // [TEST] get end time and print:
                        long endTime = System.currentTimeMillis(); System.out.println("Time: " + (endTime - startTime) + " ms.");


                        // get the value of the property value column:
                        // if no row is available, return null
                        if (resultSet.next()){
                            samples = resultSet.getBytes(1);
                            trace = new Trace(xStart+i, yStart+j, sourceID);
                            trace.setSamples(samples);
                            break loop;
                        }//if
                        
                    }//for (j)
                }//for (i)


            }//try

            finally
            {
                // Close the statement
                pStatement.close();
                //System.out.println("Statement object closed. \n");
            }//finally


        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return the retrieved samples as a byte array
        return trace;
    }//getTrace

    // ------------------------------------------------------------------------

    // obtain the first trace object found in an area
    // given xStart, yStart, source id, and threshold of this area
    // using macros, and a single Statement which execute a single query at each run
    public Trace getTrace_v2(int xStart, int yStart, int sourceID, int threshold)
    {
        // Macro query statement selecting sampled frequencies
        // of a prticular trace located at x, y location
        // to be used as a PreparedStatement
        String macroQuery;

        // the returned queried value from database:
        byte [] samples = null;

        // the returned Trace object:
        Trace trace = null;

        // ResultSet objects:
        ResultSet resultSet;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();

            try
            {
                // run the query over the required area using threshold value:
                loop: for (int i=0; i < threshold; i++)
                {
                    for (int j=0; j < threshold; j++)
                    {
                        // create the query:
                        macroQuery = "exec EDW.FETCH_AMP_TRACE ("
                                + (xStart+i) + ", " + (yStart+j) + ", " + sourceID + ")";

                        // [TEST] measure execution time:
                        long startTime = System.currentTimeMillis();

                        // Performing the query
                        resultSet = statement.executeQuery(macroQuery);

                        // [TEST] get end time and print:
                        long endTime = System.currentTimeMillis(); System.out.println("Time: " + (endTime - startTime) + " ms.");


                        // get the value of the property value column:
                        // if no row is available, return null
                        if (resultSet.next()){
                            samples = resultSet.getBytes(1);
                            trace = new Trace(xStart+i, yStart+j, sourceID);
                            trace.setSamples(samples);
                            break loop;
                        }//if

                    }//for (j)
                }//for (i)


            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally


        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return the retrieved samples as a byte array
        return trace;
    }//getTrace


    // ------------------------------------------------------------------------

    // obtain the first trace object found in an area
    // given xStart, yStart, source id, and threshold of this area
    // using macros, and a single PreparedStatement which execute multiple queries at a single run
    public Trace getTrace_v3(int xStart, int yStart, int sourceID, int threshold)
    {
        // Multiple query to be created based on threshold:
        String multiQuery = "";
        
        for (int i=0; i < threshold; i++)
        {
            for (int j=0; j < threshold; j++)
            {
                multiQuery += MACRO_QUERY;
            }//for
        }//for

        // the returned queried value from database:
        byte [] samples = null;

        // the returned Trace object:
        Trace trace = null;

        // ResultSet objects:
        ResultSet resultSet;

        try
        {
            // Creating a PreparedStatement object
            PreparedStatement pStatement = con.prepareStatement(multiQuery);

            //System.out.println("PreparedStatement object created. \n");

            try
            {
                // set parameters of the statement:
                int counter = 1;
                for (int i=0; i < threshold; i++)
                {
                    for (int j=0; j < threshold; j++)
                    {
                        pStatement.setInt(counter++, (xStart+i));
                        pStatement.setInt(counter++, (yStart+j));
                        pStatement.setInt(counter++, sourceID);
                    }//for
                }//for                
                
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // run the multiple query in one go:
                pStatement.execute();

                // [TEST] get end time and print:
                long endTime = System.currentTimeMillis(); 
                //FESVoSystem.out.println("Executed multi-statement query in: " + (endTime - startTime) + " ms.");
                FESVoSystem.out.print((endTime - startTime) + " ");
                
                
                
                // get result sets:
                loop: for (int i=0; i < threshold; i++)
                {
                    for (int j=0; j < threshold; j++)
                    {
                        // [TEST] measure execution time:
                        //startTime = System.currentTimeMillis();

                        // get result set
                        resultSet = pStatement.getResultSet();

                        // [TEST] get end time and print:
                        //endTime = System.currentTimeMillis(); System.out.println("Time: " + (endTime - startTime) + " ms.");

                        // get the value of the property value column:
                        // if no row is available, return null
                        if (resultSet.next()){
                            samples = resultSet.getBytes(1);
                            trace = new Trace(xStart+i, yStart+j, sourceID);
                            trace.setSamples(samples);
                            break loop;
                        }//if

                        // move to next result set:
                        pStatement.getMoreResults();

                    }//for (j)
                }//for (i)


            }//try

            finally
            {
                // Close the statement
                pStatement.close();
                //System.out.println("Statement object closed. \n");
            }//finally


        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return the retrieved samples as a byte array
        return trace;
    }//getTrace

    // ------------------------------------------------------------------------

    // [29/1/2012]
    // obtain the first trace object found in an area
    // given xStart, yStart, source id, and threshold of this area
    // using PreparedStatement and where x/y in
    public Trace getTrace_v4(int xStart, int yStart, int sourceID, int threshold)
    {
        // a query to be created based on threshold:
        String query = "locking row for access SELECT " 
                        + X_COLUMN + "," + Y_COLUMN + "," + PROPERTY_VALUE_COLUMN
                        + " FROM " + DATABASE + "." + TRACE_TABLE;
        
        // build the combination of x and y values:
        String s = "(?";
        for (int i=1; i < threshold; i++){
            s += ",?";
        }
        s += ")";
        
        // complete the query:
        query += " WHERE " + X_COLUMN + " in " + s 
               + " and " + Y_COLUMN + " in " + s
               + " and " + SOURCE_ID_COLUMN + "=?"
               + " and " + PROPERTY_ID_COLUMN + "=" + AMPLITUDE_PROPERTY_ID + ";";
        
        // [TEST]:
        //System.out.println("query: \n" + query);
        
        // the returned Trace object:
        Trace trace = null;

        // ResultSet objects:
        ResultSet resultSet;

        try
        {
            // Creating a PreparedStatement object
            PreparedStatement pStatement = con.prepareStatement(query);
            
            try
            {
                // set parameters of the statement:
                // x values:
                for (int i=0; i < threshold; i++){
                    pStatement.setInt((i+1), (xStart+i));
                }
                // y values:
                for (int i=0; i < threshold; i++){
                    pStatement.setInt((threshold+i+1), (yStart+i));
                }
                // source id:
                pStatement.setInt((threshold*2+1), sourceID);
                
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // run the multiple query in one go:
                pStatement.execute();

                // [TEST] get end time and print:
                long endTime = System.currentTimeMillis(); 
                //FESVoSystem.out.println("Executed query in: " + (endTime - startTime) + " ms.");
                FESVoSystem.out.print((endTime - startTime) + " ");
                
                
                
                // get result set:
                resultSet = pStatement.getResultSet();

                // only interested in one trace if available, 
                // so no need to go throw all returned rows:
                // if no row is available, return null
                if (resultSet.next()){
                    int x = resultSet.getInt(1);
                    int y = resultSet.getInt(2);
                    byte [] samples = resultSet.getBytes(3);
                    trace = new Trace(x, y, sourceID);
                    trace.setSamples(samples);
                }//if

                        


            }//try

            finally
            {
                // Close the statement
                pStatement.close();
                //System.out.println("Statement object closed. \n");
            }//finally


        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                ////ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return the retrieved samples as a byte array
        return trace;
    }//getTrace
    
    // ------------------------------------------------------------------------

    // [29/1/2012]
    // prepare a PreparedStatement with condition "where x/y in .."
    // to be used later with getTrace_v5()
    public boolean buildTracePreparedStatement(int threshold){
        return this.buildTracePreparedStatement(threshold, threshold); // [14/2/2012]
    }
            
    // ------------------------------------------------------------------------

    // [14/2/2012]
    // prepare a PreparedStatement with condition "where x/y in .."
    // to be used later with getTrace_v5()
    public boolean buildTracePreparedStatement(int width, int depth)
    {
        return this.buildTracePreparedStatement(width, depth, 1);
    }
    
    // ------------------------------------------------------------------------
    
    // [24/2/2012]
    // prepares a multi-statement PreparedStatement with condition "where x/y in .."
    // to be used later with getTrace_v5()
    // multiStatementLength must be greated or equal to the length of points array passed to getTrace_v5()
    public boolean buildTracePreparedStatement(int width, int depth, int multiStatementLength)
    {
        // store width, depth, and multiStatementLength:
        this.width = width;
        this.depth = depth;
        this.multiStatementLength = multiStatementLength;
        
        String query = "";
        
        // build the combination of x values:
        String sX = "(?";
        for (int i=1; i < width; i++){
            sX += ",?";
        }
        sX += ")";

        // build the combination of values:
        String sY = "(?";
        for (int i=1; i < depth; i++){
            sY += ",?";
        }
        sY += ")";

        for (int st=0; st < multiStatementLength; st++){
            // a query to be created based on threshold:
            query += "locking row for access SELECT " 
                            + X_COLUMN + "," + Y_COLUMN + "," + PROPERTY_VALUE_COLUMN
                            + " FROM " + DATABASE + "." + TRACE_TABLE;

            // complete the query:
            query += " WHERE " + X_COLUMN + " in " + sX 
                   + " and " + Y_COLUMN + " in " + sY
                   + " and " + SOURCE_ID_COLUMN + "=?"
                   + " and " + PROPERTY_ID_COLUMN + "=" + AMPLITUDE_PROPERTY_ID + " sample 1;";
        }//for
        
        try{
            // Creating a PreparedStatement object
            pStatement = con.prepareStatement(query);
        }
        
        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return true indicating that a prepared statement was successfully created:
        return true;
    }//buildTracePreparedStatement
    
    // ------------------------------------------------------------------------
    
    // [29/1/2012]
    // obtain the first trace object found in an area
    // given xStart, yStart, source id, and threshold of this area
    // using a PreparedStatement which should have been created by buildTracePreparedStatement()
    // modified on [14/2/2012] to use width and depth instead of threshold
    public Trace getTrace_v5(int xStart, int yStart, int sourceID)
    {
        return this.getMultiTraces(
                new GeoTag[]{new GeoTag(xStart, yStart, sourceID)})[0];
    }//getTrace_v5
    
    
    public Trace [] getMultiTraces(GeoTag [] startingPoints){
        
        // the returned traces object:
        Trace [] traces = new Trace[startingPoints.length];

        // ResultSet objects:
        ResultSet resultSet;

        try
        {
            // parameterIndex:
            int parameterIndex = 1;
            
            // set parameters of the statements:
            for (int p=0; p < startingPoints.length; p++)
            {
                // x values:
                for (int i=0; i < width; i++){
                    pStatement.setInt((parameterIndex+i), (startingPoints[p].x+i));
                }
                
                // y values:
                parameterIndex += width;
                for (int i=0; i < depth; i++){
                    pStatement.setInt((parameterIndex+i), (startingPoints[p].y+i));
                }
                
                // source id:
                parameterIndex += depth;
                pStatement.setInt((parameterIndex), startingPoints[p].sourceID);
                
                // update parameterIndex:
                parameterIndex += 1;
            }//for
            
            // if starting points are less than the multi-statement length,
            // set remaining parameters to 0:
            for (int p=startingPoints.length; p < (this.multiStatementLength); p++)
            {
                // x values:
                for (int i=0; i < width; i++){
                    pStatement.setInt((parameterIndex+i), (0));
                }
                
                // y values:
                parameterIndex += width;
                for (int i=0; i < depth; i++){
                    pStatement.setInt((parameterIndex+i), (0));
                }
                
                // source id:
                parameterIndex += depth;
                pStatement.setInt((parameterIndex), 0);
                
                // update parameterIndex:
                parameterIndex += 1;
            }//for
                

            // [TEST] measure execution time:
//            long startTime = System.currentTimeMillis();

            // run the multiple query in one go:
            pStatement.execute();

            // [TEST] get end time and print:
//            long endTime = System.currentTimeMillis(); 
//            FESVoSystem.out.println("Executed query in " + (endTime - startTime) + " ms.");
//            FESVoSystem.out.print((endTime - startTime) + " ");
            
            
            // [TEST] measure time for getting resultSet:
//            startTime = System.currentTimeMillis();
            
            // get resultSet for each statement
            for (int i=0; i < traces.length; i++)
            {
                // get result set:
                resultSet = pStatement.getResultSet();

                // only interested in one trace if available, 
                // so no need to go throw all returned rows:
                // if no row is available, return null
                if (resultSet.next()){
                    int x = resultSet.getInt(1);
                    int y = resultSet.getInt(2);
                    byte [] samples = resultSet.getBytes(3);
                    traces[i] = new Trace(x, y, startingPoints[i].sourceID);
                    traces[i].setSamples(this.removeTraceHeader(samples));
                }//if
                
                // move to next result set:
                pStatement.getMoreResults();
            }
                
            
            // [TEST] get end time and print:
//            endTime = System.currentTimeMillis(); 
//            FESVoSystem.out2.println("Got results in " + (endTime - startTime) + " ms.");
//            FESVoSystem.out2.print((endTime - startTime) + " ");

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return traces array
        return traces;
    }//getTrace
    
    // ------------------------------------------------------------------------
    
    // [4/3/2012]
    // prepares a multi-statement PreparedStatement to search against exact valid locations
    // to be used later with getMultiTraces_v2()
    // multiStatementLength must be greated or equal to the length of points array passed to getMultiTraces_v2()
    public boolean buildTracePreparedStatement_v2(int multiStatementLength)
    {
        // store multiStatementLength:
        this.multiStatementLength = multiStatementLength;
        
        String query = "";
        
        // build the query:
        for (int st=0; st < multiStatementLength; st++){
            query += MACRO_QUERY;
        }//for
        
        try{
            // Creating a PreparedStatement object
            pStatement = con.prepareStatement(query);
        }
        
        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return true indicating that a prepared statement was successfully created:
        return true;
    }//buildTracePreparedStatement_v2
    
    // [4/3/2012]
    // returns an array of traces found at the provided exact locations (goe-tags)
    public Trace [] getMultiTraces_v2(GeoTag [] geoTags){
        
        // the returned traces object:
        Trace [] traces = new Trace[geoTags.length];

        // ResultSet objects:
        ResultSet resultSet;

        try
        {
            // parameterIndex:
            int parameterIndex = 1;
            
            // set parameters of the statements:
            for (int p=0; p < geoTags.length; p++)
            {
                // x values:
                pStatement.setInt((parameterIndex++), (geoTags[p].x));
                
                // y values:
                pStatement.setInt((parameterIndex++), (geoTags[p].y));
                
                // source id:
                pStatement.setInt((parameterIndex++), geoTags[p].sourceID);
                
            }//for
            
            // if points (geo-tags) are less than the multi-statement length,
            // set remaining parameters to 0:
            for (int p=geoTags.length; p < (this.multiStatementLength); p++)
            {
                // x values:
                pStatement.setInt((parameterIndex++), (0));
                
                // y values:
                pStatement.setInt((parameterIndex++), (0));
                
                // source id:
                pStatement.setInt((parameterIndex++), 0);
                
            }//for
                

            // [TEST] measure execution time:
//            long startTime = System.currentTimeMillis();

            // run the multiple query in one go:
            pStatement.execute();

            // [TEST] get end time and print:
//            long endTime = System.currentTimeMillis(); 
//            FESVoSystem.out.println("Executed query in " + (endTime - startTime) + " ms.");
//            FESVoSystem.out.print((endTime - startTime) + " ");
            
            
            // [TEST] measure time for getting resultSet:
//            startTime = System.currentTimeMillis();
            
            // get resultSet for each statement
            for (int i=0; i < traces.length; i++)
            {
                // get result set:
                resultSet = pStatement.getResultSet();

                // if no row is available, return null
                if (resultSet.next()){
                    byte [] samples = resultSet.getBytes(1);
                    
                    // [TEST] print some trace samples: (17/3/2012)
//                    if (this.tracesToPrint > 0){
//                        ByteBuffer tempBuffer = ByteBuffer.wrap(samples);
//                        tempBuffer.order(ByteOrder.LITTLE_ENDIAN);
//                        DataAnalyzer.writeFloatBufferToFile(tempBuffer.asFloatBuffer(), "trace_"+tracesToPrint+".txt");
//                        tracesToPrint--;
//                    }
                    
                    traces[i] = new Trace(geoTags[i]);
                    traces[i].setSamples(this.removeTraceHeader(samples));
                }//if
                
                // move to next result set:
                pStatement.getMoreResults();
            }
                
            
            // [TEST] get end time and print:
//            endTime = System.currentTimeMillis(); 
//            FESVoSystem.out2.println("Got results in " + (endTime - startTime) + " ms.");
//            FESVoSystem.out2.print((endTime - startTime) + " ");

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return traces array
        return traces;
    }//getMultiTraces_v2
    
    // ------------------------------------------------------------------------
    
    public boolean closeTracePreparedStatement()
    {
        try{
            // Close the statement
            pStatement.close();
            System.out.println("Statement object closed.");
        }
        
        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Closing statement failed.") ;

        }//catch
        
        // free pStatement object:
        pStatement = null;
        
        // return true indicated that the prepared statement is closed:
        return true;
        
    }//closeTracePreparedStatement
    
    // ------------------------------------------------------------------------
    
    private byte [] removeTraceHeader(byte [] b)
    {
        return Arrays.copyOfRange(b, TRACE_HEADER_SIZE, b.length);
    }

    // ------------------------------------------------------------------------
    
    // [24/6/2012]
    // prepares a multi-statement PreparedStatement to search against exact valid locations
    // to be used later with getMultiHorizons()
    // relying on multiStatementLength set by buildTracePreparedStatement_v2()
    // and the given max. number of requested horizons
    public boolean buildHorizonPreparedStatement(int horizons)
    {
        // a multiStatementLength should have been set by buildTracePreparedStatement_v2(int multiStatementLength)
        if (this.multiStatementLength <= 0) // not set
            return false;
        
        // save number of required horizons:
        this.numOfHorizons = horizons;
        
        String query = "SELECT "
                + PROPERTY_VALUE_COLUMN + ", "
                + TWO_WAY_TRAVEL_TIME_COLUMN
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + X_COLUMN + "=?"
                  + " and " + Y_COLUMN + "=?" 
                  + " and " + SOURCE_ID_COLUMN + "=?"//" in (?,?)"
                  + " and " + PROPERTY_ID_COLUMN + "=?"
                  + " and " + PROPERTY_VALUE_COLUMN + " in (?";
        
        // complete the query to cover the required number of horizons:
        for (int i=1; i < horizons; i++)
            query += ",?";
        
        // end query:
        query += ");";
        
        // build a multi-statement query:
        String msQuery = "";
        for (int st=0; st < multiStatementLength; st++){
            msQuery += query;
        }//for
        
        try{
            // Creating a PreparedStatement object
            hPStatement = con.prepareStatement(msQuery);
        }
        
        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Couldn't create a PrepareStatement!") ;

        }//catch

        // return true indicating that a prepared statement was successfully created:
        return true;
    }//buildHorizonPreparedStatement
    
    // ------------------------------------------------------------------------
    
    // [24/6/2012]
    // returns an array of horizons found at the provided exact locations (goe-tags)
    // this method supports the new queue system
    public Feature [] getMultiHorizons(Map<Point, Set<Integer>> geoTags, int srdsSrcID){
        
        // the returned features object:
        ArrayList<Feature> features = new ArrayList<Feature>();

        // ResultSet objects:
        ResultSet resultSet;
        
        // array of points:
        Point [] points = geoTags.keySet().toArray(new Point[geoTags.size()]);

        try
        {
            // parameterIndex:
            int parameterIndex = 1;
            
            // set parameters of the statements:
            for (Point p : points)
            {
                // x value:
                hPStatement.setInt((parameterIndex++), (p.x));
                
                // y value:
                hPStatement.setInt((parameterIndex++), (p.y));
                
                // src id value:
                //hPStatement.setInt((parameterIndex++), (srdsSrcID+10));//[TEST]
                hPStatement.setInt((parameterIndex++), (srdsSrcID));
                
                // prop id value:
                hPStatement.setInt((parameterIndex++), (HORIZON_PROPERTY_ID));
                
                // prop_val values:
                // first set what's requested in geoTags
                // then fill remaining, if any, with -1:
                int hInd = 0; // horizon index
                for (int propVal : geoTags.get(p)){
                    hPStatement.setInt((parameterIndex++), (propVal));
                    hInd++;
                }
                for (int i=hInd; i < this.numOfHorizons; i++)
                    hPStatement.setInt((parameterIndex++), (-1));
                
                // go to next point
            }
            
            // if points (geo-tags) are less than the multi-statement length,
            // set remaining parameters to 0, -1:
            for (int p=geoTags.size(); p < (this.multiStatementLength); p++)
            {
                // x value:
                hPStatement.setInt((parameterIndex++), (0));
                
                // y value:
                hPStatement.setInt((parameterIndex++), (0));
                
                // src id value:
                hPStatement.setInt((parameterIndex++), (-1));
                
                // prop id value:
                hPStatement.setInt((parameterIndex++), (-1));
                
                // prop_val values:
                for (int i=0; i < this.numOfHorizons; i++)
                    hPStatement.setInt((parameterIndex++), (-1));
                
                // go to next point
                
            }//for
            
            // run the multiple query in one go:
            hPStatement.execute();

            // get resultSet for each statement
            for (Point p : points)
            {
                // get result set:
                resultSet = hPStatement.getResultSet();

                // move on rows
                while(resultSet.next())
                {
                    // get prop_val:
                    int propVal = resultSet.getInt(1);
                    
                    // get two-way-travel-time (TWT)
                    int twt = resultSet.getInt(2);
                    
                    // create a Feature object with prop_val as its id:
                    Feature f = new Feature (p.x, p.y, propVal);
                    
                    // add twt to this feature:
                    f.addW(twt);
                    
                    // add this feature to the feature array list:
                    features.add(f);
                }//if
                
                // move to next result set:
                hPStatement.getMoreResults();
            }   
            
        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        // return traces array
        return features.toArray(new Feature[0]);
    }//getMultiHorizons
    
    // ------------------------------------------------------------------------
    
    // [24/6/2012]
    public boolean closeHorizonPreparedStatement()
    {
        try{
            // Close the statement
            hPStatement.close();
            System.out.println("Horizons Statement object closed.");
        }
        
        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Closing statement failed.") ;

        }//catch
        
        // free hPStatement object:
        hPStatement = null;
        
        // return true indicated that the prepared statement is closed:
        return true;
        
    }//closeHorizonPreparedStatement
    
    // ------------------------------------------------------------------------
    
    // [24/9/2012]
    // returns an array of all horizons
    public Feature [] getAllHorizons(int srdsSrcID){
        return getAllHorizons(srdsSrcID, null);
    }
    
    // [30/9/2012]
    // returns an array of all horizons based on a given timestamp
    public Feature [] getAllHorizons(int srdsSrcID, DataTimestamp timestamp){
        return getAllHorizons(srdsSrcID, timestamp, null);
    }
    
    // [2/10/2012]
    // returns an array of required horizons based on a given timestamp and set of prop_val 
    public Feature [] getAllHorizons(int srdsSrcID, DataTimestamp timestamp, Set requiredHorizons){
        
        // the returned features object:
        ArrayList<Feature> features = new ArrayList<Feature>();

        // required columns
        String selectedColumns =  X_COLUMN + ","
                                + Y_COLUMN + ","
                                + TWO_WAY_TRAVEL_TIME_COLUMN + ","
                                + PROPERTY_VALUE_COLUMN 
                                ;
        
        // complete query:
        String query = 
                "SELECT " + selectedColumns
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + srdsSrcID
                  + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID;
        
        // specify which horizons to load if requested:
        if (requiredHorizons != null){
            query += " and " + PROPERTY_VALUE_COLUMN + " in " + printSetForQuery(requiredHorizons);
        }
        
        // include user's interpretation if BASELINE timestamp is not requested:
        if (timestamp == null || !timestamp.isBaseline())
        {
            query +=
                  " UNION "
                    + generateUserInputSelectQuery(
                        selectedColumns, srdsSrcID, 
                        INSERTION_RELATION_CODE, HORIZON_PROPERTY_ID, timestamp, requiredHorizons)
                    
                + " EXCEPT "
                    + generateUserInputSelectQuery(
                        selectedColumns, srdsSrcID, 
                        DELETION_RELATION_CODE, HORIZON_PROPERTY_ID, timestamp, requiredHorizons)

                    ;
        }//if
                
                
        
        try
        {
            
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // Performing the queries:
                ResultSet resultSet = statement.executeQuery(query);

                int size = resultSet.getFetchSize();
                
                // move on rows
                while(resultSet.next())
                {
                    int i=1; // parameter index
                    
                    // get feature parameters:
                    int x = resultSet.getInt(i++);
                    int y = resultSet.getInt(i++);
                    int twt = resultSet.getInt(i++);
                    int propVal = resultSet.getInt(i++);
                    
                    // create a Feature object with prop_val as its id:
                    Feature f = new Feature (x, y, propVal);
                    
                    // add twt to this feature:
                    f.addW(twt);
                    
                    // add this feature to the feature array list:
                    features.add(f);
                }//if
                        
                long time = System.currentTimeMillis() - startTime; 
                                
                System.out.println("Retrieved all horizons points from DB in: " + (time) + " ms.");
                
                
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }
            
        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch

        // return traces array
        return features.toArray(new Feature[0]);
    }//getAllHorizons
    
    
    // [2/10/2012]
    // returns an array of required horizons based on a given timestamp and set of prop_val 
    public Feature [] getAllHorizonsOnlyAtTS(int srdsSrcID, DataTimestamp timestamp, Set requiredHorizons){
        
        // the returned features object:
        ArrayList<Feature> features = new ArrayList<Feature>();

        // required columns
        String selectedColumns =  X_COLUMN + ","
                                + Y_COLUMN + ","
                                + TWO_WAY_TRAVEL_TIME_COLUMN + ","
                                + PROPERTY_VALUE_COLUMN 
                                ;
        
        String query;
        
        if (timestamp.isBaseline()){
            query = 
                "SELECT " + selectedColumns
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + srdsSrcID
                  + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                  + " and " + PROPERTY_VALUE_COLUMN + " in " + printSetForQuery(requiredHorizons);
        }
        
        else{
            query = 
                "SELECT " + selectedColumns
                    + " FROM " + DATABASE + "." + FEATURE_TABLE + " AS A"
                + " INNER JOIN"
                    + " (SELECT B.* FROM " 
                        + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS B"
                        + " INNER JOIN "
                        + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS C"
                        + " ON B." + GROUP_ID_COLUMN + "= C." + GROUP_ID_COLUMN
                        + " AND C." + SOURCE_ID_COLUMN + "=" + srdsSrcID // baseline source id
                        + " AND C." + GROUPING_RELATION_CODE_COLUMN + "=" + BASELINE_RELATION_CODE
                        + " AND B." + GROUPING_RELATION_CODE_COLUMN + "=" + INSERTION_RELATION_CODE
                        + " AND B." + GROUPING_EFFECTIVE_TIME_COLUMN + "='" + timestamp.getTimestampAsString() + "'"
                        + " ) AS D"
                    + " ON A." + SOURCE_ID_COLUMN + " = D." + SOURCE_ID_COLUMN
                    + " AND A." + TIMESTAMP_COLUMN + "= D." + GROUPING_EFFECTIVE_TIME_COLUMN
                    + " AND A." + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                    + " AND A." + PROPERTY_VALUE_COLUMN + " in " + printSetForQuery(requiredHorizons);
        
        }//else
                
                
        
        try
        {
            
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // Performing the queries:
                ResultSet resultSet = statement.executeQuery(query);

                int size = resultSet.getFetchSize();
                
                // move on rows
                while(resultSet.next())
                {
                    int i=1; // parameter index
                    
                    // get feature parameters:
                    int x = resultSet.getInt(i++);
                    int y = resultSet.getInt(i++);
                    int twt = resultSet.getInt(i++);
                    int propVal = resultSet.getInt(i++);
                    
                    // create a Feature object with prop_val as its id:
                    Feature f = new Feature (x, y, propVal);
                    
                    // add twt to this feature:
                    f.addW(twt);
                    
                    // add this feature to the feature array list:
                    features.add(f);
                }//if
                        
                long time = System.currentTimeMillis() - startTime; 
                                
                System.out.println("Retrieved all horizons points from DB in: " + (time) + " ms.");
                
                
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }
            
        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch

        // return traces array
        return features.toArray(new Feature[0]);
    }//getAllHorizons
    
    
    // helper method:
    private String generateUserInputSelectQuery(String selectedColumns, int srdsSrcID, int groupingRelCode, int propertyID, DataTimestamp timestamp, Set requiredHorizons)
    {
        String query = 
                "SELECT " + selectedColumns
                    + " FROM " + DATABASE + "." + FEATURE_TABLE + " AS A"
                + " INNER JOIN"
                    + " (SELECT B.* FROM " 
                        + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS B"
                        + " INNER JOIN "
                        + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS C"
                        + " ON B." + GROUP_ID_COLUMN + "= C." + GROUP_ID_COLUMN
                        + " AND C." + SOURCE_ID_COLUMN + "=" + srdsSrcID // baseline source id
                        + " AND C." + GROUPING_RELATION_CODE_COLUMN + "=" + BASELINE_RELATION_CODE
                        + " AND B." + GROUPING_RELATION_CODE_COLUMN + "=" + groupingRelCode;
        
        // add a timestamp if required:
        if (timestamp != null){
            query +=
                    " AND B." + GROUPING_EFFECTIVE_TIME_COLUMN + "<='" + timestamp.getTimestampAsString() + "'";
        }
        
        query +=
                     " ) AS D"
                + " ON A." + SOURCE_ID_COLUMN + " = D." + SOURCE_ID_COLUMN
                + " AND A." + TIMESTAMP_COLUMN + "= D." + GROUPING_EFFECTIVE_TIME_COLUMN
                + " AND A." + PROPERTY_ID_COLUMN + "=" + propertyID
                ;
        
        // specify which horizons to load if requested:
        if (requiredHorizons != null){
            query += " AND A." + PROPERTY_VALUE_COLUMN + " in " + printSetForQuery(requiredHorizons);
        }
        
        return query;
                
    }//generateUserInputSelectQuery
    
    // helper method:
    private String printSetForQuery(Set<?> set){
        String s = "(";
        
        Iterator<?> itr = set.iterator();
        
        if (itr.hasNext()){
            s += itr.next().toString();
        }
        
        while (itr.hasNext()){
            s += "," + itr.next().toString();
        }
        
        s += ")";
        
        return s;
    }//printSetForQuery
    
    // ------------------------------------------------------------------------
    // [13/2/2012] returns an array containing:
    // x min, x max, y min, y max, x distinct count, y distinct count
    public int [] getRangeAndDistinctCount(int sourceID)
    {
        // Query statement:
        String sQuery = "SELECT"
                + " min(" + X_COLUMN + "),"
                + " max(" + X_COLUMN + "),"
                + " min(" + Y_COLUMN + "),"
                + " max(" + Y_COLUMN + "),"
                + " count(distinct " + X_COLUMN + "),"
                + " count(distinct " + Y_COLUMN + ")"
                + " FROM " + DATABASE + "." + TRACE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + sourceID + ";";
                
        // the returned queried values from database:
        int [] values = new int[6];

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing the query
                ResultSet resultSet = statement.executeQuery(sQuery);
                
                resultSet.next(); // go to first row
                for (int i=0; i < values.length; i++)
                    values[i] = resultSet.getInt(i+1);
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }
        
        return values;
    }//getRangeAndDistinctCount
    
    // ------------------------------------------------------------------------
    
    // [3/3/2012] returns all traces coordinates of the given sourceID
    public Point [] getTraceDataCoordinates(int sourceID)
    {
        // query to get number of records under this sourceID:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + TRACE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + sourceID + ";";
                
        // Query to find all (x,y) coordinates:
        String query2 = "SELECT "
                + X_COLUMN + ","
                + Y_COLUMN
                + " FROM " + DATABASE + "." + TRACE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + sourceID + ";";
                
        // the returned queried values from database:
        Point [] points;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing the queries:
                ResultSet resultSet = statement.executeQuery(query1+query2);
                
                resultSet.next(); // go to first row
                
                // get number of records:
                int records = resultSet.getInt(1);
                
                // create an array of points with number of records:
                points = new Point[records];
                
                // get next result set; coordinates:
                statement.getMoreResults();
                resultSet = statement.getResultSet();
                
                int i=0;
                while(resultSet.next()){
                    points[i++] = new Point(resultSet.getInt(1), resultSet.getInt(2));
                }
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }
        
        return points;
    }//getTraceDataCoordinate
    
    // ------------------------------------------------------------------------
    
    // [22/6/2012] returns all horizons coordinates of the given sourceID and horizon's prop_val:
    public Point [] getHorizonDataCoordinates(int sourceID, int prop_val)
    {
        // query to get number of records under this sourceID:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + sourceID
                + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                + " and " + PROPERTY_VALUE_COLUMN + "=" + prop_val + ";";
                
        // Query to find all (x,y) coordinates:
        String query2 = "SELECT "
                + X_COLUMN + ","
                + Y_COLUMN
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + SOURCE_ID_COLUMN + "=" + sourceID
                + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                + " and " + PROPERTY_VALUE_COLUMN + "=" + prop_val + ";";
                
        // the returned queried values from database:
        Point [] points;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing the queries:
                ResultSet resultSet = statement.executeQuery(query1+query2);
                
                resultSet.next(); // go to first row
                
                // get number of records:
                int records = resultSet.getInt(1);
                
                // create an array of points with number of records:
                points = new Point[records];
                
                // get next result set; coordinates:
                statement.getMoreResults();
                resultSet = statement.getResultSet();
                
                int i=0;
                while(resultSet.next()){
                    points[i++] = new Point(resultSet.getInt(1), resultSet.getInt(2));
                }
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }
        
        return points;
    }//
    
    // ------------------------------------------------------------------------
    
    // [26/4/2012] returns an array of SeismicMetadata
    // [23/10/2012] updated to contain all available prop_val (features)
    public SeismicMetadata[] getSourcesMetadata()
    {
        // Query statement to get number of records in metadata table:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_TYPE_ID_COLUMN + "=" + ORIGINAL_SOURCE_TYPE_ID // [18/09/2012]
                + ";";
        
        // Query statement selecting all in the metadata table:
        String query2 = "SELECT * "
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_TYPE_ID_COLUMN + "=" + ORIGINAL_SOURCE_TYPE_ID // [18/09/2012]
                + " ORDER BY " + SOURCE_ID_COLUMN
                + ";";
        
        // [23/10/2012] Query to get all prop_val of a given src_id and prop_id:
        String query3 = "SELECT DISTINCT " + PROPERTY_VALUE_COLUMN
                + " FROM " + DATABASE + "." + FEATURE_TABLE
                + " WHERE " + SOURCE_ID_COLUMN + "=?"
                + " AND " + PROPERTY_ID_COLUMN + "=?"
                + " ORDER BY " + PROPERTY_VALUE_COLUMN
                + ";";
                
        // the returned sets from database:
        SeismicMetadata [] metadata;

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            PreparedStatement propValSt = null; // [23/10/2012]
            
            try
            {
                // [23/10/2012] prepared statement for prop_val:
                propValSt = con.prepareStatement(query3);
                
                // Performing both queries
                ResultSet resultSet = statement.executeQuery(query1+query2);
                
                resultSet.next(); // go to first row
                
                // get number of records:
                int records = resultSet.getInt(1);
                
                // create an array of metadata with number of records:
                metadata = new SeismicMetadata[records];
                
                // get next result set; metadata info:
                statement.getMoreResults();
                resultSet = statement.getResultSet();
                
                int i=0;
                while(resultSet.next()){
                    metadata[i] = 
                            new SeismicMetadata(
                                    resultSet.getInt(SOURCE_ID_COLUMN), 
                                    resultSet.getString(SOURCE_DESCRIPTION_COLUMN), 
                                    resultSet.getInt(FIRST_INLINE_COLUMN),
                                    resultSet.getInt(LAST_INLINE_COLUMN),
                                    resultSet.getInt(FIRST_XLINE_COLUMN),
                                    resultSet.getInt(LAST_XLINE_COLUMN),
                                    resultSet.getInt(X_MIN_COLUMN),
                                    resultSet.getInt(X_MAX_COLUMN),
                                    resultSet.getInt(Y_MIN_COLUMN),
                                    resultSet.getInt(Y_MAX_COLUMN),
                                    resultSet.getInt(TIME_MIN_COLUMN),
                                    resultSet.getInt(TIME_MAX_COLUMN),
                                    resultSet.getFloat(AMPLITUDE_MIN_COLUMN),
                                    resultSet.getFloat(AMPLITUDE_MAX_COLUMN),
                                    SEGY.SEGY_4BYTE_IEEE_FLOATING_POINT_FORMAT, // default sample format
                                    (int)(resultSet.getDouble(SAMPLE_INTERVAL_COLUMN) * 1000), // converting from sec. to ms.
                                    resultSet.getInt(SAMPLES_PER_TRACE_COLUMN),
                                    new Point(resultSet.getInt(POINT_A_X_COLUMN), resultSet.getInt(POINT_A_Y_COLUMN)),
                                    new Point(resultSet.getInt(POINT_B_X_COLUMN), resultSet.getInt(POINT_B_Y_COLUMN)),
                                    new Point(resultSet.getInt(POINT_C_X_COLUMN), resultSet.getInt(POINT_C_Y_COLUMN)),
                                    new Point(resultSet.getInt(POINT_D_X_COLUMN), resultSet.getInt(POINT_D_Y_COLUMN)),
                                    resultSet.getString(UTM_ZONE_COLUMN)
                                    );
                    
                    // [23/10/2012] 
                    //--------------
                    
                    ResultSet propValResult;
                    
                    // get all prop_val of horizons:
                    propValSt.setInt(1, metadata[i].srdsSrcID);
                    propValSt.setInt(2, HORIZON_PROPERTY_ID);
                    propValResult = propValSt.executeQuery();
                    ArrayList<Integer> hPropVal = new ArrayList<Integer>();
                    while(propValResult.next()){
                        hPropVal.add(propValResult.getInt(1));
                    }
                    metadata[i].setHorizonsPropVal(hPropVal);
                    
                    // get all prop_val of faults:
                    propValSt.setInt(1, metadata[i].srdsSrcID);
                    propValSt.setInt(2, FAULT_PROPERTY_ID);
                    propValResult = propValSt.executeQuery();
                    ArrayList<Integer> fPropVal = new ArrayList<Integer>();
                    while(propValResult.next()){
                        fPropVal.add(propValResult.getInt(1));
                    }
                    metadata[i].setFaultsPropVal(fPropVal);
                    
                    
                    //--------------
                    
                    // next metadata
                    i++;
                }
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                System.out.println("Statement object closed. \n");
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }
        
        return metadata;
    }//getSourcesMetadata
    
    // ------------------------------------------------------------------------
    
    // [3/10/2012] returns an array of grouping effective timestamp 
    // related to a set of required horizons
    public ArrayList<DataTimestamp> getGroupingEffectiveTimestamps(int baselineSrcID, Set requiredHorizons)
    {
        String sql;
        
        sql = "SELECT"
                +  " A." + GROUPING_EFFECTIVE_TIME_COLUMN
                + ", A." + GROUPING_RELATION_CODE_COLUMN
                + ", B." + PROPERTY_VALUE_COLUMN
                + ", A." + SOURCE_ID_COLUMN
                + ", C." + SOURCE_DESCRIPTION_COLUMN
                
                + " FROM " + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS A"
                + " INNER JOIN"
                    + " (SELECT DISTINCT "  + TIMESTAMP_COLUMN + "," 
                                            + SOURCE_ID_COLUMN + ","
                                            + PROPERTY_VALUE_COLUMN
                    + " FROM " + DATABASE + "." + FEATURE_TABLE + ") AS B"
                
                + " ON A." + GROUPING_EFFECTIVE_TIME_COLUMN + " = B." + TIMESTAMP_COLUMN
                + " AND A." + SOURCE_ID_COLUMN + " = B." + SOURCE_ID_COLUMN
                + " AND A." + GROUP_ID_COLUMN + "=" + baselineSrcID
                + " AND A." + GROUPING_RELATION_CODE_COLUMN + "<>" + BASELINE_RELATION_CODE
                + " AND B." + PROPERTY_VALUE_COLUMN + " in " + printSetForQuery(requiredHorizons)
                
                + " LEFT JOIN " + DATABASE + "." + METADATA_TABLE + " AS C"
                + " ON A." + SOURCE_ID_COLUMN + " = C." + SOURCE_ID_COLUMN
                
                + " ORDER BY A." + GROUPING_EFFECTIVE_TIME_COLUMN + " DESC"
                + ";";
        
        // Set of timestamps:
        ArrayList<DataTimestamp> timestamps = new ArrayList<DataTimestamp>();

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // execute query:
                ResultSet resultSet = statement.executeQuery(sql);
                
                while(resultSet.next())
                {
                    int i=1;
                    
                    Timestamp ts = resultSet.getTimestamp(i++);
                    int relationInt = resultSet.getInt(i++);
                    int propVal = resultSet.getInt(i++);
                    int userSrcID = resultSet.getInt(i++);
                    String userName = resultSet.getString(i++);
                    
                    DataGroupingRelation relation = null;
                    if (relationInt == INSERTION_RELATION_CODE)
                        relation = DataGroupingRelation.INSERTION;
                    else if (relationInt == DELETION_RELATION_CODE)
                        relation = DataGroupingRelation.DELETION;
                    else
                        System.err.println("Unknown relation code! " + relationInt);
                    
                    
                    timestamps.add(new DataTimestamp(ts, relation, baselineSrcID, propVal, userSrcID, userName));
                }
                
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }
        
        return timestamps;
    }//getGroupingEffectiveTimestamps
    
    
    // [3/10/2012] returns an array of grouping effective timestamp 
    // related to a set of required horizons
    // [10/10/2012] added support to DataTimestamp object
    public ArrayList<DataTimestamp> getInsertionTypeGroupingEffectiveTimestamps(int baselineSrcID, int requiredFeatureValue)
    {
        String sql;
        
        // select distinct timestamps from SRDS table to consider only the required horizons:
        sql = "SELECT"
                +  " A." + GROUPING_EFFECTIVE_TIME_COLUMN
                + ", A." + SOURCE_ID_COLUMN
                + ", C." + SOURCE_DESCRIPTION_COLUMN
                
                + " FROM " + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS A"
                + " INNER JOIN"
                    + " (SELECT DISTINCT "  + TIMESTAMP_COLUMN + "," 
                                            + SOURCE_ID_COLUMN + ","
                                            + PROPERTY_VALUE_COLUMN
                    + " FROM " + DATABASE + "." + FEATURE_TABLE + ") AS B"
                
                + " ON A." + GROUPING_EFFECTIVE_TIME_COLUMN + " = B." + TIMESTAMP_COLUMN
                + " AND A." + SOURCE_ID_COLUMN + " = B." + SOURCE_ID_COLUMN
                + " AND A." + GROUP_ID_COLUMN + "=" + baselineSrcID
                + " AND A." + GROUPING_RELATION_CODE_COLUMN + "=" + INSERTION_RELATION_CODE
                + " AND B." + PROPERTY_VALUE_COLUMN + "=" + requiredFeatureValue
                
                + " LEFT JOIN " + DATABASE + "." + METADATA_TABLE + " AS C"
                + " ON A." + SOURCE_ID_COLUMN + " = C." + SOURCE_ID_COLUMN
                
                + " ORDER BY A." + GROUPING_EFFECTIVE_TIME_COLUMN + " DESC"
                + ";";
        
        // Set of timestamps:
        ArrayList<DataTimestamp> timestamps = new ArrayList<DataTimestamp>();

        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // execute query:
                ResultSet resultSet = statement.executeQuery(sql);
                
                while(resultSet.next())
                {
                    Timestamp ts = resultSet.getTimestamp(1);
                    int userSrcID = resultSet.getInt(2);
                    String userName = resultSet.getString(3);
                    
                    timestamps.add(new DataTimestamp(ts, DataGroupingRelation.INSERTION, baselineSrcID, requiredFeatureValue, userSrcID, userName));
                }//while
                
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
            }

        }

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }
        
        return timestamps;
    }//getGroupingEffectiveTimestamps
    
    // =======================================================================
    
    // [10/9/2012]
    /**
     * Shifts some horizon points 
     * @param geoTags geo-tags of points to be shifted
     * @param timeShift time shift to be applied on each point's twt
     * @param baselineSrcID SRDS src id of the original horizon
     */
    public void shiftHorizon(Set<GeoTag> geoTags, int timeShift, int baselineSrcID)
    {
        // [TEST] measure execution time:
        long startTime = System.currentTimeMillis();
        long time;
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();

            PreparedStatement pStatement = null;
            
            try
            {
                int userSrcID;
                String sql;
                String ts;
                int count; 
                int [] counts;
                
                // generate a new src_id for this interpretation:
                userSrcID = getUserSrdsSrcID(baselineSrcID, statement);
        
                // Creating a prepared statement for inserting horizon points:
                // -----------------------------------------------------------
                String psSql = 
                      "INSERT"
                    + " INTO " + DATABASE + "." + FEATURE_TABLE 
                    + " SELECT " + X_COLUMN + ","
                                 + Y_COLUMN + ","
                                 + "NULL,"
                                 + "(" + TWO_WAY_TRAVEL_TIME_COLUMN + "+?)," // timeShift
                                 + "?," // timestamp
                                 + userSrcID + ","
                                 + PROPERTY_ID_COLUMN  + ","
                                 + PROPERTY_VALUE_COLUMN + ","
                                 + "NULL,"
                                 + "NULL"
                    + " FROM " + DATABASE + "." + FEATURE_TABLE 
                    + " WHERE " + X_COLUMN + "=?" // x
                      + " and " + Y_COLUMN + "=?" //y
                      + " and " + SOURCE_ID_COLUMN + "=" + baselineSrcID
                      + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                      + " and " + PROPERTY_VALUE_COLUMN + "=?" // tag.sourceID
                    + ";";

                pStatement = con.prepareStatement(psSql);
            
            
                
                // Need two grouping types for shifting: deletion & insertion
                // ----------------------------------------------------------
                
                // get current timestamp:
                ts = getCurrentTimestamp(statement);
                
                // create a deletion type grouping:
                // [TEMP] group id = original source id
                sql = "INSERT"
                    + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                    + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + "," + GROUPING_EFFECTIVE_TIME_COLUMN + ")"
                    + " VALUES (" + baselineSrcID + "," + userSrcID + "," + DELETION_RELATION_CODE + ",'" + ts + "')"
                    + ";";

                // run query:
                count = statement.executeUpdate(sql);
                
                if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");
                else // [TEST] print
                    System.out.println("deletion type grouping row was inserted successfully");
        
        
                
                // insert each tag with the user's src_id to indicate deletion:
                for (GeoTag tag : geoTags){
                    
                    int i=1;
                    
                    // Set Time Shift to 0:
                    pStatement.setInt(i++, 0);
                    
                    // Set timestamp:
                    pStatement.setString(i++, ts);
                    
                    // Set X:
                    pStatement.setInt(i++, tag.x);
                    
                    // Set Y:
                    pStatement.setInt(i++, tag.y);
                    
                    // Set prop_val:
                    pStatement.setInt(i++, tag.sourceID);
                    
                    
                    // add batch to statement:
                    pStatement.addBatch();
                    
                }//for
                
                // execute batch:
                counts = pStatement.executeBatch();
                
                // [TEST] print
                System.out.println("batch was executed successfully for deletion tagged rows");
                
                // clear batch:
                pStatement.clearBatch();
                
                // [TEMP] wait for 1 or 2 sec since precision of timestamp is in seconds
                Thread.sleep(2000);
                
                // -----------------------
                
                // get current timestamp:
                ts = getCurrentTimestamp(statement);
                
                // a query to end the latest grouping:
                String endGroupingSql = "UPDATE "
                        + DATABASE + "." + SOURCE_GROUPING_TABLE
                        + " SET " + GROUPING_END_TIME_COLUMN + "=" + CURRENT_TIMESTAMP
                        + " WHERE " + GROUP_ID_COLUMN + "=" + baselineSrcID
                        + " AND " + SOURCE_ID_COLUMN + "=" + userSrcID
                        + " AND " + GROUPING_END_TIME_COLUMN + " is NULL"
                        + ";";

                // run query:
                count = statement.executeUpdate(endGroupingSql);
                if (count < 1) throw new SQLException("Grouping row was not updated successfully!");
                
                // -----------------------
                
                // create an insertion type grouping:
                // [TEMP] group id = original source id
                sql = "INSERT"
                    + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                    + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + "," + GROUPING_EFFECTIVE_TIME_COLUMN + ")"
                    + " VALUES (" + baselineSrcID + "," + userSrcID + "," + INSERTION_RELATION_CODE + ",'" + ts + "')"
                    + ";";

                // run query:
                count = statement.executeUpdate(sql);
                
                if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");
                else // [TEST] print
                    System.out.println("insertion type grouping row was inserted successfully");
                
                // insert each tag with the user's src_id to indicate deletion:
                
                // add an "INSERT" batch for each tag:
                for (GeoTag tag : geoTags){
                    
                    int i=1;
                    
                    // Set Time Shift to 0:
                    pStatement.setInt(i++, timeShift);
                    
                    // Set timestamp:
                    pStatement.setString(i++, ts);
                    
                    // Set X:
                    pStatement.setInt(i++, tag.x);
                    
                    // Set Y:
                    pStatement.setInt(i++, tag.y);
                    
                    // Set prop_val:
                    pStatement.setInt(i++, tag.sourceID);
                    
                    
                    // add batch to statement:
                    pStatement.addBatch();
                    
                }//for
                
                // execute batch:
                counts = pStatement.executeBatch();
                
                // [TEST] print
                System.out.println("batch was executed successfully for insertion tagged rows");
                
                // -----------------------
                
                // end the grouping:
                count = statement.executeUpdate(endGroupingSql);
                if (count < 1) throw new SQLException("Grouping row was not updated successfully!");
                
                // -----------------------
        
                long endTime = System.currentTimeMillis(); 
                
                time = endTime - startTime;
                
                System.out.println("Time: " + (time) + " ms.");
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
                if (pStatement != null)
                    pStatement.close();
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

    }//shiftHorizon
    
    // [10/9/2012] returns the user's source id for interpretation:
    // adds this src id to the SRDS SRC table if not already added
    // and creates a new grouping:
    private int getUserSrdsSrcID(int baselineSrcID, Statement statement) throws SQLException
    {
        // [TEMP] assume user id = original id + 10
        int userSrcID = baselineSrcID + 10;
        
        
        // If not already, add this src id to the SRDS SRC table:
        // ------------------------------------------------------
        
        // a query to check if userSrcID exists:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_ID_COLUMN + "=" + userSrcID
                + ";";
        
        // a query to insert if userSrcID does not exist:
        String query2 = "INSERT"
                + " INTO " + DATABASE + "." + METADATA_TABLE
                + " (" + SOURCE_ID_COLUMN + "," + SOURCE_TYPE_ID_COLUMN + ")"
                + " VALUES (" + userSrcID + "," + USER_SOURCE_TYPE_ID + ")"
                + ";";
        
        // a query to create a new grouping:
        // [TEMP] make group id = original source id
        String query3 = "INSERT"
                + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + ")"
                + " VALUES (" + baselineSrcID + "," + baselineSrcID + "," + BASELINE_RELATION_CODE + ")"
                + ";";
        
        // Performing query 1:
        ResultSet resultSet = statement.executeQuery(query1);

        resultSet.next(); // go to first row

        // get number of records:
        int records = resultSet.getInt(1);

        if (records != 1)
        {
            // perform query 2 to add userSrcID to srouces table:
            int count = statement.executeUpdate(query2);
            if (count != 1) throw new SQLException("User's src_id was not inserted successfully!");

            // perform query 3 to create new grouping:
            count = statement.executeUpdate(query3);
            if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");

        }//if
                
        return userSrcID;
    }//getUserSrdsSrcID
    
    // ------------------------------------------------------------------------
    
    // [24/9/2012] returns current timestamp from DB as a String with no fractions in seconds
    // given Statement object (so no need to recreate one)
    private String getCurrentTimestamp(Statement statement) throws SQLException
    {
        String tsQuery = "select " + CURRENT_TIMESTAMP + ";";
        ResultSet resultSet = statement.executeQuery(tsQuery);
        resultSet.next();
        Timestamp ts = resultSet.getTimestamp(1);

        return ts.toString().split("\\.")[0];
    }//getCurrentTimestamp
    
    // =======================================================================
    
    // ------------------------------------------------------------------------
    // | the following methods replaces most of the above shifting related    |
    // | these methods are general to make any deletion or insertion          |
    // | into SRDS table                                                      |
    // | these are public methods to be accessed from the loader              |
    // ------------------------------------------------------------------------
    
    // [27/9/2012] returns the user's source id for interpretation:
    // adds this src id to the SRDS SRC table if not already added
    // and creates a new grouping:
    public int getUserSrdsSrcID_OLD(int baselineSrcID)
    {
        // [TEMP] assume user id = original id + 10
        int userSrcID = baselineSrcID + 10;
        
        
        // If not already, add this src id to the SRDS SRC table:
        // ------------------------------------------------------
        
        // a query to check if userSrcID exists:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_ID_COLUMN + "=" + userSrcID
                + ";";
        
        // a query to insert if userSrcID does not exist:
        String query2 = "INSERT"
                + " INTO " + DATABASE + "." + METADATA_TABLE
                + " (" + SOURCE_ID_COLUMN + "," + SOURCE_TYPE_ID_COLUMN + ")"
                + " VALUES (" + userSrcID + "," + USER_SOURCE_TYPE_ID + ")"
                + ";";
        
        // a query to create a new grouping:
        // [TEMP] make group id = original source id
        String query3 = "INSERT"
                + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + ")"
                + " VALUES (" + baselineSrcID + "," + baselineSrcID + "," + BASELINE_RELATION_CODE + ")"
                + ";";
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing query 1:
                ResultSet resultSet = statement.executeQuery(query1);

                resultSet.next(); // go to first row

                // get number of records:
                int records = resultSet.getInt(1);

                if (records != 1)
                {
                    // perform query 2 to add userSrcID to srouces table:
                    int count = statement.executeUpdate(query2);
                    if (count != 1) throw new SQLException("User's src_id was not inserted successfully!");

                    // perform query 3 to create new grouping:
                    count = statement.executeUpdate(query3);
                    if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");

                }//if
            }//try
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
              
        return userSrcID;
    }//getUserSrdsSrcID
    
    // ------------------------------------------------------------------------
    
    /**
     * [23.2.2013] starts a baseline grouping if not already started
     * @param baselineSrcID
     * @return 
     */
    public void startBaselineTypeGrouping(int baselineSrcID)
    {
        // a query to check if a baseline trype grouping is already available:
        String query1 = "SELECT"
                + " count(*)"
                + " FROM " + DATABASE + "." + SOURCE_GROUPING_TABLE
                + " WHERE " + GROUP_ID_COLUMN + "=" + baselineSrcID
                + " AND " + SOURCE_ID_COLUMN + "=" + baselineSrcID
                + " AND " + GROUPING_RELATION_CODE_COLUMN + "=" + BASELINE_RELATION_CODE
                + ";";
        
        // a query to create a new baseline trype grouping:
        // make group id = original source id
        String query2 = "INSERT"
                + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + ")"
                + " VALUES (" + baselineSrcID + "," + baselineSrcID + "," + BASELINE_RELATION_CODE + ")"
                + ";";
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing query 1:
                ResultSet resultSet = statement.executeQuery(query1);

                resultSet.next(); // go to first row

                // get number of records:
                int records = resultSet.getInt(1);

                if (records < 1)
                {
                    // perform query 2 to create new grouping:
                    int count = statement.executeUpdate(query2);
                    if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");

                }//if
            }//try
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
    }//startBaselineTypeGrouping
    
    
    public String startDeletionTypeGrouping(int groupID, int userSrcID){
        String ts = this.startGrouping(groupID, userSrcID, DELETION_RELATION_CODE);
        System.out.println("deletion type grouping was created");
        return ts;
    }//startInsertionTypeGrouping
    
    public String startInsertionTypeGrouping(int groupID, int userSrcID){
        String ts = this.startGrouping(groupID, userSrcID, INSERTION_RELATION_CODE);
        System.out.println("insertion type grouping was created");
        return ts;
    }//startInsertionTypeGrouping
    
    // [27/9/2012] starts a new grouping of the given type:
    private String startGrouping(int groupID, int userSrcID, int groupingType)
    {
        // timestamp as a String
        String ts = "";
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // get current timestamp:
                ts = getCurrentTimestamp(statement);
                
                if (ts == null || ts.isEmpty()){
                    System.err.println("Timestamp was not retrieved!");
                    return null;
                }
                
                // create a grouping sql:
                String sql = "INSERT"
                    + " INTO " + DATABASE + "." + SOURCE_GROUPING_TABLE
                    + " (" + GROUP_ID_COLUMN + "," + SOURCE_ID_COLUMN + "," + GROUPING_RELATION_CODE_COLUMN + "," + GROUPING_EFFECTIVE_TIME_COLUMN + ")"
                    + " VALUES (" + groupID + "," + userSrcID + "," + groupingType + ",'" + ts + "')"
                    + ";";

                // run query:
                int count = statement.executeUpdate(sql);
                
                if (count != 1) throw new SQLException("Grouping row was not inserted successfully!");
                
            }//try
            
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
        
        // return timestamp of this grouping:
        return ts;
    }//startGrouping
    
    public void endLatestGrouping(int groupID, int userSrcID)
    {
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // [TEMP] wait for 1 sec since precision of timestamp is in seconds
                Thread.sleep(1000);

                // a query to end the latest grouping:
                String endGroupingSql = "UPDATE "
                        + DATABASE + "." + SOURCE_GROUPING_TABLE
                        + " SET " + GROUPING_END_TIME_COLUMN + "=" + CURRENT_TIMESTAMP
                        + " WHERE " + GROUP_ID_COLUMN + "=" + groupID
                        + " AND " + SOURCE_ID_COLUMN + "=" + userSrcID
                        + " AND " + GROUPING_END_TIME_COLUMN + " is NULL"
                        + ";";

                // run query:
                int count = statement.executeUpdate(endGroupingSql);
                if (count < 1) throw new SQLException("Grouping row was not updated successfully!");
                
            }//try
            
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
        catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        
    }//endLatestGrouping
    
    // ------------------------------------------------------------------------
    
    public void deleteHorizon(Set<GeoTag> geoTags, int baselineSrcID, int userSrcID, String timestamp)
    {
        this.insertHorizon(geoTags, baselineSrcID, userSrcID, timestamp, 0);
    }//deleteHorizon
    
    
    /**
     * Inserts horizons points of given geo-tags as exists in the DB
     * with user's id and time shift if required.
     * This can be used with time shift 0 to indicate deletion.
     * This method should be running by multiple threads.
     * @param geoTags
     * @param baselineSrcID
     * @param userSrcID
     * @param timestamp
     * @param timeShift 
     */
    public void insertHorizon(Set<GeoTag> geoTags, int baselineSrcID, int userSrcID, String timestamp, int timeShift)
    {
        try
        {
            // Creating a prepared statement for inserting horizon points:
            // -----------------------------------------------------------
            
            String psSql = 
                  "INSERT"
                + " INTO " + DATABASE + "." + FEATURE_TABLE 
                + " SELECT " + X_COLUMN + ","
                             + Y_COLUMN + ","
                             + "NULL,"
                             + "(" + TWO_WAY_TRAVEL_TIME_COLUMN + "+" + timeShift + "),"
                             + "'" + timestamp + "',"
                             + userSrcID + ","
                             + PROPERTY_ID_COLUMN  + ","
                             + PROPERTY_VALUE_COLUMN + ","
                             + "NULL,"
                             + "NULL"
                + " FROM " + DATABASE + "." + FEATURE_TABLE 
                + " WHERE " + X_COLUMN + "=?" // x
                  + " and " + Y_COLUMN + "=?" //y
                  + " and " + SOURCE_ID_COLUMN + "=" + baselineSrcID
                  + " and " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                  + " and " + PROPERTY_VALUE_COLUMN + "=?" // tag.sourceID (prop_val)
                + ";";

            PreparedStatement pStatement = con.prepareStatement(psSql);
            
            try
            {
                // insert each tag:
                for (GeoTag tag : geoTags){
                    
                    int i=1;
                    
                    // Set X:
                    pStatement.setInt(i++, tag.x);
                    
                    // Set Y:
                    pStatement.setInt(i++, tag.y);
                    
                    // Set prop_val:
                    pStatement.setInt(i++, tag.sourceID);
                    
                    
                    // add batch to statement:
                    pStatement.addBatch();
                    
                }//for
                
                // execute batch:
                int [] counts = pStatement.executeBatch();
                
            }//try

            finally
            {
                // Close the statement
                pStatement.close();
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch

    }//insertHorizon
    
    
    // [9/10/2012]
    /**
     * Delete whole session.
     * 
     * [TEMP] Note: this does not support to delete a baseline horizon.
     * 
     * @param baselineSrcID 
     * @param propVal
     * @param delTS timestamp of the session to be deleted
     * @param userSrcID user ID who id deleting this session
     * @param newTS the time at which this session is deleted
     */
    public void deleteSession(int baselineSrcID, int propVal, DataTimestamp delTS, int userSrcID, String newTS)
    {
        String sql =
                "INSERT INTO " + DATABASE + "." + FEATURE_TABLE
                
                + " SELECT " + X_COLUMN + ","
                            + Y_COLUMN + ","
                            + Z_COLUMN + ","
                            + TWO_WAY_TRAVEL_TIME_COLUMN + ","
                            + "'" + newTS + "' AS " + TIMESTAMP_COLUMN + ","
                            + userSrcID + " AS " + SOURCE_ID_COLUMN + ","
                            + PROPERTY_ID_COLUMN + ","
                            + PROPERTY_VALUE_COLUMN + ","
                            + "BC_INLN, BC_CROSSLN"
                ;
        
        if (delTS.isBaseline()){
            sql +=
                  " FROM " + DATABASE + "." + FEATURE_TABLE
                
                + " WHERE " + SOURCE_ID_COLUMN + "=" + baselineSrcID
                + " AND " + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                + " AND " + PROPERTY_VALUE_COLUMN + "=" + propVal
                ;
        }
        
        else{
            sql +=
                  " FROM " + DATABASE + "." + FEATURE_TABLE + " AS A"
                + " INNER JOIN " + DATABASE + "." + SOURCE_GROUPING_TABLE + " AS B"
                
                + " ON A." + SOURCE_ID_COLUMN + " = B." + SOURCE_ID_COLUMN
                + " AND A." + TIMESTAMP_COLUMN + "= B." + GROUPING_EFFECTIVE_TIME_COLUMN
                + " AND A." + PROPERTY_ID_COLUMN + "=" + HORIZON_PROPERTY_ID
                + " AND A." + PROPERTY_VALUE_COLUMN + "=" + propVal
                + " AND B." + GROUP_ID_COLUMN + "=" + baselineSrcID
                + " AND B." + GROUPING_RELATION_CODE_COLUMN + "=" + INSERTION_RELATION_CODE
                + " AND B." + GROUPING_EFFECTIVE_TIME_COLUMN + "='" + delTS.getTimestampAsString() + "'"
                ;
        }
        
        
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {// run query:
                int count = statement.executeUpdate(sql);
                
                if (count < 1) throw new SQLException("Session was not deleted successfully!");
                
            }//try
            
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch

    }//deleteSession
    
    // =======================================================================
    
    /**
     * [22.2.2013] validate a user source id,
     * if found returns user's name, or null otherwise
     * 
     * @param id user ID to be validated 
     * @return user's name or null if not found
     */
    public String validateUserSrcID(int userSrcID){
        String userName = null;
        
        // a query to get the user's name:
        String query1 = "SELECT "
                + SOURCE_DESCRIPTION_COLUMN
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_ID_COLUMN + "=" + userSrcID
                + " AND " + SOURCE_TYPE_ID_COLUMN + "=" + USER_SOURCE_TYPE_ID
                + ";";
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // Performing query 1:
                ResultSet resultSet = statement.executeQuery(query1);

                // get user's name if found:
                if (resultSet.next()){
                    userName = resultSet.getString(1);
                }
                
            }//try
            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
        return userName;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * [22.2.2013] generates a new source ID for the user
     * 
     * @param userName user's name
     * @return user's new source id 
     */
    public int generateUserSrcID(String userName){
        
        // ---------------------
        int idStartValue = 1000;
        // ---------------------
        
        int userSrcID = 0; // initial invalid value
        
        // a query to get the maximum user ID available, greater than the starting ID value:
        String query1 = "SELECT"
                + " MAXIMUM (" + SOURCE_ID_COLUMN + ")"
                + " FROM " + DATABASE + "." + METADATA_TABLE
                + " WHERE " + SOURCE_ID_COLUMN + ">" + idStartValue
                + " AND " + SOURCE_TYPE_ID_COLUMN + "=" + USER_SOURCE_TYPE_ID
                + ";";
        
        // a query to insert the generated userSrcID:
        String query2 = "INSERT"
                + " INTO " + DATABASE + "." + METADATA_TABLE
                + " (" + SOURCE_ID_COLUMN + "," + SOURCE_DESCRIPTION_COLUMN + ", " + SOURCE_TYPE_ID_COLUMN + ")"
                + " VALUES (?,?," + USER_SOURCE_TYPE_ID + ")"
                + ";";
        
        try
        {
            // Creating a statement object for query 1
            Statement statement1 = con.createStatement();
            
            // Creating a prepared statement for query2
            PreparedStatement pStatement2 = con.prepareStatement(query2);
            
            try
            {
                // Performing query 1:
                ResultSet resultSet = statement1.executeQuery(query1);

                int maxID;
                if (resultSet.next() && (maxID=resultSet.getInt(1)) > 0){
                    userSrcID = (maxID + 1);
                }
                else{
                    userSrcID = idStartValue + 1;
                }
                
                // perform query 2 to insert the new user info:
                pStatement2.setInt(1, userSrcID);
                pStatement2.setString(2, userName);
                pStatement2.executeUpdate();
                
            }//try
            finally
            {
                // Close the statements
                statement1.close();
                pStatement2.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Query failed.") ;

        }//catch
        
        return userSrcID;
    }//generateUserSrcID
    
    // =======================================================================
    
    // this method is copied from TeraData JDBC examples
    // for testing purpose only .. REMOVE LATER
    private static void displayRSMetaData(ResultSet rs) throws SQLException
    {
        // This code will demonstrate all available methods for
        // retrieving table column meta data. Please note that some
        // methods are meaningful only for numeric columns.

        // Retrieve result set meta data to get information on the columns
        ResultSetMetaData rsmd = rs.getMetaData();
        // Retrieve the number of columns returned
        int colCount = rsmd.getColumnCount();
        System.out.println("\n DISPLAYING RESULT SET COLUMN META DATA:");
        System.out.println(" -----------------------------------------");
        System.out.println("\n The resulting table has " + colCount + " columns:");
        // For every column, display it's meta data.

        int i = 1; // Initialize loop counter
        while (i <= colCount)
        {
            // Demonstrating all methods for retrieving column meta data
            System.out.println();
            System.out.println(" Column " + i);
            System.out.println(" ------------ ");
            // Display the suggested column title for use in
            // printouts and displays
            System.out.println(" Column label: "
                               + rsmd.getColumnLabel(i));
            // Display the column name
            System.out.println(" Column name: "
                               + rsmd.getColumnName(i));
            // Display the SQL type of a column.
            System.out.println(" Column type: "
                               + rsmd.getColumnType(i));
            // Display the type name of a column
            System.out.println(" Column type name: "
                               + rsmd.getColumnTypeName(i));
            // Display information on whether NULL values are allowed
            System.out.println(" NULLs allowed: "
                               + rsmd.isNullable(i));
            // Display the normal maximum width of a column in characters.
            System.out.println(" Maximum character width: "
                               + rsmd.getColumnDisplaySize(i));
            // Display precision: the number of decimal digits
            // Note: default value is 0.
            System.out.println(" Column precision"
                               + " (number of decimal places): "
                               + rsmd.getPrecision(i));
            // Display the number of digits to the right of the
            // decimal point. Note: default value is 0.
            System.out.println(" Precision to the right of"
                               + " the decimal point: "
                               + rsmd.getScale(i));
            // Increment column counter
            i++;
        }
    } // end displayRSMetaData(...)

    // ------------------------------------------------------------------------

    // measures execution time of a given query
    public long testQuery(String query)
    {
        long time;
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // Performing the query
                ResultSet resultSet = statement.executeQuery(query);
        
                long endTime = System.currentTimeMillis(); 
                
                time = endTime - startTime;
                
                System.out.println("Time: " + (time) + " ms.");
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        return time;
    }//testQuery
    
    // ------------------------------------------------------------------------

    // measures execution time of a given insert/update query
    public long testUpdate(String query)
    {
        long time;
        
        try
        {
            // Creating a statement object from an active connection
            Statement statement = con.createStatement();
            
            try
            {
                // [TEST] measure execution time:
                long startTime = System.currentTimeMillis();

                // Performing the query
                int count = statement.executeUpdate(query);
        
                long endTime = System.currentTimeMillis(); 
                
                time = endTime - startTime;
                
                System.out.println("Time: " + (time) + " ms.");
                System.out.println("count = " + count);
                
            }//try

            finally
            {
                // Close the statement
                statement.close();
                //System.out.println("Statement object closed. \n");
            }//finally

        }//try

        catch(SQLException ex)
        {
            // A SQLException was generated.  Catch it and display
            // the error information.
            // Note that there could be multiple error objects chained
            // together.
            System.out.println();
            System.out.println("*** SQLException caught ***");

            while (ex != null)
            {
                System.out.println(" Error code: " + ex.getErrorCode());
                System.out.println(" SQL State: " + ex.getSQLState());
                System.out.println(" Message: " + ex.getMessage());
                //ex.printStackTrace();
                System.out.println();
                ex = ex.getNextException();
            }

            throw new IllegalStateException ("Sample failed.") ;

        }//catch

        return time;
    }//testQuery
    
    
}
