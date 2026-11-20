package org.ciprix.interfaces.motor.pwm;

import static edu.wpi.first.units.Units.Volts;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.PWMSim;
import java.util.List;
import java.util.function.BiFunction;
import org.ciprix.TestEnvironment;
import org.ciprix.interfaces.motor.basic.BasicMotorConfig;
import org.ciprix.interfaces.motor.basic.BasicMotorIO.BasicMotorIOInputs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Every PWM motor type, driving the simulated PWM port. */
class MotorIOPWMTest {

  private final TestEnvironment env = new TestEnvironment();
  private static int nextPort = 0;

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

  private interface Factory extends BiFunction<Integer, BasicMotorConfig, MotorIOPWM> {}

  private static final List<Factory> ALL_TYPES =
      List.of(
          (port, config) -> new MotorIOPWMSparkMax("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMSparkFlex("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMTalon("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMTalonFXS("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMTalonSRX("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMVictorSP("PWM" + port, port, config),
          (port, config) -> new MotorIOPWMVictorSPX("PWM" + port, port, config));

  private static void close(MotorIOPWM motor) throws Exception {
    ((AutoCloseable) motor.motor).close();
  }

  @Test
  void everyTypeDrivesItsPort() throws Exception {
    for (Factory factory : ALL_TYPES) {
      int port = nextPort++;
      MotorIOPWM motor = factory.apply(port, new BasicMotorConfig());
      PWMSim sim = new PWMSim(port);

      motor.runPercentOutput(0.5);
      assertEquals(0.5, sim.getSpeed(), 0.02, "port " + port);

      motor.runVoltage(Volts.of(-6.0));
      assertEquals(-0.5, sim.getSpeed(), 0.02, "port " + port);

      motor.stop();
      assertEquals(0.0, sim.getSpeed(), 0.02, "port " + port);
      close(motor);
    }
  }

  @Test
  void invertedMotorDrivesThePortBackward() throws Exception {
    int port = nextPort++;
    MotorIOPWM motor =
        new MotorIOPWMSparkMax("PWM" + port, port, new BasicMotorConfig().inverted(true));
    PWMSim sim = new PWMSim(port);
    motor.runPercentOutput(0.5);
    assertEquals(-0.5, sim.getSpeed(), 0.02);

    // The inputs show what the robot code asked for, not the wire.
    BasicMotorIOInputs inputs = new BasicMotorIOInputs();
    motor.updateInputs(inputs);
    assertEquals(0.5, inputs.percentOutput, 0.02);
    assertTrue(inputs.isConnected);
    close(motor);
  }

  @Test
  void commandIsSentEveryCycle() throws Exception {
    // PWM has no memory of its own: the same command is written again on every update.
    int port = nextPort++;
    MotorIOPWM motor = new MotorIOPWMVictorSP("PWM" + port, port, new BasicMotorConfig());
    PWMSim sim = new PWMSim(port);
    motor.runPercentOutput(0.3);
    sim.setSpeed(0.0); // something else overwrote the port
    motor.updateInputs(new BasicMotorIOInputs());
    assertEquals(0.3, sim.getSpeed(), 0.02);
    close(motor);
  }
}
