/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.util.ArrayList;
import utilities.Point;
import utilities.SEGY;

/**
 * This class stores meta-data of seismic data of one source as follows:
 * 1. number of samples per trace
 * 2. samples data format
 * 3. sample size (number of bytes)
 * 4. sample interval (in milliseconds)
 * 5. minimum time
 * 6. maximum time
 * 
 * 
 * @author Aqeel
 */
public class SeismicMetadata {
    
    // metadata variables:
    public final int samplesPerTrace;
    public final int sampleDataFormat;
    public final int sampleSize; 
    public final int sampleInterval;
    public final int timeMin;
    public final int timeMax;
    
    // new metadata variables [26/4/2012]
    public final int srdsSrcID;
    public final String description;
    public final int firstInline, lastInline, firstXline, lastXline;
    public final int xMin, xMax, yMin, yMax;
    public final float amplitudeMin, amplitudeMax;
    public final Point pointA, pointB, pointC, pointD;
    public final String utmZone;
    
    // [23/10/2012]
    private ArrayList<Integer> horizonsPropVal = null;
    private ArrayList<Integer> faultsPropVal = null;

    // ========================================================================
    
    public SeismicMetadata(int samplesPerTrace, int sampleDataFormat, int sampleInterval, int timeMin, int timeMax) {
        this.samplesPerTrace = samplesPerTrace;
        this.sampleDataFormat = sampleDataFormat;
        this.sampleInterval = sampleInterval;
        this.timeMin = timeMin;
        this.timeMax = timeMax;
        
        // detect sample size:
        switch (sampleDataFormat){
            
            case SEGY.SEGY_4BYTE_IBM_FLOATING_POINT_FORMAT:
            case SEGY.SEGY_4BYTE_INTEGER_FORMAT:
            case SEGY.SEGY_4BYTE_IEEE_FLOATING_POINT_FORMAT:
                sampleSize = 4;
                break;
                
            case SEGY.SEGY_2BYTE_INTEGER_FORMAT:
                sampleSize = 2;
                break;
                
            default:
                sampleSize = 0;
                
        }//switch
        
        // [26/4/2012] set all newly added variables to invalid values in this old constructor
        this.srdsSrcID = -1; // indicating an invalid source id in SRDS
        this.description = "";
        this.firstInline = 0;
        this.lastInline = 0;
        this.firstXline = 0;
        this.lastXline = 0;
        this.xMin = 0;
        this.xMax = 0;
        this.yMin = 0;
        this.yMax = 0;
        this.amplitudeMin = 0;
        this.amplitudeMax = 0;
        this.pointA = new Point(0,0);
        this.pointB = new Point(0,0);
        this.pointC = new Point(0,0);
        this.pointD = new Point(0,0);
        this.utmZone = "";
    }//constructor
    
    // ------------------------------------------------------------------------
    
    // [26/4/2012] new constructor which covers the new variables:
    public SeismicMetadata(
            int srdsSrcID, 
            String description, 
            int firstInline, int lastInline, 
            int firstXline, int lastXline,
            int xMin, int xMax, 
            int yMin, int yMax, 
            int timeMin, int timeMax, 
            float amplitudeMin, float amplitudeMax, 
            int sampleDataFormat, 
            int sampleInterval, 
            int samplesPerTrace, 
            Point pointA, Point pointB, 
            Point pointC, Point pointD, 
            String utmZone) {
        
        
        this.srdsSrcID = srdsSrcID;
        this.description = description;
        this.firstInline = firstInline;
        this.lastInline = lastInline;
        this.firstXline = firstXline;
        this.lastXline = lastXline;
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
        this.timeMin = timeMin;
        this.timeMax = timeMax;
        this.amplitudeMin = amplitudeMin;
        this.amplitudeMax = amplitudeMax;
        this.sampleDataFormat = sampleDataFormat;
        this.sampleInterval = sampleInterval;
        this.samplesPerTrace = samplesPerTrace;
        this.pointA = pointA;
        this.pointB = pointB;
        this.pointC = pointC;
        this.pointD = pointD;
        this.utmZone = utmZone;
        
        // detect sample size:
        switch (sampleDataFormat){
            
            case SEGY.SEGY_4BYTE_IBM_FLOATING_POINT_FORMAT:
            case SEGY.SEGY_4BYTE_INTEGER_FORMAT:
            case SEGY.SEGY_4BYTE_IEEE_FLOATING_POINT_FORMAT:
                this.sampleSize = 4;
                break;
                
            case SEGY.SEGY_2BYTE_INTEGER_FORMAT:
                this.sampleSize = 2;
                break;
                
            default:
                this.sampleSize = 0;
                
        }//switch
        
    }//constructor
    
    
    // ========================================================================

//    @Override
//    public String toString() {
//        return "SeismicMetaData{" + "samplesPerTrace=" + samplesPerTrace + ", sampleDataFormat=" + sampleDataFormat + ", sampleSize=" + sampleSize + ", sampleInterval=" + sampleInterval + ", timeMin=" + timeMin + ", timeMax=" + timeMax + '}';
//    }

    // ------------------------------------------------------------------------
    
    // [26/4/2012] toString in the order of SRDS.SRC table
    public String getInfo() {
        return "SeismicMetadata{" 
                + "\n\tsrdsSrcID=" + srdsSrcID 
                + ",\n\tdescription=" + description 
                + ",\n\tfirstInline=" + firstInline + ", lastInline=" + lastInline
                + ",\n\tfirstXline=" + firstXline + ", lastXline=" + lastXline
                + ",\n\txMin=" + xMin + ", xMax=" + xMax 
                + ",\n\tyMin=" + yMin + ", yMax=" + yMax 
                + ",\n\ttimeMin=" + timeMin + ", timeMax=" + timeMax 
                + ",\n\tamplitudeMin=" + amplitudeMin + ", amplitudeMax=" + amplitudeMax 
                + ",\n\tsampleDataFormat=" + sampleDataFormat 
                + ",\n\tsampleSize=" + sampleSize 
                + ",\n\tsampleInterval=" + sampleInterval 
                + ",\n\tsamplesPerTrace=" + samplesPerTrace 
                + ",\n\tpointA=" + pointA 
                + ",\n\tpointB=" + pointB 
                + ",\n\tpointC=" + pointC
                + ",\n\tpointD=" + pointD 
                + ",\n\tutmZone=" + utmZone 
                + "\n}";
    }//toString()
    
    // ------------------------------------------------------------------------
    
    // [26/4/2012] toString in the order of SRDS.SRC table
    @Override
    public String toString() {
        return "[" + srdsSrcID + "] " + description ;
    }//toString()
        
    // ------------------------------------------------------------------------
    
    // [23/12/2012]
    
    public void setHorizonsPropVal(ArrayList<Integer> propVal){
        if (this.horizonsPropVal == null)
            this.horizonsPropVal = propVal;
    }
    public void setFaultsPropVal(ArrayList<Integer> propVal){
        if (this.faultsPropVal == null)
            this.faultsPropVal = propVal;
    }

    public ArrayList<Integer> getHorizonsPropVal() {
        return horizonsPropVal;
    }

    public ArrayList<Integer> getFaultsPropVal() {
        return faultsPropVal;
    }
    
    
        
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        SeismicMetadata metaData = new SeismicMetadata (999, 8, 4, 0, 1000);
        
        System.out.println("Time min: " + metaData.timeMin);
        
        System.out.println(metaData);
    }
    
}
