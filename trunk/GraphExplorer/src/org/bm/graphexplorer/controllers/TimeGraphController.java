/*
 * TimeGraphController.java
 * Created Nov 18, 2010
 */

package org.bm.graphexplorer.controllers;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.util.Map;
import org.bm.blaise.scio.graph.time.TimeGraph;
import org.bm.blaise.specto.plane.graph.GraphManager;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import org.bm.blaise.specto.plane.graph.time.TimeGraphManager;

/**
 * <p>
 *   Specialized controller for longitudinal graphs. In a longitudinal graph,
 *   the base graph is determined by a specific point in time.
 * </p>
 *
 * @author elisha
 */
public class TimeGraphController extends GraphController {

    /** Manages the graph */
    private final TimeGraphManager tManager;
    /** Corresponding time graph component */
    private final TimeGraphComponent tComponent;

    /** Construct with specified graph */
    public TimeGraphController(TimeGraph graph) {
        this(graph, null);
    }

    /** Construct with specified graph and name */
    public TimeGraphController(TimeGraph graph, String name) {
        tManager = new TimeGraphManager(graph);
        tManager.addPropertyChangeListener(this);
        tComponent = new TimeGraphComponent(tManager);
        tComponent.setUpdateWithTime(false); // the graph component does not update itself when the time changes
        component = tComponent.getGraphComponent();
        component.setGraphManager(getManager());
        component.getAdapter().getNodeStyler().setStyleDelegate(gdc.getNodeStyler());
        setBaseGraph(tManager.getSlice());
        setName(name);
    }

    @Override
    public String toString() {
        return "TimeGraphController";
    }

    /** Return component displaying the time graph */
    public final TimeGraphComponent getTimeGraphComponent() {
        return tComponent;
    }

    /** 
     * Return manager for the tiem graph 
     * @return manager for the controller 
     */
    public TimeGraphManager getTimeGraphManager() { return tManager; }
    
    /** 
     * Return displayed time slice
     * @return time slice 
     */
    public Double getTime() { return tManager.getTime(); }

    /**
     * Set displayed time slice
     */
    public void setTime(Double t) { tManager.setTime(t); }

    @Override
    public boolean isLayoutAnimating() {
        return super.isLayoutAnimating() || tManager.isLayoutAnimating();
    }

    @Override
    public void setLayoutAnimating(boolean value) {
        if (tManager.getLayoutAlgorithm() != null)
            tManager.setLayoutAnimating(value);
        else
            super.setLayoutAnimating(value);
    }
    
    

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals("timeData"))
            setBaseGraph(tManager.getSlice());
        else if (prop.equals("nodePositions")) {
            component.getGraphManager().requestPositionMap((Map<Object,Point2D.Double>) evt.getNewValue());
        } else if (prop.equals("filteredGraph")) {
            if (evt.getNewValue() != tManager.getSlice()) {
                // filtering of longitudinal graph == special case
                tManager.setFilter(gfc.getFilterThreshold());
            }
            super.propertyChange(evt);
        } else
            super.propertyChange(evt);
    }



}
