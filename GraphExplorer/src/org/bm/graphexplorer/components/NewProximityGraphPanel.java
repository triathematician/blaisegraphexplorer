/*
 * NewWSGraphPanel.java
 * Created on Aug 6, 2010
 */

package org.bm.graphexplorer.components;

import java.awt.geom.Point2D;
import org.bm.blaise.scio.graph.GraphFactory;
import org.bm.blaise.scio.graph.ValuedGraph;

/**
 *
 * @author elisha
 */
public class NewProximityGraphPanel extends javax.swing.JPanel {


    /** Creates new form NewRandomGraphPanel */
    public NewProximityGraphPanel() {
        initComponents();
    }

    /** @return graph corresponding to the current graph settings */
    public ValuedGraph<Integer,Point2D.Double> getInstance() {
        int order = basicP.getOrder();
        float dist = (Float) distSp.getValue();
        float minX = (Float) minXSp.getValue();
        float maxX = (Float) maxXSp.getValue();
        float minY = (Float) minYSp.getValue();
        float maxY = (Float) maxYSp.getValue();
        return GraphFactory.getRandomProximityGraph(order, minX, maxX, minY, maxY, dist);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        basicP = new NewSimpleGraphPanel(50,100000,true);
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        distSp = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        minXSp = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        maxXSp = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        minYSp = new javax.swing.JSpinner();
        jLabel5 = new javax.swing.JLabel();
        maxYSp = new javax.swing.JSpinner();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.PAGE_AXIS));
        add(basicP);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Links"));
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("Link nodes within distance:");
        jPanel1.add(jLabel1);

        distSp.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(1.0f), Float.valueOf(-100.0f), Float.valueOf(100.0f), Float.valueOf(0.05f)));
        distSp.setToolTipText("Enter the probability of occurence for each edge in the resulting graph (0.00-1.00)");
        distSp.setPreferredSize(new java.awt.Dimension(70, 22));
        jPanel1.add(distSp);

        add(jPanel1);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Node Location Boundaries"));
        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        minXSp.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(-5.0f), Float.valueOf(-100.0f), Float.valueOf(100.0f), Float.valueOf(0.05f)));
        minXSp.setToolTipText("Enter the probability of occurence for each edge in the resulting graph (0.00-1.00)");
        minXSp.setPreferredSize(new java.awt.Dimension(70, 22));
        jPanel2.add(minXSp);

        jLabel3.setText(" ≤ x ≤ ");
        jPanel2.add(jLabel3);

        maxXSp.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(5.0f), Float.valueOf(-100.0f), Float.valueOf(100.0f), Float.valueOf(0.05f)));
        maxXSp.setToolTipText("Enter the probability of occurence for each edge in the resulting graph (0.00-1.00)");
        maxXSp.setPreferredSize(new java.awt.Dimension(70, 22));
        jPanel2.add(maxXSp);

        jLabel4.setText("      ");
        jPanel2.add(jLabel4);

        minYSp.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(-5.0f), Float.valueOf(-100.0f), Float.valueOf(100.0f), Float.valueOf(0.05f)));
        minYSp.setToolTipText("Enter the probability of occurence for each edge in the resulting graph (0.00-1.00)");
        minYSp.setPreferredSize(new java.awt.Dimension(70, 22));
        jPanel2.add(minYSp);

        jLabel5.setText(" ≤ y ≤ ");
        jPanel2.add(jLabel5);

        maxYSp.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(5.0f), Float.valueOf(-100.0f), Float.valueOf(100.0f), Float.valueOf(0.05f)));
        maxYSp.setToolTipText("Enter the probability of occurence for each edge in the resulting graph (0.00-1.00)");
        maxYSp.setPreferredSize(new java.awt.Dimension(70, 22));
        jPanel2.add(maxYSp);

        add(jPanel2);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.bm.graphexplorer.components.NewSimpleGraphPanel basicP;
    private javax.swing.JSpinner distSp;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSpinner maxXSp;
    private javax.swing.JSpinner maxYSp;
    private javax.swing.JSpinner minXSp;
    private javax.swing.JSpinner minYSp;
    // End of variables declaration//GEN-END:variables

}
