/*
 * GraphTable.java
 * Created Aug 12, 2010
 */

package org.bm.graphexplorer.components;

import org.bm.graphexplorer.controllers.GraphController;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * <p>
 *   Tabular view of a graph with basic information for the nodes.
 * </p>
 * @author Elisha Peterson
 */
public class GraphTable extends javax.swing.JTable
        implements PropertyChangeListener {

    /** Base model containing the data */
    private final SortableGraphTableModel model = new SortableGraphTableModel();
    /** Custom renderer */
    private final TableCellRenderer rend = new GraphTableCellRenderer();
    /** The metric column (stored so can change its name) */
    TableColumn metricColumn;

    public GraphTable() {
        setModel(model);

        javax.swing.table.TableColumnModel tcm = getColumnModel();
        for (int i = 0; i < getColumnCount(); i++)
            tcm.getColumn(i).setPreferredWidth(GraphTableModel.PREFERRED_WIDTH[i]);
        metricColumn = tcm.getColumn(GraphTableModel.COL_METRIC);

        initMenu();
    }

    private void initMenu() {
        ButtonGroup bg = new ButtonGroup();
        JPopupMenu sortMenu = new JPopupMenu();
        for (javax.swing.Action a : model.getSortActions()) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(a);
            sortMenu.add(mi);
            bg.add(mi);
            if (bg.getSelection() == null)
                bg.setSelected(mi.getModel(), true);
        }
        setComponentPopupMenu(sortMenu);
        getTableHeader().setComponentPopupMenu(sortMenu);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("activeController")) {
            GraphController gc = (GraphController) evt.getNewValue();
            model.gtm.setController(gc);
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (getColumnModel().getColumnCount() == 5) {
            String curName = (String) metricColumn.getHeaderValue();
            String newName = model.gtm.getColumnName(GraphTableModel.COL_METRIC);
            if (!curName.equals(newName))
                metricColumn.setHeaderValue(newName);
            getTableHeader().repaint();
        }
        super.tableChanged(e);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return rend;
    }

    /** A renderer that highlights selected nodes based on an underlying controller. */
    public class GraphTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Object node = model.getValueAt(row, GraphTableModel.COL_NODE);
            if (model.gtm.gc.getHighlightNodes().contains(node))
                c.setForeground(Color.RED);
            else
                c.setForeground(Color.BLACK);
            return c;
        }
    }
}
