/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bm.blaise.scio.graph.io;

import org.bm.blaise.scio.graph.time.TimeGraph;
import java.util.HashMap;
import org.bm.blaise.scio.graph.io.AbstractGraphIO.GraphType;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author elisha
 */
public class PajekLongGraphIOTest {

    static TimeGraph<Integer> SAMPLE_NEWFRAT;

    public static TimeGraph<Integer> sampleNewFrat() {
        if (SAMPLE_NEWFRAT == null)
            SAMPLE_NEWFRAT = (TimeGraph<Integer>) PajekTimeGraphIO.getInstance().importGraph(
                new HashMap<Integer,double[]>(),
                PajekTimeGraphIO.class.getResource("data/newfrat.netx"), GraphType.DYNAMIC);
        return SAMPLE_NEWFRAT;
    }

    /**
     * Test of readIntegerGraphFile method, of class SimpleGraphIO.
     */
    @Test
    public void testImportGraph() {
        System.out.println("-- LongitudinalGraphIOTest --");
        System.out.println("importGraph: MANUALLY CHECK FOR DESIRED OUTPUT");
        assertEquals(15, sampleNewFrat().getTimes().size());
        assertEquals(0.0, sampleNewFrat().getMinimumTime(), 1e-10);
        assertEquals(15.0, sampleNewFrat().getMaximumTime(), 1e-10);
        System.out.println(sampleNewFrat());
    }
    
}