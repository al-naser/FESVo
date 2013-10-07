package utilities;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Masroor Rasheed
 * modified by Aqeel Al-Naser
 */
public class LatLong {
    
    public double latitude, longitude;

    public LatLong() {
    }

    
    public LatLong(double latitude, double longitude)
    {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "(" + latitude + ", " + longitude + ")";
    }
    
}
