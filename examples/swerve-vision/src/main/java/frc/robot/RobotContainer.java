package frc.robot;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.subsystems.drive.Drive;
import frc.robot.subsystems.drive.DriveConstants;
import frc.robot.subsystems.vision.VisionIOSim;
import java.util.List;
import org.ciprix.CiprixSettings;
import org.ciprix.controller.NaturalXboxController;
import org.ciprix.controller.UtilCalculus;
import org.ciprix.util.AllianceFlipUtil;
import org.ciprix.util.drivetrain.HeadingStickRotation;
import org.ciprix.util.drivetrain.ManualRotation;
import org.ciprix.util.drivetrain.PIDRotation;
import org.ciprix.util.drivetrain.RotationStrategy;
import org.ciprix.util.drivetrain.TargetAssistModifier;
import org.ciprix.util.drivetrain.ThrottleMapModifier;
import org.ciprix.util.drivetrain.TranslationModifier;
import org.ciprix.util.drivetrain.ZoneRepulsionModifier;
import org.ciprix.vision.AprilTagVision;
import org.ciprix.vision.VisionFilter;
import org.ciprix.vision.VisionIO;
import org.ciprix.vision.VisionIOLimelight;

/**
 * Driving with the library's helpers and AprilTag vision. There is no SuperStructure here: the
 * request/state pattern is in the other examples, and this one is about the drivetrain.
 */
public class RobotContainer {

  private final Drive drive = new Drive();
  private final AprilTagVision vision;
  private final NaturalXboxController pilot = new NaturalXboxController(0);

  private final TranslationModifier targetAssist =
      new TargetAssistModifier(() -> AllianceFlipUtil.apply(DriveConstants.TARGET), 0.35);

  /** Chained in order; each one receives the previous one's output (driver frame, [-1, 1]). */
  private final List<TranslationModifier> modifiers =
      List.of(
          new ThrottleMapModifier(),
          new ZoneRepulsionModifier(DriveConstants.KEEP_OUT, 0.6),
          // Hold the left bumper to pull the robot toward the target.
          (context, input) ->
              pilot.leftBumper().getAsBoolean() ? targetAssist.apply(context, input) : input);

  private final RotationStrategy manualRotation =
      new ManualRotation(pilot::getRightX, () -> DriveConstants.MAX_ANGULAR_SPEED_RAD_PER_SEC);
  private final RotationStrategy headingRotation =
      new HeadingStickRotation(pilot::getRightX, pilot::getRightY);
  private final RotationStrategy aimRotation =
      new PIDRotation(
          () ->
              AllianceFlipUtil.apply(DriveConstants.TARGET)
                  .minus(drive.getPose().getTranslation())
                  .getAngle());

  private RotationStrategy rotation = manualRotation;

  public RobotContainer() {
    VisionIO camera =
        switch (CiprixSettings.getMode()) {
          case REAL ->
              new VisionIOLimelight("limelight-front", () -> drive.getPose().getRotation());
          case SIM -> new VisionIOSim(drive::getTruePose);
          case REPLAY -> new VisionIO() {};
        };
    vision =
        new AprilTagVision(drive::addVisionMeasurement, drive::getPose, drive::getRobotVelocity)
            .withCamera("Front", camera)
            .withFilter(
                new VisionFilter()
                    .field(AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltWelded))
                    .maxDistance(4.0)
                    .maxJump(1.5));

    configureBindings();
  }

  private void configureBindings() {
    drive.setDefaultCommand(driveCommand());

    // Rotation: the stick turns the robot by default; hold A to point it where the stick points,
    // hold the right bumper to aim at the target.
    pilot
        .a()
        .whileTrue(Commands.startEnd(() -> select(headingRotation), () -> select(manualRotation)));
    pilot
        .rightBumper()
        .whileTrue(Commands.startEnd(() -> select(aimRotation), () -> select(manualRotation)));
  }

  private void select(RotationStrategy strategy) {
    rotation = strategy;
    rotation.reset(drive);
  }

  private Command driveCommand() {
    return Commands.run(
            () -> {
              Translation2d input =
                  UtilCalculus.getLinearVelocityFromJoysticks(pilot.getLeftY(), pilot.getLeftX());
              for (TranslationModifier modifier : modifiers) {
                input = modifier.apply(drive, input);
              }
              double max = DriveConstants.MAX_SPEED_MPS;
              // Driver frame in [-1, 1] -> field-relative speeds in m/s and rad/s.
              drive.runFieldRelative(
                  AllianceFlipUtil.apply(
                      drive, input.getX() * max, input.getY() * max, rotation.calculate(drive)));
            },
            drive)
        .withName("Drive with helpers");
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
