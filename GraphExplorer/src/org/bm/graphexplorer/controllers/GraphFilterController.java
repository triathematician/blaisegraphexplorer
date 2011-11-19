/*
 * GraphFilterController.java
 * Created Nov 18, 2010
 */

package org.bm.graphexplorer.controllers;

import org.bm.blaise.scio.graph.FilteredWeightedGraph;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.WeightedGraph;

/**
 * Provides a filtered view of a graph. The filter may be "turned on and off".
 * 
 * @author elisha
 */
public class GraphFilterController extends AbstractGraphController {

    //
    // PROPERTIES
    //
    
    /** Whether the filter is currently enabled */
    private boolean enabled = false;
    /** Threshold */
    private double threshold = 1;
    /** The filtered result graph */
    private FilteredWeightedGraph filtered;

    /** Construct with no base object. */
    public GraphFilterController(GraphController gc) {
        gc.addBaseGraphFollower(this);
        addPropertyChangeListener(gc);
        setBaseGraph(gc.getBaseGraph());
    }

    //
    // FILTERING FUNCTIONALITY
    //

    @Override
    protected void updateGraph() {
        FilteredWeightedGraph oldFiltered = filtered;
        if (!(baseGraph instanceof WeightedGraph)) {
            enabled = false;
            filtered = null;
        }
        if (enabled) {
            WeightedGraph weightedBase = (WeightedGraph) baseGraph;
            if (filtered == null || !(filtered instanceof FilteredWeightedGraph) || !(filtered.getBaseGraph() == baseGraph))
                filtered = FilteredWeightedGraph.getInstance(weightedBase);
            filtered.setThreshold(threshold);
        } else
            filtered = null;
        pcs.firePropertyChange("filteredGraph", oldFiltered, getFilteredGraph());
    }

    //
    // PROPERTY PATTERNS
    //

    /** 
     * Return filter applicability, i.e. whether it can be enabled
     * @return filter applicability
     */
    public boolean isApplicable() {
        return baseGraph instanceof WeightedGraph;
    }

    /**
     * Return filter's enabled/disable status
     * @return filter status 
     */
    public boolean isEnabled() {
        return enabled;
    }

    /** 
     * Return the filtered graph
     * @return the filtered graph (or the base graph if the filter is "off") 
     */
    public Graph getFilteredGraph() {
        return filtered == null ? baseGraph : filtered;
    }

    /**
     * Sets filter status
     */
    public void setEnabled(boolean enabled) {
        if (!(baseGraph instanceof WeightedGraph))
            enabled = false;
        if (this.enabled != enabled) {
            this.enabled = enabled;
            updateGraph();
        }
    }

    /** 
     * Return filter threshold, or null if the filter is not enabled
     * @return filter threshold 
     */
    public Double getFilterThreshold() {
        return isEnabled() ? filtered.getThreshold()
                : null;
    }

    /**
     * Sets filter threshold
     * @param value the new threshold; may be null if it is not enabled
     */
    public void setFilterThreshold(Double value) {
        if (!(baseGraph instanceof WeightedGraph))
            return;
        Double old = this.threshold;
        this.threshold = value;
        enabled = true;
        updateGraph();
        pcs.firePropertyChange("filterThreshold", old, value);
    }

}
