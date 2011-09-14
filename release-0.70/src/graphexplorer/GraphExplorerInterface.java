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

    /** 
     * Returns active graph component displayed (or null if none is active)
     * @return the active graph component
     */
    public GraphComponent graphPlot();
    
    /** 
     * Returns component-parent for dialogs
     * @return the component-parent for dialogs
     */
    public Component dialogComponent();
    
    /** 
     * Return the active controller
     * @return active controller 
     */
    public GraphController controller();
    
    /** 
     * Adds a message to the output window. 
     * @param output string to print to the output window
     */
    public void output(String output);

    /**
     * Return true if there is an active graph in the interface, otherwise false
     * @return active graph status
     */
    public boolean isGraphActive();
}
