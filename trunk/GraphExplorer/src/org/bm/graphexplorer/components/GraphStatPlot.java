/*
 * GraphStatPlot.java
 * Created on Dec 20, 2010, 6:27:39 PM
 */

package org.bm.graphexplorer.components;

import edu.jhuapl.simplechart.SimpleChart;
import edu.jhuapl.simplechart.axis.AxisLabelDelegate;
import edu.jhuapl.simplechart.data.ArraySeriesModel;
import edu.jhuapl.simplechart.data.DataAddress;
import edu.jhuapl.simplechart.data.MultiSeriesModel;
import edu.jhuapl.simplechart.data.delegate.TooltipDelegate;
import edu.jhuapl.simplechart.viz.AbstractViz;
import edu.jhuapl.simplechart.viz.AreaViz;
import edu.jhuapl.simplechart.viz.ColumnViz;
import edu.jhuapl.simplechart.viz.XYViz;
import org.bm.graphexplorer.controllers.GraphController;
import org.bm.graphexplorer.controllers.GraphStatController;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;

/**
 * Provides a view of the current graph's stats.
 * @author elisha
 */
public class GraphStatPlot extends javax.swing.JPanel
        implements PropertyChangeListener {

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
        GraphStatController nue = gc == null ? null : gc.getStatController();
        if (gsc != nue) {
            if (gsc != null)
                gsc.removePropertyChangeListener(this);
            gsc = nue;
            if (gsc != null)
                gsc.addPropertyChangeListener(this);
            updateValues();
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (prop.equals("activeController")) {
            setController((GraphController) evt.getNewValue());
        } else if (prop.equals("metric")) {
            chart.setTitle(evt.getNewValue() + " Distribution");
        } else if (prop.equals("metricValues")) {
            try {
                updateValues();
            } catch (Exception ex) {
                System.err.println("Error updating chart values: " + ex);
            }
        }
    }

    /** Updates the display. */
    void updateValues() {
        long t0 = System.currentTimeMillis();
        List values = gsc == null ? null : gsc.getMetricValues();
        if (values == null || gsc.getMetric() == null) {
            if (chart != null)
                chart.clearViz();
        } else {
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
                viz.setTooltipDelegate(new TooltipDelegate(){
                    public String getTooltip(Object o, DataAddress da) {
                        Double[] d = (Double[]) o;
                        return String.format("value = %.3f", d[0]);
                    }
                });
                chart.getVAxis().setVisible(false);
                chart.setPrimaryViz(viz);
            } else {
                // use line chart
                Double[] data = new Double[(int)Math.floor(model.getMax())+1];
                int countsIndex = 0;
                for (int i = 0; i <= model.getMax(); i++) {
                    if (((Number)model.getValueAt(countsIndex, 0)).intValue() == i) {
                        data[i] = (double) counts[countsIndex];
                        countsIndex++;
                    } else
                        data[i] = 0.0;
                }
                MultiSeriesModel dataModel = new MultiSeriesModel(new ArraySeriesModel(data));
                AbstractViz viz = model.getMax() > 25 ? new AreaViz(dataModel)
                    : new ColumnViz(dataModel);
                viz.setTooltipDelegate(new TooltipDelegate(){
                    public String getTooltip(Object o, DataAddress da) {
                        return ((Double)o).intValue() + " nodes have value " + da.sample;
                    }
                });
                chart.setVLabel("Number of Nodes");
                chart.getVAxis().setVisible(true);
                chart.setPrimaryViz(viz);
                String[] labels = new String[data.length];
                for (int i = 0; i < labels.length; i++)
                    labels[i] = "" + i;
                chart.getHAxis().setLabelDelegate(new AxisLabelDelegate.Slot(labels));
            }
        }
        long t1 = System.currentTimeMillis();
        if ((t1-t0)>50)
            System.err.println("Long stat plot update: " + (t1-t0)+"ms");
    }
}
