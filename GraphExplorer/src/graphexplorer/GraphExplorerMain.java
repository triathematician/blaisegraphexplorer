/*
 * GraphExplorerMain.java
 * Created on May 14, 2010, 10:10:30 AM
 */

package graphexplorer;

import data.propertysheet.BeanEditorSupport;
import graphexplorer.controller.GraphControllerMaster;
import graphexplorer.controller.GraphController;
import graphexplorer.views.TimeMetricPlot;
import graphexplorer.actions.ExplorerLayoutActions;
import graphexplorer.actions.ExplorerGenerateActions;
import graphexplorer.io.ExplorerIOActions;
import data.propertysheet.PropertySheet;
import data.propertysheet.editor.EditorRegistration;
import graphexplorer.actions.ExplorerDecorActions;
import graphexplorer.actions.ExplorerStatActions;
import graphexplorer.actions.ExplorerStatActions.GlobalStatEnum;
import graphexplorer.actions.ExplorerStatActions.StatEnum;
import graphexplorer.controller.GraphFilterController;
import graphexplorer.controller.GraphStatController;
import graphexplorer.controller.TimeGraphController;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.bm.blaise.scio.graph.layout.IterativeGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticSpringLayout;
import org.bm.blaise.specto.plane.PlanePlotComponent;
import org.bm.blaise.specto.plane.graph.*;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationActionMap;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

/**
 *
 * @author Elisha Peterson
 */
public class GraphExplorerMain extends javax.swing.JFrame
        implements GraphExplorerInterface,
            PropertyChangeListener {
    

    // <editor-fold defaultstate="collapsed" desc="APPLICATION CODE">
    //
    // APPLICATION CODE
    //

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        Application.launch(GraphExplorerMain.App.class, args);
    }

    /** The application */
    Application app;
    /** Corresponds to the App inner class's rsc file */
    ResourceMap rsc;

    /** Class that contains the application context. */
    public static class App extends SingleFrameApplication {
        /** The main visible frame */
        private GraphExplorerMain mainFrame;
        /** The context class */
        private ApplicationContext ctxt;
        /** Resources for context */
        private ResourceMap rsc;
        /** Actions for context */
        private ApplicationActionMap acts;

        @Override
        protected void initialize(String[] args) {
            ctxt = getContext();
            rsc = ctxt.getResourceMap(GraphExplorerMain.class);
        }
        @Override
        protected void startup() {
            mainFrame = new GraphExplorerMain();
            acts = ctxt.getActionMap(mainFrame);
            mainFrame.rsc = rsc;
            mainFrame.app = this;
            show(mainFrame);
        }
    }

    /** Loads an image icon for the specified relative path */
    @Deprecated
    public static ImageIcon loadIcon(String path) {
        URL url = GraphExplorerMain.class.getResource("/graphexplorer/resources/"+path+".png");
        if (url == null) {
            System.out.println("Unable to load icon /graphexplorer/resources/" + path + ".png");
            return null;
        }
        return new ImageIcon(url);
    }
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="FILE/HELP MENU ACTIONS">
    //
    // FILE MENU ACTIONS
    //

    @Action
    public void quit() {
        System.exit(0);
    }


    @Action
    public void showAbout() {
        JOptionPane.showMessageDialog(this, rsc.getString("aboutText"));
    }

    @Action
    public void showHelp() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        java.net.URL helpURL = null;
        try {
            String filename = rsc.getResourcesDir() + rsc.getString("helpFile");
            helpURL = rsc.getClassLoader().getResource(filename);
            editorPane.setPage(helpURL);
        } catch (Exception ex) {
            Logger.getLogger(GraphExplorerMain.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Attempted to read a bad URL: " + helpURL);
            return;
        }

        //Put the editor pane in a scroll pane.
        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(480, 640));

        JOptionPane.showMessageDialog(null,
                editorScrollPane,
                rsc.getString("helpFrameTitle"), JOptionPane.QUESTION_MESSAGE);
    }
    
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="VIEW MENU ACTIONS">
    //
    // VIEW MENU ACTIONS
    //

    @Action
    public void fitToWindow() {
        GraphController gc = controller();
        if (gc != null) {
            PlanePlotComponent plot = (PlanePlotComponent) graphPlot();
            List nodes = gc.getViewNodes();
            Map<Object, Point2D.Double> pos = gc.getNodePositions();
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE,
                    maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            for (Object o : nodes) {
                Point2D.Double p = pos.get(o);
                if (p != null) {
                    minX = Math.min(minX, p.x);
                    minY = Math.min(minY, p.y);
                    maxX = Math.max(maxX, p.x);
                    maxY = Math.max(maxY, p.y);
                }
            }
            double rangeX = minX == maxX ? 1 : maxX - minX;
            double rangeY = minY == maxY ? 1 : maxY - minY;
            plot.setDesiredRange(minX - rangeX / 10., minY - rangeY / 10., maxX + rangeX / 10., maxY + rangeY / 10.);
        }
    }
    
    // </editor-fold>


    //<editor-fold defaultstate="collapsed" desc="LAYOUT MENU ACTIONS">
    //
    // LAYOUT MENU ACTIONS
    //
    
    //<editor-fold defaultstate="collapsed" desc="BackgroundLayoutTask CLASS">
    /** Task that will perform a graph layout in the background */
    private class BackgroundLayoutTask extends Task<Void,Void> 
            implements ActionListener {
        StaticGraphLayout layout;
        public BackgroundLayoutTask(StaticGraphLayout layout) { 
            super(app); 
            this.layout = layout; 
            setTitle("Computing Graph Layout...");
            setDescription("Layout is Executing");
            setMessage("Starting.");
        }       
        
        @Override
        protected Void doInBackground() throws Exception {
            if (controller() != null) {
                if (layout instanceof StaticSpringLayout) {
                    ((StaticSpringLayout)layout).setLayoutListener(this);
                    setProgress(0, 0, ((StaticSpringLayout)layout).maxSteps);
                }
                controller().applyLayout(layout, 5.0);
                fitToWindow();
            }
            return null;
        }
        public void actionPerformed(ActionEvent e) {
            if (layout instanceof StaticSpringLayout) {
                setMessage(e.getActionCommand());
                String cmd = e.getActionCommand();
                int i1 = cmd.indexOf("step = ");
                int i2 = cmd.indexOf("/", i1);
                Integer step = Integer.decode(cmd.substring(i1+7, i2));
                int maxSteps = ((StaticSpringLayout)layout).maxSteps;
                setProgress(step, 0, maxSteps);
            }
        }
    }
    //</editor-fold>
    
    
        @Action(block = Task.BlockingScope.ACTION)
    public Task layoutCircle() {
        return new BackgroundLayoutTask(StaticGraphLayout.CIRCLE);
    }

    private class LayoutCircleTask extends org.jdesktop.application.Task<Object, Void> {
        LayoutCircleTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to LayoutCircleTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }
    
        @Action(block = Task.BlockingScope.ACTION)
    public Task layoutRandom() {
        return new BackgroundLayoutTask(StaticGraphLayout.RANDOM);
    }

    private class LayoutRandomTask extends org.jdesktop.application.Task<Object, Void> {
        LayoutRandomTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to LayoutRandomTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }
    
        @Action(block = Task.BlockingScope.APPLICATION)
    public Task layoutSpringStatic() {
        return new BackgroundLayoutTask(StaticSpringLayout.getInstance());
    }

    private class LayoutSpringStaticTask extends org.jdesktop.application.Task<Object, Void> {
        LayoutSpringStaticTask(org.jdesktop.application.Application app) {
            // Runs on the EDT.  Copy GUI state that
            // doInBackground() depends on from parameters
            // to LayoutSpringStaticTask fields, here.
            super(app);
        }
        @Override protected Object doInBackground() {
            // Your Task's code here.  This method runs
            // on a background thread, so don't reference
            // the Swing GUI from here.
            return null;  // return your result
        }
        @Override protected void succeeded(Object result) {
            // Runs on the EDT.  Update the GUI based on
            // the result computed by doInBackground().
        }
    }
    
    //</editor-fold>
    
    /** Controller */
    final GraphControllerMaster master;
    /** Controllers and associated components */
    HashMap<GraphController, Component> tabs = new HashMap<GraphController, Component>();
    /** Controllers and associated plane graph elements */
    HashMap<GraphController, GraphComponent> graphs = new HashMap<GraphController, GraphComponent>();
    /** Chart displaying longitudinal metric data */
    TimeMetricPlot longMetricCP;

    /** File/IO actions */
    ExplorerIOActions actions_io;
    /** Statistics/metric actions */
    ExplorerStatActions actions_stat;
    /** Layout actions */
    ExplorerLayoutActions actions_layout;
    /** Decor actions */
    ExplorerDecorActions actions_decor;
    /** Graph-generation actions */
    ExplorerGenerateActions actions_gen;

    /** Creates new form GraphExplorerMain */
    public GraphExplorerMain() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) { }
        EditorRegistration.registerEditors();

        master = new GraphControllerMaster();
        master.addPropertyChangeListener(this);

        initActions();
        initComponents();

        for (Component c : toolbar.getComponents())
            if (c instanceof AbstractButton && ((AbstractButton)c).getIcon() != null)
                ((AbstractButton)c).setText(null);
        toolbar.add(javax.swing.Box.createHorizontalGlue());

        master.addActiveGraphListeners(mainTable, filterBar, metricBar1, metricBar2, metricPlot1);
        
        initMetricMenus();
        longMetricCP = new TimeMetricPlot();
    }

    private void initActions() {
        actions_io = new ExplorerIOActions(master);
        actions_gen = new ExplorerGenerateActions(master);

        actions_layout = new ExplorerLayoutActions(null); // needs single controller
        actions_stat = new ExplorerStatActions(null); // needs single controller
        actions_decor = new ExplorerDecorActions(null); // needs a single controller
    }
    
    /** Stores menu items corresponding to metrics */
    private EnumMap<StatEnum, JRadioButtonMenuItem> locMetricMI;

    private void initMetricMenus() {
        locMetricMI = new EnumMap<StatEnum, JRadioButtonMenuItem>(StatEnum.class);
        for (StatEnum se : StatEnum.values()) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(actions_stat.actionOf(se));
            metricMenuBG.add(mi);
            localMetricM.add(mi);
            locMetricMI.put(se, mi);
            if (se == StatEnum.NONE)
                mi.setSelected(true);
        }
        for (GlobalStatEnum se : GlobalStatEnum.values())
            globalMetricM.add(new JMenuItem(actions_stat.actionOf(se)));
    }


    // <editor-fold desc="GENERATED CODE">
    //
    // GENERATED CODE
    //

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        labelPS = new data.propertysheet.PropertySheet();
        edgePS = new data.propertysheet.PropertySheet();
        nodePS = new data.propertysheet.PropertySheet();
        layoutPS = new data.propertysheet.PropertySheet();
        newPM = new javax.swing.JPopupMenu();
        emptyMI1 = new javax.swing.JMenuItem();
        circleMI1 = new javax.swing.JMenuItem();
        starMI1 = new javax.swing.JMenuItem();
        wheelMI1 = new javax.swing.JMenuItem();
        completeMI1 = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        uniform1MI1 = new javax.swing.JMenuItem();
        sequenceMI1 = new javax.swing.JMenuItem();
        wattsMI1 = new javax.swing.JMenuItem();
        preferentialMI1 = new javax.swing.JMenuItem();
        proximityMI1 = new javax.swing.JMenuItem();
        metricMenuBG = new javax.swing.ButtonGroup();
        toolbar = new javax.swing.JToolBar();
        newTBB = new javax.swing.JButton();
        loadTBB = new javax.swing.JButton();
        saveTBB = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jButton1 = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        layoutCircleTBB = new javax.swing.JButton();
        layoutRandomTBB = new javax.swing.JButton();
        layoutStaticTBB = new javax.swing.JButton();
        layoutEnergyTBB = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jPanel1 = new javax.swing.JPanel();
        metricBar1 = new graphexplorer.views.GraphMetricBar();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        filterBar = new graphexplorer.views.GraphFilterBar();
        jPanel2 = new javax.swing.JPanel();
        jSeparator8 = new javax.swing.JToolBar.Separator();
        statusB = new javax.swing.JPanel();
        statusL = new javax.swing.JLabel();
        mainSP2 = new javax.swing.JSplitPane();
        mainSP = new javax.swing.JSplitPane();
        graphTP = new javax.swing.JTabbedPane();
        propertySP = new javax.swing.JScrollPane();
        propertyRP = new gui.RollupPanel();
        boxPanel = new javax.swing.JPanel();
        boxP1 = new javax.swing.JPanel();
        mainTableTB = new javax.swing.JToolBar();
        metricBar2 = new graphexplorer.views.GraphMetricBar();
        mainTableSP = new javax.swing.JScrollPane();
        mainTable = new graphexplorer.views.GraphTable();
        boxP2 = new javax.swing.JPanel();
        boxTP2 = new javax.swing.JTabbedPane();
        metricPlot1 = new graphexplorer.views.GraphStatPlot();
        distributionTableSP = new javax.swing.JScrollPane();
        distributionTable = new javax.swing.JTable();
        boxP3 = new javax.swing.JPanel();
        outputSP = new javax.swing.JScrollPane();
        outputTP = new javax.swing.JTextPane();
        menu = new javax.swing.JMenuBar();
        fileM = new javax.swing.JMenu();
        newM = new javax.swing.JMenu();
        emptyMI = new javax.swing.JMenuItem();
        circleMI = new javax.swing.JMenuItem();
        starMI = new javax.swing.JMenuItem();
        wheelMI = new javax.swing.JMenuItem();
        completeMI = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        uniform1MI = new javax.swing.JMenuItem();
        sequenceMI = new javax.swing.JMenuItem();
        wattsMI = new javax.swing.JMenuItem();
        preferentialMI = new javax.swing.JMenuItem();
        proximityMI = new javax.swing.JMenuItem();
        loadMI = new javax.swing.JMenuItem();
        saveMI = new javax.swing.JMenuItem();
        saveMI1 = new javax.swing.JMenuItem();
        closeMI = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        exportImageM = new javax.swing.JMenu();
        exportMovieM = new javax.swing.JMenu();
        export_movMI = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        quitMI = new javax.swing.JMenuItem();
        viewM = new javax.swing.JMenu();
        fitMI = new javax.swing.JMenuItem();
        fullScreenMI = new javax.swing.JMenuItem();
        layoutM = new javax.swing.JMenu();
        circularMI = new javax.swing.JMenuItem();
        randomMI = new javax.swing.JMenuItem();
        staticSpringMI = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        energyM = new javax.swing.JMenu();
        energyStartMI = new javax.swing.JMenuItem();
        timeStartMI = new javax.swing.JMenuItem();
        energyIterateMI = new javax.swing.JMenuItem();
        energyStopMI = new javax.swing.JMenuItem();
        metricM = new javax.swing.JMenu();
        localMetricM = new javax.swing.JMenu();
        globalMetricM = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        specialM = new javax.swing.JMenu();
        highlightMI = new javax.swing.JMenuItem();
        cooperationMI = new javax.swing.JMenuItem();
        primeMI = new javax.swing.JMenuItem();
        distinctColorMI = new javax.swing.JMenuItem();
        helpM = new javax.swing.JMenu();
        aboutMI = new javax.swing.JMenuItem();
        contentMI = new javax.swing.JMenuItem();

        emptyMI1.setAction(actions_gen.GENERATE_EMPTY);
        newPM.add(emptyMI1);

        circleMI1.setAction(actions_gen.GENERATE_CIRCLE);
        newPM.add(circleMI1);

        starMI1.setAction(actions_gen.GENERATE_STAR);
        newPM.add(starMI1);

        wheelMI1.setAction(actions_gen.GENERATE_WHEEL);
        newPM.add(wheelMI1);

        completeMI1.setAction(actions_gen.GENERATE_COMPLETE);
        newPM.add(completeMI1);
        newPM.add(jSeparator5);

        uniform1MI1.setAction(actions_gen.GENERATE_RANDOM);
        uniform1MI1.setText("Uniform (Erdos-Renyi) random graph...");
        newPM.add(uniform1MI1);

        sequenceMI1.setAction(actions_gen.GENERATE_SEQUENCE);
        sequenceMI1.setText("Degree sequence random graph...");
        newPM.add(sequenceMI1);

        wattsMI1.setAction(actions_gen.GENERATE_WS);
        wattsMI1.setText("Watts-Strogatz random graph...");
        newPM.add(wattsMI1);

        preferentialMI1.setAction(actions_gen.GENERATE_PREFERENTIAL);
        preferentialMI1.setText("Preferential Attachment...");
        newPM.add(preferentialMI1);

        proximityMI1.setAction(actions_gen.GENERATE_PROXIMITY);
        newPM.add(proximityMI1);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Graph Explorer");

        toolbar.setRollover(true);

        newTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/new-graph24.png"))); // NOI18N
        newTBB.setComponentPopupMenu(newPM);
        newTBB.setFocusable(false);
        newTBB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTBBActionPerformed(evt);
            }
        });
        toolbar.add(newTBB);

        loadTBB.setAction(actions_io.LOAD_ACTION);
        loadTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/load-graph24.png"))); // NOI18N
        loadTBB.setFocusable(false);
        toolbar.add(loadTBB);

        saveTBB.setAction(actions_io.SAVE_ACTION);
        saveTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/save-graph24.png"))); // NOI18N
        saveTBB.setFocusable(false);
        toolbar.add(saveTBB);
        toolbar.add(jSeparator2);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(GraphExplorerMain.class, this);
        jButton1.setAction(actionMap.get("fitToWindow")); // NOI18N
        jButton1.setText("");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton1);
        toolbar.add(jSeparator10);

        layoutCircleTBB.setAction(actionMap.get("layoutCircle")); // NOI18N
        layoutCircleTBB.setText("");
        layoutCircleTBB.setFocusable(false);
        toolbar.add(layoutCircleTBB);

        layoutRandomTBB.setAction(actionMap.get("layoutRandom")); // NOI18N
        layoutRandomTBB.setText("");
        layoutRandomTBB.setFocusable(false);
        toolbar.add(layoutRandomTBB);

        layoutStaticTBB.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        layoutStaticTBB.setText("");
        layoutStaticTBB.setFocusable(false);
        layoutStaticTBB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        layoutStaticTBB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(layoutStaticTBB);

        layoutEnergyTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play24.png"))); // NOI18N
        layoutEnergyTBB.setToolTipText("Start/stop animation of the spring layout algorithm for currently displayed graph.");
        layoutEnergyTBB.setFocusable(false);
        layoutEnergyTBB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layoutEnergyTBBActionPerformed(evt);
            }
        });
        toolbar.add(layoutEnergyTBB);
        toolbar.add(jSeparator3);

        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));
        toolbar.add(jPanel1);
        toolbar.add(metricBar1);
        toolbar.add(jSeparator9);
        toolbar.add(filterBar);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));
        toolbar.add(jPanel2);
        toolbar.add(jSeparator8);

        getContentPane().add(toolbar, java.awt.BorderLayout.NORTH);

        statusB.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));

        statusL.setFont(new java.awt.Font("Tahoma", 0, 12));
        statusL.setText("Status: ...");
        statusL.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        org.jdesktop.layout.GroupLayout statusBLayout = new org.jdesktop.layout.GroupLayout(statusB);
        statusB.setLayout(statusBLayout);
        statusBLayout.setHorizontalGroup(
            statusBLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusL, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1111, Short.MAX_VALUE)
        );
        statusBLayout.setVerticalGroup(
            statusBLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(statusL)
        );

        getContentPane().add(statusB, java.awt.BorderLayout.PAGE_END);

        mainSP2.setDividerSize(8);
        mainSP2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSP2.setResizeWeight(0.8);
        mainSP2.setOneTouchExpandable(true);

        mainSP.setDividerSize(8);
        mainSP.setOneTouchExpandable(true);

        graphTP.setPreferredSize(new java.awt.Dimension(800, 600));
        graphTP.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                graphTPStateChanged(evt);
            }
        });
        mainSP.setRightComponent(graphTP);

        propertySP.setPreferredSize(new java.awt.Dimension(200, 400));
        propertySP.setViewportView(propertyRP);

        mainSP.setLeftComponent(propertySP);

        mainSP2.setTopComponent(mainSP);

        boxPanel.setPreferredSize(new java.awt.Dimension(800, 250));
        boxPanel.setLayout(new javax.swing.BoxLayout(boxPanel, javax.swing.BoxLayout.LINE_AXIS));

        boxP1.setLayout(new java.awt.BorderLayout());

        mainTableTB.setFloatable(false);
        mainTableTB.setRollover(true);
        mainTableTB.add(metricBar2);

        boxP1.add(mainTableTB, java.awt.BorderLayout.PAGE_START);

        mainTableSP.setViewportView(mainTable);

        boxP1.add(mainTableSP, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP1);

        boxP2.setLayout(new java.awt.BorderLayout());

        boxTP2.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        boxTP2.addTab("Metric Plot", metricPlot1);

        distributionTable.setModel(metricPlot1.getTableModel());
        distributionTableSP.setViewportView(distributionTable);

        boxTP2.addTab("Metric Table", distributionTableSP);

        boxP2.add(boxTP2, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP2);

        boxP3.setLayout(new java.awt.BorderLayout());

        outputSP.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        outputTP.setBackground(new java.awt.Color(0, 0, 0));
        outputTP.setForeground(new java.awt.Color(204, 204, 255));
        outputTP.setText("== Output ==\n");
        outputTP.setPreferredSize(new java.awt.Dimension(500, 200));
        outputSP.setViewportView(outputTP);

        boxP3.add(outputSP, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP3);

        mainSP2.setBottomComponent(boxPanel);

        getContentPane().add(mainSP2, java.awt.BorderLayout.CENTER);

        fileM.setText("File");

        newM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/new-graph18.png"))); // NOI18N
        newM.setText("New Graph");

        emptyMI.setAction(actions_gen.GENERATE_EMPTY);
        emptyMI.setText("Empty graph...");
        newM.add(emptyMI);

        circleMI.setAction(actions_gen.GENERATE_CIRCLE);
        circleMI.setText("Circle graph...");
        newM.add(circleMI);

        starMI.setAction(actions_gen.GENERATE_STAR);
        starMI.setText("Star graph...");
        newM.add(starMI);

        wheelMI.setAction(actions_gen.GENERATE_WHEEL);
        wheelMI.setText("Wheel graph...");
        newM.add(wheelMI);

        completeMI.setAction(actions_gen.GENERATE_COMPLETE);
        completeMI.setText("Complete graph...");
        newM.add(completeMI);
        newM.add(jSeparator1);

        uniform1MI.setAction(actions_gen.GENERATE_RANDOM);
        uniform1MI.setText("Uniform (Erdos-Renyi) random graph...");
        newM.add(uniform1MI);

        sequenceMI.setAction(actions_gen.GENERATE_SEQUENCE);
        sequenceMI.setText("Degree sequence random graph...");
        newM.add(sequenceMI);

        wattsMI.setAction(actions_gen.GENERATE_WS);
        wattsMI.setText("Watts-Strogatz random graph...");
        newM.add(wattsMI);

        preferentialMI.setAction(actions_gen.GENERATE_PREFERENTIAL);
        preferentialMI.setText("Preferential Attachment...");
        newM.add(preferentialMI);

        proximityMI.setAction(actions_gen.GENERATE_PROXIMITY);
        newM.add(proximityMI);

        fileM.add(newM);

        loadMI.setAction(actions_io.LOAD_ACTION);
        loadMI.setText("Load graph from file");
        fileM.add(loadMI);

        saveMI.setAction(actions_io.SAVE_ACTION);
        saveMI.setText("Save graph");
        fileM.add(saveMI);

        saveMI1.setAction(actions_io.SAVE_AS_ACTION);
        saveMI1.setText("Save graph as ...");
        fileM.add(saveMI1);

        closeMI.setAction(actions_io.CLOSE_ACTION);
        closeMI.setText("Close current graph");
        fileM.add(closeMI);
        fileM.add(jSeparator7);

        exportImageM.setText("Export image");
        exportImageM.setEnabled(false);
        fileM.add(exportImageM);

        exportMovieM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/export-movie18.png"))); // NOI18N
        exportMovieM.setText("Export movie");
        exportMovieM.setEnabled(false);

        export_movMI.setEnabled(false);
        exportMovieM.add(export_movMI);

        fileM.add(exportMovieM);
        fileM.add(jSeparator6);

        quitMI.setAction(actionMap.get("quit")); // NOI18N
        fileM.add(quitMI);

        menu.add(fileM);

        viewM.setText("View");

        fitMI.setAction(actionMap.get("fitToWindow")); // NOI18N
        viewM.add(fitMI);

        fullScreenMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        fullScreenMI.setText("Full Screen");
        fullScreenMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullScreenMIActionPerformed(evt);
            }
        });
        viewM.add(fullScreenMI);

        menu.add(viewM);

        layoutM.setText("Layout");

        circularMI.setAction(actionMap.get("layoutCircle")); // NOI18N
        layoutM.add(circularMI);

        randomMI.setAction(actionMap.get("layoutRandom")); // NOI18N
        layoutM.add(randomMI);

        staticSpringMI.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        layoutM.add(staticSpringMI);
        layoutM.add(jSeparator4);

        energyM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/layout-spring18.png"))); // NOI18N
        energyM.setText("Animating layouts");

        energyStartMI.setAction(actions_layout.LAYOUT_ENERGY_START);
        energyStartMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play18.png"))); // NOI18N
        energyM.add(energyStartMI);

        timeStartMI.setAction(actions_layout.LAYOUT_TIME_START);
        timeStartMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play18.png"))); // NOI18N
        energyM.add(timeStartMI);

        layoutM.add(energyM);

        energyIterateMI.setAction(actions_layout.LAYOUT_ITERATE);
        layoutM.add(energyIterateMI);

        energyStopMI.setAction(actions_layout.LAYOUT_STOP);
        layoutM.add(energyStopMI);

        menu.add(layoutM);

        metricM.setText("Metrics");

        localMetricM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-node18.png"))); // NOI18N
        localMetricM.setText("Local");
        metricM.add(localMetricM);

        globalMetricM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-graph18.png"))); // NOI18N
        globalMetricM.setText("Global");
        metricM.add(globalMetricM);

        jMenuItem1.setAction(actions_stat.GLOBAL_STATS);
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-graph18.png"))); // NOI18N
        metricM.add(jMenuItem1);

        menu.add(metricM);

        specialM.setText("Special");

        highlightMI.setAction(actions_decor.HIGHLIGHT);
        specialM.add(highlightMI);

        cooperationMI.setAction(actions_stat.COOPERATION);
        specialM.add(cooperationMI);

        primeMI.setAction(actions_gen.GENERATE_PRIME);
        specialM.add(primeMI);

        distinctColorMI.setAction(actions_decor.DISTINCT_COLOR);
        specialM.add(distinctColorMI);

        menu.add(specialM);

        helpM.setText("Help");

        aboutMI.setAction(actionMap.get("showAbout")); // NOI18N
        helpM.add(aboutMI);

        contentMI.setAction(actionMap.get("showHelp")); // NOI18N
        helpM.add(contentMI);

        menu.add(helpM);

        setJMenuBar(menu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void graphTPStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_graphTPStateChanged
        // update active graph based on selected tab
        Component active = graphTP.getSelectedComponent();
        if (active == null)
            master.setActiveController(null);
        else
            for (Entry<GraphController, Component> en : tabs.entrySet())
                if (en.getValue() == active) {
                    master.setActiveController(en.getKey());
                    return;
                }
    }//GEN-LAST:event_graphTPStateChanged

    private void layoutEnergyTBBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_layoutEnergyTBBActionPerformed
        // this button swaps the current status of the layout between playing & paused
        GraphController gc = controller();
        if (gc != null) {
            if (gc.isLayoutAnimating())
                actions_layout.LAYOUT_STOP.actionPerformed(evt);
            else
                actions_layout.LAYOUT_ENERGY_START.actionPerformed(evt);
        } else {
            layoutEnergyTBB.setSelected(false);
        }
    }//GEN-LAST:event_layoutEnergyTBBActionPerformed

    private void newTBBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTBBActionPerformed
        newPM.show(newTBB, 5, 5);
    }//GEN-LAST:event_newTBBActionPerformed

    private void fullScreenMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullScreenMIActionPerformed
        GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (dev.getFullScreenWindow() == this) {
            setVisible(false);
            dispose();
            setUndecorated(false);
            setResizable(true);
            dev.setFullScreenWindow(null);
            setVisible(true);
        } else {
            setVisible(false);
            dispose();
            setUndecorated(true);
            setResizable(false);
            dev.setFullScreenWindow(this);
            setVisible(true);
        }
    }//GEN-LAST:event_fullScreenMIActionPerformed


    // <editor-fold defaultstate="collapsed" desc="VARIABLE DECLARATIONS">
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMI;
    private javax.swing.JPanel boxP1;
    private javax.swing.JPanel boxP2;
    private javax.swing.JPanel boxP3;
    private javax.swing.JPanel boxPanel;
    private javax.swing.JTabbedPane boxTP2;
    private javax.swing.JMenuItem circleMI;
    private javax.swing.JMenuItem circleMI1;
    private javax.swing.JMenuItem circularMI;
    private javax.swing.JMenuItem closeMI;
    private javax.swing.JMenuItem completeMI;
    private javax.swing.JMenuItem completeMI1;
    private javax.swing.JMenuItem contentMI;
    private javax.swing.JMenuItem cooperationMI;
    private javax.swing.JMenuItem distinctColorMI;
    private javax.swing.JTable distributionTable;
    private javax.swing.JScrollPane distributionTableSP;
    private data.propertysheet.PropertySheet edgePS;
    private javax.swing.JMenuItem emptyMI;
    private javax.swing.JMenuItem emptyMI1;
    private javax.swing.JMenuItem energyIterateMI;
    private javax.swing.JMenu energyM;
    private javax.swing.JMenuItem energyStartMI;
    private javax.swing.JMenuItem energyStopMI;
    private javax.swing.JMenu exportImageM;
    private javax.swing.JMenu exportMovieM;
    private javax.swing.JMenuItem export_movMI;
    private javax.swing.JMenu fileM;
    private graphexplorer.views.GraphFilterBar filterBar;
    private javax.swing.JMenuItem fitMI;
    private javax.swing.JMenuItem fullScreenMI;
    private javax.swing.JMenu globalMetricM;
    private javax.swing.JTabbedPane graphTP;
    private javax.swing.JMenu helpM;
    private javax.swing.JMenuItem highlightMI;
    private javax.swing.JButton jButton1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private data.propertysheet.PropertySheet labelPS;
    private javax.swing.JButton layoutCircleTBB;
    private javax.swing.JToggleButton layoutEnergyTBB;
    private javax.swing.JMenu layoutM;
    private data.propertysheet.PropertySheet layoutPS;
    private javax.swing.JButton layoutRandomTBB;
    private javax.swing.JButton layoutStaticTBB;
    private javax.swing.JMenuItem loadMI;
    private javax.swing.JButton loadTBB;
    private javax.swing.JMenu localMetricM;
    private javax.swing.JSplitPane mainSP;
    private javax.swing.JSplitPane mainSP2;
    private graphexplorer.views.GraphTable mainTable;
    private javax.swing.JScrollPane mainTableSP;
    private javax.swing.JToolBar mainTableTB;
    private javax.swing.JMenuBar menu;
    private graphexplorer.views.GraphMetricBar metricBar1;
    private graphexplorer.views.GraphMetricBar metricBar2;
    private javax.swing.JMenu metricM;
    private javax.swing.ButtonGroup metricMenuBG;
    private graphexplorer.views.GraphStatPlot metricPlot1;
    private javax.swing.JMenu newM;
    private javax.swing.JPopupMenu newPM;
    private javax.swing.JButton newTBB;
    private data.propertysheet.PropertySheet nodePS;
    private javax.swing.JScrollPane outputSP;
    private javax.swing.JTextPane outputTP;
    private javax.swing.JMenuItem preferentialMI;
    private javax.swing.JMenuItem preferentialMI1;
    private javax.swing.JMenuItem primeMI;
    private gui.RollupPanel propertyRP;
    private javax.swing.JScrollPane propertySP;
    private javax.swing.JMenuItem proximityMI;
    private javax.swing.JMenuItem proximityMI1;
    private javax.swing.JMenuItem quitMI;
    private javax.swing.JMenuItem randomMI;
    private javax.swing.JMenuItem saveMI;
    private javax.swing.JMenuItem saveMI1;
    private javax.swing.JButton saveTBB;
    private javax.swing.JMenuItem sequenceMI;
    private javax.swing.JMenuItem sequenceMI1;
    private javax.swing.JMenu specialM;
    private javax.swing.JMenuItem starMI;
    private javax.swing.JMenuItem starMI1;
    private javax.swing.JMenuItem staticSpringMI;
    private javax.swing.JPanel statusB;
    private javax.swing.JLabel statusL;
    private javax.swing.JMenuItem timeStartMI;
    private javax.swing.JToolBar toolbar;
    private javax.swing.JMenuItem uniform1MI;
    private javax.swing.JMenuItem uniform1MI1;
    private javax.swing.JMenu viewM;
    private javax.swing.JMenuItem wattsMI;
    private javax.swing.JMenuItem wattsMI1;
    private javax.swing.JMenuItem wheelMI;
    private javax.swing.JMenuItem wheelMI1;
    // End of variables declaration//GEN-END:variables
    // </editor-fold>

    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="GraphExplorerInterface METHODS">
    //
    // GraphExplorerInterface METHODS
    //
    public Component dialogComponent() {
        return this;
    }

    public void output(String output) {
        Document d = outputTP.getDocument();
        try {
            d.insertString(d.getLength(), output + "\n\n", null);
        } catch (BadLocationException ex) {
            Logger.getLogger(GraphExplorerMain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public GraphController controller() {
        return master.getActiveController();
    }

    public GraphComponent graphPlot() {
        GraphController gc = master.getActiveController();
        Component c = tabs.get(gc);
        if (c instanceof GraphComponent)
            return (GraphComponent) c;
        else if (c instanceof TimeGraphComponent) {
            return ((TimeGraphComponent)c).getGraphComponent();
        }
        return null;
    }
    // </editor-fold>


    //
    // UPDATE METHODS
    //

    /** Updates the property panel with currently active graph and energy layout */
    private void updatePropertyPanel() {
        // TODO - move this to a separate view
        propertyRP.removeAll();
        GraphController ac = master.getActiveController();
        GraphManager active = ac == null ? null : ac.getManager();

        if (active == null) {
            if (nodePS != null)
                nodePS.removeBeanChangeListener(this);
            if (edgePS != null)
                edgePS.removeBeanChangeListener(this);
            if (labelPS != null)
                labelPS.removeBeanChangeListener(this);
            nodePS = null;
            edgePS = null;
            labelPS = null;
            layoutPS = null;
        } else {
            propertyRP.add(new PropertySheet(new BackgroundBean()), "Background");
            try {
                PlaneGraphAdapter adapter = ac.getComponent().getAdapter();
                propertyRP.add(nodePS = new PropertySheet(adapter.getNodeRenderer()), "Nodes");
                propertyRP.add(edgePS = new PropertySheet(adapter.getEdgeRenderer()), "Edges");
                propertyRP.add(labelPS = new PropertySheet(adapter.getNodeLabelRenderer()), "Labels");
                nodePS.addBeanChangeListener(this);
                edgePS.addBeanChangeListener(this);
                labelPS.addBeanChangeListener(this);
//                VPointGraph vg = ac.getComponent().getAdapter().getViewGraph();
//                propertyRP.add(new PropertySheet(vg), "Graph");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
//            if (active instanceof PlaneGraph) {
//                PlaneGraphBean bean = new PlaneGraphBean((PlaneGraph)active);
//                propertyRP.add(nodePS = new PropertySheet(bean.nodeBean()), "Node Settings");
//                propertyRP.add(edgePS = new PropertySheet(bean.edgeBean()), "Edge Settings");
//                propertyRP.add(labelPS = new PropertySheet(bean.labelBean()), "Label Settings");
//            } else if (active instanceof ImageGraph) {
//                ImageGraphBean bean = new ImageGraphBean((ImageGraph)active);
//                propertyRP.add(nodePS = new PropertySheet(bean.nodeBean()), "Node Settings");
//                propertyRP.add(edgePS = new PropertySheet(bean.edgeBean()), "Edge Settings");
//            }
            IterativeGraphLayout layout = active.getLayoutAlgorithm();
            if (layout != null)
                propertyRP.add(layoutPS = new PropertySheet(layout), "Iterative Layout Settings");
            else
                layoutPS = null;
        }
    }

    /** Updates the longitudinal metric chart, provided the graph is longitudinal */
    private void updateLongChart() {
        GraphController gc = controller();
        if (gc != null && gc instanceof TimeGraphController) {
            longMetricCP.setController((TimeGraphController) gc);
            if (!Arrays.asList(boxTP2.getComponents()).contains(longMetricCP))
                boxTP2.add(longMetricCP, "Longitudinal Metric Chart");
        } else {
            longMetricCP.setController(null);
            boxTP2.remove(longMetricCP);
        }
    }

    /** Updates the currently active graph */
    private void updateActiveGraph() {
        // remove anything not in the master controller
        HashSet<GraphController> temp = new HashSet<GraphController>();
        for (GraphController gc : tabs.keySet())
            if (!master.containsController(gc))
                temp.add(gc);
        for (GraphController gc : temp) {
            gc.removeAllPropertyChangeListener(this);
            if (graphTP.indexOfComponent(tabs.get(gc)) != -1)
                graphTP.remove(tabs.get(gc));
            tabs.remove(gc);
            graphs.remove(gc);
        }

        // add anything from the master controller not already present
        temp.clear();
        for (GraphController gc : master.getControllers())
            if (!(tabs.containsKey(gc)))
                temp.add(gc);
        for (GraphController gc : temp) {
            Component c = null;

            if (gc instanceof TimeGraphController) {
                TimeGraphController tContr = (TimeGraphController) gc;
                TimeGraphComponent tComp = tContr.getTimeGraphComponent();
                graphs.put(gc, tComp.getGraphComponent());
                c = tComp;
            } else {
                c = gc.getComponent();
                graphs.put(gc, (GraphComponent) c);
            }
            tabs.put(gc, c);
            graphTP.add(c, gc.getName());
        }

        // set up for the active controller
        GraphController gc = controller();
        updating = true;
        if (graphTP.getSelectedComponent() != tabs.get(gc))
            graphTP.setSelectedComponent(tabs.get(gc));
        updatePropertyPanel();
        updateLongChart();

        if (gc != null) {
            gc.addAllPropertyChangeListener(this, true, false, false, true, false);
            exportImageM.setEnabled(true);
            exportImageM.removeAll();
            for (javax.swing.Action a : actions_io.imageActions(graphPlot()))
                exportImageM.add(a);
        } else {
            exportImageM.setEnabled(false);
            exportImageM.removeAll();
        }

        if (gc != null && gc instanceof TimeGraphController) {
            export_movMI.setAction(new ExplorerIOActions.MovieAction(gc, (TimeGraphComponent) tabs.get(gc)));
            exportMovieM.setEnabled(true);
        } else {
            export_movMI.setEnabled(false);
            exportMovieM.setEnabled(false);
        }

        actions_stat.setController(gc == null ? null : gc.getStatController());
        actions_decor.setController(gc == null ? null : gc.getDecorController());
        actions_layout.setController(gc == null ? null : gc);
        updating = false;
    }

    //
    // PROPERTY CHANGES FED FROM THE CONTROLLERS
    //

    boolean updating = false;

    int pcn = 0;

    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        
//        if (!prop.equals(GraphManager.$POSITIONS))
//            System.err.println("Property change " + (pcn++) + " src=" + evt.getSource() + " & name=" + evt.getPropertyName());
        
        if (prop.equals(GraphController.$OUTPUT))
            output((String) evt.getNewValue());
        else if (prop.equals(GraphController.$STATUS))
            statusL.setText((String) evt.getNewValue());
        else if (evt.getSource() == master) {
            if (prop.equals(GraphControllerMaster.$ACTIVE))
                updateActiveGraph();
            else
                System.err.println("GraphExplorerMain is not handling property change \"" + prop + "\" from " + evt);
        } else if (prop.equals(GraphManager.$ANIMATING)) {
            boolean animating = (Boolean) evt.getNewValue();
            layoutEnergyTBB.setSelected(animating);
            layoutEnergyTBB.setText(animating ? "Stop" : "Start");
        } else if (prop.equals(GraphManager.$LAYOUT_ALGORITHM)) {
            updatePropertyPanel();
        } else if (evt.getSource() == controller()) {
            GraphController gc = controller();
            if (prop.equals(GraphController.$NAME)) {
                Component c = tabs.get(gc);
                int index = graphTP.indexOfComponent(c);
                graphTP.setTitleAt(index, (String) evt.getNewValue());
            } else if (prop.equals(GraphStatController.$METRIC)) {
                if (gc == null)
                    locMetricMI.get(StatEnum.NONE).setSelected(true);
                else
                    locMetricMI.get(StatEnum.itemOf(gc.getMetric())).setSelected(true);
            } else if (prop.equals(GraphController.$BASE) 
                    || prop.equals(GraphController.$VIEWGRAPH)
                    || prop.equals(GraphFilterController.$FILTER_THRESHOLD)) {
                // IGNORE
            } else
                System.err.println("GraphExplorerMain is not handling controller PC \"" + prop + "\" from " + evt);
        } else if (prop.equals(GraphManager.$POSITIONS) || prop.equals(GraphManager.$GRAPH)) {
            // IGNORE
        } else if (evt.getSource() instanceof BeanEditorSupport) {
            graphPlot().getAdapter().appearanceChanged();
        } else
            System.err.println("GraphExplorerMain is not handling PC \"" + prop + "\" from " + evt);
    }

    
    // <editor-fold defaultstate="collapsed" desc="INNER CLASSES - BEANS">
    //
    // INNER CLASSES - BEANS
    //

    /** Provides a bean to access the background color of the active plot component */
    public class BackgroundBean {
        public Color getColor() { return graphPlot() == null ? Color.WHITE : graphPlot().getBackground(); }
        public void setColor(Color col) { if (graphPlot() != null) graphPlot().setBackground(col); }
    }
    // </editor-fold>

}