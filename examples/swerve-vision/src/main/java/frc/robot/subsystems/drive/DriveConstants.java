package frc.robot.subsystems.drive;

import edu.wpi.first.math.geometry.Translation2d;
import org.ciprix.zones.Polygon2d;

/** Drivetrain constants and the places on the field that the driving helpers use. */
public final class DriveConstants {
  private DriveConstants() {}

  public static final double MAX_SPEED_MPS = 4.5;
  public static final double MAX_ANGULAR_SPEED_RAD_PER_SEC = 2.0 * Math.PI;

  /** How far the simulated odometry drifts, to give the vision something to correct. */
  public static final double ODOMETRY_DRIFT_FACTOR = 1.04;

  /** The thing the driver aims at, in blue alliance coordinates (flipped on red). */
  public static final Translation2d TARGET = new Translation2d(4.6, 4.0);

  /** An obstacle the robot should be pushed away from (blue coordinates). */
  public static final Polygon2d KEEP_OUT =
      new Polygon2d(
          new Translation2d(6.0, 2.5),
          new Translation2d(7.5, 2.5),
          new Translation2d(7.5, 3.5),
          new Translation2d(6.0, 3.5));
}
