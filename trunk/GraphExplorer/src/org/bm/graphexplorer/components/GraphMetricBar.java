/*
 * GraphMetricBar.java
 * Created Dec 20, 2010
 */

package org.bm.graphexplorer.components;

import org.bm.graphexplorer.controllers.GraphController;
import org.bm.graphexplorer.controllers.GraphStatController;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.bm.blaise.scio.graph.GraphServices;
import org.bm.blaise.scio.graph.metrics.NodeMetric;

/**
 * Provides access to node metric computation algorithms.
 * 
 * @author elisha
 */
public class GraphMetricBar extends JPanel
        implements PropertyChangeListener, ActionListener {
    
    /** The stat controller */
    GraphStatController gsc = null;

    /** The label */
    JLabel label;
    /** The threshold */
    JComboBox metricCB;

    /** Sets up the panel without a controller */
    public GraphMetricBar() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(label = new JLabel("Node metric: "));
        add(metricCB = new JComboBox(new DefaultComboBoxModel(GraphServices.getAvailableNodeMetrics().toArray())));
        setPreferredSize(new Dimension(250, getPreferredSize().height));
        setMaximumSize(new Dimension(330, 30));
        metricCB.addActionListener(this);
        setToolTipText("Compute and display the specified metric.");
        updateValues();
    }
    
    /** Sets up the panel with a controller */
    public GraphMetricBar(GraphController gc) { 
        this(); 
        setController(gc);
    }

    public void setController(GraphController gc) {
        if (gc == null || gsc != gc.getStatController()) {
            if (gsc != null)
                gsc.removePropertyChangeListener(this);
            gsc = gc == null ? null : gc.getStatController();
            if (gsc != null)
                gsc.addPropertyChangeListener(this);
            updateValues();
        }
    }

    /** Called to update components with values from the controller */
    void updateValues() {
        if (gsc == null) {
            metricCB.setEnabled(false);
        } else {
            metricCB.setEnabled(true);
            metricCB.setSelectedItem(gsc.getMetric());
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("metric"))
            updateValues();
        else if (evt.getPropertyName().equals("activeController"))
            setController((GraphController)evt.getNewValue());
    }

    public void actionPerformed(ActionEvent e) {
        gsc.setMetric((NodeMetric) metricCB.getSelectedItem());
    }

}
