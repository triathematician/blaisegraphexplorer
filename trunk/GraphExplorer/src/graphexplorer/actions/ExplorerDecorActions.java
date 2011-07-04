/*
 * ExplorerStatActions.java
 * Created Jul 13, 2010
 */

package graphexplorer.actions;

import graphexplorer.GraphExplorerMain;
import graphexplorer.controller.GraphDecorController;
import graphexplorer.views.GraphListModel;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * Describes statistical & metric actions to perform in explorer app
 * @author Elisha Peterson
 */
public class ExplorerDecorActions {

    /** What this class works with */
    GraphDecorController controller;
    /** Construction requires a controller */
    public ExplorerDecorActions(GraphDecorController controller) {
        setController(controller);
    }

    public final void setController(GraphDecorController controller) {
        this.controller = controller;
        boolean nonNull = controller != null && controller.getBaseGraph() != null;
        HIGHLIGHT.setEnabled(nonNull);
    }

    public Action HIGHLIGHT = new AbstractAction("Highlight subset of nodes", GraphExplorerMain.loadIcon("highlight18")) {
        {
            putValue(SHORT_DESCRIPTION, "Select 1 or more nodes to display as highlighted.");
            setEnabled(true);
        }
        public void actionPerformed(ActionEvent e) {
            GraphListModel glm = new GraphListModel(controller);
            JList jl = new JList(glm);
            jl.setMaximumSize(new Dimension(800,600));
            jl.setLayoutOrientation(JList.VERTICAL_WRAP);
            int result = JOptionPane.showConfirmDialog(null, new JScrollPane(jl), "Select 1 or more nodes",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                int[] selected = jl.getSelectedIndices();
                TreeSet set = new TreeSet();
                for (int i : selected)
                    set.add(glm.getNodeAt(i));
                controller.setHighlightNodes(set);
            }
        }
    };

    public Action DISTINCT_COLOR = new AbstractAction("Give each node a distinct color") {
        {
            putValue(SHORT_DESCRIPTION, "Gives each node a unique color.");
            setEnabled(true);
        }
        public void actionPerformed(ActionEvent e) {
            if (controller != null)
                controller.distinctColors();
        }
    };

}
