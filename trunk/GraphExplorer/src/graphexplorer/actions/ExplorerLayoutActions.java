/*
 * GraphLayout.java
 * Created Jul 14, 2010
 */

package graphexplorer.actions;

import graphexplorer.GraphExplorerView;
import graphexplorer.controller.GraphController;
import graphexplorer.controller.TimeGraphController;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.bm.blaise.scio.graph.layout.SpringLayout;

/**
 * Describes layout actions supporting the graph explorer app.
 * @author Elisha Peterson
 */
public class ExplorerLayoutActions {

    /** What this class works with */
    private GraphController controller;
    /** Construction requires a controller */
    public ExplorerLayoutActions(GraphController controller) {
        setController(controller);
    }
    
    public void setController(GraphController controller) {
        this.controller = controller;
        boolean nonNull = controller != null;
//        LAYOUT_CIRCULAR.setEnabled(nonNull);
//        LAYOUT_RANDOM.setEnabled(nonNull);
//        LAYOUT_SPRING_STATIC.setEnabled(nonNull);
        LAYOUT_ENERGY_START.setEnabled(nonNull);
        LAYOUT_STOP.setEnabled(nonNull);
        LAYOUT_ITERATE.setEnabled(nonNull);
        LAYOUT_TIME_START.setEnabled(controller instanceof TimeGraphController);
    }

    public Action LAYOUT_ENERGY_START = new AbstractAction("Spring layout - start", GraphExplorerView.loadIcon("play18")) {
        {
            putValue(SHORT_DESCRIPTION, "Set the active layout algorithm to a spring-based layout algorithm," +
                    " and begin animation.");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.ALT_DOWN_MASK));
            putValue(MNEMONIC_KEY, KeyEvent.VK_E);
            setEnabled(true);
        }
        public void actionPerformed(ActionEvent e) {
            if (controller != null) {
                if (controller.getLayoutAlgorithm() == null)
                    controller.setLayoutAlgorithm(new SpringLayout(controller.getNodePositions()));
                controller.setLayoutAnimating(true);
            }
        }
    };

    public Action LAYOUT_TIME_START = new AbstractAction("Time Spring layout - start", GraphExplorerView.loadIcon("play18")) {
        {
            putValue(SHORT_DESCRIPTION, "Set the active layout algorithm to a time/spring-based layout algorithm," +
                    " and begin animation.");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK));
            putValue(MNEMONIC_KEY, KeyEvent.VK_F);
            setEnabled(false);
        }
        public void actionPerformed(ActionEvent e) {
            if (controller instanceof TimeGraphController) {
                TimeGraphController tgc = (TimeGraphController) controller;
                tgc.getTimeGraphManager().initLayoutAlgorithm();
                tgc.getTimeGraphManager().setLayoutAnimating(true);
            }
        }
    };

    public Action LAYOUT_ITERATE = new AbstractAction("Iterate layout", GraphExplorerView.loadIcon("step18")) {
        {
            putValue(SHORT_DESCRIPTION, "Runs a single iteration of the currently active iterative layout algorithm.");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_DOWN_MASK));
            putValue(MNEMONIC_KEY, KeyEvent.VK_I);
            setEnabled(true);
        }
        public void actionPerformed(ActionEvent e) {
            if (controller != null)
                controller.iterateLayout();
        }
    };

    public Action LAYOUT_STOP = new AbstractAction("Stop layout animation", GraphExplorerView.loadIcon("stop18")) {
        {
            putValue(SHORT_DESCRIPTION, "Stop animation of the currently active iterative layout algorithm.");
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK));
            putValue(MNEMONIC_KEY, KeyEvent.VK_S);
            setEnabled(true);
        }
        public void actionPerformed(ActionEvent e) {
            if (controller != null)
                controller.setLayoutAnimating(false);
        }
    };

}
