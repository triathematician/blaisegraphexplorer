/**
 * GraphFileChooser.java
 * Created Jul 12, 2010 (or earlier)
 */
package org.bm.graphexplorer.components;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileView;
import org.bm.blaise.scio.graph.io.AbstractGraphIO;
import org.bm.blaise.scio.graph.io.DynetMLGraphIO;
import org.bm.blaise.scio.graph.io.EdgeListGraphIO;
import org.bm.blaise.scio.graph.io.GraphMLIO;
import org.bm.blaise.scio.graph.io.PajekGraphIO;
import org.bm.blaise.scio.graph.io.PajekTimeGraphIO;
import org.bm.blaise.scio.graph.io.UCINetGraphIO;

/**
 * File chooser that is intended for use with graphs
 * @author elisha
 */
public final class GraphFileChooser extends javax.swing.JFileChooser {

    //
    // SINGLETON INSTANCE
    //

    /** The file chooser box */
    public static GraphFileChooser fc;
    
    /** 
     * Lazy initialization will cause a bit of a delay when first loaded 
     * @param filter group of filters to use
     * @param dialogTitle title for dialog
     * @param approve string for approval button
     * @return chooser
     */
    public static GraphFileChooser initFileChooser(ChooserMode filter, String dialogTitle, String approve) {
        if (fc == null)
            fc = new GraphFileChooser();
        fc.setMode(filter);
        fc.setDialogTitle(dialogTitle);
        fc.setApproveButtonText(approve);
        return fc;
    }

    /**
     * Returns static instance of the file chooser
     * @return chooser
     */
    public static GraphFileChooser getInstance() {
        if (fc == null)
            fc = new GraphFileChooser();
        return fc;
    }
    
    //
    // CHOOSER DEFINITIONS
    //    
    
    /** Provides preview of graph files. */
    GraphFileAccessory gfa;
    /** Type of file being saved/exported/etc. */
    ChooserMode filterGroup = ChooserMode.GRAPH;
    /** Input/output algorithms */
    final static List<AbstractGraphIO> ios = Arrays.asList(
            EdgeListGraphIO.getInstance(),
            PajekGraphIO.getInstance(),
            PajekGraphIO.getExtendedInstance(),
            PajekTimeGraphIO.getInstance(),
            UCINetGraphIO.getInstance(),
            GraphMLIO.getInstance(),
            DynetMLGraphIO.getInstance()
            );

    public GraphFileChooser() {
        setFileView(new GraphFileView());
    }

    /**
     * Sets the filter used in the chooser
     * @param group the type of filter being used
     * @param filters zero or more filters, the first of which will be
     *   used for the active selected filter
     */
    public void setMode(ChooserMode group, FileFilter... filters) {
        filterGroup = group;
        resetChoosableFileFilters();
        for (FileFilter ff : filters)
            addChoosableFileFilter(ff);
        switch (group) {
            case GRAPH:
                for (AbstractGraphIO agio : ios)
                    addChoosableFileFilter(agio.getFileFilter());
                setFileFilter(ios.get(1).getFileFilter()); // default to pajek
                GraphFileAccessory gfa = new GraphFileAccessory(this);
        //        gfa.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
                gfa.setBorder(BorderFactory.createTitledBorder("Graph Preview"));
                setAccessory(gfa);
                break;
            case IMAGE:
                setDialogTitle("Export image of current graph");
                setApproveButtonText("Export");
                setAccessory(null);
                break;
            case MOVIE:
                setDialogTitle("Export movie of current graph");
                setApproveButtonText("Export");
                setAccessory(null);
                break;
        }
        if (filters.length > 0) {
            setFileFilter(filters[0]);
        }
    }

    /** @return the io method corresponding to the selected filter, or null if the "allow all" option is selected */
    public AbstractGraphIO activeIO() {
        // use IO from currently selected filter
        javax.swing.filechooser.FileFilter activeFilter = getFileFilter();
        for (AbstractGraphIO agio : ios)
            if (activeFilter == agio.getFileFilter())
                return agio;
        
        // if none active, attempt to automatically select filter
        File sel = getSelectedFile();
        for (AbstractGraphIO agio : ios)
            if (agio.getFileFilter().accept(sel))
                return agio;
        
        return null;
    }
    
    /**
     * Look up IO for specified file.
     * @return io most appropriate for specified file, or null if none can be found.
     */
    public static AbstractGraphIO getIO(File f) {
        for (AbstractGraphIO io : ios)
            if (io.getFileFilter().accept(f))
                return io;
        return null;
    }
    
    //
    // INNER CLASSES
    //
    
    /** File chooser state */
    public static enum ChooserMode { GRAPH, IMAGE, MOVIE; }

    /** File view, customizes appearance of files in file dialog */
    static class GraphFileView extends FileView {
        @Override
        public Icon getIcon(File f) { return null; }
    }
}
