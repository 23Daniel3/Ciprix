package org.ciprix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChassisSpeedsLimiterTest {

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void pauseTime() {
    SimHooks.pauseTiming();
    SimHooks.restartTiming();
  }

  @AfterEach
  void resumeTime() {
    SimHooks.resumeTiming();
  }

  @Test
  void rampsEveryComponentAtTheConfiguredAcceleration() {
    ChassisSpeedsLimiter limiter = new ChassisSpeedsLimiter(4.0);
    SimHooks.stepTiming(0.25);
    ChassisSpeeds out = limiter.calculate(new ChassisSpeeds(3.0, -3.0, 10.0));
    // 0.25 s at 4 units/s^2 allows a change of 1 per component.
    assertEquals(1.0, out.vxMetersPerSecond, 1e-6);
    assertEquals(-1.0, out.vyMetersPerSecond, 1e-6);
    assertEquals(1.0, out.omegaRadiansPerSecond, 1e-6);
  }

  @Test
  void passesTheTargetThroughWhenTheChangeIsSmall() {
    ChassisSpeedsLimiter limiter = new ChassisSpeedsLimiter(4.0);
    SimHooks.stepTiming(1.0);
    ChassisSpeeds out = limiter.calculate(new ChassisSpeeds(0.5, 0.2, -0.3));
    assertEquals(0.5, out.vxMetersPerSecond, 1e-9);
    assertEquals(0.2, out.vyMetersPerSecond, 1e-9);
    assertEquals(-0.3, out.omegaRadiansPerSecond, 1e-9);
  }

  @Test
  void reachesTheTargetAfterEnoughTime() {
    ChassisSpeedsLimiter limiter = new ChassisSpeedsLimiter(2.0);
    ChassisSpeeds out = new ChassisSpeeds();
    for (int i = 0; i < 100; i++) {
      SimHooks.stepTiming(0.02);
      out = limiter.calculate(new ChassisSpeeds(2.0, 0.0, 0.0));
    }
    assertEquals(2.0, out.vxMetersPerSecond, 1e-9);
  }

  @Test
  void returnsTheLastOutputWhenNoTimePassed() {
    ChassisSpeedsLimiter limiter = new ChassisSpeedsLimiter(4.0);
    SimHooks.stepTiming(0.25);
    ChassisSpeeds first = limiter.calculate(new ChassisSpeeds(3.0, 0.0, 0.0));
    ChassisSpeeds second = limiter.calculate(new ChassisSpeeds(-3.0, 0.0, 0.0));
    assertEquals(first.vxMetersPerSecond, second.vxMetersPerSecond, 1e-9);
  }

  @Test
  void resetStartsFromZeroAgain() {
    ChassisSpeedsLimiter limiter = new ChassisSpeedsLimiter(4.0);
    SimHooks.stepTiming(0.25);
    limiter.calculate(new ChassisSpeeds(3.0, 0.0, 0.0));
    SimHooks.stepTiming(5.0);
    limiter.reset();
    SimHooks.stepTiming(0.25);
    // Without reset the 5 s gap would let it jump straight to 3; after reset it ramps from 0.
    assertEquals(1.0, limiter.calculate(new ChassisSpeeds(3.0, 0.0, 0.0)).vxMetersPerSecond, 1e-6);
  }
}
