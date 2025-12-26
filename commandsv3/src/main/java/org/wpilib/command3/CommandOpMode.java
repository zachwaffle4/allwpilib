// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package org.wpilib.command3;

import java.util.concurrent.atomic.AtomicBoolean;

import org.wpilib.driverstation.DriverStation;
import org.wpilib.hardware.hal.ControlWord;
import org.wpilib.hardware.hal.DriverStationJNI;
import org.wpilib.hardware.hal.HAL;
import org.wpilib.hardware.hal.NotifierJNI;
import org.wpilib.networktables.NetworkTableInstance;
import org.wpilib.opmode.OpMode;
import org.wpilib.smartdashboard.SmartDashboard;
import org.wpilib.system.Watchdog;
import org.wpilib.system.RobotController;
import org.wpilib.util.WPIUtilJNI;

/**
 * An opmode structure for periodic operation built around the command scheduler.
 *
 * <p>This class provides a periodic execution loop that drives a {@link Scheduler}
 * and integrates with the Driver Station lifecycle (selected, enabled/disabled).
 * It offers a few convenience triggers representing the state of the opmode and
 * exposes lifecycle hooks required by the {@link OpMode} interface.
 *
 * <p>Key behaviors and guarantees:
 * <ul>
 *   <li>The scheduler is run while the opmode is active and enabled.</li>
 *   <li>Watchdog epochs are tracked to detect loop overruns and produce helpful
 *       diagnostics when the loop exceeds the configured period.</li>
 *   <li>NetworkTables are flushed after each loop so telemetry is delivered
 *       promptly to the driver station.</li>
 * </ul>
 *
 * <p>Note: This class is specific to the commandsv3 scheduler integration and
 * intentionally documents its own behavior (it does not inherit or re-use docs
 * from other opmode base classes).</p>
 */
public class CommandOpMode implements OpMode {
    /** Default loop period in seconds (20 ms). */
    public static final double kPeriod = 0.02;

    // The C pointer to the notifier object. We don't use it directly, it is
    // just passed to the JNI bindings.
    private int m_notifier = NotifierJNI.createNotifier();
    private final Scheduler m_scheduler;

    private final ControlWord m_word = new ControlWord();
    private final Watchdog m_watchdog;
    private final double m_period;

    private long m_opModeId;
    private final AtomicBoolean m_running = new AtomicBoolean(false);

    /**
     * Trigger that is active while this opmode object is currently selected on
     * the driver station. Useful for wiring commands or actions that should run
     * only when this opmode is the chosen program.
     */
    public final Trigger selected;

    /**
     * Trigger that is active while the Driver Station reports the robot is
     * disabled and this opmode is selected. This is the logical "disabled"
     * condition for building command bindings.
     */
    public final Trigger disabled;

    /**
     * Trigger that is active while the Driver Station reports the robot is
     * enabled and this opmode is selected. This is the logical "enabled"
     * condition for building command bindings.
     */
    public final Trigger enabled;

    /**
     * Constructor.
     *
     * @param scheduler the Scheduler instance this opmode should drive. Typically
     *                  you will pass {@link Scheduler#getDefault()}.
     */
    public CommandOpMode(Scheduler scheduler) {
        this(scheduler, kPeriod);
    }

    /**
     * Constructor that allows configuring the loop period.
     *
     * @param scheduler the Scheduler instance this opmode should drive.
     * @param period loop period in seconds.
     */
    public CommandOpMode(Scheduler scheduler, double period) {
        m_period = period;
        m_watchdog = new Watchdog(m_period, this::printLoopOverrunMessage);
        m_scheduler = scheduler;

        selected = new Trigger(m_scheduler, () -> DriverStation.isOpMode(m_opModeId));
        disabled = new Trigger(m_scheduler, () -> !DriverStation.isEnabled()).and(selected);
        enabled = new Trigger(m_scheduler, m_running::get).and(selected);

        NotifierJNI.setNotifierName(m_notifier, "CommandOpMode");

        HAL.reportUsage("OpMode", "CommandOpMode");
    }

    /**
     * Convenience constructor that uses the default scheduler.
     */
    public CommandOpMode() {
        this(Scheduler.getDefault());
    }

    /**
     * Called periodically while the opmode is selected on the Driver Station
     * and the Driver Station reports the robot is disabled. Implementations may
     * override this to add behavior that should run while disabled (e.g. input
     * polling or diagnostics). The default implementation runs the scheduler.
     */
    @Override
    public void disabledPeriodic() {
        m_scheduler.run();
    }

    // implements OpMode interface
    @Override
    public final void opModeRun(long opModeId) {
        m_opModeId = opModeId;
        m_running.set(true);

        final long periodMicros = (long) (m_period * 1e6);
        long nextTime = RobotController.getFPGATime() + periodMicros;

        while (m_running.get()) {
            // Schedule the notifier for the next loop time and wait for it.
            NotifierJNI.setNotifierAlarm(m_notifier, nextTime, 0, true, true);

            try {
                WPIUtilJNI.waitForObject(m_notifier);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }

            // Update nextTime for the following iteration (and catch up if we're behind).
            long currentTime = RobotController.getFPGATime();

            // Prepare DriverStation state and check for disable/selection changes.
            DriverStation.refreshData();
            DriverStation.refreshControlWordFromCache(m_word);
            m_word.setOpModeId(m_opModeId);
            DriverStationJNI.observeUserProgram(m_word.getNative());

            if (!DriverStation.isEnabled() || DriverStation.getOpModeId() != m_opModeId) {
                m_running.set(false);
                return;
            }

            m_watchdog.reset();
            m_scheduler.run();
            m_watchdog.addEpoch("periodic()");

            SmartDashboard.updateValues();
            m_watchdog.addEpoch("SmartDashboard.updateValues()");

            // if (isSimulation()) {
            //  HAL.simPeriodicBefore();
            //  simulationPeriodic();
            //  HAL.simPeriodicAfter();
            //  m_watchdog.addEpoch("simulationPeriodic()");
            // }

            m_watchdog.disable();

            // Flush NetworkTables
            NetworkTableInstance.getDefault().flushLocal();

            // Warn on loop time overruns
            if (m_watchdog.isExpired()) {
                m_watchdog.printEpochs();
            }

            // Advance nextTime by one period; if we're behind, skip ahead to the next
            // future multiple to avoid rapid-fire loops.
            nextTime += periodMicros;
            if (currentTime >= nextTime) {
                long periodsBehind = (currentTime - nextTime) / periodMicros + 1;
                nextTime += periodsBehind * periodMicros;
            }
        }
    }

    /**
     * Called when the opmode is stopped; destroys the native notifier. This
     * will be called by the runtime when the opmode is no longer needed.
     */
    @Override
    public final void opModeStop() {
        NotifierJNI.destroyNotifier(m_notifier);
        m_notifier = 0;
    }

    /**
     * Called when the opmode object is being closed. If the notifier still
     * exists it is destroyed. This method is safe to call multiple times.
     */
    @Override
    public final void opModeClose() {
        if (m_notifier != 0) {
            NotifierJNI.destroyNotifier(m_notifier);
        }
    }

    /**
     * Prints the list of watchdog epochs that have been recorded for the most
     * recent loop. This can be useful for debugging timing issues.
     */
    @SuppressWarnings("unused")
    public void printWatchdogEpochs() {
        m_watchdog.printEpochs();
    }

    private void printLoopOverrunMessage() {
        DriverStation.reportWarning("Loop time of " + m_period + "s overrun\n", false);
    }

    /**
     * Gets the configured period for the loop in seconds.
     *
     * @return loop period in seconds.
     */
    public double getPeriod() {
        return m_period;
    }
}
