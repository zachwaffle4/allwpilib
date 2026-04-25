package org.wpilib.examples.decodestarterbot;

import org.wpilib.driverstation.DefaultUserControls;
import org.wpilib.driverstation.Gamepad;

/**
 * Custom user controls implementation for StarterBot. Provides convenient access to gamepad inputs
 * used by the robot.
 */
public class StarterBotControls extends DefaultUserControls {

  /**
   * Get the primary driver gamepad (port 0).
   *
   * @return The primary gamepad instance.
   */
  public Gamepad getDriverGamepad() {
    return getGamepad(0);
  }

  /**
   * Get the forward/backward drive input from the left stick Y axis.
   *
   * @return The drive value, positive is forward.
   */
  public double getDriveForward() {
    return -getDriverGamepad().getLeftY();
  }

  /**
   * Get the rotation input from the right stick X axis.
   *
   * @return The rotation value, positive is clockwise.
   */
  public double getDriveRotation() {
    return getDriverGamepad().getRightX();
  }

  /**
   * Check if the launcher spin-up button (Y) is pressed.
   *
   * @return True if Y button is pressed.
   */
  public boolean isLauncherSpinUpPressed() {
    return getDriverGamepad().getNorthFaceButton();
  }

  /**
   * Check if the launcher stop button (B) is pressed.
   *
   * @return True if B button is pressed.
   */
  public boolean isLauncherStopPressed() {
    return getDriverGamepad().getEastFaceButton();
  }

  /**
   * Check if the launch button (right bumper) was pressed since last check.
   *
   * @return True if right bumper was just pressed.
   */
  public boolean isLaunchButtonPressed() {
    return getDriverGamepad().getRightBumperButtonPressed();
  }
}
