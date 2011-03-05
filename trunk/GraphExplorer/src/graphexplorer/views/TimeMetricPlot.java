/*
 * TimeMetricPlot.java
 * Created Aug 4, 2010
 */

package graphexplorer.views;

import edu.jhuapl.simplechart.SimpleChart;
import edu.jhuapl.simplechart.data.MultiSeriesModel;
import edu.jhuapl.simplechart.viz.XYViz;
import graphexplorer.controller.GraphDecorController;
import graphexplorer.controller.GraphFilterController;
import graphexplorer.controller.GraphStatController;
import graphexplorer.controller.TimeGraphController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bm.blaise.scio.graph.FilteredWeightedGraph;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.WeightedGraph;
import org.bm.blaise.scio.graph.time.TimeGraph;
import org.bm.blaise.scio.graph.metrics.NodeMetric;

/**
 * Provides views of metrics over time; creates a line graph displaying the value
 * of the controller's special-node subset over the time frame specified by a
 * longitudinal graph & the controller's active metric.
 * @author Elisha Peterson
 */
public class TimeMetricPlot extends javax.swing.JPanel
        implements PropertyChangeListener {

    /** The underlying controller */
    private TimeGraphController gc;
    /** The chart */
    private final SimpleChart chart = new SimpleChart();

    /** Construct with specified controller */
    public TimeMetricPlot() {
        chart.setHLabel("Time");
        chart.setVLabel("Metric Value");
        chart.setTitle("Metric Distribution over Time");
        chart.setPreferredSize(new Dimension(400,300));
        setLayout(new BorderLayout());
        add(chart, BorderLayout.CENTER);
    }

    /** @return current graph controller */
    public TimeGraphController getController() { return gc; }
    /** Sets current graph controller */
    public void setController(TimeGraphController tgc) {
        if (this.gc != null) {
            gc.removePropertyChangeListener(this);
            gc.getFilterController().removePropertyChangeListener(this);
        }
        this.gc = tgc;
        if (tgc != null) {
            tgc.addPropertyChangeListener(this);
            tgc.getFilterController().addPropertyChangeListener(this);
            tgc.getStatController().addPropertyChangeListener(this);
            tgc.getDecorController().addPropertyChangeListener(this);
        }
    }


    /** Updates chart based on current controller */
    void updateChart() {       
        TimeGraph lg = gc.getTimeGraphManager().getTimeGraph();
        NodeMetric metric = gc.getMetric();
        Set set = gc.getHighlightNodes();

        if (lg == null || metric == null || set == null || set.isEmpty()) {
            chart.setViz(null);
        } else {
            List<Double> times = lg.getTimes();
            ArrayList subset = new ArrayList(set); // ensures consistent ordering

            int n_subset = subset.size();
            int n_time = times.size();

            Double[][][] dataArr = new Double[n_subset][n_time][2];
            for (int i_time = 0; i_time < n_time; i_time++) {
                Graph g = lg.slice(times.get(i_time), true);
                if (gc.isFilterEnabled()) {
                    g = FilteredWeightedGraph.getInstance((WeightedGraph) g);
                    ((FilteredWeightedGraph)g).setThreshold(gc.getFilterController().getFilterThreshold());
                }
                List nodes = g.nodes();
                List values = metric.allValues(g);
                for (int i_subset = 0; i_subset < n_subset; i_subset++) {
                    int index = nodes.indexOf(subset.get(i_subset));
                    if (index != -1) {
                        dataArr[i_subset][i_time][0] = times.get(i_time);
                        dataArr[i_subset][i_time][1] = ((Number)values.get(index)).doubleValue();
                    }
                }
            }

            MultiSeriesModel<Double[]> m = MultiSeriesModel.getInstance(dataArr);
            // TODO - fix chart
//            chart.setViz(new XYViz(m, false));
            chart.setVLabel("Value of Metric " + metric);
            chart.setTitle("Distribution of " + metric + " over time");
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String pn = evt.getPropertyName();
        if (pn.equals(GraphFilterController.$FILTERED_GRAPH) || pn.equals(GraphFilterController.$FILTER_THRESHOLD)
                || pn.equals(GraphStatController.$METRIC) || pn.equals(GraphDecorController.$HIGHLIGHT))
            updateChart();
    }

}
