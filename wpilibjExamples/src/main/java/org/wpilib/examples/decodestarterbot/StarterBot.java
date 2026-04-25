package org.wpilib.examples.decodestarterbot;

import org.wpilib.drive.DifferentialDrive;
import org.wpilib.driverstation.UserControlsInstance;
import org.wpilib.framework.OpModeRobot;
import org.wpilib.hardware.expansionhub.ExpansionHubCRServo;
import org.wpilib.hardware.expansionhub.ExpansionHubMotor;
import org.wpilib.system.RobotController;

/**
 * Main robot class for the StarterBot example. This robot features a differential drive system, a
 * launcher motor with velocity control, and feeder servos for launching projectiles.
 *
 * <p>This class owns hardware objects and provides reusable actuator actions shared by autonomous
 * and teleop opmodes.
 */
@UserControlsInstance(StarterBotControls.class)
public class StarterBot extends OpModeRobot {
  public enum LaunchState {
    IDLE,
    SPIN_UP,
    FEEDING
  }

  // Drive motors - configured for differential/skid-steer drive
  public final ExpansionHubMotor leftDrive = new ExpansionHubMotor(0, 0);
  public final ExpansionHubMotor rightDrive = new ExpansionHubMotor(0, 1);

  // Launcher motor - uses velocity control for consistent shots
  public final ExpansionHubMotor launcher = new ExpansionHubMotor(0, 2);

  // Feeder servos - work together to feed projectiles into the launcher
  public final ExpansionHubCRServo leftFeeder = new ExpansionHubCRServo(0, 0);
  public final ExpansionHubCRServo rightFeeder = new ExpansionHubCRServo(0, 1);

  // Drive object
  public final DifferentialDrive drive =
      new DifferentialDrive(leftDrive::setThrottle, rightDrive::setThrottle);

  private LaunchState launchState = LaunchState.IDLE;
  private long shotTimerStart = 0;
  private long feederTimerStart = 0;

  /** Creates the StarterBot hardware map and applies motor/servo inversion defaults. */
  public StarterBot() {
    leftDrive.setReversed(true);
    rightDrive.setReversed(false);

    leftFeeder.setReversed(true);
    rightFeeder.setReversed(false);
  }

  /** Resets both drive encoder positions to zero. */
  public void resetDriveEncoders() {
    leftDrive.resetEncoder();
    rightDrive.resetEncoder();
  }

  /**
   * Sets closed-loop drive position setpoints for both sides.
   *
   * @param left Left drive setpoint in encoder ticks.
   * @param right Right drive setpoint in encoder ticks.
   */
  public void setDrivePositionSetpoints(double left, double right) {
    leftDrive.setPositionSetpoint(left);
    rightDrive.setPositionSetpoint(right);
  }

  /**
   * Sets both feeder servos to the same throttle.
   *
   * @param throttle Servo throttle in the range [-1, 1].
   */
  public void setFeedersThrottle(double throttle) {
    leftFeeder.setThrottle(throttle);
    rightFeeder.setThrottle(throttle);
  }

  /** Resets launcher-cycle state and feeder output to idle. */
  public void resetLauncherCycle() {
    launchState = LaunchState.IDLE;
    shotTimerStart = 0;
    feederTimerStart = 0;
    setFeedersThrottle(StarterBotConstants.STOP_SPEED);
  }

  /** Returns the current launcher-cycle state. */
  public LaunchState getLauncherCycleState() {
    return launchState;
  }

  /**
   * Runs one launcher/feed state-machine iteration.
   *
   * @param shotRequested True to request a new shot cycle.
   * @param minCycleSeconds Minimum total cycle time before reporting completion.
   * @return True when the current shot cycle has completed.
   */
  public boolean runLauncherCycle(boolean shotRequested, double minCycleSeconds) {
    switch (launchState) {
      case IDLE:
        if (shotRequested) {
          launchState = LaunchState.SPIN_UP;
          shotTimerStart = RobotController.getMonotonicTime();
        }
        break;
      case SPIN_UP:
        launcher.setVelocitySetpoint(StarterBotConstants.LAUNCHER_TARGET_VELOCITY);
        if (launcher.getEncoderVelocity() > StarterBotConstants.LAUNCHER_MIN_VELOCITY) {
          launchState = LaunchState.FEEDING;
          setFeedersThrottle(StarterBotConstants.FULL_SPEED);
          feederTimerStart = RobotController.getMonotonicTime();
        }
        break;
      case FEEDING:
        double feederElapsed =
            (RobotController.getMonotonicTime() - feederTimerStart) / 1_000_000.0;
        if (feederElapsed > StarterBotConstants.FEED_TIME) {
          setFeedersThrottle(StarterBotConstants.STOP_SPEED);
          double shotElapsed = (RobotController.getMonotonicTime() - shotTimerStart) / 1_000_000.0;
          if (shotElapsed > minCycleSeconds) {
            launchState = LaunchState.IDLE;
            return true;
          }
        }
        break;
      default:
        throw new IllegalStateException("Invalid launcher state: " + launchState);
    }
    return false;
  }

  /** Stops drive, launcher, and feeders. Safe to call repeatedly in disabled/end loops. */
  public void stopAllActuators() {
    drive.arcadeDrive(0, 0);
    launcher.setVelocitySetpoint(StarterBotConstants.STOP_SPEED);
    resetLauncherCycle();
  }
}
