package org.ciprix.util.drivetrain;

import edu.wpi.first.math.geometry.Translation2d;
import org.ciprix.util.AllianceFlipUtil;

/**
 * Changes the driver's translation input before it reaches the drivetrain (assists, zones, curves).
 *
 * <p>Both {@code input} and the returned value are in the <b>driver frame</b>: the joystick vector,
 * normalized to [-1, 1], where +X points away from the driver's own alliance wall. That is the
 * frame {@link AllianceFlipUtil#apply(DriveContext, double, double, double)} expects, so modifiers
 * can be chained and the result converted to field speeds once at the end. A modifier that computes
 * a direction from field positions (a target, a zone center) must convert it with {@link
 * #fieldToDriver(Translation2d)}.
 */
@FunctionalInterface
public interface TranslationModifier {
  Translation2d apply(DriveContext drivetrain, Translation2d input);

  /**
   * Converts a direction measured on the field (blue origin) into the driver frame: unchanged on
   * the Blue Alliance, reversed on the Red Alliance, where the driver faces -X.
   */
  static Translation2d fieldToDriver(Translation2d fieldDirection) {
    return AllianceFlipUtil.shouldFlip() ? fieldDirection.unaryMinus() : fieldDirection;
  }
}
