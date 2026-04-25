package org.wpilib.examples.decodestarterbot;

import static org.wpilib.units.Units.Degrees;
import static org.wpilib.units.Units.Inches;
import static org.wpilib.units.Units.Millimeters;
import static org.wpilib.units.Units.Radians;

import org.wpilib.driverstation.Alliance;
import org.wpilib.driverstation.MatchState;
import org.wpilib.opmode.Autonomous;
import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.system.RobotController;
import org.wpilib.units.measure.Angle;
import org.wpilib.units.measure.Distance;

/**
 * Autonomous routine for the StarterBot example.
 *
 * <p>This opmode launches three projectiles, drives away from the goal, turns based on alliance,
 * and then drives off the line.
 */
@Autonomous(
    name = "StarterBotAuto",
    group = "StarterBot",
    description = "Autonomous mode for StarterBot")
public class StarterBotAuto extends PeriodicOpMode {
  private final StarterBot robot;

  private enum AutonomousState {
    LAUNCH,
    WAIT_FOR_LAUNCH,
    DRIVING_AWAY_FROM_GOAL,
    ROTATING,
    DRIVING_OFF_LINE,
    COMPLETE
  }

  private AutonomousState autonomousState = AutonomousState.LAUNCH;

  private int shotsToFire = 3;
  private double robotRotationAngle = 45;
  private Alliance alliance = null;

  // Motor state
  private double leftTargetPosition = 0;
  private double rightTargetPosition = 0;
  private boolean motionActive = false;
  private long motionWithinToleranceStart = 0;

  private static final double kMotionToleranceMm = 10;
  private static final double kMotionHoldSeconds = 1.0;

  /**
   * Creates the autonomous opmode.
   *
   * @param robot Shared robot hardware/actions instance.
   */
  public StarterBotAuto(StarterBot robot) {
    this.robot = robot;

    autonomousState = AutonomousState.LAUNCH;
    shotsToFire = 3;
    leftTargetPosition = 0;
    rightTargetPosition = 0;
    motionActive = false;
    motionWithinToleranceStart = 0;
  }

  /** Initializes autonomous state, timers, and mechanism defaults. */
  @Override
  public void start() {
    autonomousState = AutonomousState.LAUNCH;
    shotsToFire = 3;
    clearMotion();

    robot.stopAllActuators();
    robot.resetDriveEncoders();
    robot.resetLauncherCycle();

    alliance = MatchState.getAlliance().orElse(Alliance.RED);
  }

  /** Runs one autonomous loop iteration and publishes dashboard status. */
  @Override
  public void periodic() {
    switch (autonomousState) {
      case LAUNCH -> {
        robot.runLauncherCycle(true, StarterBotConstants.TIME_BETWEEN_SHOTS);
        autonomousState = AutonomousState.WAIT_FOR_LAUNCH;
      }
      case WAIT_FOR_LAUNCH -> {
        if (robot.runLauncherCycle(false, StarterBotConstants.TIME_BETWEEN_SHOTS)) {
          shotsToFire--;
          if (shotsToFire > 0) {
            autonomousState = AutonomousState.LAUNCH;
          } else {
            robot.resetDriveEncoders();
            robot.launcher.setVelocitySetpoint(StarterBotConstants.STOP_SPEED);
            autonomousState = AutonomousState.DRIVING_AWAY_FROM_GOAL;
          }
        }
      }
      case DRIVING_AWAY_FROM_GOAL -> {
        if (!motionActive) {
          beginDriveMotion(Inches.of(-4));
        }

        if (isMotionComplete()) {
          robot.resetDriveEncoders();
          clearMotion();
          autonomousState = AutonomousState.ROTATING;
        }
      }
      case ROTATING -> {
        if (alliance == Alliance.RED) {
          robotRotationAngle = 45;
        } else if (alliance == Alliance.BLUE) {
          robotRotationAngle = -45;
        }

        if (!motionActive) {
          beginRotateMotion(Degrees.of(robotRotationAngle));
        }

        if (isMotionComplete()) {
          robot.resetDriveEncoders();
          clearMotion();
          autonomousState = AutonomousState.DRIVING_OFF_LINE;
        }
      }
      case DRIVING_OFF_LINE -> {
        if (!motionActive) {
          beginDriveMotion(Inches.of(-26));
        }

        if (isMotionComplete()) {
          clearMotion();
          autonomousState = AutonomousState.COMPLETE;
        }
      }
      case COMPLETE -> robot.stopAllActuators();
      default -> {
        throw new IllegalStateException("Invalid autonomous state: " + autonomousState);
      }
    }

    // Update telemetry
    SmartDashboard.putString("AutoState", autonomousState.toString());
    SmartDashboard.putString("LauncherState", robot.getLauncherCycleState().toString());
    SmartDashboard.putString(
        "Motor Current Positions",
        String.format(
            "left (%.0f), right (%.0f)",
            robot.leftDrive.getEncoderPosition(), robot.rightDrive.getEncoderPosition()));
    SmartDashboard.putString(
        "Motor Target Positions",
        String.format("left (%.0f), right (%.0f)", leftTargetPosition, rightTargetPosition));
  }

  /** Stops all mechanisms when autonomous exits. */
  @Override
  public void end() {
    robot.stopAllActuators();
    robot.resetLauncherCycle();
  }

  private void beginDriveMotion(Distance distance) {
    double targetPosition = distance.in(Millimeters) * StarterBotConstants.TICKS_PER_MM;
    beginMotion(targetPosition, targetPosition);
  }

  private void beginRotateMotion(Angle angle) {
    double targetMm = angle.in(Radians) * (StarterBotConstants.TRACK_WIDTH_MM / 2);
    beginMotion(
        -(targetMm * StarterBotConstants.TICKS_PER_MM),
        targetMm * StarterBotConstants.TICKS_PER_MM);
  }

  private void beginMotion(double leftTarget, double rightTarget) {
    motionActive = true;
    motionWithinToleranceStart = 0;
    leftTargetPosition = leftTarget;
    rightTargetPosition = rightTarget;
    robot.setDrivePositionSetpoints(leftTarget, rightTarget);
  }

  private boolean isMotionComplete() {
    if (!motionActive) {
      return false;
    }

    double toleranceTicks = kMotionToleranceMm * StarterBotConstants.TICKS_PER_MM;
    double leftError = Math.abs(leftTargetPosition - robot.leftDrive.getEncoderPosition());
    double rightError = Math.abs(rightTargetPosition - robot.rightDrive.getEncoderPosition());

    if (leftError > toleranceTicks || rightError > toleranceTicks) {
      motionWithinToleranceStart = 0;
      return false;
    }

    if (motionWithinToleranceStart == 0) {
      motionWithinToleranceStart = RobotController.getMonotonicTime();
      return false;
    }

    double holdElapsed =
        (RobotController.getMonotonicTime() - motionWithinToleranceStart) / 1_000_000.0;
    return holdElapsed > kMotionHoldSeconds;
  }

  private void clearMotion() {
    motionActive = false;
    motionWithinToleranceStart = 0;
    leftTargetPosition = 0;
    rightTargetPosition = 0;
  }
}
