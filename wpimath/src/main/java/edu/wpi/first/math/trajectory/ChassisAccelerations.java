package edu.wpi.first.math.trajectory;

import edu.wpi.first.math.controller.struct.ArmFeedforwardStruct;
import edu.wpi.first.math.trajectory.struct.ChassisAccelerationsStruct;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.measure.AngularAcceleration;
import edu.wpi.first.units.measure.LinearAcceleration;
import edu.wpi.first.util.struct.Struct;
import edu.wpi.first.util.struct.StructSerializable;

import java.util.Objects;

public class ChassisAccelerations implements StructSerializable {
    /** Acceleration along the x-axis in meters per second squared. (Fwd is +) */
    public final double ax;

    /** Acceleration along the y-axis in meters per second squared. (Left is +) */
    public final double ay;

    /** Angular acceleration of the robot frame in radians per second squared. (CCW is +) */
    public final double alpha;

    /** ChassisAccelerations struct for serialization. */
    public static final ChassisAccelerationsStruct struct = new ChassisAccelerationsStruct();

    /**
     * Constructs a ChassisAccelerations with zeros for ax, ay, and alpha.
     */
    public ChassisAccelerations() {
        this(0.0, 0.0, 0.0);
    }

    /**
     * Constructs a ChassisAccelerations object.
     *
     * @param ax Forward acceleration in meters per second squared.
     * @param ay Sideways acceleration in meters per second squared.
     * @param alpha Angular acceleration in radians per second squared.
     */
    public ChassisAccelerations(double ax, double ay, double alpha) {
        this.ax = ax;
        this.ay = ay;
        this.alpha = alpha;
    }

    /**
     * Constructs a ChassisAccelerations object.
     *
     * @param ax Forward acceleration.
     * @param ay Sideways acceleration.
     * @param alpha Angular acceleration.
     */
    public ChassisAccelerations(LinearAcceleration ax, LinearAcceleration ay, AngularAcceleration alpha) {
        this(ax.in(Units.MetersPerSecondPerSecond), ay.in(Units.MetersPerSecondPerSecond), alpha.in(Units.RadiansPerSecondPerSecond));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChassisAccelerations that)) return false;
        return Double.compare(ax, that.ax) == 0 && Double.compare(ay, that.ay) == 0 && Double.compare(alpha, that.alpha) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ax, ay, alpha);
    }

    @Override
    public String toString() {
        return String.format(
                "ChassisAcceleration(Ax: %.2f m/s², Ay: %.2f m/s², Alpha: %.2f rad/s²)", ax, ay, alpha);
    }
}
