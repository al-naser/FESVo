/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 * @author Aqeel
 */
public class Feature extends GeospatialData{
    
    // a set holding all two-way travel time values within the GeoTag
    // where this feature takes placed
    private TreeSet<Integer> w ; 
        
    // ------------------------------------------------------------------------
    
    public Feature(GeoTag tag) {
        super(tag);
        w = new TreeSet<Integer>();
    }//constructor
    
    public Feature(int x, int y, int sourceID) {
        super(x, y, sourceID);
        w = new TreeSet<Integer>();
    }//constructor
    
    // ------------------------------------------------------------------------

    public boolean addW(int w) {
        return this.w.add(w);
    }//addW
    
    public boolean removeW(int w) {
        return this.w.remove(w);
    }//removeW

    public int [] getAllWs() {
                
        int [] result = new int[w.size()];
        
        int i = 0;
        
        for (Integer intObj : w.descendingSet())
        {
            result[i++] = intObj.intValue();
        }
        
        // return result:
        return result;
        
    } //getAllWs
    
    // ------------------------------------------------------------------------
    
    // merge w values of a given Feature into this Feature:
    public boolean merge(Feature other)
    {
        // don't merge if other's tag doesn't equal this feature's tag:
        if (!other.geoTag.equals(this.geoTag))
            return false;
        
        // merge w values:
        return this.w.addAll(other.w); 
    }//merge
    
    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Feature{" + this.geoTag + '}';
    }
    
    
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        Feature feature1 = new Feature (1, 2, 10);
        
        feature1.addW(100);
        feature1.addW(50);
        feature1.addW(150);
        feature1.addW(60);
        feature1.addW(3);
        feature1.addW(44);
        //feature1.removeW(100);
        
        System.out.println(feature1);
        
        int [] wSet = feature1.getAllWs();
        
        for (int i : wSet)
        {
            System.out.println(i);
        }
    }//main
}
