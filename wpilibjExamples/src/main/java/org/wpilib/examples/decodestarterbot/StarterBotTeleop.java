package org.wpilib.examples.decodestarterbot;

import org.wpilib.opmode.PeriodicOpMode;
import org.wpilib.opmode.Teleop;
import org.wpilib.smartdashboard.SmartDashboard;

/**
 * Teleoperated mode for the StarterBot. Provides driver control of the robot with arcade-style
 * driving, launcher control, and projectile feeding mechanisms.
 *
 * <p>Drive controls are open-loop arcade drive, while the launcher/feed system runs a small state
 * machine so feeding starts only after the launcher reaches minimum velocity.
 */
@Teleop(
    name = "StarterBotTeleop",
    group = "StarterBot",
    description = "Driver-controlled mode for StarterBot")
public class StarterBotTeleop extends PeriodicOpMode {
  private final StarterBot robot;
  private final StarterBotControls userControls;

  /**
   * Constructor accepting the robot instance and user controls.
   *
   * @param robot The robot instance containing hardware references.
   * @param userControls The user controls for gamepad input.
   */
  public StarterBotTeleop(StarterBot robot, StarterBotControls userControls) {
    this.robot = robot;
    this.userControls = userControls;
  }

  /** Initializes teleop state and publishes initial dashboard status. */
  @Override
  public void start() {
    resetTeleopState();

    SmartDashboard.putString("Status", "Initialized");
  }

  /** Runs one teleop loop iteration: drive, launcher commands, feed state machine, dashboard. */
  @Override
  public void periodic() {
    // Drive control using arcade drive
    double driveForward = userControls.getDriveForward();
    double driveRotation = userControls.getDriveRotation();
    robot.drive.arcadeDrive(driveForward, driveRotation);

    // Manual launcher speed control
    if (userControls.isLauncherSpinUpPressed()) {
      robot.launcher.setVelocitySetpoint(StarterBotConstants.LAUNCHER_TARGET_VELOCITY);
    } else if (userControls.isLauncherStopPressed()) {
      robot.launcher.setVelocitySetpoint(StarterBotConstants.STOP_SPEED);
    }

    // Launch control with shared robot state machine.
    robot.runLauncherCycle(userControls.isLaunchButtonPressed(), 0.0);

    // Update telemetry
    SmartDashboard.putString("State", robot.getLauncherCycleState().toString());
    SmartDashboard.putString(
        "Drive Powers", String.format("forward (%.2f), turn (%.2f)", driveForward, driveRotation));
    SmartDashboard.putNumber("Launcher Speed", robot.launcher.getEncoderVelocity());
  }

  /** Stops all mechanisms while disabled. */
  @Override
  public void disabledPeriodic() {
    resetTeleopState();
  }

  /** Stops all mechanisms when teleop ends. */
  @Override
  public void end() {
    resetTeleopState();
  }

  private void resetTeleopState() {
    robot.resetLauncherCycle();
    robot.stopAllActuators();
  }
}
