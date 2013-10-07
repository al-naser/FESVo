/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seismic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import utilities.FESVoSystem;
import utilities.Point;

/**
 * This class stores seismic and features data in HashMap static objects
 *
 * @author Aqeel
 */
public class FESVo {
    
    // a Map holding seismic & features data: [NOT USED]
    private static final Map<GeoTag, GeospatialData> DATA = new HashMap<GeoTag, GeospatialData>();
    
    // map seismic & features data to their local location on level 0 (LOD0): [13/02/2012]
    private static final Map<PointTag, GeospatialData> LOD0 = new HashMap<PointTag, GeospatialData>();
    
    // a Map holding a source IDs with data description:
    private static final Map<Integer, DataType> SOURCE_IDs = new HashMap<Integer, DataType>();
    
    // a Map holding a source IDs with metadata:
    // this should be for data of type traces only:
    private static final Map<Integer, SeismicMetadata> METADATA = new HashMap<Integer, SeismicMetadata>();
    
    // a map of all data coordinates: [3/3/2012]
    // this map holds a source ID against a sorted map of X vallues,
    // the X map holds x values against a set of y values associated with this x
    private static final Map<Integer, TreeMap<Integer, TreeSet<Integer>>> DATA_COORDINATES = 
                 new HashMap<Integer, TreeMap<Integer, TreeSet<Integer>>>();
    
    // ------------------------------------------------------------------------
    
    // coordinate range of data:
    // updated on every insertion of GeospatialData:
    // or updated via setXYrange(...) [13/2/2012]
    private static int xMin, xMax, yMin, yMax;
    
    // LOD Unit:
    private static int lodUnitWidth;
    private static int lodUnitDepth;
    
    // 25% correction for computing LOD unit dimension:
    public static final float LOD_UNIT_CORRECTION = 0.25f; 
    
    // to help in assigning the above min and max values:
    private static boolean firstUpload = true;
    
    
    // ------------------------------------------------------------------------
    // ::::::::::::::::::::::::  putting data methods  ::::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // insert a trace into LOD0 map, mapped to its point tag in LOd0 [13/2/2012]
    public static synchronized void putTrace(Trace trace, PointTag tag, int lod)
    {
        LOD0.put(mapPointToLOD0(tag, lod), trace);
        
    }//putTrace
    
    // OLD METHOD
    // insert a trace into the map, mapped to its geoTag
    // and add its source id to the sources set:
    public static synchronized void putTrace(Trace trace)
    {
        DATA.put(trace.geoTag, trace);
        SOURCE_IDs.put(trace.geoTag.sourceID, DataType.TRACE);
        
        // update coord range
        updateCoordRange(trace.geoTag);
        
    }//putTrace
    
    // insert a horizon points of one PointTag into LOD0 map [13/2/2012]
    public static synchronized void putHorizon(Feature horizon, PointTag tag, int lod)
    {
        // map this tag to LOD0 (if lod is not 0):
        PointTag tag0 = mapPointToLOD0(tag, lod);
        
        // check if the data map already contains a horizon object with same tag
        Feature h = (Feature)LOD0.get(tag0);
        if (h != null)
        {
            // merge both horizons:
            h.merge(horizon);
        }
        
        else
        {
            // otherwise put horizon into map:
            LOD0.put(tag0, horizon);
        }
        
        
    }//putHorizon
    
    // OLD METHOD
    // insert a horizon points of one GeoTag into the map,
    // and add its source id to the sources set:
    public static synchronized void putHorizon(Feature horizon)
    {
        // check if the data map already contains a horizon object with same GeoTag
        Feature h = (Feature)DATA.get(horizon.geoTag);
        if (h != null)
        {
            // merge both horizons:
            h.merge(horizon);
        }
        
        else
        {
            // otherwise put horizon into map:
            DATA.put(horizon.geoTag, horizon);
            SOURCE_IDs.put(horizon.geoTag.sourceID, DataType.HORIZON);
            
            // update coord range
            updateCoordRange(horizon.geoTag);
        }
        
        
    }//putHorizon
    
    // OLD METHOD
    // insert a fault points of one GeoTag into the map,
    // and add its source id to the sources set:
    public static synchronized void putFault(Feature fault)
    {
        DATA.put(fault.geoTag, fault);
        SOURCE_IDs.put(fault.geoTag.sourceID, DataType.FAULT);
        
        // update coord range
        updateCoordRange(fault.geoTag);
    }//putFault
    
    
    // ------------------------------------------------------------------------
    // :::::::::::::::::::::::  data retrieval methods  :::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // OLD METHOD
    // return a geospatial data based on a given geo-tag:
    public static GeospatialData get(GeoTag tag)
    {
        return DATA.get(tag);
    }//get
    
    // return a geospatial data based on a given point-tag and LOD: [13/2/2012]
    public static GeospatialData get(PointTag tag, int lod)
    {
        return LOD0.get(mapPointToLOD0(tag, lod));
    }//get
    
    // ------------------------------------------------------------------------
    // ::::::::::::::::::::::: source IDs based methods :::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // Generate a free source ID giving a data type [10/2/2012]:
    public static int generateSourceID(DataType type)
    {
        // source ID starts at 0,
        // so the new ID equals to current number of available IDs
        int id = SOURCE_IDs.size();
        
        // add data type to SOURCE_IDs map:
        // if the same ID was previously mapped to a type, throw an exception
        // this should not happen if this method was always called before inserting data into FESVo
        try {
            if (SOURCE_IDs.put(id, type) != null){
                throw new Exception("Error in generating a new source ID - ID " + id + " was already created!");
            }//if
            
            // create an X map, and map it to the sourceID in DATA_COORDINATES: [3/3/2012]
            DATA_COORDINATES.put(id, new TreeMap<Integer, TreeSet<Integer>>());
        }//try
        catch (Exception ex) {
            //Logger.getLogger(FESVo.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex);
        }//catch
        
        // return the new ID:
        return id;
    }
    
    // ------------------------------------------------------------------------
    
    // return an array of source IDs:
    public static int [] getSourceIDs()
    {
        int [] result = new int[SOURCE_IDs.size()];
        
        // upload this array with ID values:
        int i=0;
        for (Integer id : SOURCE_IDs.keySet())
        {
            result [i++] = id.intValue();
        }
        
        return result;
    }//getSourceIDs
    
    // ------------------------------------------------------------------------
    
    // return DataType of a given source IDs:
    public static DataType getDataType(int sourceID)
    {
        return SOURCE_IDs.get(sourceID);
    }//getDataType
    
    // ------------------------------------------------------------------------
    
    // return an array of source IDs of a given DataType:
    public static int [] getSourceIDs(DataType type)
    {
        Set<Integer> tempSet = new HashSet<Integer>();
        
        // go through all keys:
        for (Integer id : SOURCE_IDs.keySet())
        {
            // check if this key is mapped to the required data type:
            if (SOURCE_IDs.get(id) == type)
                // add this key to tempSet:
                tempSet.add(id);
        }
        
        // convert tempSet to array of int:
        int [] result = new int[tempSet.size()];
        int i=0;
        for (Integer id : tempSet)
        {
            result [i++] = id.intValue();
        }
        
        return result;
    }//getSourceIDs
    
    // ------------------------------------------------------------------------
    // ::::::::::::::::::::::::  data removal methods  ::::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // [11/9/2012]
    
    /**
     * removes all data of a given source ID from LOD0 map
     * @param sourceID
     * @return number of data objects removed from FESVo
     */
    public static int clear(int sourceID)
    {
        int count=0;
        
        PointTag [] tags = LOD0.keySet().toArray(new PointTag[0]);
        
        for (PointTag tag : tags){
            if (tag.sourceID == sourceID){
                LOD0.remove(tag);
                count++;
            }
        }
        
        return count;
    }//clear
    
    
    /**
     * remove given point tags from LOD0
     * @param tags
     */
    public static void remove(PointTag [] tags)
    {
        for (PointTag tag : tags){
            LOD0.remove(tag);
        }//for
    }//remove
    
    // ------------------------------------------------------------------------
    // :::::::::::::::::::::::  data dimension methods  :::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // OLD METHOD
    // return size of data map (number of records):
    public static int dataSize(){
        return DATA.size();
    }
    
    // ------------------------------------------------------------------------

    public static int xMax() {
        return xMax;
    }

    public static int xMin() {
        return xMin;
    }

    public static int yMax() {
        return yMax;
    }

    public static int yMin() {
        return yMin;
    }
    
    // [30/4/2012] changed from public to private; update only when a metadata is added
    // set the range of x and y [13/2/2012]
    private static void setXYrange(int xMin, int xMax, int yMin, int yMax) {
        FESVo.xMin = xMin;
        FESVo.xMax = xMax;
        FESVo.yMin = yMin;
        FESVo.yMax = yMax;
        
        // LATER: update, don't just set; check existing values and update
        
    }//setXYrange

    // set width and depth of the LOD unit [13/2/2012]
    public static void setLodUnitWidthDepth(int lodUnitWidth, int lodUnitDepth) {
        FESVo.lodUnitWidth = lodUnitWidth;
        FESVo.lodUnitDepth = lodUnitDepth;
    }//setLodUnitWidthDepth

    public static int getLodUnitDepth() {
        return lodUnitDepth;
    }

    public static int getLodUnitWidth() {
        return lodUnitWidth;
    }
    
    
    
    // ------------------------------------------------------------------------
    // :::::::::::::::::::::  seismic metadata methods  :::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // add a SeismicMetadata object associated to a source IDs of type trace:
    public static boolean setMetadata(int sourceID, SeismicMetadata metadata)
    {
        if (FESVo.getDataType(sourceID) != DataType.TRACE)
            return false;
        
        FESVo.METADATA.put(sourceID, metadata);
        
        // [30/4/2012] update x,y:min,max:
        FESVo.setXYrange(metadata.xMin, metadata.xMax, metadata.yMin, metadata.yMax);
        
        return true; 
    }//setMetadata
    
    
    // [30/12/2012]
    // add a SeismicMetadata object associated to a newly generated source IDs of type trace,
    // return the generated source id:
    public static int setMetadata(SeismicMetadata metadata)
    {
        int id = FESVo.generateSourceID(DataType.TRACE);
        FESVo.setMetadata(id, metadata);
        return id;
    }//setMetadata
    
    // ------------------------------------------------------------------------
    
    // returns the SeismicMetadata object associated with a given source IDs:
    // returns null if no SeismicMetadata exists for the given sourceID:
    public static SeismicMetadata getMetadata(int sourceID)
    {
        return METADATA.get(sourceID);
    }//getMetadata
    
    
    // ------------------------------------------------------------------------
    // ::::::::::::::::::::::  Data Coordinates methods  ::::::::::::::::::::::
    // ------------------------------------------------------------------------
    
    // adds a geo-tag of an existing data to the DATA_COORDINATES map: [3/3/2012]
    // source id here is FESVo id; not DB id
    public static void addDataCoordinate(GeoTag tag){
        addDataCoordinate(tag.x, tag.y, tag.sourceID);
    }//addDataCoordinate
    
    public static void addDataCoordinate(int x, int y, int sourceID){
        // get the xMap associated with the sourceID:
        // an X map should be already there when the source was generated
        TreeMap<Integer, TreeSet<Integer>> xMap = DATA_COORDINATES.get(sourceID);
        
        // get the y set mapped to x:
        TreeSet<Integer> ySet = xMap.get(x);
        
        // if no y set exists for this x, create one and map it to x:
        if (ySet == null){
            ySet = new TreeSet<Integer>();
            xMap.put(x, ySet);
        }
        
        // add y value to ySet:
        ySet.add(y);
        
    }//addDataCoordinate
    
    // get a data coordinate which is located on an LOD unit which starts from the given point:
    public static Point getDataCoordinate (Point fromPoint, int sourceID)
    {
        return getDataCoordinate(
                fromPoint, 
                new Point((fromPoint.x + FESVo.lodUnitWidth), (fromPoint.y + FESVo.lodUnitDepth)), 
                sourceID);
    }//getDataCoordinate
    
    // [19/6/2012] get a data coordinate which is located on LOD units,
    // starting from the given point, 
    // and covering multi LOD units based on given LOD
    public static Point getDataCoordinate (Point fromPoint, int sourceID, int lod)
    {
        return getDataCoordinate(
                fromPoint, 
                new Point((fromPoint.x + (FESVo.lodUnitWidth << lod)), (fromPoint.y + (FESVo.lodUnitDepth << lod))), 
                sourceID);
    }//getDataCoordinate
    
    // get a data coordinate which is located between the given points:
    public static Point getDataCoordinate (Point fromPoint, Point toPoint, int sourceID)
    {
        Point point = null;
        
        // get the xMap associated with the sourceID:
        TreeMap<Integer, TreeSet<Integer>> xMap = DATA_COORDINATES.get(sourceID);
        
        // get a sub-map for x values lies between fromPoint and toPoint:
        SortedMap<Integer, TreeSet<Integer>> xSubMap = xMap.subMap(fromPoint.x, toPoint.x);
        
        // return null if no x is found:
        if (xSubMap.isEmpty()) return null;
        
        // loop through x values:
        for (int x : xSubMap.keySet()){
            // get y values mapped to this x:
            TreeSet<Integer> ySet = xSubMap.get(x);

            // get sub set of y values between fromPoint and toPoint:
            SortedSet<Integer> ySubSet = ySet.subSet(fromPoint.y, toPoint.y);

            // go to next x if no y is found:
            if (ySubSet.isEmpty()) continue;

            // get first y value:
            int y = ySubSet.first();

            // create a point to be returned, and exit loop:
            point = new Point(x, y);
            break;
        }//for
        
        return point;
        
    }//getDataCoordinate
    
    
    /**
     * Checks if the given coordinate has data for the given source id.
     * @param coordinate
     * @param sourceID
     * @return true if data of sourceID is available at the given coordinate
     * @since [19/6/2012]
     */
    public static boolean contains (Point coordinate, int sourceID)
    {
        // get the xMap associated with the sourceID:
        TreeMap<Integer, TreeSet<Integer>> xMap = DATA_COORDINATES.get(sourceID);
        
        // get y values mapped to x:
        TreeSet<Integer> ySet = xMap.get(coordinate.x);
        
        // return false if xMap does note contain coordinate's x:
        if (ySet == null)
            return false;
        
        // now check if ySet contains the coordinate's y:
        return ySet.contains(coordinate.y);
        
    }//contains
    
    // [8/9/2012]
    // [9/9/2012] this method is replaced by getAllPointTags()
    // [10/9/2012] THIS METHOD IS NO LONGER USED
    // returns all data coordinate of the given source id:
    public static Point [] getAllDataCoordinates (int sourceID)
    {
        ArrayList<Point> points = new ArrayList<Point>();
        
        // get the xMap associated with the sourceID:
        TreeMap<Integer, TreeSet<Integer>> xMap = DATA_COORDINATES.get(sourceID);
        
        // for each x, get all y values mapped to x:
        for (int x : xMap.keySet()){
            
            // get y values mapped to x:
            TreeSet<Integer> ySet = xMap.get(x);
        
            for (int y : ySet){
                // add point to the list:
                points.add(new Point(x,y));
            }
        }
        
        return points.toArray(new Point[0]);
        
    }//getAllDataCoordinates
    
    // ========================================================================
    
    // [9/9/2012]
    // [10/9/2012] THIS METHOD IS NO LONGER USED
    // returns all point tags of the given source id at a given resolution level:
    public static PointTag [] getAllPointTags (int sourceID)
    {
        SortedSet<PointTag> reqTags = new TreeSet<PointTag>();
        
        // go through all tags from LOD0:
        for (PointTag tag : LOD0.keySet()){
            // add tag to the required tags set if it matches the required source:
            if (tag.sourceID == sourceID){
                reqTags.add(tag);
            }
        }//for
        
        return reqTags.toArray(new PointTag[0]);
    }//getAllPointTags
    
    // ========================================================================
    // ========================================================================
    // ========================================================================
    
    public static PointTag mapPointToLOD0(PointTag tag, int lod)
    {
        // return same tag if LOD is 0:
        if (lod == 0)
            return tag;
        
        // otherwise, perform mapping of tag to LOD0:
        return new PointTag((tag.x << lod),
                            (tag.y << lod),
                            tag.sourceID);
        
    }//mapPointToLOD0
    
    // ------------------------------------------------------------------------
    
    // [15/6/2012] dealing with Point instead of PointTag
    public static Point mapPointToLOD0(Point p, int lod)
    {
        // return same point if LOD is 0:
        if (lod == 0)
            return p;
        
        // otherwise, perform mapping of p to LOD0:
        return new Point((p.x << lod),
                            (p.y << lod));
        
    }//mapPointToLOD0
    
    // ------------------------------------------------------------------------
    
    // [15/6/2012] maps local point to a geo-location:
    public static Point mapToGeoLocation(Point p, int lod)
    {
        // maps point to LOD 0:
        Point p0 = mapPointToLOD0(p, lod);
        
        return mapToGeoLocation(p0);
        
    }//mapToGeoLocation
    
    // ------------------------------------------------------------------------
    
    // [15/6/2012] maps local point at LOD 0 to a geo-location:
    public static Point mapToGeoLocation(Point p)
    {
        return new Point((p.x * FESVo.getLodUnitWidth()) + FESVo.xMin(),
                         (p.y * FESVo.getLodUnitDepth()) + FESVo.yMin());
        
    }//mapToGeoLocation
    
    // ------------------------------------------------------------------------
    
    // [25/9/2012] maps geo-location point to a local point at LOD 0:
    public static Point mapToLocalPoint(Point p)
    {
        return new Point((p.x - FESVo.xMin()) / FESVo.getLodUnitWidth(),
                         (p.y - FESVo.yMin()) / FESVo.getLodUnitDepth());
        
    }//mapToLocalPoint
    
    // ------------------------------------------------------------------------
    
    // maps a local point (xL, yL) from iLOD to fLOD
    public static int [] mapPoint(int xLi, int yLi, int iLOD, int fLOD)
    {
        // the mapped point:
        int xLf, yLf;
        
        // moving down to larger LOD
        if (iLOD > fLOD){
            xLf = xLi << (iLOD - fLOD);
            yLf = yLi << (iLOD - fLOD);
        }
        // moving up to smaller LOD
        else{
            xLf = xLi >> (fLOD - iLOD);
            yLf = yLi >> (fLOD - iLOD);
        }
        
        return new int[]{xLf, yLf};
    }
    
    // ------------------------------------------------------------------------
    
    // [10/7/2012] similar to the above method, but dealing with Point object
    // maps a local point (xL, yL) from iLOD to fLOD
    public static Point mapPoint(Point iP, int iLOD, int fLOD)
    {
        // the mapped point:
        int xLf, yLf;
        
        // moving down to larger LOD
        if (iLOD > fLOD){
            xLf = iP.x << (iLOD - fLOD);
            yLf = iP.y << (iLOD - fLOD);
        }
        // moving up to smaller LOD
        else{
            xLf = iP.x >> (fLOD - iLOD);
            yLf = iP.y >> (fLOD - iLOD);
        }
        
        return new Point(xLf, yLf);
    }
    
    // ------------------------------------------------------------------------
    
    private static void updateCoordRange(GeoTag tag)
    {
        // if this is first upload, make x and y min and max:
        if (firstUpload)
        {
            xMin = tag.x;
            xMax = tag.x;
            
            yMin = tag.y;
            yMax = tag.y;
            
            firstUpload = false;
            
            // exit this method:
            return;
        }
        
        
        // update X min and max:
        if (tag.x < xMin)
            xMin = tag.x;
        
        else if (tag.x > xMax)
            xMax = tag.x;
        
        // update Y min and max:
        if (tag.y < yMin)
            yMin = tag.y;
        
        else if (tag.y > yMax)
            yMax = tag.y;
                
    }//updateCoordRange
    
    // ------------------------------------------------------------------------
    
    // OLD METHOD
    // calculate distance frequency of x and y from FESVo
    public static void FESVoDistanceFrequency (int src)
    {
        // sorted sets for x and y values:
        TreeSet<Integer> setX = new TreeSet<Integer>();
        TreeSet<Integer> setY = new TreeSet<Integer>();
        
        for (GeoTag tag : DATA.keySet()){
            if (tag.sourceID == src){
                setX.add(tag.x);
                setY.add(tag.y);
            }//if
        }//for
        
        
        // Mapping distance to its frequency:
        Map<Integer, Integer> freqX = new HashMap<Integer, Integer>();
        Map<Integer, Integer> freqY = new HashMap<Integer, Integer>();
        
        // calculate distance frequency for X:
        Object [] x = setX.toArray();
        for (int i=0; i < x.length-1; i+=2){
            // calculate distance:
            int dis = ((Integer)x[i+1]).intValue() - ((Integer)x[i]).intValue();
            
            // update frequency:
            Integer freq = freqX.get(dis);
            freqX.put(dis, (freq == null? 1 : freq+1));
        }
        
        // calculate distance frequency for Y:
        Object [] y = setY.toArray();
        for (int i=0; i < y.length-1; i+=2){
            // calculate distance:
            int dis = ((Integer)y[i+1]).intValue() - ((Integer)y[i]).intValue();
            
            // update frequency:
            Integer freq = freqY.get(dis);
            freqY.put(dis, (freq == null? 1 : freq+1));
        }
        
        // print results to file:
        
        FESVoSystem.out.println("X Distance Frequency:");
        FESVoSystem.out.println("---------------------");
        for (Integer disX : freqX.keySet()){
            FESVoSystem.out.println(disX + ": " + freqX.get(disX));
        }//for
        
        FESVoSystem.out.println("\n");
        FESVoSystem.out.println("Y Distance Frequency:");
        FESVoSystem.out.println("---------------------");
        for (Integer disY : freqY.keySet()){
            FESVoSystem.out.println(disY + ": " + freqY.get(disY));
        }//for
        
    }
    
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        //test1();
        
        test2();
        
    }
    
    private static void test1() {
        Trace trace1 = new Trace (1, 2, 1);
        FESVo.putTrace(trace1);
        
        Trace trace2 = new Trace (1, 4, 1);
        FESVo.putTrace(trace2);
        
        Feature feature1 = new Feature (1, 2, 2);
        feature1.addW(10);
        FESVo.putHorizon(feature1);
        
        Feature feature2 = new Feature (1, 2, 3);
        FESVo.putHorizon(feature2);
        
        Feature feature3 = new Feature (1, 2, 2);
        feature3.addW(20);
        FESVo.putHorizon(feature3);
        
        for(GeoTag tag : FESVo.DATA.keySet())
        {
            System.out.println(tag);
        }
        
        for(Integer sourceID : FESVo.SOURCE_IDs.keySet())
        {
            System.out.println(sourceID);
        }
        
        System.out.println("w values in (1,2,2)");
        for(int w : ((Feature)(FESVo.DATA.get(new GeoTag(1,2,2)))).getAllWs())
        {
            System.out.println(w);
        }
        
        
        boolean bool1 = FESVo.DATA.containsKey(trace1.geoTag);
        GeoTag tag = new GeoTag(1, 2, 1);
        
        boolean bool3 = tag.equals(trace1.geoTag);
        
        boolean bool2 = FESVo.DATA.containsKey(tag);
        
        
        System.out.println("sourceIDs:");
        for (int i : FESVo.getSourceIDs())
        {
            System.out.println(i + ": " + FESVo.getDataType(i));
        }
        
        System.out.println("sourceIDs of type Trace:");
        for (int i : FESVo.getSourceIDs(DataType.TRACE))
        {
            System.out.println(i);
        }
        
        System.out.println("sourceIDs of type Horizon:");
        for (int i : FESVo.getSourceIDs(DataType.HORIZON))
        {
            System.out.println(i);
        }
        
        FESVo.setMetadata(1, new SeismicMetadata(999, 8, 4, 0, 1000));
        System.out.println("Seismic Meta-Data for ID 1: " + FESVo.getMetadata(1));
        System.out.println("Seismic Meta-Data for ID 2: " + FESVo.getMetadata(2));
    }
    
    private static void test2(){
        // [3/3/2012] test data coordinate:
        
        int src = FESVo.generateSourceID(DataType.TRACE);
        
        FESVo.addDataCoordinate(1, 1, src);
        FESVo.addDataCoordinate(1, 2, src);
        FESVo.addDataCoordinate(5, 2, src);
        FESVo.addDataCoordinate(1, 6, src);
        
        Point point1 = FESVo.getDataCoordinate(new Point(0,0), new Point(5,5), src);
        Point point2 = FESVo.getDataCoordinate(new Point(5,0), new Point(10,5), src);
        Point point3 = FESVo.getDataCoordinate(new Point(0,5), new Point(5,10), src);
        Point point4 = FESVo.getDataCoordinate(new Point(20,0), new Point(20,5), src);
        
        int z = 0;
    }
}
