/*
 * GraphStatController.java
 * Created Nov 11, 2010
 */

package org.bm.graphexplorer.controllers;

import org.bm.blaise.scio.graph.metrics.subset.AdditiveSubsetMetric;
import org.bm.blaise.scio.graph.metrics.subset.CooperationSubsetMetric;
import org.bm.blaise.scio.graph.metrics.subset.ContractiveSubsetMetric;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.JOptionPane;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.GraphServices;
import org.bm.blaise.scio.graph.metrics.*;
import org.bm.graphexplorer.components.CooperationPanel;

/**
 * <p>
 * This controller is used to compute getValues of metrics on a graph.
 * </p>
 * <p>
 * The getValues of the metric are not stored by default, but they are cached in case the
 * user requests the same getValues more than once. The computation of metric getValues is done
 * in a background thread. If a request is sent while the thread is running, the getValues
 * returns as null. Otherwise, a property change is sent to notify listeners when the
 * computed getValues are available.
 * </p>
 * @author elisha
 */
public class GraphStatController extends AbstractGraphController {

    /** Stores the active metric (may be null) */
    private NodeMetric metric = null;
    /** Caches calculations for all metrics. */
    private GraphStats statsCache;

    //
    // CONSTRUCTOR
    //

    /** Constructs a new instance. */
    public GraphStatController(GraphController gc) {
        gc.addViewGraphFollower(this);
        addPropertyChangeListener(gc);
        setBaseGraph(gc.getBaseGraph());
    }

    //
    // METRIC COMPUTATIONS
    //


    /** Used to perform calculations */
    private static ExecutorService exec = Executors.newSingleThreadExecutor();
    
    @Override
    protected void updateGraph() {
        if (metric != null && baseGraph != null) {
            exec.execute(new Runnable(){
                public void run() {
                    if (statsCache == null || statsCache.getGraph() != baseGraph)
                        statsCache = new GraphStats(baseGraph);
                    List values;
                    if (statsCache.containsNodeStats(metric)) {
                        values = statsCache.getNodeStats(metric).getValues();
                    } else {
                        setStatus("Computing metric " + metric + "...");
                        long t0 = System.currentTimeMillis();
                        values = statsCache.getNodeStats(metric).getValues();
                        SummaryStatistics stat = statsCache.getNodeStats(metric).getStatistics();
                        long t = System.currentTimeMillis() - t0;
                        String status = String.format("%s computed on %d nodes in %dms: min=%.2f, avg=%.2f, max=%.2f, dev=%.2f",
                                metric, stat.getN(), t, stat.getMin(), stat.getMean(), stat.getMax(), stat.getStandardDeviation());
                        setStatus(status);
                        setOutput(status);
                    }
                    pcs.firePropertyChange("metricValues", null, values);
                }
            });
        } else if (metric == null && baseGraph != null)
            pcs.firePropertyChange("metricValues", null, new ArrayList());
    }

    //
    // PROPERTY PATTERNS
    //

    /**
     * Return active metric.
     * @return active metric (maybe null) 
     */
    public synchronized NodeMetric getMetric() {
        return metric;
    }

    /**
     * Sets the currently active metric
     * @param metric the new metric
     */
    public synchronized void setMetric(NodeMetric metric) {
        if (this.metric != metric) {
            Object oldMetric = this.metric;
            this.metric = metric;
            updateGraph();
            pcs.firePropertyChange("metric", oldMetric, metric);
        }
    }

    /**
     * Return last computed metric values
     * @return active metric getValues (the last ones that have been computed) 
     */
    public List getMetricValues() {
        return statsCache != null && statsCache.containsNodeStats(metric) ? statsCache.getNodeStats(metric).getValues() : null;
    }
    
    /**
     * Returns statistics associated with computed metric values
     * @return stats
     */
    public SummaryStatistics getStats() {
        return statsCache != null && statsCache.containsNodeStats(metric) ? statsCache.getNodeStats(metric).getStatistics() : null;
    }
    
    
    //<editor-fold defaultstate="collapsed" desc="AVAILABLE METRICS">
    //
    // AVAILABLE METRICS
    //

    /** Encodes global metric options */
    public enum GlobalStatEnum {
        ORDER(GlobalMetrics.ORDER),
        EDGE_NUMBER(GlobalMetrics.EDGE_NUMBER),
        AVERAGE_DEGREE(GlobalMetrics.DEGREE_AVERAGE),
        DENSITY(GlobalMetrics.DENSITY),
        DIAMETER(GlobalMetrics.DIAMETER),
        RADIUS(GlobalMetrics.RADIUS),
        CLUSTERING(GlobalMetrics.CLUSTERING_A),
        CLUSTERING_B(GlobalMetrics.CLUSTERING_B);

        GlobalMetric metric;
        GlobalStatEnum(GlobalMetric metric) { this.metric = metric; }
        @Override public String toString() { return metric.getName(); }
        public GlobalMetric getMetric() { return metric; }
    }
    
    //</editor-fold>
    
    
    // TODO - move elsewhere!!
    /** Computes cooperation scores. */
    public void computeCooperationScores() {
        Graph active = getBaseGraph();
        if (active == null)
            return;
        CooperationPanel cp = new CooperationPanel();
        int result = JOptionPane.showConfirmDialog(null, cp, "Cooperation parameters", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            NodeMetric m = cp.getMetric();
            Collection<Integer> subset = cp.getSubset();
            CooperationSubsetMetric m1 = new CooperationSubsetMetric(new AdditiveSubsetMetric(m));
            CooperationSubsetMetric m2 = new CooperationSubsetMetric(new ContractiveSubsetMetric(m));
            double[] v1 = m1.getValue(active, subset);
            double[] v2 = m2.getValue(active, subset);
            setOutput("Computed additive metric with " + m + "   : selfish = " + v1[0] + ", altruistic = " + v1[1] + ", total = " + (v1[0]+v1[1]));
            setOutput("Computed contractive metric with " + m + ": selfish = " + v2[0] + ", altruistic = " + v2[1] + ", total = " + (v2[0]+v2[1]));
        }
    }    
    
    /** Comptues global statistics. */
    public void computeGlobalStatistics(String name) {
        if (getBaseGraph() != null) {
            Graph g = getBaseGraph();
            for (GlobalStatEnum en : GlobalStatEnum.values()) {
                Object value = en.metric.value(g);
                setOutput("Value of metric " + en.metric.getName() + " on graph " 
                        + name + ": " + value);
            }
        }
    }
    
}
