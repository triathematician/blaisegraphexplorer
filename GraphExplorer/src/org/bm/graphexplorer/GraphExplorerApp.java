/**
 * GraphExplorerApp.java
 * Created Aug 17, 2011
 */
package org.bm.graphexplorer;

import org.bm.graphexplorer.controllers.MasterGraphController;
import org.bm.graphexplorer.controllers.TimeGraphController;
import org.bm.graphexplorer.controllers.GraphController;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.io.AbstractGraphIO;
import org.bm.blaise.scio.graph.io.AbstractGraphIO.GraphType;
import org.bm.blaise.specto.plane.graph.GraphComponent;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import org.bm.firestarter.propertysheet.editor.EditorRegistration;
import org.bm.graphexplorer.components.GraphFileChooser;
import org.bm.graphexplorer.components.GraphFileChooser.ChooserMode;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.SingleFrameApplication;
import org.jdesktop.application.Task;

/**
 * This is the main application that will be run. This is where global actions are specified, things that
 * are independent of particular GUI elements. This class also keeps track of the master graph controller,
 * which keeps loaded graphs in memory.
 * 
 * With the exception of the {@link GraphExplorerApp#startup()} method, this class should NOT reference
 * {@link GraphExplorerView}.
 * 
 * @author elisha
 */
public class GraphExplorerApp extends SingleFrameApplication {
    
    /** Master controller that keeps track of the open graphs. */
    private final MasterGraphController master = new MasterGraphController();

    
    //<editor-fold defaultstate="collapsed" desc="PROPERTIES">
    //
    // PROPERTIES
    //

    /** Return true if there is an active graph */
    public boolean isGraphActive() { 
        return getActiveController() != null;
    }
    
    /** Return true if a time graph is active */
    public boolean isTimeGraphActive() {
        return getActiveController() != null && getActiveController() instanceof TimeGraphController;
    }
    
    /**
     * Returns the master graph controller.
     * @return master graph controller
     */
    public MasterGraphController getMasterController() {
        return master;
    }
    
    /**
     * Returns the active controller
     * @return active controller, or null if there is none
     */
    public GraphController getActiveController() {
        return master.getActiveController();
    }
    
    /**
     * Returns the active graph component
     * @return active component, or null if there is none
     */
    public GraphComponent getActiveGraphComponent() {
        return getActiveController() == null ? null : getActiveController().getComponent();
    }
    
    /**
     * Returns the time graph component
     * @return time graph component, or null if there is noen
     */
    public TimeGraphComponent getActiveTimeGraphComponent() {
        return isTimeGraphActive() ? ((TimeGraphController)getActiveController()).getTimeGraphComponent() : null;
    }
    
    /**
     * Return static instance of the application.
     * @return application instance
     */
    public static GraphExplorerApp getApplication() {
        return Application.getInstance(GraphExplorerApp.class);
    }
    //</editor-fold>
    
    
    
    
    //<editor-fold defaultstate="collapsed" desc="FILE/IO ACTIONS">
    //
    // FILE/IO ACTIONS
    //
    
    @Action(block = Task.BlockingScope.APPLICATION)
    public Task<File,Void> loadGraph() {
        return new FileSelectTask(ChooserMode.GRAPH, "Load a graph", "Load", null) {
            @Override
            protected boolean execute(File selected) {
                setMessage("Loading graph file...");
                return master.loadGraphFile(selected, chooser.activeIO());
            }
            @Override
            protected void succeeded(File result) {
                if (result != null) {
                    session.addRecentFile(result);
                    firePropertyChange("recentFileActions", null, session.getRecentFileActions(master));
                    setMessage("Load succeeded.");
                } else
                    setMessage("Load failed.");
            }            
        };
    }
    
    @Action(enabledProperty = "graphActive", block = Task.BlockingScope.APPLICATION)
    public Task<File,Void> saveGraph() {
        if (getActiveController().getFile() != null)
            return new Task<File,Void>(this) {
                @Override
                protected File doInBackground() throws Exception {
                    File f = getActiveController().getFile();
                    GraphType result = GraphFileChooser.getInstance().activeIO().saveGraph(getActiveController().getBaseGraph(), getActiveController().getNodePositions(), f);
                    return result != null && result != GraphType.UNKNOWN ? f : null;
                }
                @Override
                protected void succeeded(File result) {
                    setMessage(result == null ? "Save failed." : "Save succeeded.");
                }
            };
        else
            return saveGraphAs();
    }
    
    @Action(block = Task.BlockingScope.APPLICATION, enabledProperty = "graphActive")
    public Task<File,Void> saveGraphAs() {
        return new FileSelectTask(ChooserMode.GRAPH, "Save graph as", "Save", null) {
            @Override
            protected boolean execute(File selected) {
                setMessage("Saving graph file...");
                Object active = getActiveController().getBaseGraph();
                GraphType result = chooser.activeIO().saveGraph(active, getActiveController().getNodePositions(), selected);
                return result != null && result != GraphType.UNKNOWN;
            }
            @Override
            protected void succeeded(File result) {
                if (result != null) {
                    session.addRecentFile(result);
                    firePropertyChange("recentFileActions", null, session.getRecentFileActions(master));
                    setMessage("Save succeeded.");
                } else
                    setMessage("Save failed.");
            }            
        };
    }

    @Action(enabledProperty = "graphActive")
    public void closeGraph() {
        master.closeController();
    }
    
    @Action(block = Task.BlockingScope.APPLICATION, enabledProperty = "timeGraphActive")
    public Task<File,Void> exportMovie() {
        return new FileSelectTask(ChooserMode.MOVIE, "Export Movie of Active Graph", "Export", "Quicktime movie (.mov)", "mov") {
            @Override
            protected boolean execute(File selected) {
                if (!isTimeGraphActive())
                    return false;

                TimeGraphController tgc = (TimeGraphController) getActiveController();
                TimeGraphComponent tgm = getActiveTimeGraphComponent();
                MovieExport me = new MovieExport(selected);
                ProgressMonitor pm = new ProgressMonitor(tgm, "Exporting graph to .mov file", "", 0, 1);
                ExportMovieTask task = new ExportMovieTask(tgc, tgm, me, pm);
                if (tgc.getTimeGraphManager().getLayoutAlgorithm() != null)
                    task.setIterateLayout(false);
                task.run();
                return true;
            }
        };
    }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="NEW GRAPH ACTIONS">
    //
    // NEW GRAPH ACTIONS
    //
    
    @Action public Task<Graph, Void> newCircleGraph() { return GenerateGraphTasks.circleGraphTask(this); }
    @Action public Task<Graph, Void> newCompleteGraph() { return GenerateGraphTasks.completeGraphTask(this); }
    @Action public Task<Graph, Void> newEmptyGraph() { return GenerateGraphTasks.emptyGraphTask(this); }
    @Action public Task<Graph, Void> newStarGraph() { return GenerateGraphTasks.starGraphTask(this); }
    @Action public Task<Graph, Void> newWheelGraph() { return GenerateGraphTasks.wheelGraphTask(this); }

    @Action public Task<Graph, Void> newPrimeGraph() { return GenerateGraphTasks.primeGraphTask(this); }

    @Action public Task<Graph, Void> newPrefRandomGraph() { return GenerateGraphTasks.prefRandomGraphTask(this); }
    @Action public Task<Graph, Void> newProximityGraph() { return GenerateGraphTasks.proximityRandomGraphTask(this); }
    @Action public Task<Graph, Void> newRandomGraph() { return GenerateGraphTasks.randomGraphTask(this); }
    @Action public Task<Graph, Void> newDegreeSequenceGraph() { return GenerateGraphTasks.sequenceRandomGraphTask(this); }
    @Action public Task<Graph, Void> newWattsStrogatzGraph() { return GenerateGraphTasks.wsRandomGraphTask(this); }
    
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="HELP ACTIONS">
    //
    // HELP ACTIONS
    //
    
    @Action
    public void showAbout() {
        ResourceMap rsc = getContext().getResourceMap();
        JOptionPane.showMessageDialog(getMainFrame(),
                rsc.getString("aboutText"), rsc.getString("aboutTitle"), JOptionPane.INFORMATION_MESSAGE);
    }

    @Action
    public void showHelp() {
        ResourceMap rsc = getContext().getResourceMap();
        
        JEditorPane editorPane = new JEditorPane();
        java.net.URL helpURL = null;
        try {
            String filename = rsc.getResourcesDir() + rsc.getString("helpFile");
            helpURL = rsc.getClassLoader().getResource(filename);
            editorPane.setPage(helpURL);
        } catch (Exception ex) {
            Logger.getLogger(GraphExplorerApp.class.getName()).log(Level.SEVERE, null, ex);
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
    
    
    
    
    //<editor-fold defaultstate="collapsed" desc="EVENT HANDLING">
    //
    // EVENT HANDLING
    //
    
    /** 
     * Add listener for the active graph controller 
     * @param l the listener
     */
    public void addActiveControllerListener(PropertyChangeListener l) { 
        master.addPropertyChangeListener("activeController", l); 
    }
    
    /** 
     * Remove listener for the active graph controller 
     * @param l the listener
     */
    public void removeActiveControllerListener(PropertyChangeListener l) { 
        master.removePropertyChangeListener("activeController", l); 
    }
    
    //</editor-fold>
    
    
    
    
    
    //<editor-fold defaultstate="collapsed" desc="APPLICATION LIFECYCLE">
    //
    // APPLICATION LIFECYCLE
    //

    /** Tracks session info to store from one time to the next. */
    GESessionBean session;
    
    @Override
    protected void initialize(String[] args) {
        super.initialize(args);
        try {
            Object obj = getContext().getLocalStorage().load("GESession.xml");
            if (obj != null) {
                session = (GESessionBean) obj;
            }
        } catch (IOException ex) {
            Logger.getLogger(GraphExplorerApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (session == null)
            session = new GESessionBean();
        
        // ensure that actions depending on an active graph are updated appropriately
        master.addPropertyChangeListener("activeController", new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                int size = getActiveController() == null ? 0 : getActiveController().getBaseGraph().order();
                // TODO - move messages to resoure class
                if (size > 5000)
                    JOptionPane.showMessageDialog(getMainFrame(), 
                            "<html>The graph loading has " + size + " vertices.<br>"
                            + "GraphExplorer may operate very slowly graphs with more than 5000 vertices.<br>"
                            + "Resizing the display so the displayed graph is smaller may help.",
                            "WARNING", JOptionPane.WARNING_MESSAGE);
                else if (size > 2000)
                    JOptionPane.showMessageDialog(getMainFrame(), 
                            "<html>The graph loading has " + size + " vertices.<br>"
                            + "GraphExplorer may operate slowly for graphs with more than 2000 vertices.<br>"
                            + "Resizing the display so the displayed graph is smaller may help.",
                            "WARNING", JOptionPane.WARNING_MESSAGE);
            
                firePropertyChange("graphActive", null, isGraphActive());
                firePropertyChange("timeGraphActive", null, isTimeGraphActive());
            }
        });
    }
    
    @Override
    protected void startup() {        
        EditorRegistration.registerEditors();
        show(new GraphExplorerView(this));
    }

    @Override
    protected void ready() {
        super.ready();
        firePropertyChange("recentFileActions", null, session.getRecentFileActions(master));
    }

    @Override
    protected void shutdown() {
        try {
            getContext().getLocalStorage().save(session, "GESession.xml");
        } catch (IOException ex) {
            Logger.getLogger(GraphExplorerApp.class.getName()).log(Level.SEVERE, null, ex);
        }
        super.shutdown();
    }
    
    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        launch(GraphExplorerApp.class, args);
    }
    //</editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="SESSION BEAN">
    
    /**
     * Tracks the elements that are stored from one session to the next.
     */
    public static class GESessionBean {
        /** 15 most recent files that have been opened */
        private ArrayList<String> recent = new ArrayList<String>(15);
        
        /** No-arg constructor */
        public GESessionBean() {}

        public ArrayList<String> getRecent() { 
            return recent; 
        }
        public void setRecent(ArrayList<String> recent) { 
            this.recent = recent;
        }
        
        void addRecentFile(java.io.File f) {
            if (f == null)
                return;
            String path = f.getAbsolutePath();
            if (recent.contains(path))
                recent.remove(path);
            recent.add(0, path);
            while (recent.size() > 15)
                recent.remove(14);
        }
        
        javax.swing.Action[] getRecentFileActions(MasterGraphController master) {
            javax.swing.Action[] result = new javax.swing.Action[recent.size()];
            int i = 0;
            for (String path : recent)
                result[i++] = loadAction(path, master);
            return result;
        }
        
        javax.swing.Action loadAction(final String path, final MasterGraphController master) {
            return new AbstractAction(path) {
                public void actionPerformed(ActionEvent e) {
                    File f = new File(path);
                    master.loadGraphFile(f, GraphFileChooser.getIO(f));
                }                
            };         
        }
    }
    //</editor-fold>
    

    
    
    //<editor-fold defaultstate="collapsed" desc="UTILITES">
    //
    // UTILITIES
    //
    
    /**
     * Return list of platform-supported image export actions.
     */
    List<AbstractAction> createExportImageActions() {
        List<AbstractAction> result = new ArrayList<AbstractAction>();
        
        List supportedFormats = Arrays.asList(ImageIO.getWriterFormatNames());
        if (supportedFormats.contains("jpg"))
            result.add(new ExportImageAction("JPEG format (.jpg)", "jpg"));
        if (supportedFormats.contains("png"))
            result.add(new ExportImageAction("Portable Network Graphics format (.png)", "png"));
        if (supportedFormats.contains("gif"))
            result.add(new ExportImageAction("Graphics Interchange Format (.gif)", "gif"));
        if (supportedFormats.contains("bmp"))
            result.add(new ExportImageAction("Bitmap format (.bmp)", "bmp"));
        
        return result;
    }
    
    /**
     * Action to export image of current view. These are dynamic because supported
     * image formats may differ by platform.
     */
    private class ExportImageAction extends AbstractAction {
        String text;
        String[] ext;
        ExportImageAction(String text, String... ext) {
            super(text);
            this.text = text;
            this.ext = ext;            
            GraphExplorerApp.this.addPropertyChangeListener("graphActive", new PropertyChangeListener(){
                public void propertyChange(PropertyChangeEvent evt) { setEnabled((Boolean) evt.getNewValue()); }
            });
        }
        public void actionPerformed(ActionEvent e) {
            FileSelectTask t = new FileSelectTask(ChooserMode.IMAGE, "Export Graph Image", "Export", text, ext) {
                protected boolean execute(File selected) {
                    GraphComponent gc = getActiveGraphComponent();
                    if (gc == null)
                        return false;
                    BufferedImage image = new BufferedImage(gc.getWidth(), gc.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D canvas = image.createGraphics();
                    canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    gc.renderTo(canvas);
                    canvas.dispose();
                    try {
                        ImageIO.write(image, ext[0], selected);
                        return true;
                    } catch (IOException ex) {
                        System.err.println(ex);
                        return false;
                    }
                }
            };
            getContext().getTaskMonitor().setForegroundTask(t);
            getContext().getTaskService().execute(t);
        }
    }
    
    /**
     * Task that begins by showing the file chooser and allowing the user to select a file,
     * then does something with that file. Displaying the file chooser window and selecting a file
     * is done in the constructor. Processing the file is done in the "doInBackground" method.
     * 
     * Instances of this class override the "doInBackground" method and can use the "selected" file
     * property to see what the user selected.
     */
    private abstract class FileSelectTask extends Task<File,Void> {
        protected GraphFileChooser chooser;
        protected File selected;
        /** 
         * Construct the task.
         * @param filterType type of file filter to use for the selector
         * @param title title of the chooser window
         * @param button text of the approval button
         * @param descrip description of the filter(s)
         * @param ext extensions associated with the filter(s)
         */
        public FileSelectTask(ChooserMode filterType, String title, String button, String descrip, String... ext) {
            super(GraphExplorerApp.this);
            chooser = GraphFileChooser.initFileChooser(filterType, title, button);
            chooser.setDialogTitle(title);
            chooser.setApproveButtonText(button);
            if (descrip == null || ext.length == 0)
                chooser.setMode(filterType);
            else
                chooser.setMode(filterType, new FileNameExtensionFilter(descrip, ext));
            File file = master.getOpenFile();
            if (file != null)
                chooser.setSelectedFile(deriveFile(file, ext[0]));
            selected = chooser.showOpenDialog(getMainFrame()) == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
        }

        @Override
        protected File doInBackground() throws Exception {
            if (selected != null) {
                boolean result = execute(selected);
                if (result)
                    return selected;
            }
            return null;
        }
        
        /** 
         * Executes the task for the specified file. 
         * @param selected the file chosen by the user
         * @return true if execution succeeded, false otherwise
         */
        protected abstract boolean execute(File selected);
    }
    
    /** 
     * Derives a new file name from the open file 
     * @param file the file
     * @param newExtension a new extension for the file
     * @return file representing same base, new extension
     */
    private static File deriveFile(File file, String newExtension) {
        String name = file.getName();
        int pos = name.indexOf(".");
        String newName = name.substring(0, pos) + "." + newExtension;
        return new File(file.getParent(), newName);
    }
    //</editor-fold>

    
    //<editor-fold defaultstate="collapsed" desc="OTHER UTILITY DIALOGS">
    //
    // OTHER UTILITY DIALOGS
    //

    /**
     * Shows option pane to retrieve an integer value in provided range.
     * @return a value between min and max, or -1 if the user cancelled the dialog
     */
    public static int showIntegerInputDialog(String message, int min, int max) {
        int num = 0;
        do {
            String response = JOptionPane.showInputDialog(message);
            if (response == null) return -1;
            try { num = Integer.decode(response); } catch (NumberFormatException ex) { System.out.println("Improperly formatted number..."); }
        } while (num < min || num > max);
        return num;
    }

    /**
     * Shows option pane to retrieve an array of int values in specified range
     * @return a value between min and max, or null if the user cancelled the dialog
     */
    public static int[] showIntegerArrayInputDialog(String message, int min, int max) {
        int[] result = null;
        boolean valid;
        do {
            valid = true;
            String response = JOptionPane.showInputDialog(message);
            if (response == null) return null;
            String[] responseArr = response.split(",");
            result = new int[responseArr.length];
            try {
                for (int i = 0; i < responseArr.length; i++) {
                    result[i] = Integer.decode(responseArr[i].trim());
                    valid = valid && result[i] >= min && result[i] <= max;
                    if (!valid) System.out.println("showIntegerArrayInputDialog: Element " + responseArr[i] + " interpreted as " + result[i] + " deemed to be outside the boundaries [" + min + "," + max + "]");
                }
            } catch (NumberFormatException ex) { 
                System.out.println("showIntegerArrayInputDialog: Improperly formatted number with input: " + message);
            }
        } while (!valid);
        return result;
    }

    /** 
     * Shows option pane to retrieve a float value in provided range.
     * @return a value between min and max, or -1 if the user cancelled the dialog
     */
    public static float showFloatInputDialog(String message, float min, float max) {
        float num = 0;
        do {
            String response = JOptionPane.showInputDialog(message);
            if (response == null) return -1;
            try { num = Float.parseFloat(response); } catch (NumberFormatException ex) { System.out.println("Improperly formatted number..."); }
        } while (num < min || num > max);
        return num;
    }

    /** 
     * Shows option pane to retrieve a float value in provided range.
     * @return a value between min and max, or null if the user cancelled the dialog
     */
    public static float[] showFloatArrayInputDialog(String message, float min, float max) {
        float[] result = null;
        boolean valid = false;
        do {
            String response = JOptionPane.showInputDialog(message);
            if (response == null) return null;
            String[] responseArr = response.split(",");
            result = new float[responseArr.length];
            try {
                float sum = 0f;
                for (int i = 0; i < responseArr.length; i++) {
                    result[i] = Float.valueOf(responseArr[i].trim());
                    sum += result[i];
                    valid = valid && (result[i] >= min && result[i] <= max);
                }
                valid = valid && sum == 1f;
            } catch (NumberFormatException ex) { System.out.println("Improperly formatted number..."); }
        } while (!valid);
        return result;
    }
    //</editor-fold>


}