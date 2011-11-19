/*
 * GraphController.java
 * Created Jul 28, 2010
 */

package org.bm.graphexplorer.controllers;

import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.ValuedGraph;
import org.bm.blaise.scio.graph.layout.IterativeGraphLayout;
import org.bm.blaise.scio.graph.layout.StaticGraphLayout;
import org.bm.blaise.scio.graph.metrics.NodeMetric;
import org.bm.blaise.specto.plane.graph.GraphComponent;
import org.bm.blaise.specto.plane.graph.GraphManager;

/**
 * <p>
 *   This controller is used to handle views into a graph object, with a single
 *   graph viewable at a time. The controller is set up with a persistent graph
 *   (the <code>baseGraph</code> property in the super-class)
 *   that represents the "base graph" represented by the controller.
 *   A filter sits on top of this graph, providing a "partial view" of the object,
 *   (the <code>baseGraph</code> property).
 * </p>
 * <p>
 *   The controller has sub-controllers used to handle the visible <i>layout</i> of the
 *   graph and the <i>metrics</i> computed for the active graph. In general these
 *   will interact with the <code>baseGraph</code>, although the layout controller
 *   also maintains location information for non-visible nodes.
 * </p>
 *
 * @author Elisha Peterson
 */
public class GraphController extends AbstractGraphController
        implements PropertyChangeListener {

    //
    // PROPERTIES
    //
    
    /** File Name of graph */
    private String name = null;
    /** File corresponding to graph */
    private File file = null;

    /** Corresponding graph component */
    protected GraphComponent component;
    /** Handles the filter */
    protected final GraphFilterController gfc;
    /** Handles the metric */
    protected final GraphStatController gsc;
    /** Handles node decorations */
    protected final GraphDecorController gdc;


    // <editor-fold defaultstate="collapsed" desc="Constructors & Standard Utility Methods">
    //
    // CONSTRUCTORS & FACTORY METHODS
    //

    /** Constructs controller without an object */
    protected GraphController() { this(null, null); }

    /** Construct instance of controller for a regular graph */
    public GraphController(Graph graph) { this(graph, null); }

    /** Construct instance of controller for a regular graph w/ specified name */
    public GraphController(Graph graph, String name) {
        gfc = new GraphFilterController(this);
        gsc = new GraphStatController(this);
        gdc = new GraphDecorController(this, gsc);
        setBaseGraph(graph); // causes filtered graph to change, cascading to a property change where the manager is generated
        setName(name);
    }

    @Override
    public String toString() {
        return "GraphController["+name+"]";
    }
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="Property Patterns">
    //
    // PROPERTY PATTERNS
    //

    /** 
     * Return name of graph
     * @return name of the graph (may be file name, may not be)
     */
    public String getName() { 
        return name;
    }
    
    /**
     * Sets name of graph being controlled.
     * @param name the graph's name (typically a file name)
     */
    public final void setName(String name) {
        if (this.name == null ? name != null : !this.name.equals(name)) {
            String oldName = this.name;
            this.name = name;
            pcs.firePropertyChange("name", oldName, name);
        }
    }
    
    /**
     * Return file of the graph, if it is saved
     * @return file of graph, if saved; null otherwise
     */
    public File getFile() { 
        return file; 
    }
    
    /**
     * Set file of graph
     * @param file file where graph is stored
     */
    public void setFile(File file) {
        if (this.file != file) {
            File old = this.file;
            this.file = file;
            pcs.firePropertyChange("file", old, file);
        }
        this.file = file;
    }
    // </editor-fold>
    
    
    //<editor-fold defaultstate="collapsed" desc="DELEGATE OBJECTS">
    //
    // DELEGATE OBJECTS
    //
    
    /** 
     * Return the graph's component
     * @return the graph's component 
     */
    public final GraphComponent getComponent() {
        if (component == null) {
            component = new GraphComponent(getViewGraph());
            gdc.applyStyles(component.getAdapter());
        }
        return component;
    }
    
    /** 
     * Return filter controller
     * @return filter controller 
     */
    public GraphFilterController getFilterController() { return gfc; }
    /** 
     * Return stat controller
     * @return stat controller 
     */
    public GraphStatController getStatController() { return gsc; }
    /** 
     * Return decor controller
     * @return decor controller 
     */
    public GraphDecorController getDecorController() { return gdc; }

    /** 
     * Return the graph's position/layout manager
     * @return the graph manager
     */
    public final GraphManager getManager() { return getComponent().getGraphManager(); }
    
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="DELEGATE PROPERTIES">
    //
    // DELEGATE PROPERTIES
    //
    
    /** 
     * Return filtered graph for display 
     * @return filtered graph, which is currently visible 
     */
    public Graph getViewGraph() { return gfc.getFilteredGraph(); }
    /** 
     * Return filtered graph's nodes @return viewable nodes 
     */
    public List getViewNodes() { return gfc.getFilteredGraph().nodes(); }

    /** 
     * Return true if filter is enabled
     * @return true if filter is enabled 
     */
    public boolean isFilterEnabled() { return gfc.isEnabled(); }
    /** 
     * Sets filter to be enabled or disabled 
     */
    public void setFilterEnabled(boolean val) { gfc.setEnabled(val); }

    /** 
     * @return active node metric 
     */
    public NodeMetric getMetric() { return gsc.getMetric(); }
    /** 
     * Sets underlying metric 
     */
    public void setMetric(NodeMetric m) { gsc.setMetric(m); }
    /** 
     * Return last computed metric values 
     */
    public List getMetricValues() { return gsc.getMetricValues(); }

    /** @return set of nodes that have highlights */
    public Set getHighlightNodes() { return gdc.getHighlightNodes(); }
    /** Sets nodes that have highlights */
    public void setHighlightNodes(Set subset) { gdc.setHighlightNodes(subset); }

    /** @return map with positions of all nodes */
    public Map<Object, Point2D.Double> getNodePositions() { return getManager().getLocationMap(); }
    /** @return position of specified node */
    public Point2D.Double positionOf(Object node) { return getManager().getLocationMap().get(node); }
    /** Sets positions of nodes in current graph */
    public void setNodePositions(Map<Object, Point2D.Double> pos) { getManager().requestPositionMap(pos); }

    /** Apply specified static layout algorithm (graph will keep animating) */
    public void applyLayout(StaticGraphLayout layout, double... parameters) { getManager().applyLayout(layout, parameters); }
    /** Steps current iterative graph layout */
    public void iterateLayout() { getManager().iterateLayout(); }
    /** @return active layout algorithm */
    public IterativeGraphLayout getLayoutAlgorithm() { return getManager().getLayoutAlgorithm(); }
    /** Sets active layout algorithm */
    public void setLayoutAlgorithm(IterativeGraphLayout layout) { getManager().setLayoutAlgorithm(layout); }
    /** @return true if layout animation is on */
    public boolean isLayoutAnimating() { return getManager().isLayoutAnimating(); }
    /** Turns on/off layout animation */
    public void setLayoutAnimating(boolean value) { getManager().setLayoutAnimating(value); }
    
    /** @return label of specified node */
    public Object getNodeLabel(Object node) { return baseGraph instanceof ValuedGraph ? ((ValuedGraph)baseGraph).getValue(node) : null; }
    /** Sets visible label at specified node */
    public void setNodeLabel(Object node, Object label) {
        if (baseGraph instanceof ValuedGraph) {
            ((ValuedGraph)baseGraph).setValue(node, label);
            component.getAdapter().updateNodeLabels();
        }
    }
    // </editor-fold>


    // <editor-fold defaultstate="collapsed" desc="Event Handling">

    /**
     * Sets up event forwarding from various subcontrollers
     * @param l the listener
     * @param main if listens to main changes
     * @param filter if listens to filter changes
     * @param stat if listens to stat changes
     * @param layout if listens to layout changes
     * @param decor if listens to decor changes
     */
    public void addAllPropertyChangeListener(PropertyChangeListener l,
            boolean main, boolean filter, boolean stat, boolean layout, boolean decor) {
        if (main) 
            addPropertyChangeListener(l);
        if (filter) 
            gfc.addPropertyChangeListener(l);
        if (stat) 
            gsc.addPropertyChangeListener(l);
        if (decor) 
            gdc.addPropertyChangeListener(l);
        if (layout) 
            getManager().addPropertyChangeListener(l);
    }

    /**
     * Removes event listening for this controller and all sub-controllers
     * @param l the listener
     */
    public void removeAllPropertyChangeListener(PropertyChangeListener l) {
        this.removePropertyChangeListener(l);
        gfc.removePropertyChangeListener(l);
        gsc.removePropertyChangeListener(l);
        gdc.removePropertyChangeListener(l);
        getManager().removePropertyChangeListener(l);
    }

    /**
     * Sets up a "follows" relationship for the specified controller.
     * Whenever this graph's "view" changes, the specified controller
     * will update its base graph to the current view grpah
     */
    void addViewGraphFollower(final AbstractGraphController controller) {
        addPropertyChangeListener("viewGraph", new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                controller.setBaseGraph(getViewGraph());
            }
        });
    }

    /**
     * Sets up a "follows" relationship for the specified controller.
     * Whenever this graph's "base graph" changes, the specified controller
     * will update its base graph to that base graph.
     */
    void addBaseGraphFollower(final AbstractGraphController controller) {
        addPropertyChangeListener("baseGraph", new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                controller.setBaseGraph(getBaseGraph());
            }
        });
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals("status") || prop.equals("output"))
            pcs.firePropertyChange(evt);
        else if (prop.equals("filteredGraph")) {
            if (evt.getNewValue() != null)
                getManager().setGraph((Graph) evt.getNewValue());
            pcs.firePropertyChange("viewGraph", evt.getOldValue(), evt.getNewValue());
            gdc.applyStyles(component.getAdapter());
        } else if (prop.equals("nodeStyler")) {
            if (getComponent() != null)
                component.getAdapter().updateAppearance();
        }
    }
    // </editor-fold>

}
