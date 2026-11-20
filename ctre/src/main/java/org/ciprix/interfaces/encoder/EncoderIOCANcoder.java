package org.ciprix.interfaces.encoder;

import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.units.measure.Angle;

/**
 * {@link org.ciprix.interfaces.encoder.EncoderIO} for a CTRE CANcoder (Phoenix 6).
 *
 * <p>Reports native position and velocity straight off the CAN bus — no numeric differentiation.
 * Inversion is pushed into the hardware sensor-direction config so the sensor itself reports
 * correctly signed values from boot, before a subsystem's first periodic loop even runs. The offset
 * stays purely in {@link EncoderBase}'s software layer (not also baked into the hardware {@code
 * MagnetOffset}) so there's exactly one place it's applied.
 */
public class EncoderIOCANcoder extends EncoderBase {

  private final CANcoder encoder;

  public EncoderIOCANcoder(String name, int id, EncoderConfig config) {
    this(name, id, new CANBus("rio"), config);
  }

  /**
   * @param canBus the bus the CANcoder is on (a CANivore, for example)
   */
  public EncoderIOCANcoder(String name, int id, CANBus canBus, EncoderConfig config) {
    super(name, config);

    this.encoder = new CANcoder(id, canBus);

    CANcoderConfiguration hwConfig = new CANcoderConfiguration();
    hwConfig.MagnetSensor.SensorDirection =
        config.inverted
            ? SensorDirectionValue.Clockwise_Positive
            : SensorDirectionValue.CounterClockwise_Positive;
    hwConfig.MagnetSensor.AbsoluteSensorDiscontinuityPoint = 0.5; // signed [-0.5, 0.5)

    encoder.getConfigurator().apply(hwConfig);
  }

  @Override
  protected RawSample readRaw() {
    var positionSignal = encoder.getPosition();
    var velocitySignal = encoder.getVelocity();

    return new RawSample(
        Rotations.of(positionSignal.getValueAsDouble()),
        RotationsPerSecond.of(velocitySignal.getValueAsDouble()),
        true,
        positionSignal.getStatus().isOK(),
        CtreEncoderFaults.getCANcoderFaults(encoder));
  }

  /** CAN id of the CANcoder (used by a Talon that reads it directly as feedback). */
  /** The underlying CANcoder, for vendor features the library does not wrap. */
  public CANcoder getCANcoder() {
    return encoder;
  }

  public int getDeviceId() {
    return encoder.getDeviceID();
  }

  @Override
  public void setPosition(Angle position) {
    // Hardware-level zero: more precise and survives brownouts, unlike a software offset.
    encoder.setPosition(position.in(Rotations));
  }
}
