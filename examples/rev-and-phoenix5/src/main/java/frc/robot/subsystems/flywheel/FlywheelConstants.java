package frc.robot.subsystems.flywheel;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.RPM;

import org.ciprix.interfaces.motor.advanced.MotorConfig;

/**
 * Flywheel constants. The subsystem, its states and the simulation never change when the vendor
 * does; only the motor configuration and the IO class do. The gains differ because each controller
 * expects them in its own units (see the vendor guides): tune them on the robot.
 */
public final class FlywheelConstants {
  private FlywheelConstants() {}

  public static final int SPARK_MAX_ID = 30;
  public static final int TALON_SRX_ID = 31;

  public static final double GEARING = 1.5;
  public static final double TARGET_RPM = 3000.0;

  /** The flywheel spins this close to the target (RPM) before it counts as ready. */
  public static final double TOLERANCE_RPM = 100.0;

  /** SPARK MAX with a NEO: the encoder is built in. */
  public static final MotorConfig SPARK_CONFIG =
      new MotorConfig()
          .currentLimit(Amps.of(60))
          .coastMode()
          .conversionFactors(1.0 / GEARING, 1.0 / GEARING)
          .pid(0, 0.0003, 0.0, 0.0)
          .svag(0, 0.0, 0.0018, 0.0, 0.0)
          .withVelocityTolerance(RPM.of(TOLERANCE_RPM));

  /** Talon SRX with a quadrature encoder on its data port: counts per revolution are needed. */
  public static final MotorConfig SRX_CONFIG = srxConfig();

  private static MotorConfig srxConfig() {
    MotorConfig config =
        new MotorConfig()
            .currentLimit(Amps.of(40))
            .coastMode()
            .conversionFactors(1.0 / GEARING, 1.0 / GEARING)
            .pid(0, 0.25, 0.0, 0.0)
            .svag(0, 0.0, 0.05, 0.0, 0.0)
            .withVelocityTolerance(RPM.of(TOLERANCE_RPM));
    config.countsPerRevolution = 4096;
    return config;
  }

  /** What the SuperStructure (or the co-driver) asks of the flywheel. */
  public enum FlywheelRequest {
    STOP,
    SPIN_UP
  }

  /** States of the flywheel's state machine. */
  public enum FlywheelState {
    STOPPED,
    SPINNING_UP,
    READY
  }
}
