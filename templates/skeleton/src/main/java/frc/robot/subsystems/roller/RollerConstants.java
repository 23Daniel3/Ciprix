package frc.robot.subsystems.roller;

import static edu.wpi.first.units.Units.Amps;

import org.ciprix.interfaces.motor.advanced.MotorConfig;

/** Roller constants: ids, power levels, motor configuration, requests and states. */
public final class RollerConstants {
  private RollerConstants() {}

  public static final int MOTOR_ID = 10;

  public static final double INTAKE_OUTPUT = 0.6;
  public static final double EJECT_OUTPUT = -0.5;

  public static final MotorConfig MOTOR_CONFIG =
      new MotorConfig().currentLimit(Amps.of(40)).coastMode();

  /** What the SuperStructure (or the co-driver) asks of the roller. */
  public enum RollerRequest {
    STOP,
    INTAKE,
    EJECT
  }

  /** States of the roller's state machine. */
  public enum RollerState {
    STOPPED,
    INTAKING,
    EJECTING
  }
}
