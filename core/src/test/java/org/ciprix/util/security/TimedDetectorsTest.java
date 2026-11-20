package org.ciprix.util.security;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.MetersPerSecondPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecondPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Slew limiters and current/stall detectors depend on the clock, so these tests run on the
 * simulated HAL with paused time and step it by hand.
 */
class TimedDetectorsTest {

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void pauseTime() {
    SimHooks.pauseTiming();
    SimHooks.restartTiming();
    setEnabled(true);
  }

  @AfterEach
  void resumeTime() {
    SimHooks.resumeTiming();
  }

  private static void setEnabled(boolean enabled) {
    DriverStationSim.setEnabled(enabled);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
  }

  // ---------------------------------------------------------------------------------------------
  // Slew rate limiters
  // ---------------------------------------------------------------------------------------------

  @Test
  void linearLimiterCapsAccelerationPerSecond() {
    DynamicSlewRateLimiter limiter = new DynamicSlewRateLimiter(MetersPerSecondPerSecond.of(2.0));
    SimHooks.stepTiming(0.5);
    // Asked for 10 m/s, but 0.5 s at 2 m/s^2 only allows 1 m/s.
    assertEquals(1.0, limiter.calculate(MetersPerSecond.of(10.0)).in(MetersPerSecond), 1e-6);
    SimHooks.stepTiming(0.5);
    assertEquals(2.0, limiter.calculate(MetersPerSecond.of(10.0)).in(MetersPerSecond), 1e-6);
  }

  @Test
  void linearLimiterUsesSeparateDeceleration() {
    DynamicSlewRateLimiter limiter =
        new DynamicSlewRateLimiter(
            MetersPerSecondPerSecond.of(1.0),
            MetersPerSecondPerSecond.of(-4.0), // sign is ignored
            MetersPerSecond.of(3.0));
    SimHooks.stepTiming(0.5);
    assertEquals(1.0, limiter.calculate(MetersPerSecond.of(0.0)).in(MetersPerSecond), 1e-6);
  }

  @Test
  void linearLimiterReachesASmallTargetExactly() {
    DynamicSlewRateLimiter limiter = new DynamicSlewRateLimiter(MetersPerSecondPerSecond.of(5.0));
    SimHooks.stepTiming(1.0);
    assertEquals(0.3, limiter.calculate(MetersPerSecond.of(0.3)).in(MetersPerSecond), 1e-6);
    assertEquals(0.3, limiter.getLastValue().in(MetersPerSecond), 1e-6);
  }

  @Test
  void linearLimiterHoldsWhenNoTimePassed() {
    DynamicSlewRateLimiter limiter = new DynamicSlewRateLimiter(MetersPerSecondPerSecond.of(5.0));
    assertEquals(0.0, limiter.calculate(MetersPerSecond.of(4.0)).in(MetersPerSecond), 1e-9);
  }

  @Test
  void linearLimiterLimitsCanChangeAndResetJumps() {
    DynamicSlewRateLimiter limiter = new DynamicSlewRateLimiter(MetersPerSecondPerSecond.of(1.0));
    limiter.setAccelerationLimit(MetersPerSecondPerSecond.of(10.0));
    SimHooks.stepTiming(0.1);
    assertEquals(1.0, limiter.calculate(MetersPerSecond.of(5.0)).in(MetersPerSecond), 1e-6);
    limiter.setAccelerationLimits(
        MetersPerSecondPerSecond.of(10.0), MetersPerSecondPerSecond.of(2.0));
    SimHooks.stepTiming(0.1);
    assertEquals(0.8, limiter.calculate(MetersPerSecond.of(-5.0)).in(MetersPerSecond), 1e-6);
    limiter.reset(MetersPerSecond.of(-3.0));
    assertEquals(-3.0, limiter.getLastValue().in(MetersPerSecond), 1e-9);
  }

  @Test
  void angularLimiterCapsAndUsesSeparateDeceleration() {
    AngularDynamicSlewRateLimiter limiter =
        new AngularDynamicSlewRateLimiter(
            RadiansPerSecondPerSecond.of(4.0),
            RadiansPerSecondPerSecond.of(8.0),
            RadiansPerSecond.of(0.0));
    SimHooks.stepTiming(0.25);
    assertEquals(1.0, limiter.calculate(RadiansPerSecond.of(10.0)).in(RadiansPerSecond), 1e-6);
    SimHooks.stepTiming(0.25);
    assertEquals(-1.0, limiter.calculate(RadiansPerSecond.of(-10.0)).in(RadiansPerSecond), 1e-6);
    limiter.setAccelerationLimit(RadiansPerSecondPerSecond.of(100.0));
    SimHooks.stepTiming(0.25);
    assertEquals(3.0, limiter.calculate(RadiansPerSecond.of(3.0)).in(RadiansPerSecond), 1e-6);
    limiter.reset(RadiansPerSecond.of(0.5));
    assertEquals(0.5, limiter.getLastValue().in(RadiansPerSecond), 1e-9);
  }

  // ---------------------------------------------------------------------------------------------
  // Current spike and locked motor detectors
  // ---------------------------------------------------------------------------------------------

  @Test
  void currentSpikeNeedsToLastLongEnough() {
    CurrentSpikeDetector detector = new CurrentSpikeDetector(40.0, 0.3);
    assertFalse(detector.update(60.0));
    SimHooks.stepTiming(0.2);
    assertFalse(detector.update(60.0));
    SimHooks.stepTiming(0.2);
    assertTrue(detector.update(60.0));
    assertTrue(detector.getAsBoolean());
  }

  @Test
  void currentSpikeResetsWhenCurrentDrops() {
    CurrentSpikeDetector detector = new CurrentSpikeDetector(40.0, 0.3);
    detector.update(60.0);
    SimHooks.stepTiming(0.2);
    assertFalse(detector.update(10.0));
    detector.update(60.0);
    SimHooks.stepTiming(0.2);
    assertFalse(detector.update(60.0), "the timer restarted when the current dropped");
    assertFalse(detector.getAsBoolean());
  }

  @Test
  void currentSpikeTriggerIsCached() {
    CurrentSpikeDetector detector = new CurrentSpikeDetector(40.0, 0.3);
    assertTrue(detector.asTrigger() == detector.asTrigger());
  }

  @Test
  void lockedMotorNeedsCommandAndNoMotionForLongEnough() {
    LockedMotorDetector detector = new LockedMotorDetector(0.5, 1.0);
    assertFalse(detector.update(true, 0.1));
    SimHooks.stepTiming(0.6);
    assertTrue(detector.update(true, 0.1));
    assertTrue(detector.getAsBoolean());
  }

  @Test
  void lockedMotorIgnoresAMotorThatMovesOrIsNotCommanded() {
    LockedMotorDetector detector = new LockedMotorDetector(0.5, 1.0);
    detector.update(true, 5.0);
    SimHooks.stepTiming(0.6);
    assertFalse(detector.update(true, -5.0), "moving backwards still counts as moving");
    detector.update(false, 0.0);
    SimHooks.stepTiming(0.6);
    assertFalse(detector.update(false, 0.0), "a motor that should be still is not locked");
  }

  @Test
  void lockedMotorNeverFiresWhileDisabled() {
    LockedMotorDetector detector = new LockedMotorDetector(0.5, 1.0);
    detector.update(true, 0.0);
    SimHooks.stepTiming(0.6);
    setEnabled(false);
    assertFalse(detector.update(true, 0.0));
    assertFalse(detector.getAsBoolean());
    assertTrue(detector.asTrigger() == detector.asTrigger());
  }
}
