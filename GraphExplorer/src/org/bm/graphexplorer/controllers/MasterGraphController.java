/*
 * GraphControllerMaster.java
 * Created Jul 29, 2010
 */

package org.bm.graphexplorer.controllers;

import java.awt.Image;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.ValuedGraph;
import org.bm.blaise.scio.graph.io.AbstractGraphIO;
import org.bm.blaise.scio.graph.time.InterpolationTimeGraph;
import org.bm.blaise.scio.graph.time.TimeGraph;

/**
 * <p>
 *   Provides "master" control for application where multiple graphs are open
 *   simultaneously.
 * </p>
 * @author Elisha Peterson
 */
public class MasterGraphController {

    /** Stores the loaded controllers, and their "active" reportStatus. */
    private ArrayList<GraphController> controllers = new ArrayList<GraphController>();

    //
    // CONSTRUCTOR & UTILITY METHODS
    //

    /** Default constructor */
    public MasterGraphController() {}

    @Override
    public String toString() {
        return "GraphControllerMaster";
    }

    //
    // PROPERTY PATTERNS
    //
    
    /** Stores the active controller */
    private transient GraphController active = null;
    /** Return active controller */
    public GraphController getActiveController() { return active; }
    /** Updates active controller */
    public synchronized void setActiveController(GraphController nue) {
        if (active != nue) {
            GraphController old = active;
            if (nue != null && !controllers.contains(nue))
                controllers.add(nue);
            active = nue;
            pcs.firePropertyChange("activeController", old, nue);
        }
    }
    
    /** Return open file */
    public File getOpenFile() { 
        return active == null ? null : active.getFile(); 
    }
    
    /** Currently reported status */
    private String status;
    /** Return current status */
    public String getStatus() { return status; }
    /** Updates the application reportStatus bar. */
    public void setStatus(String string) { 
        pcs.firePropertyChange("status", null, status = string); 
    }
    
    /** Currently reported output */
    private String output;
    /** Return current output */
    public String getOutput() { return output; }
    /** Updates the application reportOutput. */
    public void setOutput(String string) {
        pcs.firePropertyChange("output", null, output = string); 
    }

    //
    // CONTROLLER HANDLING
    //

    /** @return unmodifiable view of controllers */
    public synchronized List<GraphController> getControllers() {
        return Collections.unmodifiableList(controllers);
    }

    /** @return true if specified controller is in this master's list */
    public synchronized boolean containsController(GraphController gc) {
        return controllers.contains(gc);
    }

    /** Closes the active controller, removing it from the list of controllers;
     * if no controllers are active, activates the last one & notifies listeners */
    public synchronized void closeController() {
        closeController(active);
    }

    /** Closes specified controller, removing it from the list of controllers;
     * if no controllers are active, activates the last one & notifies listeners */
    public synchronized void closeController(GraphController c) {
        if (controllers.contains(c)) {
            controllers.remove(c);
            if (active == c)
                setActiveController(controllers.size() > 0 ? controllers.get(0) : null);
        }
    }
    
    //
    // INPUT/OUTPUT ACTIONS
    //
    
    /** 
     * Attempt to load a file with the specified IO. If successful, update the active
     * controller and graph view with the result.
     * @param file the file to load
     * @param loader used to parse the file
     * @return true if load is successful, false otherwise
     */ 
    public boolean loadGraphFile(File file, AbstractGraphIO loader) {
        if (file == null || loader == null)
            return false;
        
        TreeMap<Integer, double[]> loc = new TreeMap<Integer, double[]>();
        Object loaded = loader.importGraph(loc, file, null);
        if (loaded == null) {
            setStatus("Attempt to load file " + file + " failed: problem loading file with parser " + loader.getFileFilter().getDescription());
            return false;
        } else if (!(loaded instanceof Graph || loaded instanceof TimeGraph)) {
            setStatus("Attemt to load file " + file + " failed: unsupported graph type.");
            return false;
        }
        
        GraphController gc = null;
        if (loaded instanceof TimeGraph) {
//            TimeGraph interp = new InterpolationTimeGraph((TimeGraph<Integer>) loaded, 3);
            gc = new TimeGraphController((TimeGraph) loaded, file.getName());
            gc.setFile(file);
        } else if (loaded instanceof Graph) {
            Graph<Integer> gl = (Graph<Integer>) loaded;
            gc = new GraphController(gl, file.getName());
            gc.setFile(file);
            if (loc != null && loc.size() > 0) {
                LinkedHashMap<Object,Point2D.Double> pos = new LinkedHashMap<Object,Point2D.Double>();
                for (Entry<Integer,double[]> en : loc.entrySet())
                    pos.put(en.getKey(), new Point2D.Double(en.getValue()[0], en.getValue()[1]));
                gc.setNodePositions(pos);
            }
            
            // look for images attached with file
            List nodes = gl.nodes();
            if (gl instanceof ValuedGraph && nodes.size() > 0) {
                ValuedGraph vg = (ValuedGraph) gl;
                Object o1 = vg.getValue(nodes.get(0));
                if (o1.getClass().equals(Object[].class)) {
                    Object[] o2 = (Object[]) o1;
                    if (o2.length >= 2 && o2[1].getClass().equals(String.class)) {
                        ValuedGraph<Integer,Object[]> vg2 = (ValuedGraph<Integer,Object[]>) vg;
                        for (Integer i : vg2.nodes()) {
                            String fileName = (String) vg2.getValue(i)[1];
                            File imageFile = new File(file.getParent(), fileName);
                            try {
                                Image image = ImageIO.read(imageFile);
                                vg2.getValue(i)[1] = image;
                            } catch (Exception ex) {
                                System.out.println("Failed to read image file " + imageFile);
                            }
                        }
                    }
                }
            }
        }
        
        if (gc == null) {
            setStatus("Attempt to load file " + file + " failed after initial load!");
            return false;
        } else {
            setActiveController(gc);
            return true;
        }
    }
    

    //<editor-fold defaultstate="collapsed" desc="PropertyChangeSupport METHODS">
    //
    // PropertyChangeSupport METHODS
    //

    /** Handles property changes */
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) { pcs.removePropertyChangeListener(propertyName, listener); }
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) { pcs.removePropertyChangeListener(listener); }
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { pcs.addPropertyChangeListener(propertyName, listener); }
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) { pcs.addPropertyChangeListener(listener); }
    //</editor-fold>

}
