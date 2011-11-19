/*
 * GraphDecorController.java
 * Created Nov 18, 2010
 */

package org.bm.graphexplorer.controllers;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.bm.blaise.specto.plane.graph.PlaneGraphAdapter;
import org.bm.blaise.style.Anchor;
import org.bm.blaise.style.PointStyleSupport;
import org.bm.blaise.style.BasicPointStyle;
import org.bm.blaise.style.BasicPathStyle;
import org.bm.blaise.style.DecoratorPointStyle;
import org.bm.blaise.style.PointStyle;
import org.bm.blaise.scio.graph.WeightedGraph;
import org.bm.blaise.style.BasicStringStyle;
import org.bm.blaise.style.PathStyle;
import org.bm.blaise.style.StringStyle;
import org.bm.util.Delegator;

/**
 * <p>
 * Responsible for managing "decorations" of a graph, e.g. node sizes and/or colors
 * based on metric computations, or preset values.
 * </p>
 *
 * @author elisha
 */
public final class GraphDecorController extends AbstractGraphController {


    /** Special style for nodes in GraphExplorer */
    public class NodeStyler implements Delegator<Object,PointStyle> {
        /** Subset of interest (highlight applied) */
        private Set subset = null;
        /** Values determining relative size */
        private Map<Object, Float> values = new HashMap<Object, Float>();
        /** Min & max node values */
        private float fmin = 0, fmax = 1;
        /** Whether to generate unique colors per node */
        private boolean distinctColors = false;
        /** Cached color map for nodes and colors */
        private final Map<Object,Color> colorMap = Collections.synchronizedMap(new HashMap<Object,Color>());
        
        /** Base point renderer object */
        private final BasicPointStyle baseRenderer = new BasicPointStyle();
        /** Highlight renderer object */
        private final PointStyleSupport highlightRenderer = new DecoratorPointStyle(baseRenderer, new Color(255, 128, 128));

        /** Return base renderer */
        public BasicPointStyle getBaseStyle() { return baseRenderer; }
        
        /** Updates node values & stats */
        public synchronized void setNodeValues(List valueList, SummaryStatistics stats) {
            List nodes = baseGraph == null ? null : baseGraph.nodes();
            if (nodes != null && valueList != null && valueList.size() > 0) {
                for(int i = 0; i < Math.min(nodes.size(), valueList.size()); i++) {
                    double v = Math.sqrt(valueList.get(i) instanceof Number ? ((Number)valueList.get(i)).doubleValue() : 1);
                    values.put(nodes.get(i), (float) v);
                }
            }
            fmin = stats == null ? 0 : (float) Math.sqrt(stats.getMin());
            fmax = stats == null ? 1 : (float) Math.sqrt(stats.getMax());
        }
    
        public synchronized PointStyle of(Object src) {
            PointStyleSupport base = subset != null && subset.contains(src)
                    ? highlightRenderer
                    : baseRenderer;
            float val = values == null || values.isEmpty() ? 1f
                    : values.containsKey(src) ? Math.max(0.1f, values.get(src)/fmax)
                    : 0.1f;
            if (distinctColors) {
                if (!colorMap.containsKey(src))
                    colorMap.put(src, nextColor());
                return new DecoratorPointStyle(base, val, colorMap.get(src));
            } else
                return new DecoratorPointStyle(base, val);
        }
    } // INNER CLASS NodeStyler

    
    /** Special style for node labels in GraphExplorer */
    public class NodeLabelStyler implements Delegator<Object,StringStyle> {
        /** Root style object */
        private final BasicStringStyle rootStyle = new BasicStringStyle();
        /** Whether labels are visible */
        private boolean visible = true;
        
        public StringStyle of(Object src) { return visible ? rootStyle : null; }

        public boolean isVisible() { return visible; }
        public void setVisible(boolean val) { this.visible = val; }
        
        /** Return base renderer */
        public BasicStringStyle getBaseStyle() { return rootStyle; }
        
        public Point2D getOffset() { return rootStyle.getOffset(); }
        public void setOffset(Point2D off) { rootStyle.setOffset(off); }
        public float getFontSize() { return rootStyle.getFontSize(); }
        public void setFontSize(float size) { rootStyle.setFontSize(size); }
        public Font getFont() { return rootStyle.getFont(); }
        public void setFont(Font font) { rootStyle.setFont(font); }
        public Color getColor() { return rootStyle.getColor(); }
        public void setColor(Color color) { rootStyle.setColor(color); }
        public Anchor getAnchor() { return rootStyle.getAnchor(); }
        public void setAnchor(Anchor newValue) { rootStyle.setAnchor(newValue); }
    }
    
    /** Special style for edges in GraphExplorer */
    public class EdgeStyler implements Delegator<Object,PathStyle> {
        /** Root style object */
        private final BasicPathStyle rootStyle = new BasicPathStyle(new Color(32,128,32,192), 1f);
        /** Return base renderer */
        public BasicPathStyle getBaseStyle() { return rootStyle; }
        public PathStyle of(Object src) { return rootStyle; }
    }
    
    
    //
    // MAIN CLASS
    //
    
    /** Node styler */
    private final NodeStyler nodeStyler = new NodeStyler();
    /** Node labeler */
    private final Delegator<Object, String> nodeLabeler = new Delegator<Object,String>() {
        public String of(Object o) { return o == null ? "" : o.toString(); }
    };
    /** Label styler */
    private final NodeLabelStyler nodeLabelStyler = new NodeLabelStyler();
    /** Edge styler */
    private final EdgeStyler edgeStyler = new EdgeStyler();
    
    //
    // CONSTRUCTORS
    //

    /** Set up with listening for specified controllers. */
    public GraphDecorController(GraphController gc, final GraphStatController mc) {
        gc.addViewGraphFollower(this); // automatically notified when the view graph changes
        mc.addPropertyChangeListener("metricValues", new PropertyChangeListener(){
            public void propertyChange(PropertyChangeEvent evt) {
                nodeStyler.setNodeValues((List) evt.getNewValue(), mc.getStats());
                fireNodeStyleUpdate();
            }
        });
        addPropertyChangeListener(gc);
        setBaseGraph(gc.getViewGraph());
    }
    
    /** Sets up the specified adapter with styles. */
    void applyStyles(PlaneGraphAdapter adapter) {
        adapter.getNodeStyler().setStyleDelegate(nodeStyler);
        adapter.getNodeStyler().setLabelDelegate(nodeLabeler);
        adapter.getNodeStyler().setLabelStyleDelegate(nodeLabelStyler);
        adapter.getEdgeStyler().setStyleDelegate(edgeStyler);
    }

    private void fireNodeStyleUpdate() {
        pcs.firePropertyChange("nodeStyler", null, nodeStyler);
    }
    
    //
    // PROPERTY PATTERNS
    //

    public NodeStyler getNodeStyler() { return nodeStyler; }
    public EdgeStyler getEdgeStyler() { return edgeStyler; }
    public NodeLabelStyler getNodeLabelStyler() { return nodeLabelStyler; }
    
    /** Overrides current node colors to give each node a different color */
    public void setDistinctColors(boolean val) {
        if (nodeStyler.distinctColors != val) {
            nodeStyler.distinctColors = val;
            fireNodeStyleUpdate();
        }
    }

    /** @return highlighted subset of nodes (non-null, but maybe empty) */
    public Set getHighlightNodes() {
        return nodeStyler.subset == null ? Collections.emptySet() : nodeStyler.subset;
    }
    /**
     * Sets the current selection of nodes for the subset. If any elements of the
     * subset <i>are not</i> in the larger set of nodes, an exception is thrown.
     * @param subset subset of nodes to select; if null, the subset is set to the empty subset
     * @throws IllegalArgumentException if the subset is not contained in the set
     *   of nodes of the primary graph object (or all its nodes if the graph is longitudinal)
     */
    public void setHighlightNodes(Set subset) {
        if (nodeStyler.subset != subset) {
            Set oldSubset = nodeStyler.subset;
            nodeStyler.subset = subset == null ? new HashSet() : subset;
            fireNodeStyleUpdate();
            pcs.firePropertyChange("highlightNodes", oldSubset, nodeStyler.subset);
        }
    }

    //
    // DECOR PROPERTY GETTERS
    //

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
    
    
    //<editor-fold defaultstate="collapsed" desc="PRIVATE METHODS & INNER CLASSES">
    //
    // UTILITY CLASSES
    //

    int i = 0;
    /** Generates next color in sequence */
    private Color nextColor() {
        i++;
        return new Color(37*i%255, 17*i%255, 67*i%255);
    }
    
    //</editor-fold>
}
