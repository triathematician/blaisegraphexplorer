/*
 * GraphStatPlot.java
 * Created on Dec 20, 2010, 6:27:39 PM
 */

package graphexplorer.views;

import edu.jhuapl.simplechart.AxisDisplayManager;
import edu.jhuapl.simplechart.SimpleChart;
import edu.jhuapl.simplechart.convert.CSlot;
import edu.jhuapl.simplechart.data.MultiSeriesModel;
import edu.jhuapl.simplechart.viz.AbstractViz;
import edu.jhuapl.simplechart.viz.AreaViz;
import edu.jhuapl.simplechart.viz.BarViz;
import edu.jhuapl.simplechart.viz.XYViz;
import graphexplorer.controller.GraphController;
import graphexplorer.controller.GraphControllerListener;
import graphexplorer.controller.GraphStatController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.util.Arrays;
import java.util.List;
import util.stats.views.FrequencyTableModel;

/**
 * Provides a view of the current graph's stats.
 * @author elisha
 */
public class GraphStatPlot extends javax.swing.JPanel
        implements GraphControllerListener {

    /** The stat controller */
    private GraphStatController gsc = null;
    /** The chart */
    private final SimpleChart chart = new SimpleChart();

    /** Frequency table model */
    private final FrequencyTableModel model = new FrequencyTableModel(Arrays.asList());

    /** Construct instance of the plot panel */
    public GraphStatPlot() {
        setLayout(new BorderLayout());
        chart.setHLabel("Value");
        chart.setVLabel("Number of Nodes");
        chart.setTitle("Metric Distribution");
        chart.setPreferredSize(new Dimension(400,300));
        add(chart, BorderLayout.CENTER);
    }

    /** @return table model for frequency plot */
    public FrequencyTableModel getTableModel() { return model; }

    public void setController(GraphController gc) {
        if (gsc != gc.getStatController()) {
            if (gsc != null)
                gsc.removePropertyChangeListener(this);
            gsc = gc.getStatController();
            if (gsc != null)
                gsc.addPropertyChangeListener(this);
            updateValues();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        try {
            updateValues();
        } catch (Exception ex) {
            System.err.println("Error updating chart values: " + ex);
        }
    }

    /** Updates the display. */
    void updateValues() {
        long t0 = System.currentTimeMillis();
        List values = gsc.getValues();
        if (values == null || gsc.getMetric() == null) {
            if (chart != null)
                chart.setViz(null);
        } else {
            String metricString = gsc.getMetric().toString();
            chart.setTitle(metricString + " Distribution");
            model.setValues(values);
            int[] counts = model.getCounts();
            double avgCount = values.size() / counts.length;
            if (!model.intBased() || avgCount < 2) {
                // use point chart
                Double[][][] data = new Double[1][values.size()][2];
                double y0 = .5;
                for (int i = 0; i < values.size(); i++) {
                    y0 = Math.sin(1+5000*y0);
                    data[0][i][0] = ((Number)values.get(i)).doubleValue();
                    data[0][i][1] = y0;
                }
                XYViz viz = new XYViz(MultiSeriesModel.getInstance(data));
                chart.setVLabel("(irrelevant)");
                chart.setVAxisVisible(false);
                chart.setViz(viz);
            } else {
                // use line chart
                Double[][] data = new Double[1][(int)Math.floor(model.getMax())+1];
                int countsIndex = 0;
                for (int i = 0; i <= model.getMax(); i++) {
                    if (((Number)model.getValueAt(countsIndex, 0)).intValue() == i) {
                        data[0][i] = (double) counts[countsIndex];
                        countsIndex++;
                    } else
                        data[0][i] = 0.0;
                }
                MultiSeriesModel dataModel = MultiSeriesModel.getInstance(data);
                AbstractViz viz = model.getMax() > 25 ? new AreaViz(dataModel)
                    : new BarViz(dataModel);
                chart.setVLabel("Number of Nodes");
                chart.setVAxisVisible(true);
                chart.setViz(viz);
                String[] labels = new String[data[0].length];
                for (int i = 0; i < labels.length; i++)
                    labels[i] = "" + i;
                chart.setHAxisDisplayManager(new AxisDisplayManager.CSlotManager((CSlot) viz.getHGeometry(), labels));
            }
        }
        long t1 = System.currentTimeMillis();
        if ((t1-t0)>50)
            System.err.println("Long stat plot update: " + (t1-t0)+"ms");
    }
}
