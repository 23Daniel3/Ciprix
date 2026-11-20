package org.ciprix.util.drivetrain;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.LinearVelocity;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import org.ciprix.zones.Polygon2d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Rotation strategies and translation modifiers. Modifiers work in the driver frame (see {@link
 * TranslationModifier}), so the field-based ones are checked on both alliances.
 */
class DrivetrainAssistsTest {

  private static final double MAX_OMEGA = 6.0;

  /** A 2 x 2 m square centered at (5, 4). */
  private static final Polygon2d ZONE =
      new Polygon2d(
          new Translation2d(4.0, 3.0),
          new Translation2d(6.0, 3.0),
          new Translation2d(6.0, 5.0),
          new Translation2d(4.0, 5.0));

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void blue() {
    setAlliance(AllianceStationID.Blue1);
  }

  @AfterEach
  void resetAlliance() {
    setAlliance(AllianceStationID.Blue1);
  }

  private static void setAlliance(AllianceStationID station) {
    DriverStationSim.setAllianceStationId(station);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
  }

  /** A drivetrain frozen at a pose. */
  private static final class FakeDrive implements DriveContext {
    Pose2d pose;

    FakeDrive(double x, double y, double headingDeg) {
      pose = new Pose2d(x, y, Rotation2d.fromDegrees(headingDeg));
    }

    @Override
    public Pose2d getPose() {
      return pose;
    }

    @Override
    public ChassisSpeeds getRobotVelocity() {
      return new ChassisSpeeds();
    }

    @Override
    public LinearVelocity getMaxSpeed() {
      return MetersPerSecond.of(4.0);
    }

    @Override
    public AngularVelocity getMaxAngularSpeed() {
      return RadiansPerSecond.of(MAX_OMEGA);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Rotation
  // ---------------------------------------------------------------------------------------------

  @Test
  void manualRotationSquaresTheStickAndKeepsTheSign() {
    ManualRotation rotation = new ManualRotation(() -> -0.5, () -> 4.0);
    assertEquals(-1.0, rotation.calculate(new FakeDrive(0, 0, 0)), 1e-9);
  }

  @Test
  void pidRotationTurnsTowardTheTargetTheShortWay() {
    FakeDrive drive = new FakeDrive(0, 0, 170);
    PIDRotation rotation = new PIDRotation(() -> Rotation2d.fromDegrees(-170));
    // From 170 to -170 is +20 degrees across the wrap, so it must turn counter-clockwise.
    assertTrue(rotation.calculate(drive) > 0.0);
  }

  @Test
  void pidRotationNeverAsksForMoreThanTheDrivetrainCanDo() {
    FakeDrive drive = new FakeDrive(0, 0, 0);
    PIDRotation rotation = new PIDRotation(() -> Rotation2d.fromDegrees(179));
    assertEquals(MAX_OMEGA, rotation.calculate(drive), 1e-9);
  }

  @Test
  void pidRotationManualStickOverridesTheTarget() {
    FakeDrive drive = new FakeDrive(0, 0, 0);
    double[] stick = {0.5};
    PIDRotation rotation = new PIDRotation(() -> Rotation2d.fromDegrees(90), () -> stick[0]);
    assertEquals(0.25 * MAX_OMEGA, rotation.calculate(drive), 1e-9);
    stick[0] = 0.01; // inside the deadband: back to the PID
    assertTrue(rotation.calculate(drive) > 0.0);
  }

  @Test
  void headingStickHoldsTheLastHeadingWhenReleased() {
    FakeDrive drive = new FakeDrive(0, 0, 0);
    double[] x = {0.0};
    double[] y = {0.0};
    HeadingStickRotation rotation = new HeadingStickRotation(() -> x[0], () -> y[0]);
    rotation.reset(drive);
    // Released from the start: holds the current heading, so no rotation.
    assertEquals(0.0, rotation.calculate(drive), 1e-9);

    // Stick pointed at atan2(x, y) = 90 degrees.
    x[0] = 1.0;
    double turning = rotation.calculate(drive);
    assertTrue(turning > 0.0);

    // Let go: it keeps turning toward the same target.
    x[0] = 0.0;
    assertEquals(turning, rotation.calculate(drive), 1e-6);
  }

  // ---------------------------------------------------------------------------------------------
  // Translation
  // ---------------------------------------------------------------------------------------------

  @Test
  void throttleMapModifierSoftensSmallInputs() {
    Translation2d out =
        new ThrottleMapModifier().apply(new FakeDrive(0, 0, 0), new Translation2d(0.3, -1.0));
    assertEquals(0.15, out.getX(), 1e-9);
    assertEquals(-1.0, out.getY(), 1e-9);
  }

  @Test
  void targetAssistBlendsTowardTheTarget() {
    FakeDrive drive = new FakeDrive(0, 0, 0);
    TargetAssistModifier assist = new TargetAssistModifier(() -> new Translation2d(0, 5), 0.5);
    Translation2d out = assist.apply(drive, new Translation2d(1.0, 0.0));
    assertEquals(0.5, out.getX(), 1e-9);
    assertEquals(0.5, out.getY(), 1e-9);
  }

  @Test
  void targetAssistPointsTheRightWayForTheRedDriver() {
    setAlliance(AllianceStationID.Red1);
    FakeDrive drive = new FakeDrive(0, 0, 0);
    TargetAssistModifier assist = new TargetAssistModifier(() -> new Translation2d(5, 0), 1.0);
    // The target is at +X on the field, which is "backwards" (-X) for the red driver.
    assertEquals(-1.0, assist.apply(drive, new Translation2d()).getX(), 1e-9);
  }

  @Test
  void repulsionOnlyActsInsideTheZone() {
    ZoneRepulsionModifier repulsion = new ZoneRepulsionModifier(ZONE, 0.5);
    Translation2d input = new Translation2d(0.2, 0.0);
    assertEquals(input, repulsion.apply(new FakeDrive(0, 0, 0), input));

    // Inside, to the right of the center: pushed further right (+X).
    Translation2d out = repulsion.apply(new FakeDrive(5.5, 4.0, 0), new Translation2d());
    assertEquals(0.5, out.getX(), 1e-9);
    assertEquals(0.0, out.getY(), 1e-9);
  }

  @Test
  void repulsionPushesOutOfTheZoneForTheRedDriverToo() {
    setAlliance(AllianceStationID.Red1);
    ZoneRepulsionModifier repulsion = new ZoneRepulsionModifier(ZONE, 0.5);
    Translation2d out = repulsion.apply(new FakeDrive(5.5, 4.0, 0), new Translation2d());
    // Field +X is driver -X on red.
    assertEquals(-0.5, out.getX(), 1e-9);
  }

  @Test
  void suctionPullsThroughTheZoneAndCentersSideways() {
    ZoneSuctionModifier suction = new ZoneSuctionModifier(ZONE, 0.5);
    Translation2d input = new Translation2d(0.3, 0.3);
    assertEquals(input, suction.apply(new FakeDrive(0, 0, 0), input));

    // Entered from the left (x < center), 0.1 m below the center line.
    Translation2d out = suction.apply(new FakeDrive(4.5, 3.9, 0), input);
    assertTrue(out.getX() > 0.0, "keeps going through the zone");
    assertTrue(out.getY() > 0.0, "pulled up toward the center line");
    assertTrue(out.getNorm() <= 0.5 + 1e-9);

    // The direction is latched on entry, even after passing the center.
    assertTrue(suction.apply(new FakeDrive(5.5, 4.0, 0), input).getX() > 0.0);
  }

  @Test
  void suctionIsMirroredForTheRedDriver() {
    setAlliance(AllianceStationID.Red1);
    ZoneSuctionModifier suction = new ZoneSuctionModifier(ZONE, 0.5);
    Translation2d out = suction.apply(new FakeDrive(4.5, 4.0, 0), new Translation2d());
    // Field +X (through the zone) is driver -X on red.
    assertTrue(out.getX() < 0.0);
  }

  @Test
  void dualZoneSuctionPicksTheZoneTheRobotIsIn() {
    Polygon2d other =
        new Polygon2d(
            new Translation2d(10.0, 3.0),
            new Translation2d(12.0, 3.0),
            new Translation2d(12.0, 5.0),
            new Translation2d(10.0, 5.0));
    DualZoneSuctionBumpModifier bump = new DualZoneSuctionBumpModifier(ZONE, other, 0.4);
    Translation2d input = new Translation2d(0.1, 0.1);

    assertEquals(input, bump.apply(new FakeDrive(8.0, 4.0, 0), input));
    // Entered the second zone from the right: pulled to -X.
    assertEquals(-0.4, bump.apply(new FakeDrive(11.5, 4.0, 0), input).getX(), 1e-9);

    setAlliance(AllianceStationID.Red1);
    bump = new DualZoneSuctionBumpModifier(ZONE, other, 0.4);
    // Same field direction, seen from the red wall.
    assertEquals(0.4, bump.apply(new FakeDrive(11.5, 4.0, 0), input).getX(), 1e-9);
  }
}
