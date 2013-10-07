/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

/**
 *
 * @author Aqeel
 */
public abstract class GeospatialData { // implements Comparable {
    
    public final GeoTag geoTag;
    
    // ------------------------------------------------------------------------
    
    public GeospatialData(GeoTag tag) {
        this.geoTag = tag;
    }//constructor
    
    public GeospatialData(int x, int y, int sourceID) {
        this.geoTag = new GeoTag(x, y, sourceID);
    }//constructor

    
    
    
}
