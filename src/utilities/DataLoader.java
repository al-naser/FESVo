/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import com.jogamp.opengl.util.GLBuffers;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import seismic.*;

/**
 * created on [10/2/2012]
 * used by Main class to load data from FESVo to Main if data available
 * or load new data from database to FESVo then Main
 *
 * @author Aqeel
 */
public class DataLoader{
    
    /**
     * [8.2.2013] using class PropertyReader to get configurations from a file
     */
    
    // folder name for data cache:
    private static final String DATA_CACHE_FOLDER = PropertyReader.getProperty("DATA_CACHE_FOLDER");
    
    // connections array to be used by threads:
    // [Later: consider using connection pool]
    private final int numOfThreads;
    private DatabaseConnector[] dbConnection;
    
    // array lists as queues for threads to retrieve points form DB:
    private ArrayList<PointTag>[] pointsQueue;
    
    // [31/5/2012] this should replace the above queue:
    // array of queues, 
    // a queue element maps a point to a set of required source IDs
    private Map<Point, Set<Integer>>[] pointsQueue_new;
    
    // length of multi-statement [25/2/2012]
    private final int multiStatementLength;
    
    // ------------------------------------------------------------------------
    
    // consider only one dataset (one seismic and several related horizons)
    // per loader [might change in future]:
    
    // FESVo source ID for traces:
    private final int tracesSourceID;
    
    // FESVo source IDs for horizons mapped to SRDS prop_val:
    private final Map<Integer, Integer> horizonsSourceIDs = new HashMap<Integer, Integer>();
    
    // [28/10/2012] FESVo source IDs for faults mapped to SRDS prop_val:
    private final Map<Integer, Integer> faultsSourceIDs = new HashMap<Integer, Integer>();
    
    // ------------------------------------------------------------------------
    
    // maximum size of the texture buffer:
    private long maxTextureSize;
    
    // maximum texture dimention: {width, depth, height}
    private int [] maxTextureDimension = new int[3];
    
    // current (active) texture dimention: {width, depth, height}
    private int [] textureDimension = new int[3];
    
    // [8/5/2012] dimension of LOD 0:
    private int [] lod0Dimension;
    
    // top level of details (LOD) which can fully fit in max. texture, 
    // depending on max. texture dimension:
    private int topLOD;
    
    // texture buffer:
    private ByteBuffer traceTexBuffer, featuresTexBuffer;
    
    // a boolean value to control the loading of data at a lower resolution
    // than buffer's dimension, so that a higher resoltion data later be loaded:
    private boolean loadAtLowerResolution = false;
    
    // [28/5/2012] a boolean value to indicate loading features texture (not traces texture).
    // As the loading process is so much similar, this is helps in code reusability.
    private boolean loadingFeatures = false;
    
    // [31/5/2012] a set of the required IDs for the current loading process:
    private Set<Integer> requiredIDs;
    
    // [22/2/2012] initial LOD, and final LOD
    // an initial (lower resolution) level would be loaded first 
    // into a buffer which fits the desired final level (higher resolution),
    // since an enhancement will be performed on the buffer by loading 
    // the final level slice by slice
    // if a lower resolution version (and thus a later enhancement) is not required,
    // use initialLOD as the current LOD
    private int initialLOD, finalLOD;
    
    // local reference point on the current loaded data mapped to finalLOD:
    // these values must only be set by the build texture methods :
    private int xLRefFinalLOD, yLRefFinalLOD;
    
    // number of slices to be enhanced in one go:
    private int slicesToEnhance = 1;
    
    // ------------------------------------------------------------------------
    
    // Maps source IDs (of features) to an array list holding 
    // its min height (index 0) and max heights (index 1) in texture:
    private Map<Integer, ArrayList<Integer>> featureHeight;
    
    // ------------------------------------------------------------------------
    
    // [27/4/2012]
    // an array holding metadata from SRDS:
    private static SeismicMetadata[] SRDS_SRC_METADATA = null;
    
    // [28/4/2012]
    // the selected source:
    private final SeismicMetadata srdsSource;
    
    // ------------------------------------------------------------------------
    
    // [28/4/2012]
    // the default SRDS src_id to maintain old constructors:
    private static final int DEFAULT_SRDS_SRC_ID = 3;
    
    // ------------------------------------------------------------------------
    
    // [TEST] counters for FESVo visits and DB visits during texture buffer building:
    private int FESVoPointsCount = 0;
    private int dbVisitCount = 0; // FESVoVisitCount + dbVisitCount = total lod units
    private int emptyUnitsCount = 0; // empty units in DB
    
    // ------------------------------------------------------------------------
    
    // [25/9/2012] variables to support pre-load features points mode:
    private final boolean preLoadFeatures;
    private boolean featuresLoadedIntoFESVo = false;
    
    // [30/9/2012]
    // [10/10/2012] changed from String to DataTimestamp
    private DataTimestamp featuresTimestamp = null;
    
    // [8/10/2012]
    // if true: load up to the selected newTimestamp
    // if false: load only the selected newTimestamp
    private boolean loadOnlySelectedTimestamp = false;
    
    // [9/10/2012] if true: only load these IDs
    private boolean selectedFeaturesIDsToLoad = false;
    
    
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    
    // [27/4/2012] reads SRC table in SRDS,
    // stores a copy of all src metadata in a static object
    // to be available for all data loader objects:
    public static SeismicMetadata[] readSrdsSrcMetadata()
    {
        if (SRDS_SRC_METADATA == null)
        {
            // create a connection:
            DatabaseConnector db = new DatabaseConnector();
            
            // get SRDS sources metadata:
            SRDS_SRC_METADATA = db.getSourcesMetadata(); 
            
            // disconnect [22.2.2013]
            db.disconnect();
        }//if
        
        return SRDS_SRC_METADATA;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * [22.2.2013] validate a user id, if found returns user's name, or null otherwise
     * 
     * @param id user ID to be validated 
     * @return user's name or null if not found
     */
    public static String validateUserID(int id)
    {
        // create a connection:
        DatabaseConnector db = new DatabaseConnector();
        
        // Validate User ID, and get user's name if found:
        String userName = db.validateUserSrcID(id);
        
        // disconnect
        db.disconnect();
        
        return userName;
        
    }//validateUserID
    
    // ------------------------------------------------------------------------
    
    /**
     * [22.2.2013] generates a new source ID for the user
     * @param name user's name
     * @return user's new source id 
     */
    public static int generateUserID(String name)
    {
        // create a connection:
        DatabaseConnector db = new DatabaseConnector();
        
        // Generate a new user ID passing the name of the user to the DB:
        int userID = db.generateUserSrcID(name);
        
        // disconnect
        db.disconnect();
        
        return userID;
        
    }
    
    // ::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
    
    // constructor with default texture size:
    public DataLoader() {
        // set default max texture size:
        this(1000000 * 60); // 60MB
    }
    
    // ------------------------------------------------------------------------
    
    // constructor specifying max. texture size:
    public DataLoader(long textureSize){
        // set default number of threads:
        this(textureSize, 4); // 4 threads
    }
    
    // ------------------------------------------------------------------------
    
    // constructor specifying max. texture size and number of threads:
    public DataLoader(long textureSize, int threads) {
        // set default multiStatementLength value:
        this(textureSize, threads, 1); // length 1 as a default value
    }
    
    // ------------------------------------------------------------------------
    
    // constructor specifying max. texture size, number of threads, and multiStatementLength:
    // modified on [28/4/2012]: this constructor is replaced by the next one
    // which takes an SRDS source selected by user.
    // This constructor is maintained for some testing methods and classes. 
    public DataLoader(long textureSize, int threads, int multiStatementLength){
        // select the default SRDS src_id indicated above:
        this(readSrdsSrcMetadata()[DEFAULT_SRDS_SRC_ID-1], textureSize, threads, multiStatementLength);
    }
    
    // ------------------------------------------------------------------------
    
    // [28/4/2012] the source here is selected by the user and passed to this constructor
    // constructor specifying an SRDS source, max. texture size, number of threads, and multiStatementLength:
    public DataLoader(SeismicMetadata aSource, long textureSize, int threads, int multiStatementLength) {
        this(aSource, textureSize, threads, multiStatementLength, false);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * [4/5/2012] the source here is selected by the user and passed to this constructor
     * constructor specifying an SRDS source, max. texture size, number of threads, 
     * multiStatementLength, and whether to start with volume or slice based visualization.
     * 
     * @param aSource
     * @param textureSize
     * @param threads
     * @param multiStatementLength
     * @param sliceVis 
     */
//    public DataLoader(SeismicMetadata aSource, long textureSize, int threads, int multiStatementLength, boolean sliceVis) {
    
    // ------------------------------------------------------------------------
    
    /**
     * [26/9/2012] the source here is selected by the user and passed to this constructor
     * constructor specifying an SRDS source, max. texture size, number of threads, 
     * multiStatementLength, and pre-load features mode.
     * 
     * @param aSource
     * @param textureSize
     * @param threads
     * @param multiStatementLength
     * @param preLoadFeaturesMode 
     */
    public DataLoader(SeismicMetadata aSource, long textureSize, int threads, int multiStatementLength, boolean preLoadFeaturesMode) {
        
        // select some default horizons, and no faults
        this(aSource, textureSize, threads, multiStatementLength, preLoadFeaturesMode, new int[]{1,2}, new int[]{}); 
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * [23/10/2012] the source here is selected by the user and passed to this constructor
     * constructor specifying an SRDS source, max. texture size, number of threads, 
     * multiStatementLength, pre-load features mode, and array of selected horizons and faults
     * 
     * @param aSource
     * @param textureSize
     * @param threads
     * @param multiStatementLength
     * @param preLoadFeaturesMode
     * @param selectedHorizons
     * @param selectedFaults 
     */
    public DataLoader( SeismicMetadata aSource, 
                            long textureSize, 
                            int threads, 
                            int multiStatementLength, 
                            boolean preLoadFeaturesMode, 
                            int [] selectedHorizons,
                            int [] selectedFaults) {
     
        // set the selected SRDS source:
        this.srdsSource = aSource;
        
        // set number of threads:
        this.numOfThreads = threads;
        
        // set length of multi-statement:
        this.multiStatementLength = multiStatementLength;
        
        // set pre-load features mode:
        this.preLoadFeatures = preLoadFeaturesMode;
        
        // create an array of DatabaseConnector one for each thread:
        this.dbConnection = new DatabaseConnector[this.numOfThreads];
        for (int i=0; i < this.numOfThreads; i++){
            dbConnection[i] = new DatabaseConnector();
        }//for
        
        // array lists as queues for threads to retrieve points form DB:
        this.pointsQueue = new ArrayList[this.numOfThreads];
        
        // [31/5/2012] new queue system:
        // queues for threads to retrieve points form DB:
        this.pointsQueue_new = new Map[this.numOfThreads];
        
        // get a free source ID from FESVo for traces by setting the metadata:
        this.tracesSourceID = FESVo.setMetadata(aSource);
        
        
        // [23/10/2012] map property values of selected horizons to two newly generated source IDs from FESVo:
        for (int hPropVal : selectedHorizons){
            this.horizonsSourceIDs.put(FESVo.generateSourceID(DataType.HORIZON), hPropVal);
        }
        
        // [23/10/2012] map property values of selected faults to two newly generated source IDs from FESVo:
        for (int fPropVal : selectedFaults){
            System.err.print("Faults are not yet supported; sorry!"); // [TEMP] not supported
            
            this.faultsSourceIDs.put(FESVo.generateSourceID(DataType.FAULT), fPropVal);
        }
        
        
        // compute LOD unit dimension:
        int lodUnitWidth = (int)Math.ceil(
                this.srdsSource.pointA.distanceTo(this.srdsSource.pointB)/
                (this.srdsSource.lastXline - this.srdsSource.firstXline + 1));
        
        int lodUnitDepth = (int)Math.ceil(
                this.srdsSource.pointA.distanceTo(this.srdsSource.pointD)/
                (this.srdsSource.lastInline - this.srdsSource.firstInline + 1));
        
        // set LOD unit dimension in FESVo:
        FESVo.setLodUnitWidthDepth(lodUnitWidth, lodUnitDepth);
        
        // build a prepared statement for each connection with LOD Unit width and depth, and multi-statement length:
        for (int i=0; i < this.numOfThreads; i++){
//            dbConnection[i].buildTracePreparedStatement(
//                    FESVo.getLodUnitWidth(), FESVo.getLodUnitDepth(), this.multiStatementLength);
            // [4/3/2012]
            dbConnection[i].buildTracePreparedStatement_v2(this.multiStatementLength);
            
            // [25/6/2012] prepared statements for horizons:
            // passing number of available horizons:
            // [3/10/2012] only if pre-load features mode is not set
            if (!this.preLoadFeatures)
                dbConnection[i].buildHorizonPreparedStatement(this.horizonsSourceIDs.size());
        }
            
        // set max texture size and thus compute max. texture dimension and topLOD:
        this.setMaxTextureSize(textureSize);
        
        // load trace data coordinates from DB to FESVo:
        this.loadDataCoordinates(tracesSourceID);
        
        // Don't load horizon's coordinates if pre-load mode is set:
//        if (!this.preLoadFeatures){
        
        // ----------------------------------------------------------
        // | [TEMP] load horizons coodinates anyhow                 |
        // | since horizon shift method uses the cached coordinates |
        // ----------------------------------------------------------
        
            // load horizons data coordinates from DB to FESVo:
            for (int id : this.horizonsSourceIDs.keySet())
                this.loadDataCoordinates(id);
//        }
        
        // map for feature min/max height:
        featureHeight = new HashMap<Integer, ArrayList<Integer>>();
        
    }//constructor
    
    // ------------------------------------------------------------------------
    
    /**
     * set max. texture size and thus 
     * re-compute max. texture dimension and topLOD
     */ 
    public void setMaxTextureSize(long maxTextureSize) {
        this.maxTextureSize = maxTextureSize;
        
        /**
         * compute max. allowed texture width and depth
         * equation:
         * width * depth * height * sampleSize = maxTexSize
         * and: width/depth = delta.X/delta.Y
         * thus: width = depth * delta.X / delta.Y
         *       depth^2 = (maxTexSize * delta.Y) / (delta.X * height * sampleSize)
         */
        
        // delta X and delta Y, for the following equations:
        int deltaX = FESVo.xMax() - FESVo.xMin();
        int deltaY = FESVo.yMax() - FESVo.yMin();
        
        int maxTexDepth = (int)(Math.sqrt(
                  ((double)this.maxTextureSize * deltaY) 
                / ((double)deltaX * this.srdsSource.samplesPerTrace * this.srdsSource.sampleSize)));
        int maxTexWidth = (int)(((float)maxTexDepth * deltaX) / (float)deltaY);
                
        // compute top LOD, and thus max. texture dimension:
        // -------------------------------------------------
        
        // (1) get dimension of LOD 0:
        int texWidth = deltaX/FESVo.getLodUnitWidth();
        int texDepth = deltaY/FESVo.getLodUnitDepth();
        
        // save it:
        this.lod0Dimension = new int[]{texWidth, texDepth, this.srdsSource.samplesPerTrace};
        
        // (2) compute top LOD:
        this.topLOD = 0; // initial value
        while (true){
            // stop if the computed texture width and depth can fit in texture:
            if (texWidth <= maxTexWidth && texDepth <= maxTexDepth)
                break;
            
            // otherwise, go to next level - dividing by 2
            texWidth /= 2;
            texDepth /= 2;
            this.topLOD ++;
        }//while
        
        // check width and depth not to be0; 
        // if so, make them 1 as their min. values
        // and re-set max. texture size:
        if (texWidth < 1 && texDepth < 1) this.maxTextureSize = this.srdsSource.samplesPerTrace * this.srdsSource.sampleSize;
        if (texWidth < 1) texWidth = 1;
        if (texDepth < 1) texDepth = 1;
        
        // (3) set max. texture dimension:
        this.maxTextureDimension[0] = texWidth;
        this.maxTextureDimension[1] = texDepth;
        this.maxTextureDimension[2] = this.srdsSource.samplesPerTrace;
        
        // make current (active) texture dimension same as max. dimension as a default value:
        this.textureDimension = this.maxTextureDimension;
    }//setMaxTextureSize
    
    // ------------------------------------------------------------------------
    
    public void closeConnections()
    {
        for (DatabaseConnector db : this.dbConnection){
            db.closeTracePreparedStatement();
            if (!this.preLoadFeatures) // [3/10/2012]
                db.closeHorizonPreparedStatement(); // [25/6/2012]
            db.disconnect();
        }
    }//closeConnections
    
    // ========================================================================

    // returns the SRDS source related to this loader
    public SeismicMetadata getSrdsSource() {
        return srdsSource;
    }//getSrdsSource
    
    // ========================================================================
    
    public ByteBuffer buildFeaturesTexture() {

        // current lod (initial lod) here is top lod:
        return this.buildFeaturesTexture(0, 0, topLOD);
    }
    
    
    // ------------------------------------------------------------------------

    public ByteBuffer buildFeaturesTextureAtLowerResolution(int xLRef, int yLRef, int lod) {
        
        // finalLOD is topLOD if it's not defined:
        return this.buildFeaturesTextureAtLowerResolution(xLRef, yLRef, lod, topLOD);
        
    }
    
    // ------------------------------------------------------------------------

    public ByteBuffer buildFeaturesTextureAtLowerResolution(int xLRef, int yLRef, int iLOD, int fLOD) {
        
        // only if iLOD > fLOD:
        if (iLOD > fLOD){
            
            // set finalLOD:
            this.finalLOD = fLOD;
            
            this.loadAtLowerResolution = true;
        }//if

        return this.buildFeaturesTexture(xLRef, yLRef, iLOD);
    }
    
    // ------------------------------------------------------------------------
    
    // build a feature texture of a given LOD and reference location (local point)
    public ByteBuffer buildFeaturesTexture(int xLRef, int yLRef, int lod) {
        
        // set requested texture:
        this.loadingFeatures = true;
        
        // modified on [9/10/2012] to consider user's selection of features IDs
        if (!this.selectedFeaturesIDsToLoad){
            // set required IDs to be all available features since users did not specify:
            this.requiredIDs = this.horizonsSourceIDs.keySet();
        }
        else{
            // reset:
            this.selectedFeaturesIDsToLoad = false;
        }
        
        // call helper method:
        return buildTexture(xLRef, yLRef, lod);
        
    }//buildFeaturesTexture
    
    // ------------------------------------------------------------------------
    
    // build a single slice feature texture of a given LOD and reference location (local point)
    public ByteBuffer buildSingleSliceFeaturesTexture(int xLRef, int yLRef, int lod) {
        
        // set requested texture:
        this.loadingFeatures = true;
        
        // modified on [9/10/2012] to consider user's selection of features IDs
        if (!this.selectedFeaturesIDsToLoad){
            // set required IDs to be all available features since users did not specify:
            this.requiredIDs = this.horizonsSourceIDs.keySet();
        }
        else{
            // reset:
            this.selectedFeaturesIDsToLoad = false;
        }
        
        // call helper method:
        return buildSingleSliceTexture(xLRef, yLRef, lod);
        
    }//buildSingleSliceFeaturesTexture
    
    // ========================================================================
    
    // build a trace texture of top LOD
    public ByteBuffer buildTracesTexture() {
        // current lod (initial lod) here is top lod:
        return this.buildTracesTexture(0, 0, topLOD);
    }

    // ------------------------------------------------------------------------
    
    // [21/2/2012]
    // build a trace texture of a given LOD and reference location (local point)
    // loading traces into maximum texture dimension 
    // by repeating traces when lod > topLOD
    // this is to later enhance resolution in the buffer
    public ByteBuffer buildTracesTextureAtLowerResolution(int xLRef, int yLRef, int lod) {

        // finalLOD is topLOD if it's not defined:
        return this.buildTracesTextureAtLowerResolution(xLRef, yLRef, lod, topLOD);
        
    }//buildTracesTextureAtMaxTextureDimension()
    
    // ------------------------------------------------------------------------
    
    // [22/2/2012]
    // build a trace texture of a given LOD and reference location (local point)
    // loading traces into a texture of fLOD dimension
    // this is to later enhance resolution in the buffer
    public ByteBuffer buildTracesTextureAtLowerResolution(int xLRef, int yLRef, int iLOD, int fLOD) {
        
        // only if iLOD > fLOD:
        if (iLOD > fLOD){
            
            // set finalLOD:
            this.finalLOD = fLOD;
            
            this.loadAtLowerResolution = true;
            
            // map reference point to finalLOD and store it:
            int [] refPointFinal = FESVo.mapPoint(xLRef, yLRef, iLOD, fLOD);
            this.xLRefFinalLOD = refPointFinal[0];
            this.yLRefFinalLOD = refPointFinal[1];
        }//if
            
        return this.buildTracesTexture(xLRef, yLRef, iLOD);
        
    }//buildTracesTextureAtMaxTextureDimension()
    
    // ------------------------------------------------------------------------
    
    // build a trace texture of a given LOD and reference location (local point)
    public ByteBuffer buildTracesTexture(int xLRef, int yLRef, int lod) {
        
        // set requested texture:
        this.loadingFeatures = false;
        
        // set required IDs for this loading process:
        this.requiredIDs = new HashSet<Integer>(1); // capacity of 1
        this.requiredIDs.add(this.tracesSourceID);
        
        // call helper method:
        return buildTexture(xLRef, yLRef, lod);
        
    }//buildTracesTexture
    
    // ------------------------------------------------------------------------
    
    // build a trace texture of a given LOD and reference location (local point)
    public ByteBuffer buildSingleSliceTracesTexture(int xLRef, int yLRef, int lod) {
        
        // set requested texture:
        this.loadingFeatures = false;
        
        // set required IDs for this loading process:
        this.requiredIDs = new HashSet<Integer>(1); // capacity of 1
        this.requiredIDs.add(this.tracesSourceID);
        
        // call helper method:
        return buildSingleSliceTexture(xLRef, yLRef, lod);
        
    }//buildSingleSliceTracesTexture

    
    // ========================================================================
    
    // [28/5/2012] helper method: 
    // build a texture of a given LOD and reference location (local point)
    private ByteBuffer buildTexture(int xLRef, int yLRef, int lod) {
        // set initialLOD to be used if later enhancement was requested:
        this.initialLOD = lod;
        
        // compute data texture dimension based on requested LOD:
        int [] dataDimension = this.getTextureDimension(lod);
        
        // [TEST]
        System.out.print("\n\t[DataLoader_SRDS] Building ");
        if (loadingFeatures) System.out.print("Feature Texture");
        else System.out.print("Trace Texture");
        
        // depending on loading request, compute true texture dimension:
        if (loadAtLowerResolution){
            // compute true (final) texture 
            this.textureDimension = this.getTextureDimension(finalLOD);
            
            System.out.print(" at a lower resolution to be later enhanced, finalLOD: " + finalLOD + "\n"
                    + "\tdata dimension: [" + dataDimension[0] + "," + dataDimension[1] + "," + dataDimension[2] + "]");
        }//if
        
        else
            // set the dataDimension to be the true texture dimension:
            this.textureDimension = dataDimension;
        
        // create a buffer of size based on the computed texture dimension:
        if (loadingFeatures)
            featuresTexBuffer = GLBuffers.newDirectByteBuffer(
                textureDimension[0] * textureDimension[1] * textureDimension[2] * this.srdsSource.sampleSize);       
        else
            traceTexBuffer = GLBuffers.newDirectByteBuffer(
                textureDimension[0] * textureDimension[1] * textureDimension[2] * this.srdsSource.sampleSize);            
            
        // [TEST]
        System.out.println("\n\ttextureDimension: [" + textureDimension[0] + "," + textureDimension[1] + "," + textureDimension[2] + "], " 
                         + "\n\tat ref: (" + xLRef + "," + yLRef + "), LOD: " + lod);
        
        // call helper method:
        loadData(dataDimension, xLRef, yLRef, lod);
        
        // return the requested buffer:
        if (loadingFeatures)
            return this.featuresTexBuffer;
        // otherwise:
        return this.traceTexBuffer;
    }//buildTexture
    
    // ------------------------------------------------------------------------
    
    // [28/5/2012] helper method: 
    // build a single slice texture of a given LOD and reference location (local point)
    private ByteBuffer buildSingleSliceTexture(int xLRef, int yLRef, int lod) {
        
        // set initialLOD to be used if later enhancement was requested:
        this.initialLOD = lod;
        
        // [7/5/2012] For width: use the texture dimension of requested LOD, for depth use 1:
        int width = this.maxTextureDimension[0];
        if (this.topLOD > lod){
            width = width << (this.topLOD-lod);
        }
        int [] dataDimension = {width, 1, maxTextureDimension[2]};
        
        // [TEST]
        System.out.print("\n\t[DataLoader_SRDS] Building a single slice ");
        if (loadingFeatures) System.out.print("Feature Texture");
        else System.out.print("Trace Texture");
        
        
        // NO LOADING AT LOWER RESOLUTION:
        // -------------------------------
        
        // set the dataDimension to be the true texture dimension:
        this.textureDimension = dataDimension;
        
        // create a buffer of size based on the computed texture dimension:
        if (loadingFeatures)
            featuresTexBuffer = GLBuffers.newDirectByteBuffer(
                textureDimension[0] * textureDimension[1] * textureDimension[2] * this.srdsSource.sampleSize);       
        else
            traceTexBuffer = GLBuffers.newDirectByteBuffer(
                textureDimension[0] * textureDimension[1] * textureDimension[2] * this.srdsSource.sampleSize);            
            
        // [TEST]
        System.out.println("\n\ttextureDimension: [" + textureDimension[0] + "," + textureDimension[1] + "," + textureDimension[2] + "], " 
                         + "\n\tat ref: (" + xLRef + "," + yLRef + "), LOD: " + lod);
        
        // call helper method:
        loadData(dataDimension, xLRef, yLRef, lod);
        
        // return the requested buffer:
        if (loadingFeatures)
            return this.featuresTexBuffer;
        // otherwise:
        return this.traceTexBuffer;
    }//buildSingleSliceTexture
    
    // ========================================================================
    
    // Modified and renamed on [29/5/2012] to include features loading
    // helper method for re-usability, 
    // loads data into buffer
    private void loadData(int[] dataDimension, int xLRef, int yLRef, int lod) {
        
        // TEMP
        if (!this.loadingFeatures)
            this.loadTraces(dataDimension, xLRef, yLRef, lod, 0, dataDimension[1]);
        
        else{
            
            // [25/9/2012] considering pre-load features mode:
            if (this.preLoadFeatures){
                // load all features from DB into FESVo if not loaded already:
                if (!this.featuresLoadedIntoFESVo){
                    preLoadFeaturesIntoFESVo();
                }
                this.loadDataFromFESVoOnly(dataDimension, xLRef, yLRef, lod, 0, dataDimension[1]);
            }//if
            
            // otherwise, go to normal:
            else{
                // new methods to cover traces and features:
                this.loadData(dataDimension, xLRef, yLRef, lod, 0, dataDimension[1]);
            }
            
        }
        
        
    }//loadData
    
    // a more general trace loader method,
    // to be also used by enhanceTraceTextureResolution
    private void loadTraces(int[] dataDimension, int xLRef, int yLRef, int lod, int firstSlice, int lastSlice) {
        // create array lists as queues for threads to retrieve points form DB:
        for (int i=0; i < this.pointsQueue.length; i++)
            this.pointsQueue[i] = new ArrayList<PointTag>();
        
        // queue iterator to equally distribute points among threads:
        int queueNo = 0; // initial value
        
        // [TEST] get starting time:
        long startTime = System.currentTimeMillis();
                
        // for every (s,t) in the texture buffer, 
        // load data from FESVo, or from DB if not available in FESVo:
        for (int t = firstSlice; t < lastSlice; t++)
        {
            // calculate yL (local point on required LOD relative to the ref. point)
            int yL = t + yLRef;
            
            for (int s = 0; s < dataDimension[0]; s++)
            {
                // calculate xL (local point on required LOD relative to the ref. point)
                int xL = s + xLRef;
                
                // create required point tag:
                PointTag tag = new PointTag(xL, yL, this.tracesSourceID);
                
                // trace for this point:
                Trace trace = null;
                
                // check if FESVo has a trace in this point:
                trace = (Trace) FESVo.get(tag, lod);
                
                // if trace is found, load it to buffer:
                if (trace != null){
                    // check if this is an upload at lower resoltion
                    if (this.loadAtLowerResolution)
                        this.uploadTextureBufferAtLowerResolution(s, t, lod, trace);
                    else
                        this.uploadTextureBuffer(s, t, trace);
                    
                    this.FESVoPointsCount++;
                }//if
                
                // if trace not available in FESVo, get it from DB
                // adding the required point tag to a thread queue:
                else{
                    this.pointsQueue[queueNo++].add(tag);
                    
                    // reset queueNo if reached max:
                    if (queueNo == this.numOfThreads)
                        queueNo = 0;
                }//else
                
            }// go to next s
        }//completed all (s,t) of texture
        
        // get end time:
        long endTime = System.currentTimeMillis();

        // print result:
        System.out.println("\tLoaded " + this.FESVoPointsCount + " points from FESVo in " + (endTime - startTime) + "ms,");
        
        // don't start threads of empty queue:
        int requiredThreads = this.numOfThreads; // initial value
        for (int i=0; i < this.numOfThreads; i++)
        {
            if (this.pointsQueue[this.numOfThreads - 1 - i].isEmpty())
                requiredThreads --;
            else
                break;
        }//for
        
        // [TEST] get starting time:
        startTime = System.currentTimeMillis();
        
        // create and run threads passing points queue to each:
        Thread [] thread = new Thread[requiredThreads];
        for (int i=0; i < requiredThreads; i++)
        {
            // create a LoadingThread object:
            LoadingThread loadingThread = new LoadingThread(xLRef, yLRef, lod, this.pointsQueue[i], this.dbConnection[i], this);
            
            // create a Thread passing this LoadingThread object:
            thread[i] = new Thread(loadingThread);
            
            // run it:
            thread[i].start();
        }//for
        
        // wait for all threads to complete:
        for (int i=0; i < requiredThreads; i++)
        {
            try {
                thread[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//for
        
        // get end time:
        endTime = System.currentTimeMillis();

        /**
         * [28/5/2012] Note: if data coordinates were known in advance (fix-points mode), 
         * then all searched LOD united have non-null data units; empty units are not searched;
         * in the case of discovery mode, all data units are searched
         * those empty units are of the total searched units.
         */
        
        // print result:
        System.out.println("\tCompleted search in " + this.dbVisitCount + " LOD units; " + this.emptyUnitsCount + " were empty; "
                + "in " + (endTime - startTime) + "ms.\n");
        
        // clear counts:
        this.FESVoPointsCount = 0;
        this.dbVisitCount = 0;
        this.emptyUnitsCount = 0;
        
        this.traceTexBuffer.rewind();
        
        // reset loadAtLowerResolution to false
        loadAtLowerResolution = false;
    }//loadTraces
    
    // ------------------------------------------------------------------------
    
    // Modified and renamed on [29/5/2012] to include features loading
    // Note: only supporting horizons for now.
    // a more general data loader method,
    // to be also used by enhanceTraceTextureResolution
    private void loadData(int[] dataDimension, int xLRef, int yLRef, int lod, int firstSlice, int lastSlice) {
        // create array lists as queues for threads to retrieve points form DB:
        for (int i=0; i < this.pointsQueue_new.length; i++){
            this.pointsQueue_new[i] = new HashMap<Point, Set<Integer>>();
        }
        
        // queue iterator to equally distribute points among threads:
        int queueNo = 0; // initial value
        
        // [TEST] get starting time:
        long startTime = System.currentTimeMillis();
                
        // for every (s,t) in the texture buffer, 
        // load data from FESVo, or from DB if not available in FESVo:
        for (int t = firstSlice; t < lastSlice; t++)
        {
            // calculate yL (local point on required LOD relative to the ref. point)
            int yL = t + yLRef;
            
            for (int s = 0; s < dataDimension[0]; s++)
            {
                // calculate xL (local point on required LOD relative to the ref. point)
                int xL = s + xLRef;
                
                // [1/6/2012] for the new queue system:
                // current point:
                Point point = new Point(xL, yL);
                // set of required source IDs associated with the above point
                // to be placed in a queue for DB queries:
                Set<Integer> pointIDs = new HashSet<Integer>();
                
                
                // a loop through all sources to consider multi-features:
                for (int id : this.requiredIDs){
                
                    // create required point tag:
                    PointTag tag = new PointTag(xL, yL, id);

                    // check if FESVo has a data in this point:
                    GeospatialData data = FESVo.get(tag, lod);

                    // if data is found, load it to buffer:
                    if (data != null){
                        if (this.loadingFeatures){
                            if (this.loadAtLowerResolution)
                                this.uploadTextureBufferAtLowerResolution(s, t, lod, (Feature)data, id);
                            else
                                this.uploadTextureBuffer(s, t, (Feature)data, id);
                        }
                        // if loading a trace:
                        else{
                            if (this.loadAtLowerResolution)
                                this.uploadTextureBufferAtLowerResolution(s, t, lod, (Trace)data);
                            else
                                this.uploadTextureBuffer(s, t, (Trace)data);
                        }
                        

                        this.FESVoPointsCount++;
                    }//if

                    // if data not available in FESVo, add its ID 
                    // to get it from DB:
                    else{
                        // for the new Queue system:
                        // add source id to pointsIDs set:
                        pointIDs.add(tag.sourceID);
                    }
                
                }// go to next required ID
                
                // add point to the new queue if there is one or more ID linked to it:
                if (!pointIDs.isEmpty()){
                    this.pointsQueue_new[queueNo++].put(point, pointIDs);
                    
                    // reset queueNo if reached max:
                    if (queueNo == this.numOfThreads)
                        queueNo = 0;
                }
                
            }// go to next s
        }//completed all (s,t) of texture
        
        // get end time:
        long endTime = System.currentTimeMillis();

        // print result:
        System.out.println("\tLoaded " + this.FESVoPointsCount + " points from FESVo in " + (endTime - startTime) + "ms,");
        
        // don't start threads of empty queue:
        int requiredThreads = this.numOfThreads; // initial value
        int requiredThreads_new = this.numOfThreads; // initial value
        for (int i=0; i < this.numOfThreads; i++)
        {
            if (this.pointsQueue_new[this.numOfThreads - 1 - i].isEmpty())
                requiredThreads_new --;
            else
                break;
        }//for
        
        
        // [TEST] get starting time:
        startTime = System.currentTimeMillis();
        
        // create and run threads passing points queue to each:
        Thread [] thread = new Thread[requiredThreads];
        for (int i=0; i < requiredThreads; i++)
        {
            // create a LoadingThread object:
            LoadingThread loadingThread = new LoadingThread(xLRef, yLRef, lod, this.pointsQueue_new[i], this.dbConnection[i], this);
            
            // create a Thread passing this LoadingThread object:
            thread[i] = new Thread(loadingThread);
            
            // run it:
            thread[i].start();
        }//for
        
        // wait for all threads to complete:
        for (int i=0; i < requiredThreads; i++)
        {
            try {
                thread[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//for
        
        // get end time:
        endTime = System.currentTimeMillis();

        /**
         * [28/5/2012] Note: if data coordinates were known in advance (fix-points mode), 
         * then all searched LOD united have non-null data units; empty units are not searched;
         * in the case of discovery mode, all data units are searched
         * those empty units are of the total searched units.
         */
        
        // print result:
        System.out.println("\tCompleted search in " + this.dbVisitCount + " LOD units; " + this.emptyUnitsCount + " were empty; "
                + "in " + (endTime - startTime) + "ms.\n");
        
        // clear counts:
        this.FESVoPointsCount = 0;
        this.dbVisitCount = 0;
        this.emptyUnitsCount = 0;
        
        this.traceTexBuffer.rewind();
        
        // reset loadAtLowerResolution to false
        loadAtLowerResolution = false;
    }//loadData
    
    // ------------------------------------------------------------------------
    
    // [25/9/2012] used in case of preLoad mode
    private void loadDataFromFESVoOnly(int[] dataDimension, int xLRef, int yLRef, int lod, int firstSlice, int lastSlice) {
        // [TEST] get starting time:
        long startTime = System.currentTimeMillis();
                
        // for every (s,t) in the texture buffer, 
        // load data from FESVo:
        for (int t = firstSlice; t < lastSlice; t++)
        {
            // calculate yL (local point on required LOD relative to the ref. point)
            int yL = t + yLRef;
            
            for (int s = 0; s < dataDimension[0]; s++)
            {
                // calculate xL (local point on required LOD relative to the ref. point)
                int xL = s + xLRef;
                
                // a loop through all sources to consider multi-features:
                for (int id : this.requiredIDs){
                
                    // create required point tag:
                    PointTag tag = new PointTag(xL, yL, id);

                    // check if FESVo has a data in this point:
                    GeospatialData data = FESVo.get(tag, lod);

                    // if data is found, load it to buffer:
                    if (data != null){
                        if (this.loadingFeatures){
                            if (this.loadAtLowerResolution)
                                this.uploadTextureBufferAtLowerResolution(s, t, lod, (Feature)data, id);
                            else
                                this.uploadTextureBuffer(s, t, (Feature)data, id);
                        }
                        // if loading a trace:
                        else{
                            if (this.loadAtLowerResolution)
                                this.uploadTextureBufferAtLowerResolution(s, t, lod, (Trace)data);
                            else
                                this.uploadTextureBuffer(s, t, (Trace)data);
                        }
                        

                        this.FESVoPointsCount++;
                    }//if

                    // if data not available in FESVo, do nothing
                
                }// go to next required ID
                
                
                
            }// go to next s
        }//completed all (s,t) of texture
        
        // get end time:
        long endTime = System.currentTimeMillis();

        // print result:
        System.out.println("\tLoaded " + this.FESVoPointsCount + " points from FESVo in " + (endTime - startTime) + "ms,");
        
        // clear counts:
        this.FESVoPointsCount = 0;
        this.dbVisitCount = 0;
        this.emptyUnitsCount = 0;
                
        // reset loadAtLowerResolution to false
        loadAtLowerResolution = false;
    }//loadData
    
    // ------------------------------------------------------------------------
    
    // reloads a number of slices (slicesToEnhance) strating from firstSlice
    // at finalLOD
    public ByteBuffer enhanceTracesTextureResolution(int firstSlice){
        
        // [TEST]
        System.out.println("\n\t[DataLoader_SRDS] Enhancing Trace Texture Resolution"
                + "\n\tfirstSlice: " + firstSlice 
                + ", slicesToEnhance: " + slicesToEnhance);
        
        this.loadData(textureDimension, xLRefFinalLOD, yLRefFinalLOD, finalLOD, firstSlice, (firstSlice+slicesToEnhance));
        
        return this.traceTexBuffer;
        
    } 
    
    // ------------------------------------------------------------------------

    // [21/2/2012]
    public int[] getMaxTextureDimension() {
        return maxTextureDimension;
    }

    // [8/5/2012]
    public int[] getLod0Dimension() {
        return lod0Dimension;
    }
    
    public int[] getTextureDimension() {
        // return current texture dimension
        return this.textureDimension;
    }//getTextureDimension()
    
    // returns texture dimension based on the given LOD
    private int[] getTextureDimension(int lod) {
        // reduce texture dimension if given lod is larger than top LOD:
        if (lod > this.topLOD){
            int width = this.maxTextureDimension[0] >> (lod-this.topLOD); // ( >> ..) means (/2^..)
            int depth = this.maxTextureDimension[1] >> (lod-this.topLOD); // ( >> ..) means (/2^..)
            
            // make width and/or depth 1 as their min. values:
            if (width < 1) width = 1;
            if (depth < 1) depth = 1;
            
            // return dimension array:
            return new int[]{width, depth, this.maxTextureDimension[2]};
        }
            
        
        // otherwise, return max. texture dimension
        return this.maxTextureDimension;
    }//getTextureDimension(int lod)

    // ------------------------------------------------------------------------
    
    public int[] getMinMaxHeight(int src) {
        
        if (this.featureHeight.isEmpty() || !this.featureHeight.containsKey(src))
            return new int[]{0,0};
        
        // otherwise:
        int [] h = new int[2];
        
        h[0] = this.featureHeight.get(src).get(0).intValue();
        h[1] = this.featureHeight.get(src).get(1).intValue();
        
        return h;
    }
    
    // ------------------------------------------------------------------------

    public int getTopLOD() {
        return topLOD;
    }//getTopLOD
    
//    public Point getRefPoint(){
//        return new Point(this.xLRefFinalLOD, this.yLRefFinalLOD);
//    }
//    
//    public int getCurrentLOD(){
//        return this.initialLOD;
//    }

    // ------------------------------------------------------------------------

    // [9/9/2012]
    public void shiftHorizon_old(PointTag refTag0, int timeShift, int diameter){
        
        // prepare geo-tags to be shifted:
        // -------------------------------
        
        // a reference geo-tag:
        GeoTag refGeoTag = (FESVo.get(refTag0, 0)).geoTag;
        
        // a set of new geo-tags to alter (shift):
        Set<GeoTag> geoTags = new HashSet<GeoTag>();
        
        for (int y=(-1*diameter/2); y < (diameter/2); y++)
        {
            for (int x=(-1*diameter/2); x < (diameter/2); x++)
            {
                // this point:
                Point p = new Point((refGeoTag.x + x), (refGeoTag.y + y));
                
                // check if this point is part of the required horizon:
                if (FESVo.contains(p, refTag0.sourceID)){
                    geoTags.add(new GeoTag(p, refGeoTag.sourceID));
                }//if
                
            }//for
        }//for
        
        // [TEST] print
        System.out.println(geoTags.size() + " geo-tags to be shifted");
        
        
        // pass geo-tags to DB only if geoTags is not empty:
        // -------------------------------------------------
        
        if (geoTags.isEmpty())
            System.err.println("No geoTags found to shift!");
        
        else
            this.dbConnection[0].shiftHorizon(geoTags, timeShift, this.srdsSource.srdsSrcID);
        
    }//shiftHorizon
    
    
    // ------------------------------------------------------------------------

    // [27/9/2012]
    public void shiftHorizon(PointTag refTag0, int timeShift, int diameter, int userSrcID){
        
        // change number of threads to be used in this method if required
        int numOfThreads = this.numOfThreads;
        
        long startTime, endTime;
        
        // [TEST] get starting time:
        startTime = System.currentTimeMillis();
        
        // prepare geo-tags to be shifted:
        // -------------------------------
        
        // a reference geo-tag:
        GeoTag refGeoTag = (FESVo.get(refTag0, 0)).geoTag;
        
        // a set of new geo-tags to alter (shift) in queues:
        Set<GeoTag> [] geoTags = new Set[numOfThreads];
        for (int i=0; i < geoTags.length; i++){
            geoTags[i] = new HashSet<GeoTag>();
        }
        
        // queue number:
        int q = 0;
        
        for (int y=(-1*diameter/2); y < (diameter/2); y++)
        {
            for (int x=(-1*diameter/2); x < (diameter/2); x++)
            {
                // this point:
                Point p = new Point((refGeoTag.x + x), (refGeoTag.y + y));
                
                // check if this point is part of the required horizon:
                if (FESVo.contains(p, refTag0.sourceID)){
                    geoTags[q++].add(new GeoTag(p, refGeoTag.sourceID));
                    
                    // reset queue if required:
                    if (q == numOfThreads) q = 0;
                }//if
                
            }//for
        }//for
        
        // [TEST] calculate number of tags to alter:
        int totalSize = 0;
        for (Set<GeoTag> tagsQueue : geoTags)
            totalSize += tagsQueue.size();
        System.out.println(totalSize + " geo-tags to be shifted");
        
        
        // Now deal with the DB:
        // ---------------------
        
        int baselineSrcID = this.srdsSource.srdsSrcID;
        //int userSrcID;
        String timestamp;
        
        // [23.2.2013] start a baseline type grouping:
        this.dbConnection[0].startBaselineTypeGrouping(baselineSrcID);
        
        // start deletion type grouping and get a newTimestamp:
        timestamp = this.dbConnection[0].startDeletionTypeGrouping(baselineSrcID, userSrcID);
        
        // don't start threads of empty queue:
        int requiredThreads = numOfThreads; // initial value
        for (int i=0; i < numOfThreads; i++)
        {
            if (geoTags[numOfThreads - 1 - i].isEmpty()){
                requiredThreads --;
            }
            else{
                break;
            }
        }//for
        
        // create and run threads passing tags queue to each:
        Thread [] thread = new Thread[requiredThreads];
        for (int i=0; i < requiredThreads; i++)
        {
            // create a HorizonInsertionThread object:
            HorizonInsertionThread horizonInsertionThread = 
                    new HorizonInsertionThread(this.dbConnection[i], geoTags[i], baselineSrcID, userSrcID, timestamp, 0);
            
            // create a Thread passing this LoadingThread object:
            thread[i] = new Thread(horizonInsertionThread);
            
            // run it:
            thread[i].start();
        }//for
        
        // wait for all threads to complete:
        for (int i=0; i < requiredThreads; i++)
        {
            try {
                thread[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//for
        
        
        // end latest grouping:
        this.dbConnection[0].endLatestGrouping(baselineSrcID, userSrcID);
        
        // start insertion type grouping and get a newTimestamp:
        timestamp = this.dbConnection[0].startInsertionTypeGrouping(baselineSrcID, userSrcID);
        
        // create and run threads passing tags queue to each:
        for (int i=0; i < requiredThreads; i++)
        {
            // create a HorizonInsertionThread object:
            HorizonInsertionThread horizonInsertionThread = 
                    new HorizonInsertionThread(this.dbConnection[i], geoTags[i], baselineSrcID, userSrcID, timestamp, timeShift);
            
            // create a Thread passing this LoadingThread object:
            thread[i] = new Thread(horizonInsertionThread);
            
            // run it:
            thread[i].start();
        }//for
        
        // wait for all threads to complete:
        for (int i=0; i < requiredThreads; i++)
        {
            try {
                thread[i].join();
            } catch (InterruptedException ex) {
                Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }//for
        
        // end latest grouping:
        this.dbConnection[0].endLatestGrouping(baselineSrcID, userSrcID);
        
        // get end time:
        endTime = System.currentTimeMillis();

        // print result:
        System.out.println("\tCompleted horizon shifting in " + (endTime - startTime) + "ms.\n");
        
        
    }//shiftHorizon
    
    // ------------------------------------------------------------------------

    // [9/10/2012]
    /**
     * 
     * @param horizonSrcID FESVo ID for this horizon
     * @param timestamp timestamp at which this horizon was created
     * @param userSrcID the user who is deleting
     */
    public void deleteHorizon(int horizonSrcID, DataTimestamp timestamp, int userSrcID){
        
        int baselineSrcID = this.srdsSource.srdsSrcID;
        String newTimestamp;
        
        
        // verify user id:
        // [TO DO] //
        
        // start deletion type grouping and get a newTimestamp:
        newTimestamp = this.dbConnection[0].startDeletionTypeGrouping(baselineSrcID, userSrcID);
        
        // delete:
        this.dbConnection[0].deleteSession(baselineSrcID, this.horizonsSourceIDs.get(horizonSrcID), timestamp, userSrcID, newTimestamp);
        
        // end latest grouping:
        this.dbConnection[0].endLatestGrouping(baselineSrcID, userSrcID);
    }//deleteHorizon
    
    // ------------------------------------------------------------------------

//    // [25/9/2012] sets the mode of pre-loading features:
//    public void setPreLoadFeaturesMode(boolean mode){
//        this.preLoadFeatures = mode;
//    }
    
    // ------------------------------------------------------------------------

    // [26/9/2012] clears cached features in FESVo
    public void clearFeaturesCache(){
        for (int featureID : this.horizonsSourceIDs.keySet()){
            FESVo.clear(featureID);
        }
        this.featuresLoadedIntoFESVo = false;
    }//clearFeaturesCache
    
    // ------------------------------------------------------------------------

    // [29/9/2012] returns an array of grouping effective newTimestamp
    public ArrayList<DataTimestamp> getGroupingEffectiveTimestamps()
    {
        return this.dbConnection[0].getGroupingEffectiveTimestamps(this.srdsSource.srdsSrcID, new HashSet(this.horizonsSourceIDs.values()));
    }//getGroupingEffectiveTimestamps
    
    // ------------------------------------------------------------------------

    // [8/10/2012] returns an array of 'insertion' type grouping effective newTimestamp
    public ArrayList<DataTimestamp> getInsertionTypeGroupingEffectiveTimestamps(int fesvoSrcID)
    {
        return this.dbConnection[0].getInsertionTypeGroupingEffectiveTimestamps(this.srdsSource.srdsSrcID, this.horizonsSourceIDs.get(fesvoSrcID).intValue());
    }//getGroupingEffectiveTimestamps
    
    // ------------------------------------------------------------------------

    // [30/9/2012] sets selected newTimestamp
    public void setFeaturesTimestamp(DataTimestamp ts){
        setFeaturesTimestamp(ts, false);
    }//setFeaturesTimestamp
    
    // ------------------------------------------------------------------------

    // [8/10/2012] sets selected newTimestamp and an option to load only selected newTimestamp
    public void setFeaturesTimestamp(DataTimestamp ts, boolean tsOnly){
        this.featuresTimestamp = ts;
        this.loadOnlySelectedTimestamp = tsOnly;
    }//setFeaturesTimestamp
    
    // ------------------------------------------------------------------------

    // [3/10/2012], updated on [28/10/2012] to support faults
    // returns an array of currently loaded features FESVo IDs
    public int [] getFeaturesIDs()
    {
        int [] ids = new int[this.horizonsSourceIDs.keySet().size()
                           + this.faultsSourceIDs.keySet().size()];
        int i=0;
        
        // add horizons IDs
        for (int id : this.horizonsSourceIDs.keySet()){
            ids[i++] = id;
        }
        
        // add faults IDs
        for (int id : this.faultsSourceIDs.keySet()){
            ids[i++] = id;
        }
        
        return ids;
    }//getFeaturesIDs
    
    // ------------------------------------------------------------------------

    // [9/10/2012]
    // sets required features FESVo ID to laod
    public void setFeaturesIDsToLoad(int id){
        Set<Integer> ids = new HashSet<Integer>();
        ids.add(id);
        setFeaturesIDsToLoad(ids);
    }
    
    public void setFeaturesIDsToLoad(Set<Integer> ids){
        this.requiredIDs = ids;
        this.selectedFeaturesIDsToLoad = true;
    }
    
    // ========================================================================
    // ::::::::::::::::::::::: Private (Helper) Methods :::::::::::::::::::::::
    // ========================================================================

    private static class LoadingThread
        implements Runnable {
        
        private int xLRef, yLRef, lod;
        private ArrayList<PointTag> pointsQueue_old;
        private Map<Point, Set<Integer>> pointsQueue;
        private DatabaseConnector db;
        private DataLoader loader;

        // constructor
        public LoadingThread(int xLRef, int yLRef, int lod, ArrayList pointsQueue, DatabaseConnector db, DataLoader loader) {
            this.xLRef = xLRef;
            this.yLRef = yLRef;
            this.lod = lod;
            this.pointsQueue_old = pointsQueue;
            this.db = db;
            this.loader = loader;
            
        }
        
        // [14/6/2012] new constructor to support the new queue system:
        public LoadingThread(int xLRef, int yLRef, int lod, Map pointsQueue, DatabaseConnector db, DataLoader loader) {
            this.xLRef = xLRef;
            this.yLRef = yLRef;
            this.lod = lod;
            this.pointsQueue = pointsQueue;
            this.db = db;
            this.loader = loader;
            
        }
        
        @Override
        public void run() {
            if (loader.loadingFeatures){
                run_new(); // new queue system
            }
            else
                run_old(); // old queue system
        }
        
        private void run_old(){
            
            // for each point:
            int i=0; // an index on pointsQueue
            while (i < pointsQueue_old.size())
            {
                // ArrayList for starting points to be passed to getMultiTrace()
//                ArrayList<GeoTag> startingPoints = new ArrayList<GeoTag>();
                
                // ArrayList of geo-tags to be passed to getMultiTrace() [4/3/2012]
                ArrayList<GeoTag> geoTags = new ArrayList<GeoTag>();
                
                // Mapping geo-tags to point-tags [25/6/2012] to fix the re-mapping bug
                Map<GeoTag, PointTag> tagMap = new HashMap<GeoTag, PointTag>();
                
                for (int s=0; s < loader.multiStatementLength; s++)
                {
                    // get next point tag from queue:
                    PointTag pointTag = pointsQueue_old.get(i++);
                    
                    // map point to LOD0:
                    PointTag lod0Tag = FESVo.mapPointToLOD0(pointTag, lod);

                    // now map local points of LOD0 to geo-location (x,y)
                    int x = (lod0Tag.x * FESVo.getLodUnitWidth()) + FESVo.xMin();
                    int y = (lod0Tag.y * FESVo.getLodUnitDepth()) + FESVo.yMin();
                    
                    // create a GeoTag and add it the array list
//                    startingPoints.add(new GeoTag(x,y,loader.srdsDataSrcID));
                    
                    // get exact coordinate of data which lies in this LOD unit: [4/3/2012]
                    Point point = FESVo.getDataCoordinate(new Point(x,y), loader.tracesSourceID);
                    
                    // if no trace lies there: increment empty cells count,
                    // break and decrement s counter:
                    if (point == null){
                        loader.incrementEmptyUnitsCount();
                        s--;
                        //break; // [BUG] chenged to continue on [18/6/2012]
                    }
                    else{
                        // add tag to array to be searched in DB:
                        GeoTag gt = new GeoTag(point, loader.srdsSource.srdsSrcID);
                        geoTags.add(gt);
                        
                        // map this geo-tag to the local tag [25/6/2012]
                        tagMap.put(gt, pointTag);
                    }
                    
                    // break if reached end of pointsQueue
                    if (i == pointsQueue_old.size()) break;
                }//for
                    
                // look for traces in multi locations:
//                Trace [] traces = db.getMultiTraces(startingPoints.toArray(new GeoTag[startingPoints.size()]));
                Trace [] traces = db.getMultiTraces_v2(geoTags.toArray(new GeoTag[geoTags.size()]));//[4/3/2012]

                // loop through traces
                for (int p=0; p < traces.length; p++){
                    
                    // increment db visit count:
                    loader.incrementDbVisitCount();
                
                    // [28/5/2012] Note: if knowing data coordinates in advance, then all returned data units are non-null:
                    
                    if (traces[p] != null)
                    {
                        
                        ///////////////////////////////////////////////////////
                        //  __________________________________________________
                        // |                                                  |
                        // | BUG  BUG  BUG  BUG  BUG  BUG  BUG  BUG  BUG  BUG |
                        // |__________________________________________________|
                        //
                        ///////////////////////////////////////////////////////
                        //  __________________________________________________
                        // |                                                  |
                        // |       FIXED ON [25/6/2012] by adding tagMap      |
                        // |__________________________________________________|
                        //
                        ///////////////////////////////////////////////////////
                        
                        
                        // current associated tag:
                        PointTag tag;
//                        tag = pointsQueue_old.get(i-traces.length+p); // [BUG]
                        tag = tagMap.get(traces[p].geoTag); // to fix the bug
                        
                        // compute location of trace in texture (s,t):
                        int s = tag.x - this.xLRef;
                        int t = tag.y - this.yLRef;

                        // [TEST]
    //                    FESVoSystem.out.println(Thread.currentThread().getName() + ": Trace found for ( " + s + " , " + t + " ), LOD0 Location ( " + lod0Tag.x + " , " + lod0Tag.y + " )");
    //                    FESVoSystem.out2.println(Thread.currentThread().getName() + ": " + trace.toString());

                        // upload texture buffer:
                        if (loader.loadAtLowerResolution)
                            loader.uploadTextureBufferAtLowerResolution(s, t, lod, traces[p]);
                        else
                            loader.uploadTextureBuffer(s, t, traces[p]);

                        // put trace into FESVo:
                        FESVo.putTrace(traces[p], tag, lod);
                    }//if

                    // if not, increment empty cells count:
                    else
                        loader.incrementEmptyUnitsCount();
                        
                }//for

            }//while
                
        }//run()
        
        private void run_new(){
            
            // an interator to loop through points:
            Iterator<Point> pointsIterator = pointsQueue.keySet().iterator();
            
            // an index for multiStatementLength:
            int si = 0;// statement-index (si): initial value 0
            
            // Map of geo-tags to be passed to DB
            Map<Point, Set<Integer>> geoTags = new HashMap<Point, Set<Integer>>();
            
            // Mapping geo-tags to point-tags
            Map<GeoTag, PointTag> tagMap = new HashMap<GeoTag, PointTag>();
            
            // go through points in the queue:
            while (pointsIterator.hasNext())
            {
                // current local point: 
                Point localPoint = pointsIterator.next();

                // an iterator for source IDs required for this point:
                Iterator<Integer> ids = pointsQueue.get(localPoint).iterator();
                
                // map next points to geo-location:
                Point referenceLocation = FESVo.mapToGeoLocation(localPoint, lod);
                
                // get first id:
                int id = ids.next();
                
                // first loop through IDs unill we get an valid coordinate:
                outerLoop: while(true)
                {
                    // get exact coordinate of data which lies in this LOD unit: [4/3/2012]
                    Point dataCoord = FESVo.getDataCoordinate(referenceLocation, id, lod);
                    
                    // if no trace lies there: increment empty cells count,
                    // break and decrement s counter:
                    if (dataCoord == null){
                        loader.incrementEmptyUnitsCount();
                        
                        if (ids.hasNext()){
                            // get next id:
                            id = ids.next();
                            continue; // goto outerLoop
                        }
                        else
                            break; // exit outerLoop
                        
                    }
                    
                    // add data coordinate to geo-tags map:
                    geoTags.put(dataCoord, (new HashSet<Integer>()));
                    
                    // get SRDS (DB) trace's source id or feature's prop_val:
                    int srdsID;
                    if (loader.loadingFeatures)
                        srdsID = loader.horizonsSourceIDs.get(id);
                    else
                        srdsID = loader.srdsSource.srdsSrcID;
                    
                    // add to geoTags:
                    geoTags.get(dataCoord).add(srdsID);

                    // mapp data geo-tag to its coresponding point-tag:
                    // (for backward mapping)
                    tagMap.put(new GeoTag(dataCoord, srdsID), new PointTag(localPoint, id));

                    // second loop through IDs to get those  coordinate:
                    innerLoop: while(ids.hasNext())
                    {
                        // get next id:
                        id = ids.next();

                        // check if the coordinate found is also applicable for this id:
                        if (FESVo.contains(dataCoord, id))
                        {
                            // get SRDS (DB) trace's source id or feature's prop_val:
                            if (loader.loadingFeatures)
                                srdsID = loader.horizonsSourceIDs.get(id);
                            else
                                srdsID = loader.srdsSource.srdsSrcID;

                            // add to geoTags:
                            geoTags.get(dataCoord).add(srdsID);

                            // mapp data geo-tag to its coresponding point-tag:
                            // (for backward mapping)
                            tagMap.put(new GeoTag(dataCoord, srdsID), new PointTag(localPoint, id));
                        }
                        else{
                            // increment multi-statement index:
                            si++;
                            
                            // prepare for a new multi-statement request:
                            if (si == loader.multiStatementLength){
                                // reset statement index:
                                si = 0;

                                // load required data in the geo-tags list:
                                runStatement(geoTags, tagMap);

                                // re-create required maps:
                                geoTags = new HashMap<Point, Set<Integer>>();
                                tagMap = new HashMap<GeoTag, PointTag>();
                            }//if
                            
                            continue outerLoop; // goto outerLoop
                        }
                    }//innerLoop
                    
                    // increment multi-statement index:
                    si++;
                    
                    // exit loop
                    break;
                    
                }//outerLoop
                
                
                // prepare for a new multi-statement request:
                if (si == loader.multiStatementLength){
                   // reset statement index:
                    si = 0;
                    
                    // load required data in the geo-tags list:
                    runStatement(geoTags, tagMap);
                    
                    // re-create required maps:
                    geoTags = new HashMap<Point, Set<Integer>>();
                    tagMap = new HashMap<GeoTag, PointTag>();
                }//if
                
                if (si > loader.multiStatementLength){
                    // ERROR!
                    System.err.println("Error: statement index exceeded multi-statement length!");
                }
                
            }// get next point in queue
        }//run
        
        
        private void runStatement(Map<Point, Set<Integer>> geoTags, Map<GeoTag, PointTag> tagMap){

            GeospatialData [] data = null;
            
            // get multi-data from DB:
            if (loader.loadingFeatures){
                data = db.getMultiHorizons(geoTags, loader.srdsSource.srdsSrcID);
            }
            else{
                ////// TO DO >>>> db.getMultiTraces_v3
            }
            
            if (data == null){
                System.err.println("Data array is null, nothing was returned from DB!!");
                return;
            }//if
            
            // loop through data array
            for (int i=0; i < data.length; i++){

                // increment db visit count:
                loader.incrementDbVisitCount();

                // [28/5/2012] Note: if knowing data coordinates in advance, then all returned data units are non-null:
                if (data[i] == null){
                    System.err.println("data [" + i + "] is null!!"); // shouldn't happen
                    loader.incrementEmptyUnitsCount();
                    continue;
                }
                
                // current associated tag:
                PointTag tag = tagMap.get(data[i].geoTag);
                
                // compute location of trace in texture (s,t):
                int s = tag.x - this.xLRef;
                int t = tag.y - this.yLRef;

                
                // upload texture buffer & FESVo:
                // ------------------------------
                
                // if loading features:
                if (loader.loadingFeatures){
                    if (loader.loadAtLowerResolution)
                        loader.uploadTextureBufferAtLowerResolution(s, t, lod, (Feature)data[i], tag.sourceID);
                    else
                        loader.uploadTextureBuffer(s, t, (Feature)data[i], tag.sourceID);
                    
                    FESVo.putHorizon((Feature)data[i], tag, lod);
                    
                    //[TEST]
//                    System.out.println(tag.sourceID + ", " + tag.x + ", " + tag.y + ", " + data[i].geoTag.x + ", " + data[i].geoTag.y);
        
                }
                // if loading a trace:
                else{
                    if (loader.loadAtLowerResolution)
                        loader.uploadTextureBufferAtLowerResolution(s, t, lod, (Trace)data[i]);
                    else
                        loader.uploadTextureBuffer(s, t, (Trace)data[i]);
                    
                    FESVo.putTrace((Trace)data[i], tag, lod);
                }
                
                   
            }
           
        }//runStatement
        
    }// class LoadingThread
    
    // ------------------------------------------------------------------------
    
    // [27/9/2012]
    private static class HorizonInsertionThread
        implements Runnable {
        
        DatabaseConnector db;
        Set<GeoTag> queue;
        int baselineSrcID;
        int userSrcID;
        String timestamp;
        int timeShift;
        
        // constructor
        public HorizonInsertionThread(DatabaseConnector db, Set<GeoTag> queue, int baselineSrcID, int userSrcID, String timestamp, int timeShift) {
            this.db = db;
            this.queue = queue;
            this.baselineSrcID = baselineSrcID;
            this.userSrcID = userSrcID;
            this.timestamp = timestamp;
            this.timeShift = timeShift;
        }
        
        
        
        @Override
        public void run() {
            db.insertHorizon(queue, baselineSrcID, userSrcID, timestamp, timeShift);
        }
        
    }//class HorizonInsertionThread
    
    // ------------------------------------------------------------------------
    
    // [13/6/2012] upload a trace into the texture buffer:
    private synchronized void uploadTextureBuffer(int s, int t, Trace trace){
        
        // reposition texture buffer to (s,t)
        this.traceTexBuffer.position((t * textureDimension[0] * textureDimension[2] * this.srdsSource.sampleSize)
                               +(s * textureDimension[2]*this.srdsSource.sampleSize));
        
        // insert byte array into buffer:
        this.traceTexBuffer.put(trace.getSamples());
    }//uploadTextureBuffer
    
    // ------------------------------------------------------------------------
    
    // [13/6/2012] upload a feature into the texture buffer:
    private synchronized void uploadTextureBuffer(int s, int t, Feature f, int fesvoID){
        
        // get all w (TWT) values
        for (int w : f.getAllWs())
        {
            // map w value to an index:
            int heightIndex = Math.round(
                    ((float)(this.srdsSource.timeMax - (w*-1))
                    / (this.srdsSource.timeMax - this.srdsSource.timeMin))
                    * textureDimension[2]);
            
            int bufIndex = (t*textureDimension[0]*textureDimension[2]
                            + s*textureDimension[2] 
                            + heightIndex) * this.srdsSource.sampleSize;

            // put src id at w height:
            this.featuresTexBuffer.putFloat(bufIndex, fesvoID);

            // update min/max heigt map:
            ArrayList<Integer> h;
            if ((h = featureHeight.get(fesvoID)) != null){
                if (h.get(0).intValue() > heightIndex)
                    h.set(0, heightIndex);
                else if (h.get(1).intValue() < heightIndex)
                    h.set(1, heightIndex);
            }//if
            else{
                h = new ArrayList<Integer>();
                h.add(heightIndex);
                h.add(heightIndex);
                featureHeight.put(fesvoID, h);
            }//else

        }//for
    }//uploadTextureBuffer

    // ------------------------------------------------------------------------
    
    private synchronized void uploadTextureBufferAtLowerResolution(int s, int t, int lod, Trace trace){
        
        int lodDif = lod - finalLOD;
        
        // compute (s,t) at finalLOD dimension:
        int S = s << lodDif;
        int T = t << lodDif;
        
        // insert byte array into buffer:
        // and repeat this to fill the max. dimension texture:
        for (int j=0; j < (1 << lodDif); j++)
        {
            // reposition buffer:
            this.traceTexBuffer.position(
                    ((T+j) * textureDimension[0] * textureDimension[2] * this.srdsSource.sampleSize)
                   +(S * textureDimension[2]*this.srdsSource.sampleSize));
            
            // insert trace in the s direction:
            for (int i=0; i < (1 << lodDif); i++)
                // insert trace
                this.traceTexBuffer.put(trace.getSamples());
            
            // now go to next t value:
        }//for
            
        
    }//uploadTextureBufferAtLowerResolution
    
    // ------------------------------------------------------------------------
    
    private synchronized void uploadTextureBufferAtLowerResolution(int s, int t, int lod, Feature f, int fesvoID){
        
        int lodDif = lod - finalLOD;
        
        // compute (s,t) at finalLOD dimension:
        int S = s << lodDif;
        int T = t << lodDif;
        
        // get all w (TWT) values
        for (int w : f.getAllWs())
        {
            // map w value to an index:
            int heightIndex = Math.round(
                    ((float)(this.srdsSource.timeMax - (w*-1))
                    / (this.srdsSource.timeMax - this.srdsSource.timeMin))
                    * textureDimension[2]);
            
            // insert into buffer:
            // and repeat this to fill the max. dimension texture:
            for (int j=0; j < (1 << lodDif); j++)
            {
                for (int i=0; i < (1 << lodDif); i++)
                {
                    
                    int bufIndex = ((T+j) * textureDimension[0]*textureDimension[2]
                                    + (S+i) * textureDimension[2] 
                                    + heightIndex) * this.srdsSource.sampleSize;

                    // put src id at w height:
                    this.featuresTexBuffer.putFloat(bufIndex, fesvoID);
                }
            }//for
            
            
            // update min/max heigt map:
            ArrayList<Integer> h;
            if ((h = featureHeight.get(fesvoID)) != null){
                if (h.get(0).intValue() > heightIndex)
                    h.set(0, heightIndex);
                else if (h.get(1).intValue() < heightIndex)
                    h.set(1, heightIndex);
            }//if
            else{
                h = new ArrayList<Integer>();
                h.add(heightIndex);
                h.add(heightIndex);
                featureHeight.put(fesvoID, h);
            }//else
            
            
            // go to next w
        }//for
        
    }//uploadTextureBufferAtLowerResolution
    
    // ------------------------------------------------------------------------
    
    private synchronized void incrementDbVisitCount(){
        this.dbVisitCount++;
    }//incrementDbVisitCount
    
    // ------------------------------------------------------------------------
    
    private synchronized void incrementEmptyUnitsCount(){
        this.emptyUnitsCount++;
    }//incrementEmptyUnitsCount
    
    // ------------------------------------------------------------------------
    
    @Override
    public String toString() {
        return "DataLoader_SRDS{" 
                + "\n\tnumOfThreads=" + numOfThreads 
//                + ", \nsrdsSource=" + this.srdsSource.getInfo()
                + ", \n\ttracesSourceID=" + tracesSourceID 
                + ", \n\tmaxTextureSize=" + maxTextureSize 
                + ", \n\tmaxTextureDimension= [" + maxTextureDimension[0] + ", " + maxTextureDimension[1] + ", " + maxTextureDimension[2] + "]" 
                + ", \n\ttextureDimension= [" + textureDimension[0] + ", " + textureDimension[1] + ", " + textureDimension[2] + "]" 
                + ", \n\ttopLOD=" + topLOD 
                + ", \n\tinitialLOD=" + initialLOD 
                + ", \n\tfinalLOD=" + finalLOD 
                + ", \n\tfeatureHeight=" + featureHeight 
                + ", \n\tFESVoPointsCount=" + FESVoPointsCount 
                + ", \n\tdbVisitCount=" + dbVisitCount 
                + ", \n\temptyUnitsCount=" + emptyUnitsCount
                + ", \n\tX-Range= [ " + FESVo.xMin() + " - " + FESVo.xMax() + " ]"
                + ", \n\tY-Range= [ " + FESVo.yMin() + " - " + FESVo.yMax() + " ]"
                + ", \n\tLOD Unit Width=" + FESVo.getLodUnitWidth()
                + ", \n\tLOD Unit Depth=" + FESVo.getLodUnitDepth() + "}";
    }

    
    // ------------------------------------------------------------------------
    
    // [21/6/2012]
    // Loads data coordinates for the given data id (FESVo id)
    private void loadDataCoordinates(int id) {
        
        // first, try to read coordinates from file:
        if (this.loadDataCoordinatesFromFileCache(id)){
            // exit method:
            return;
        }//if
        
        
        // get coordinates from DB:
        //-------------------------
        
        // [TEST]
        System.out.println("Reading trace data coordinates from SRDS ...");
        
        Point [] points;
        
        // if id type is a trace
        if (FESVo.getDataType(id) == DataType.TRACE){
            points = dbConnection[0].getTraceDataCoordinates(this.srdsSource.srdsSrcID);
        }
        // if id type is a horizon:
        else if (FESVo.getDataType(id) == DataType.HORIZON){
            points = dbConnection[0].getHorizonDataCoordinates(this.srdsSource.srdsSrcID, this.horizonsSourceIDs.get(id));
        }
        // otherwise, it's an error:
        else{
            System.err.println("ID is not of type TRACE or HORIZON!!");
            return;
        }
        
        // [TEST]
        System.out.println("Loading trace data coordinates into FESVo ...");
        
        // insert points into FESVo:
        for (Point point : points){
            FESVo.addDataCoordinate(point.x, point.y, id);
        }//for
        
        // cache these coordinates:
        this.cacheDataCoordinates(points, id);
        
        // [TEST]
        System.out.println("Done!\n");
        
    }//loadTraceDataCoordinate
    
    
    // reads data coordinate of a given source id from file and loads it into FESVo
    // return false if no file cache exists, true otherwise:
    // [26/4/2012]: was public!! changed it to private
    // [30/4/2012]: removed parameter; no need
    // [22/6/2012]: expanded to cover features too
    private boolean loadDataCoordinatesFromFileCache(int id)
    {
        try {
            // file name:
            String fName = DATA_CACHE_FOLDER + "/DataCoordinate_" + FESVo.getDataType(id).name() + "_ID_" + this.srdsSource.srdsSrcID;
            
            // file object:
            File file;
            
            // if id type is a trace
            if (FESVo.getDataType(id) == DataType.TRACE){
                // create a file object:
                file = new File(fName + ".FESVo");
            }
            // if id type is a horizon:
            else if (FESVo.getDataType(id) == DataType.HORIZON){
                // create a file object:
                file = new File(fName + "_prop_" + this.horizonsSourceIDs.get(id) + ".FESVo");
            }
            // otherwise, it's an error:
            else{
                System.err.println("ID is not of type TRACE or HORIZON!!");
                System.exit(0); // exit 
                return false;
            }
            
            
            // check if file exists:
            if (!file.exists()) return false;
            
            // create a buffer reader for the feature file:
            BufferedReader in = new BufferedReader(new FileReader (file));
            
            // current line:
            String line;
            
            // index:
            int i = 0;
            
            // read till end of file:
            while((line = in.readLine()) != null)
            {
                // split the line around white spaces:
                String [] subLine = line.split(":");
                
                // convert each subString into int to get x, y
                // round float values to closest int:
                int x = Math.round(Float.parseFloat(subLine[0]));
                int y = Math.round(Float.parseFloat(subLine[1]));
                
                // insert points into FESVo:
                FESVo.addDataCoordinate(x, y, id);
                
                i++;
                
            }//while
            
            // [TEST]
            String s = (i + " data coordinates of type " + FESVo.getDataType(id).name()
                    + ", id " + id
                    + ", SRDS src_id " + this.srdsSource.srdsSrcID);
            
            if (FESVo.getDataType(id) == DataType.HORIZON)
                s += ", prop_val " + this.horizonsSourceIDs.get(id);
            
            s+= " were load from cache file into FESVo.\n";
            
            System.out.println(s);
            
            // close:
            in.close();
            
            // return true indicating a successful loading:
            return true;
            
        }//try
        
        catch (IOException ex) {
            System.err.println(ex);
        }
        
        return false;
        
    }//loadDataCoordinateFromFileCache
    
    private void cacheDataCoordinates(Point [] points, int id){
        
        PrintStream out = null;
        
        // file name:
        String fName = DATA_CACHE_FOLDER + "/DataCoordinate_" + FESVo.getDataType(id).name() + "_ID_" + this.srdsSource.srdsSrcID;

        // if id type is a trace
        if (FESVo.getDataType(id) == DataType.TRACE){
            fName += ".FESVo";
        }
        // if id type is a horizon:
        else if (FESVo.getDataType(id) == DataType.HORIZON){
            fName += ("_prop_" + this.horizonsSourceIDs.get(id) + ".FESVo");
        }
        // otherwise, it's an error:
        else{
            System.err.println("ID is not of type TRACE or HORIZON!!");
            System.exit(0); // exit 
            return;
        }
        
        try {
            // get a print stream with right file:
            out = new PrintStream(fName);
            
            // write coordinate points:
            for (Point point : points){
                out.println(point.x + ":" + point.y);
            }//for
            
            // [TEST]
            System.out.println("Data coordinates of source id " + id + " were cached into file.\n");
        
        }//try
        
        catch (FileNotFoundException ex) {
            Logger.getLogger(DataLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        finally{
            // close the stream:
            if (out != null) out.close();
        }
        
    }//cacheDataCoordinates
    
    // ------------------------------------------------------------------------
    
    // [25/9/2012]
    private void preLoadFeaturesIntoFESVo()
    {
        Feature [] features;
        
        // set of prop_val of required features:
        Set<Integer> propVals = new HashSet<Integer>();
        for (int id : this.requiredIDs){
            propVals.add(this.horizonsSourceIDs.get(id));
        }
        
        // [9/10/2012] check if only require to load the selected TS:
        if (this.loadOnlySelectedTimestamp){
            features = this.dbConnection[0].getAllHorizonsOnlyAtTS(
                    this.srdsSource.srdsSrcID, this.featuresTimestamp, propVals);
        }
        
        else{
            // get all horizons from DB till selected TS:
            features = this.dbConnection[0].getAllHorizons(
                    this.srdsSource.srdsSrcID, this.featuresTimestamp, propVals);
        }
        
        // loop through features:
        for (Feature f : features)
        {
            // calculate local point at LOD0 for this feature:
            Point p = FESVo.mapToLocalPoint(f.geoTag.getPoint());
            
            
            // point-tag id (FESVo ID):
            // ------------------------
            int pointTagID = -1; // -1 => not found yet
            
            // get all horizons FESVo IDs
            Set<Integer> fesvoIDs = this.horizonsSourceIDs.keySet();
            
            // go through each FESVo ID to check if it's mapped to the feature's ID (prop_val)
            for (int id : fesvoIDs){
                if (this.horizonsSourceIDs.get(id).equals(f.geoTag.sourceID)){
                    pointTagID = id;
                    break;
                }
            }
            
            // if point tag id was not found, then it's an error:
            if (pointTagID == -1){
                System.err.println("Point Tag ID was not found!!");
                this.closeConnections();
                System.exit(0);
            }
            
            // load feature into FESVo:
            FESVo.putHorizon(f, new PointTag(p, pointTagID), 0);
            
        }//for
        
        this.featuresLoadedIntoFESVo = true;
    }//loadFeaturesIntoFESVo()
    
    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        DataLoader loader = new DataLoader(1000 * 1000 * 10); // 10 MB
        
//        // LOD 4
//        loader.buildTracesTexture(0,0,4);
//        System.out.println(loader.toString());
//        
//        // LOD 3
//        loader.buildTracesTexture(0,0,3);
//        System.out.println(loader.toString());
        
        // LOD 5 : on max dimension
        loader.buildTracesTextureAtLowerResolution(0, 0, 5);
        System.out.println(loader.toString());
        
        loader.closeConnections();
        
    
        int i=0;
    }
    
}
