/*
 * GraphFilterController.java
 * Created Nov 18, 2010
 */

package graphexplorer.controller;

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
    // PROPERTY CHANGE NAMES
    //

    /** Property change when filtered result graph changes */
    public static final String $FILTERED_GRAPH = "filtered graph";
    /** Threshold for underlying filter */
    public static final String $FILTER_THRESHOLD = "filter threshold";

    //
    // PROPERTIES
    //
    
    /** Whether the filter is currently enabled */
    boolean enabled = false;
    /** Threshold */
    double threshold = 1;
    /** The filtered result graph */
    FilteredWeightedGraph filtered;

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
        pcs.firePropertyChange($FILTERED_GRAPH, null, getFilteredGraph());
    }

    //
    // PROPERTY PATTERNS
    //

    /** Return the filtered graph (or the base graph if the filter is "off") */
    public Graph getFilteredGraph() {
        return filtered == null ? baseGraph : filtered;
    }

    /** @return filter status */
    public boolean isApplicable() {
        return baseGraph instanceof WeightedGraph;
    }

    /** @return filter status */
    public boolean isEnabled() {
        return enabled;
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

    /** @return filter threshold */
    public Double getFilterThreshold() {
        return isEnabled() ? filtered.getThreshold()
                : null;
    }

    /**
     * Sets filter threshold
     */
    public void setFilterThreshold(double value) {
        if (!(baseGraph instanceof WeightedGraph))
            return;
        double old = this.threshold;
        this.threshold = value;
        enabled = true;
        updateGraph();
        pcs.firePropertyChange($FILTER_THRESHOLD, old, value);
    }

}
