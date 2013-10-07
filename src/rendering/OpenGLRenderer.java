/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rendering;

import utilities.RawVolumeReader;
import java.awt.event.*;
import javax.swing.*;

import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.opengl.awt.*;
import com.jogamp.opengl.util.*;

import seismic.DataType;
import seismic.FESVo;
import utilities.*;

/**
 *
 * @author Aqeel
 */
public class OpenGLRenderer implements GLEventListener, KeyListener{

    private GLU glu;
    //private GLUT glut; // added on 4/10/2011
    
    private float zoom = -10.0f;    // Depth Into The Screen
    private float xrot = 0.0f;      // X Rotation
    private float yrot = 0.0f;      // Y Rotation

    // an alpha value, can be changed by user, for seismic texture transparency
    private float seismicAlpha = 1.0f;  // added on [21/11/2011]

    //------------------------------------------------------------------------

    // Variables for Shaders: (added on 18/10/2011)
    private int shaderProgram;

    //------------------------------------------------------------------------

    // Variables for texture objects name: (added on 22/10/2011)
    private int seismicTexture, featuresTexture;

    //------------------------------------------------------------------------

    // changed from static final: [9/12/2011]
    
    private float min_amplitude;
    private float max_amplitude;

    
    private int seismicVolumeWidth;
    private int seismicVolumeHeight;
    private int seismicVolumeDepth;

    // Note: changed this buffer type from FloatBuffer to Buffer to be more general,
    // Buffer is accepted in glTexImage method
    private Buffer seismicVolumeBuffer, featuresVolumeBuffer;
    
    //------------------------------------------------------------------------
    
    // depth of the original 3D texture - [21/11/2011]
    private int textureDepth;
    
    // width of the 3D texture - [7/10/2012]
    private int textureWidth;
    
    // height of the 3D texture - [17/10/2012]
    private int textureHeight;
    
    // factor to multiply volume dimension to have texture dimension - [7/10/2012]
    private final int volumeTextureSliceFactor = 3;
    

    // dpeth of the seismic texture only as required by user - [21/11/2011]
    private int seismicViewDepth;

    // a factor for hiding/showing slices controled by user - [21/11/2011]
    private float hideShowFactor = 0.05f; //

    //------------------------------------------------------------------------
    
    // added on 9/12/2011
    private DataLoader loader;
    
    // current LOD: [20/2/2012]
    // this value must only be changed using the method moveLOD() 
    // to correctly modify the reference point:
    private int lod;
    
    // current local reference point on the current LOD: [20/2/2012]
    // these values must not be changed manually,
    // only through calling moveLOD() method:
    private int xLRef, yLRef;
    
    // [TEST]
    private boolean loadNextLOD = false;
    private boolean enhanceResolution = false;
    
    // [TEST] [19/3/2012]
    private boolean forwardSlice = false;
    private boolean backSlice = false;
    
    // [4/5/2012]
    // the current visualization mode: slice based (2D) or volume (3D)
    private boolean sliceVisualization = false;
    
    // [7/5/2012]
    // an indication that visualization mode was changed by user from GUI:
    // to reload textures:
    private boolean visModeChaged = false;
    
    // [8/5/2012]
    // the initial LOD above Top LOD
    private int initLodAboveTopLod;
    
    // [10/7/2012]
    // an indication that visualization location was changed by user from GUI:
    // to reload textures:
    private boolean visLocationChaged = false;
    
    // [11/9/2012]
    // to reload and render horizon points
    private boolean reloadHorizons = false;
    
    //------------------------------------------------------------------------
    
    // constructor: added on 9/12/2011
    public OpenGLRenderer(DataLoader loader) {
        this(loader, false);
    }
    
    // constructor: added on 4/5/2012
    public OpenGLRenderer(DataLoader loader, boolean sliceVis) {
        this(loader, sliceVis, 3); // default value of 3 for initLodAboveTopLod
    }
    
    // constructor: added on 27/5/2012
    public OpenGLRenderer(DataLoader loader, boolean sliceVis, int initLodAboveTopLod) {
        
        // set loader:
        this.loader = loader;
        
        // set initial LOD above top LOD:
        this.initLodAboveTopLod = initLodAboveTopLod;
        
        // call a helper method to set visualization mode:
        setVisualizationMode_helper(sliceVis);
        
        // set current reference point for X:
        this.xLRef = 0;//270;
        
        
        // set initial max and min amplitudes
        this.min_amplitude = loader.getSrdsSource().amplitudeMin;
        this.max_amplitude = loader.getSrdsSource().amplitudeMax;
        
    }

    private void setVisualizationMode_helper(boolean sliceVis)
    {
        // set visualization mode:
        this.sliceVisualization = sliceVis;
        
        if (sliceVisualization){
            this.lod = 0; // [TEMP], later compute exactly a top lod for one slice
            this.yLRef = loader.getLod0Dimension()[1]/2;// [TEMP] start with a slice in the middle
        }
        else{
            // set current LOD:
            this.lod = loader.getTopLOD() + this.initLodAboveTopLod; 
            this.yLRef = 0;
        }
    }

    //------------------------------------------------------------------------
    
    public float getMax_amplitude() {
        return max_amplitude;
    }

    public float getMin_amplitude() {
        return min_amplitude;
    }

    public void setMax_amplitude(float max_amplitude) {
        this.max_amplitude = max_amplitude;
    }

    public void setMin_amplitude(float min_amplitude) {
        this.min_amplitude = min_amplitude;
    }
    
    //------------------------------------------------------------------------

    public void setVisualizationMode(boolean sliceVisualization) {
        // exit if we have the same current mode:
        if (this.sliceVisualization == sliceVisualization)
            return;
        
        // otherwise:
        setVisualizationMode_helper(sliceVisualization);
        
        this.visModeChaged = true;
    }
    
    //------------------------------------------------------------------------

    // [11/9/2012]
    public void reloadHorizons() {
        this.reloadHorizons = true;
    }//reloadHorizons
    
    //------------------------------------------------------------------------

    /**
     * changes the visualization location given a new reference point (relative to LOD0) and new LOD
     * @param lod0Ref a new reference point relative to LOD0
     * @param lod a new level-of-details (LOD)
     */
    public void setVisLocation(Point lod0Ref, int lod) {
        // calculate a ref point relatively to the requested LOD:
        Point ref = FESVo.mapPoint(lod0Ref, 0, lod);
        
        // exit if we already have the same values (no change):
        if (this.lod == lod){
            if (this.xLRef == ref.x && this.yLRef == ref.y)
                return; //exit
        }
        
        // otherwise:
        this.lod = lod;
        this.xLRef = ref.x;
        this.yLRef = ref.y;
        
        this.visLocationChaged = true;
    }//setVisLocation
    
    //------------------------------------------------------------------------
    
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();

        // Set up shader:
        setupShader(gl);

        //glut = new GLUT(); // added on 4/10/2011

        // Black Background:
        //gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // White Background:
        gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);

        // load seismic and features volume:
        makeSeismicVolume();
        makeFeaturesVolume(); //(added: 21/11/2011)
        
        // initialize textures:
        initializeTextures(gl);
        
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
        
        gl.glShadeModel(GL2.GL_FLAT);

        // enable blending:
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc (GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
    }

    //----------------------------------------------------------------------

    public void display(GLAutoDrawable drawable) {
        
        GL2 gl = drawable.getGL().getGL2();
        
        // check user's requests:
//        if (loadNextLOD) loadNextLOD(gl);
        if (enhanceResolution) enhanceResolution(gl);
        if (sliceVisualization){
            if (backSlice) backSlice(gl);
            if (forwardSlice) forwardSlice(gl);
        }
        if (visModeChaged){
            changeVisMode(gl);
        }
        if (visLocationChaged){
            changeVisLocation(gl);
        }
        
        if (reloadHorizons){
            reloadHorizon(gl);
        }
        
        gl.glLoadIdentity();  // Reset The View
        gl.glTranslatef(0.0f, 0.0f, zoom);

        gl.glRotatef(xrot, 1.0f, 0.0f, 0.0f);
        gl.glRotatef(yrot, 0.0f, 1.0f, 0.0f);
        
        // assign texels from the texture map into vertices:

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // use shader program:
        gl.glUseProgram(shaderProgram);

        
        // ------------------------------------------
        // [28/10/2012] Updated to use ShaderCreator
        // ------------------------------------------
        
        
        // get sampler uniform from shader:
        int seismicSampler = gl.glGetUniformLocation(shaderProgram, ShaderCreator.TRACES_SAMPLER);// "seismicTexture");
        int featuresSampler = gl.glGetUniformLocation(shaderProgram, ShaderCreator.FEATURES_SAMPLER);// "featuresTexture"); // [21/11/2011]

        // set texture unit index for these sampler:
        gl.glUniform1i(seismicSampler, 0); //GL_TEXTURE0
        gl.glUniform1i(featuresSampler, 1); //GL_TEXTURE1

        // get max and min amplitude uniforms from shader:
        int min_uniform = gl.glGetUniformLocation(shaderProgram, ShaderCreator.MIN_AMPLITUDE);// "min_amplitude");
        int max_uniform = gl.glGetUniformLocation(shaderProgram, ShaderCreator.MAX_AMPLITUDE);// "max_amplitude");

        // a uniform for alpha value for the seismic texture transparency: [21/11/2011]
        int alpha_uniform = gl.glGetUniformLocation(shaderProgram, ShaderCreator.ALPHA);// "alpha");
        
        //  a uniform for seismic texture depth: [21/11/2011]
        int seismicTexDepth_uniform = gl.glGetUniformLocation(shaderProgram, ShaderCreator.SEISMIC_TEX_DEPTH);// "seismicTexDepth");

        
        //  a uniform for feature src id and min/max heights: [14/12/2011]
        // [3/10/2012] changed to an array of features IDs
        int [] featureIDs = this.loader.getFeaturesIDs();
        
        int [] feature_uniform = new int[featureIDs.length];
        for (int i=0; i < featureIDs.length; i++){
            feature_uniform[i] = gl.glGetUniformLocation(shaderProgram, (ShaderCreator.FEATURE_NO_ + (i)));
        }
        
        // set the max and min amplitude values in shader to perform normalization:
        gl.glUniform1f(min_uniform, min_amplitude);
        gl.glUniform1f(max_uniform, max_amplitude);

        // set required alpha: [21/11/2011]
        gl.glUniform1f(alpha_uniform, seismicAlpha);

        // set required seismic texture depth: [21/11/2011]
        gl.glUniform1f(seismicTexDepth_uniform, (seismicViewDepth / (float)textureDepth));

        
        // set required features data: [14/12/2011]
        //int [] src = FESVo.getSourceIDs(DataType.HORIZON);
        // [3/10/2012] changed to support an array of features IDs and feature uniforms
        for (int i=0; i < featureIDs.length; i++){
            gl.glUniform3f(feature_uniform[i], (float)featureIDs[i], 
                ((float)(loader.getMinMaxHeight(featureIDs[i])[0])) / (float)this.seismicVolumeHeight, 
                ((float)(loader.getMinMaxHeight(featureIDs[i])[1])) / (float)this.seismicVolumeHeight);
        }
        
        
        // render slices based on rotation angle:
        // check the vertical angle first:
        double xAng = Utilities.getStandardAngle(xrot);
        double yAng = Utilities.getStandardAngle(yrot);
        
        if (xAng > 50 & xAng < 130)
            renderSlicesDownToTop(gl);
        
        else if (xAng > 230 & xAng < 310)
            renderSlicesTopToDown(gl);
        
        else if (yAng <= 45 || yAng > 315)
            renderSlicesAt0(gl);
        
        else if (yAng <= 135)
            renderSlicesAt90(gl);
        
        else if (yAng <= 225)
            renderSlicesAt180(gl);
        
        else if (yAng <= 315)
            renderSlicesAt270(gl);

        // stop using the shader program:
        gl.glUseProgram(0);

        gl.glFlush();
        
    }//display
    
    // =======================================================================
    
    // [7/10/2012]
    // Rendering slices at variouse angles based on horizontal rotation angle:
    // -----------------------------------------------------------------------

    private void renderSlicesAt0(GL2 gl)
    {
        for (int i=0; i < textureDepth; i++)
        {
            float r = (float)(0.5 + i) / (float)textureDepth;
            float texR = 1 - r; // to start from r 1 to 0
            float vertZ = r * 4 - 2.0f; // start from -2.0
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, 0.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, 0.0f, texR);
                gl.glVertex3f(-2.0f, 2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, 1.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, 1.0f, texR);
                gl.glVertex3f(2.0f, 2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, 1.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, 1.0f, texR);
                gl.glVertex3f(2.0f, -2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, 0.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, 0.0f, texR);
                gl.glVertex3f(-2.0f, -2.0f, vertZ);
            gl.glEnd();
        }//for
    }//renderSlicesAt0
    
    private void renderSlicesAt90(GL2 gl)
    {
        for (int i=0; i < textureWidth; i++)
        {
            float t = (float)(0.5 + i) / (float)textureWidth;
            float texT = 1 - t; // (start from 1)
            float vertX = 2.0f - t * 4; // (start from 2.0)
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, texT, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, texT, 0.0f);
                gl.glVertex3f(vertX, 2.0f, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, texT, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, texT, 1.0f);
                gl.glVertex3f(vertX, 2.0f, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, texT, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, texT, 1.0f);
                gl.glVertex3f(vertX, -2.0f, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, texT, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, texT, 0.0f);
                gl.glVertex3f(vertX, -2.0f, 2.0f);
            gl.glEnd();
        }//for
    }//renderSlicesAt90
    
    private void renderSlicesAt180(GL2 gl)
    {
        for (int i=0; i < textureDepth; i++)
        {
            float r = (float)(0.5 + i) / (float)textureDepth;
            float texR = r; // start from s 0 to 1
            float vertZ = 2.0f - (r * 4); // from front to back rendering (start from 2.0)
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, 0.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, 0.0f, texR);
                gl.glVertex3f(-2.0f, 2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, 1.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, 1.0f, texR);
                gl.glVertex3f(2.0f, 2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, 1.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, 1.0f, texR);
                gl.glVertex3f(2.0f, -2.0f, vertZ);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, 0.0f, texR);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, 0.0f, texR);
                gl.glVertex3f(-2.0f, -2.0f, vertZ);
            gl.glEnd();
        }//for
    }//renderSlicesAt180
    
    private void renderSlicesAt270(GL2 gl)
    {
        for (int i=0; i < textureWidth; i++)
        {
            float t = (float)(0.5 + i) / (float)textureWidth;
            float texT = t; // (start from 0)
            float vertX = t * 4 - 2.0f; // (start from -2.0)
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, texT, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, texT, 0.0f);
                gl.glVertex3f(vertX, 2.0f, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 0.0f, texT, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 0.0f, texT, 1.0f);
                gl.glVertex3f(vertX, 2.0f, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, texT, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, texT, 1.0f);
                gl.glVertex3f(vertX, -2.0f, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, 1.0f, texT, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, 1.0f, texT, 0.0f);
                gl.glVertex3f(vertX, -2.0f, 2.0f);
            gl.glEnd();
        }//for
    }//renderSlicesAt270
    
    // [17/10/2012]
    // Rendering slices at variouse angles based on vertical rotation angle:
    // ---------------------------------------------------------------------
    
    private void renderSlicesDownToTop(GL2 gl)
    {
        for (int i=0; i < textureHeight; i++)
        {
            float s = (float)(0.5 + i) / (float)textureHeight;
            float texS = 1 - s; // to start from s 1 to 0
            float vertY = s * 4 - 2.0f; // (start from -2.0)
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 0.0f, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 0.0f, 0.0f);
                gl.glVertex3f(-2.0f, vertY, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 1.0f, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 1.0f, 0.0f);
                gl.glVertex3f(2.0f, vertY, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 1.0f, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 1.0f, 1.0f);
                gl.glVertex3f(2.0f, vertY, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 0.0f, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 0.0f, 1.0f);
                gl.glVertex3f(-2.0f, vertY, -2.0f);
            gl.glEnd();
        }//for
    }//renderSlicesDownToTop
    
    private void renderSlicesTopToDown(GL2 gl)
    {
        for (int i=0; i < textureHeight; i++)
        {
            float s = (float)(0.5 + i) / (float)textureHeight;
            float texS = s; // to start from s 0 to 1
            float vertY = 2.0f - (s * 4); // (start from +2.0)
            
            gl.glBegin(GL2.GL_QUADS);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 0.0f, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 0.0f, 0.0f);
                gl.glVertex3f(-2.0f, vertY, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 1.0f, 0.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 1.0f, 0.0f);
                gl.glVertex3f(2.0f, vertY, 2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 1.0f, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 1.0f, 1.0f);
                gl.glVertex3f(2.0f, vertY, -2.0f);

                gl.glMultiTexCoord3f(GL.GL_TEXTURE0, texS, 0.0f, 1.0f);
                gl.glMultiTexCoord3f(GL.GL_TEXTURE1, texS, 0.0f, 1.0f);
                gl.glVertex3f(-2.0f, vertY, -2.0f);
            gl.glEnd();
        }//for
    }//renderSlicesTopToDown
    
    // =======================================================================
    
    private void loadNextLOD(GL2 gl) {
        // [TEST] move LOD down till reaching topLOD
        if (this.lod > loader.getTopLOD())
        {
            // move LOD down
            this.moveLOD(lod-1);
            
            // reload textures:
            this.makeSeismicVolume();
            this.makeFeaturesVolume();
            
            // reinitialize textures:
            this.initializeTextures(gl);
        }//if
        
        loadNextLOD = false;
    }
    
    private void enhanceResolution(GL2 gl){
        
        // reload textures:
        seismicVolumeBuffer = loader.enhanceTracesTextureResolution(0);
            
        // reinitialize textures:
        this.initializeTextures(gl);
        
        enhanceResolution = false;
    }
    
    private void backSlice (GL2 gl){
        if (yLRef < 1500)
        {
            yLRef += 20;
            // reload textures:
            seismicVolumeBuffer = this.loader.buildSingleSliceTracesTexture(xLRef, yLRef, 0);

            // reinitialize textures:
            this.initializeTextures(gl);
            
            backSlice = false;
        }
    }
    
    private void forwardSlice (GL2 gl){
        if (yLRef > 1)
        {
            yLRef -= 20;
            // reload textures:
            seismicVolumeBuffer = this.loader.buildSingleSliceTracesTexture(xLRef, yLRef, 0);

            // reinitialize textures:
            this.initializeTextures(gl);
            
            forwardSlice = false;
        }
    }

    //----------------------------------------------------------------------

    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        GL2 gl = drawable.getGL().getGL2();
        //
        gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        glu.gluPerspective(60.0, 1.0 * (float) w / (float) h, 1.0, 30.0);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glTranslatef(0.0f, 0.0f, -3.6f);
    }

    //----------------------------------------------------------------------

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
            boolean deviceChanged) {
    }

    //----------------------------------------------------------------------

    public void dispose(GLAutoDrawable drawable) {
//        throw new UnsupportedOperationException("Not supported yet.");
        this.loader.closeConnections();
    }

    //----------------------------------------------------------------------

    public void keyTyped(KeyEvent key) {
        
        // for testing, print the pressed key:
        //System.out.println("Key pressed: " + key.getKeyCode() + " : " + KeyEvent.getKeyText(key.getKeyCode()));

        switch (key.getKeyChar()) {
            case '+':
                zoomIn();
                break;

            case '-':
                zoomOut();
                break;

            default:
                break;
        }//switch

    }//keyTyped()

    //----------------------------------------------------------------------

    public void keyPressed(KeyEvent key) {
        
        // for testing, print the pressed key:
        
        switch (key.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                runExit();
                break;

            case KeyEvent.VK_RIGHT:
            //case KeyEvent.VK_R:
                rotateRight();
                break;

            case KeyEvent.VK_LEFT:
            //case KeyEvent.VK_L:
                rotateLeft();
                break;

            case KeyEvent.VK_UP:
            //case KeyEvent.VK_U:
                rotateUp();
                break;

            case KeyEvent.VK_DOWN:
            //case KeyEvent.VK_D:
                rotateDown();
                break;

            case KeyEvent.VK_A:
                increaseTransparency();
                break;

            case KeyEvent.VK_S:
                decreaseTransparency();
                break;

            case 44: // '<'
                hideSeismicSlice();
                break;

            case 46: // '>'
                showSeismicSlice();
                break;
                
            case KeyEvent.VK_L:
                loadNextLOD = true;
                break;

            case KeyEvent.VK_E:
                enhanceResolution = true;
                break;
                
            case KeyEvent.VK_1:
                forwardSlice = true;
                break;
            
            case KeyEvent.VK_2:
                backSlice = true;
                break;
                
            default:
                break;
        }//switch
    }

    //----------------------------------------------------------------------

    public void keyReleased(KeyEvent key) {
        //throw new UnsupportedOperationException("Not supported yet.");
        
    }//keyReleased()

    //=======================================================================

    private void runExit() {
        new Thread(new Runnable() {
            public void run() {
                // close connections of DB [14/2/2012]:
                loader.closeConnections();
                
                //if (animator != null) animator.stop();
                System.exit(0);
            }
        }).start();

    }//runExit()
    
    //----------------------------------------------------------------------
    
    private void zoomIn() {
        if (zoom < 0.0){
            zoom += 0.5f;
            System.out.println("Zooming in ...");
	}
    }//zoomIn()

    //----------------------------------------------------------------------

    private void zoomOut() {
        if (zoom > -30.0){
            zoom -= 0.5f;
            System.out.println("Zooming out ...");
	}
    }//zoomOut()

    //----------------------------------------------------------------------

    private void rotateRight() {
        yrot -= 1.0f;
        System.out.println("Rotating right ... [" + yrot + "]");
    }//rotateRight()

    //----------------------------------------------------------------------

    private void rotateLeft() {
        yrot += 1.0f;
        System.out.println("Rotating left ... [" + yrot + "]");
    }//rotateLeft()

    //----------------------------------------------------------------------

    private void rotateUp() {
        xrot -= 1.0f;
        System.out.println("Rotating upward ...[" + xrot + "]");
    }//rotateUp()

    //----------------------------------------------------------------------

    private void rotateDown() {
        xrot += 1.0f;
        System.out.println("Rotating downward ...[" + xrot + "]");
    }//rotateDown()

    //----------------------------------------------------------------------

    private void increaseTransparency() {
        if (seismicAlpha > 0.0f){
            seismicAlpha -= 0.05f;
            System.out.println("alpha = " + seismicAlpha);
	}
    }//increaseTransparency()

    //----------------------------------------------------------------------

    private void decreaseTransparency() {
        if (seismicAlpha < 1.0f){
            seismicAlpha += 0.05f;
            System.out.println("alpha = " + seismicAlpha);
	}
    }//decreaseTransparency()

    //----------------------------------------------------------------------

    private void hideSeismicSlice() {
        if (seismicViewDepth > 0 ){
            seismicViewDepth -= (int)(textureDepth * hideShowFactor);
            System.out.println("seismicViewDepth = " + seismicViewDepth);
	}
    }//hideSeismicSlice()

    //----------------------------------------------------------------------

    private void showSeismicSlice() {
        if (seismicViewDepth < textureDepth){
            seismicViewDepth += (int)(textureDepth * hideShowFactor);
            System.out.println("seismicViewDepth = " + seismicViewDepth);
	}
    }//showSeismicSlice()

    //----------------------------------------------------------------------
    
    // change the LOD and map the local reference point to the new LOD:
    private void moveLOD(int newLOD)
    {
        // if newLOD = current LOD, do nothing:
        if (this.lod == newLOD)
            return;
        
        int [] refPoint = FESVo.mapPoint(xLRef, yLRef, lod, newLOD);
        
        this.xLRef = refPoint[0];
        this.yLRef = refPoint[1];
        
        // set new LOD:
        this.lod = newLOD;
        
    }//moveLOD
    
    //----------------------------------------------------------------------
    
    // [20/2/2012] initialize textures
    // this is called when requesting a new LOD or move area of view:
    private void initializeTextures(GL2 gl)
    {
        // [TEST] measure execution time:
        long startTime = System.currentTimeMillis();
        
        // set up the textures and get their names (id): (modified: 21/11/2011)
        seismicTexture = setupTexture(gl, seismicVolumeBuffer, false);
        featuresTexture = setupTexture(gl, featuresVolumeBuffer, false);

        // define two texture units using the two texture objects:
        // Seismic Texture:
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, seismicTexture);
        // Features Texture:
        gl.glActiveTexture(GL.GL_TEXTURE1);
        gl.glEnable(GL2.GL_TEXTURE_3D);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, featuresTexture);
        
        // [TEST]
        long endTime = System.currentTimeMillis();
        System.out.println("\tTextures were re-initialized in " + (endTime - startTime) + " ms.\n");
        
        
    }//initializeTextures
    
    //----------------------------------------------------------------------

    // modified on [20/2/2012]
    // loads traces from SRDS using DataLoader
    // on the current LOD and reference local (x, y) point
    private void makeSeismicVolume()
    {
        System.out.println("Loading a volume data into the buffer ...");

        // [TEST] measure execution time:
        long startTime = System.currentTimeMillis();

        // build buffer from loader
//        seismicVolumeBuffer = this.loader.buildTracesTexture(xLRef, yLRef, lod);
        
        // [7/5/2012] depending on visualization mode (volume or slice), load buffer:
        if (this.sliceVisualization)
            seismicVolumeBuffer = this.loader.buildSingleSliceTracesTexture(xLRef, yLRef, lod);
        else
            seismicVolumeBuffer = this.loader.buildTracesTextureAtLowerResolution(xLRef, yLRef, lod); // [22/2/2012]
        
        // get dimension of texture
        int [] dimension = this.loader.getTextureDimension();
        setTextureDimension(dimension);


        // [TEST]
        long endTime = System.currentTimeMillis();
        System.out.println("Volume data, of size " + seismicVolumeBuffer.capacity() + " bytes, was successfully loaded into the buffer, in "
                + (endTime - startTime) + " ms.\n");

        
        // rewind the buffer:
        seismicVolumeBuffer.rewind();

    }

    //----------------------------------------------------------------------

    // modified on [20/2/2012]
    // loads features from SRDS using DataLoader
    // on a given LOD and reference local (x, y) point
    private void makeFeaturesVolume()
    {
        System.out.println("Loading a features into the buffer ...");

        // [TEST] measure execution time:
        long startTime = System.currentTimeMillis();
        
        
        // Using DataLoader: [11/12/2011]
//        featuresVolumeBuffer = this.loader.buildFeaturesTexture();
        featuresVolumeBuffer = this.loader.buildFeaturesTextureAtLowerResolution(xLRef, yLRef, lod); //[22/2/2012]
//        featuresVolumeBuffer = this.loader.buildFeaturesTexture(0,0,0);

        
        long endTime = System.currentTimeMillis();

        System.out.println("Volume data, of size " + featuresVolumeBuffer.capacity() + " bytes, was successfully loaded into the buffer, in "
                + (endTime - startTime) + " ms.\n");

    }//makeFeaturesVolume

    //----------------------------------------------------------------------
    
    // set texture dimension and depending values: [20/2/2012]
    private void setTextureDimension(int[] dimension) {
        // set the texture dimension:
        this.seismicVolumeWidth = dimension[0];
        this.seismicVolumeDepth = dimension[1];
        this.seismicVolumeHeight = dimension[2];
        
        // set texture depth:
        this.textureDepth = this.seismicVolumeDepth * this.volumeTextureSliceFactor;
        
        // set texture width [7/10/2012]:
        this.textureWidth = this.seismicVolumeWidth * this.volumeTextureSliceFactor;
        
        // set texture height [17/10/2012]:
        this.textureHeight = this.seismicVolumeHeight;


        // set initial depth of the seismic texture only as required by user
        this.seismicViewDepth = this.textureDepth;
    }//setTextureDimension

    //----------------------------------------------------------------------

    // setup 3D texture and returns its name (id):
    // passing the buffer from which texture is created (modified: 21/11/2011):
    // passing the format size: e.g. 1 byte for features, 4 bytes for seismic traces [11/12/2011]:
    private int setupTexture(GL2 gl, Buffer buffer, boolean featureBuffer) {
        // generate a texture object name:
        int [] textureID = new int[1];
        gl.glGenTextures(1, textureID, 0);

        // Create and bind the above texture name:
        gl.glBindTexture(GL2.GL_TEXTURE_3D, textureID[0]);

        // default values:
        int internalFormat = GL2.GL_LUMINANCE32F;
        int format = GL2.GL_LUMINANCE;
        int type = GL2.GL_FLOAT;
            
        // change these if required feature buffer
        if (featureBuffer){
            internalFormat = GL2.GL_R32I; //GL2.GL_R;//
            format = GL2.GL_RED;
            type =  GL2.GL_INT; //GL2.GL_UNSIGNED_BYTE;
            
        }//if
        
        
        // [TEST] measure execution time:
        long startTime = System.currentTimeMillis();
        
        // due to the way seismic trace data is store, which is different from an ordinary image,
        // we swap height and width of the seismic slice
        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, internalFormat, 
                seismicVolumeHeight, seismicVolumeWidth, seismicVolumeDepth, 
                0, format, type, buffer);
            
        
        // [TEST]
        long endTime = System.currentTimeMillis();
        System.out.println("\tTexture (ID: " + textureID[0] + ") was loaded to GPU in " + (endTime - startTime) + " ms.\n");
        
        
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL.GL_REPEAT); // [12/12/2011]
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameterf(GL2.GL_TEXTURE_3D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        //gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_DECAL);
        gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        
        // trying blending [9/12/2011]
        //gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_BLEND);
        
        return textureID[0];
    }//setupTexture

    //----------------------------------------------------------------------

    // set up shader
    private void setupShader(GL2 gl)
    {
        // check if vertex and fragment shaders are supported:
        if (!gl.isExtensionAvailable("GL_ARB_vertex_shader")
                || !gl.isExtensionAvailable("GL_ARB_fragment_shader"))
        {
            System.err.println("Error: vertex shader and/or fragment shader is not supported!");
            runExit();
        }//if

        // create a shader program object from gl
        shaderProgram = gl.glCreateProgram();

        // verify if shader program object was created successfully:
        if (shaderProgram == 0){
            System.err.println("Error: program object was not created successfully!");
        }//if

        // create a fragment shader:
        // [24/10/2012] generate shader code on-the-fly:
        // ---------------------------------------------
        
        // get features IDs:
        int [] featureIDs = this.loader.getFeaturesIDs();
        
        // create a ShaderCreator object passing features info:
        ShaderCreator sh = new ShaderCreator(featureIDs);// featuresInfo);
        
        // now create a feag. shader program based on run-time generated code:
        int fragShader = sh.createFragmentShader(gl); //ShaderCreator.createFragmentShader(gl, "shaders/fragment.glsl");

        // ---------------------------------------------
        
        
        // attach this fragment shader to the shader program object:
        gl.glAttachShader(shaderProgram, fragShader);

        // link the shader program object:
        gl.glLinkProgram(shaderProgram);

        // validate the shader program object,
        // to check if the shader(s) in the program can be executed:
        gl.glValidateProgram(shaderProgram);

        // verify program validation status:
        if (!ShaderCreator.programValidationStatus(gl, shaderProgram)){
            System.err.println("Error: shader program object was not validated successfully!");

            // print validation info log:
            ShaderCreator.printProgramInfoLog(gl, shaderProgram);

            // exit
            runExit();
        }//if

        // for testing:
        System.out.println("Shader program object was validated successfully.");

        // print program info log:
        ShaderCreator.printProgramInfoLog(gl, shaderProgram);
        
    }//setupShader

    //----------------------------------------------------------------------

    private void changeVisMode(GL2 gl)
    {
        // load seismic and features volume:
        makeSeismicVolume();
        makeFeaturesVolume();

        // initialize textures:
        initializeTextures(gl);
        
        // reset:
        visModeChaged = false;
    }//changeVisMode

    //----------------------------------------------------------------------

    // [10/7/2012]
    private void changeVisLocation(GL2 gl)
    {
        // load seismic and features volume:
        makeSeismicVolume();
        makeFeaturesVolume();

        // initialize textures:
        initializeTextures(gl);
        
        // reset:
        visLocationChaged = false;
    }//changeVisLocation
    
    //----------------------------------------------------------------------

    // [11/9/2012]
    private void reloadHorizon(GL2 gl)
    {
        // load features volume:
        makeFeaturesVolume();

        // initialize textures:
        initializeTextures(gl);
        
        // reset:
        reloadHorizons = false;
    }//reloadHorizon
    
    //----------------------------------------------------------------------
    
    // [10/7/2012]
    public Point getRefPoint(){
        return new Point(this.xLRef, this.yLRef);
    }
    
    public int getCurrentLOD(){
        return this.lod;
    }
}
