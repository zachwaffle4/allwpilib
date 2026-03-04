// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.wpilib.command3;

import java.util.function.BooleanSupplier;
import org.wpilib.driverstation.DriverStation;
import org.wpilib.event.EventLoop;

public class Context implements BooleanSupplier {
  // always true
  public static final Context all = new Context(() -> true);

  // true when the given robot mode is selected, regardless of enabled/disabled state
  public static final Context allAuto = all.and(DriverStation::isAutonomous);
  public static final Context allTeleop = all.and(DriverStation::isTeleop);
  public static final Context allTest = all.and(DriverStation::isTest);

  private final BooleanSupplier m_condition;

  // used by CommandOpMode and the internals
  protected Context(BooleanSupplier condition) {
    m_condition = condition;
  }

  // creates a Trigger with the given base condition
  public Trigger trigger(BooleanSupplier condition) {
    return new Trigger(Scheduler.getDefault(), this, Conditions.and(this, condition));
  }

  public Trigger trigger(EventLoop loop, BooleanSupplier condition) {
    return new Trigger(Scheduler.getDefault(), this, loop, Conditions.and(this, condition));
  }

  public Context and(BooleanSupplier condition) {
    return new Context(Conditions.and(this, condition));
  }

  public Context or(Context condition) {
    return new Context(Conditions.or(this, condition));
  }

  @Override
  public boolean getAsBoolean() {
    return m_condition.getAsBoolean();
  }
}
