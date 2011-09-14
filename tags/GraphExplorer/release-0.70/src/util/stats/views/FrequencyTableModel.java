/*
 * FrequencyTableModel.java
 * Created May 18, 2010
 */

package util.stats.views;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.table.AbstractTableModel;
import org.bm.blaise.scio.graph.metrics.GraphMetrics;

/**
 * <p>
 * Transforms a list of values into a frequency table.
 * </p>
 * 
 * @author elisha
 */
public class FrequencyTableModel extends AbstractTableModel {

    /** Stores the values in the table. */
    private Object[] values;
    /** Stores the number of each value in the table. */
    private int[] counts;

    boolean intBased = false;
    double min, max;

    /** Construct the distribution table given a list of values. */
    public FrequencyTableModel(List values) {
        setValues(values);
    }

    public final <N> void setValues(List<N> values) {
        Map<N, Integer> map = GraphMetrics.distribution(values);
        this.values = new Object[map.size()];
        counts = new int[map.size()];
        int i = 0;
        boolean allInt = true;
        double min = Double.MAX_VALUE, max = -Double.MIN_VALUE;
        for (Entry<N, Integer> en : map.entrySet()) {
            this.values[i] = en.getKey();
            counts[i] = en.getValue();
            i++;
            if (en.getKey() instanceof Number) {
                double v = ((Number)en.getKey()).doubleValue();
                min = Math.min(min, v);
                max = Math.max(max, v);
                if (Math.abs(v%1.0) > 1e-6)
                    allInt = false;
            }
        }
        intBased = allInt;
        this.min = min;
        this.max = max;
        fireTableDataChanged();
    }

    /** @return the i'th value */
    public Object getValue(int i) {
        return values[i];
    }

    /** @return the distribution of counts */
    public int[] getCounts() {
        return counts;
    }

    String[] LABELS = { "Value", "Count" };
    Class[] TYPES = { Object.class, Integer.class };

    public int getRowCount() { return values == null ? 0 : values.length; }
    public int getColumnCount() { return LABELS.length; }
    @Override public String getColumnName(int col) { return LABELS[col]; }
    @Override public Class<?> getColumnClass(int col) { return TYPES[col]; }

    public Object getValueAt(int row, int col) { return values == null ? null : col == 0 ? values[row] : counts[row]; }

    public boolean intBased() { return intBased; }
    public double getMin() { return min; }
    public double getMax() { return max; }

}
