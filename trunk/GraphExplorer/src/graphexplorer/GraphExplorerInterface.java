/*
 * GraphExplorerInterface.java
 * Created Jul 2010
 */

package graphexplorer;

import graphexplorer.controller.GraphController;
import java.awt.Component;
import org.bm.blaise.specto.plane.graph.GraphComponent;

/**
 * Provides methods that actions classes can use, regardless of the GUI.
 * @author Elisha Peterson
 */
public interface GraphExplorerInterface {

    /** Returns active graph component displayed (or null if none is active) */
    public GraphComponent graphPlot();
    /** Returns component-parent for dialogs */
    public Component dialogComponent();
    /** @return active controller */
    public GraphController controller();
    
    /** Adds a message to the output window. */
    public void output(String output);

}
