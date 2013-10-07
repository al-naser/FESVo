/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package seismic;

/**
 *
 * @author Aqeel
 */
public class Trace extends GeospatialData{
    
    private byte [] samples; // float for now
    
    // ------------------------------------------------------------------------
    
    public Trace(GeoTag tag) {
        super(tag);
    }//constructor
    
    public Trace(int x, int y, int sourceID) {
        super(x, y, sourceID);
    }//constructor
    
    // ------------------------------------------------------------------------
    
    public void setSamples(byte [] iSamples){
        samples = iSamples;
    }
    
    public byte[] getSamples() {
        return samples;
    }

    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Trace{" + geoTag + '}';
    }
    
    

    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        GeospatialData trace1 = new Trace (1, 2, 10);
        
        System.out.println(trace1);
    }//main
    
}
