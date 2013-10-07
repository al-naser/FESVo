/*
 * utility public static methods
 */
package utilities;

/**
 *
 * @author Aqeel
 */
public class Utilities {
    
    
    public static double getStandardAngle(double ang){
        
        double sAng = ang % 360;
        if (sAng < 0)
            sAng += 360;
        
        //System.out.println(ang + " > " + sAng);
        
        return sAng;
    }
    
    //------------------------------------------------------------------------
    
    // convert into array of Integer:
    public static Integer [] toIntegerArray(int [] array){
        
        Integer [] returnArray = new Integer[array.length];
        for (int i=0; i < array.length; i++)
            returnArray[i] = array[i];
        
        return returnArray;
    }//toIntegerArray
    
    //------------------------------------------------------------------------
    
    // convert into array of int:
    public static int [] toIntArray(Integer [] array){
        
        int [] returnArray = new int[array.length];
        for (int i=0; i < array.length; i++)
            returnArray[i] = array[i].intValue();
        
        return returnArray;
    }//toIntegerArray
    
    //------------------------------------------------------------------------
    
}//class Utilities
