package org.ciprix.util.security;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.MetersPerSecondPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecondPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.units.measure.AngularAcceleration;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.LinearAcceleration;
import edu.wpi.first.units.measure.LinearVelocity;
import java.util.Optional;
import org.ciprix.util.drivetrain.DriveAccelerationLimits;
import org.ciprix.util.drivetrain.DriveContext;
import org.ciprix.util.security.antitipping.AntiTipping;
import org.ciprix.util.security.antitipping.AntiTippingAccel;
import org.ciprix.util.security.antitipping.AntiTippingAccel.AntiTippingConfig;
import org.junit.jupiter.api.Test;

/**
 * Both anti-tipping helpers must drive toward the side that is going down, robot-relative: positive
 * pitch is nose down (drive +X), positive roll is right side down (drive -Y).
 */
class AntiTippingTest {

  private static Rotation3d tilt(double rollDeg, double pitchDeg) {
    return new Rotation3d(Units.degreesToRadians(rollDeg), Units.degreesToRadians(pitchDeg), 0.0);
  }

  // ---------------------------------------------------------------------------------------------
  // AntiTipping (proportional)
  // ---------------------------------------------------------------------------------------------

  @Test
  void levelRobotNeedsNoCorrection() {
    AntiTipping antiTipping = new AntiTipping(0.1, 10.0, 2.0);
    assertTrue(antiTipping.calculate(tilt(2, -3)).isEmpty());
    assertFalse(antiTipping.isTipping());
  }

  @Test
  void noseDownDrivesForward() {
    AntiTipping antiTipping = new AntiTipping(0.1, 10.0, 2.0);
    ChassisSpeeds speeds = antiTipping.calculate(tilt(0, 15)).orElseThrow();
    assertEquals(1.5, speeds.vxMetersPerSecond, 1e-9);
    assertEquals(0.0, speeds.vyMetersPerSecond, 1e-9);
    assertEquals(0.0, antiTipping.getLastYawDirectionDeg(), 1e-9);
  }

  @Test
  void noseUpDrivesBackward() {
    AntiTipping antiTipping = new AntiTipping(0.1, 10.0, 2.0);
    assertEquals(-1.5, antiTipping.calculate(tilt(0, -15)).orElseThrow().vxMetersPerSecond, 1e-9);
  }

  @Test
  void rightSideDownDrivesRight() {
    AntiTipping antiTipping = new AntiTipping(0.1, 10.0, 2.0);
    ChassisSpeeds speeds = antiTipping.calculate(tilt(15, 0)).orElseThrow();
    assertEquals(0.0, speeds.vxMetersPerSecond, 1e-9);
    assertEquals(-1.5, speeds.vyMetersPerSecond, 1e-9);
  }

  @Test
  void correctionIsCappedAtTheMaximumSpeed() {
    AntiTipping antiTipping = new AntiTipping(1.0, 10.0, 2.0);
    ChassisSpeeds speeds = antiTipping.calculate(tilt(-20, 20)).orElseThrow();
    assertEquals(2.0, Math.hypot(speeds.vxMetersPerSecond, speeds.vyMetersPerSecond), 1e-9);
    assertTrue(speeds.vxMetersPerSecond > 0.0);
    assertTrue(speeds.vyMetersPerSecond > 0.0, "left side down: drive left");
    assertEquals(Math.hypot(20, 20), antiTipping.getLastInclinationMagnitude(), 1e-9);
  }

  @Test
  void settersChangeTheBehavior() {
    AntiTipping antiTipping = new AntiTipping(0.1, 10.0, 2.0);
    antiTipping.setTippingThreshold(20.0);
    assertTrue(antiTipping.calculate(tilt(0, 15)).isEmpty());
    antiTipping.setTippingThreshold(5.0);
    antiTipping.setKp(0.01);
    antiTipping.setMaxCorrectionSpeed(0.1);
    assertEquals(0.1, antiTipping.calculate(tilt(0, 15)).orElseThrow().vxMetersPerSecond, 1e-9);
  }

  // ---------------------------------------------------------------------------------------------
  // AntiTippingAccel (correction plus preventive acceleration limits)
  // ---------------------------------------------------------------------------------------------

  private static final class FakeDrive implements DriveContext, DriveAccelerationLimits {
    ChassisSpeeds velocity = new ChassisSpeeds();
    LinearAcceleration maxAccel = MetersPerSecondPerSecond.of(5.0);
    AngularAcceleration maxAngularAccel = RadiansPerSecondPerSecond.of(10.0);

    @Override
    public Pose2d getPose() {
      return Pose2d.kZero;
    }

    @Override
    public ChassisSpeeds getRobotVelocity() {
      return velocity;
    }

    @Override
    public LinearVelocity getMaxSpeed() {
      return MetersPerSecond.of(4.5);
    }

    @Override
    public AngularVelocity getMaxAngularSpeed() {
      return RadiansPerSecond.of(10.0);
    }

    @Override
    public LinearAcceleration getMaxAcceleration() {
      return maxAccel;
    }

    @Override
    public void setMaxAcceleration(LinearAcceleration maxAcceleration) {
      maxAccel = maxAcceleration;
    }

    @Override
    public AngularAcceleration getMaxAngularAcceleration() {
      return maxAngularAccel;
    }

    @Override
    public void setMaxAngularAcceleration(AngularAcceleration maxAngularAcceleration) {
      maxAngularAccel = maxAngularAcceleration;
    }
  }

  private static final LinearAcceleration ZERO = MetersPerSecondPerSecond.of(0.0);

  @Test
  void accelVersionDrivesTowardTheLowSide() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    FakeDrive drive = new FakeDrive();

    ChassisSpeeds forward = antiTipping.calculate(tilt(0, 15), ZERO, ZERO, drive).orElseThrow();
    assertEquals(2.0, forward.vxMetersPerSecond, 1e-9); // 0.2 * 15 deg, capped at 2 m/s
    assertEquals(0.0, forward.vyMetersPerSecond, 1e-9);

    ChassisSpeeds right = antiTipping.calculate(tilt(15, 0), ZERO, ZERO, drive).orElseThrow();
    assertEquals(-2.0, right.vyMetersPerSecond, 1e-9);
  }

  @Test
  void accelVersionDoesNothingBelowTheThreshold() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    Optional<ChassisSpeeds> correction =
        antiTipping.calculate(tilt(0, 3), ZERO, ZERO, new FakeDrive());
    assertTrue(correction.isEmpty());
    assertFalse(antiTipping.getLastState().isTipping());
  }

  @Test
  void riskLowersTheAccelerationLimitsGradually() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    FakeDrive drive = new FakeDrive();

    antiTipping.calculate(tilt(0, 9), ZERO, ZERO, drive);
    double firstStep = drive.maxAccel.in(MetersPerSecondPerSecond);
    // Limited by accelChangeRate (2 m/s^3) * 20 ms per cycle.
    assertEquals(5.0 - 0.04, firstStep, 1e-9);
    assertTrue(antiTipping.getLastState().riskScore() > 0.0);
    assertTrue(drive.maxAngularAccel.in(RadiansPerSecondPerSecond) < 10.0);

    for (int i = 0; i < 500; i++) {
      antiTipping.calculate(tilt(0, 9), ZERO, ZERO, drive);
    }
    // 9 of 10 degrees, still: risk = 0.9 / riskSensitivity 2 = 0.45. The limit settles at
    // 55 % of the drivetrain's own 5 m/s^2 instead of compounding toward zero.
    assertEquals(0.45, antiTipping.getLastState().riskScore(), 1e-9);
    assertEquals(2.75, drive.maxAccel.in(MetersPerSecondPerSecond), 1e-9);
    assertEquals(6.4, drive.maxAngularAccel.in(RadiansPerSecondPerSecond), 1e-9);
  }

  @Test
  void limitsComeBackWhenTheRobotIsLevelAgain() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    FakeDrive drive = new FakeDrive();
    for (int i = 0; i < 500; i++) {
      antiTipping.calculate(tilt(0, 9), ZERO, ZERO, drive);
    }
    for (int i = 0; i < 500; i++) {
      antiTipping.calculate(tilt(0, 0), ZERO, ZERO, drive);
    }
    assertEquals(5.0, drive.maxAccel.in(MetersPerSecondPerSecond), 1e-9);
    assertEquals(10.0, drive.maxAngularAccel.in(RadiansPerSecondPerSecond), 1e-9);
  }

  @Test
  void resetRestoresFullAccelerationOnTheNextCycle() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    FakeDrive drive = new FakeDrive();
    for (int i = 0; i < 500; i++) {
      antiTipping.calculate(tilt(0, 9), ZERO, ZERO, drive);
    }
    antiTipping.reset();
    antiTipping.calculate(tilt(0, 0), ZERO, ZERO, drive);
    assertEquals(5.0, drive.maxAccel.in(MetersPerSecondPerSecond), 1e-9);
  }

  @Test
  void reactiveOnlyModeLeavesTheLimitsAlone() {
    AntiTippingConfig config = new AntiTippingConfig();
    config.reactiveOnlyMode = true;
    AntiTippingAccel antiTipping = new AntiTippingAccel(config);
    FakeDrive drive = new FakeDrive();
    antiTipping.calculate(tilt(0, 9), ZERO, ZERO, drive);
    assertEquals(5.0, drive.maxAccel.in(MetersPerSecondPerSecond), 1e-12);
    assertEquals(0.0, antiTipping.getLastState().riskScore(), 1e-12);
  }

  @Test
  void levelAndStillRobotKeepsFullAcceleration() {
    AntiTippingAccel antiTipping = new AntiTippingAccel(new AntiTippingConfig());
    FakeDrive drive = new FakeDrive();
    for (int i = 0; i < 10; i++) {
      antiTipping.calculate(tilt(0, 0), ZERO, ZERO, drive);
    }
    assertEquals(5.0, drive.maxAccel.in(MetersPerSecondPerSecond), 1e-9);
    assertEquals(10.0, drive.maxAngularAccel.in(RadiansPerSecondPerSecond), 1e-9);
  }
}
