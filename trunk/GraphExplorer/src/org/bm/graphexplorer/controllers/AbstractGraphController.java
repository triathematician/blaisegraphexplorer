/*
 * AbstractGraphController.java
 * Created Nov 18, 2010
 */

package org.bm.graphexplorer.controllers;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import org.bm.blaise.scio.graph.Graph;

/**
 * <p>
 *   Provides generic functionality for controllers operating with a single "base" graph.
 *   This base graph may be of any time, and may or may not be the graph that is
 *   "presented" as the "active" graph.
 * </p>
 * 
 * @author elisha
 */
abstract public class AbstractGraphController {

    /** The base graph for the controller */
    protected Graph baseGraph = null;

    //
    // CONSTRUCTOR
    //

    /** Construct instance (without a base graph) */
    protected AbstractGraphController() {}

    /** Construct instance (with a base graph) */
    protected AbstractGraphController(Graph base) { baseGraph = base; }

    //
    // PROPERTY PATTERNS
    //

    /** @return active graph */
    public Graph getBaseGraph() {
        return baseGraph;
    }

    /** Sets active graph. */
    public final void setBaseGraph(Graph graph) {
        if (this.baseGraph != graph) {
            Graph oldGraph = baseGraph;
            baseGraph = graph;
            updateGraph();
            pcs.firePropertyChange("baseGraph", oldGraph, baseGraph);
        }
    }

    /** 
     * Provides a hook for updating the graph IMMEDIATELY after the baseGraph
     * property has changed in the {@link AbstractGraphController#setBaseGraph(Graph)} method,
     * and immediately before a {@link PropertyChangeEvent} is generated.
     */
    protected void updateGraph() {}

    
    /** 
     * Report an updated status to listeners.
     * @param string new status
     */
    public void setStatus(String string) { 
        pcs.firePropertyChange("status", null, string); 
    }
    
    /** 
     * Report an updated output to listeners.
     * @param string new output
     */
    public void setOutput(String string) {
        pcs.firePropertyChange("output", null, string); 
    }


    //<editor-fold defaultstate="collapsed" desc="PropertyChangeSupport METHODS">
    //
    // PropertyChangeSupport METHODS
    //
    
    /** Handles property changes */
    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) { pcs.removePropertyChangeListener(propertyName, listener); }
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) { pcs.removePropertyChangeListener(listener); }
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) { pcs.addPropertyChangeListener(propertyName, listener); }
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) { pcs.addPropertyChangeListener(listener); }
    //</editor-fold>

}
