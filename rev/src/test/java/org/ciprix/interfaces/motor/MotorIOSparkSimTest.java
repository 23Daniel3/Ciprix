package org.ciprix.interfaces.motor;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkFlex;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkBaseConfigAccessor;
import edu.wpi.first.hal.HAL;
import java.util.ArrayList;
import java.util.List;
import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.interfaces.motor.advanced.MotorConfig.FeedbackSensorType;
import org.ciprix.util.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link MotorIOSparkMax} and {@link MotorIOSparkFlex} against the REVLib simulated SPARK: the
 * Ciprix configuration must end up in the controller's parameters.
 */
class MotorIOSparkSimTest {

  private static int nextId = 30;

  private final List<String> reports = new ArrayList<>();

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void captureReports() {
    reports.clear();
    Report.setSink((isError, message, printTrace) -> reports.add(message));
  }

  @AfterEach
  void restoreReports() {
    Report.resetSink();
  }

  private static MotorConfig armConfig() {
    MotorConfig config = new MotorConfig();
    config.inverted = true;
    config.brakeMode = true;
    config.currentLimit = Amps.of(35.4);
    config.positionConversionFactor = 0.25;
    config.velocityConversionFactor = 0.25;
    config.kP[1] = 0.8;
    config.maxMotionMaxVelocity[1] = RotationsPerSecond.of(2.0);
    config.softLimitEnabled = true;
    config.minPosition = Rotations.of(-0.1);
    config.maxPosition = Rotations.of(0.4);
    return config;
  }

  private static void assertConfigured(SparkMax spark) {
    assertConfigured(spark.configAccessor);
  }

  private static void assertConfigured(SparkFlex spark) {
    assertConfigured(spark.configAccessor);
  }

  private static void assertConfigured(SparkBaseConfigAccessor accessor) {
    assertTrue(accessor.getInverted());
    assertEquals(IdleMode.kBrake, accessor.getIdleMode());
    assertEquals(35, accessor.getSmartCurrentLimit());
    assertEquals(0.25, accessor.encoder.getPositionConversionFactor(), 1e-6);
    assertEquals(0.8, accessor.closedLoop.getP(ClosedLoopSlot.kSlot1), 1e-6);
    // Cruise velocity goes to the SPARK in RPM of the mechanism.
    assertEquals(
        120.0, accessor.closedLoop.maxMotion.getCruiseVelocity(ClosedLoopSlot.kSlot1), 1e-3);
    assertTrue(accessor.softLimit.getForwardSoftLimitEnabled());
    assertEquals(0.4, accessor.softLimit.getForwardSoftLimit(), 1e-6);
    assertEquals(-0.1, accessor.softLimit.getReverseSoftLimit(), 1e-6);
  }

  @Test
  void sparkMaxReceivesTheConfiguration() {
    MotorIOSparkMax motor = new MotorIOSparkMax("SimMax" + nextId, nextId++, armConfig());
    assertConfigured(motor.motor);
    assertTrue(reports.isEmpty(), reports.toString());
  }

  @Test
  void sparkFlexReceivesTheConfiguration() {
    MotorIOSparkFlex motor = new MotorIOSparkFlex("SimFlex" + nextId, nextId++, armConfig());
    assertConfigured((SparkFlex) motor.motor);
    assertTrue(reports.isEmpty(), reports.toString());
  }

  @Test
  void runtimeChangesReachTheController() throws InterruptedException {
    MotorConfig config = new MotorConfig();
    config.currentLimit = Amps.of(40);
    MotorIOSparkMax motor = new MotorIOSparkMax("SimMax" + nextId, nextId++, config);
    motor.setBrakeMode(true);
    motor.setCurrentLimit(Amps.of(20));
    // Runtime changes are sent asynchronously.
    for (int i = 0;
        i < 100 && ((SparkMax) motor.motor).configAccessor.getSmartCurrentLimit() != 20;
        i++) {
      Thread.sleep(10);
    }
    assertEquals(20, ((SparkMax) motor.motor).configAccessor.getSmartCurrentLimit());
    assertEquals(IdleMode.kBrake, ((SparkMax) motor.motor).configAccessor.getIdleMode());
  }

  @Test
  void canCoderFeedbackIsNotAvailableOnASpark() {
    MotorConfig config = new MotorConfig();
    config.feedbackType = FeedbackSensorType.REMOTE_CANCODER;
    new MotorIOSparkMax("SimMax" + nextId, nextId++, config);
    assertEquals(1, reports.stream().filter(r -> r.contains("does not exist on a SPARK")).count());
  }

  @Test
  void coastIsTheDefault() {
    MotorIOSparkMax motor = new MotorIOSparkMax("SimMax" + nextId, nextId++, new MotorConfig());
    assertEquals(IdleMode.kCoast, ((SparkMax) motor.motor).configAccessor.getIdleMode());
    assertFalse(((SparkMax) motor.motor).configAccessor.getInverted());
  }
}
