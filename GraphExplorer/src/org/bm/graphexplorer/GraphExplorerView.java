/*
 * GraphExplorerView.java
 * Created Aug 2011 (based almost entirely on earlier code)
 */

package org.bm.graphexplorer;

import org.bm.graphexplorer.controllers.MasterGraphController;
import org.bm.graphexplorer.controllers.TimeGraphController;
import org.bm.graphexplorer.controllers.GraphController;
import org.bm.graphexplorer.components.TimeMetricPlot;
import org.bm.graphexplorer.components.GraphListModel;
import org.bm.graphexplorer.components.FileEditorDialog;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.TaskMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.GraphServices;
import org.bm.blaise.scio.graph.layout.SpringLayout;
import org.bm.blaise.scio.graph.layout.StaticGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticSpringLayout;
import org.bm.blaise.scio.graph.metrics.DecayCentrality;
import org.bm.blaise.scio.graph.metrics.GlobalMetric;
import org.bm.blaise.scio.graph.metrics.NodeMetric;
import org.bm.blaise.specto.plane.PlanePlotComponent;
import org.bm.blaise.specto.plane.graph.GraphComponent;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import org.bm.blaise.specto.plane.graph.time.TimeGraphManager;
import org.bm.graphexplorer.controllers.GraphStatController.GlobalStatEnum;
import org.jdesktop.application.Task;

/**
 * The application's main frame.
 */
public class GraphExplorerView extends FrameView implements PropertyChangeListener {


    // <editor-fold defaultstate="collapsed" desc="VIEW MENU ACTIONS">
    //
    // VIEW MENU ACTIONS
    //

    private FileEditorDialog fileEditor;
    
    @Action(enabledProperty = "graphActive")
    public void showEditorDialog() {
        if (controller() != null) {
            if (fileEditor == null) {
                fileEditor = new FileEditorDialog(getFrame());
                fileEditor.setController(controller());
                ((GraphExplorerApp)getApplication()).addActiveControllerListener(fileEditor);
            }
            fileEditor.setVisible(true);
        }
    }    
    
    @Action
    public void saveEditorChanges() {
        JOptionPane.showMessageDialog(getFrame(), "Feature not yet implemented.");
    }
    
    @Action
    public void applyEditorChanges() {
        JOptionPane.showMessageDialog(getFrame(), "Feature not yet implemented.");
    }
    
    @Action(enabledProperty = "graphActive")
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
    
    @Action
    public void toggleFullScreen() {
        GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        JFrame frame = getFrame();
        
        if (dev.getFullScreenWindow() == frame) {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(false);
            frame.setResizable(true);
            dev.setFullScreenWindow(null);
            frame.setVisible(true);
        } else {
            frame.setVisible(false);
            frame.dispose();
            frame.setUndecorated(true);
            frame.setResizable(false);
            dev.setFullScreenWindow(frame);
            frame.setVisible(true);
        }        
    }
    
    // </editor-fold>


    //<editor-fold defaultstate="collapsed" desc="LAYOUT MENU ACTIONS">
    //
    // LAYOUT MENU ACTIONS
    //
    
    //<editor-fold defaultstate="collapsed" desc="BackgroundLayoutTask CLASS">
    /** Task that will perform a graph layout in the background */
    private class BackgroundLayoutTask extends Task<Void,Void> implements ActionListener {
        StaticGraphLayout layout;
        public BackgroundLayoutTask(StaticGraphLayout layout) { 
            super(GraphExplorerApp.getApplication()); 
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
    
    
    @Action(block = Task.BlockingScope.ACTION, enabledProperty = "graphActive")
    public Task layoutCircle() {
        return new BackgroundLayoutTask(StaticGraphLayout.CIRCLE);
    }
    
    @Action(block = Task.BlockingScope.ACTION, enabledProperty = "graphActive")
    public Task layoutRandom() {
        return new BackgroundLayoutTask(StaticGraphLayout.RANDOM);
    }
    
    @Action(block = Task.BlockingScope.APPLICATION, enabledProperty = "graphActive")
    public Task layoutSpringStatic() {
        return new BackgroundLayoutTask(StaticSpringLayout.getInstance());
    }   
    
    @Action(enabledProperty = "graphActive")
    public void layoutEnergyStart() {
        GraphController gc = controller();
        if (gc != null) {
            if (gc.getLayoutAlgorithm() == null)
                gc.setLayoutAlgorithm(new SpringLayout(gc.getNodePositions()));
            gc.setLayoutAnimating(true);
        }
    }
    
    @Action(enabledProperty = "graphActive")
    public void layoutEnergyIterate() {
        GraphController gc = controller();
        if (gc != null)
            gc.iterateLayout();
    }
    
    @Action(enabledProperty = "graphActive")
    public void layoutEnergyStop() {
        GraphController gc = controller();
        if (gc != null)
            gc.setLayoutAnimating(false);
    }
    
    @Action(enabledProperty = "timeGraphActive")
    public void layoutTimeSpringStart() {
        GraphController gc = controller();
        if (gc != null && gc instanceof TimeGraphController) {
            TimeGraphManager tgm = ((TimeGraphController)gc).getTimeGraphManager();
            tgm.initLayoutAlgorithm();
            tgm.setLayoutAnimating(true);
        }
    }
    
    @Action(enabledProperty = "graphActive")
    public void layoutPlayToolbar() {
        // this button swaps the current status of the layout between playing & paused
        GraphController gc = controller();
        if (gc != null) {
            if (gc.isLayoutAnimating())
                layoutEnergyStop();
            else
                layoutEnergyStart();
        } else {
            layoutEnergyTBB.setSelected(false);
        }
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="METRIC MENU ACTIONS">
    //
    // METRIC MENU ACTIONS
    //
    
    /** Task for computing a particular node metric */
    public javax.swing.Action getNodeMetricAction(final NodeMetric metric) {
        return new javax.swing.AbstractAction(metric.toString()) {
            { putValue(SHORT_DESCRIPTION, "Compute metric " + metric.toString() + " and update views."); }
            public void actionPerformed(ActionEvent e) {
                if (controller() != null)
                    controller().getStatController().setMetric(metric);
            }
        };
    }
    
    /** Task for computing a particular global metric */
    public javax.swing.Action getGlobalMetricAction(final GlobalMetric metric) {
        return new javax.swing.AbstractAction(metric.getName()) {
            { putValue(SHORT_DESCRIPTION, metric.getDescription()); }
            public void actionPerformed(ActionEvent e) {
                GraphExplorerApp.getApplication().getContext().getTaskService().execute(
                    new Task<Void,Void>(GraphExplorerApp.getApplication()) {
                        @Override protected Void doInBackground() throws Exception {
                            if (controller() != null) {
                                Graph g = controller().getStatController().getBaseGraph();
                                if (g != null) {
                                    Object value = metric.value(g);
                                    output("Value of metric " + metric.getName() + " on active graph: " + value);
                                }
                            }
                            return null;
                        }       
                    });
            }
        };
    }
    
    @Action(enabledProperty = "graphActive")
    public Task<Void,Void> computeCustomDecayCentralityStats() {
        return new Task<Void,Void>(GraphExplorerApp.getApplication()) {
            @Override
            protected Void doInBackground() throws Exception {
                if (controller() != null) {
                    float parameter = GraphExplorerApp.showFloatInputDialog("Enter parameter for decay centrality (between 0 and 1)", 0, 1);
                    if (parameter == -1) return null;
                    controller().getStatController().setMetric(new DecayCentrality(parameter));
                }
                return null;
            }
        };
    }
    
    @Action(enabledProperty = "graphActive")
    public Task<Void,Void> computeGlobalStats() {
        return new Task<Void,Void>(GraphExplorerApp.getApplication()) {
            @Override
            protected Void doInBackground() throws Exception {
                if (controller() != null) {
                    Component c = tabs.get(controller());
                    String name = graphTP.getTitleAt(graphTP.indexOfComponent(c));
                    controller().getStatController().computeGlobalStatistics(name);
                }
                return null;
            }
        };
    }
    
    @Action(enabledProperty = "graphActive")
    public Task<Void,Void> computeCooperationStats() {
        return new Task<Void,Void>(GraphExplorerApp.getApplication()) {
            @Override
            protected Void doInBackground() throws Exception {
                if (controller() != null)
                    controller().getStatController().computeCooperationScores();
                return null;
            }
        };
    }
    
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="SPECIAL MENU ACTIONS">
    //
    // SPECIAL MENU ACTIONS
    //
    
    @Action(enabledProperty = "graphActive")
    public void highlightSubset() {
        GraphController gc = controller();
        if (gc == null)
            return;
        
        GraphListModel glm = new GraphListModel(gc);
        JList jl = new JList(glm);
        jl.setLayoutOrientation(JList.VERTICAL_WRAP);
        jl.setVisibleRowCount(20);
        JScrollPane sp = new JScrollPane(jl);
        sp.getViewport().setPreferredSize(new Dimension(600,jl.getPreferredSize().height));
        int result = JOptionPane.showConfirmDialog(null, sp, "Select 1 or more nodes",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            int[] selected = jl.getSelectedIndices();
            TreeSet set = new TreeSet();
            for (int i : selected)
                set.add(glm.getNodeAt(i));
            gc.setHighlightNodes(set);
            highlightB.setSelected(!set.isEmpty());
        }
    }
    
    @Action(enabledProperty = "graphActive")
    public void distinctColors() {
        GraphController gc = controller();
        if (gc == null)
            return;
        gc.getDecorController().setDistinctColors(distinctB.isSelected());        
    }
    
    //</editor-fold>
    
    
    /** Controllers and associated components */
    HashMap<GraphController, Component> tabs = new HashMap<GraphController, Component>();
    /** Controllers and associated plane graph elements */
    HashMap<GraphController, GraphComponent> graphs = new HashMap<GraphController, GraphComponent>();
    /** Chart displaying longitudinal metric data */
    TimeMetricPlot longMetricCP;

    //<editor-fold defaultstate="collapsed" desc="INSTRUCTOR AND INITIALIZATION">
    //
    // INSTRUCTOR AND INITIALIZATION
    //
    
    /** Construct the view for the specified GraphExplorerApp application */
    public GraphExplorerView(SingleFrameApplication app) {
        super(app);
        initComponents();
        
        GraphExplorerApp mainApp = (GraphExplorerApp) app;        
        longMetricCP = new TimeMetricPlot();
        
        // update recent files
        mainApp.addPropertyChangeListener("recentFileActions", new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                javax.swing.Action[] actions = (javax.swing.Action[]) evt.getNewValue();
                recentMenu.removeAll();
                for (javax.swing.Action a : actions)
                    recentMenu.add(a);
            }
        });
        
        // update image export options
        for (AbstractAction ac : mainApp.createExportImageActions())
            exportImageMenu.add(ac);
        
        // update the metric menus
        initMetricMenus();

        // set up components that will change when the active graph changes
        mainApp.addActiveControllerListener(this);
        mainApp.addActiveControllerListener(mainTable);
        mainApp.addActiveControllerListener(filterBar);
        mainApp.addActiveControllerListener(metricBar1);
        mainApp.addActiveControllerListener(metricBar2);
        mainApp.addActiveControllerListener(metricPlot1);
        mainApp.addActiveControllerListener(graphProps);

        //<editor-fold defaultstate="collapsed" desc="status bar initialization">
        // status bar initialization - message timeout, idle icon and busy animation, etc
        ResourceMap resourceMap = getResourceMap();
        int messageTimeout = resourceMap.getInteger("StatusBar.messageTimeout");
        messageTimer = new Timer(messageTimeout, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                statusMessageLabel.setText("");
            }
        });
        messageTimer.setRepeats(false);
        int busyAnimationRate = resourceMap.getInteger("StatusBar.busyAnimationRate");
        for (int i = 0; i < busyIcons.length; i++) {
            busyIcons[i] = resourceMap.getIcon("StatusBar.busyIcons[" + i + "]");
        }
        busyIconTimer = new Timer(busyAnimationRate, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                busyIconIndex = (busyIconIndex + 1) % busyIcons.length;
                statusAnimationLabel.setIcon(busyIcons[busyIconIndex]);
            }
        });
        idleIcon = resourceMap.getIcon("StatusBar.idleIcon");
        statusAnimationLabel.setIcon(idleIcon);
        progressBar.setVisible(false);

        // connecting action tasks to status bar via TaskMonitor
        TaskMonitor taskMonitor = new TaskMonitor(getApplication().getContext());
        taskMonitor.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if ("started".equals(propertyName)) {
                    if (!busyIconTimer.isRunning()) {
                        statusAnimationLabel.setIcon(busyIcons[0]);
                        busyIconIndex = 0;
                        busyIconTimer.start();
                    }
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(true);
                } else if ("done".equals(propertyName)) {
                    busyIconTimer.stop();
                    statusAnimationLabel.setIcon(idleIcon);
                    progressBar.setVisible(false);
                    progressBar.setValue(0);
                } else if ("message".equals(propertyName)) {
                    String text = (String)(evt.getNewValue());
                    statusMessageLabel.setText((text == null) ? "" : text);
                    messageTimer.restart();
                } else if ("progress".equals(propertyName)) {
                    int value = (Integer)(evt.getNewValue());
                    progressBar.setVisible(true);
                    progressBar.setIndeterminate(false);
                    progressBar.setValue(value);
                }
            }
        });
        //</editor-fold>
    }
    
    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;
    
    /** Stores menu items corresponding to metrics */
    private final Map<NodeMetric, JRadioButtonMenuItem> locMetricMI = new HashMap<NodeMetric, JRadioButtonMenuItem>();

    private void initMetricMenus() {
        for (NodeMetric metric : GraphServices.getAvailableNodeMetrics()) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(getNodeMetricAction(metric));
            metricMenuBG.add(mi);
            localMetricMenu.add(mi);
            locMetricMI.put(metric, mi);
        }
        
        for (GlobalStatEnum se : GlobalStatEnum.values())
            globalMetricMenu.add(new JMenuItem(getGlobalMetricAction(se.getMetric())));
    }
    
    // </editor-fold>

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        bindingGroup = new org.jdesktop.beansbinding.BindingGroup();

        mainPanel = new javax.swing.JPanel();
        mainSP2 = new javax.swing.JSplitPane();
        mainSP = new javax.swing.JSplitPane();
        graphTP = new javax.swing.JTabbedPane();
        graphProps = new org.bm.graphexplorer.components.GraphStyleRollupPanel();
        boxPanel = new javax.swing.JPanel();
        boxP1 = new javax.swing.JPanel();
        mainTableTB = new javax.swing.JToolBar();
        metricBar2 = new org.bm.graphexplorer.components.GraphMetricBar();
        mainTableSP = new javax.swing.JScrollPane();
        mainTable = new org.bm.graphexplorer.components.GraphTable();
        boxP2 = new javax.swing.JPanel();
        boxTP2 = new javax.swing.JTabbedPane();
        metricPlot1 = new org.bm.graphexplorer.components.GraphStatPlot();
        distributionTableSP = new javax.swing.JScrollPane();
        distributionTable = new javax.swing.JTable();
        boxP3 = new javax.swing.JPanel();
        outputSP = new javax.swing.JScrollPane();
        outputTP = new javax.swing.JTextPane();
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
        menu = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenu = new javax.swing.JMenu();
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
        recentMenu = new javax.swing.JMenu();
        saveMI = new javax.swing.JMenuItem();
        saveAsMI = new javax.swing.JMenuItem();
        closeMI = new javax.swing.JMenuItem();
        jSeparator13 = new javax.swing.JPopupMenu.Separator();
        showEdMI = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        exportImageMenu = new javax.swing.JMenu();
        exportMovieMenu = new javax.swing.JMenu();
        export_movMI = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JPopupMenu.Separator();
        quitMI = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        fitMI = new javax.swing.JMenuItem();
        fullScreenMI = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        highlightB = new javax.swing.JCheckBoxMenuItem();
        distinctB = new javax.swing.JCheckBoxMenuItem();
        layoutMenu = new javax.swing.JMenu();
        circularMI = new javax.swing.JMenuItem();
        randomMI = new javax.swing.JMenuItem();
        staticSpringMI = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        energyStartMI = new javax.swing.JMenuItem();
        timeStartMI = new javax.swing.JMenuItem();
        jSeparator12 = new javax.swing.JPopupMenu.Separator();
        energyIterateMI = new javax.swing.JMenuItem();
        energyStopMI = new javax.swing.JMenuItem();
        metricsMenu = new javax.swing.JMenu();
        localMetricMenu = new javax.swing.JMenu();
        globalMetricMenu = new javax.swing.JMenu();
        globalStatsMI = new javax.swing.JMenuItem();
        specialMenu = new javax.swing.JMenu();
        cooperationMI = new javax.swing.JMenuItem();
        primeMI = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMI = new javax.swing.JMenuItem();
        contentMI = new javax.swing.JMenuItem();
        metricMenuBG = new javax.swing.ButtonGroup();
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
        toolbar = new javax.swing.JToolBar();
        newTBB = new javax.swing.JButton();
        loadTBB = new javax.swing.JButton();
        saveTBB = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        fitTBB = new javax.swing.JButton();
        jSeparator10 = new javax.swing.JToolBar.Separator();
        layoutCircleTBB = new javax.swing.JButton();
        layoutRandomTBB = new javax.swing.JButton();
        layoutStaticTBB = new javax.swing.JButton();
        layoutEnergyTBB = new javax.swing.JToggleButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        jPanel1 = new javax.swing.JPanel();
        metricBar1 = new org.bm.graphexplorer.components.GraphMetricBar();
        jSeparator9 = new javax.swing.JToolBar.Separator();
        filterBar = new org.bm.graphexplorer.components.GraphFilterBar();
        jPanel2 = new javax.swing.JPanel();
        jSeparator8 = new javax.swing.JToolBar.Separator();

        mainPanel.setName("mainPanel"); // NOI18N
        mainPanel.setLayout(new java.awt.BorderLayout());

        mainSP2.setDividerSize(8);
        mainSP2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        mainSP2.setResizeWeight(0.8);
        mainSP2.setName("mainSP2"); // NOI18N
        mainSP2.setOneTouchExpandable(true);

        mainSP.setDividerSize(8);
        mainSP.setName("mainSP"); // NOI18N
        mainSP.setOneTouchExpandable(true);

        graphTP.setName("graphTP"); // NOI18N
        graphTP.setPreferredSize(new java.awt.Dimension(800, 600));
        graphTP.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                graphTPStateChanged(evt);
            }
        });
        mainSP.setRightComponent(graphTP);

        graphProps.setName("graphProps"); // NOI18N
        mainSP.setLeftComponent(graphProps);

        mainSP2.setTopComponent(mainSP);

        boxPanel.setName("boxPanel"); // NOI18N
        boxPanel.setPreferredSize(new java.awt.Dimension(800, 250));
        boxPanel.setLayout(new javax.swing.BoxLayout(boxPanel, javax.swing.BoxLayout.LINE_AXIS));

        boxP1.setName("boxP1"); // NOI18N
        boxP1.setLayout(new java.awt.BorderLayout());

        mainTableTB.setFloatable(false);
        mainTableTB.setRollover(true);
        mainTableTB.setName("mainTableTB"); // NOI18N

        metricBar2.setName("metricBar2"); // NOI18N
        mainTableTB.add(metricBar2);

        boxP1.add(mainTableTB, java.awt.BorderLayout.PAGE_START);

        mainTableSP.setName("mainTableSP"); // NOI18N

        mainTable.setName("mainTable"); // NOI18N
        mainTableSP.setViewportView(mainTable);

        boxP1.add(mainTableSP, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP1);

        boxP2.setName("boxP2"); // NOI18N
        boxP2.setLayout(new java.awt.BorderLayout());

        boxTP2.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        boxTP2.setName("boxTP2"); // NOI18N

        metricPlot1.setName("metricPlot1"); // NOI18N
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance(org.bm.graphexplorer.GraphExplorerApp.class).getContext().getResourceMap(GraphExplorerView.class);
        boxTP2.addTab(resourceMap.getString("metricPlot1.TabConstraints.tabTitle"), metricPlot1); // NOI18N

        distributionTableSP.setName("distributionTableSP"); // NOI18N

        distributionTable.setModel(metricPlot1.getTableModel());
        distributionTable.setName("distributionTable"); // NOI18N
        distributionTableSP.setViewportView(distributionTable);

        boxTP2.addTab(resourceMap.getString("distributionTableSP.TabConstraints.tabTitle"), distributionTableSP); // NOI18N

        boxP2.add(boxTP2, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP2);

        boxP3.setName("boxP3"); // NOI18N
        boxP3.setLayout(new java.awt.BorderLayout());

        outputSP.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        outputSP.setName("outputSP"); // NOI18N

        outputTP.setBackground(resourceMap.getColor("outputTP.background")); // NOI18N
        outputTP.setForeground(resourceMap.getColor("outputTP.foreground")); // NOI18N
        outputTP.setText(resourceMap.getString("outputTP.text")); // NOI18N
        outputTP.setName("outputTP"); // NOI18N
        outputTP.setPreferredSize(new java.awt.Dimension(500, 200));
        outputSP.setViewportView(outputTP);

        boxP3.add(outputSP, java.awt.BorderLayout.CENTER);

        boxPanel.add(boxP3);

        mainSP2.setBottomComponent(boxPanel);

        mainPanel.add(mainSP2, java.awt.BorderLayout.CENTER);

        statusPanel.setName("statusPanel"); // NOI18N

        statusPanelSeparator.setName("statusPanelSeparator"); // NOI18N

        statusMessageLabel.setName("statusMessageLabel"); // NOI18N

        statusAnimationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        statusAnimationLabel.setName("statusAnimationLabel"); // NOI18N

        progressBar.setName("progressBar"); // NOI18N

        javax.swing.GroupLayout statusPanelLayout = new javax.swing.GroupLayout(statusPanel);
        statusPanel.setLayout(statusPanelLayout);
        statusPanelLayout.setHorizontalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(statusPanelSeparator, javax.swing.GroupLayout.DEFAULT_SIZE, 1058, Short.MAX_VALUE)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(statusMessageLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 883, Short.MAX_VALUE)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusAnimationLabel)
                .addContainerGap())
        );
        statusPanelLayout.setVerticalGroup(
            statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(statusPanelLayout.createSequentialGroup()
                .addComponent(statusPanelSeparator, javax.swing.GroupLayout.PREFERRED_SIZE, 2, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(statusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(statusMessageLabel)
                    .addComponent(statusAnimationLabel)
                    .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(3, 3, 3))
        );

        menu.setName("menu"); // NOI18N

        fileMenu.setText(resourceMap.getString("fileMenu.text")); // NOI18N
        fileMenu.setName("fileMenu"); // NOI18N

        newMenu.setIcon(resourceMap.getIcon("newMenu.icon")); // NOI18N
        newMenu.setText(resourceMap.getString("newMenu.text")); // NOI18N
        newMenu.setName("newMenu"); // NOI18N

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance(org.bm.graphexplorer.GraphExplorerApp.class).getContext().getActionMap(GraphExplorerView.class, this);
        emptyMI.setAction(actionMap.get("newEmptyGraph")); // NOI18N
        emptyMI.setName("emptyMI"); // NOI18N
        newMenu.add(emptyMI);

        circleMI.setAction(actionMap.get("newCircleGraph")); // NOI18N
        circleMI.setName("circleMI"); // NOI18N
        newMenu.add(circleMI);

        starMI.setAction(actionMap.get("newStarGraph")); // NOI18N
        starMI.setName("starMI"); // NOI18N
        newMenu.add(starMI);

        wheelMI.setAction(actionMap.get("newWheelGraph")); // NOI18N
        wheelMI.setName("wheelMI"); // NOI18N
        newMenu.add(wheelMI);

        completeMI.setAction(actionMap.get("newCompleteGraph")); // NOI18N
        completeMI.setName("completeMI"); // NOI18N
        newMenu.add(completeMI);

        jSeparator1.setName("jSeparator1"); // NOI18N
        newMenu.add(jSeparator1);

        uniform1MI.setAction(actionMap.get("newRandomGraph")); // NOI18N
        uniform1MI.setName("uniform1MI"); // NOI18N
        newMenu.add(uniform1MI);

        sequenceMI.setAction(actionMap.get("newDegreeSequenceGraph")); // NOI18N
        sequenceMI.setName("sequenceMI"); // NOI18N
        newMenu.add(sequenceMI);

        wattsMI.setAction(actionMap.get("newWattsStrogatzGraph")); // NOI18N
        wattsMI.setName("wattsMI"); // NOI18N
        newMenu.add(wattsMI);

        preferentialMI.setAction(actionMap.get("newPrefRandomGraph")); // NOI18N
        preferentialMI.setName("preferentialMI"); // NOI18N
        newMenu.add(preferentialMI);

        proximityMI.setAction(actionMap.get("newProximityGraph")); // NOI18N
        proximityMI.setName("proximityMI"); // NOI18N
        newMenu.add(proximityMI);

        fileMenu.add(newMenu);

        loadMI.setAction(actionMap.get("loadGraph")); // NOI18N
        loadMI.setName("loadMI"); // NOI18N
        fileMenu.add(loadMI);

        recentMenu.setIcon(resourceMap.getIcon("recentMenu.icon")); // NOI18N
        recentMenu.setText(resourceMap.getString("recentMenu.text")); // NOI18N
        recentMenu.setName("recentMenu"); // NOI18N
        fileMenu.add(recentMenu);

        saveMI.setAction(actionMap.get("saveGraph")); // NOI18N
        saveMI.setName("saveMI"); // NOI18N
        fileMenu.add(saveMI);

        saveAsMI.setAction(actionMap.get("saveGraphAs")); // NOI18N
        saveAsMI.setName("saveAsMI"); // NOI18N
        fileMenu.add(saveAsMI);

        closeMI.setAction(actionMap.get("closeGraph")); // NOI18N
        closeMI.setName("closeMI"); // NOI18N
        fileMenu.add(closeMI);

        jSeparator13.setName("jSeparator13"); // NOI18N
        fileMenu.add(jSeparator13);

        showEdMI.setAction(actionMap.get("showEditorDialog")); // NOI18N
        showEdMI.setName("showEdMI"); // NOI18N
        fileMenu.add(showEdMI);

        jSeparator7.setName("jSeparator7"); // NOI18N
        fileMenu.add(jSeparator7);

        exportImageMenu.setText(resourceMap.getString("exportImageMenu.text")); // NOI18N
        exportImageMenu.setName("exportImageMenu"); // NOI18N

        org.jdesktop.beansbinding.Binding binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${graphActive}"), exportImageMenu, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        fileMenu.add(exportImageMenu);

        exportMovieMenu.setIcon(resourceMap.getIcon("exportMovieMenu.icon")); // NOI18N
        exportMovieMenu.setText(resourceMap.getString("exportMovieMenu.text")); // NOI18N
        exportMovieMenu.setName("exportMovieMenu"); // NOI18N

        binding = org.jdesktop.beansbinding.Bindings.createAutoBinding(org.jdesktop.beansbinding.AutoBinding.UpdateStrategy.READ_WRITE, this, org.jdesktop.beansbinding.ELProperty.create("${timeGraphActive}"), exportMovieMenu, org.jdesktop.beansbinding.BeanProperty.create("enabled"));
        bindingGroup.addBinding(binding);

        export_movMI.setAction(actionMap.get("exportMovie")); // NOI18N
        export_movMI.setName("export_movMI"); // NOI18N
        exportMovieMenu.add(export_movMI);

        fileMenu.add(exportMovieMenu);

        jSeparator6.setName("jSeparator6"); // NOI18N
        fileMenu.add(jSeparator6);

        quitMI.setAction(actionMap.get("quit")); // NOI18N
        quitMI.setName("quitMI"); // NOI18N
        fileMenu.add(quitMI);

        menu.add(fileMenu);

        viewMenu.setText(resourceMap.getString("viewMenu.text")); // NOI18N
        viewMenu.setName("viewMenu"); // NOI18N

        fitMI.setAction(actionMap.get("fitToWindow")); // NOI18N
        fitMI.setName("fitMI"); // NOI18N
        viewMenu.add(fitMI);

        fullScreenMI.setAction(actionMap.get("toggleFullScreen")); // NOI18N
        fullScreenMI.setName("fullScreenMI"); // NOI18N
        viewMenu.add(fullScreenMI);

        jSeparator11.setName("jSeparator11"); // NOI18N
        viewMenu.add(jSeparator11);

        highlightB.setAction(actionMap.get("highlightSubset")); // NOI18N
        highlightB.setName("highlightB"); // NOI18N
        viewMenu.add(highlightB);

        distinctB.setAction(actionMap.get("distinctColors")); // NOI18N
        distinctB.setName("distinctB"); // NOI18N
        viewMenu.add(distinctB);

        menu.add(viewMenu);

        layoutMenu.setText(resourceMap.getString("layoutMenu.text")); // NOI18N
        layoutMenu.setName("layoutMenu"); // NOI18N

        circularMI.setAction(actionMap.get("layoutCircle")); // NOI18N
        circularMI.setName("circularMI"); // NOI18N
        layoutMenu.add(circularMI);

        randomMI.setAction(actionMap.get("layoutRandom")); // NOI18N
        randomMI.setName("randomMI"); // NOI18N
        layoutMenu.add(randomMI);

        staticSpringMI.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        staticSpringMI.setName("staticSpringMI"); // NOI18N
        layoutMenu.add(staticSpringMI);

        jSeparator4.setName("jSeparator4"); // NOI18N
        layoutMenu.add(jSeparator4);

        energyStartMI.setAction(actionMap.get("layoutEnergyStart")); // NOI18N
        energyStartMI.setName("energyStartMI"); // NOI18N
        layoutMenu.add(energyStartMI);

        timeStartMI.setAction(actionMap.get("layoutTimeSpringStart")); // NOI18N
        timeStartMI.setIcon(resourceMap.getIcon("timeStartMI.icon")); // NOI18N
        timeStartMI.setName("timeStartMI"); // NOI18N
        layoutMenu.add(timeStartMI);

        jSeparator12.setName("jSeparator12"); // NOI18N
        layoutMenu.add(jSeparator12);

        energyIterateMI.setAction(actionMap.get("layoutEnergyIterate")); // NOI18N
        energyIterateMI.setName("energyIterateMI"); // NOI18N
        layoutMenu.add(energyIterateMI);

        energyStopMI.setAction(actionMap.get("layoutEnergyStop")); // NOI18N
        energyStopMI.setName("energyStopMI"); // NOI18N
        layoutMenu.add(energyStopMI);

        menu.add(layoutMenu);

        metricsMenu.setText(resourceMap.getString("metricsMenu.text")); // NOI18N
        metricsMenu.setName("metricsMenu"); // NOI18N

        localMetricMenu.setText(resourceMap.getString("localMetricMenu.text")); // NOI18N
        localMetricMenu.setEnabled(false);
        localMetricMenu.setName("localMetricMenu"); // NOI18N
        metricsMenu.add(localMetricMenu);

        globalMetricMenu.setText(resourceMap.getString("globalMetricMenu.text")); // NOI18N
        globalMetricMenu.setEnabled(false);
        globalMetricMenu.setName("globalMetricMenu"); // NOI18N
        metricsMenu.add(globalMetricMenu);

        globalStatsMI.setAction(actionMap.get("computeGlobalStats")); // NOI18N
        globalStatsMI.setName("globalStatsMI"); // NOI18N
        metricsMenu.add(globalStatsMI);

        menu.add(metricsMenu);

        specialMenu.setText(resourceMap.getString("specialMenu.text")); // NOI18N
        specialMenu.setName("specialMenu"); // NOI18N

        cooperationMI.setAction(actionMap.get("computeCooperationStats")); // NOI18N
        cooperationMI.setName("cooperationMI"); // NOI18N
        specialMenu.add(cooperationMI);

        primeMI.setAction(actionMap.get("newPrimeGraph")); // NOI18N
        primeMI.setName("primeMI"); // NOI18N
        specialMenu.add(primeMI);

        menu.add(specialMenu);

        helpMenu.setText(resourceMap.getString("helpMenu.text")); // NOI18N
        helpMenu.setName("helpMenu"); // NOI18N

        aboutMI.setAction(actionMap.get("showAbout")); // NOI18N
        aboutMI.setName("aboutMI"); // NOI18N
        helpMenu.add(aboutMI);

        contentMI.setAction(actionMap.get("showHelp")); // NOI18N
        contentMI.setName("contentMI"); // NOI18N
        helpMenu.add(contentMI);

        menu.add(helpMenu);

        newPM.setName("newPM"); // NOI18N

        emptyMI1.setAction(actionMap.get("newEmptyGraph")); // NOI18N
        emptyMI1.setName("emptyMI1"); // NOI18N
        newPM.add(emptyMI1);

        circleMI1.setAction(actionMap.get("newCircleGraph")); // NOI18N
        circleMI1.setName("circleMI1"); // NOI18N
        newPM.add(circleMI1);

        starMI1.setAction(actionMap.get("newStarGraph")); // NOI18N
        starMI1.setName("starMI1"); // NOI18N
        newPM.add(starMI1);

        wheelMI1.setAction(actionMap.get("newWheelGraph")); // NOI18N
        wheelMI1.setName("wheelMI1"); // NOI18N
        newPM.add(wheelMI1);

        completeMI1.setAction(actionMap.get("newCompleteGraph")); // NOI18N
        completeMI1.setName("completeMI1"); // NOI18N
        newPM.add(completeMI1);

        jSeparator5.setName("jSeparator5"); // NOI18N
        newPM.add(jSeparator5);

        uniform1MI1.setAction(actionMap.get("newRandomGraph")); // NOI18N
        uniform1MI1.setName("uniform1MI1"); // NOI18N
        newPM.add(uniform1MI1);

        sequenceMI1.setAction(actionMap.get("newDegreeSequenceGraph")); // NOI18N
        sequenceMI1.setName("sequenceMI1"); // NOI18N
        newPM.add(sequenceMI1);

        wattsMI1.setAction(actionMap.get("newWattsStrogatzGraph")); // NOI18N
        wattsMI1.setName("wattsMI1"); // NOI18N
        newPM.add(wattsMI1);

        preferentialMI1.setAction(actionMap.get("newPrefRandomGraph")); // NOI18N
        preferentialMI1.setName("preferentialMI1"); // NOI18N
        newPM.add(preferentialMI1);

        proximityMI1.setAction(actionMap.get("newProximityGraph")); // NOI18N
        proximityMI1.setName("proximityMI1"); // NOI18N
        newPM.add(proximityMI1);

        toolbar.setRollover(true);
        toolbar.setName("toolbar"); // NOI18N

        newTBB.setIcon(resourceMap.getIcon("newTBB.icon")); // NOI18N
        newTBB.setFocusable(false);
        newTBB.setName("newTBB"); // NOI18N
        newTBB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTBBActionPerformed(evt);
            }
        });
        toolbar.add(newTBB);

        loadTBB.setAction(actionMap.get("loadGraph")); // NOI18N
        loadTBB.setFocusable(false);
        loadTBB.setHideActionText(true);
        loadTBB.setName("loadTBB"); // NOI18N
        toolbar.add(loadTBB);

        saveTBB.setAction(actionMap.get("saveGraph")); // NOI18N
        saveTBB.setFocusable(false);
        saveTBB.setHideActionText(true);
        saveTBB.setName("saveTBB"); // NOI18N
        toolbar.add(saveTBB);

        jSeparator2.setName("jSeparator2"); // NOI18N
        toolbar.add(jSeparator2);

        fitTBB.setAction(actionMap.get("fitToWindow")); // NOI18N
        fitTBB.setFocusable(false);
        fitTBB.setHideActionText(true);
        fitTBB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        fitTBB.setName("fitTBB"); // NOI18N
        fitTBB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(fitTBB);

        jSeparator10.setName("jSeparator10"); // NOI18N
        toolbar.add(jSeparator10);

        layoutCircleTBB.setAction(actionMap.get("layoutCircle")); // NOI18N
        layoutCircleTBB.setFocusable(false);
        layoutCircleTBB.setHideActionText(true);
        layoutCircleTBB.setName("layoutCircleTBB"); // NOI18N
        toolbar.add(layoutCircleTBB);

        layoutRandomTBB.setAction(actionMap.get("layoutRandom")); // NOI18N
        layoutRandomTBB.setFocusable(false);
        layoutRandomTBB.setHideActionText(true);
        layoutRandomTBB.setName("layoutRandomTBB"); // NOI18N
        toolbar.add(layoutRandomTBB);

        layoutStaticTBB.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        layoutStaticTBB.setFocusable(false);
        layoutStaticTBB.setHideActionText(true);
        layoutStaticTBB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        layoutStaticTBB.setName("layoutStaticTBB"); // NOI18N
        layoutStaticTBB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(layoutStaticTBB);

        layoutEnergyTBB.setAction(actionMap.get("layoutPlayToolbar")); // NOI18N
        layoutEnergyTBB.setFocusable(false);
        layoutEnergyTBB.setHideActionText(true);
        layoutEnergyTBB.setName("layoutEnergyTBB"); // NOI18N
        toolbar.add(layoutEnergyTBB);

        jSeparator3.setName("jSeparator3"); // NOI18N
        toolbar.add(jSeparator3);

        jPanel1.setName("jPanel1"); // NOI18N
        jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.X_AXIS));
        toolbar.add(jPanel1);

        metricBar1.setName("metricBar1"); // NOI18N
        toolbar.add(metricBar1);

        jSeparator9.setName("jSeparator9"); // NOI18N
        toolbar.add(jSeparator9);

        filterBar.setName("filterBar"); // NOI18N
        toolbar.add(filterBar);

        jPanel2.setName("jPanel2"); // NOI18N
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));
        toolbar.add(jPanel2);

        jSeparator8.setName("jSeparator8"); // NOI18N
        toolbar.add(jSeparator8);

        setComponent(mainPanel);
        setMenuBar(menu);
        setStatusBar(statusPanel);
        setToolBar(toolbar);

        bindingGroup.bind();
    }// </editor-fold>//GEN-END:initComponents

private void newTBBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTBBActionPerformed
        newPM.show(newTBB, 5, 5);
}//GEN-LAST:event_newTBBActionPerformed

private void graphTPStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_graphTPStateChanged
        // update active graph based on selected tab
        Component compSelected = graphTP.getSelectedComponent();
        GraphController selected = null;
        if (compSelected != null)
            for (Entry<GraphController, Component> en : tabs.entrySet())
                if (en.getValue() == compSelected) {
                    selected = en.getKey();
                    break;
                }
        ((GraphExplorerApp)getApplication()).getMasterController().setActiveController(selected);
}//GEN-LAST:event_graphTPStateChanged


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
    private javax.swing.JCheckBoxMenuItem distinctB;
    private javax.swing.JTable distributionTable;
    private javax.swing.JScrollPane distributionTableSP;
    private javax.swing.JMenuItem emptyMI;
    private javax.swing.JMenuItem emptyMI1;
    private javax.swing.JMenuItem energyIterateMI;
    private javax.swing.JMenuItem energyStartMI;
    private javax.swing.JMenuItem energyStopMI;
    private javax.swing.JMenu exportImageMenu;
    private javax.swing.JMenu exportMovieMenu;
    private javax.swing.JMenuItem export_movMI;
    private javax.swing.JMenu fileMenu;
    private org.bm.graphexplorer.components.GraphFilterBar filterBar;
    private javax.swing.JMenuItem fitMI;
    private javax.swing.JButton fitTBB;
    private javax.swing.JMenuItem fullScreenMI;
    private javax.swing.JMenu globalMetricMenu;
    private javax.swing.JMenuItem globalStatsMI;
    private org.bm.graphexplorer.components.GraphStyleRollupPanel graphProps;
    private javax.swing.JTabbedPane graphTP;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem highlightB;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
    private javax.swing.JPopupMenu.Separator jSeparator12;
    private javax.swing.JPopupMenu.Separator jSeparator13;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JPopupMenu.Separator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JToolBar.Separator jSeparator8;
    private javax.swing.JToolBar.Separator jSeparator9;
    private javax.swing.JButton layoutCircleTBB;
    private javax.swing.JToggleButton layoutEnergyTBB;
    private javax.swing.JMenu layoutMenu;
    private javax.swing.JButton layoutRandomTBB;
    private javax.swing.JButton layoutStaticTBB;
    private javax.swing.JMenuItem loadMI;
    private javax.swing.JButton loadTBB;
    private javax.swing.JMenu localMetricMenu;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JSplitPane mainSP;
    private javax.swing.JSplitPane mainSP2;
    private org.bm.graphexplorer.components.GraphTable mainTable;
    private javax.swing.JScrollPane mainTableSP;
    private javax.swing.JToolBar mainTableTB;
    private javax.swing.JMenuBar menu;
    private org.bm.graphexplorer.components.GraphMetricBar metricBar1;
    private org.bm.graphexplorer.components.GraphMetricBar metricBar2;
    private javax.swing.ButtonGroup metricMenuBG;
    private org.bm.graphexplorer.components.GraphStatPlot metricPlot1;
    private javax.swing.JMenu metricsMenu;
    private javax.swing.JMenu newMenu;
    private javax.swing.JPopupMenu newPM;
    private javax.swing.JButton newTBB;
    private javax.swing.JScrollPane outputSP;
    private javax.swing.JTextPane outputTP;
    private javax.swing.JMenuItem preferentialMI;
    private javax.swing.JMenuItem preferentialMI1;
    private javax.swing.JMenuItem primeMI;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JMenuItem proximityMI;
    private javax.swing.JMenuItem proximityMI1;
    private javax.swing.JMenuItem quitMI;
    private javax.swing.JMenuItem randomMI;
    private javax.swing.JMenu recentMenu;
    private javax.swing.JMenuItem saveAsMI;
    private javax.swing.JMenuItem saveMI;
    private javax.swing.JButton saveTBB;
    private javax.swing.JMenuItem sequenceMI;
    private javax.swing.JMenuItem sequenceMI1;
    private javax.swing.JMenuItem showEdMI;
    private javax.swing.JMenu specialMenu;
    private javax.swing.JMenuItem starMI;
    private javax.swing.JMenuItem starMI1;
    private javax.swing.JMenuItem staticSpringMI;
    private javax.swing.JLabel statusAnimationLabel;
    private javax.swing.JLabel statusMessageLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.JMenuItem timeStartMI;
    private javax.swing.JToolBar toolbar;
    private javax.swing.JMenuItem uniform1MI;
    private javax.swing.JMenuItem uniform1MI1;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JMenuItem wattsMI;
    private javax.swing.JMenuItem wattsMI1;
    private javax.swing.JMenuItem wheelMI;
    private javax.swing.JMenuItem wheelMI1;
    private org.jdesktop.beansbinding.BindingGroup bindingGroup;
    // End of variables declaration//GEN-END:variables

    
    //<editor-fold defaultstate="collapsed" desc="PROPERTY PATTERNS">
    //
    // PROPERTY PATTERNS
    //
    
    private boolean active = false;
    public boolean isGraphActive() { 
        return active; 
    }
    public void setGraphActive(boolean val) {
        if (active != val) {
            active = val;
            // this event firing will automatically enable/disable most components
            localMetricMenu.setEnabled(val);
            globalMetricMenu.setEnabled(val);
            firePropertyChange("graphActive", !val, val);
        }
    }
    
    private boolean timeGraphActive = false;
    public boolean isTimeGraphActive() {
        return timeGraphActive;
    }

    public void setTimeGraphActive(boolean b) {
        boolean old = isTimeGraphActive();
        this.timeGraphActive = b;
        firePropertyChange("timeGraphActive", old, isTimeGraphActive());
    }
    
    //</editor-fold>

    
    //
    // GraphExplorerInterface METHODS
    //

    public void output(String output) {
        Document d = outputTP.getDocument();
        try {
            d.insertString(d.getLength(), output + "\n", null);
        } catch (BadLocationException ex) {
            Logger.getLogger(GraphExplorerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public GraphController controller() {
        return ((GraphExplorerApp)getApplication()).getActiveController();
    }

    public GraphComponent graphPlot() {
        GraphController gc = controller();
        Component c = tabs.get(gc);
        if (c instanceof GraphComponent)
            return (GraphComponent) c;
        else if (c instanceof TimeGraphComponent) {
            return ((TimeGraphComponent)c).getGraphComponent();
        }
        return null;
    }

    
    //
    // UPDATE METHODS
    //

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

    private GraphController activeC = null;
    
    /** Updates the currently active graph */
    private synchronized void updateActiveGraph() {
        MasterGraphController master = ((GraphExplorerApp)getApplication()).getMasterController();

        if (activeC != null)
            activeC.removeAllPropertyChangeListener(this);
        
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
        if (graphTP.getSelectedComponent() != tabs.get(gc))
            graphTP.setSelectedComponent(tabs.get(gc));
        updateLongChart();

        activeC = gc;
        if (activeC != null)
            activeC.addAllPropertyChangeListener(this, true, false, true, true, false);

        setGraphActive(activeC != null && activeC.getBaseGraph() != null);
        setTimeGraphActive(activeC != null && activeC instanceof TimeGraphController);
        
        if (activeC.getMetric() == null)
            metricMenuBG.clearSelection();
        else
            locMetricMI.get(activeC.getMetric()).setSelected(true);
    }

    //
    // PROPERTY CHANGES FED FROM THE CONTROLLERS
    //

    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        
        if (prop.equals("output"))
            output((String) evt.getNewValue());
        else if (prop.equals("status"))
            statusMessageLabel.setText((String) evt.getNewValue());
        else if (prop.equals("activeController")) {
            updateActiveGraph();
        } else if (prop.equals("layoutAnimating")) {
            boolean animating = (Boolean) evt.getNewValue();
            layoutEnergyTBB.setSelected(animating);
            layoutEnergyTBB.setText(animating ? "Stop" : "Start");
        } else if (prop.equals("metric")) {
            locMetricMI.get(controller().getMetric()).setSelected(true);
        } else if (evt.getSource() == controller()) {
            GraphController gc = controller();
            if (prop.equals("name")) {
                Component c = tabs.get(gc);
                int index = graphTP.indexOfComponent(c);
                graphTP.setTitleAt(index, (String) evt.getNewValue());
            } else if (prop.equals("baseGraph") 
                    || prop.equals("viewGraph")
                    || prop.equals("filterThreshold")
                    || prop.equals("file")
                    || prop.equals("layoutAlgorithm")) {
                // IGNORE
            }
        }
    }

}
