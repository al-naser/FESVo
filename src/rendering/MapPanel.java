package rendering;


import utilities.LatLong;
import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Aqeel
 */
public class MapPanel extends JPanel implements ChangeListener{
    
    private static final int ZOOM_MIN = 0;
    private static final int ZOOM_MAX = 21;
    private static final int ZOOM_INIT = 10;
    
    private static final String PATH_COLOR = "0x00000000";
    private static final String PATH_WEIGHT = "5";
    private static final String PATH_FILLCOLOR = "red"; //"0xFFFF0033";
    
    private JLabel mapLabel;
    private String url;
    private int currentZoom = -1; // initial value
    
    private int mapWidth, mapHeight;
    
    public MapPanel(LatLong... vertices) {
        
        super(new BorderLayout());
        
        url = "http://maps.googleapis.com/maps/api/staticmap?"
                + "size=600x600&"
                + "sensor=false&"
                + "path=color:" + PATH_COLOR
                + "%7Cweight:" + PATH_WEIGHT
                + "%7Cfillcolor:" + PATH_FILLCOLOR;
        
        // add the vertices in the path:
        for (LatLong vertex : vertices){
            url += "%7C" + vertex.latitude + "," + vertex.longitude;
        }
        
        ImageIcon icon;
        try {
            // create a map label
            icon = new ImageIcon(new URL(url));
            mapLabel = new JLabel(icon, JLabel.CENTER);
            add(mapLabel, BorderLayout.CENTER);

            mapWidth = icon.getIconWidth();
            mapHeight = icon.getIconHeight();

            System.out.println("Image width: " + icon.getIconWidth());
            System.out.println("Image height: " + icon.getIconHeight());
        }
        catch (MalformedURLException ex) {
            Logger.getLogger(MapPanel.class.getName()).log(Level.SEVERE, null, ex);
        }     
        
        // create a zoom slider
        JSlider zoomSlider = new JSlider(JSlider.VERTICAL, ZOOM_MIN, ZOOM_MAX, ZOOM_INIT);
        zoomSlider.addChangeListener(this);
        zoomSlider.setMajorTickSpacing(5);
        zoomSlider.setMinorTickSpacing(1);
        zoomSlider.setPaintTicks(true);
        zoomSlider.setPaintLabels(true);
        add(zoomSlider, BorderLayout.LINE_END);
    }
    
    public int getMapWidth(){
        return mapWidth;
    }
    
    public int getMapHeight(){
        return mapHeight;
    }

    @Override
    public void stateChanged(ChangeEvent ce) {
        
        /**
         * --------------------------------------
         * Later: cache previously fetched images
         * --------------------------------------
         */
        
        // get the slider in which an change even occured:
        JSlider slider = (JSlider)ce.getSource();
        
        if (!slider.getValueIsAdjusting()) {
            // get the new zoom level
            int zoom = (int)slider.getValue();
            
            // check if it has changed:
            if (zoom == currentZoom)
                return;
            
            // [TEST] print new zoom level:
            System.out.println("Zoom: " + zoom);
            
            try {
                // fetch a new map with the new zoom level:
                mapLabel.setIcon(new ImageIcon(new URL(url + "&zoom=" + zoom)));
                
                // set current zoom:
                currentZoom = zoom;
            } 
            catch (MalformedURLException ex) {
                Logger.getLogger(MapPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }//if
    }
}
