/*
 * GraphStyleRollupPanel.java
 * Created on Sep 18, 2011, 6:37:34 AM
 */
package org.bm.graphexplorer.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import org.bm.blaise.style.Anchor;
import org.bm.blaise.style.BasicStringStyle;
import org.bm.blaise.scio.graph.layout.IterativeGraphLayout;
import org.bm.blaise.specto.plane.graph.GraphComponent;
import org.bm.blaise.specto.plane.graph.GraphManager;
import org.bm.firestarter.propertysheet.BeanEditorSupport;
import org.bm.firestarter.propertysheet.PropertySheet;
import org.bm.graphexplorer.controllers.GraphController;
import org.bm.graphexplorer.controllers.GraphDecorController;

/**
 *
 * @author elisha
 */
public class GraphStyleRollupPanel extends javax.swing.JPanel implements PropertyChangeListener {

    /** Current decor controller */
    private GraphDecorController gdc;
    /** Current controller */
    private GraphController gc;
    /** Current graph manager */
    private GraphManager gm;
    
    /** Creates new form GraphStyleRollupPanel */
    public GraphStyleRollupPanel() {
        initComponents();
        
        backgroundPS = nodePS = labelPS = edgePS = layoutPS = null;
    }

    public void setController(GraphController gc) {
        if (this.gc != gc) {
            if (this.gc != null) {
                this.gc.removePropertyChangeListener(this);
                this.gdc.removePropertyChangeListener(this);
                this.gm.removePropertyChangeListener(this);
            }
            this.gc = gc;
            if (gc != null) {
                this.gdc = gc.getDecorController();
                this.gm = gc.getManager();
                gc.addPropertyChangeListener(this);
                gdc.addPropertyChangeListener(this);
                gm.addPropertyChangeListener(this);
            } else {
                this.gdc = null;
                this.gm = null;
            }
            update();
        }
    }
    
    /** Call to update everything in the property sheet. */
    void update() {
        GraphManager active = gc == null ? null : gc.getManager();

        if (nodePS != null)
            nodePS.removeBeanChangeListener(this);
        if (edgePS != null)
            edgePS.removeBeanChangeListener(this);
        if (labelPS != null)
            labelPS.removeBeanChangeListener(this);
        if (layoutPS != null)
            layoutPS.removeBeanChangeListener(this);
        
        propertyRP.removeAll();
        
        if (active == null) {
            nodePS = null;
            edgePS = null;
            labelPS = null;
            layoutPS = null;
            backgroundPS = null;
        } else {
            propertyRP.add(backgroundPS = new PropertySheet(new BackgroundBean()), "Background");
            propertyRP.add(nodePS = new PropertySheet(gdc.getNodeStyler().getBaseStyle()), "Nodes");
            propertyRP.add(edgePS = new PropertySheet(gdc.getEdgeStyler().getBaseStyle()), "Edges");
            propertyRP.add(labelPS = new PropertySheet(gdc.getNodeLabelStyler()), "Labels");
            IterativeGraphLayout layout = active.getLayoutAlgorithm();
            if (layout != null)
                propertyRP.add(layoutPS = new PropertySheet(layout), "Iterative Layout Settings");

            nodePS.addBeanChangeListener(this);
            edgePS.addBeanChangeListener(this);
            labelPS.addBeanChangeListener(this);
        }
        repaint();
    }

    
    public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (evt.getSource() instanceof BeanEditorSupport) {
            if (graphPlot() != null)
                graphPlot().repaint();
            repaint();
        } else if (prop.equals("activeController"))
            setController((GraphController)evt.getNewValue());
        else if (prop.equals("nodeStyler") || prop.equals("layoutAlgorithm"))
            update();
    }
    
    
    // <editor-fold defaultstate="collapsed" desc="INNER CLASSES - BEANS">
    //
    // INNER CLASSES - BEANS
    //

    private GraphComponent graphPlot() { return gc == null ? null : gc.getComponent(); }
    
    /** Provides a bean to access the background color of the active plot component */
    public class BackgroundBean {
        public Color getColor() { return graphPlot() == null ? Color.WHITE : graphPlot().getBackground(); }
        public void setColor(Color col) { if (graphPlot() != null) graphPlot().setBackground(col); }
    }
    
    /** Wraps label bean to allow visibility checks */
    public class LabelBean {
        private int alpha;
        private final BasicStringStyle rend;
        public LabelBean(BasicStringStyle rend) { 
            assert rend != null;
            this.rend = rend; 
            alpha = rend.getColor().getAlpha(); 
        }
        
        public boolean isVisible() { 
            return rend.getColor().getAlpha() > 0; 
        }
        public void setVisible(boolean val) {
            Color c = rend.getColor();
            if (val && c.getAlpha() == 0) {
                if (alpha == 0)
                    alpha = 255;
                setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
            } else if (!val) {
                if (c.getAlpha() > 0)
                    this.alpha = c.getAlpha();
                setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
            }
        }

        public Point2D getOffset() { return rend.getOffset(); }
        public void setOffset(Point2D off) { rend.setOffset(off); }
        
        public Font getFont() { return rend.getFont(); }
        public void setFont(Font font) { rend.setFont(font); }
        
        public Color getColor() { return rend.getColor(); }
        public void setColor(Color color) { rend.setColor(color); }
        
        public Anchor getAnchor() { return rend.getAnchor(); }
        public void setAnchor(Anchor newValue) { rend.setAnchor(newValue); }
    }
    
    // </editor-fold>
    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        nodePS = new org.bm.firestarter.propertysheet.PropertySheet();
        labelPS = new org.bm.firestarter.propertysheet.PropertySheet();
        backgroundPS = new org.bm.firestarter.propertysheet.PropertySheet();
        edgePS = new org.bm.firestarter.propertysheet.PropertySheet();
        layoutPS = new org.bm.firestarter.propertysheet.PropertySheet();
        propertySP = new javax.swing.JScrollPane();
        propertyRP = new org.bm.util.gui.RollupPanel();

        nodePS.setName("nodePS"); // NOI18N

        labelPS.setName("labelPS"); // NOI18N

        backgroundPS.setName("backgroundPS"); // NOI18N

        edgePS.setName("edgePS"); // NOI18N

        layoutPS.setName("layoutPS"); // NOI18N

        setName("Form"); // NOI18N
        setLayout(new java.awt.BorderLayout());

        propertySP.setName("propertySP"); // NOI18N

        propertyRP.setName("propertyRP"); // NOI18N
        propertySP.setViewportView(propertyRP);

        add(propertySP, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.bm.firestarter.propertysheet.PropertySheet backgroundPS;
    private org.bm.firestarter.propertysheet.PropertySheet edgePS;
    private org.bm.firestarter.propertysheet.PropertySheet labelPS;
    private org.bm.firestarter.propertysheet.PropertySheet layoutPS;
    private org.bm.firestarter.propertysheet.PropertySheet nodePS;
    private org.bm.util.gui.RollupPanel propertyRP;
    private javax.swing.JScrollPane propertySP;
    // End of variables declaration//GEN-END:variables
}
