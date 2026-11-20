package org.ciprix.interfaces.encoder;

import static edu.wpi.first.units.Units.Degrees;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.units.measure.Angle;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import org.ciprix.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncoderTest {

  private final TestEnvironment env = new TestEnvironment();

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    env.install();
    SimHooks.pauseTiming();
    SimHooks.restartTiming();
  }

  @AfterEach
  void tearDown() {
    SimHooks.resumeTiming();
    env.uninstall();
  }

  /** An encoder whose raw reading is set by the test. */
  private static final class FakeEncoder extends EncoderBase {
    double rawRotations = 0.0;
    Double nativeVelocityRps = null;
    boolean connected = true;
    String[] faults = {};

    FakeEncoder(EncoderConfig config) {
      super("TestEncoder" + System.nanoTime(), config);
    }

    @Override
    protected RawSample readRaw() {
      Angle position = Rotations.of(rawRotations);
      if (nativeVelocityRps == null) {
        return RawSample.withoutVelocity(position, connected, faults);
      }
      AngularVelocity velocity = RotationsPerSecond.of(nativeVelocityRps);
      return new RawSample(position, velocity, true, connected, faults);
    }
  }

  private static EncoderIO.EncoderIOInputs read(EncoderIO io) {
    EncoderIO.EncoderIOInputs inputs = new EncoderIO.EncoderIOInputs();
    io.updateInputs(inputs);
    return inputs;
  }

  @Test
  void configBuilderSetsEveryField() {
    EncoderConfig config =
        new EncoderConfig().conversionFactors(2.0, 3.0).inverted(true).offsetRotations(0.25);
    config.averageDepth(4);
    assertEquals(2.0, config.positionConversionFactor);
    assertEquals(3.0, config.velocityConversionFactor);
    assertTrue(config.inverted);
    assertEquals(0.25, config.offset.in(Rotations), 1e-12);
    assertEquals(4, config.samplesToAverage);
    assertEquals(90.0, new EncoderConfig().offset(Degrees.of(90)).offset.in(Degrees), 1e-9);
  }

  @Test
  void positionSubtractsTheOffsetThenConverts() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig().offsetRotations(0.25));
    encoder.config.positionConversionFactor = 10.0;
    encoder.rawRotations = 0.75;
    EncoderIO.EncoderIOInputs inputs = read(encoder);
    assertEquals(0.75, inputs.absolutePosition.in(Rotations), 1e-12);
    assertEquals(5.0, inputs.position.in(Rotations), 1e-12);
  }

  @Test
  void setPositionMakesTheCurrentReadingThatValue() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig().conversionFactors(2.0, 1.0));
    encoder.rawRotations = 3.0;
    encoder.setPosition(Rotations.of(1.0));
    assertEquals(1.0, read(encoder).position.in(Rotations), 1e-12);
    encoder.rawRotations = 3.5; // half a raw rotation at 2x
    assertEquals(2.0, read(encoder).position.in(Rotations), 1e-12);
  }

  @Test
  void velocityIsDerivedFromThePositionWhenTheSensorHasNone() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig().conversionFactors(1.0, 60.0));
    read(encoder); // first sample: no velocity yet
    SimHooks.stepTiming(0.5);
    encoder.rawRotations = 1.0;
    // 1 rotation in 0.5 s = 2 rot/s, times the 60x velocity factor.
    assertEquals(120.0, read(encoder).velocity.in(RotationsPerSecond), 1e-6);
  }

  @Test
  void derivedVelocityIsAveragedOverTheConfiguredSamples() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig().averageDepth(2));
    read(encoder);
    SimHooks.stepTiming(1.0);
    encoder.rawRotations = 2.0;
    // Samples so far: 0 (first read) and 2 rot/s.
    assertEquals(1.0, read(encoder).velocity.in(RotationsPerSecond), 1e-6);
  }

  @Test
  void nativeVelocityIsUsedAndConverted() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig().conversionFactors(1.0, 0.5));
    encoder.nativeVelocityRps = 8.0;
    assertEquals(4.0, read(encoder).velocity.in(RotationsPerSecond), 1e-12);
  }

  @Test
  void shortDropoutIsDebouncedButARealDisconnectIsReported() {
    FakeEncoder encoder = new FakeEncoder(new EncoderConfig());
    encoder.faults = new String[] {"Low signal"};
    assertTrue(read(encoder).isConnected);

    encoder.connected = false;
    SimHooks.stepTiming(0.2);
    assertTrue(read(encoder).isConnected, "a 0.2 s glitch is not a disconnect");

    SimHooks.stepTiming(0.5);
    EncoderIO.EncoderIOInputs inputs = read(encoder);
    assertFalse(inputs.isConnected);
    assertArrayEquals(new String[] {"Low signal"}, inputs.activeFaults);
  }

  @Test
  void simpleEncoderWrapsTheIo() {
    FakeEncoder io = new FakeEncoder(new EncoderConfig());
    SimpleEncoder encoder = new SimpleEncoder(io);
    io.rawRotations = 0.5;
    io.nativeVelocityRps = 2.0;
    encoder.update();
    assertEquals(0.5, encoder.getRotations(), 1e-12);
    assertEquals(180.0, encoder.getDegrees(), 1e-9);
    assertEquals(120.0, encoder.getVelocityRPM(), 1e-9);
    assertTrue(encoder.isConnected());

    encoder.zero();
    encoder.update();
    assertEquals(0.0, encoder.getRotations(), 1e-12);

    encoder.setDegrees(90);
    encoder.update();
    assertEquals(0.25, encoder.getRotations(), 1e-12);

    encoder.setRotations(2.0);
    encoder.update();
    assertEquals(2.0, encoder.getRotations(), 1e-12);
  }

  @Test
  void noneEncoderReadsZeroAndIgnoresCommands() {
    SimpleEncoder encoder = new SimpleEncoder(new EncoderIONone());
    encoder.setRotations(5.0);
    encoder.update();
    assertEquals(0.0, encoder.getRotations(), 1e-12);
    assertFalse(encoder.isConnected());
  }
}
