# Summary

This document describes how opmodes are implemented in command-based projects.

# Motivation

See [opmodes](opmodes.md) for the motivation for adding operator selectable opmodes to the core robot structure.  For command-based programs, the entire robot program is structured around command-based subsystems and commands tied to robot states and inputs, where everything can be constructed during initialization, so for more natural integration with the rest of the command-based framework, a different approach than the annotation-based approach used for periodic and linear opmodes is warranted. It will also be possible for teams to mix command-based and non-command-based opmodes in the same project, using annotations.

# Background

A significant portion of FRC teams use the command-based framework, which builds on top of the periodic model with the concepts of subsystems and commands and a scheduler that provides cooperative multitasking. Subsystem objects are typically member variables of the `Robot` class (or a separate class called `RobotContainer`), and command objects are created and bound to joystick buttons or other triggers during construction.  In this code model, most behaviors are modeled as commands configured during robot construction, and very little is done explicitly iteratively.  Subsystems have a standardized `periodic()` method that is called by the command scheduler, which is most often used for telemetry.  In the provided template code, the command scheduler is set up to run in all modes because it is called by the `robotPeriodic()` method.  Triggers can be configured to run specific commands at the start of disabled/teleop/auto/test modes.  Operator selection of autonomous is still done via `SendableChooser`, with the `SendableChooser` value usually being a `Command` that is started when the auto mode is started.  Notably, this model shares both subsystems and command implementations between all operational modes–only the commands that are being run change; in general this has been seen as a benefit because it enables reuse (e.g. a "set elevator to height" command is useful in both autonomous and teleoperated modes, and a subsystem operates fundamentally the same in both modes).

The command-based framework is an approach and implementation for structuring robot programs with the key concepts being subsystems, commands, and triggers.  Commands require certain subsystems to run.  While multiple commands can be running simultaneously, only one command can be running that requires a particular subsystem, so subsystems effectively provide mutually exclusive behavior.  If a command is started that requires a subsystem that some other command also requires, the currently running command is canceled.  Triggers are used to start commands; triggers are boolean suppliers that can be combined with logical operations (e.g. and, or).  A command can be started based on a trigger value (true or false) or a change in trigger value (true to false or false to true).  A command scheduler (run periodically, including when the robot is disabled) handles trigger updating and command scheduling, and also runs `periodic()` on each subsystem.

The 2025 version of the command-based framework provides a [`RobotModeTriggers`](https://github.wpilib.org/allwpilib/docs/release/java/edu/wpi/first/wpilibj2/command/button/RobotModeTriggers.html) class that provides triggers for each of the fixed robot modes (autonomous, teleop, test, and disabled) available in the 2025 FRC system.

In addition, third-party command-based frameworks inspired by WPILib's are also used by many FTC teams. While these frameworks are are developed by FTC students and mentors and are not officially supported by FIRST or the FTC SDK, they are widely used by FTC teams as they offer similar benefits to WPILib's command-based framework, most notably the ability to reuse commands between OpModes. The core concepts of command-based programming are the same, with users defining subsystems and commands, but the usage patterns are different. Teams create OpModes and register them with annotations, and each OpMode typically has its own set of triggers and commands, often registered in the OpMode's `start` method. Because command-based OpModes are registered with annotations just like standard Opmodes, it is possible (and somewhat common) to mix command-based and non-command-based OpModes in the same project.

# Design

Unlike the non-command-based approach, the command-based framework generally favors a design where commands can be used in all modes and explicit periodic code is discouraged.  In addition, the general approach for "modern" commands eschews classes, preferring a "fluent" method chained builder approach.  To support this, opmode registration for command-based can be performed either via annotations or via factory functions.

The design for multiple opmodes in the command-based framework extends the current `RobotModeTriggers` approach in two important ways:
- Instead of fixed modes, opmodes are explicitly created by the user
- The opmodes provide multiple triggers; this makes it possible to tie behaviors to DS selection/initialization as well as the enabled period

A unique `RobotBase` class is also provided that allows users to create command-based OpModes using factory functions. This class extends `OpModeRobot` and uses `OpModeRobot`'s functions for OpMode registration and initialization.

## `Context`s

This design uses the idea of [command contexts](command-contexts.md) to control when triggers are active. As describes in that document, a `CommandOpMode` is a `Context` whose condition is it being selected.

## CommandOpMode

The `CommandOpMode` class provides triggers to enable users to tie into each section of a opmode's lifetime.  Triggers have the ability to be tied to specific commands or actions on both transitions (e.g. false to true, true to false) and while the condition is in a particular state.  The framework will ensure these triggers always start in false state, even if code is started while attached to the field, so that it's safe to attach an action to the false to true transition of these triggers.

```java
public class CommandOpMode extends Context implements OpMode {
  // opmode is selected on DS (regardless of enabled or disabled)
  public final Trigger selected;

  // opmode is selected, robot is disabled
  public final Trigger disabled;

  // opmode is running, robot is enabled
  public final Trigger running;
}
```

`CommandOpMode` will use a similar mechanism to `PeriodicOpMode` for periodically running the command scheduler while the OpMode is running. It also overrides `disabledPeriodic` to run the scheduler in disabled mode.

## CommandRobot

The `CommandRobot` class is the base class for the user's command-based `Robot` class.  It also implements the private library machinery for robot startup and robot execution, and provides factory functions for creating opmodes.

```java
public class CommandRobot extends OpModeRobot {
  // this is run periodically by all OpModes after the scheduler is run
  public void robotPeriodic() {}

  // these are the factory functions for creating opmodes
  // they return CommandOpModes that can be used to tie triggers to opmodes on robot initialization
  // overloads with default values for group, description, and color are also provided
  public final CommandOpMode autonomous(String name, String group, String description, Color textColor, Color backgroundColor);
  public final CommandOpMode teleop(String name, String group, String description, Color textColor, Color backgroundColor);
  public final CommandOpMode test(String name, String group, String description, Color textColor, Color backgroundColor);
}
```

## Subsystem enhancements

The `Subsystem` interface adds an overload of `setDefaultCommand` to support creating per-opmode default commands, and similarly a new `removeDefaultCommand` overload.  The non-`CommandOpMode` overloads are applied in all opmodes where a per-opmode default has not been set.

```java
class Subsystem {
  // Sets a default command for this subsystem active only in the given opmode.
  default void setDefaultCommand(CommandOpMode opmode, Command defaultCommand);

  // Sets a default command for this subsystem active in all opmodes where there's no per-opmode default command.
  default void setDefaultCommand(Command defaultCommand);

  // Removes the default command for the given opmode.  No effect if none set.
  default void removeDefaultCommand(CommandOpMode opmode);

  // Removes the default command for all opmodes where there's no per-opmode default command.
  default void removeDefaultCommand();
}
```

## Java Robot Code Examples

The template/example code for command-based Java includes the following:
- A Robot class with subsystems and constructor that sets up a command-based teleop opmode, a couple of auto opmodes, and a test opmode

Robot:

```java
public class Robot extends CommandRobot {
  // actuators (Subsystem derived)
  public final Drive drive = new Drive();
  public final Intake intake = new Intake();
  public final Storage storage = new Storage();

  // sensors (not Subsystem derived)
  public final Vision vision = new Vision();

  public Robot() {
    // Automatically disable and retract the intake whenever the ball storage is full.
    storage.hasCargo.onTrue(intake.retractCommand());

    // Create auto opmodes
    addSimpleAuto();
    addPathAuto("drive and turn");

    // Create teleop opmodes
    addArcadeTeleop();
  }

  private void addSimpleAuto() {
    // A simple autonomous opmode
    autonomous("Simple Auto").running.whileTrue(Autos.simpleAuto(this));
  }

  private void addPathAuto(String path) {
    // A complex autonomous opmode that loads a path when selected in the DS while still disabled
    CommandOpMode opmode = autonomous(path, "Follow Path");
    opmode.selected.onTrue(Commands.runOnce(() -> Paths.loadPath(path)));
    opmode.running.whileTrue(Autos.followPath(this, path));
  }

  @Override
  public void robotPeriodic() {
    // this code is called periodically in all robot modes of operation after the scheduler is run

    // run vision processing in preparation for next loop
    vision.process();

    // output telemetry from all subsystems
    Telemetry.log("drive", drive);
    Telemetry.log("intake", intake);
    Telemetry.log("storage", storage);
  }
}
```

Autos:

```java
public class Autos {
  public static Command simpleAuto(Robot robot) {...}
  public static Command followPath(Robot robot, String path) {...}
}
```

Teleop:
```java
@Teleop(name = "Arcade Teleop")
public class ArcadeTeleop extends CommandOpMode {
  CommandXboxController driverController = new CommandXboxController(1);

  public ArcadeTeleop(CommandRobot robot) {
    super(robot);

    // Set the default command for the drive subsystem to use arcade drive.
    robot.drive.setDefaultCommand(this,
        robot.drive.arcadeDriveCommand(
            () -> -driverController.getLeftY(),
            () -> -driverController.getRightX()));

    // Deploy the intake with the X button
    running.and(driverController.x()).onTrue(robot.intake.intakeCommand());

    // Retract the intake with the Y button
    running.and(driverController.y()).onTrue(robot.intake.retractCommand());
  }
}
```

# Drawbacks

The ability to mix command-based and non-command-based opmodes in a single project is a nice feature, but it can potentially be confusing for teams who are not familiar with the command-based framework. This is discussed further in the Alternatives section.

# Trades

- Binding teleop joysticks is very verbose if different behavior is desired in different teleop opmodes.  Maybe add to CommandOpMode a separate event loop that's only active in that opmode?  At the minimum, it may make sense to add CommandOpMode overloads to joysticks so users could write `driverController.x(teleop)` instead of `teleop.running.and(driverController.x())`?

# Alternatives

## Not Use `OpModeRobot` for Command-Based Opmodes

An alternative to this proposal is to not use `OpModeRobot` as the base class for command-based robots, and instead have a fully separate mechanism to register opmodes in `CommandRobot`. This means opmodes registered with annotations are not registered automatically. This alternative also uses `CommandRobot`'s periodic function to run the command scheduler, meaning that the command scheduler is run in all modes, so making traditional `LinearOpMode` or `PeriodicOpMode` opmodes would be heavily discouraged as the command scheduler would still run. Teams would also be responsible for manually registering those opmodes, as `CommandRobot` would only provide methods to register command-based opmodes.

The goal of that alternative is to separate command-based and non-command-based opmodes by design, though teams can still mix them in the same project by manually creating registration functions (probably by copying what `OpModeRobot` does). Less importantly, that design's version of the `CommandOpMode` class does not actually implement the `OpMode` interface, and the actual `OpMode` implementations are provided by another class. That design makes relatively little sense. Command-based opmodes should be opmodes, and as such they should be able to be used with the rest of the OpMode framework.

The largest advantage of mixing command-based and non-command-based opmodes is it allows for rapid testing of mechanisms, as teams can make opmodes that test specific behavior without having to write a full subsystem with commands for the mechanism, and then switch to the full subsystem when the mechanism is ready for use. 

Since this proposal only runs the command scheduler when a `CommandOpMode` is selected on the Driver Station, it does not interfere with the behavior of `LinearOpMode` or `PeriodicOpMode` opmodes, so it is okay that this proposal allows command-based and traditional opmodes to be mixed in the same project.
