package frc.robot.subsystems.arm;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;

import edu.wpi.first.math.geometry.Translation3d;
import org.ciprix.interfaces.motor.advanced.MotorConfig;

/** Arm constants: ids, gearing, targets, motor configuration, requests and states. */
public final class ArmConstants {
  private ArmConstants() {}

  public static final int MOTOR_ID = 20;

  /** Motor turns per turn of the arm. */
  public static final double GEARING = 50.0;

  public static final double MASS_KG = 3.0;
  public static final double LENGTH_METERS = 0.6;

  /** Targets in rotations of the arm. Zero is horizontal and positive is up. */
  public static final double STOW_ROTATIONS = -0.22;

  public static final double INTAKE_ROTATIONS = -0.05;
  public static final double SCORE_ROTATIONS = 0.20;
  public static final double MIN_ROTATIONS = -0.25;
  public static final double MAX_ROTATIONS = 0.30;

  /**
   * A point on the arm's rotation axis, measured in the CAD from the elevator carriage's origin.
   */
  public static final Translation3d PIVOT_AXIS = new Translation3d(0.25, 0.0, 0.7);

  /**
   * Position control with Motion Magic. {@code armGravity()} makes kG a cosine of the position, so
   * the arm holds itself up at any angle.
   */
  public static final MotorConfig MOTOR_CONFIG =
      new MotorConfig()
          .currentLimit(Amps.of(50))
          .brakeMode()
          .conversionFactors(1.0 / GEARING, 1.0 / GEARING)
          .pid(0, 60.0, 0.0, 1.0)
          .svag(0, 0.1, 8.0, 0.0, 0.35)
          .armGravity()
          .smartMotion(
              0,
              RotationsPerSecond.of(0.6),
              RotationsPerSecondPerSecond.of(1.5),
              Rotations.of(0.01))
          .softLimits(Rotations.of(MIN_ROTATIONS), Rotations.of(MAX_ROTATIONS))
          .withPositionTolerance(Rotations.of(0.01));

  /** What the SuperStructure (or the co-driver) asks of the arm. */
  public enum ArmRequest {
    STOW,
    INTAKE,
    SCORE
  }

  /** States of the arm's state machine: one resting state per target, one for each trip. */
  public enum ArmState {
    STOWED,
    AT_INTAKE,
    AT_SCORE,
    MOVING_TO_STOW,
    MOVING_TO_INTAKE,
    MOVING_TO_SCORE
  }
}
