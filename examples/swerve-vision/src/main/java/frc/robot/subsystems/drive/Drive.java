package frc.robot.subsystems.drive;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;

import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N3;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.LinearVelocity;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import org.ciprix.util.drivetrain.DriveContext;
import org.littletonrobotics.junction.Logger;

/**
 * A drivetrain reduced to what the driving helpers need. It implements {@link DriveContext}, the
 * only contract between your drivetrain and the library's helpers: with a real swerve you keep your
 * own class (CTRE's {@code SwerveDrivetrain}, YAGSL, AdvantageKit's template...) and make it
 * implement the same four methods.
 *
 * <p>Here the robot is simulated as an ideal holonomic base, and its odometry drifts a little on
 * purpose, so you can watch the vision pipeline pull the estimate back in AdvantageScope ({@code
 * Drive/EstimatedPose} against {@code Drive/TruePose}).
 */
public class Drive extends SubsystemBase implements DriveContext {

  private Pose2d truePose = new Pose2d(Constants.START_X, Constants.START_Y, Rotation2d.kZero);
  private Pose2d estimatedPose = truePose;
  private ChassisSpeeds robotSpeeds = new ChassisSpeeds();

  // --- DriveContext: what the library's helpers read ---

  @Override
  public Pose2d getPose() {
    return estimatedPose;
  }

  @Override
  public ChassisSpeeds getRobotVelocity() {
    return robotSpeeds;
  }

  @Override
  public LinearVelocity getMaxSpeed() {
    return MetersPerSecond.of(DriveConstants.MAX_SPEED_MPS);
  }

  @Override
  public AngularVelocity getMaxAngularSpeed() {
    return RadiansPerSecond.of(DriveConstants.MAX_ANGULAR_SPEED_RAD_PER_SEC);
  }

  // --- Driving ---

  /** Drives with speeds relative to the field. */
  public void runFieldRelative(ChassisSpeeds fieldSpeeds) {
    robotSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(fieldSpeeds, truePose.getRotation());
  }

  public void stop() {
    robotSpeeds = new ChassisSpeeds();
  }

  /** The simulated truth. Real robots do not have this: it is only for the example. */
  public Pose2d getTruePose() {
    return truePose;
  }

  // --- Vision: the signature AprilTagVision expects ---

  /** Receives the accepted vision poses; blends them into the estimate by their uncertainty. */
  public void addVisionMeasurement(
      Pose2d visionPose, double timestampSeconds, Matrix<N3, N1> standardDeviations) {
    // A real pose estimator (WPILib's SwerveDrivePoseEstimator, for example) also replays the
    // odometry since the image was taken. This example just takes a step toward the measurement.
    double gain = 1.0 / (1.0 + 10.0 * standardDeviations.get(0, 0));
    estimatedPose =
        new Pose2d(
            estimatedPose.getTranslation().interpolate(visionPose.getTranslation(), gain),
            estimatedPose.getRotation());
  }

  @Override
  public void periodic() {
    double dt = 0.02;
    Twist2d step =
        new Twist2d(
            robotSpeeds.vxMetersPerSecond * dt,
            robotSpeeds.vyMetersPerSecond * dt,
            robotSpeeds.omegaRadiansPerSecond * dt);
    truePose = truePose.exp(step);
    // The odometry sees the same motion with a small error.
    Twist2d measured =
        new Twist2d(step.dx * DriveConstants.ODOMETRY_DRIFT_FACTOR, step.dy, step.dtheta);
    estimatedPose = estimatedPose.exp(measured);

    Logger.recordOutput("Drive/TruePose", truePose);
    Logger.recordOutput("Drive/EstimatedPose", estimatedPose);
    Logger.recordOutput("Drive/RobotSpeeds", robotSpeeds);
  }
}
