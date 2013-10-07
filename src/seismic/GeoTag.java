/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import utilities.Point;

/**
 *
 * @author Aqeel
 */
public class GeoTag {
    
    public final int x, y, sourceID;

    // added on [4/3/2012]
    public GeoTag(Point p, int sourceID) {
        this(p.x, p.y, sourceID);
    }
    
    public GeoTag(int x, int y, int sourceID) {
        this.x = x;
        this.y = y;
        this.sourceID = sourceID;
    }

    // ------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "GeoTag{" + "x=" + x + ", y=" + y + ", sourceID=" + sourceID + '}';
    }
    
    // ------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o) {
        
        if (o instanceof GeoTag)
        {
            GeoTag tag = (GeoTag)o;
            if (       tag.x == this.x 
                    && tag.y == this.y 
                    && tag.sourceID == this.sourceID)
                return true;
            
            return false;
        }
        
        return super.equals(o);
    }
    
    // ------------------------------------------------------------------------
    
    // override hashCode method so that HashSet and HashMap checks for duplications:
    // source of this idea: http://stackoverflow.com/questions/5186081/java-hashset-duplicates-comparison
    @Override
    public int hashCode() {
        return Arrays.hashCode(new int[] {this.x, this.y, this.sourceID});
    }//hashCode
    
    // ------------------------------------------------------------------------

    // [25/9/2012] 
    public Point getPoint()
    {
        return new Point(this.x, this.y);
    }
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        GeoTag tag1 = new GeoTag(2, 2, 1);
        int int1 = tag1.hashCode();
        
        GeoTag tag2 = new GeoTag(2, 2, 1);
        int int2 = tag2.hashCode();
        
        boolean bool = tag1.equals(tag2);
        
        Set<GeoTag> tagSet = new HashSet<GeoTag>();
        
        tagSet.add(tag1);
        tagSet.add(tag2);
        
        for (GeoTag tag : tagSet)
        {
            System.out.println(tag);
        }
        
        int i = 0;
    }
}
