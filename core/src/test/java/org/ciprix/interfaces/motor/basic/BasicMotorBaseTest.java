package org.ciprix.interfaces.motor.basic;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Celsius;
import static edu.wpi.first.units.Units.Volts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import java.util.ArrayList;
import java.util.List;
import org.ciprix.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The open-loop motor layer that keeps the robot code's intent: only changes reach the hardware,
 * the last command is re-sent periodically, and the output range and health are applied in one
 * place for every vendor.
 */
class BasicMotorBaseTest {

  private final TestEnvironment env = new TestEnvironment();

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    env.install();
  }

  @AfterEach
  void tearDown() {
    env.uninstall();
  }

  /** Records every call that would reach the motor controller. */
  private static final class FakeMotor extends BasicMotorBase {
    final List<String> calls = new ArrayList<>();
    boolean connected = true;
    double temperature = 30.0;

    FakeMotor(BasicMotorConfig config) {
      super("FakeMotor" + System.nanoTime(), config);
    }

    @Override
    protected void applyVoltage(double volts) {
      calls.add("V " + volts);
    }

    @Override
    protected void applyPercentOutput(double percent) {
      calls.add("P " + percent);
    }

    @Override
    protected void applyStop() {
      calls.add("stop");
    }

    @Override
    protected void applyBrakeMode(boolean enabled) {
      calls.add("brake " + enabled);
    }

    @Override
    protected void applyCurrentLimit(double amps) {
      calls.add("limit " + amps);
    }

    @Override
    protected void updateHardwareInputs(BasicMotorIOInputs inputs) {
      inputs.isConnected = connected;
      inputs.temperature = Celsius.of(temperature);
    }

    BasicMotorIOInputs read() {
      BasicMotorIOInputs inputs = new BasicMotorIOInputs();
      updateInputs(inputs);
      return inputs;
    }
  }

  @Test
  void motorThatWasNeverCommandedIsNeverTouched() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig());
    for (int i = 0; i < 100; i++) {
      env.now += 0.02;
      motor.read();
    }
    assertTrue(motor.calls.isEmpty());
  }

  @Test
  void repeatedCommandReachesTheHardwareOnce() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig());
    motor.runPercentOutput(0.5);
    motor.runPercentOutput(0.5);
    motor.runPercentOutput(0.5);
    assertEquals(List.of("P 0.5"), motor.calls);

    motor.runPercentOutput(0.25);
    motor.stop();
    assertEquals(List.of("P 0.5", "P 0.25", "stop"), motor.calls);
  }

  @Test
  void unchangedCommandIsSentAgainEveryHalfSecond() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig());
    motor.runVoltage(Volts.of(6.0));
    for (int i = 0; i < 24; i++) { // 0.48 s
      env.now += 0.02;
      motor.read();
    }
    assertEquals(1, motor.calls.size());
    env.now += 0.02;
    motor.read();
    assertEquals(List.of("V 6.0", "V 6.0"), motor.calls);
  }

  @Test
  void outputRangeScalesBothDirections() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig().outputRange(-0.5, 0.8));
    motor.runPercentOutput(1.0);
    motor.runPercentOutput(-1.0);
    motor.runPercentOutput(5.0); // clamped to 1 first
    assertEquals(List.of("P 0.8", "P -0.5"), motor.calls);
  }

  @Test
  void voltageIsScaledAndNeverExceedsTheNominalVoltage() {
    FakeMotor motor =
        new FakeMotor(new BasicMotorConfig().outputRange(-1.0, 0.5).nominalVoltage(Volts.of(10)));
    motor.runVoltage(Volts.of(8.0));
    motor.runVoltage(Volts.of(-30.0));
    assertEquals(List.of("V 4.0", "V -10.0"), motor.calls);
  }

  @Test
  void brakeModeAndCurrentLimitAreOnlyWrittenWhenTheyChange() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig().coastMode().currentLimit(Amps.of(40)));
    motor.setBrakeMode(false);
    motor.setCurrentLimit(Amps.of(40));
    assertTrue(motor.calls.isEmpty());

    motor.setBrakeMode(true);
    motor.setCurrentLimit(Amps.of(-30)); // the sign is ignored
    assertEquals(List.of("brake true", "limit 30.0"), motor.calls);
  }

  @Test
  void controllerForwardsToTheMotor() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig());
    BasicMotorController controller = motor.getMotorController();
    controller.runPercentOutput(0.3);
    controller.runVoltage(Volts.of(2.0));
    controller.setBrakeMode(true);
    controller.setCurrentLimit(Amps.of(20));
    controller.stop();
    assertEquals(List.of("P 0.3", "V 2.0", "brake true", "limit 20.0", "stop"), motor.calls);
  }

  @Test
  void disconnectedMotorIsAProblem() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig());
    assertFalse(motor.read().hasProblem);
    motor.connected = false;
    BasicMotorIOInputs inputs = motor.read();
    assertTrue(inputs.hasProblem);
    assertEquals("Disconnected", inputs.problem);
    assertEquals(inputs, motor.getMotorIOInputs());
  }

  @Test
  void temperatureWarningHasHysteresis() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig().temperatureWarning(Celsius.of(80)));
    motor.temperature = 81;
    assertEquals("Over temperature", motor.read().problem);
    motor.temperature = 77; // still within 5 degrees of the limit
    assertTrue(motor.read().hasProblem);
    motor.temperature = 74;
    assertFalse(motor.read().hasProblem);
  }

  @Test
  void zeroTemperatureWarningDisablesTheCheck() {
    FakeMotor motor = new FakeMotor(new BasicMotorConfig().temperatureWarning(Celsius.of(0)));
    motor.temperature = 150;
    assertFalse(motor.read().hasProblem);
  }
}
