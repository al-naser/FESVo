package utilities;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Aqeel
 * translated from a c# code written by Masroor Rasheed
 */

public class GIS {
    private static final double eccSquared = 0.00669438; // eccentricity (0.081819191 ^ 2) WGS84
    private static final double dEquatorialRadius = 6378137.0; // WGS84 (note above: varies from 6,356.750 km to 6,378.135 km)
    private static final double dScaleFactor = 0.9996; // scale factor, used as k0
    private static final double dDenominatorOfFlatteningRatio = 298.257223563;


    /// <summary>
    /// convert utm to lat/lon
    /// . converts UTM coords to lat/long.  Equations from USGS Bulletin 1532.
    ///   East Longitudes are positive, West longitudes are negative.
    ///   North latitudes are positive, South latitudes are negative.
    /// . Resultant Lat and Lon are in decimal degrees.
    /// . Original cpp code I translated this from was written by Chuck Gantz- chuck.gantz@globalstar.com
    /// </summary>
    public static LatLong UTM2LatLon(double UTMNorthing,
                              double UTMEasting,
                              String sUTMZone  // expected format "12N"
                              )
    {
        // if deviation from WGS84 is desired, do this (after loading the array, duh):
        //  cEllipsoid[] ellipsoidRg = EllipsoidLoad();
        //  dEquatorialRadius = ellipsoid[index-of-desired-reference-ellipsoid].EquatorialRadius;
        //  eccSquared = ellipsoid[index-of-desired-reference-ellipsoid].eccentricitySquared;

        LatLong point = new LatLong();

        // populate North/South
        char cZoneLetter = sUTMZone.charAt(sUTMZone.length()-1);
        boolean bNorthernHemisphere = (cZoneLetter >= 'N');

        String sZoneNum = sUTMZone.substring(0, (sUTMZone.length()-1));
        int iZoneNumber = Integer.parseInt(sZoneNum);

        double x = UTMEasting - 500000.0; //remove 500,000 meter offset for longitude
        double y = UTMNorthing;
        if (!bNorthernHemisphere) // point is in southern hemisphere
            y -= 10000000.0; // remove 10,000,000 meter offset used for southern hemisphere

        double dLongOrigin = (iZoneNumber - 1) * 6 - 180 + 3; // +3 puts origin in middle of zone

        double eccPrimeSquared = (eccSquared) / (1 - eccSquared);

        double M = y / dScaleFactor;
        double mu = M / (dEquatorialRadius * (1 - eccSquared / 4 - 3 * eccSquared * eccSquared / 64 - 5 * eccSquared * eccSquared * eccSquared / 256));

        double e1 = (1 - Math.sqrt(1 - eccSquared)) / (1 + Math.sqrt(1 - eccSquared));
        // phi in radians
        double phi1Rad = mu + (3 * e1 / 2 - 27 * e1 * e1 * e1 / 32) * Math.sin(2 * mu)
              + (21 * e1 * e1 / 16 - 55 * e1 * e1 * e1 * e1 / 32) * Math.sin(4 * mu)
              + (151 * e1 * e1 * e1 / 96) * Math.sin(6 * mu);
        // convert to degrees
        double dCvtRad2Deg = 180 / 3.14159265358979323846264338327950288;
        double phi1 = phi1Rad * dCvtRad2Deg;


        double N1 = dEquatorialRadius / Math.sqrt(1 - eccSquared * Math.sin(phi1Rad) * Math.sin(phi1Rad));
        double T1 = Math.tan(phi1Rad) * Math.tan(phi1Rad);
        double C1 = eccPrimeSquared * Math.cos(phi1Rad) * Math.cos(phi1Rad);
        double R1 = dEquatorialRadius * (1 - eccSquared) / Math.pow(1 - eccSquared * Math.sin(phi1Rad) * Math.sin(phi1Rad), 1.5);
        double D = x / (N1 * dScaleFactor);

        // phi in radians
        point.latitude = phi1Rad - (N1 * Math.tan(phi1Rad) / R1) * (D * D / 2 - (5 + 3 * T1 + 10 * C1 - 4 * C1 * C1 - 9 * eccPrimeSquared) * D * D * D * D / 24
                + (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 - 252 * eccPrimeSquared - 3 * C1 * C1) * D * D * D * D * D * D / 720);
        // convert to degrees
        point.latitude = point.latitude * dCvtRad2Deg;

        // lon in radians
        point.longitude = (D - (1 + 2 * T1 + C1) * D * D * D / 6 + (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 + 8 * eccPrimeSquared + 24 * T1 * T1)
                * D * D * D * D * D / 120) / Math.cos(phi1Rad);
        // convert to degrees
        point.longitude = dLongOrigin + point.longitude * dCvtRad2Deg;

        
        // [TEST] print:
        System.out.println();
        System.out.println("\tUTM Location (x, y, zone): (" + UTMEasting + ", " + UTMNorthing + ", " + sUTMZone + ")");
        System.out.println("\tconverted to (lat, long): " + point.toString());
        System.out.println();
        
        // return point:
        return (point);
    }
}
