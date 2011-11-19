/*
 * GenerateGraphTasks.java
 * Created May 14, 2010
 */

package org.bm.graphexplorer;

import org.bm.graphexplorer.controllers.TimeGraphController;
import org.bm.graphexplorer.controllers.GraphController;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import org.bm.blaise.scio.graph.Graph;
import org.bm.blaise.scio.graph.GraphFactory;
import org.bm.blaise.scio.graph.ValuedGraph;
import org.bm.blaise.scio.graph.time.TimeGraph;
import org.bm.graphexplorer.components.NewDegreeSequenceGraphPanel;
import org.bm.graphexplorer.components.NewPreferentialGraphPanel;
import org.bm.graphexplorer.components.NewProximityGraphPanel;
import org.bm.graphexplorer.components.NewRandomGraphPanel;
import org.bm.graphexplorer.components.NewSimpleGraphPanel;
import org.bm.graphexplorer.components.NewWSGraphPanel;
import org.jdesktop.application.Task;

/**
 * Displays dialogs and returns graphs generated using the dialogs.
 * 
 * @author Elisha Peterson
 */
public class GenerateGraphTasks {
    
    //<editor-fold defaultstate="collapsed" desc="ABSTRACT GenerateGraphTask">
    //
    // ABSTRACT GenerateGraphTask
    //
    
    /** Task to generate a graph. */
    public abstract static class GenerateGraphTask<OptionsType> extends Task<Graph, Void> {
        /** Object displayed in dialog */
        protected final OptionsType options;
        /** Tracks the result of the dialog box (i.e. OK/Cancel) */
        private final int dialogResult;
        /** The name of the resulting graph */
        protected final String name;
        
        /** 
         * Construct for specified app. 
         * @param app the base application
         * @param m the message displayed in the dialog
         * @param name the name of the graph generated (e.g. "empty graph")
         */
        public GenerateGraphTask(GraphExplorerApp app, OptionsType m, String name) { 
            super(app); 
            this.options = m;
            this.name = name;
            dialogResult = showDialog(app.getMainFrame());
        }
        
        /** 
         * Display dialog with parameters for the task. This will be called when the
         * task is first constructed.
         * 
         * @param frame parent frame for the dialog
         * @return dialog result; should return {@link JOptionPane#OK_OPTION} to continue generating the graph
         */
        public int showDialog(javax.swing.JFrame frame) {
            return JOptionPane.showOptionDialog(frame, options, "New " + name,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, JOptionPane.OK_OPTION);
        }

        /**
         * Generates the graph using the dialog settings.
         * @return newly generated graph (may take some time)
         */
        public abstract Graph generateGraph();

        @Override
        protected Graph doInBackground() throws Exception {
            return dialogResult == JOptionPane.OK_OPTION ? generateGraph() : null;
        }

        @Override
        protected void succeeded(Graph result) {
            GraphExplorerApp app = (GraphExplorerApp) getApplication();
            if (result != null) {
                GraphController newC = new GraphController(result, name + " (" + result.order() + " vertices)");
                app.getMasterController().setActiveController(newC);
            }
        }
    }

    //</editor-fold>
    
    
    /** Task for an empty graph */
    public static GenerateGraphTask emptyGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(), "empty graph") {
            @Override public Graph generateGraph() {
                return GraphFactory.getEmptyGraph(options.getOrder(), options.getDirected());
            }            
        };
    }
    
    /** Task for a complete graph */
    public static GenerateGraphTask completeGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(), "complete graph") {
            @Override public Graph generateGraph() {
                return GraphFactory.getCompleteGraph(options.getOrder(), options.getDirected());
            }            
        };
    }
    
    /** Task for a circle graph */
    public static GenerateGraphTask circleGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(), "circle graph") {
            @Override public Graph generateGraph() {
                return GraphFactory.getCycleGraph(options.getOrder(), options.getDirected());
            }            
        };
    }
    
    /** Task for a star graph */
    public static GenerateGraphTask starGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(), "star graph") {
            @Override public Graph generateGraph() {
                return GraphFactory.getStarGraph(options.getOrder(), options.getDirected());
            }            
        };
    }
    
    /** Task for a wheel graph */
    public static GenerateGraphTask wheelGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(), "wheel graph") {
            @Override public Graph generateGraph() {
                return GraphFactory.getWheelGraph(options.getOrder(), options.getDirected());
            }            
        };
    }
    
    /** Task for a uniform random graph */
    public static GenerateGraphTask randomGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewRandomGraphPanel>(app, new NewRandomGraphPanel(), "random graph") {
            @Override public Graph generateGraph() {
                return options.getInstance();
            }            
        };
    }
    
    /** Task for a sequence random graph */
    public static GenerateGraphTask sequenceRandomGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewDegreeSequenceGraphPanel>(app, new NewDegreeSequenceGraphPanel(), "degree-sequence random graph") {
            @Override public Graph generateGraph() {
                return options.getInstance();
            }            
        };
    }
    
    /** Task for a Watts-Strogatz random graph */
    public static GenerateGraphTask wsRandomGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewWSGraphPanel>(app, new NewWSGraphPanel(), "Watts-Strogatz random graph") {
            @Override public Graph generateGraph() {
                return options.getInstance();
            }            
        };
    }
    
    /** Task for a proximity random graph */
    public static GenerateGraphTask proximityRandomGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewProximityGraphPanel>(app, new NewProximityGraphPanel(), "proximity random graph") {
            @Override public Graph generateGraph() {
                return options.getInstance();
            }
            @Override
            protected void succeeded(Graph result) {
                super.succeeded(result);
                
                GraphExplorerApp app = (GraphExplorerApp) getApplication();
                ValuedGraph<Integer,Point2D.Double> vg = (ValuedGraph<Integer,Point2D.Double>) result;
                Map<Object,Point2D.Double> pos = new HashMap<Object,Point2D.Double>();
                
                for (Integer i : vg.nodes()) {
                    Point2D.Double pt = vg.getValue(i);
                    pos.put(i, pt);
                    app.getActiveController().setNodeLabel(i, String.format("(%.3f,%.3f)", pt.x, pt.y));
                }
                app.getActiveController().setNodePositions(pos);
            }
        };
    }

    /** Task for a prime number graph */
    public static GenerateGraphTask primeGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewSimpleGraphPanel>(app, new NewSimpleGraphPanel(200, 50000, true), "Watts-Strogatz random graph") {
            @Override public Graph generateGraph() {
                int order = options.getOrder();
                int min = (int) Math.log10(2*order);
                return GraphFactory.getPrimeNumberGraph(order, min);
            }            
        };
    }
    
    /** Task for a preferential random graph */
    public static GenerateGraphTask prefRandomGraphTask(GraphExplorerApp app) {
        return new GenerateGraphTask<NewPreferentialGraphPanel>(app, new NewPreferentialGraphPanel(), "preferential-attachment graph") {
            TimeGraph tgr;
            @Override public Graph generateGraph() {
                Object result = options.getInstance();
                if (result instanceof Graph)
                    return (Graph) result;
                else {
                    tgr = (TimeGraph) result;
                    return null;
                }
            }
            @Override
            protected void succeeded(Graph result) {
                GraphExplorerApp app = (GraphExplorerApp) getApplication();
                GraphController newC = 
                        result == null ? new TimeGraphController(tgr, 
                                name + " (" + tgr.getAllNodes().size() + " vertices, " + tgr.getTimes().size() + " time steps)")
                        : new GraphController(result, name + " (" + result.order() + " vertices)");
                app.getMasterController().setActiveController(newC);
            }
        };
    }

}
