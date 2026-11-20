package org.ciprix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.event.EventLoop;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link SuppliedWaitCommand} and {@link CheesyTrigger}, run on the real command scheduler. */
class CommandUtilsTest {

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    SimHooks.pauseTiming();
    SimHooks.restartTiming();
    DriverStationSim.setEnabled(true);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
    CommandScheduler.getInstance().cancelAll();
    CommandScheduler.getInstance().getActiveButtonLoop().clear();
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().cancelAll();
    CommandScheduler.getInstance().getActiveButtonLoop().clear();
    SimHooks.resumeTiming();
  }

  @Test
  void suppliedWaitReadsTheDurationEveryCycle() {
    double[] seconds = {1.0};
    Command wait = new SuppliedWaitCommand(() -> seconds[0]);
    CommandScheduler.getInstance().schedule(wait);
    CommandScheduler.getInstance().run();
    assertTrue(wait.isScheduled());

    SimHooks.stepTiming(0.5);
    CommandScheduler.getInstance().run();
    assertTrue(wait.isScheduled());

    // Shortening the duration while it runs ends it on the next check.
    seconds[0] = 0.4;
    CommandScheduler.getInstance().run();
    assertFalse(wait.isScheduled());
  }

  @Test
  void suppliedWaitRunsWhenDisabled() {
    assertTrue(new SuppliedWaitCommand(() -> 1.0).runsWhenDisabled());
  }

  @Test
  void cheesyTriggerReschedulesAnInterruptedCommandWhileHeld() {
    EventLoop loop = new EventLoop();
    AtomicBoolean pressed = new AtomicBoolean(false);
    AtomicInteger starts = new AtomicInteger();
    Command held = Commands.startEnd(starts::incrementAndGet, () -> {});
    new CheesyTrigger(loop, pressed::get).whileTrueAlwaysRunning(held);

    pressed.set(true);
    loop.poll();
    CommandScheduler.getInstance().run();
    assertTrue(held.isScheduled());
    assertEquals(1, starts.get());

    // Something else cancels it; while the button is still held it comes back.
    held.cancel();
    loop.poll();
    CommandScheduler.getInstance().run();
    assertTrue(held.isScheduled());
    assertEquals(2, starts.get());

    pressed.set(false);
    loop.poll();
    assertFalse(held.isScheduled());

    // Released: a later cancel is not undone.
    loop.poll();
    assertFalse(held.isScheduled());
  }
}
