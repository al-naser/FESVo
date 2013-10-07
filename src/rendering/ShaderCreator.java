/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rendering;

import java.awt.Color;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.opengl.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import seismic.DataType;
import seismic.FESVo;


/**
 * Updated on [25/10/2012] to support dynamic shader
 * Code is generated on the fly
 *
 * @author Aqeel
 */
public class ShaderCreator {
    
    // variables types
    // ---------------
    private final static String SAMPLER3D_TYPE = "sampler3D";
    private final static String FLOAT_TYPE = "float";
    private final static String VEC3_TYPE = "vec3";

    
    // version and functions names
    // ---------------------------
    
    private final static int VERSION = 120;
    
    private final static String PALETTE_FUNC = "palette";
    private final static String PALETTE_REV_FUNC = "palette_rev";
    
    
    // uniform names; public to be accessed by renderer
    // ------------------------------------------------
    
    public final static String TRACES_SAMPLER = "tracesTexture"; // to be accessed by the renderer
    public final static String FEATURES_SAMPLER = "featuresTexture"; // to be accessed by the renderer
    
    public final static String MIN_AMPLITUDE = "min_amplitude";
    public final static String MAX_AMPLITUDE = "max_amplitude";
    public final static String ALPHA = "alpha";
    public final static String SEISMIC_TEX_DEPTH = "seismicTexDepth";
    
    public final static String FEATURE_NO_ = "feature_";
    
    
    
    // Variables to be set by user:
    // -----------------------------
    
    private final boolean renderTraces, renderFeatures;
    private final int [] featuresIDs;
    
    
    
    // Colors Database to be used for fault patches:
    // Source: http://www.two4u.com/color/medium-txt.html
    // excluding white and black colors
    // ---------------------------------------------
    
    private final static Color[] COLOR_DB = new Color[98];
    
    static{
        COLOR_DB[0] = new Color(255,0,0); // Red #FF0000
        COLOR_DB[1] = new Color(0,255,0); // Green #00FF00
        COLOR_DB[2] = new Color(0,0,255); // Blue #0000FF
        COLOR_DB[3] = new Color(255,0,255); // Magenta #FF00FF
        COLOR_DB[4] = new Color(0,255,255); // Cyan #00FFFF
        COLOR_DB[5] = new Color(255,255,0); // Yellow #FFFF00
        COLOR_DB[6] = new Color(112,219,147); // Aquamarine #70DB93
        COLOR_DB[7] = new Color(92,51,23); // Baker's Chocolate #5C3317
        COLOR_DB[8] = new Color(159,95,159); // Blue Violet #9F5F9F
        COLOR_DB[9] = new Color(181,166,66); // Brass #B5A642
        COLOR_DB[10] = new Color(217,217,25); // Bright Gold #D9D919
        COLOR_DB[11] = new Color(166,42,42); // Brown #A62A2A
        COLOR_DB[12] = new Color(140,120,83); // Bronze #8C7853
        COLOR_DB[13] = new Color(166,125,61); // Bronze II #A67D3D
        COLOR_DB[14] = new Color(95,159,159); // Cadet Blue #5F9F9F
        COLOR_DB[15] = new Color(217,135,25); // Cool Copper #D98719
        COLOR_DB[16] = new Color(184,115,51); // Copper #B87333
        COLOR_DB[17] = new Color(255,127,0); // Coral #FF7F00
        COLOR_DB[18] = new Color(66,66,111); // Corn Flower Blue #42426F
        COLOR_DB[19] = new Color(92,64,51); // Dark Brown #5C4033
        COLOR_DB[20] = new Color(47,79,47); // Dark Green #2F4F2F
        COLOR_DB[21] = new Color(74,118,110); // Dark Green Copper #4A766E
        COLOR_DB[22] = new Color(79,79,47); // Dark Olive Green #4F4F2F
        COLOR_DB[23] = new Color(153,50,205); // Dark Orchid #9932CD
        COLOR_DB[24] = new Color(135,31,120); // Dark Purple #871F78
        COLOR_DB[25] = new Color(107,35,142); // Dark Slate Blue #6B238E
        COLOR_DB[26] = new Color(47,79,79); // Dark Slate Grey #2F4F4F
        COLOR_DB[27] = new Color(151,105,79); // Dark Tan #97694F
        COLOR_DB[28] = new Color(112,147,219); // Dark Turquoise #7093DB
        COLOR_DB[29] = new Color(133,94,66); // Dark Wood #855E42
        COLOR_DB[30] = new Color(84,84,84); // Dim Grey #545454
        COLOR_DB[31] = new Color(133,99,99); // Dusty Rose #856363
        COLOR_DB[32] = new Color(209,146,117); // Feldspar #D19275
        COLOR_DB[33] = new Color(142,35,35); // Firebrick #8E2323
        COLOR_DB[34] = new Color(245,204,176); // Flesh #F5CCB0
        COLOR_DB[35] = new Color(35,142,35); // Forest Green #238E23
        COLOR_DB[36] = new Color(205,127,50); // Gold #CD7F32
        COLOR_DB[37] = new Color(219,219,112); // Goldenrod #DBDB70
        COLOR_DB[38] = new Color(192,192,192); // Grey #C0C0C0
        COLOR_DB[39] = new Color(82,127,118); // Green Copper #527F76
        COLOR_DB[40] = new Color(147,219,112); // Green Yellow #93DB70
        COLOR_DB[41] = new Color(33,94,33); // Hunter Green #215E21
        COLOR_DB[42] = new Color(78,47,47); // Indian Red #4E2F2F
        COLOR_DB[43] = new Color(159,159,95); // Khaki #9F9F5F
        COLOR_DB[44] = new Color(192,217,217); // Light Blue #C0D9D9
        COLOR_DB[45] = new Color(168,168,168); // Light Grey #A8A8A8
        COLOR_DB[46] = new Color(143,143,189); // Light Steel Blue #8F8FBD
        COLOR_DB[47] = new Color(233,194,166); // Light Wood #E9C2A6
        COLOR_DB[48] = new Color(50,205,50); // Lime Green #32CD32
        COLOR_DB[49] = new Color(228,120,51); // Mandarian Orange #E47833
        COLOR_DB[50] = new Color(142,35,107); // Maroon #8E236B
        COLOR_DB[51] = new Color(50,205,153); // Medium Aquamarine #32CD99
        COLOR_DB[52] = new Color(50,50,205); // Medium Blue #3232CD
        COLOR_DB[53] = new Color(107,142,35); // Medium Forest Green #6B8E23
        COLOR_DB[54] = new Color(234,234,174); // Medium Goldenrod #EAEAAE
        COLOR_DB[55] = new Color(147,112,219); // Medium Orchid #9370DB
        COLOR_DB[56] = new Color(66,111,66); // Medium Sea Green #426F42
        COLOR_DB[57] = new Color(127,0,255); // Medium Slate Blue #7F00FF
        COLOR_DB[58] = new Color(127,255,0); // Medium Spring Green #7FFF00
        COLOR_DB[59] = new Color(112,219,219); // Medium Turquoise #70DBDB
        COLOR_DB[60] = new Color(219,112,147); // Medium Violet Red #DB7093
        COLOR_DB[61] = new Color(166,128,100); // Medium Wood #A68064
        COLOR_DB[62] = new Color(47,47,79); // Midnight Blue #2F2F4F
        COLOR_DB[63] = new Color(35,35,142); // Navy Blue #23238E
        COLOR_DB[64] = new Color(77,77,255); // Neon Blue #4D4DFF
        COLOR_DB[65] = new Color(255,110,199); // Neon Pink #FF6EC7
        COLOR_DB[66] = new Color(0,0,156); // New Midnight Blue #00009C
        COLOR_DB[67] = new Color(235,199,158); // New Tan #EBC79E
        COLOR_DB[68] = new Color(207,181,59); // Old Gold #CFB53B
        COLOR_DB[69] = new Color(255,127,0); // Orange #FF7F00
        COLOR_DB[70] = new Color(255,36,0); // Orange Red #FF2400
        COLOR_DB[71] = new Color(219,112,219); // Orchid #DB70DB
        COLOR_DB[72] = new Color(143,188,143); // Pale Green #8FBC8F
        COLOR_DB[73] = new Color(188,143,143); // Pink #BC8F8F
        COLOR_DB[74] = new Color(234,173,234); // Plum #EAADEA
        COLOR_DB[75] = new Color(217,217,243); // Quartz #D9D9F3
        COLOR_DB[76] = new Color(89,89,171); // Rich Blue #5959AB
        COLOR_DB[77] = new Color(111,66,66); // Salmon #6F4242
        COLOR_DB[78] = new Color(140,23,23); // Scarlet #8C1717
        COLOR_DB[79] = new Color(35,142,104); // Sea Green #238E68
        COLOR_DB[80] = new Color(107,66,38); // Semi-Sweet Chocolate #6B4226
        COLOR_DB[81] = new Color(142,107,35); // Sienna #8E6B23
        COLOR_DB[82] = new Color(230,232,250); // Silver #E6E8FA
        COLOR_DB[83] = new Color(50,153,204); // Sky Blue #3299CC
        COLOR_DB[84] = new Color(0,127,255); // Slate Blue #007FFF
        COLOR_DB[85] = new Color(255,28,174); // Spicy Pink #FF1CAE
        COLOR_DB[86] = new Color(0,255,127); // Spring Green #00FF7F
        COLOR_DB[87] = new Color(35,107,142); // Steel Blue #236B8E
        COLOR_DB[88] = new Color(56,176,222); // Summer Sky #38B0DE
        COLOR_DB[89] = new Color(219,147,112); // Tan #DB9370
        COLOR_DB[90] = new Color(216,191,216); // Thistle #D8BFD8
        COLOR_DB[91] = new Color(173,234,234); // Turquoise #ADEAEA
        COLOR_DB[92] = new Color(92,64,51); // Very Dark Brown #5C4033
        COLOR_DB[93] = new Color(205,205,205); // Very Light Grey #CDCDCD
        COLOR_DB[94] = new Color(79,47,79); // Violet #4F2F4F
        COLOR_DB[95] = new Color(204,50,153); // Violet Red #CC3299
        COLOR_DB[96] = new Color(216,216,191); // Wheat #D8D8BF
        COLOR_DB[97] = new Color(153,204,50); // Yellow Green #99CC32
    }
    
    // -----------------------------

    /**
     * Creates a ShaderCreator object for rendering traces and features
     * @param featuresIDs 
     */
    public ShaderCreator(int[] featuresIDs) {
        
        this(featuresIDs, true);
    }
    
    /**
     * Creates a ShaderCreator object for rendering traces only
     */
    public ShaderCreator() {
        
        this(null, true);
    }
    
    /**
     * Creates a ShaderCreator object for rendering traces if renderTraces is true, and features is featuresIDs is not empty and not null
     * @param featuresIDs
     * @param renderTraces 
     */
    public ShaderCreator(int[] featuresIDs, boolean renderTraces) {
        
        this.renderTraces = renderTraces;
        this.featuresIDs = featuresIDs;
        
        if (featuresIDs == null || featuresIDs.length == 0){
            this.renderFeatures = false;
        }
        else{
            this.renderFeatures = true;
        }
        
        
        // give a warning if we have more faults than available colors:
        int count = 0;
        for (int id : featuresIDs){
            if (FESVo.getDataType(id) == DataType.FAULT){
                count ++;
            }
        }
        
        if (count > COLOR_DB.length){
            System.err.println("There are more fault patches than available colors!");
        }
    }
    
    // ------------------------------------------------------------------------
    
    public int createFragmentShader(GL2 gl)
    {
        // create a fragment shader:
        int fragShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

        // verify if shader was created successfully:
        if (fragShader == 0){
            System.err.println("Error: fragment shader was not created successfully!");
            runExit();
            return 0;
        }//if

        // generate shader code:
        String [] fragCode = new String[1];
        fragCode[0] = this.generateFragShaderCode();

        // Associate the code with the shader:
        gl.glShaderSource(fragShader, 1, fragCode, null);

        // compile the shader:
        gl.glCompileShader(fragShader);
        
        // verify shader compilation status:
        if (!shaderCompilationStatus(gl, fragShader)){
            System.err.println("Error: Shader was not compiled successfully!");

            // print shader info log:
            printShaderInfoLog(gl, fragShader);

            // exit and/or return 0
            runExit();
            return 0;
        }//if

        // for testing:
        System.out.println("Shader was successfully compiled.");

        // print shader info log:
        printShaderInfoLog(gl, fragShader);

        // return the created shader:
        return fragShader;
    }//createFragmentShader
    
    // ------------------------------------------------------------------------

    private String generateFragShaderCode(){
        
        // create a StringBuilder object to build a code
        // start with the shader VERSION:
        StringBuilder code = new StringBuilder("#version " + VERSION + "\n");
        
        // add uniforms:
        if (renderTraces){
            addUniform(code, SAMPLER3D_TYPE, TRACES_SAMPLER);
            addUniform(code, FLOAT_TYPE, MIN_AMPLITUDE, MAX_AMPLITUDE, ALPHA, SEISMIC_TEX_DEPTH);
            
            
        }
        if (renderFeatures){
            addUniform(code, SAMPLER3D_TYPE, FEATURES_SAMPLER);
            
            for (int i=0; i < featuresIDs.length; i++){
                addUniform(code, VEC3_TYPE, FEATURE_NO_ + i);
            }
            
            // add Palette Functions:
            addPaletteFunctions(code);
        }
        
        // add main function:
        addMainFunction(code);
        
        
        return code.toString();
    }//generateFragShaderCode
    
    
    // Helper methods to generate the code:
    // ------------------------------------
    
    private void addUniform(StringBuilder code, String varType, String... varNames){
        
        for (String varName : varNames){
            code.append("uniform " + varType + " " + varName + ";\n");
        }
        
    }//addUniform
    
    private void addPaletteFunctions(StringBuilder code){
        
        // a palette of blue - light blue - green - yellow - red
        // takes a value on range [0, 1], returns a vec3 color
        
        String func = 
                
                "vec3 " + PALETTE_FUNC + "(float i){ \n"
                
                + "vec3 color;\n"
                
                + "if (i > 0.75){\n"
                + "float c = (i - 0.75) / .25;\n"
                + "color = vec3(1.0, (1 - c), 0.0);}\n" // toward red
                
                + "else if (i > 0.5){\n"
                + "float c = (i - 0.5) / 0.25;\n"
                + "color = vec3(c, 1.0, 0.0);}\n" // toward yellow
                
                + "else if (i > 0.25){\n"
                + "float c = (i - .25) / 0.25;\n"
                + "color = vec3(0, 1.0, (1.0 - c));}\n" // toward green
                
                + "else {\n"
                + "float c = i / 0.25;\n"
                + "color = vec3(0, c, 1.0);}\n" // from blue toward light blue
                
                + "return color;}\n\n" 
                
                // a reverse of palette1
                + "vec3 " + PALETTE_REV_FUNC + "(float i){\n"
                + "return " + PALETTE_FUNC + "(1-i);}\n\n"
                
                ;
        
        
        
        // add function to the code:
        code.append(func);
    
    }//addPaletteFunctions
    
    private void addMainFunction(StringBuilder code){
        
        // variables names:
        final String luminance = "luminance";
        final String feature = "feature";
        final String heightColor = "h";
        
        // function code:
        String func = "void main(void){\n";
        
        
                
        if (renderTraces){

            func +=
                // access the seismic texture sampler, as a base color:
                FLOAT_TYPE + " " + luminance + " = float(texture3D(" + TRACES_SAMPLER + ", gl_TexCoord[0].stp));\n"

                // normalize data:
                + luminance + " = ((" + luminance + " - " + MIN_AMPLITUDE + ") /  (" + MAX_AMPLITUDE + " - " + MIN_AMPLITUDE + "));\n"

                ;

        }//if
                
                
        if (renderFeatures){

            func += 
                // access the features texture sampler:
                FLOAT_TYPE + " " + feature + " = float(texture3D(" + FEATURES_SAMPLER + ", gl_TexCoord[1].stp));\n"

                // Height-Based Color
                + FLOAT_TYPE + " " + heightColor + ";\n"

                ;

            // go through the features:
            String loop = "";
            for (int i=0; i < featuresIDs.length; i++){
                
                // get a new color
                float [] c = new float[3];
                c = COLOR_DB[i].getRGBColorComponents(c);
                System.out.println("(" + c[0] + "," + c[1] + "," + c[2] + ")");

                if (i != 0)
                    loop += "else ";

                loop += 
                    "if (" + feature + " == " + FEATURE_NO_ + i + "[0]){\n"
                    + heightColor + " = (gl_TexCoord[1].s - " + FEATURE_NO_ + i + "[1])"
                        + " / ("  + FEATURE_NO_ + i + "[2] - " + FEATURE_NO_ + i + "[1]);\n"
                        
                    + "gl_FragColor = vec4("
                        + PALETTE_REV_FUNC + "(" + heightColor + "), 1.0);\n}\n" //vec4(1.0, 0.0, 0.0, 1.0);
//                        + c[0] + "," + c[1] + "," + c[2] + ",1.0);\n}\n"

                    ;

            }

            // add the loop to the function:
            func += loop;

        }//if
        
        
        if (renderTraces){
            String ifStatement = "";
            
            // add "else" if features to be rendered to continue the loop:
            if (renderFeatures){
                ifStatement += "else ";
            }
            
            ifStatement += "if((1 - gl_TexCoord[0].p) < " + SEISMIC_TEX_DEPTH + "){\n"
                        + "gl_FragColor = vec4(" + luminance + ", " + luminance + ", " + luminance + ", " + ALPHA + ");\n}\n";
            
            // add it to func:
            func += ifStatement;
        }//if
        
        // [20/11/2012] add a clear color in case nothing to be rendered:
        // this is required after the updated on the graphics card in Nov. 2012
        func += "else{\n"
              + "gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n}\n";
        
        
        // end func and add it to the code:
        code.append(func + "}\n");
    
    }//addMainFunction
    
    // ========================================================================
    // :::::::::::::::::::::: Static Methods [Oct. 2011] ::::::::::::::::::::::
    // ========================================================================
    
    public static int createFragmentShader(GL2 gl, String fileName)
    {
        // create a fragment shader:
        int fragShader = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

        // verify if shader was created successfully:
        if (fragShader == 0){
            System.err.println("Error: fragment shader was not created successfully!");
            runExit();
            return 0;
        }//if

        // String array, with a single index, to hold the shader code:
        String [] fragCode = new String[1];
        fragCode[0] = "";
        String line;
        try {
            // load the file into the string array:
            BufferedReader reader = new BufferedReader(new FileReader(fileName));

            while ((line = reader.readLine()) != null) {
                fragCode[0] += line + "\n";
            }
        }//try
        catch (IOException ex) {
            Logger.getLogger(ShaderCreator.class.getName()).log(Level.SEVERE, null, ex);
        }

        // Associate the code with the shader:
        gl.glShaderSource(fragShader, 1, fragCode, null);

        // compile the shader:
        gl.glCompileShader(fragShader);
        
        // verify shader compilation status:
        if (!shaderCompilationStatus(gl, fragShader)){
            System.err.println("Error: Shader was not compiled successfully!");

            // print shader info log:
            printShaderInfoLog(gl, fragShader);

            // exit and/or return 0
            runExit();
            return 0;
        }//if

        // for testing:
        System.out.println("Shader was successfully compiled.");

        // print shader info log:
        printShaderInfoLog(gl, fragShader);

        // return the created shader:
        return fragShader;
    }//createFragmentShader

    //=======================================================================

    private static void runExit() {
        new Thread(new Runnable() {
            public void run() {
                System.exit(0);
            }
        }).start();

    }//runExit()

    //----------------------------------------------------------------------

    // returns true if the given shader was compiled successfully,
    // returns false otherwise:
    private static boolean shaderCompilationStatus(GL2 gl, int shader)
    {
        // create an IntBuffer object to hold the required parameter:
        IntBuffer parameter = IntBuffer.allocate(1);

        // get shader compilation status:
        gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, parameter);

        // if successful return true:
        if (parameter.get(0) == GL.GL_TRUE)
            return true;

        // otherwise return false:
        return false;
    }//shaderCompilationStatus

    //----------------------------------------------------------------------

    // prints a given shader info log:
    private static void printShaderInfoLog(GL2 gl, int shader)
    {
        // create an IntBuffer object to hold the required info log length:
        IntBuffer lengthBuf = IntBuffer.allocate(1);

        // get length of info log:
        gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, lengthBuf);
        int length = lengthBuf.get(0);

        // create a ByteBuffer object to hold the info log:
        ByteBuffer log = ByteBuffer.allocate(length);

        // flip the buffer, to be used again:
        lengthBuf.flip();

        // get info log:
        gl.glGetShaderInfoLog(shader, length, lengthBuf, log);

        // print shader info log:
        System.out.println("Shader info log:\n" + new String(log.array()));

    }//printShaderInfoLog

    //=======================================================================
    // :::::::::::: Service Methods (Public) for Program Objects ::::::::::::
    //=======================================================================

    // returns true if the given shader program object was validated successfully,
    // returns false otherwise:
    // Note: this is a public method to be accessed from outside (e.g. Main class)
    public static boolean programValidationStatus(GL2 gl, int program)
    {
        // create an IntBuffer object to hold the required parameter:
        IntBuffer parameter = IntBuffer.allocate(1);

        // get program  validation status:
        gl.glGetProgramiv(program, GL2.GL_VALIDATE_STATUS, parameter);

        // if successful return true:
        if (parameter.get(0) == GL.GL_TRUE)
            return true;

        // otherwise return false:
        return false;
    }//programValidationStatus

    //----------------------------------------------------------------------

    // prints a given program object info log:
    // Note: this is a public method to be accessed from outside (e.g. Main class)
    public static void printProgramInfoLog(GL2 gl, int program)
    {
        // create an IntBuffer object to hold the required info log length:
        IntBuffer lengthBuf = IntBuffer.allocate(1);

        // get length of info log:
        gl.glGetProgramiv(program, GL2.GL_INFO_LOG_LENGTH, lengthBuf);
        int length = lengthBuf.get(0);

        // create a ByteBuffer object to hold the info log:
        ByteBuffer log = ByteBuffer.allocate(length);

        // flip the buffer, to be used again:
        lengthBuf.flip();

        // get info log:
        gl.glGetProgramInfoLog(program, length, lengthBuf, log);

        // print program info log:
        System.out.println("Program info log:\n" + new String(log.array()));

    }//printShaderInfoLog

    // ========================================================================
    // :::::::::::::::::::::::  Main Method -  TESTING  :::::::::::::::::::::::
    // ========================================================================
    
    // main method for testing:
    public static void main (String [] args)
    {
        int [] f = {1,2,3};
        
        ShaderCreator sh = new ShaderCreator(f);
        
        String code = sh.generateFragShaderCode();
        
        System.out.println(code);
        
        Color color = COLOR_DB[6];
        float [] c = new float[3];
        c = color.getRGBColorComponents(c);
        System.out.println(color);
        System.out.println("(" + c[0] + "," + c[1] + "," + c[2] + ")");
        int z=0;
    }
}
