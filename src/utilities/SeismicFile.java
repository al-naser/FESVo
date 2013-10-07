/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.io.File;

/**
 *
 * @author Aqeel
 */
public abstract class SeismicFile {
    
    protected final File file;
    
    public SeismicFile(String fileName){
        this.file = new File(fileName);
    }
    
}
