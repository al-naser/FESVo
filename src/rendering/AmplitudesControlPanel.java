/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package rendering;

import utilities.LatLong;
import java.awt.BorderLayout;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import utilities.DataLoader;

/**
 *
 * @author Aqeel
 */
public class AmplitudesControlPanel extends JPanel implements ChangeListener{
    
    private static final String POS_SLIDER = "POS_SLIDER";
    private static final String NEG_SLIDER = "NEG_SLIDER";
    
    private final OpenGLRenderer renderer;
    
    public AmplitudesControlPanel(OpenGLRenderer renderer, float amplitudeMin, float amplitudeMax) {
        
        //super(new BorderLayout()); 
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        
        this.renderer = renderer;
        
        // create a slider for the -ve part
        JSlider negSlider = new JSlider(JSlider.HORIZONTAL, (int)amplitudeMin, 0, (int)renderer.getMin_amplitude());
        negSlider.addChangeListener(this);
        negSlider.setMajorTickSpacing((int)Math.abs(amplitudeMin/10));//5);
        negSlider.setMinorTickSpacing((int)Math.abs(amplitudeMin/50));//1);
        negSlider.setPaintTicks(true);
        negSlider.setPaintLabels(true);
        negSlider.setName(NEG_SLIDER);
        //add(negSlider, BorderLayout.WEST);
        add(negSlider);
        
        // create a slider for the +ve part
        JSlider posSlider = new JSlider(JSlider.HORIZONTAL, 0, (int)amplitudeMax, (int)renderer.getMax_amplitude());
        posSlider.addChangeListener(this);
        posSlider.setMajorTickSpacing((int)Math.abs(amplitudeMax/5));//5);
        posSlider.setMinorTickSpacing((int)Math.abs(amplitudeMax/50));//1);
        posSlider.setPaintTicks(true);
        posSlider.setPaintLabels(true);
        posSlider.setName(POS_SLIDER);
        //add(posSlider, BorderLayout.EAST);
        add(posSlider);
    }
    
    @Override
    public void stateChanged(ChangeEvent ce) {
                
        // get the slider in which an change even occured:
        JSlider slider = (JSlider)ce.getSource();
        
        if (!slider.getValueIsAdjusting()) {
            // get the new 
            int amp = (int)slider.getValue();
            
            if (slider.getName().equals(POS_SLIDER))
                renderer.setMax_amplitude(amp);
            else
                renderer.setMin_amplitude(amp);
            
        }//if
    }
    
    
}
