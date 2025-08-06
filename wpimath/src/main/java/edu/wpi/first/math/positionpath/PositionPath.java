package edu.wpi.first.math.positionpath;

import edu.wpi.first.math.geometry.Translation2d;

public interface PositionPath {
    double length();

    /**
     * Gets the point at the specified distance along the path.
     */
     Translation2d getPoint(double s);
}
