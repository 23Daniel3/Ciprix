package org.ciprix.interfaces.encoder;

import static edu.wpi.first.units.Units.Rotations;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.hal.HAL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@link EncoderIOCANcoder} against the Phoenix 6 simulated CANcoder. */
class EncoderIOCANcoderSimTest {

  private static int nextId = 40;

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  private static CANcoderConfiguration readBack(EncoderIOCANcoder encoder) {
    CANcoderConfiguration applied = new CANcoderConfiguration();
    // Make sure the simulated device exists, then give it time to answer.
    encoder.getCANcoder().getSimState();
    StatusCode status = StatusCode.StatusCodeNotInitialized;
    for (int i = 0; i < 30 && !status.isOK(); i++) {
      status = encoder.getCANcoder().getConfigurator().refresh(applied, 0.25);
    }
    assertTrue(status.isOK(), "refresh: " + status);
    return applied;
  }

  /** Reads until the simulated value shows up (the sim publishes it on its next frame). */
  private static EncoderIO.EncoderIOInputs readUntil(
      EncoderIOCANcoder encoder, double expectedRotations) throws InterruptedException {
    EncoderIO.EncoderIOInputs inputs = new EncoderIO.EncoderIOInputs();
    for (int i = 0; i < 100; i++) {
      encoder.updateInputs(inputs);
      if (Math.abs(inputs.position.in(Rotations) - expectedRotations) < 1e-3) {
        return inputs;
      }
      Thread.sleep(10);
    }
    return inputs;
  }

  @Test
  void configurationReachesTheDevice() {
    EncoderIOCANcoder normal =
        new EncoderIOCANcoder("SimCANcoder" + nextId, nextId++, new EncoderConfig());
    CANcoderConfiguration applied = readBack(normal);
    assertEquals(
        SensorDirectionValue.CounterClockwise_Positive, applied.MagnetSensor.SensorDirection);
    assertEquals(0.5, applied.MagnetSensor.AbsoluteSensorDiscontinuityPoint, 1e-9);

    EncoderIOCANcoder inverted =
        new EncoderIOCANcoder(
            "SimCANcoder" + nextId,
            nextId++,
            new CANBus("rio"),
            new EncoderConfig().inverted(true));
    assertEquals(
        SensorDirectionValue.Clockwise_Positive, readBack(inverted).MagnetSensor.SensorDirection);
    assertEquals(nextId - 1, inverted.getDeviceId());
  }

  @Test
  void hardwareZeroMovesTheReportedPosition() throws InterruptedException {
    EncoderIOCANcoder encoder =
        new EncoderIOCANcoder("SimCANcoder" + nextId, nextId++, new EncoderConfig());
    encoder.setPosition(Rotations.of(0.3));
    EncoderIO.EncoderIOInputs inputs = readUntil(encoder, 0.3);
    assertEquals(0.3, inputs.position.in(Rotations), 1e-3);
    assertTrue(inputs.isConnected);
  }
}
