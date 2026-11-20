package org.ciprix.interfaces.motor;

import static edu.wpi.first.units.Units.Volts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.TalonFXSConfiguration;
import com.ctre.phoenix6.controls.VoltageOut;
import com.ctre.phoenix6.signals.ExternalFeedbackSensorSourceValue;
import com.ctre.phoenix6.signals.MotorArrangementValue;
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

/** {@link MotorIOTalonFXS} against the Phoenix 6 simulated Talon FXS. */
class MotorIOTalonFXSSimTest {

  private static final CANBus BUS = new CANBus("rio");
  private static int nextId = 20;

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

  private static TalonFXSConfiguration readBack(MotorIOTalonFXS motor) {
    TalonFXSConfiguration applied = new TalonFXSConfiguration();
    StatusCode status = motor.getTalon().getConfigurator().refresh(applied, 1.0);
    assertTrue(status.isOK(), "refresh: " + status);
    return applied;
  }

  @Test
  void arrangementAndDataPortSensorReachTheDevice() {
    MotorConfig config = new MotorConfig();
    config.feedbackType = FeedbackSensorType.ALTERNATE;
    config.positionConversionFactor = 0.5;
    MotorIOTalonFXS motor =
        new MotorIOTalonFXS(
            "SimFXS" + nextId, nextId++, BUS, MotorArrangementValue.Minion_JST, config);

    TalonFXSConfiguration applied = readBack(motor);
    assertEquals(MotorArrangementValue.Minion_JST, applied.Commutation.MotorArrangement);
    assertEquals(
        ExternalFeedbackSensorSourceValue.Quadrature,
        applied.ExternalFeedback.ExternalFeedbackSensorSource);
    assertEquals(2.0, applied.ExternalFeedback.SensorToMechanismRatio, 1e-6);
    assertTrue(reports.isEmpty(), reports.toString());
  }

  @Test
  void missingArrangementIsReported() {
    new MotorIOTalonFXS("SimFXS" + nextId, nextId++, BUS, new MotorConfig());
    assertEquals(1, reports.stream().filter(r -> r.contains("motor arrangement")).count());
  }

  @Test
  void percentOutputIsSentAsVoltage() {
    MotorIOTalonFXS motor =
        new MotorIOTalonFXS(
            "SimFXS" + nextId, nextId++, BUS, MotorArrangementValue.Minion_JST, new MotorConfig());
    motor.runPercentOutput(0.25);
    VoltageOut request = assertInstanceOf(VoltageOut.class, motor.getTalon().getAppliedControl());
    assertEquals(3.0, request.Output, 1e-9);
    motor.runVoltage(Volts.of(-4.0));
    request = assertInstanceOf(VoltageOut.class, motor.getTalon().getAppliedControl());
    assertEquals(-4.0, request.Output, 1e-9);
  }
}
