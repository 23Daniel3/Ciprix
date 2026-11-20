package frc.robot.subsystems.elevator;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;

import edu.wpi.first.math.geometry.Translation3d;
import org.ciprix.interfaces.motor.advanced.MotorConfig;

/** Elevator constants. Positions are in rotations of the drum. */
public final class ElevatorConstants {
  private ElevatorConstants() {}

  public static final int MOTOR_ID = 21;

  /** Motor turns per turn of the drum. */
  public static final double GEARING = 9.0;

  public static final double CARRIAGE_MASS_KG = 6.0;
  public static final double DRUM_RADIUS_METERS = 0.025;

  /** Meters the carriage rises per turn of the drum. */
  public static final double METERS_PER_ROTATION = 2.0 * Math.PI * DRUM_RADIUS_METERS;

  public static final double LOW_METERS = 0.0;
  public static final double MID_METERS = 0.45;
  public static final double HIGH_METERS = 0.90;

  /** Where the carriage starts and the direction it rises, measured in the CAD. */
  public static final Translation3d CARRIAGE_START = new Translation3d(0.0, 0.0, 0.1);

  public static final Translation3d UP = new Translation3d(0.0, 0.0, 1.0);

  /** {@code ELEVATOR} gravity (the default) is a constant kG: the weight always pulls the same. */
  public static final MotorConfig MOTOR_CONFIG =
      new MotorConfig()
          .currentLimit(Amps.of(60))
          .brakeMode()
          .conversionFactors(1.0 / GEARING, 1.0 / GEARING)
          .pid(0, 40.0, 0.0, 0.5)
          .svag(0, 0.1, 0.12, 0.0, 0.45)
          .smartMotion(
              0,
              RotationsPerSecond.of(4.0),
              RotationsPerSecondPerSecond.of(10.0),
              Rotations.of(0.05))
          .softLimits(Rotations.of(0.0), Rotations.of(HIGH_METERS / METERS_PER_ROTATION + 0.1))
          .withPositionTolerance(Rotations.of(0.05));

  /** What the SuperStructure (or the co-driver) asks of the elevator. */
  public enum ElevatorRequest {
    LOW,
    MID,
    HIGH
  }

  /** States of the elevator's state machine: one resting state per height, one per trip. */
  public enum ElevatorState {
    AT_LOW,
    AT_MID,
    AT_HIGH,
    MOVING_TO_LOW,
    MOVING_TO_MID,
    MOVING_TO_HIGH
  }
}
