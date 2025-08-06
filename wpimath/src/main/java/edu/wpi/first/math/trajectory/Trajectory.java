package edu.wpi.first.math.trajectory;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Time;

public class Trajectory {
    public static class Sample {
        public final Time time;
        public final Pose2d pose;
        public final ChassisSpeeds speed;
        public final ChassisAccelerations accel;

        public Sample(Time time, Pose2d pose, ChassisSpeeds speed, ChassisAccelerations accel) {
            this.time = time;
            this.pose = pose;
            this.speed = speed;
            this.accel = accel;
        }

        public Sample() {
            this(Units.Seconds.of(0.0), new Pose2d(), new ChassisSpeeds(), new ChassisAccelerations());
        }
    }

    public Time duration() {
        return Units.Seconds.of(-1.0);
    }
    public Distance length() {
        return Units.Meters.of(-1.0);
    }

    public Sample sampleAt(Time time) {
        return new Sample();
    }
    public Pose2d poseAt(Time time) {
        return new Pose2d();
    }

    public Sample start() { return sampleAt(Units.Seconds.of(0.0)); }
    public Sample end() { return sampleAt(duration()); }
}
