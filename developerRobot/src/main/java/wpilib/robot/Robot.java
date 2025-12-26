// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package wpilib.robot;

import org.wpilib.command3.Command;
import org.wpilib.command3.CommandRobot;
import org.wpilib.driverstation.DriverStation;

public class Robot extends CommandRobot {
  /**
   * This function is run when the robot is first started up and should be used for any
   * initialization code.
   */
  public Robot() {
    autonomous("simple auto")
            .enabled.whileTrue(simpleAutonomousCommand());

    DriverStation.publishOpModes();
  }

  public Command simpleAutonomousCommand() {
    return Command.noRequirements().executing(co -> {
      System.out.println("Autonomous!");
    }).named("Simple Autonomous Command");
  }
}
