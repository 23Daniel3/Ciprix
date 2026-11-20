package org.ciprix.interfaces.motor;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.Volts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.MotionMagicVoltage;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import edu.wpi.first.hal.HAL;
import java.util.ArrayList;
import java.util.List;
import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.util.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Runs {@link MotorIOTalonFX} against the Phoenix 6 simulated Talon FX: what the robot code asks
 * for must become the right control request and configuration on the device.
 */
class MotorIOTalonFXSimTest {

  private static final CANBus BUS = new CANBus("rio");
  private static int nextId = 1;

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

  private static MotorIOTalonFX motor(MotorConfig config) {
    return new MotorIOTalonFX("SimTalon" + nextId, nextId++, BUS, config);
  }

  private static TalonFXConfiguration readBack(MotorIOTalonFX motor) {
    TalonFXConfiguration applied = new TalonFXConfiguration();
    StatusCode status = motor.getTalon().getConfigurator().refresh(applied, 1.0);
    assertTrue(status.isOK(), "refresh: " + status);
    return applied;
  }

  @Test
  void configurationReachesTheDevice() {
    MotorConfig config = new MotorConfig();
    config.inverted = true;
    config.brakeMode = true;
    config.currentLimit = Amps.of(35);
    config.positionConversionFactor = 0.2; // 5:1
    config.kP[0] = 1.5;
    MotorIOTalonFX motor = motor(config);

    TalonFXConfiguration applied = readBack(motor);
    assertEquals(InvertedValue.Clockwise_Positive, applied.MotorOutput.Inverted);
    assertEquals(NeutralModeValue.Brake, applied.MotorOutput.NeutralMode);
    assertEquals(35.0, applied.CurrentLimits.StatorCurrentLimit, 1e-6);
    assertEquals(35.0, applied.CurrentLimits.SupplyCurrentLimit, 1e-6);
    assertEquals(5.0, applied.Feedback.SensorToMechanismRatio, 1e-6);
    assertEquals(1.5, applied.Slot0.kP, 1e-6);
  }

  @Test
  void openLoopCommandsBecomeVoltageRequests() {
    MotorIOTalonFX motor = motor(new MotorConfig());
    motor.runVoltage(Volts.of(6.0));
    VoltageOut request = assertInstanceOf(VoltageOut.class, motor.getTalon().getAppliedControl());
    assertEquals(6.0, request.Output, 1e-9);

    motor.runPercentOutput(-0.5);
    request = assertInstanceOf(VoltageOut.class, motor.getTalon().getAppliedControl());
    assertEquals(-6.0, request.Output, 1e-9);
  }

  @Test
  void closedLoopCommandsUseTheirSlot() {
    MotorIOTalonFX motor = motor(new MotorConfig());
    motor.runVelocity(RotationsPerSecond.of(10.0), 2);
    VelocityVoltage velocity =
        assertInstanceOf(VelocityVoltage.class, motor.getTalon().getAppliedControl());
    assertEquals(10.0, velocity.Velocity, 1e-9);
    assertEquals(2, velocity.Slot);

    // A Talon has slots 0 to 2: slot 3 falls back to 0, reported once.
    motor.runPosition(Rotations.of(0.5), 3);
    PositionVoltage position =
        assertInstanceOf(PositionVoltage.class, motor.getTalon().getAppliedControl());
    assertEquals(0, position.Slot);
    assertEquals(1, reports.stream().filter(r -> r.contains("slot 3")).count());
  }

  @Test
  void smartPositionUsesMotionMagicOnlyWithAProfile() {
    MotorConfig config = new MotorConfig();
    config.maxMotionMaxVelocity[1] = RotationsPerSecond.of(4.0);
    MotorIOTalonFX motor = motor(config);

    motor.runSmartPosition(Rotations.of(1.0), 1);
    MotionMagicVoltage motion =
        assertInstanceOf(MotionMagicVoltage.class, motor.getTalon().getAppliedControl());
    assertEquals(1, motion.Slot);

    // Slot 0 has no profile: plain position control, with one warning.
    motor.runSmartPosition(Rotations.of(2.0), 0);
    assertInstanceOf(PositionVoltage.class, motor.getTalon().getAppliedControl());
    assertEquals(1, reports.stream().filter(r -> r.contains("smartMotion")).count());
  }

  @Test
  void runtimeChangesAreWrittenToTheDevice() {
    MotorConfig config = new MotorConfig();
    config.currentLimit = Amps.of(40);
    MotorIOTalonFX motor = motor(config);
    motor.setBrakeMode(true);
    motor.setCurrentLimit(Amps.of(20));

    TalonFXConfiguration applied = readBack(motor);
    assertEquals(NeutralModeValue.Brake, applied.MotorOutput.NeutralMode);
    assertEquals(20.0, applied.CurrentLimits.StatorCurrentLimit, 1e-6);
    // No explicit supply limit: it follows the stator limit.
    assertEquals(20.0, applied.CurrentLimits.SupplyCurrentLimit, 1e-6);
  }
}
