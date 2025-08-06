package edu.wpi.first.math.curves;

import edu.wpi.first.math.Num;
import edu.wpi.first.math.Vector;

/**
 * Represents a parametric curve in N dimensions.
 *
 * @param <N>
 */
public interface CurveNd<N extends Num> {
    int getDim();

    /**
     * Gets the point at time t.
     * @param t value between 0 and 1.
     */
    Vector<N> getPoint(double t);
}
