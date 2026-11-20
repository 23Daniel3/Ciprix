package org.ciprix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.ciprix.CiprixSettings.Mode;
import org.ciprix.util.PreMatchChecklist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CiprixRobotTest {

  private final TestEnvironment env = new TestEnvironment();

  @BeforeEach
  void setUp() {
    HAL.initialize(500, 0);
    env.install();
    CommandScheduler.getInstance().cancelAll();
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().cancelAll();
    CiprixSettings.setLoopPeriodSeconds(0.02);
    CiprixSettings.setTuningMode(false);
    CiprixSettings.setControllerDeadband(0.1);
    CiprixSettings.setDefaultAlliance(Alliance.Blue);
    CiprixSettings.setFieldSize(16.54, 8.07);
    env.uninstall();
  }

  @Test
  void constructorAppliesSettingsRunsConfigureStartsLoggerAndInstallsPreMatchDefaults() {
    AtomicBoolean configureCalled = new AtomicBoolean(false);
    AtomicBoolean loggerStarted = new AtomicBoolean(false);
    CiprixRobot.setLoggerStarterForTesting(() -> loggerStarted.set(true));

    try (CiprixRobot robot =
        new CiprixRobot(Mode.SIM, true, 0.01) {
          @Override
          protected void configure() {
            configureCalled.set(true);
            CiprixSettings.setControllerDeadband(0.08);
            CiprixSettings.setDefaultAlliance(Alliance.Red);
            CiprixSettings.setFieldSize(16.541, 8.069);
          }
        }) {
      assertTrue(configureCalled.get());
      assertTrue(loggerStarted.get());
      assertEquals(Mode.SIM, CiprixSettings.getMode());
      assertTrue(CiprixSettings.isTuningMode());
      assertEquals(0.01, CiprixSettings.getLoopPeriodSeconds(), 1e-9);
      assertEquals(0.01, robot.getPeriod(), 1e-9);
      assertEquals(0.08, CiprixSettings.getControllerDeadband(), 1e-9);
      assertEquals(Alliance.Red, CiprixSettings.getDefaultAlliance());
      assertEquals(16.541, CiprixSettings.getFieldLengthMeters(), 1e-9);
      assertEquals(8.069, CiprixSettings.getFieldWidthMeters(), 1e-9);

      // PreMatchChecklist defaults are registered and evaluate when disabled.
      env.enabled = false;
      env.batteryVolts = 11.0;
      env.cycle();
      assertFalse(PreMatchChecklist.isReady());
      assertEquals(1, PreMatchChecklist.getPending().length);
    }
  }

  @Test
  void lifecycleMethodsRunSchedulerManageAutonomousAndInvokeHooks() {
    AtomicInteger periodicCalls = new AtomicInteger();
    AtomicInteger autoStartCalls = new AtomicInteger();
    AtomicInteger teleopStartCalls = new AtomicInteger();
    AtomicInteger testStartCalls = new AtomicInteger();
    Command autoCommand = Commands.idle().ignoringDisable(true);

    try (CiprixRobot robot =
        new CiprixRobot(Mode.SIM, false) {
          @Override
          protected Command getAutonomousCommand() {
            return autoCommand;
          }

          @Override
          protected void periodic() {
            periodicCalls.incrementAndGet();
          }

          @Override
          protected void autonomousStart() {
            autoStartCalls.incrementAndGet();
          }

          @Override
          protected void teleopStart() {
            teleopStartCalls.incrementAndGet();
          }

          @Override
          protected void testStart() {
            testStartCalls.incrementAndGet();
          }
        }) {
      robot.robotPeriodic();
      assertEquals(1, periodicCalls.get());

      robot.autonomousInit();
      assertEquals(1, autoStartCalls.get());
      assertTrue(CommandScheduler.getInstance().isScheduled(autoCommand));

      robot.teleopInit();
      assertEquals(1, teleopStartCalls.get());
      assertFalse(CommandScheduler.getInstance().isScheduled(autoCommand));

      Command testCommand = Commands.idle().ignoringDisable(true);
      CommandScheduler.getInstance().schedule(testCommand);
      assertTrue(CommandScheduler.getInstance().isScheduled(testCommand));

      robot.testInit();
      assertEquals(1, testStartCalls.get());
      assertFalse(CommandScheduler.getInstance().isScheduled(testCommand));
    }
  }
}
