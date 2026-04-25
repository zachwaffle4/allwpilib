package org.wpilib.examples.decodestarterbot;

/**
 * Tunable constants for the StarterBot example.
 *
 * <p>These values are intentionally grouped here so teams can tune launcher timing/speed and drive
 * conversion values without changing opmode logic.
 */
public final class StarterBotConstants {
  /** Duration, in seconds, to run feeder servos for each shot. */
  public static final double FEED_TIME =
      0.20; // The feeder servos run this long when a shot is requested.

  /*
   * When we control our launcher motor, we are using encoders. These allow the control system
   * to read the current speed of the motor and apply more or less power to keep it at a constant
   * velocity. Here we are setting the target and minimum velocity that the launcher should run
   * at. The minimum velocity is a threshold for determining when to fire.
   */
  public static final double LAUNCHER_TARGET_VELOCITY = 1125;

  /** Minimum launcher velocity required before feeding a projectile. */
  public static final double LAUNCHER_MIN_VELOCITY = 1075;

  /*
   * The number of seconds that we wait between each of our 3 shots from the launcher. This
   * can be much shorter, but the longer break is reasonable since it maximizes the likelihood
   * that each shot will score.
   */
  /** Delay, in seconds, between autonomous shots. */
  public static final double TIME_BETWEEN_SHOTS = 2;

  /*
   * Here we capture a few variables used in driving the robot. DRIVE_SPEED and ROTATE_SPEED
   * are from 0-1, with 1 being full speed. Encoder ticks per revolution is specific to the motor
   * ratio that we use in the kit; if you're using a different motor, this value can be found on
   * the product page for the motor you're using.
   * Track width is the distance between the center of the drive wheels on either side of the
   * robot. Track width is used to determine the amount of linear distance each wheel needs to
   * travel to create a specified rotation of the robot.
   */
  /** Open-loop drive speed used by this example's movement helpers. */
  public static final double DRIVE_SPEED = 0.5;

  /** Open-loop rotate speed used by this example's movement helpers. */
  public static final double ROTATE_SPEED = 0.2;

  /** Drive wheel diameter used for distance-to-ticks conversion. */
  public static final double WHEEL_DIAMETER_MM = 96;

  /** Motor encoder ticks per wheel revolution for the starter kit drivetrain. */
  public static final double ENCODER_TICKS_PER_REV = 537.7;

  /** Encoder ticks per millimeter of travel. */
  public static final double TICKS_PER_MM = ENCODER_TICKS_PER_REV / (WHEEL_DIAMETER_MM * Math.PI);

  /** Distance between left and right wheel contact patch centers. */
  public static final double TRACK_WIDTH_MM = 404;

  /** Zero throttle/velocity for motors and servos. */
  public static final double STOP_SPEED = 0.0;

  /** Full forward throttle for feeder servos. */
  public static final double FULL_SPEED = 1.0;

  private StarterBotConstants() {}
}
