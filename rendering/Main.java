/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package rendering;


/**
 * This program is to render slices of seismic, loaded from SRDS,
 * using texture mapping technique.
 *
 * v. 2.0
 *
 * @author Aqeel
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                GUI gui = new GUI();
            }
        });
    }

}
