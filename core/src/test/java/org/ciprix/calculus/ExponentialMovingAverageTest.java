package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import org.junit.jupiter.api.Test;

/** The exponential moving averages smooth noisy readings without lagging forever. */
class ExponentialMovingAverageTest {

  @Test
  void firstValuePassesThroughUnchanged() {
    ExponentialMovingAverage ema = new ExponentialMovingAverage(0.2);
    assertEquals(7.0, ema.calculate(7.0), 1e-9);
  }

  @Test
  void movesAlphaOfTheWayTowardsEachNewValue() {
    ExponentialMovingAverage ema = new ExponentialMovingAverage(0.25);
    ema.calculate(0.0);
    assertEquals(1.0, ema.calculate(4.0), 1e-9); // 0 + 0.25 * (4 - 0)
    assertEquals(1.75, ema.calculate(4.0), 1e-9); // 1 + 0.25 * (4 - 1)
  }

  @Test
  void alphaOneFollowsInputExactly() {
    ExponentialMovingAverage ema = new ExponentialMovingAverage(1.0);
    ema.calculate(3.0);
    assertEquals(-2.0, ema.calculate(-2.0), 1e-9);
  }

  @Test
  void convergesToAConstantInput() {
    ExponentialMovingAverage ema = new ExponentialMovingAverage(0.3);
    ema.calculate(0.0);
    double value = 0.0;
    for (int i = 0; i < 100; i++) {
      value = ema.calculate(10.0);
    }
    assertEquals(10.0, value, 1e-6);
  }

  @Test
  void resetMakesTheNextValuePassThroughAgain() {
    ExponentialMovingAverage ema = new ExponentialMovingAverage(0.1);
    ema.calculate(0.0);
    ema.calculate(100.0);
    ema.reset();
    assertEquals(42.0, ema.calculate(42.0), 1e-9);
  }

  @Test
  void poseFiltersEachAxis() {
    ExponentialMovingAveragePose ema = new ExponentialMovingAveragePose(0.5);
    ema.calculate(new Pose2d(0.0, 0.0, Rotation2d.kZero));
    Pose2d out = ema.calculate(new Pose2d(2.0, -4.0, Rotation2d.fromDegrees(90)));
    assertEquals(1.0, out.getX(), 1e-9);
    assertEquals(-2.0, out.getY(), 1e-9);
    assertEquals(45.0, out.getRotation().getDegrees(), 1e-9);
  }

  @Test
  void poseHeadingTakesTheShortWayAcrossPlusMinus180() {
    // 179 deg and -179 deg are 2 deg apart: the average must stay near 180, not swing to 0.
    ExponentialMovingAveragePose ema = new ExponentialMovingAveragePose(0.5);
    ema.calculate(new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(179)));
    Pose2d out = ema.calculate(new Pose2d(0.0, 0.0, Rotation2d.fromDegrees(-179)));
    assertEquals(0.0, out.getRotation().minus(Rotation2d.fromDegrees(180)).getDegrees(), 1e-6);
  }

  @Test
  void poseResetMakesTheNextPosePassThrough() {
    ExponentialMovingAveragePose ema = new ExponentialMovingAveragePose(0.1);
    ema.calculate(new Pose2d(0.0, 0.0, Rotation2d.kZero));
    ema.reset();
    Pose2d out = ema.calculate(new Pose2d(5.0, 6.0, Rotation2d.fromDegrees(-120)));
    assertEquals(5.0, out.getX(), 1e-9);
    assertEquals(6.0, out.getY(), 1e-9);
    assertEquals(-120.0, out.getRotation().getDegrees(), 1e-9);
  }
}
