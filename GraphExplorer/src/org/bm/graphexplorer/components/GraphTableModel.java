/*
 * GraphTableModel.java
 * Created May 15, 2010
 */

package org.bm.graphexplorer.components;

import org.bm.graphexplorer.controllers.GraphController;
import org.bm.graphexplorer.controllers.GraphDecorController;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.ValuedGraph;
import org.bm.blaise.scio.graph.metrics.NodeMetric;
import org.bm.blaise.specto.plane.graph.GraphManager;

/**
 * <p>
 * Provides a model for displaying a table of vertices within a graph.
 * Displays the index of a vertex, its label (if a labeled graph),
 * its screen location, the degree, and (optionally) its value for a particular metric.
 *</p>
 * 
 * @see Graph
 *
 * @author Elisha Peterson
 */
public final class GraphTableModel extends AbstractTableModel
        implements PropertyChangeListener {

    /** The overall graph controller */
    GraphController gc = null;
    /** Tracks the values for the metric for each vertex */
    transient List<Number> values;

    /** Constructs without a graph */
    public GraphTableModel() {
    }

    /** Constructs with specified graph. */
    public GraphTableModel(GraphController gc) {
        setController(gc);
    }

    /** Sets the controller that contains the data backed by this model. */
    public void setController(GraphController gc) {
        if (this.gc != gc) {
            if (this.gc != null)
                this.gc.removeAllPropertyChangeListener(this);
            this.gc = gc;
            if (gc != null)
                gc.addAllPropertyChangeListener(this, true, false, true, true, true);
            updateValues();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String $PROP = evt.getPropertyName();
        if ($PROP.equals("activeController")) {
            GraphController gc = (GraphController) evt.getNewValue();
            setController(gc);
        } else if ($PROP.equals("viewGraph") || $PROP.equals("positionData"))
            fireTableDataChanged();
        else if ($PROP.equals("metricValues")) {
            NodeMetric m = gc.getMetric();
            String newTitle = m == null ? "Metric" : m.toString();
            colNames[COL_METRIC] = newTitle;
            updateValues();
        } else if ($PROP.equals("highlightNodes"))
            fireTableDataChanged();
    }

    /** Updates table getValues based on current GraphController */
    private void updateValues() {
        values = gc == null ? null : gc.getMetricValues();
        fireTableDataChanged();
    }


    // <editor-fold defaultstate="collapsed" desc="TableModel Methods">

    public int getRowCount() {
        if (gc == null) return 0;
        Graph graph = gc.getViewGraph();
        return graph == null ? 0 : graph.order();
    }

    static final int                     COL_NODE = 0,   COL_LABEL = 1, COL_LOC = 2,  COL_DEGREE = 3, COL_METRIC = 4;
    protected String[] colNames =       { "Node",       "Label",       "Location",   "Degree",       "Metric" };
    protected Class[] colTypes =        { Object.class, String.class,  String.class, Integer.class,  Number.class };
    static final int[] PREFERRED_WIDTH = { 20,           60,            50,           20,             30 };

    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int col) {
        return colNames[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return colTypes[col];
    }

    public Object getValueAt(int row, int col) {
        Graph graph = gc.getViewGraph();
        try {
            Object node = graph.nodes().get(row);
            switch (col) {
                case COL_NODE :
                    return node;
                case COL_LABEL :
                    Object value = gc.getNodeLabel(node);
                    if (value == null)
                        return null;
                    else if (value.getClass().isArray()) {
                        Object[] value2 = (Object[]) value;
                        for (Object o : value2)
                            if (o instanceof String)
                                return o;
                        return Arrays.deepToString(value2);
                    } else {
                        return value.toString();
                    }
                case COL_LOC:
                    Point2D.Double pos = gc.positionOf(node);
                    return pos == null ? "unknown" : String.format("(%.2f,%.2f)", pos.x, pos.y);
                case COL_DEGREE :
                    return graph.degree(node);
                case COL_METRIC:
                    if (values == null || row >= values.size())
                        values = gc.getMetricValues();
                    return values == null ? null : values.get(row);
            }
        } catch (IndexOutOfBoundsException ex) {
            return null;
        }
        throw new IllegalArgumentException("Graph's table does not contain entry at (row, col) = (" + row + ", " + col + ").");
    }

    public boolean isLabelColumn(int col) {
        return col == COL_LABEL;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return gc.getBaseGraph() instanceof ValuedGraph && col == COL_LABEL;
    }
    
    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (!isCellEditable(row, col))
            throw new IllegalArgumentException("GraphTableModel: cannot set value at (row, col) = (" + row + ", " + col + ") to " + aValue);
        gc.setNodeLabel(getValueAt(row, COL_NODE), aValue);
    }
    // </editor-fold>

}
