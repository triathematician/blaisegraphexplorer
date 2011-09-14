/**
 * GraphExplorerApp.java
 * Created Aug 17, 2011
 */
package graphexplorer;

import data.propertysheet.editor.EditorRegistration;
import javax.swing.JFrame;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The application 
 * @author elisha
 */
public class GraphExplorerApp extends SingleFrameApplication {

    @Override
    protected void startup() {        
        EditorRegistration.registerEditors();
        show(new GraphExplorerView(this));
    }
    
    public static GraphExplorerApp getApplication() {
        return Application.getInstance(GraphExplorerApp.class);
    }
    

    /**
    * @param args the command line arguments
    */
    public static void main(String args[]) {
        launch(GraphExplorerApp.class, args);
    }
}