package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import org.ciprix.calculus.LinearInterpolation.Point;
import org.ciprix.logger.LoggedTunableNumber;
import org.junit.jupiter.api.Test;

class ShotOnTheMoveCalculatorTest {

  private static final double WHEEL = 0.1;
  private static final Translation2d TARGET = new Translation2d(5.0, 0.0);

  private static int counter = 0;

  /** Flat 3000 RPM map, no smoothing, full efficiency, scalars at 1. */
  private static ShotOnTheMoveCalculator calculator(double shooterOffset) {
    String prefix = "Test/SOTM" + (counter++);
    return new ShotOnTheMoveCalculator(
        prefix,
        pose -> TARGET,
        new LoggedTunableMap(prefix + "/Map", false, new Point(0, 3000), new Point(10, 3000)),
        new LoggedTunableNumber(prefix + "/Aim", 1.0),
        new LoggedTunableNumber(prefix + "/Rpm", 1.0),
        new LoggedTunableNumber(prefix + "/Eff", 1.0),
        new ShotOnTheMoveCalculator.Config(WHEEL, 0.0, shooterOffset, 1.0));
  }

  @Test
  void stillRobotAimsStraightAtTheTargetWithTheMapRpm() {
    ShotOnTheMoveCalculator calc = calculator(0.0);
    ShotParameters shot = calc.calculate(new Pose2d(), new ChassisSpeeds());
    assertEquals(0.0, shot.aimAngle().getDegrees(), 1e-9);
    assertEquals(3000.0, shot.rpm(), 1e-9);
    assertEquals(shot, calc.getLastResult());
  }

  @Test
  void drivingTowardTheTargetNeedsLessRpm() {
    ShotParameters shot = calculator(0.0).calculate(new Pose2d(), new ChassisSpeeds(2.0, 0, 0));
    double expected = 3000.0 - 2.0 * 60.0 / (Math.PI * WHEEL);
    assertEquals(expected, shot.rpm(), 1e-6);
    assertEquals(0.0, shot.aimAngle().getDegrees(), 1e-9);
  }

  @Test
  void drivingAwayNeedsMoreRpm() {
    ShotParameters shot = calculator(0.0).calculate(new Pose2d(), new ChassisSpeeds(-2.0, 0, 0));
    assertTrue(shot.rpm() > 3000.0);
  }

  @Test
  void movingSidewaysAimsAgainstTheMotion() {
    // Moving left (+Y) while shooting toward +X: the ball drifts left, so aim right.
    ShotParameters left = calculator(0.0).calculate(new Pose2d(), new ChassisSpeeds(0, 1.0, 0));
    assertTrue(left.aimAngle().getDegrees() < 0.0);

    // Exit speed 3000 rpm * pi * 0.1 / 60 = 15.7 m/s; 5 m takes 0.318 s; 0.318 m of drift.
    double tof = 5.0 / (3000.0 * Math.PI * WHEEL / 60.0);
    assertEquals(-Math.toDegrees(Math.atan2(tof, 5.0)), left.aimAngle().getDegrees(), 1e-6);
    // Sideways motion does not change the RPM.
    assertEquals(3000.0, left.rpm(), 1e-9);
  }

  @Test
  void speedsAreRobotRelative() {
    // Facing +Y and driving "forward" is moving +Y on the field: same as the case above.
    Pose2d facingLeft = new Pose2d(0, 0, Rotation2d.kCCW_90deg);
    ShotParameters shot = calculator(0.0).calculate(facingLeft, new ChassisSpeeds(1.0, 0, 0));
    assertTrue(shot.aimAngle().getDegrees() < 0.0);
  }

  @Test
  void shooterOffsetIsCorrected() {
    // A shooter mounted 0.5 m to the left turns the robot slightly right.
    ShotParameters shot = calculator(0.5).calculate(new Pose2d(), new ChassisSpeeds());
    assertEquals(-Math.toDegrees(Math.asin(0.1)), shot.aimAngle().getDegrees(), 1e-9);
  }
}
