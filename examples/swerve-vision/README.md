# Example: driving helpers and vision

Everything the library offers around the drivetrain, on a drivetrain simulated as an ideal
holonomic base, so it runs anywhere with `Simulate Robot Code`.

| What | Where |
| --- | --- |
| `DriveContext`: the only contract between your drivetrain and the helpers | `Drive.java` |
| Translation modifiers chained in order: `ThrottleMapModifier`, `ZoneRepulsionModifier`, `TargetAssistModifier` | `RobotContainer.modifiers` |
| Rotation strategies you can swap at runtime: `ManualRotation`, `HeadingStickRotation`, `PIDRotation` | `RobotContainer` |
| The driver frame and the single conversion to field speeds at the end | `RobotContainer.driveCommand()` |
| `AprilTagVision` with `VisionFilter` (field bounds, max distance, `maxJump`) | `RobotContainer`, `VisionIOSim` |
| A simulated camera that sometimes sends a bad pose, so you can see the rejection reasons | `VisionIOSim` |

## Controls (controller 0)

Left stick drives, right stick turns. Hold **A** to point the robot where the right stick points.
Hold the **right bumper** to aim at the target and the **left bumper** to pull the robot toward it.

## Watching the vision work

Open AdvantageScope and plot `Drive/TruePose` and `Drive/EstimatedPose`. The simulated odometry
drifts when you drive; the camera pulls the estimate back. Under `Vision/Front` you will see
`AcceptedPoses`, `RejectedPoses` and `LastRejection`: every 5 seconds the simulated camera sends a
pose outside the field, and the filter rejects it with the reason.

## What this example is not

- It is not a swerve implementation. For a real one, keep your own drivetrain class (CTRE's
  generated swerve, YAGSL, ...), make it implement `DriveContext`, and pass its pose estimator's
  `addVisionMeasurement` to `AprilTagVision`.
- It has no PathPlanner setup, because that needs your robot's paths and configuration.
- On the real robot it reads a Limelight named `limelight-front`: change the name to yours.

The request/state pattern for mechanisms is in the other examples and in the
[skeleton](../../templates/skeleton).
