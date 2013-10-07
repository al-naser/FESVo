/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 *
 * Provides a PrintStream to the other classes
 * Later: get configurations from a configurations file
 * 
 * @author Aqeel
 */
public class FESVoSystem {
    
    // folder name for output data:
    private static final String OUTPUT_FOLDER = PropertyReader.getProperty("OUTPUT_FOLDER");
    
    private static String fileName = OUTPUT_FOLDER + "/FESVo_output1.txt";
    private static String file2Name = OUTPUT_FOLDER + "/FESVo_output2.txt"; //[2/2/2012]
    private static String file3Name = OUTPUT_FOLDER + "/FESVo_output3.txt"; //[25/2/2012]
    
    // A PrintStream object to handle outputs of other classes
    // controled from here: using System.out or a file output
    public static final PrintStream out;
    public static final PrintStream out2; //[2/2/2012]
    public static final PrintStream out3; //[25/2/2012]
    
    // (1) System.out
    // out = System.out;
    
    // (2) File Output
    //public static final PrintStream out2 = new PrintStream(fileName);
    private static PrintStream outFile;
    private static PrintStream outFile2; //[2/2/2012]
    private static PrintStream outFile3; //[25/2/2012]
    
    // for file1:
    static{
        try {
            outFile = new PrintStream(fileName);
        }
        catch(FileNotFoundException ex){
            // if file can't be created, direct output to Syste.out
            outFile = System.out;
            
            // print error
            System.err.println(ex);
        }
        finally{
            out = outFile;
        }
    }
    
    // for file2:
    static{
        try {
            outFile2 = new PrintStream(file2Name);
        }
        catch(FileNotFoundException ex){
            // if file can't be created, direct output to Syste.out
            outFile2 = System.out;
            
            // print error
            System.err.println(ex);
        }
        finally{
            out2 = outFile2;
        }
    }
    
    // for file3:
    static{
        try {
            outFile3 = new PrintStream(file3Name);
        }
        catch(FileNotFoundException ex){
            // if file can't be created, direct output to Syste.out
            outFile3 = System.out;
            
            // print error
            System.err.println(ex);
        }
        finally{
            out3 = outFile3;
        }
    }
}
