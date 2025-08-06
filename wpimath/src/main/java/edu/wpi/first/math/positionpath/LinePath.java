package edu.wpi.first.math.positionpath;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

public class LinePath implements PositionPath {
    public final Translation2d start;
    public final Translation2d direction;
    public final double length;

    public LinePath(Translation2d start, Rotation2d direction, double length) {
        this.start = start;
        this.direction = new Translation2d(direction.getCos(), direction.getSin());
        this.length = length;
    }

    public LinePath(Translation2d start, Translation2d end) {
        this.start = start;

        Translation2d dist = start.minus(end);

        this.length = dist.getNorm();
        this.direction = dist.div(length);
    }

    @Override public double length() {
        return length;
    }

    @Override
    public Translation2d getPoint(double s) {
        if (s <= 0) {
            return start;
        } else if (s >= length) {
            return start.plus(direction.times(length));
        }
        return start.plus(direction.times(s));
    }
}
