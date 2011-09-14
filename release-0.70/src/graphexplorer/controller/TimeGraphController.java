/*
 * TimeGraphController.java
 * Created Nov 18, 2010
 */

package graphexplorer.controller;

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
        tComponent = new TimeGraphComponent(tManager);
        tComponent.setUpdateWithTime(false); // the graph component does not update itself when the time changes
        component = tComponent.getGraphComponent();
        component.setGraphManager(getManager());
        component.getAdapter().setNodeRenderer(gdc.getNodeRenderer());
        component.getAdapter().setNodeCustomizer(gdc.getNodeCustomizer());
        tManager.addPropertyChangeListener(this);
        setBaseGraph(tManager.getSlice());
        setName(name);
    }

    @Override
    public String toString() {
        return "LongitudinalGraphController";
    }

    /** @return manager for the controller */
    public TimeGraphManager getTimeGraphManager() { return tManager; }
    /** @return time slice */
    public double getTime() { return tManager.getTime(); }
    public void setTime(Double t) { tManager.setTime(t); }

    public final TimeGraphComponent getTimeGraphComponent() {
        return tComponent;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals(TimeGraphManager.$TIME))
            setBaseGraph(tManager.getSlice());
        else if (prop.equals(GraphFilterController.$FILTERED_GRAPH)) {
            if (evt.getNewValue() != tManager.getSlice()) {
                // filtering of longitudinal graph == special case
                tManager.setFilter(gfc.getFilterThreshold());
            }
            super.propertyChange(evt);
        } else if (prop.equals(GraphManager.$POSITIONS)) {
            component.getGraphManager().requestPositionMap((Map<Object,Point2D.Double>) evt.getNewValue());
        } else
            super.propertyChange(evt);
    }

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



}
