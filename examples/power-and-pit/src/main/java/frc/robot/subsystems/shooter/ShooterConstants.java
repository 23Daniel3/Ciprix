package frc.robot.subsystems.shooter;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.RPM;

import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.power.PowerPriority;

/** Shooter constants: the mechanism that decides the match keeps the default HIGH priority. */
public final class ShooterConstants {
  private ShooterConstants() {}

  public static final int MOTOR_ID = 41;
  public static final double TARGET_RPM = 3000.0;
  public static final double GEARING = 1.0;

  /**
   * {@code HIGH} is the default priority: the shooter's current limit is never reduced when the
   * battery sags, so it still reaches its speed in the last seconds of the match.
   */
  public static final MotorConfig MOTOR_CONFIG =
      new MotorConfig()
          .currentLimit(Amps.of(60))
          .coastMode()
          .powerPriority(PowerPriority.HIGH)
          .pid(0, 0.4, 0.0, 0.0)
          .svag(0, 0.0, 0.12, 0.0, 0.0)
          .withVelocityTolerance(RPM.of(100));

  /** What the SuperStructure asks of the shooter. */
  public enum ShooterRequest {
    STOP,
    SPIN_UP
  }

  /** States of the shooter's state machine. */
  public enum ShooterState {
    STOPPED,
    SPINNING_UP,
    READY
  }
}
