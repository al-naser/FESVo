/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * A class to generate a horizon randomly for testing purposes
 *
 * @author Aqeel
 */
public class HorizonCreator {

    public static void createHorizone(FloatBuffer seismicVolume, int width, int depth, int height)
    {

        // for now, create a flat horizon at a specific height for each trace:
        int horizonHeight = height/3;

        // A unique value (id) for this horizon:
        float horizonID = 2.0f;

        // an index where the horizon id will be inserted;
        // an initial value of index is horizon height:
        int index = horizonHeight;

        // for each depth value:
        for (int i=0; i < depth; i++)
        {
            // for each width value, i.e. for each trace:
            for (int j=0; j < width; j++)
            {
                // insert a unique value at the horizon height:
                seismicVolume.put(index, horizonID);

                // increment index by height value:
                index += height;
            }
        }

        // verify size on seismic buffer:
        //System.out.println("Size of buffer: " + seismicVolume.capacity());

        
    }//createHorizon
    
    
    public static void createHorizone(IntBuffer seismicVolume, int width, int depth, int height)
    {

        // for now, create a flat horizon at a specific height for each trace:
        int horizonHeight = height/3;

        // A unique value (id) for this horizon:
        int horizonID = 2;

        // an index where the horizon id will be inserted;
        // an initial value of index is horizon height:
        int index = horizonHeight;

        // for each depth value:
        for (int i=0; i < depth; i++)
        {
            // for each width value, i.e. for each trace:
            for (int j=0; j < width; j++)
            {
                // insert a unique value at the horizon height:
                seismicVolume.put(index, horizonID);

                // increment index by height value:
                index += height;
            }
        }

        // verify size on seismic buffer:
        //System.out.println("Size of buffer: " + seismicVolume.capacity());

        
    }//createHorizon
}
