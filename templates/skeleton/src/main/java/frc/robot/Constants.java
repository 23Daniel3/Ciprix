package frc.robot;

import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj.RobotBase;

/** Constants for the whole robot. Each subsystem's live in {@code <Subsystem>Constants}. */
public final class Constants {
  private Constants() {}

  /** What runs off the robot: SIM (simulation with physics) or REPLAY (re-runs a log). */
  public static final Mode simMode = Mode.SIM;

  public static final Mode currentMode = RobotBase.isReal() ? Mode.REAL : simMode;

  /** Values tunable from the dashboard. Turn this off at competitions. */
  public static final boolean tuningMode = true;

  public static final double CONTROLLER_DEADBAND = 0.08;

  /** Alliance used when there is no FMS or Driver Station. */
  public static final Alliance DEFAULT_ALLIANCE = Alliance.Blue;

  /** The 2026 field (REBUILT, welded version), in meters. */
  public static final double FIELD_LENGTH_METERS = 16.541;

  public static final double FIELD_WIDTH_METERS = 8.069;

  public enum Mode {
    /** On the real robot. */
    REAL,
    /** Simulation with physics. */
    SIM,
    /** Re-running a log. */
    REPLAY
  }

  /** What the driver asks of the robot. One button (or combination) per request. */
  public enum RobotRequest {
    IDLE,
    INTAKE,
    EJECT
  }

  /** States of the overall state machine (the SuperStructure). */
  public enum RobotState {
    IDLE,
    INTAKING,
    EJECTING
  }
}
