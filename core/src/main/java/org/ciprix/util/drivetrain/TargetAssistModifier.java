package org.ciprix.util.drivetrain;

import edu.wpi.first.math.geometry.Translation2d;
import java.util.function.Supplier;

/**
 * Blends the driver's translation stick with a unit vector pointing toward a field target.
 *
 * <p>Because the target vector has magnitude {@code weight} even when the stick is centered, this
 * modifier pulls the robot toward the target whenever it is active. Gate it behind a held button
 * (for example {@code pilot.leftBumper().getAsBoolean() ? assist.apply(ctx, in) : in}) rather than
 * leaving it unconditionally in the modifier chain.
 */
public class TargetAssistModifier implements TranslationModifier {
  private final Supplier<Translation2d> targetSupplier;
  private final double weight; // 0.0 to 1.0

  public TargetAssistModifier(Supplier<Translation2d> targetSupplier, double weight) {
    this.targetSupplier = targetSupplier;
    this.weight = weight;
  }

  @Override
  public Translation2d apply(DriveContext drivetrain, Translation2d joystick) {
    Translation2d current = drivetrain.getPose().getTranslation();
    Translation2d target = targetSupplier.get();
    Translation2d vectorToTarget = TranslationModifier.fieldToDriver(target.minus(current));

    if (vectorToTarget.getNorm() > 0.01)
      vectorToTarget = vectorToTarget.div(vectorToTarget.getNorm());
    else vectorToTarget = new Translation2d();

    Translation2d blended = joystick.times(1.0 - weight).plus(vectorToTarget.times(weight));

    if (blended.getNorm() > 1.0) blended = blended.div(blended.getNorm());
    return blended;
  }
}
