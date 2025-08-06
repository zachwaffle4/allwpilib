package edu.wpi.first.math.trajectory.struct;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.ChassisAccelerations;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.units.Units;
import edu.wpi.first.units.measure.Time;
import edu.wpi.first.util.struct.Struct;

import java.nio.ByteBuffer;

public class TrajectorySampleStruct implements Struct<Trajectory.Sample> {
    @Override
    public Class<Trajectory.Sample> getTypeClass() {
        return Trajectory.Sample.class;
    }

    @Override
    public String getTypeName() {
        return "TrajectorySample";
    }

    @Override
    public int getSize() {
        return kSizeDouble + Pose2d.struct.getSize() + ChassisSpeeds.struct.getSize() + ChassisAccelerations.struct.getSize();
    }

    @Override
    public String getSchema() {
        return "double time;Pose2d pose;ChassisSpeeds speed;ChassisAccelerations accel";
    }

    @Override
    public Trajectory.Sample unpack(ByteBuffer bb) {
        Time timestamp = Units.Seconds.of(bb.getDouble());
        Pose2d pose = Pose2d.struct.unpack(bb);
        ChassisSpeeds speed = ChassisSpeeds.struct.unpack(bb);
        ChassisAccelerations accel = ChassisAccelerations.struct.unpack(bb);
        return new Trajectory.Sample(timestamp, pose, speed, accel);
    }

    @Override
    public void pack(ByteBuffer bb, Trajectory.Sample value) {
        bb.putDouble(value.time.in(Units.Seconds));
        Pose2d.struct.pack(bb, value.pose);
        ChassisSpeeds.struct.pack(bb, value.speed);
        ChassisAccelerations.struct.pack(bb, value.accel);
    }
}
