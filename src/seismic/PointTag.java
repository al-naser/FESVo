/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.util.Arrays;
import utilities.Point;

/**
 * A local point tag, containing point location (x, y) and data source ID
 * 
 * created on [13/2/2012]
 *
 * @author Aqeel
 */
public class PointTag implements Comparable {
    public final int x, y, sourceID;

    public PointTag(int x, int y, int sourceID) {
        this.x = x;
        this.y = y;
        this.sourceID = sourceID;
    }
    
    public PointTag(Point p, int sourceID) {
        this(p.x, p.y, sourceID);
    }

    // ------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "PointTag{" + "x=" + x + ", y=" + y + ", sourceID=" + sourceID + '}';
    }
    
    // ------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o) {
        
        if (o instanceof PointTag)
        {
            PointTag tag = (PointTag)o;
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
    
    // [31/5/2012] return a Point object of this tag:
    // note: ideally, this class should store a Point object, not x and y
    //       but it was created before the creation of class Point
    public Point getPoint(){
        return new Point(this.x, this.y);
    }
    
    // ------------------------------------------------------------------------
    
    // [9/9/2012]
    @Override
    public int compareTo(Object t) {
        //throw new UnsupportedOperationException("Not supported yet.");
        if (this.x == ((PointTag)t).x)
            return (this.y - ((PointTag)t).y);
        
        else
            return (this.x - ((PointTag)t).x);
    }
    
}
