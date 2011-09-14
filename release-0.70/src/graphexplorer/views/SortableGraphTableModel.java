/*
 * SortableGraphTableModel.java
 * Created Aug 11, 2010
 */

package graphexplorer.views;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import org.jdesktop.application.Task;

/**
 * Provides sortable version of the graph table.
 * @author Elisha Peterson
 */
public class SortableGraphTableModel extends AbstractTableModel 
        implements TableModelListener {
    
    /** Base class */
    GraphTableModel gtm;
    /** This does the sorting */
    private RowComparator comparator;
    /** The sort column */
    private int sortColumn;
    /** Whether to sort ascending */
    private boolean sortAsc = true;
    /** Stores the sorted indices: values are rows in original table, indices are rows in sorted version */
    private final List<Integer> sortRows = Collections.synchronizedList(new ArrayList<Integer>());

    public SortableGraphTableModel() {
        this(new GraphTableModel(), GraphTableModel.COL_DEGREE);
    }

    public SortableGraphTableModel(GraphTableModel gtm, int col) {
        setBaseModel(gtm);
        sortColumn = col;
        comparator = new RowComparator();
    }

    /** @return list of actions which can be used to sort the table */
    public Action[] getSortActions() {
        return new Action[] {
            new SortAction(-1, true),
            new SortAction(GraphTableModel.COL_NODE, true),
            new SortAction(GraphTableModel.COL_LABEL, true),
            new SortAction(GraphTableModel.COL_DEGREE, false),
            new SortAction(GraphTableModel.COL_METRIC, false)
        };
    }

    public GraphTableModel getBaseModel() {
        return gtm;
    }
    
    public void setBaseModel(GraphTableModel gtm) {
        if (this.gtm != gtm) {
            if (this.gtm != null)
                this.gtm.removeTableModelListener(this);
            this.gtm = gtm;
            sortRows.clear();
            gtm.addTableModelListener(this);
        }
    }

    /** @return current sort column */
    public int getSortColumn() {
        return sortColumn;
    }

    /** Sets current sort column */
    public void setSortColumn(int col) {
        sortColumn = col;
        resort();
    }

    public void tableChanged(TableModelEvent e) {
        if (e.getSource() == gtm) {
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW)
                fireTableStructureChanged();
            else
                resort();
        }
    }

    /** Used to perform calculations */
    private static ExecutorService exec = Executors.newSingleThreadExecutor();
    /** Flag to keep from excessive sorting */
    private boolean sorting = false;
    
    /** Sorts the table and fires a change event */
    private void resort() {
        if (sorting)
            return;
        exec.execute(new Runnable(){
            public void run() {
                sorting = true;
                comparator.sortColumn = sortColumn;
                long t0 = System.currentTimeMillis();
                ArrayList<Integer> result = new ArrayList<Integer>();
                for (int i = 0; i < getRowCount(); i++)
                    result.add(i);
                if (sortAsc)
                    Collections.sort(result, comparator);
                else
                    Collections.sort(result, Collections.reverseOrder(comparator));
                synchronized (sortRows) {
                    sortRows.clear();
                    sortRows.addAll(result);
                }
                long t1 = System.currentTimeMillis();
                if ((t1-t0) > 200)
                    System.err.println("Long sort time: " + (t1-t0));
                fireTableDataChanged();
                sorting = false;
            }
        });
    }


    // <editor-fold defaultstate="collapsed" desc="TableModel methods">
    
    @Override public String getColumnName(int col) { return gtm.getColumnName(col); }
    @Override public Class<?> getColumnClass(int col) { return gtm.getColumnClass(col); }
    public int getRowCount() { return gtm.getRowCount(); }
    public int getColumnCount() { return gtm.getColumnCount(); }

    public Object getValueAt(int row, int col) {
        if (row >= sortRows.size())
            resort();
        return gtm.getValueAt(sortRows.get(row), col);
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        if (row >= sortRows.size())
            resort();
        gtm.setValueAt(aValue, sortRows.get(row), col);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= sortRows.size())
            resort();
        return gtm.isCellEditable(sortRows.get(row), col);
    }

    // </editor-fold>
    

    // <editor-fold defaultstate="collapsed" desc="Inner Classes (for sorting)">

    /** A comparator for rows of the table */
    private class RowComparator implements Comparator<Integer> {
        int sortColumn = -1;
        public int compare(Integer row1, Integer row2) {
            if (sortColumn == -1)
                return row1-row2;
            else if (row1 == row2)
                return 0;
            Object val1 = gtm.getValueAt(row1, sortColumn);
            Object val2 = gtm.getValueAt(row2, sortColumn);
            if (val1 == null || val2 == null)
                return row1-row2;
            else if (!(val1 instanceof Comparable)) {
                val1 = val1.toString();
                val2 = val2.toString();
            }
            int result = ((Comparable)val1).compareTo(val2);
            if (result == 0)
                result = row1 - row2;
            return result;
        }
    }

    /** Sort actions */
    private class SortAction extends AbstractAction {
        int col;
        boolean asc;
        SortAction(int col, boolean asc) {
            super((col == -1 ? "No sorting" : ("Sort by " + gtm.colNames[col]))
                    + (asc ? "" : " (descending)"));
            this.col = col;
            this.asc = asc;
        }
        public void actionPerformed(ActionEvent e) {
            sortAsc = asc;
            setSortColumn(col);
        }
    }
    // </editor-fold>
}
