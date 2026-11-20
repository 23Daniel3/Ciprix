package frc.robot;

import edu.wpi.first.wpilibj.DriverStation.Alliance;
import org.ciprix.CiprixSettings.Mode;

/** Robot-wide constants. Each subsystem's constants live in {@code <Subsystem>Constants}. */
public final class Constants {
  private Constants() {}

  /** What runs off-robot: {@link Mode#SIM} (physics simulation) or {@link Mode#REPLAY}. */
  public static final Mode simMode = Mode.SIM;

  /** Live-tunable values on the dashboard. Turn off at competitions. */
  public static final boolean tuningMode = true;

  public static final double CONTROLLER_DEADBAND = 0.08;
  public static final Alliance DEFAULT_ALLIANCE = Alliance.Blue;
  public static final double FIELD_LENGTH_METERS = 16.541;
  public static final double FIELD_WIDTH_METERS = 8.069;

  /** Which vendor's controller the flywheel uses on the real robot. */
  public enum Vendor {
    REV_SPARK_MAX,
    CTRE_TALON_SRX
  }

  /** Change this one line to run the same flywheel on the other controller. */
  public static final Vendor FLYWHEEL_VENDOR = Vendor.REV_SPARK_MAX;

  /** What the driver asks the robot to do. */
  public enum RobotRequest {
    IDLE,
    SHOOT
  }

  /** States of the overall state machine (the SuperStructure). */
  public enum RobotState {
    IDLE,
    SHOOTING
  }
}
