/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.util.Arrays;

/**
 * This is a general point (x,y): geo-location, local point, or texture point
 * No tagging, no source id
 *
 * @author Aqeel
 */
public class Point  implements Comparable {
    public final int x, y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // ------------------------------------------------------------------------

    @Override
    public String toString() {
        return "Point{" + "x=" + x + ", y=" + y + '}';
    }
    
    // ------------------------------------------------------------------------
    
    @Override
    public boolean equals(Object o) {
        
        if (o instanceof Point)
        {
            Point point = (Point)o;
            if (   point.x == this.x 
                && point.y == this.y)
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
        return Arrays.hashCode(new int[] {this.x, this.y});
    }//hashCode
    
    // ------------------------------------------------------------------------
    
    // [30/4/2012] calculate distance between this point and a given point:
    public double distanceTo(Point to)
    {
        return Point.distanceBetween(this, to);
    }//distanceBetween
    
    // [30/4/2012] calculates distance between two given points:
    public static double distanceBetween(Point A, Point B)
    {
        return Math.sqrt(Math.pow((A.x - B.x), 2) + Math.pow((A.y - B.y), 2));
    }//distanceBetween

    @Override
    public int compareTo(Object t) {
        if (this.x == ((Point)t).x)
            return (this.y - ((Point)t).y);
        
        else
            return (this.x - ((Point)t).x);
    }
}
