package frc.robot.subsystems.intake;

import static edu.wpi.first.units.Units.Amps;

import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.power.PowerPriority;

/** Intake constants: a roller that matters little when the battery sags, and can jam. */
public final class IntakeConstants {
  private IntakeConstants() {}

  public static final int MOTOR_ID = 40;
  public static final double INTAKE_OUTPUT = 0.8;

  /**
   * Three protections, each one a single line of configuration:
   *
   * <ul>
   *   <li>{@code LOW} priority: under a low battery this motor's current limit is cut first (to 50%
   *       below 9.5 V and 25% below 8 V), so the drivetrain and the shooter keep their power.
   *   <li>Stall reversal: commanded, drawing over 30 A and barely turning for 0.2 s, it reverses at
   *       50% for 0.3 s and goes back to what the code last asked, on its own.
   *   <li>Current warning: a Driver Station warning if the average draw stays high.
   * </ul>
   */
  public static final MotorConfig MOTOR_CONFIG =
      new MotorConfig()
          .currentLimit(Amps.of(40))
          .coastMode()
          .powerPriority(PowerPriority.LOW)
          .withStallReversal(Amps.of(30), 0.2, 0.3, 0.5)
          .withCurrentWarning(Amps.of(35), 50);

  public static final double MIN_SPEED_RPS = 5.0;

  /** What the SuperStructure (or the co-driver) asks of the intake. */
  public enum IntakeRequest {
    STOP,
    INTAKE
  }

  /** States of the intake's state machine. */
  public enum IntakeState {
    STOPPED,
    INTAKING
  }
}
