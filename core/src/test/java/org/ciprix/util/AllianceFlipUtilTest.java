package org.ciprix.util;

import static edu.wpi.first.units.Units.MetersPerSecond;
import static edu.wpi.first.units.Units.RadiansPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.AllianceStationID;
import edu.wpi.first.hal.HAL;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.units.measure.AngularVelocity;
import edu.wpi.first.units.measure.LinearVelocity;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import org.ciprix.CiprixSettings;
import org.ciprix.util.drivetrain.DriveContext;
import org.ciprix.zones.Polygon2d;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AllianceFlipUtilTest {

  private static final double LENGTH = 16.0;
  private static final double WIDTH = 8.0;

  private double oldLength;
  private double oldWidth;

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    oldLength = CiprixSettings.getFieldLengthMeters();
    oldWidth = CiprixSettings.getFieldWidthMeters();
    CiprixSettings.setFieldSize(LENGTH, WIDTH);
    setAlliance(AllianceStationID.Blue1);
  }

  @AfterEach
  void tearDown() {
    CiprixSettings.setFieldSize(oldLength, oldWidth);
    setAlliance(AllianceStationID.Blue1);
  }

  private static void setAlliance(AllianceStationID station) {
    DriverStationSim.setAllianceStationId(station);
    DriverStationSim.setDsAttached(true);
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();
  }

  @Test
  void flipMirrorsAcrossTheFieldSize() {
    assertEquals(13.0, AllianceFlipUtil.flipX(3.0), 1e-9);
    assertEquals(6.0, AllianceFlipUtil.flipY(2.0), 1e-9);
    assertEquals(new Translation2d(13.0, 6.0), AllianceFlipUtil.flip(new Translation2d(3.0, 2.0)));
    assertEquals(Rotation2d.fromDegrees(-150), AllianceFlipUtil.flip(Rotation2d.fromDegrees(30)));
  }

  @Test
  void flipPoseMirrorsPositionAndTurnsTheHeading() {
    Pose2d flipped = AllianceFlipUtil.flip(new Pose2d(1.0, 1.0, Rotation2d.kZero));
    assertEquals(15.0, flipped.getX(), 1e-9);
    assertEquals(7.0, flipped.getY(), 1e-9);
    assertEquals(180.0, Math.abs(flipped.getRotation().getDegrees()), 1e-9);
  }

  @Test
  void flip3dKeepsTheHeight() {
    Translation3d t = AllianceFlipUtil.flip(new Translation3d(1.0, 2.0, 0.5));
    assertEquals(new Translation3d(15.0, 6.0, 0.5), t);
    Rotation3d r = AllianceFlipUtil.flip(new Rotation3d(0.0, 0.0, 0.0));
    assertEquals(Math.PI, Math.abs(r.getZ()), 1e-9);
    Pose3d p = AllianceFlipUtil.flip(new Pose3d(t, new Rotation3d()));
    assertEquals(1.0, p.getX(), 1e-9);
    assertEquals(0.5, p.getZ(), 1e-9);
  }

  @Test
  void blueAllianceLeavesEverythingAlone() {
    assertFalse(AllianceFlipUtil.shouldFlip());
    assertEquals(3.0, AllianceFlipUtil.applyX(3.0), 1e-9);
    assertEquals(2.0, AllianceFlipUtil.applyY(2.0), 1e-9);
    Pose2d pose = new Pose2d(1.0, 2.0, Rotation2d.fromDegrees(45));
    assertEquals(pose, AllianceFlipUtil.apply(pose));
    assertEquals("blue", AllianceFlipUtil.resolve("blue", "red"));
  }

  @Test
  void redAllianceFlips() {
    setAlliance(AllianceStationID.Red2);
    assertTrue(AllianceFlipUtil.shouldFlip());
    assertTrue(AllianceSelector.getInstance().isRed());
    assertFalse(AllianceSelector.getInstance().isBlue());
    assertEquals(13.0, AllianceFlipUtil.applyX(3.0), 1e-9);
    assertEquals(new Translation2d(13.0, 6.0), AllianceFlipUtil.apply(new Translation2d(3.0, 2.0)));
    assertEquals("red", AllianceFlipUtil.resolve("blue", "red"));
  }

  @Test
  void useAllianceColorFalseForcesTheFlipOnBlue() {
    assertFalse(AllianceFlipUtil.shouldFlip());
    assertEquals(13.0, AllianceFlipUtil.applyX(3.0, false), 1e-9);
    assertEquals(6.0, AllianceFlipUtil.applyY(2.0, false), 1e-9);
    assertEquals(
        Rotation2d.fromDegrees(-150), AllianceFlipUtil.apply(Rotation2d.fromDegrees(30), false));
  }

  @Test
  void polygonFlipsOnlyTheRequestedAxes() {
    Polygon2d square =
        new Polygon2d(
            new Translation2d(1.0, 1.0),
            new Translation2d(2.0, 1.0),
            new Translation2d(2.0, 2.0),
            new Translation2d(1.0, 2.0));

    assertSame(square, AllianceFlipUtil.apply(square, true, true, true)); // blue: unchanged
    assertSame(square, AllianceFlipUtil.apply(square, false, false, false)); // no axis

    Translation2d[] xOnly = AllianceFlipUtil.apply(square, true, false, false).getVertices();
    assertEquals(new Translation2d(15.0, 1.0), xOnly[0]);

    Translation2d[] both = AllianceFlipUtil.apply(square, true, true, false).getVertices();
    assertEquals(new Translation2d(15.0, 7.0), both[0]);
  }

  @Test
  void driverRelativeSpeedsAreTurnedOnRed() {
    DriveContext drive = new FixedDrive(Rotation2d.kZero);
    ChassisSpeeds blue = AllianceFlipUtil.apply(drive, 1.0, 0.0, 0.0);
    assertEquals(1.0, blue.vxMetersPerSecond, 1e-9);

    setAlliance(AllianceStationID.Red1);
    ChassisSpeeds red = AllianceFlipUtil.apply(drive, 1.0, 0.0, 0.0);
    // The red driver stands on the other side, so "forward" is -X on the field.
    assertEquals(-1.0, red.vxMetersPerSecond, 1e-9);
    assertEquals(0.0, red.vyMetersPerSecond, 1e-9);
  }

  private record FixedDrive(Rotation2d heading) implements DriveContext {
    @Override
    public Pose2d getPose() {
      return new Pose2d(0.0, 0.0, heading);
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
      return RadiansPerSecond.of(6.0);
    }
  }
}
