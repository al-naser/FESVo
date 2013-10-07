/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import seismic.DataType;
import seismic.FESVo;
import seismic.Feature;


/**
 *
 * @author Aqeel
 */
public class FeatureFile extends SeismicFile {
    
    private DataType featureType;

    public FeatureFile(String fileName, DataType featureType) {
        
        super(fileName);
        
        this.featureType = featureType;
        
    }//constructor
    
    public void loadFeatureToFESVo(int sourceID)
    {
        try {
            
            // create a buffer reader for the feature file:
            BufferedReader in = new BufferedReader(new FileReader (this.file));
            
            // current line:
            String line;
            
            // index:
            int i = 0;
            
            // read till end of file:
            while((line = in.readLine()) != null)
            {
                // split the line around white spaces:
                String [] subLine = line.split(" ");
                
                // convert each subString into int to get x, y, w
                // round float values to closest int:
                int x = Math.round(Float.parseFloat(subLine[0]));
                int y = Math.round(Float.parseFloat(subLine[1]));
                int w = Math.round(Float.parseFloat(subLine[2]));
                
                // create a Feature object:
                Feature feature = new Feature(x, y, sourceID);
                
                // add w to this feature object:
                feature.addW(w);
                
                // load this feature object into FESVo data map based on feature type:
                if (this.featureType == DataType.HORIZON)
                {
                    FESVo.putHorizon(feature);
                }
                else if (this.featureType == DataType.FAULT)
                {
                    FESVo.putFault(feature);
                }
                else
                {
                    System.err.println("No feature was loaded to FESVo due to unsupported feature type");
                    break;
                }
                
                // update index:
                i ++;
                
            }//while
            
            // [TEST]
            System.out.println(i + " feature units were load to FESVo data map.");
            
        } 
        
        catch (IOException ex) {
            System.err.println(ex);
        }
        
    }//loadFeatureToFESVo
    
}
