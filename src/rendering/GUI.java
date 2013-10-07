package rendering;

import java.awt.event.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.opengl.awt.*;
import com.jogamp.opengl.util.*;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import utilities.GIS;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.WindowConstants;
import seismic.DataTimestamp;
import seismic.DataType;
import seismic.FESVo;
import seismic.PointTag;
import seismic.SeismicMetadata;
import utilities.DataLoader;
import utilities.Point;
import utilities.PropertyReader;
import utilities.Utilities;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Aqeel
 */
public class GUI{
    
    /**
     * [8.2.2013] using class PropertyReader to get configurations from a file
     */
    
    // This GUI Frame:
    private final JFrame frame;
    
    // Frame title:
    private static final String FRAME_TITLE = PropertyReader.getProperty("FRAME_TITLE");// "Data-Centric Seismic Visualization";// "Parallel Streaming Seismic Visualization";
    
    //------------------------------------------------------------------------
    
    // SRDS source:
    private SeismicMetadata src;
    
    // [22.2.2013] user's ID
    private int userID = 0; // 0 means that it is not valid yet
    private String userName = "";
    
    // [23/10/2012] selected horizons SRDS property values (prop_val):
    private int [] horizons;
    
    // [23/10/2012] selected faults SRDS property values (prop_val):
    private int [] faults;
    
    // Max. texture size in MB:
    private double maxTextureSize = Double.parseDouble(PropertyReader.getProperty("maxTextureSize"));// 60.0; // default value
    
    // Number of threads:
    private int threads = Integer.parseInt(PropertyReader.getProperty("threads"));//12;//4; // default value
    
    // Multi-statement Length:
    private int multiStatementLength = Integer.parseInt(PropertyReader.getProperty("multiStatementLength"));//16; // default value
    
    // a lower resolution initial LOD:
    private int lowerResolutionInitialLOD = Integer.parseInt(PropertyReader.getProperty("lowerResolutionInitialLOD"));//0; // default value
    
    // current connection parameters:
    private String connectionParameters;
    
    // TEMP
    private String utm_zone = "32W";
    
    //------------------------------------------------------------------------
    
    // Menu Items needed to be later altered:
    private JMenuItem   showMapMenuItem, 
                        showSrcInfoMenuItem, 
                        showConParametersMenuItem,
                        showLoaderInfoMenuItem,
                        closeMenuItem,
                        controlAmplitudeMenuItem,
                        shiftHorizonMenuItem,
                        historyMenuItem,
                        deleteSessionMenuItem,
                        reloadFeaturesMenuItem;
    
    //------------------------------------------------------------------------
    
    // progress bar and its dialog:
    JDialog progressDialog;
    JProgressBar progressBar;
    JLabel progressBarLabel; // [23.2.2013]
    
    //------------------------------------------------------------------------
    
    // The current OpenGL renderer engine inside the frame:
    OpenGLRenderer renderer = null;
    
    // the current Loader associated with the renderer:
    DataLoader loader = null;
    
    // [4/5/2012]
    // the current visualization mode: slice based (2D) or volume (3D)
    private boolean sliceVisualization = false;
    
    // [26/9/2012] pre-load features mode
    private boolean preLoadFeaturesMode = true; // default value
    
    // [29/9/2012] selected timestamp for history
    private DataTimestamp selectedTimestamp = null; // default value
    
    //------------------------------------------------------------------------
    
    // [23/10/2012]
    // JLists for horizons & faults:
    private JList horizonsList, faultsList;
    private JComboBox sourcesList;
    
    // =======================================================================
    
    public GUI() {
        frame = new JFrame(FRAME_TITLE);
        
        // add a menu bar:
        frame.setJMenuBar(createMenuBar());
        
        // add an image of the architecture once the program starts:
        showArchitectureImage();
        
        // set frame parameters
        frame.setSize(800, 800);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        
        // [22.2.2013] ask user to sign in:
        this.userSignIn();
        
    }//constructor
    
    // =======================================================================
    
    /**
     * This method is called at the launch of the GUI 
     * or when closing a data visualization session 
     * to wipe out the frame's content 
     * and display an illustration of the architecture.
     */
    private void showArchitectureImage()
    {
        // add an image of the architecture:
        JLabel imgLabel = new JLabel(new ImageIcon("architecture_s.jpg"), JLabel.CENTER);
        frame.setContentPane(imgLabel);
        
        // show frame:
        frame.setVisible(true);
    }//showArchitectureImage
    
    //------------------------------------------------------------------------
    
    /**
     * Creates an OpenGL Renderer content
     * This is called once the user selects a data source
     * @param metadata 
     */
    private void createOpenGLContent(SeismicMetadata metadata)
    {
        // create an OpenGL panel:
        GLProfile glprofile = GLProfile.getDefault();
        GLCapabilities caps = new GLCapabilities( glprofile );
        GLJPanel canvas = new GLJPanel(caps);
        
        // create a data loader based on user's selection:
        loader = new DataLoader(metadata, (long)(maxTextureSize*1024*1024), threads, multiStatementLength, preLoadFeaturesMode, horizons, faults);
        
        // create an OpenGLRenderer and add it to the panel
        renderer = new OpenGLRenderer(loader, sliceVisualization, lowerResolutionInitialLOD);
        canvas.addGLEventListener(renderer);
        canvas.addKeyListener((KeyListener) renderer);
        
        // replace the content of the frame with this GL panel:
        frame.setContentPane(canvas);
        canvas.requestFocusInWindow();
        
        // setup and start an animator:
        // this is to ensure that the display method is repeatedly called
        FPSAnimator animator = new FPSAnimator( canvas, 10 ); //changed to 10fps (was 30) on 30/5/2012
	animator.start();
        
        // update frame title:
        this.updateFrameTitle(); //[23.2.2013]
        
        // save connection parameters in a String:
        connectionParameters = "Max. Texture Size = " + maxTextureSize + "MB";
        connectionParameters += "\nNumber of Threads = " + threads;
        connectionParameters += "\nMulti-Statement Length = " + multiStatementLength;
        connectionParameters += "\nLower-Resolution Initial level above top LOD = " + lowerResolutionInitialLOD;


        // show the frame:
        frame.setVisible(true);
        
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // close progress bar:
                progressDialog.dispose();
                
                // enable related menu item:
                enableMenuItems(true);
            }
        });
    }
    
    // =======================================================================
    
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu menu, submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        JCheckBoxMenuItem cbMenuItem;
        
        // ---------------------
        
        // First Menu:
        menu = new JMenu("FESVo");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Sign in");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        userSignIn();
                    }
                });    
        menu.add(menuItem);
        
        menu.addSeparator();
        
        menuItem = new JMenuItem("Open a seismic volume");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        selectSeismicVolume();
                    }
                });    
        menu.add(menuItem);
        
        menu.addSeparator();
        
        showSrcInfoMenuItem = new JMenuItem("Show source info");
        showSrcInfoMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        showSrcInfo();
                    }
                });
        showSrcInfoMenuItem.setEnabled(false);
        menu.add(showSrcInfoMenuItem);
        
        showLoaderInfoMenuItem = new JMenuItem("Show loader full info");
        showLoaderInfoMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        showLoaderInfo();
                    }
                });
        showLoaderInfoMenuItem.setEnabled(false);
        menu.add(showLoaderInfoMenuItem);
        
        menu.addSeparator();
        
        closeMenuItem = new JMenuItem("Close");
        closeMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        close();
                    }
                });  
        closeMenuItem.setEnabled(false);
        menu.add(closeMenuItem);
        
        menu.addSeparator();
        
        menuItem = new JMenuItem("Quit");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        // first, close visualization session:
                        close();
                        
                        // exit:
                        System.exit(0);
                    }
                });    
        menu.add(menuItem);
        
        // ---------------------
        
        // Second Menu:
        menu = new JMenu("Connection Parameters");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Set maximum texture size");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        checkBeforeSettingConnectionParameters();
                        setMaxTextureSize();
                    }
                });    
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Set number of threads");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        checkBeforeSettingConnectionParameters();
                        setNumberOfThreads();
                    }
                });
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Set multi-statement length");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        checkBeforeSettingConnectionParameters();
                        setMultiStatementLength();
                    }
                });
        menu.add(menuItem);
        
        menuItem = new JMenuItem("Set lower resolution initial LOD");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        checkBeforeSettingConnectionParameters();
                        setLowerResolutionInitialLOD();
                    }
                });
        menu.add(menuItem);
        
        menu.addSeparator();
        
        cbMenuItem = new JCheckBoxMenuItem("Pre-Load Features (to support user's input & versioning)");
        cbMenuItem.setSelected(this.preLoadFeaturesMode);
        cbMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        checkBeforeSettingConnectionParameters();
                        preLoadFeaturesMode = !preLoadFeaturesMode;
                    }
                });
        
        menu.add(cbMenuItem);
        
        menu.addSeparator();
        
        showConParametersMenuItem = new JMenuItem("Show current connection parameters");
        showConParametersMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        showConnectionParameters();
                    }
                });
        showConParametersMenuItem.setEnabled(false);
        menu.add(showConParametersMenuItem);
        
        // ---------------------
        
        // Third Menu:
        menu = new JMenu("Visualize");
        menuBar.add(menu);
        
        //a group of radio button menu items
        ButtonGroup group = new ButtonGroup();

        rbMenuItem = new JRadioButtonMenuItem("Volume Visualization");
        rbMenuItem.setSelected(true);
        group.add(rbMenuItem);
        rbMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        setVolumeVisualizationMode();
                    }
                });
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("Slice Visualization");
        group.add(rbMenuItem);
        rbMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        setSliceVisualizationMode();
                    }
                });
        menu.add(rbMenuItem);
        
        menu.addSeparator();
        
        menuItem = new JMenuItem("Navigate");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        setVisualizationLocation();
                    }
                });
        menu.add(menuItem);
        
        menu.addSeparator();
        
        controlAmplitudeMenuItem = new JMenuItem("Show amplitudes control");
        controlAmplitudeMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                controlAmplitude();
                            }
                        });
                    }
                });
        controlAmplitudeMenuItem.setEnabled(false);
        menu.add(controlAmplitudeMenuItem);
        
        menu.addSeparator();
        
        reloadFeaturesMenuItem = new JMenuItem("Reload Features");
        reloadFeaturesMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                reloadFeatures();
                            }
                        });
                    }
                });
        reloadFeaturesMenuItem.setEnabled(false);
        menu.add(reloadFeaturesMenuItem);
        
        // ---------------------
        
        // Fourth Menu:
        menu = new JMenu("Map");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Set UTM Zone");
        menuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        setUTMZone();
                    }
                });
        menu.add(menuItem);
        
        menu.addSeparator();
        
        showMapMenuItem = new JMenuItem("Locate data on Google Map");
        showMapMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                locateDataOnMap();
                            }
                        });
                    }
                });
        showMapMenuItem.setEnabled(false);
        menu.add(showMapMenuItem);
        
        // ---------------------
        
        // Fifth Menu:
        menu = new JMenu("Interpret");
        menuBar.add(menu);
        
        shiftHorizonMenuItem = new JMenuItem("Time-Shift horizon");
        shiftHorizonMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                shiftHorizon();
                            }
                        });
                    }
                });
        shiftHorizonMenuItem.setEnabled(false);
        menu.add(shiftHorizonMenuItem);
        
        deleteSessionMenuItem = new JMenuItem("Delete Complete Session");
        deleteSessionMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                deleteSession();
                            }
                        });
                    }
                });
        deleteSessionMenuItem.setEnabled(false);
        menu.add(deleteSessionMenuItem);
        
        menu.addSeparator();
        
        historyMenuItem = new JMenuItem("History");
        historyMenuItem.addActionListener(
                new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        //Schedule a job for the event-dispatching thread:
                        //creating and showing this application's GUI.
                        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                viewHistory();
                            }
                        });
                    }
                });
        historyMenuItem.setEnabled(false);
        menu.add(historyMenuItem);
        
        // ---------------------
        
        return menuBar;
    }
    
    //------------------------------------------------------------------------
    
    private void selectSeismicVolume()
    {
        System.out.println("Selecting a seismic volume ...");
        
        // create a pane:
        JOptionPane pane = new JOptionPane();
        pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
        
        // create a dialog:
        JDialog dialog = pane.createDialog(this.frame, "Selecting seismic dataset & features (Horizons & Faults)");
        
        // ----------------------------
        
        // get SRDS sources info:
        Object [] sources = DataLoader.readSrdsSrcMetadata(); //{1, 2, 3}; 
        
        // create a combo box for user to choose one dataset:
        sourcesList = new JComboBox(sources);
        sourcesList.setAlignmentX(Component.LEFT_ALIGNMENT);
        sourcesList.addActionListener(new ActionListener(){
                    public void actionPerformed(ActionEvent e){
                        // update the features lists:
                        horizonsList.setListData(
                                ((SeismicMetadata)sourcesList.getSelectedItem()).getHorizonsPropVal().toArray());
                        faultsList.setListData(
                                ((SeismicMetadata)sourcesList.getSelectedItem()).getFaultsPropVal().toArray());
                    }//actionPerformed
            });
        
        // a label:
        JLabel sourcesLabel = new JLabel("Please select a dataset source:");
        sourcesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // a panel to contain sources list and its label:
        JPanel sourcePane = new JPanel();
        sourcePane.setLayout(new BoxLayout(sourcePane, BoxLayout.Y_AXIS));
        sourcePane.add(sourcesLabel);
        sourcePane.add(Box.createRigidArea(new Dimension(0,5)));
        sourcePane.add(sourcesList);
        sourcePane.add(Box.createRigidArea(new Dimension(0,15)));
        
        // ----------------------------
        
        // create a list of horizons:
        horizonsList = new JList(((SeismicMetadata)sourcesList.getSelectedItem()).getHorizonsPropVal().toArray());
        
        // a scroller for the list:
        JScrollPane horizonsListScroller = new JScrollPane(horizonsList);
        horizonsListScroller.setPreferredSize(new Dimension(150, 450));
        horizonsListScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Label for horizons:
        JLabel horizonsLabel = new JLabel("Select horizons:");
        horizonsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // panel to contain these components:
        JPanel horizonsPane = new JPanel();
        horizonsPane.setLayout(new BoxLayout(horizonsPane, BoxLayout.Y_AXIS));
        horizonsPane.add(horizonsLabel);
        horizonsPane.add(Box.createRigidArea(new Dimension(0,5)));
        horizonsPane.add(horizonsListScroller);        
        
        // ----------------------------
        
        // create a list of faults:
        faultsList = new JList(((SeismicMetadata)sourcesList.getSelectedItem()).getFaultsPropVal().toArray());
        
        // a scroller for the list:
        JScrollPane faultsListScroller = new JScrollPane(faultsList);
        faultsListScroller.setPreferredSize(new Dimension(150, 450));
        faultsListScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Label for horizons:
        JLabel faultsLabel = new JLabel("Select faults:");
        faultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // panel to contain these components:
        JPanel faultsPane = new JPanel();
        faultsPane.setLayout(new BoxLayout(faultsPane, BoxLayout.Y_AXIS));
        faultsPane.add(faultsLabel);
        faultsPane.add(Box.createRigidArea(new Dimension(0,5)));
        faultsPane.add(faultsListScroller);
        
        // ----------------------------
        
        // container for the lists:
        JPanel content = new JPanel(new BorderLayout());
        content.add(sourcePane, BorderLayout.NORTH);
        content.add(horizonsPane, BorderLayout.WEST);
        content.add(faultsPane, BorderLayout.EAST);
        
        // ----------------------------
        
        pane.setMessage(content);
        
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(455, 285));
        
        // show dialog:
        dialog.setVisible(true);
        
        // ----------------------------
        // Now wait for user to select
        // ----------------------------
        
        Integer opt = (Integer)pane.getValue();
        
        if (opt == null || opt.intValue() == JOptionPane.CANCEL_OPTION){
            System.out.println("User cancelled!");
            return;
        }
        
        // set selected source:
        this.src = (SeismicMetadata)sourcesList.getSelectedItem();
        
        // [TEST] print user's selection:
        System.out.println("User selected: " + this.src);
        
        // set selected horizons & print:
        System.out.println("User Selected Horizons:");
        Object [] selectedHorizons = horizonsList.getSelectedValues();
        this.horizons = new int[selectedHorizons.length]; int i=0;
        for (Object h : selectedHorizons){
            this.horizons[i++] = ((Integer)h).intValue();
            System.out.println("\t" + h.toString());
        }
        
        // set selected faults & print:
        System.out.println("User Selected Faults:");
        Object [] selectedFaults = faultsList.getSelectedValues();
        this.faults = new int[selectedFaults.length]; i=0;
        for (Object f : selectedFaults){
            this.faults[i++] = ((Integer)f).intValue();
            System.out.println("\t" + f.toString());
        }
        
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // create an OpenGL content based on this selection:
                createOpenGLContent(src);
            }
        });

        // show progress bar:
        showProgressBar("Loading data", ("Loading " + this.src.toString() + " ..."));
            
    }//selectSeismicVolume
    
    //------------------------------------------------------------------------
    
    // [20/10/2012]
    // allow user to select required features
    // returns user selection of either:
    //      null or JOptionPane.OK_OPTION
    private Object selectFeatures_old(SeismicMetadata src)
    {
        System.out.println("Selecting features to be displayed ...");
        
        // create a pane:
        JOptionPane pane = new JOptionPane();
        pane.setMessageType(JOptionPane.QUESTION_MESSAGE);
        pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);
        
        // create a dialog:
        JDialog dialog = pane.createDialog(this.frame, "Selecting Features: Horizons & Faults");
        
        // ----------------------------
        
        // create a list of horizons:
        JList horizonsList = new JList(src.getHorizonsPropVal().toArray());
        
        // a scroller for the list:
        JScrollPane horizonsListScroller = new JScrollPane(horizonsList);
        horizonsListScroller.setPreferredSize(new Dimension(100, 450));
        horizonsListScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Label for horizons:
        JLabel horizonsLabel = new JLabel("Select horizons:");
        horizonsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // panel to contain these components:
        JPanel horizonsPane = new JPanel();
        horizonsPane.setLayout(new BoxLayout(horizonsPane, BoxLayout.Y_AXIS));
        horizonsPane.add(horizonsLabel);
        horizonsPane.add(Box.createRigidArea(new Dimension(0,5)));
        horizonsPane.add(horizonsListScroller);
        horizonsPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        
        // ----------------------------
        
        // create a list of faults:
        JList faultsList = new JList(src.getFaultsPropVal().toArray());
        
        // a scroller for the list:
        JScrollPane faultsListScroller = new JScrollPane(faultsList);
        faultsListScroller.setPreferredSize(new Dimension(100, 450));
        faultsListScroller.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Label for horizons:
        JLabel faultsLabel = new JLabel("Select faults:");
        faultsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // panel to contain these components:
        JPanel faultsPane = new JPanel();
        faultsPane.setLayout(new BoxLayout(faultsPane, BoxLayout.Y_AXIS));
        faultsPane.add(faultsLabel);
        faultsPane.add(Box.createRigidArea(new Dimension(0,5)));
        faultsPane.add(faultsListScroller);
        faultsPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // ----------------------------
        
        // container for the lists:
        JPanel listsPane = new JPanel();
        listsPane.setLayout(new BoxLayout(listsPane, BoxLayout.X_AXIS));
        listsPane.add(horizonsPane);
        listsPane.add(faultsPane);
        
        // ----------------------------
        
        pane.setMessage(listsPane);
        
        dialog.setResizable(true);
        dialog.setMinimumSize(new Dimension(380, 285));
        
        // show dialog:
        dialog.setVisible(true);
        
        // ----------------------------
        // Now wait for user to select
        // ----------------------------
        
        Integer opt = (Integer)pane.getValue();
        
        if (opt == null || opt.intValue() == JOptionPane.CANCEL_OPTION){
            System.out.println("User cancelled!");
            return null;
        }
        
        // set selected horizons & print:
        System.out.println("User Selected Horizons:");
        Object [] selectedHorizons = horizonsList.getSelectedValues();
        this.horizons = new int[selectedHorizons.length]; int i=0;
        for (Object h : selectedHorizons){
            this.horizons[i++] = ((Integer)h).intValue();
            System.out.println("\t" + h.toString());
        }
        
        
        // set selected faults & print:
        System.out.println("User Selected Faults:");
        Object [] selectedFaults = faultsList.getSelectedValues();
        this.faults = new int[selectedFaults.length]; i=0;
        for (Object f : selectedFaults){
            this.faults[i++] = ((Integer)f).intValue();
            System.out.println("\t" + f.toString());
        }
        
        return opt;
        
    }//selectFeatures
    
    //------------------------------------------------------------------------
    
    private void locateDataOnMap()
    {
        System.out.println("Loading Google Map ...");
        
        MapPanel mapPanel = new MapPanel(GIS.UTM2LatLon(this.src.pointA.y, this.src.pointA.x, utm_zone),
                                GIS.UTM2LatLon(this.src.pointB.y, this.src.pointB.x, utm_zone),
                                GIS.UTM2LatLon(this.src.pointC.y, this.src.pointC.x, utm_zone),
                                GIS.UTM2LatLon(this.src.pointD.y, this.src.pointD.x, utm_zone));
        
        JFrame mapFrame = new JFrame(this.src.toString() + " - Dataset Location on Google Map");
        
        mapFrame.add(mapPanel);
        mapFrame.pack();
        mapFrame.setResizable(false);
        mapFrame.setLocationRelativeTo(null);
        mapFrame.setVisible(true);
        
    }
    
    //------------------------------------------------------------------------
    
    private void setMaxTextureSize()
    {
        System.out.println("Setting maximum texture size ...");
        
        // create a an input dialog for users to input a texture size in MB:
        String sSize = (String)JOptionPane.showInputDialog(
                frame, 
                "Please enter the maximum size of a texture object in MB:", 
                "Maximum texture size", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                maxTextureSize);
        
        
        if (sSize != null){
            try{
                // try parsing input into type double:
                double size = Double.parseDouble(sSize);
                
                // set max. texture size:
                this.maxTextureSize = size;
                
                // [TEST] print:
                System.out.println("New max. texture size: " + this.maxTextureSize + "MB");
            }
            catch(NumberFormatException ex){
                // if input is not a number, show an error message:
                JOptionPane.showMessageDialog(frame, "Input is not a number!", "Error in input", JOptionPane.ERROR_MESSAGE);
            }
            
        }//if
    }//setMaxTextureSize
    
    
    //------------------------------------------------------------------------
    
    private void setNumberOfThreads()
    {
        System.out.println("Setting number of threads ...");
        
        // create a an input dialog for users to input number of threads:
        String sThreads = (String)JOptionPane.showInputDialog(
                frame, 
                "Please enter number of threads:", 
                "Number of threads", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                threads);
        
        
        if (sThreads != null){
            try{
                // try parsing input into type int:
                int thr = Integer.parseInt(sThreads);
                
               // set number of threads:
                this.threads = thr;
                
                // [TEST] print:
                System.out.println("New number of threads: " + this.threads);
            }
            catch(NumberFormatException ex){
                // if input is not an integer, show an error message:
                JOptionPane.showMessageDialog(frame, "Input is not an integer!", "Error in input", JOptionPane.ERROR_MESSAGE);
            }
            
        }//if
    }//setNumberOfThreads
    
    //------------------------------------------------------------------------
    
    private void setMultiStatementLength()
    {
        System.out.println("Setting Multi-Statement Length ...");
        
        // create a an input dialog for users to input multiStatementLength:
        String sLength = (String)JOptionPane.showInputDialog(
                frame, 
                "Please enter a length for the multi statements:\n(i.e. number of queries executed in a single statement)", 
                "Multi-Statement Length", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                multiStatementLength);
        
        
        if (sLength != null){
            try{
                // try parsing input into type int:
                int len = Integer.parseInt(sLength);
                
               // set multiStatementLength:
                this.multiStatementLength = len;
                
                // [TEST] print:
                System.out.println("New Multi-Statement Length: " + this.multiStatementLength);
            }
            catch(NumberFormatException ex){
                // if input is not an integer, show an error message:
                JOptionPane.showMessageDialog(frame, "Input is not an integer!", "Error in input", JOptionPane.ERROR_MESSAGE);
            }
            
        }//if
    }//setMultiStatementLength
    
    //------------------------------------------------------------------------
    
    private void setLowerResolutionInitialLOD()
    {
        System.out.println("Setting a Lower Resolution Initial LOD ...");
        
        // create a an input dialog for users to input initial LOD:
        String sLength = (String)JOptionPane.showInputDialog(
                frame, 
                "Please enter a number of levels above the default level of details (top LOD)\nto initially load data at a lower resolution.", 
                "Lower Resolution Initial LOD", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                lowerResolutionInitialLOD);
        
        
        if (sLength != null){
            try{
                // try parsing input into type int:
                int value = Integer.parseInt(sLength);
                
               // set lowerResolutionInitialLOD:
                this.lowerResolutionInitialLOD = value;
                
                // [TEST] print:
                System.out.println("New initial level above top LOD: " + this.lowerResolutionInitialLOD);
            }
            catch(NumberFormatException ex){
                // if input is not an integer, show an error message:
                JOptionPane.showMessageDialog(frame, "Input is not an integer!", "Error in input", JOptionPane.ERROR_MESSAGE);
            }
            
        }//if
    }
    
    //------------------------------------------------------------------------
    
    private void checkBeforeSettingConnectionParameters()
    {
        // if user is currently visualizing a dataset,
        // give a warning indicating that new settings will be applied for next session:
        if (this.src != null){
            JOptionPane.showMessageDialog(
                    frame, 
                    "Any change on connection parameters will be applied on next session!", 
                    "Warning - changing connection parameters", 
                    JOptionPane.WARNING_MESSAGE);
        }
    }
    
    //------------------------------------------------------------------------
    
    private void showSrcInfo()
    {
        if (this.src != null){
            JOptionPane.showMessageDialog(
                    frame, 
                    this.src.getInfo(), 
                    this.src.toString() + " - Info", 
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    //------------------------------------------------------------------------
    
    private void showLoaderInfo()
    {
        if (this.src != null){
            JOptionPane.showMessageDialog(
                    frame, 
                    this.loader.toString(), 
                    this.src.toString() + " - Current Loader Info", 
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    //------------------------------------------------------------------------
    
    private void showConnectionParameters()
    {
        if (this.src != null){
            JOptionPane.showMessageDialog(
                    frame, 
                    this.connectionParameters, 
                    "Connection Parameters", 
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    //------------------------------------------------------------------------
    
    private void close()
    {
        if (this.src != null){
            // to close the current visualization methods,
            // we can just replace the frame's content
            // this will call the dispose() methods of the renderer
            // which will close network connections:
            showArchitectureImage();
            
            // disable some menu items:
            enableMenuItems(false);

            // make sure source, renderer and loader are null:
            this.src = null;
            this.loader = null;
            this.renderer = null;
            
            // update frame title:
            this.updateFrameTitle();
        }
    }
    
    //------------------------------------------------------------------------
    
    // enable or disable some menu items,
    // i.e. enable them after openning a session,
    // and disable them after closing
    private void enableMenuItems(boolean en)
    {
        showMapMenuItem.setEnabled(en);
        showSrcInfoMenuItem.setEnabled(en);
        showConParametersMenuItem.setEnabled(en);
        showLoaderInfoMenuItem.setEnabled(en);
        closeMenuItem.setEnabled(en);
        controlAmplitudeMenuItem.setEnabled(en);
        shiftHorizonMenuItem.setEnabled(en);
        historyMenuItem.setEnabled(en);
        deleteSessionMenuItem.setEnabled(en);
        reloadFeaturesMenuItem.setEnabled(en);
    }//enableMenuItems
    
    //------------------------------------------------------------------------
    
    private void setVolumeVisualizationMode()
    {
        System.out.println("Volume Visualization Mode ...");
        this.sliceVisualization = false;
        
        if (this.src != null){
            this.renderer.setVisualizationMode(sliceVisualization);
        }
    }
    
    //------------------------------------------------------------------------
    
    // [10/7/2012]
    private void setVisualizationLocation()
    {
        // for now, don't support setting vis location before loading a volume:
        if (this.loader == null)
            return;
        
        
        System.out.println("Setting a new location ...");
        
        // current reference point relative to LOD0 and current LOD:
        int currentLOD = this.renderer.getCurrentLOD();
        Point ref = FESVo.mapPointToLOD0(this.renderer.getRefPoint(), currentLOD);
        String defS = "" + ref.x + "," + ref.y + "," + currentLOD;
        
        // range:
        int [] dim = loader.getLod0Dimension();
        String range = "[from (0,0) to (" + dim[0] + "," + dim[1] + ")]";
        
        // create a an input dialog for users to input new reference point and LOD:
        String sLocation = (String)JOptionPane.showInputDialog(
                frame, 
                "Please enter a reference point relative to LOD0 " + range + " and a resolution level (LOD)."
                + "\n Example: {100, 20, 3} means a reference point (100, 20) and an LOD 3."
                + "\n Note: LOD -1 means topLOD", 
                "Navigating", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                defS);
        
        
        if (sLocation != null){
            try{
                // split input:
                String [] in = sLocation.split(",");
                
                // try parsing input into type int:
                int xRef = Integer.parseInt(in[0]);
                int yRef = Integer.parseInt(in[1]);
                int lod = Integer.parseInt(in[2]);
                
                // check if topLOD is requested:
                if (lod < 0){
                    lod = loader.getTopLOD();
                }
                
                // set new vis location:
                this.renderer.setVisLocation(new Point(xRef, yRef), lod);
                
                // [TEST] print:
                System.out.println("New location: " + xRef + "," + yRef + "," + lod);
            }
            catch(Exception ex){
                // if input is not as required, show an error message:
                JOptionPane.showMessageDialog(frame, "Input is not as required!", "Error in input", JOptionPane.ERROR_MESSAGE);
            }
            
        }//if
    }
    
    //------------------------------------------------------------------------
    
    private void setSliceVisualizationMode()
    {
        System.out.println("Slice Visualization Mode ...");
        this.sliceVisualization = true;
        
        if (this.src != null){
            this.renderer.setVisualizationMode(sliceVisualization);
        }
    }
    
    //------------------------------------------------------------------------
    
    private void controlAmplitude()
    {
        if (this.src == null) return;
        
        System.out.println("Controlling min/max amplitudes to be rendered ...");
        
        AmplitudesControlPanel panel = new AmplitudesControlPanel(renderer, src.amplitudeMin, src.amplitudeMax);
        
        JFrame controlFrame = new JFrame(this.src.toString() + " - Amplitudes Control")
        {
            @Override
            public void dispose(){
                controlAmplitudeMenuItem.setEnabled(true);
                super.dispose();
            }
        }
        ;
        
        controlFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        controlFrame.add(panel);
        controlFrame.setSize(800, 80);
        //controlFrame.setResizable(false);
        controlFrame.setLocationRelativeTo(null);
        controlFrame.setVisible(true);
        
        // disable the related menu item:
        controlAmplitudeMenuItem.setEnabled(false);
    }//controlAmplitude
    
    //=======================================================================
    
    private void showProgressBar(String title, String message)
    {
        
        // the progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setIndeterminate(true);

        // a label with text:
        progressBarLabel = new JLabel(message, JLabel.CENTER);
        //label.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        
        // a panel to hold both the progress bar and the label text:
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressBarLabel, BorderLayout.PAGE_START);
        panel.add(progressBar, BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        
        // Use a modal JDialog to hold the progress bar:
        progressDialog = new JDialog(this.frame, title, true);
        progressDialog.add(panel);
        //progressDialog.pack();
        progressDialog.setSize(400, 150);
        progressDialog.setLocationRelativeTo(this.frame);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setVisible(true);
        
    }//showProgressBar
    
    //------------------------------------------------------------------------
    
    private void setUTMZone()
    {
        System.out.println("Setting UTM Zone ...");
        
        // create a an input dialog for users to correct UTM Zone:
        String newZone = (String)JOptionPane.showInputDialog(
                frame, 
                "If provided UTM zone is not correct, please enter required zone.", 
                "UTM Zone", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                this.utm_zone);
        
        
        if (newZone != null){
            // set new zone:
            this.utm_zone = newZone;

            // [TEST] print:
            System.out.println("New UTM Zone: " + this.utm_zone);

        }//if
    }//setUTMZone
    
    //=======================================================================
    
    // [7/9/2012]
    private void shiftHorizon()
    {
        System.out.println("Time shifting a horizon ...");
        
        // Let user selects a 2D texture point (s,t) & shifting parameters:
        // ----------------------------------------------------------------
        
        // get list of features FESVo IDs:
        int [] ids = this.loader.getFeaturesIDs();
        
        // convert into array of Integer:
        Integer [] idsInt = Utilities.toIntegerArray(ids);
        
        // create a an input dialog for users to select a horizon source id:
        Integer selectedSrc = (Integer) JOptionPane.showInputDialog(
                frame, "Please select a horizon source id to time-shift:", "Horizons source", 
                JOptionPane.QUESTION_MESSAGE, null, idsInt, null);
        
        // exit if no source id was selected:
        if (selectedSrc == null)
            return;
            
        // [TEST] print user's selection:
        System.out.println("User selected: " + selectedSrc);
        
        // array list of texture points with the required horizon:
        ArrayList<Point> texPoints = new ArrayList<Point>();
        
        // texture dimension
        int [] dim = this.loader.getTextureDimension();
        
        // curretn reference point:
        Point refPoint = this.renderer.getRefPoint();
        
        // current LOD:
        int lod = this.renderer.getCurrentLOD();
        
        // generate all points from texture dimension:
        for (int t=0; t < dim[1]; t++){
            for (int s=0; s < dim[0]; s++){
                // generate a point tag:
                PointTag tag = new PointTag((s+refPoint.x), (t+refPoint.y), selectedSrc);
                
                // check if this tag has required horizon point:
                if (FESVo.get(tag, lod) != null){
                    // add texture point (s,t) to the list:
                    texPoints.add(new Point(s,t));
                }
            }
        }

        // create a an input dialog for users to select a horizon point:
        Point selectedTexPoint = (Point) JOptionPane.showInputDialog(
                frame, "Please select a horizon point to interpret:", "Horizon (ID: " + selectedSrc + ") points", 
                JOptionPane.QUESTION_MESSAGE, null, texPoints.toArray(), null);
        
        // exit if no point was selected:
        if (selectedTexPoint == null)
            return;
        
        // [TEST] print user's selection:
        System.out.println("User selected: " + selectedTexPoint);
        
        // get alteration (time shift) value from user:
        String timeShiftS = (String)JOptionPane.showInputDialog(
                frame, 
                "Enter a time shift value (delta w).", 
                "Time Shift Value", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                0);
        
        // exit if no value was entered:
        if (timeShiftS == null || timeShiftS.isEmpty())
            return;
        
        int timeShift = Integer.parseInt(timeShiftS);
        
        if (timeShift == 0) return;
        
        // [TEST] print user's selection:
        System.out.println("User selected: " + timeShift);
        
        // get alteration diameter value from user:
        String diameterS = (String)JOptionPane.showInputDialog(
                frame, 
                "Enter an alteration diameter (in meter).", 
                "Alteration Diameter", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                0);
        
        // exit if no value was entered:
        if (diameterS == null || diameterS.isEmpty())
            return;
        
        int diameter = Integer.parseInt(diameterS);
        
        if (diameter < 1) return;
        
        // [TEST] print user's selection:
        System.out.println("User selected: " + diameter);
        
        
        // Shift horizon in DB via loader:
        // -------------------------------
        
        // converting selected texture point to a local tag at LOD0:
        PointTag tag0 = FESVo.mapPointToLOD0(
                new PointTag(
                    (selectedTexPoint.x+refPoint.x), 
                    (selectedTexPoint.y+refPoint.y),
                    selectedSrc),
                lod);
        
        // [TEST] print user's selection:
        System.out.println("Tag0: " + tag0);
        
        // [23.2.2013] check if user didn't sign in:
        if (this.userID == 0){
            this.userSignIn("You must sign in to complete this task. Please, enter your user ID.");
            
            // if user decided not to sign in, exit:
            if (this.userID == 0){
                JOptionPane.showMessageDialog(
                        frame, 
                        "This task was not completed as you didn't sign in!", 
                        "Error!", 
                        JOptionPane.ERROR_MESSAGE);
                
                return;
            } 
        }
        
        // shift horizon in DB:
        this.loader.shiftHorizon(tag0, timeShift, diameter, this.userID);
        
        // clear horizons, reload, and re-render: ([29.3.2013])
        this.reloadFeatures();
        
    }//shiftHorizon
    
    //------------------------------------------------------------------------
    
    // [29.3.2013] Clears features cache from FESVo, then reloads and render
    private void reloadFeatures()
    {
        // Clear this horizon from FESVo:
        // ------------------------------
        
        this.loader.clearFeaturesCache();
        
        // [TEST] print
        System.out.println("Features were removed from FESVo");
                
        
        // Rebuild texture buffer (reload & rendering):
        // --------------------------------------------
        
        // reload and render latest horizons:
        this.renderer.reloadHorizons();
    }
    
    //------------------------------------------------------------------------
    
    // [29/9/2012]
    private void viewHistory()
    {
        System.out.println("View interpretation history ...");
        
        // get list of grouping effective time:
        ArrayList<DataTimestamp> timestamps = this.loader.getGroupingEffectiveTimestamps();
        
        // insert a "Baseline" timestamp
        timestamps.add(timestamps.size(), new DataTimestamp(this.src.srdsSrcID));
        
        // create a an input dialog for users to select a timestamp:
        DataTimestamp ts = (DataTimestamp) JOptionPane.showInputDialog(
                frame, "Please select a timestamp:", "Interpretation Timestamp", 
                JOptionPane.QUESTION_MESSAGE, null, timestamps.toArray(), this.selectedTimestamp);
        
        // exit if no source id was selected:
        if (ts == null)
            return;
        
        // [TEST] print user's selection:
        System.out.println("User selected: " + ts);
        
        // make it NULL if latest ts is selected:
        if (ts == timestamps.get(0)){
            ts = null;
        }
        
        // check if a new timestamp was selected:
        if (this.selectedTimestamp == ts 
            || (this.selectedTimestamp != null 
                && ts != null 
                && this.selectedTimestamp.equals(ts)))
        {
            return;
        }
        
        // set selected timestamp:
        this.selectedTimestamp = ts;
        
        
        
        // Clear this horizon from FESVo:
        // ------------------------------
        
        this.loader.clearFeaturesCache();
        
        // set requested timestamp:
        this.loader.setFeaturesTimestamp(ts);
        
        // [TEST] print
        System.out.println("Features were removed from FESVo");
                
        
        // Rebuild texture buffer (reload & rendering):
        // --------------------------------------------
        
        // reload and render latest horizons:
        this.renderer.reloadHorizons();
        
    }//viewHistory
    
    //------------------------------------------------------------------------
    
    // [8/10/2012]
    private void deleteSession()
    {
        System.out.println("Deleting a complete session ...");
        
        // ---------------------------------------------------
        
        // get list of features FESVo IDs:
        int [] ids = this.loader.getFeaturesIDs();
        
        // convert into array of Integer:
        Integer [] idsInt = Utilities.toIntegerArray(ids);
        
        // create a an input dialog for users to select a horizon source id:
        Integer selectedSrc = (Integer) JOptionPane.showInputDialog(
                null, "Please select a feature (e.g. horizon) source ID:", "Horizons source", 
                JOptionPane.QUESTION_MESSAGE, null, idsInt, null);
        
        // exit if no source id was selected:
        if (selectedSrc == null)
            return;
            
        // [TEST] print user's selection:
        System.out.println("User selected: " + selectedSrc);
        
        // ---------------------------------------------------
        
        // get list of insertion type grouping effective time:
        ArrayList<DataTimestamp> timestamps = this.loader.getInsertionTypeGroupingEffectiveTimestamps(selectedSrc);
        
        // insert a "Baseline" timestamp
        timestamps.add(timestamps.size(), new DataTimestamp(this.src.srdsSrcID));
        
        // if need to reload; to be used in the loop:
        boolean reload = false;
        
        while (true){
            // create a an input dialog for users to select a timestamp:
            DataTimestamp ts = (DataTimestamp) JOptionPane.showInputDialog(
                    null, "Please select a session to completely delete:", "Timestamps of Data Input", 
                    JOptionPane.QUESTION_MESSAGE, null, timestamps.toArray(), this.selectedTimestamp);

            // exit if no source id was selected:
            if (ts == null){
                if (reload)
                    break;
                else
                    return; //exit
            }//if

            // [TEST] print user's selection:
            System.out.println("User selected: " + ts);

            // load and render only selected timestamp:
            // ----------------------------------------
            
            // clear FESVo:
            this.loader.clearFeaturesCache();
            
            // set requested timestamp with an option to load only alone:
            this.loader.setFeaturesTimestamp(ts, true);
            
            // set requested feature ID:
            this.loader.setFeaturesIDsToLoad(selectedSrc);
            
            // reload and render:
            this.renderer.reloadHorizons();
            
            // ----------------------------------------

            // verify with user:
            int ans = JOptionPane.showConfirmDialog(
                    null, 
                    "Is this the object you want to delete?", 
                    "Confirmation of deletion",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            
            if (ans != JOptionPane.YES_OPTION){
                reload = true; // need to reload
                continue;
            }//if
            
            
            // [23.2.2013] check if user didn't sign in:
            if (this.userID == 0){
                this.userSignIn("You must sign in to complete this task. Please, enter your user ID.");

                // if user decided not to sign in, exit:
                if (this.userID == 0){
                    JOptionPane.showMessageDialog(
                            frame, 
                            "This task was not completed as you didn't sign in!", 
                            "Error!", 
                            JOptionPane.ERROR_MESSAGE);

                    break;
                } 
            }
            
            // delete if user confirms:
            this.loader.deleteHorizon(selectedSrc, ts, this.userID);
            
            // exit the loop:
            break;
        
        }//while
        
        // clear FESVo:
        this.loader.clearFeaturesCache();

        // set requested timestamp to latest:
        this.loader.setFeaturesTimestamp(null);

        // reload and render:
        this.renderer.reloadHorizons();
        
    }//deleteSession()
    
    //=======================================================================
    
    /**
     * [22/2/2013] Sign user in, by entering their user ID
     * using a standard message 
     */
    private void userSignIn(){
        userSignIn("Please, enter your user ID to sign in.");
    }
    
    private void userSignIn(String message){
        
        // a panel for the message asking user to enter their user ID:
        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        
        // a text field for the ID:
        JTextField idField = new JTextField();
        
        // add components to the message panel:
        messagePanel.add(Box.createRigidArea(new Dimension(0,10))); // some space
        messagePanel.add( new JLabel(message));
        messagePanel.add(Box.createRigidArea(new Dimension(0,5))); // some space
        messagePanel.add(idField);
        messagePanel.add(Box.createRigidArea(new Dimension(0,10))); // some space
        
        idField.requestFocusInWindow();
        
        // options for user to select from:
        Object [] options = {"Sign in", "Create a new account", "Cancel"};
        
        // the optional dialog:
        int n = JOptionPane.showOptionDialog(
                frame, 
                messagePanel,
                "User Sign In", 
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, // no custom icon
                options, // buttons title
                options[0]); // default option
        
        // --------------------------------------------------
        
        // if "sign in" option is selected:
        if (n == 0){
            String userIdS = idField.getText();
            
            // validate String:
            if (userIdS.isEmpty() || !userIdS.matches(".*\\d.*")){
                // ask again
                this.userSignIn("You have entered an invalid user ID, please try again.");
            }
            
            else{
                
                int id = Integer.parseInt(userIdS);
                
                // --------------------------------------------------
                // ::::::::::::: Validate User ID on DB :::::::::::::
                // --------------------------------------------------
                String userName = DataLoader.validateUserID(id);
                // --------------------------------------------------
                
                // if user found on DB, 
                if (userName != null){
                    // welcome user:
                    JOptionPane.showMessageDialog(frame, ("Welcome! " + userName));
                    
                    // set user ID:
                    this.setUserID(id, userName);
                    
                    // [TEST] print user's selection:
                    System.out.println("User ID: " + this.userID);
                }//if
                
                else{
                    // ask again
                    this.userSignIn("You have entered an invalid user ID, please try again.");
                }//else
            }//else
        }// end: if "sign in" option is selected:
        
        // --------------------------------------------------
        
        // if "Create a new account" option is selected:
        else if (n == 1){
            System.out.println("Creating a new account ...");
            
            // sign up user:
            this.userSignUp();
            
        }//end: if "Create a new account" option is selected:
        
        // --------------------------------------------------
        
        // if cancel option was selected
        else{
            System.out.println("User does not wish to sign in!");
        }
        
    }//userSignIn
    
    //------------------------------------------------------------------------
    
    /**
     * Create an account in the DB for the user
     * 
     * @return a newly generated user ID
     */
    private void userSignUp(){
        this.userSignUp("Please, enter your name to create a new account.");
    }
    
    private void userSignUp(String message){
        
        int userID = 0;
        
        String userName = (String)JOptionPane.showInputDialog(
                frame, 
                message, 
                "Creating a new account", 
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                null);

        // exit if no value was entered:
        if (userName == null){
            System.out.println("User does not wish to sign in!");
            return;
        }

        // if empty or starts with a digit, then ask again:
        if (userName.isEmpty() || Character.isDigit(userName.charAt(0))){
            this.userSignUp("User's name should not be empty and should not start with a digit, please try again.\n"
                          + "Enter your name to create a new account.");
        }
        else{
            
            // --------------------------------------------------
            // ::::::::::::: Generate User ID on DB :::::::::::::
            // --------------------------------------------------
            userID = DataLoader.generateUserID(userName);
            // --------------------------------------------------
            
            // set user ID:
            this.setUserID(userID, userName);
            
            // inform user:
            JOptionPane.showMessageDialog(frame, 
                    ("Welcome! " + userName + 
                     "\nYour user ID is " + userID +
                     "\nKeep a record of this."));
        }
        
            
    }//userSignUp
    
    //------------------------------------------------------------------------
    
    private void setUserID(int userID, String userName){
        
        // set user ID and name:
        this.userID = userID;
        this.userName = userName;
        
        // update frame title:
        this.updateFrameTitle();
        
    }//setUserID
    
    //------------------------------------------------------------------------
    
    private void updateFrameTitle(){
        
        String title = "";
        
        // 1. Dataset title:
        if (this.src != null){
            title += src.toString() + " | ";
        }
        
        // 2. Application title:
        title += FRAME_TITLE;
        
        // 3. User Info.
        if (this.userID != 0){
            title += (" | " + userName + " [" + userID + "]");
        }
        
        // update frame title:
        this.frame.setTitle(title);
        
    }//updateFrameTitle
}
