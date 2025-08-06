package edu.wpi.first.math.curves;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Num;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.Vector;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N6;

public class QuinticSpline<N extends Num> implements CurveNd<N> {
    public final Matrix<N, N6> coefficients;

    public QuinticSpline(Matrix<N, N6> coefficients) {
        this.coefficients = coefficients;
    }

    public static QuinticSpline<N1> create1dSpline(Vector<N6> coefficients) {
        return new QuinticSpline<>(coefficients.transpose());
    }

    @Override
    public Vector<N> getPoint(double t) {
        return new Vector<>(coefficients.times(
            VecBuilder.fill(
                1,
                t,
                t * t,
                t * t * t,
                t * t * t * t,
                t * t * t * t * t
            )
        ));
    }

    @Override public int getDim() {
        return coefficients.getNumRows();
    }
}
