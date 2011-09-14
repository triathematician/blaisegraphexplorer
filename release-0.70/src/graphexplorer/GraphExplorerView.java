/*
 * TestSAFView.java
 */

package graphexplorer;

import data.propertysheet.BeanEditorSupport;
import data.propertysheet.PropertySheet;
import graphexplorer.actions.ExplorerGenerateActions;
import graphexplorer.actions.ExplorerLayoutActions;
import graphexplorer.actions.ExplorerStatActions;
import graphexplorer.actions.ExplorerStatActions.GlobalStatEnum;
import graphexplorer.actions.ExplorerStatActions.StatEnum;
import graphexplorer.controller.GraphController;
import graphexplorer.controller.GraphControllerMaster;
import graphexplorer.controller.GraphFilterController;
import graphexplorer.controller.GraphStatController;
import graphexplorer.controller.TimeGraphController;
import graphexplorer.io.ExplorerIOActions;
import graphexplorer.views.GraphListModel;
import graphexplorer.views.TimeMetricPlot;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import org.bm.blaise.graphics.renderer.Anchor;
import org.bm.blaise.graphics.renderer.StringRenderer;
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
import java.net.URL;
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
import javax.swing.AbstractButton;
import javax.swing.Timer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.bm.blaise.graphics.renderer.BasicStringRenderer;
import org.bm.blaise.scio.graph.layout.IterativeGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticSpringLayout;
import org.bm.blaise.specto.plane.PlanePlotComponent;
import org.bm.blaise.specto.plane.graph.GraphComponent;
import org.bm.blaise.specto.plane.graph.GraphManager;
import org.bm.blaise.specto.plane.graph.PlaneGraphAdapter;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import org.jdesktop.application.Task;

/**
 * The application's main frame.
 */
public class GraphExplorerView extends FrameView implements GraphExplorerInterface, PropertyChangeListener {

    /** Loads an image icon for the specified relative path */
    @Deprecated
    public static ImageIcon loadIcon(String path) {
        URL url = GraphExplorerView.class.getResource("/graphexplorer/resources/"+path+".png");
        if (url == null) {
            System.out.println("Unable to load icon /graphexplorer/resources/" + path + ".png");
            return null;
        }
        return new ImageIcon(url);
    }


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
        ResourceMap rsc = getResourceMap();
        JOptionPane.showMessageDialog(getFrame(),
                rsc.getString("aboutText"), rsc.getString("aboutTitle"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Action
    public void showHelp() {
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        java.net.URL helpURL = null;
        try {
            ResourceMap rsc = getResourceMap();
            String filename = rsc.getResourcesDir() + rsc.getString("helpFile");
            helpURL = rsc.getClassLoader().getResource(filename);
            editorPane.setPage(helpURL);
        } catch (Exception ex) {
            Logger.getLogger(GraphExplorerView.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println("Attempted to read a bad URL: " + helpURL);
            return;
        }

        //Put the editor pane in a scroll pane.
        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(480, 640));

        ResourceMap rsc = getResourceMap();
        JOptionPane.showMessageDialog(null,
                editorScrollPane,
                rsc.getString("helpFrameTitle"), JOptionPane.QUESTION_MESSAGE);
    }
    
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="VIEW MENU ACTIONS">
    //
    // VIEW MENU ACTIONS
    //

    private FileEditorDialog fileEditor;
    
    @Action(enabledProperty = "graphActive")
    public void showEditorDialog() {
        if (controller() != null) {
            if (fileEditor == null)
                fileEditor = new FileEditorDialog(getFrame());
            else
                fileEditor.initTextArea();
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
    /** Graph-generation actions */
    ExplorerGenerateActions actions_gen;

    public GraphExplorerView(SingleFrameApplication app) {
        super(app);
        
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
    }

    private void initActions() {
        actions_io = new ExplorerIOActions(master);
        actions_gen = new ExplorerGenerateActions(master);

        actions_layout = new ExplorerLayoutActions(null); // needs single controller
        actions_stat = new ExplorerStatActions(null); // needs single controller
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

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainPanel = new javax.swing.JPanel();
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
        statusPanel = new javax.swing.JPanel();
        javax.swing.JSeparator statusPanelSeparator = new javax.swing.JSeparator();
        statusMessageLabel = new javax.swing.JLabel();
        statusAnimationLabel = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();
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
        jSeparator11 = new javax.swing.JPopupMenu.Separator();
        showEdMI = new javax.swing.JMenuItem();
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
        highlightB = new javax.swing.JCheckBoxMenuItem();
        distinctB = new javax.swing.JCheckBoxMenuItem();
        cooperationMI = new javax.swing.JMenuItem();
        primeMI = new javax.swing.JMenuItem();
        helpM = new javax.swing.JMenu();
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
        labelPS = new data.propertysheet.PropertySheet();
        edgePS = new data.propertysheet.PropertySheet();
        nodePS = new data.propertysheet.PropertySheet();
        layoutPS = new data.propertysheet.PropertySheet();
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

        propertySP.setName("propertySP"); // NOI18N
        propertySP.setPreferredSize(new java.awt.Dimension(200, 400));

        propertyRP.setName("propertyRP"); // NOI18N
        propertySP.setViewportView(propertyRP);

        mainSP.setLeftComponent(propertySP);

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
        org.jdesktop.application.ResourceMap resourceMap = org.jdesktop.application.Application.getInstance().getContext().getResourceMap(GraphExplorerView.class);
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

        fileM.setText(resourceMap.getString("fileM.text")); // NOI18N
        fileM.setName("fileM"); // NOI18N

        newM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/new-graph18.png"))); // NOI18N
        newM.setText(resourceMap.getString("newM.text")); // NOI18N
        newM.setName("newM"); // NOI18N

        emptyMI.setAction(actions_gen.GENERATE_EMPTY);
        emptyMI.setText(resourceMap.getString("emptyMI.text")); // NOI18N
        emptyMI.setName("emptyMI"); // NOI18N
        newM.add(emptyMI);

        circleMI.setAction(actions_gen.GENERATE_CIRCLE);
        circleMI.setText(resourceMap.getString("circleMI.text")); // NOI18N
        circleMI.setName("circleMI"); // NOI18N
        newM.add(circleMI);

        starMI.setAction(actions_gen.GENERATE_STAR);
        starMI.setText(resourceMap.getString("starMI.text")); // NOI18N
        starMI.setName("starMI"); // NOI18N
        newM.add(starMI);

        wheelMI.setAction(actions_gen.GENERATE_WHEEL);
        wheelMI.setText(resourceMap.getString("wheelMI.text")); // NOI18N
        wheelMI.setName("wheelMI"); // NOI18N
        newM.add(wheelMI);

        completeMI.setAction(actions_gen.GENERATE_COMPLETE);
        completeMI.setText(resourceMap.getString("completeMI.text")); // NOI18N
        completeMI.setName("completeMI"); // NOI18N
        newM.add(completeMI);

        jSeparator1.setName("jSeparator1"); // NOI18N
        newM.add(jSeparator1);

        uniform1MI.setAction(actions_gen.GENERATE_RANDOM);
        uniform1MI.setText(resourceMap.getString("uniform1MI.text")); // NOI18N
        uniform1MI.setName("uniform1MI"); // NOI18N
        newM.add(uniform1MI);

        sequenceMI.setAction(actions_gen.GENERATE_SEQUENCE);
        sequenceMI.setText(resourceMap.getString("sequenceMI.text")); // NOI18N
        sequenceMI.setName("sequenceMI"); // NOI18N
        newM.add(sequenceMI);

        wattsMI.setAction(actions_gen.GENERATE_WS);
        wattsMI.setText(resourceMap.getString("wattsMI.text")); // NOI18N
        wattsMI.setName("wattsMI"); // NOI18N
        newM.add(wattsMI);

        preferentialMI.setAction(actions_gen.GENERATE_PREFERENTIAL);
        preferentialMI.setText(resourceMap.getString("preferentialMI.text")); // NOI18N
        preferentialMI.setName("preferentialMI"); // NOI18N
        newM.add(preferentialMI);

        proximityMI.setAction(actions_gen.GENERATE_PROXIMITY);
        proximityMI.setName("proximityMI"); // NOI18N
        newM.add(proximityMI);

        fileM.add(newM);

        loadMI.setAction(actions_io.LOAD_ACTION);
        loadMI.setText(resourceMap.getString("loadMI.text")); // NOI18N
        loadMI.setName("loadMI"); // NOI18N
        fileM.add(loadMI);

        saveMI.setAction(actions_io.SAVE_ACTION);
        saveMI.setText(resourceMap.getString("saveMI.text")); // NOI18N
        saveMI.setName("saveMI"); // NOI18N
        fileM.add(saveMI);

        saveMI1.setAction(actions_io.SAVE_AS_ACTION);
        saveMI1.setText(resourceMap.getString("saveMI1.text")); // NOI18N
        saveMI1.setName("saveMI1"); // NOI18N
        fileM.add(saveMI1);

        closeMI.setAction(actions_io.CLOSE_ACTION);
        closeMI.setText(resourceMap.getString("closeMI.text")); // NOI18N
        closeMI.setName("closeMI"); // NOI18N
        fileM.add(closeMI);

        jSeparator7.setName("jSeparator7"); // NOI18N
        fileM.add(jSeparator7);

        exportImageM.setText(resourceMap.getString("exportImageM.text")); // NOI18N
        exportImageM.setEnabled(false);
        exportImageM.setName("exportImageM"); // NOI18N
        fileM.add(exportImageM);

        exportMovieM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/export-movie18.png"))); // NOI18N
        exportMovieM.setText(resourceMap.getString("exportMovieM.text")); // NOI18N
        exportMovieM.setEnabled(false);
        exportMovieM.setName("exportMovieM"); // NOI18N

        export_movMI.setEnabled(false);
        export_movMI.setName("export_movMI"); // NOI18N
        exportMovieM.add(export_movMI);

        fileM.add(exportMovieM);

        jSeparator6.setName("jSeparator6"); // NOI18N
        fileM.add(jSeparator6);

        javax.swing.ActionMap actionMap = org.jdesktop.application.Application.getInstance().getContext().getActionMap(GraphExplorerView.class, this);
        quitMI.setAction(actionMap.get("quit")); // NOI18N
        quitMI.setName("quitMI"); // NOI18N
        fileM.add(quitMI);

        menu.add(fileM);

        viewM.setText(resourceMap.getString("viewM.text")); // NOI18N
        viewM.setName("viewM"); // NOI18N

        fitMI.setAction(actionMap.get("fitToWindow")); // NOI18N
        fitMI.setName("fitMI"); // NOI18N
        viewM.add(fitMI);

        fullScreenMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        fullScreenMI.setText(resourceMap.getString("fullScreenMI.text")); // NOI18N
        fullScreenMI.setName("fullScreenMI"); // NOI18N
        fullScreenMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullScreenMIActionPerformed(evt);
            }
        });
        viewM.add(fullScreenMI);

        jSeparator11.setName("jSeparator11"); // NOI18N
        viewM.add(jSeparator11);

        showEdMI.setAction(actionMap.get("showEditorDialog")); // NOI18N
        showEdMI.setName("showEdMI"); // NOI18N
        viewM.add(showEdMI);

        menu.add(viewM);

        layoutM.setText(resourceMap.getString("layoutM.text")); // NOI18N
        layoutM.setName("layoutM"); // NOI18N

        circularMI.setAction(actionMap.get("layoutCircle")); // NOI18N
        circularMI.setName("circularMI"); // NOI18N
        layoutM.add(circularMI);

        randomMI.setAction(actionMap.get("layoutRandom")); // NOI18N
        randomMI.setName("randomMI"); // NOI18N
        layoutM.add(randomMI);

        staticSpringMI.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        staticSpringMI.setName("staticSpringMI"); // NOI18N
        layoutM.add(staticSpringMI);

        jSeparator4.setName("jSeparator4"); // NOI18N
        layoutM.add(jSeparator4);

        energyM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/layout-spring18.png"))); // NOI18N
        energyM.setText(resourceMap.getString("energyM.text")); // NOI18N
        energyM.setName("energyM"); // NOI18N

        energyStartMI.setAction(actions_layout.LAYOUT_ENERGY_START);
        energyStartMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play18.png"))); // NOI18N
        energyStartMI.setName("energyStartMI"); // NOI18N
        energyM.add(energyStartMI);

        timeStartMI.setAction(actions_layout.LAYOUT_TIME_START);
        timeStartMI.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play18.png"))); // NOI18N
        timeStartMI.setName("timeStartMI"); // NOI18N
        energyM.add(timeStartMI);

        layoutM.add(energyM);

        energyIterateMI.setAction(actions_layout.LAYOUT_ITERATE);
        energyIterateMI.setName("energyIterateMI"); // NOI18N
        layoutM.add(energyIterateMI);

        energyStopMI.setAction(actions_layout.LAYOUT_STOP);
        energyStopMI.setName("energyStopMI"); // NOI18N
        layoutM.add(energyStopMI);

        menu.add(layoutM);

        metricM.setText(resourceMap.getString("metricM.text")); // NOI18N
        metricM.setName("metricM"); // NOI18N

        localMetricM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-node18.png"))); // NOI18N
        localMetricM.setText(resourceMap.getString("localMetricM.text")); // NOI18N
        localMetricM.setName("localMetricM"); // NOI18N
        metricM.add(localMetricM);

        globalMetricM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-graph18.png"))); // NOI18N
        globalMetricM.setText(resourceMap.getString("globalMetricM.text")); // NOI18N
        globalMetricM.setName("globalMetricM"); // NOI18N
        metricM.add(globalMetricM);

        jMenuItem1.setAction(actions_stat.GLOBAL_STATS);
        jMenuItem1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/metric-graph18.png"))); // NOI18N
        jMenuItem1.setName("jMenuItem1"); // NOI18N
        metricM.add(jMenuItem1);

        menu.add(metricM);

        specialM.setText(resourceMap.getString("specialM.text")); // NOI18N
        specialM.setName("specialM"); // NOI18N

        highlightB.setAction(actionMap.get("highlightSubset")); // NOI18N
        highlightB.setName("highlightB"); // NOI18N
        specialM.add(highlightB);

        distinctB.setAction(actionMap.get("distinctColors")); // NOI18N
        distinctB.setName("distinctB"); // NOI18N
        specialM.add(distinctB);

        cooperationMI.setAction(actions_stat.COOPERATION);
        cooperationMI.setName("cooperationMI"); // NOI18N
        specialM.add(cooperationMI);

        primeMI.setAction(actions_gen.GENERATE_PRIME);
        primeMI.setName("primeMI"); // NOI18N
        specialM.add(primeMI);

        menu.add(specialM);

        helpM.setText(resourceMap.getString("helpM.text")); // NOI18N
        helpM.setName("helpM"); // NOI18N

        aboutMI.setAction(actionMap.get("showAbout")); // NOI18N
        aboutMI.setName("aboutMI"); // NOI18N
        helpM.add(aboutMI);

        contentMI.setAction(actionMap.get("showHelp")); // NOI18N
        contentMI.setName("contentMI"); // NOI18N
        helpM.add(contentMI);

        menu.add(helpM);

        newPM.setName("newPM"); // NOI18N

        emptyMI1.setAction(actions_gen.GENERATE_EMPTY);
        emptyMI1.setName("emptyMI1"); // NOI18N
        newPM.add(emptyMI1);

        circleMI1.setAction(actions_gen.GENERATE_CIRCLE);
        circleMI1.setName("circleMI1"); // NOI18N
        newPM.add(circleMI1);

        starMI1.setAction(actions_gen.GENERATE_STAR);
        starMI1.setName("starMI1"); // NOI18N
        newPM.add(starMI1);

        wheelMI1.setAction(actions_gen.GENERATE_WHEEL);
        wheelMI1.setName("wheelMI1"); // NOI18N
        newPM.add(wheelMI1);

        completeMI1.setAction(actions_gen.GENERATE_COMPLETE);
        completeMI1.setName("completeMI1"); // NOI18N
        newPM.add(completeMI1);

        jSeparator5.setName("jSeparator5"); // NOI18N
        newPM.add(jSeparator5);

        uniform1MI1.setAction(actions_gen.GENERATE_RANDOM);
        uniform1MI1.setText(resourceMap.getString("uniform1MI1.text")); // NOI18N
        uniform1MI1.setName("uniform1MI1"); // NOI18N
        newPM.add(uniform1MI1);

        sequenceMI1.setAction(actions_gen.GENERATE_SEQUENCE);
        sequenceMI1.setText(resourceMap.getString("sequenceMI1.text")); // NOI18N
        sequenceMI1.setName("sequenceMI1"); // NOI18N
        newPM.add(sequenceMI1);

        wattsMI1.setAction(actions_gen.GENERATE_WS);
        wattsMI1.setText(resourceMap.getString("wattsMI1.text")); // NOI18N
        wattsMI1.setName("wattsMI1"); // NOI18N
        newPM.add(wattsMI1);

        preferentialMI1.setAction(actions_gen.GENERATE_PREFERENTIAL);
        preferentialMI1.setText(resourceMap.getString("preferentialMI1.text")); // NOI18N
        preferentialMI1.setName("preferentialMI1"); // NOI18N
        newPM.add(preferentialMI1);

        proximityMI1.setAction(actions_gen.GENERATE_PROXIMITY);
        proximityMI1.setName("proximityMI1"); // NOI18N
        newPM.add(proximityMI1);

        labelPS.setName("labelPS"); // NOI18N

        edgePS.setName("edgePS"); // NOI18N

        nodePS.setName("nodePS"); // NOI18N

        layoutPS.setName("layoutPS"); // NOI18N

        toolbar.setRollover(true);
        toolbar.setName("toolbar"); // NOI18N

        newTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/new-graph24.png"))); // NOI18N
        newTBB.setFocusable(false);
        newTBB.setName("newTBB"); // NOI18N
        newTBB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newTBBActionPerformed(evt);
            }
        });
        toolbar.add(newTBB);

        loadTBB.setAction(actions_io.LOAD_ACTION);
        loadTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/load-graph24.png"))); // NOI18N
        loadTBB.setFocusable(false);
        loadTBB.setName("loadTBB"); // NOI18N
        toolbar.add(loadTBB);

        saveTBB.setAction(actions_io.SAVE_ACTION);
        saveTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/save-graph24.png"))); // NOI18N
        saveTBB.setFocusable(false);
        saveTBB.setName("saveTBB"); // NOI18N
        toolbar.add(saveTBB);

        jSeparator2.setName("jSeparator2"); // NOI18N
        toolbar.add(jSeparator2);

        jButton1.setAction(actionMap.get("fitToWindow")); // NOI18N
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setName("jButton1"); // NOI18N
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(jButton1);

        jSeparator10.setName("jSeparator10"); // NOI18N
        toolbar.add(jSeparator10);

        layoutCircleTBB.setAction(actionMap.get("layoutCircle")); // NOI18N
        layoutCircleTBB.setFocusable(false);
        layoutCircleTBB.setName("layoutCircleTBB"); // NOI18N
        toolbar.add(layoutCircleTBB);

        layoutRandomTBB.setAction(actionMap.get("layoutRandom")); // NOI18N
        layoutRandomTBB.setFocusable(false);
        layoutRandomTBB.setName("layoutRandomTBB"); // NOI18N
        toolbar.add(layoutRandomTBB);

        layoutStaticTBB.setAction(actionMap.get("layoutSpringStatic")); // NOI18N
        layoutStaticTBB.setFocusable(false);
        layoutStaticTBB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        layoutStaticTBB.setName("layoutStaticTBB"); // NOI18N
        layoutStaticTBB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toolbar.add(layoutStaticTBB);

        layoutEnergyTBB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/graphexplorer/resources/play24.png"))); // NOI18N
        layoutEnergyTBB.setToolTipText(resourceMap.getString("layoutEnergyTBB.toolTipText")); // NOI18N
        layoutEnergyTBB.setFocusable(false);
        layoutEnergyTBB.setName("layoutEnergyTBB"); // NOI18N
        layoutEnergyTBB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                layoutEnergyTBBActionPerformed(evt);
            }
        });
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
    }// </editor-fold>//GEN-END:initComponents

private void fullScreenMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullScreenMIActionPerformed
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
}//GEN-LAST:event_fullScreenMIActionPerformed

private void newTBBActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newTBBActionPerformed
        newPM.show(newTBB, 5, 5);
}//GEN-LAST:event_newTBBActionPerformed

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
    private javax.swing.JCheckBoxMenuItem highlightB;
    private javax.swing.JButton jButton1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator11;
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
    private javax.swing.JPanel mainPanel;
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
    private javax.swing.JProgressBar progressBar;
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
    private javax.swing.JMenuItem showEdMI;
    private javax.swing.JMenu specialM;
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
    private javax.swing.JMenu viewM;
    private javax.swing.JMenuItem wattsMI;
    private javax.swing.JMenuItem wattsMI1;
    private javax.swing.JMenuItem wheelMI;
    private javax.swing.JMenuItem wheelMI1;
    // End of variables declaration//GEN-END:variables

    private final Timer messageTimer;
    private final Timer busyIconTimer;
    private final Icon idleIcon;
    private final Icon[] busyIcons = new Icon[15];
    private int busyIconIndex = 0;

    private JDialog aboutBox;
    
    
    // <editor-fold defaultstate="collapsed" desc="GraphExplorerInterface METHODS">
    //
    // GraphExplorerInterface METHODS
    //
    
    public Component dialogComponent() {
        return getFrame();
    }

    public void output(String output) {
        Document d = outputTP.getDocument();
        try {
            d.insertString(d.getLength(), output + "\n\n", null);
        } catch (BadLocationException ex) {
            Logger.getLogger(GraphExplorerView.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public GraphController controller() {
        return master.getActiveController();
    }
    
    private boolean active = false;
    
    public boolean isGraphActive() { 
        return active; 
    }
    
    public void setGraphActive(boolean val) {
        if (active != val) {
            active = val;
            firePropertyChange("graphActive", !val, val);
        }
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
                StringRenderer lRend = adapter.getNodeLabelRenderer();
                if (lRend instanceof BasicStringRenderer) {
                    LabelBean lBean = new LabelBean((BasicStringRenderer)lRend);
                    int size = ac.getBaseGraph().order();
                    if (size > 100)
                        lBean.setVisible(false);
                    propertyRP.add(labelPS = new PropertySheet(lBean), "Labels");
                } else 
                    propertyRP.add(labelPS = new PropertySheet(lRend), "Labels");
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
            
            int size = gc.getBaseGraph().order();
            if (size > 5000)
                JOptionPane.showMessageDialog(getFrame(), 
                        "<html>The graph loading has " + size + " vertices.<br>"
                        + "GraphExplorer may operate very slowly graphs with more than 5000 vertices.<br>"
                        + "Resizing the display so the displayed graph is smaller may help.",
                        "WARNING", JOptionPane.WARNING_MESSAGE);
            else if (size > 2000)
                JOptionPane.showMessageDialog(getFrame(), 
                        "<html>The graph loading has " + size + " vertices.<br>"
                        + "GraphExplorer may operate slowly for graphs with more than 2000 vertices.<br>"
                        + "Resizing the display so the displayed graph is smaller may help.",
                        "WARNING", JOptionPane.WARNING_MESSAGE);

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
        actions_layout.setController(gc == null ? null : gc);
        setGraphActive(controller() != null && controller().getBaseGraph() != null);
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
            statusMessageLabel.setText((String) evt.getNewValue());
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
    
    /** Wraps label bean to allow visibility checks */
    public class LabelBean {
        private int alpha;
        private final BasicStringRenderer rend;
        public LabelBean(BasicStringRenderer rend) { 
            assert rend != null;
            this.rend = rend; 
            alpha = rend.getColor().getAlpha(); 
        }
        
        public boolean isVisible() { 
            return rend.getColor().getAlpha() > 0; 
        }
        public void setVisible(boolean val) {
            Color c = rend.getColor();
            if (val && c.getAlpha() == 0) {
                if (alpha == 0)
                    alpha = 255;
                setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            } else if (!val) {
                if (c.getAlpha() > 0)
                    this.alpha = c.getAlpha();
                setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
            }
        }

        public Point getOffset() { return rend.getOffset(); }
        public void setOffset(Point off) { rend.setOffset(off); }
        
        public Font getFont() { return rend.getFont(); }
        public void setFont(Font font) { rend.setFont(font); }
        
        public Color getColor() { return rend.getColor(); }
        public void setColor(Color color) { rend.setColor(color); }
        
        public Anchor getAnchor() { return rend.getAnchor(); }
        public void setAnchor(Anchor newValue) { rend.setAnchor(newValue); }
    }
    
    // </editor-fold>

}
