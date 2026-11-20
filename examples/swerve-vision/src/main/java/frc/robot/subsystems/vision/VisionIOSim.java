package frc.robot.subsystems.vision;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.wpilibj.Timer;
import java.util.Random;
import java.util.function.Supplier;
import org.ciprix.vision.PoseObservation;
import org.ciprix.vision.PoseObservation.Type;
import org.ciprix.vision.VisionIO;

/**
 * A simulated camera: ten times per second it "sees" two tags and reports the true robot pose with
 * some noise. Every few seconds it also reports a bad pose (far outside the field), so you can see
 * {@code VisionFilter} reject it, with the reason, in {@code Vision/Front/LastRejection}.
 */
public class VisionIOSim implements VisionIO {
  private static final int FRAMES_PER_SECOND = 10;
  private static final int BAD_FRAME_EVERY = 50;

  private final Supplier<Pose2d> truePose;
  private final Random random = new Random(7);
  private int cycle = 0;
  private int frames = 0;

  public VisionIOSim(Supplier<Pose2d> truePose) {
    this.truePose = truePose;
  }

  @Override
  public void updateInputs(VisionIOInputs inputs) {
    inputs.connected = true;
    cycle++;
    if (cycle % (50 / FRAMES_PER_SECOND) != 0) {
      inputs.poseObservations = new PoseObservation[0];
      inputs.tagIds = new int[0];
      return;
    }
    frames++;
    Pose2d pose = truePose.get();
    boolean bad = frames % BAD_FRAME_EVERY == 0;
    double x = pose.getX() + random.nextGaussian() * 0.03 + (bad ? 40.0 : 0.0);
    double y = pose.getY() + random.nextGaussian() * 0.03;
    Rotation3d heading = new Rotation3d(0.0, 0.0, pose.getRotation().getRadians());
    inputs.poseObservations =
        new PoseObservation[] {
          new PoseObservation(
              Timer.getFPGATimestamp(),
              new Pose3d(x, y, 0.0, heading),
              0.05,
              2,
              2.5,
              Type.MEGATAG_1)
        };
    inputs.tagIds = new int[] {1, 2};
  }
}
