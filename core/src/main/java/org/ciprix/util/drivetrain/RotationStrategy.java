package org.ciprix.util.drivetrain;

/**
 * Decides the drivetrain's angular velocity each cycle (manual stick, heading lock, aim at a
 * target).
 */
@FunctionalInterface
public interface RotationStrategy {
  /**
   * @return the angular velocity request, counter-clockwise positive. {@link ManualRotation} and
   *     the manual override of {@link PIDRotation} return rad/s already scaled by the maximum
   *     speed; the PID path returns the controller output in rad/s.
   */
  double calculate(DriveContext drivetrain);

  /** Called when the strategy becomes active, so it can drop stale state. */
  default void reset(DriveContext drivetrain) {}
}
