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

  /** Starting pose of the simulated robot (blue alliance side). */
  public static final double START_X = 2.0;

  public static final double START_Y = 4.0;
}
