package edu.wpi.first.math.curves;

import edu.wpi.first.math.*;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.math.numbers.N6;
import org.ejml.simple.SimpleMatrix;

public class QuinticSpline<N extends Num> implements CurveNd<N> {
    public static final Matrix<N6, N6> HERMITE_COEFFICIENTS = new Matrix<>(Nat.N6(), Nat.N6(), new double[] {
            1, 0, 0, 0, 0, 0,
            0, 1, 0, 0, 0, 0,
            0, 0, 2, 0, 0, 0,
            0, 1, 2, 3, 4, 5,
            0, 0, 2, 6, 12, 20
    });

    public final Matrix<N, N6> coefficients;

    public QuinticSpline(Matrix<N, N6> coefficients) {
        this.coefficients = coefficients;
    }

    public static QuinticSpline<N1> create1dSpline(Vector<N6> coefficients) {
        return new QuinticSpline<>(coefficients.transpose());
    }

    public static QuinticSpline<N1> create1dHermiteSpline(Vector<N3> x0, Vector<N3> x1) {
        return create1dSpline(new Vector<>(HERMITE_COEFFICIENTS.times(
                new Matrix<>(Nat.N6(), Nat.N1(), new double[] {
                        x0.get(0), x0.get(1), x0.get(2),
                        x1.get(0), x1.get(1), x1.get(2)
                })
        )));
    }

    public static QuinticSpline<N2> create2dSpline(Vector<N6> xCoefficients, Vector<N6> yCoefficients) {
        return new QuinticSpline<>(
                new Matrix<>(
                        new SimpleMatrix(new double[][] { xCoefficients.getData(), yCoefficients.getData() })
                )
        );
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

    @Override public int dim() {
        return coefficients.getNumRows();
    }
}
