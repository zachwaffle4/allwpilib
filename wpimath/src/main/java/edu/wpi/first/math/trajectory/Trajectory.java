package edu.wpi.first.math.trajectory;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.measure.Distance;
import edu.wpi.first.units.measure.Time;

public interface Trajectory {
    public static class Sample {
        public Time time;
        public Pose2d pose;
        public ChassisSpeeds speed;
        public ChassisAccelerations accel;

        public Sample(Time time, Pose2d pose, ChassisSpeeds speed, ChassisAccelerations accel) {
            this.time = time;
            this.pose = pose;
            this.speed = speed;
            this.accel = accel;
        }
    }

    Time duration();
    Distance length();

    Sample sampleAt(Time time);
    Pose2d poseAt(Time time);

    default Sample start() { return sampleAt(Units.Seconds.of(0.0)); }
    default Sample end() { return sampleAt(duration()); }
}
