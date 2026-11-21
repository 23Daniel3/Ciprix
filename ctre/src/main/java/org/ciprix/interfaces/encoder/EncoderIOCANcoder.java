package org.ciprix.interfaces.encoder;

import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;

import com.ctre.phoenix6.CANBus;
import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.CANcoderConfiguration;
import com.ctre.phoenix6.hardware.CANcoder;
import com.ctre.phoenix6.signals.SensorDirectionValue;
import edu.wpi.first.units.measure.Angle;
import org.ciprix.util.Report;

/**
 * {@link org.ciprix.interfaces.encoder.EncoderIO} for a CTRE CANcoder (Phoenix 6).
 *
 * <p>Reports native position and velocity straight off the CAN bus — no numeric differentiation.
 * Inversion is pushed into the hardware sensor-direction config so the sensor itself reports
 * correctly signed values from boot, before a subsystem's first periodic loop even runs. The offset
 * stays purely in {@link EncoderBase}'s software layer (not also baked into the hardware {@code
 * MagnetOffset}) so there's exactly one place it's applied. Because a Talon that reads this
 * CANcoder directly over CAN ({@code withExternalEncoder} / {@code withRemoteCANcoder}) sees the
 * raw CANcoder position without {@link EncoderBase}'s software offset, keep {@code
 * EncoderConfig.offset} at zero when the CANcoder is used as Talon hardware feedback and zero via
 * {@link #setPosition(Angle)} instead.
 */
public class EncoderIOCANcoder extends EncoderBase {

  /** Time allowed to the boot configuration, per attempt (same as the Phoenix 6 Talons). */
  static final double BOOT_TIMEOUT_SECONDS = 0.25;

  static final int BOOT_ATTEMPTS = 5;

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

    // A CANcoder that is still booting can miss the first write. Without the retry it would keep
    // its saved direction, and an inverted encoder would silently read backwards.
    StatusCode status = StatusCode.OK;
    for (int attempt = 0; attempt < BOOT_ATTEMPTS; attempt++) {
      status = encoder.getConfigurator().apply(hwConfig, BOOT_TIMEOUT_SECONDS);
      if (status.isOK()) {
        return;
      }
    }
    Report.error(
        "Encoder "
            + name
            + ": configuration failed after "
            + BOOT_ATTEMPTS
            + " attempts ("
            + status
            + "). Check the CAN wiring, the id and the bus.");
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

  /** The underlying CANcoder, for vendor features the library does not wrap. */
  public CANcoder getCANcoder() {
    return encoder;
  }

  /** CAN id of the CANcoder (used by a Talon that reads it directly as feedback). */
  public int getDeviceId() {
    return encoder.getDeviceID();
  }

  @Override
  public void setPosition(Angle position) {
    // Convert desired mechanism position back to the raw sensor frame expected by EncoderBase:
    // reported = (raw - softwareOffset) * factor  =>  raw = position / factor + softwareOffset.
    // Note: the CANcoder's relative position register resets to the absolute magnet reading if the
    // sensor itself reboots or browns out.
    double factor = config.positionConversionFactor != 0.0 ? config.positionConversionFactor : 1.0;
    double rawRotations = position.in(Rotations) / factor + getSoftwareOffset().in(Rotations);
    for (int attempt = 0; attempt < BOOT_ATTEMPTS; attempt++) {
      if (encoder.setPosition(rawRotations, BOOT_TIMEOUT_SECONDS).isOK()) {
        return;
      }
    }
  }
}
