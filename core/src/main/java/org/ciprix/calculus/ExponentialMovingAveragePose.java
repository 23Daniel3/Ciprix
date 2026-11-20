package org.ciprix.calculus;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

/**
 * Exponential moving average of a {@link Pose2d}: X, Y and heading are each smoothed with the same
 * {@code alpha}. The heading is unwrapped before filtering, so a robot facing 179 deg that reads
 * -179 deg is averaged across 180 deg instead of swinging through 0 deg.
 */
public class ExponentialMovingAveragePose {
  private final ExponentialMovingAverage filterX;
  private final ExponentialMovingAverage filterY;
  private final ExponentialMovingAverage filterTheta;

  // Continuous heading fed to the filter, so the +-180 deg wrap never looks like a full turn.
  private double lastRawTheta = Double.NaN;
  private double unwrappedTheta = 0.0;

  public ExponentialMovingAveragePose(double alpha) {
    filterX = new ExponentialMovingAverage(alpha);
    filterY = new ExponentialMovingAverage(alpha);
    filterTheta = new ExponentialMovingAverage(alpha);
  }

  public void reset() {
    filterX.reset();
    filterY.reset();
    filterTheta.reset();
    lastRawTheta = Double.NaN;
  }

  public Pose2d calculate(Pose2d value) {
    double newX = filterX.calculate(value.getX());
    double newY = filterY.calculate(value.getY());

    double rawTheta = value.getRotation().getRadians();
    if (Double.isNaN(lastRawTheta)) {
      unwrappedTheta = rawTheta;
    } else {
      unwrappedTheta += MathUtil.angleModulus(rawTheta - lastRawTheta);
    }
    lastRawTheta = rawTheta;
    double newTheta = filterTheta.calculate(unwrappedTheta);

    return new Pose2d(newX, newY, new Rotation2d(newTheta));
  }
}
