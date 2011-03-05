/*
 * GraphDecorController.java
 * Created Nov 18, 2010
 */

package graphexplorer.controller;

import graphics.renderer.AbstractPointRenderer;
import graphics.renderer.BasicPointRenderer;
import graphics.renderer.BasicShapeRenderer;
import graphics.renderer.BasicStrokeRenderer;
import graphics.renderer.PointRenderer;
import graphics.renderer.ShapeLibrary;
import graphics.renderer.ShapeRenderer;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bm.blaise.scio.graph.ValuedGraph;
import org.bm.blaise.scio.graph.WeightedGraph;
import utils.MapGetter;

/**
 * <p>
 * Responsible for managing "decorations" of a graph, e.g. node sizes and/or colors
 * based on metric computations, or preset values.
 * </p>
 *
 * @author elisha
 */
public final class GraphDecorController extends AbstractGraphController
        implements PropertyChangeListener {

    // <editor-fold defaultstate="collapsed" desc="Property Change Constants">
    //
    // PROPERTY CHANGE NAMES
    //

    /** Changes to the "highlighted" subset of nodes. */
    public static final String $HIGHLIGHT = "highlight";
    /** Node customizer changes. */
    public static final String $NODE_CUSTOMIZER = "node customizer";
    /** Node labels changes. */
    public static final String $NODE_LABELS = "node labels";
    // </editor-fold>

    // <editor-fold desc="Properties">

    /** Stores values of the metric */
    private List values = null;
    /** Stores the "subset" of the graph that is currently of interest (may be null or empty) */
    private Set subset = null;

    /** Edge renderer */
    private final BasicStrokeRenderer edgeRenderer = new BasicStrokeRenderer(new Color(32,128,32,192), 1f);
    /** Base point renderer object */
    private final BasicPointRenderer baseRenderer = new BasicPointRenderer();
    /** Highlight renderer object */
    private final AbstractPointRenderer highlightRenderer = new AbstractPointRenderer(){
        Color c = new Color(255, 128, 128);
        @Override public ShapeLibrary getShape() { return baseRenderer.getShape(); }
        @Override public float getRadius() { return baseRenderer.getRadius(); }
        @Override public ShapeRenderer getShapeRenderer() { return new BasicShapeRenderer(c, baseRenderer.getStroke(), baseRenderer.getThickness()); }
    };
    /** The object that provides the "decor" for nodes */
    private MapGetter<PointRenderer> nodeDecor = null;
    // </editor-fold>

    //
    // CONSTRUCTORS
    //

    /** Set up with listening for specified controllers. */
    public GraphDecorController(GraphController gc, GraphStatController mc) {
        gc.addViewGraphFollower(this); // automatically notified when the view graph changes
        mc.addPropertyChangeListener(GraphStatController.$VALUES, this);
        addPropertyChangeListener(gc);
        setBaseGraph(gc.getViewGraph());
    }

    /** Cached color map for nodes and colors */
    private final Map<Object,Color> colorMap = Collections.synchronizedMap(new HashMap<Object,Color>());
    int i = 0;
    /** Generates next color in sequence */
    private Color nextColor() {
        i++;
        return new Color(37*i%255, 17*i%255, 67*i%255);
    }

    boolean distinctColors = false;

    public void distinctColors() {
        distinctColors = true;
        updateDecor();
    }

    public PointRenderer getNodeRenderer() { return baseRenderer; }
    public MapGetter<PointRenderer> getNodeCustomizer() { return nodeDecor; }
    public BasicStrokeRenderer getEdgeRenderer() { return edgeRenderer; }
    public MapGetter<BasicStrokeRenderer> getEdgeCustomizer() { return null; }

    private void updateDecor() {
        if (baseGraph == null)
            return;
        List nodes = baseGraph.nodes();
        double max = 0;
        final Map<Object, Float> vMap = new HashMap<Object, Float>();
        if (values == null || values.isEmpty())
            max = 1;
        else {
            for(int i = 0; i < Math.min(nodes.size(), values.size()); i++) {
                double v = Math.sqrt(weightOf(nodes.get(i)));
                max = Math.max(max, v);
                vMap.put(nodes.get(i), (float) v);
            }
        }
        final float fmax = max == 0 ? 1f : (float) max;
        nodeDecor = new MapGetter<PointRenderer>() {
            public PointRenderer getElement(final Object o) {
                AbstractPointRenderer base = subset != null && subset.contains(o)
                        ? highlightRenderer
                        : baseRenderer;
                float val = values == null || values.isEmpty() ? 1f
                        : vMap.containsKey(o) ? Math.max(0.1f, vMap.get(o) / fmax)
                        : 0.1f;
                if (distinctColors) {
                    if (!colorMap.containsKey(o))
                        colorMap.put(o, nextColor());
                    return new FollowRenderer(base, val, colorMap.get(o));
                } else
                    return new FollowRenderer(base, val);
            }
        };
        pcs.firePropertyChange($NODE_CUSTOMIZER, null, nodeDecor);
    }

    /** A renderer that generally defers to a base renderer, except for radius */
    private static class FollowRenderer extends AbstractPointRenderer {
        private final AbstractPointRenderer base;
        private final Float rad;
        private final Color col;
        FollowRenderer(AbstractPointRenderer base, float rad) {
            this.base = base;
            this.rad = rad;
            col = null;
        }
        private FollowRenderer(AbstractPointRenderer base, Color c) {
            this.base = base;
            this.col = c;
            this.rad = 1f;
        }

        private FollowRenderer(AbstractPointRenderer base, float val, Color get) {
            this.base = base;
            this.rad = val;
            this.col = get;
        }
        @Override public ShapeLibrary getShape() { return base.getShape(); }
        @Override public ShapeRenderer getShapeRenderer() {
            if (col == null)
                return base.getShapeRenderer();
            BasicShapeRenderer r = (BasicShapeRenderer) base.getShapeRenderer();
            return new BasicShapeRenderer(col, r.getStroke(), r.getThickness());
        }
        @Override public float getRadius() { return rad * base.getRadius(); }
    }

    //
    // EVENT HANDLING
    //

    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals(GraphStatController.$VALUES)) {
            values = (List) evt.getNewValue();
            updateDecor();
        }
    }

    //
    // PROPERTY PATTERNS
    //

    /** @return highlighted subset of nodes (non-null, but maybe empty) */
    public Set getHighlightNodes() {
        return subset == null ? Collections.emptySet() : subset;
    }
    /**
     * Sets the current selection of nodes for the subset. If any elements of the
     * subset <i>are not</i> in the larger set of nodes, an exception is thrown.
     * @param subset subset of nodes to select; if null, the subset is set to the empty subset
     * @throws IllegalArgumentException if the subset is not contained in the set
     *   of nodes of the primary graph object (or all its nodes if the graph is longitudinal)
     */
    public void setHighlightNodes(Set subset) {
        if (this.subset != subset) {
            Set oldSubset = this.subset;
            if (subset == null) {
                this.subset = new HashSet();
                pcs.firePropertyChange($HIGHLIGHT, oldSubset, this.subset);
            } else {
                if (baseGraph.nodes().containsAll(subset)) {
                    this.subset = subset;
                    pcs.firePropertyChange($HIGHLIGHT, oldSubset, this.subset);
                } else {
                    reportStatus("Invalid node subset: " + subset);
                    pcs.firePropertyChange($HIGHLIGHT, oldSubset, this.subset);
                }
            }
            updateDecor();
        }
    }

    //
    // DECOR PROPERTY GETTERS
    //

    /** @return label corresponding to specified node in graph */
    public String labelOf(Object node) {
        if (baseGraph instanceof ValuedGraph) {
            return ((ValuedGraph) baseGraph).getValue(node).toString();
        } else {
            reportStatus("ERROR: unable to return node label: graph is not a valued graph!");
            return node.toString();
        }
    }

    /** @return weight/size of specified node (as a percentage of maximum size) */
    public double weightOf(Object node) {
        if (values == null) return 1.0;
        int i = baseGraph.nodes().indexOf(node);
        if (i == -1) return 1.0;
        Object value = values.get(i);
        return value instanceof Number
                ? ((Number)value).doubleValue()
                : 1.0;
    }

    /** @return weight of an edge */
    public double weightOf(Object node1, Object node2) {
        if (baseGraph instanceof WeightedGraph) {
            WeightedGraph wg = (WeightedGraph) baseGraph;
            Object weight = wg.getWeight(node1, node2);
            return weight instanceof Number
                    ? ((Number)weight).doubleValue()
                    : 1.0;
        }
        return 1.0;
    }

    //
    // DECOR PROPERTY SETTERS
    //

    /**
     * Sets the label corresponding to the specified node in the graph.
     * If the graph is already a "valued graph", this updates the value.
     * If not, returns an error.
     *
     * @param node the node to label
     * @param label the label of the node
     */
    public void setNodeLabel(Object node, String label) {
        if (baseGraph instanceof ValuedGraph) {
            ValuedGraph vg = (ValuedGraph) baseGraph;
            Object oldValue = vg.getValue(node);
            vg.setValue(node, label);
            pcs.firePropertyChange($NODE_CUSTOMIZER, oldValue, label);
        } else {
            reportStatus("ERROR: unable to change node label: graph is not a valued graph!");
        }
    }

}
