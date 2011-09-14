package graphexplorer.io;



import graphexplorer.controller.TimeGraphController;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Map;
import org.bm.blaise.specto.plane.graph.time.TimeGraphComponent;
import javax.swing.ProgressMonitor;
import org.bm.blaise.scio.graph.layout.SpringLayout;
import org.bm.blaise.specto.plane.graph.GraphManager;

/**
 * task for managing the exporting of the movie
 * @author skyebend
 *
 */
public class ExportMovieTask implements Runnable {

    // initial number of layout frames
    private static final int _INITIAL_LAYOUT_FRAMES = 50;
    // number of layout frames per slice
    private static final int _INTERP_FRAMES = 30;
    // number of steps per frame
    private static final int _STEPS_PER_FRAME = 10;

    // time to wait while each frame is rendered
    private static final int _FRAME_DELAY = 10;

    private final TimeGraphController gc;
    private final TimeGraphComponent component;

    private boolean iterateLayout = true; // whether to iterate layout throughout export
    
    private MovieExport maker;
    private final ProgressMonitor pm;

    private String status = "preparing to start...";
    private boolean stop = false;
    private int curSl = 0;
    private int maxSl = 0;

    public ExportMovieTask(TimeGraphController gc, TimeGraphComponent tComponent, MovieExport maker, ProgressMonitor pm) {
        this.component = tComponent;
        this.gc = gc;
        GraphManager mg = tComponent.getGraphComponent().getGraphManager();
        mg.setLayoutAnimating(false);
        
        this.maker = maker;
        this.pm = pm;
        pm.setMillisToDecideToPopup(200);
        pm.setMillisToPopup(300);
    }

    public boolean isIterateLayout() { return iterateLayout; }
    public void setIterateLayout(boolean iterateLayout) { this.iterateLayout = iterateLayout; }

    public void stop() { stop = true; }
    public boolean isDone() { return stop; }
    public int getTaskLength() { return maxSl; }

    private void reportStatus() {
        if (pm == null)
            gc.reportStatus("ExportMovieTask: " + status);
        else {
            pm.setProgress(curSl);
            pm.setNote(status);
        }
    }

    /**
     * Captures a number of steps for currently active layout.
     */
    void captureLayoutSteps(int nFrames) {
        for (int i = 0; i < nFrames; i++) {
            for (int j = 0; j < _STEPS_PER_FRAME; j++)
                if (iterateLayout)
                component.getGraphAdapter().getGraphManager().iterateLayout();
            maker.captureImage();
            try { Thread.sleep(_FRAME_DELAY); } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }

    public void run() {
        component.hidePlot();
        try {
            List<Double> times = component.getManager().getTimeGraph().getTimes();
            int endIndex = times.size();
            int numFrames = endIndex * _INTERP_FRAMES;
            maker.setupMovie(component.getGraphComponent(), numFrames);
            if (iterateLayout) {
                Map<Object,Point2D.Double> iPos = component.getGraphAdapter().getGraphManager().getNodePositions();
                component.getGraphAdapter().getGraphManager().setLayoutAlgorithm(new SpringLayout(iPos));
    //            component.getManager().initLayoutAlgorithm();
    //            gm.setLayoutAlgorithm(new SpringLayout(iPos));
            }
            curSl = 0;
            gc.setTime(times.get(curSl));
            captureLayoutSteps(_INITIAL_LAYOUT_FRAMES);
            maker.captureImage();
            //movie export loop
            maxSl = endIndex - 1;
            if (pm != null)
                pm.setMaximum(maxSl);
            while (curSl < maxSl) {
                curSl++;
                status = "Exporting slice " + curSl;
                reportStatus();
                gc.setTime(times.get(curSl));
                captureLayoutSteps(_INTERP_FRAMES);
                if (stop) break;
            }
            maker.finishMovie();
            maker = null;
            stop = true;
            status = "Movie export finished";
            reportStatus();
        } catch (Exception e) {
            status = "Movie export error: " + e;
            e.printStackTrace();
            stop = true;
            reportStatus();
            maker.finishMovie();
            maker = null;
        }
        component.showPlot();
    }
}
