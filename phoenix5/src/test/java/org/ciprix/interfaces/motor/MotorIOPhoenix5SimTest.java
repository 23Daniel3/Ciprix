package org.ciprix.interfaces.motor;

import static edu.wpi.first.units.Units.Volts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix.motorcontrol.ControlMode;
import edu.wpi.first.hal.HAL;
import java.util.ArrayList;
import java.util.List;
import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.interfaces.motor.basic.BasicMotorConfig;
import org.ciprix.util.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** {@link MotorIOTalonSRX} and {@link MotorIOVictorSPX} on the Phoenix 5 desktop simulation. */
class MotorIOPhoenix5SimTest {

  private static int nextId = 40;

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

  @Test
  void talonSrxOpenLoopCommandsAreSentAsPercentOutput() {
    MotorConfig config = new MotorConfig();
    config.inverted = true;
    MotorIOTalonSRX motor = new MotorIOTalonSRX("SimSRX" + nextId, nextId++, config);
    assertTrue(motor.getTalon().getInverted());

    motor.runVoltage(Volts.of(6.0));
    assertEquals(ControlMode.PercentOutput, motor.getTalon().getControlMode());
    motor.runPercentOutput(0.25);
    assertEquals(ControlMode.PercentOutput, motor.getTalon().getControlMode());
    assertTrue(reports.isEmpty(), reports.toString());
  }

  @Test
  void talonSrxClosedLoopModes() {
    MotorIOTalonSRX motor = new MotorIOTalonSRX("SimSRX" + nextId, nextId++, new MotorConfig());
    motor.runVelocity(edu.wpi.first.units.Units.RotationsPerSecond.of(5.0), 0);
    assertEquals(ControlMode.Velocity, motor.getTalon().getControlMode());
    motor.runPosition(edu.wpi.first.units.Units.Rotations.of(1.0), 0);
    assertEquals(ControlMode.Position, motor.getTalon().getControlMode());
  }

  @Test
  void talonSrxReportsFeedforwardTermsItDoesNotHave() {
    MotorConfig config = new MotorConfig();
    config.kS[0] = 0.3;
    new MotorIOTalonSRX("SimSRX" + nextId, nextId++, config);
    assertEquals(1, reports.stream().filter(r -> r.contains("no kS, kA or kG")).count());
  }

  @Test
  void victorSpxOpenLoopCommands() {
    BasicMotorConfig config = new BasicMotorConfig().inverted(true);
    MotorIOVictorSPX motor = new MotorIOVictorSPX("SimSPX" + nextId, nextId++, config);
    assertTrue(motor.getVictor().getInverted());

    motor.runPercentOutput(-0.5);
    assertEquals(ControlMode.PercentOutput, motor.getVictor().getControlMode());
    motor.stop();
    assertTrue(reports.isEmpty(), reports.toString());
  }

  @Test
  void victorSpxFollowerMode() {
    BasicMotorConfig config = new BasicMotorConfig().withMotorLeader(7);
    MotorIOVictorSPX motor = new MotorIOVictorSPX("SimSPX" + nextId, nextId++, config);
    assertEquals(ControlMode.Follower, motor.getVictor().getControlMode());
  }
}
