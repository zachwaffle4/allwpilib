package org.wpilib.command3;

import org.wpilib.framework.OpModeRobot;
import org.wpilib.hardware.hal.RobotMode;
import org.wpilib.util.Color;

/**
 * Base robot class for command-based programs.
 *
 * <p>This class wires a {@link Scheduler} into the robot lifecycle: it registers
 * a periodic callback that invokes {@link #robotPeriodic()} on the provided
 * scheduler. It also provides convenient factory methods for creating and
 * registering {@link CommandOpMode} instances for the common robot modes
 * (autonomous, teleoperated, and test).
 *
 * <p>Typical usage:
 * <pre>
 * public class MyRobot extends CommandRobot {
 *   public MyRobot() {
 *     super(Scheduler.getDefault());
 *     autonomous("Auto")
 *       .enabled.onTrue(someCommand);
 *   }
 * }
 * </pre>
 */
public abstract class CommandRobot extends OpModeRobot {
    /**
     * Create a CommandRobot that registers the provided scheduler's periodic
     * callback which will call {@link #robotPeriodic()} every scheduler cycle.
     *
     * @param scheduler the Scheduler to attach the robot periodic callback to
     */
    public CommandRobot(Scheduler scheduler) {
        scheduler.addPeriodic(this::robotPeriodic);
    }

    /**
     * Convenience constructor that uses the default {@link Scheduler}.
     */
    public CommandRobot() {
        this(Scheduler.getDefault());
    }

    /**
     * Periodic hook invoked by the scheduler each cycle. Subclasses should
     * override this method to perform robot-wide periodic work (telemetry,
     * diagnostics, subsystem updates, etc.). This method is called from the
     * scheduler's periodic context.
     */
    public void robotPeriodic() {}

    /**
     * Create and register an autonomous {@link CommandOpMode} with the robot.
     *
     * <p>This convenience method constructs a new {@link CommandOpMode}, adds a
     * factory that returns it to the underlying {@link OpModeRobot} registry,
     * and associates it with {@link RobotMode#AUTONOMOUS} along with the
     * provided metadata.
     *
     * @param name display name of the opmode
     * @param group grouping used by the UI
     * @param description short description shown in the UI
     * @param textColor optional foreground color for the opmode entry
     * @param backgroundColor optional background color for the opmode entry
     * @return the created {@link CommandOpMode} instance for further setup
     */
    public CommandOpMode autonomous(String name, String group, String description, Color textColor, Color backgroundColor) {
        var opMode = new CommandOpMode();
        addOpModeFactory(() -> opMode, RobotMode.AUTONOMOUS, name, group, description, textColor, backgroundColor);
        return opMode;
    }

    /** Convenience overload that omits color metadata. */
    public CommandOpMode autonomous(String name, String group, String description) {
        return autonomous(name, group, description, null, null);
    }

    /** Convenience overload that omits the description. */
    public CommandOpMode autonomous(String name, String group) {
        return autonomous(name, group, "");
    }

    /** Convenience overload that only specifies the name. */
    public CommandOpMode autonomous(String name) {
        return autonomous(name, "", "");
    }

    /**
     * Create and register a teleoperated {@link CommandOpMode} with the robot.
     *
     * @param name display name of the opmode
     * @param group grouping used by the UI
     * @param description short description shown in the UI
     * @param textColor optional foreground color for the opmode entry
     * @param backgroundColor optional background color for the opmode entry
     * @return the created {@link CommandOpMode} instance for further setup
     */
    public CommandOpMode teleop(String name, String group, String description, Color textColor, Color backgroundColor) {
        var opMode = new CommandOpMode();
        addOpModeFactory(() -> opMode, RobotMode.TELEOPERATED, name, group, description, textColor, backgroundColor);
        return opMode;
    }

    /** Convenience overload that omits color metadata. */
    public CommandOpMode teleop(String name, String group, String description) {
        return teleop(name, group, description, null, null);
    }

    /** Convenience overload that omits the description. */
    public CommandOpMode teleop(String name, String group) {
        return teleop(name, group, "");
    }

    /** Convenience overload that only specifies the name. */
    public CommandOpMode teleop(String name) {
        return teleop(name, "", "");
    }

    /**
     * Create and register a test {@link CommandOpMode} with the robot.
     *
     * @param name display name of the opmode
     * @param group grouping used by the UI
     * @param description short description shown in the UI
     * @param textColor optional foreground color for the opmode entry
     * @param backgroundColor optional background color for the opmode entry
     * @return the created {@link CommandOpMode} instance for further setup
     */
    public CommandOpMode testOpMode(String name, String group, String description, Color textColor, Color backgroundColor) {
        var opMode = new CommandOpMode();
        addOpModeFactory(() -> opMode, RobotMode.TEST, name, group, description, textColor, backgroundColor);
        return opMode;
    }

    /** Convenience overload that omits color metadata. */
    public CommandOpMode testOpMode(String name, String group, String description) {
        return testOpMode(name, group, description, null, null);
    }

    /** Convenience overload that omits the description. */
    public CommandOpMode testOpMode(String name, String group) {
        return testOpMode(name, group, "");
    }

    /** Convenience overload that only specifies the name. */
    public CommandOpMode testOpMode(String name) {
        return testOpMode(name, "", "");
    }
}
