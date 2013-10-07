/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

import com.jogamp.opengl.util.GLBuffers;
import java.io.*;
import java.nio.*;

/**
 *
 * @author Aqeel
 */
public class RawVolumeReader {

    private static final String file_path = "";

    public RawVolumeReader(){

    }//constructor

    // read a raw seismic file giving its name
    // and return array of bytes
    public static byte [] read(String fileName)
    {
        File file = null;
        FileInputStream inStream = null;
        byte [] rawData;// = new byte[samples_per_trace * sample_size];

        try
        {
            file = new File(file_path + fileName);
            inStream = new FileInputStream(file);

            // create an array of bytes to hold all data from the file:
            // assuming file size is no larger than 2^31 = ~2GB
            rawData = new byte[(int)file.length()];

            // read:
            int i = inStream.read(rawData);

            // throw exception if returned value is -1 (error)
            if (i == -1)
                throw new Exception("End of file");

            // closing:
            inStream.close();

            // return the read data:
            return rawData;

        }//try

        catch(Exception ex){
            System.err.println(ex);
        }

        // only if an exception is thrown,
        // return an empty array:
        return new byte[1];

    }//read()

    
    // using the above reaad() method, read a raw seismic file
    // and return as array of float normalized to the range [0,1]
    public static float [] read(String fileName, float min, float max)
    {
        // read data as bytes:
        byte [] dataB = read(fileName);

        // create a ByteBuffer from the byte array
        ByteBuffer bufferB = ByteBuffer.wrap(dataB);

        // changing byte order to Little Endian:
        bufferB.order(ByteOrder.LITTLE_ENDIAN);

        // create a float array from the FloatBuffer,
        // and normalized values to the range [0,1]
        float [] dataF = new float[dataB.length / 4];
        float sample;
        for (int i=0; i < dataF.length; i++){
            dataF[i] = ((bufferB.getFloat() - min) /  (max-min));
        }//for

        // return the output trace:
        return dataF;
    }//read()
    
    

}
