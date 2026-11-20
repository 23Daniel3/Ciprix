# Vision

Vision has two levels: the library's ready-made pipeline, which works for any team, and the
extra rules each team can add on their own robot.

## Level 1: `AprilTagVision` (library)

Works with Limelight without needing `LimelightHelpers`:

```java
vision =
    new AprilTagVision(
            drivetrain::addVisionMeasurement, drivetrain::getPose, () -> drivetrain.getState().Speeds)
        .withCamera("Front", new VisionIOLimelight("limelight-front", drivetrain::getRotation))
        .withCamera("Back", new VisionIOLimelight("limelight-back", drivetrain::getRotation), 1.5)
        .withFilter(
            new VisionFilter()
                .field(AprilTagFieldLayout.loadField(AprilTagFields.k2026RebuiltWelded))
                .maxDistance(4.0));
```

- `VisionIOLimelight` reads every new frame from the NetworkTables queue, so none is lost and
  none is used twice, and delivers both MegaTag 1 and MegaTag 2 poses.
- `VisionFilter` rejects, with a reason: pose outside the field, robot off the ground or tilted,
  tags too far away, an ambiguous single tag (MegaTag 1), too fast a turn for MegaTag 2, and, if
  enabled with `maxJump(meters)`, a single tag "jumping" far from the current estimate while it's
  still trusted. After 1 s with no accepted pose the estimate stops being trusted and the robot
  re-finds itself.
- `VisionStdDevs` computes the standard deviation as distance² divided by the number of tags. On
  MegaTag 2 the angle comes from the gyro itself, so the angle's standard deviation is ignored.
- The number passed to `withCamera` (1.5 above) multiplies the deviations of a less trustworthy
  camera.

In the log, each camera has `Vision/<camera>/AcceptedPoses`, `RejectedPoses`, `LastRejection`,
`AcceptedCount`, `RejectedCount` and `TagPoses`, and a camera that stops responding shows up in
the hardware alerts. In replay, use `new VisionIO() {}` for each camera.

## Level 2: your robot's rules

When a team needs a tighter filter, keep a `Vision` subsystem in the robot project with the
team's own maps (distance, velocity, ambiguity) and add the library's rules on top. Rules that
tend to be worth it:

- **Judge each frame once.** If you read the Limelight as a "last value" every cycle, the same
  frame enters the estimator across several cycles and the estimator trusts it too much.
- **Season field bounds**, robot on the ground, fast turns, and single-tag jumps
  (`maxJump(meters)`).
- **Trust MegaTag 1's angle only with 2 or more tags.** Otherwise use the gyro's angle and ignore
  the angle's standard deviation: correcting the gyro with itself only injects the camera's
  latency.
- Log the reason for each rejection (for example `Vision/<CAMERA>/RejectReason`).

To tune: look at the rejection reasons and the poses in AdvantageScope after a practice run. If a
rule rejects too many good frames, loosen its constant.

## Field

Use the layout for the event's field: `AprilTagFields.k2026RebuiltAndymark` or
`AprilTagFields.k2026RebuiltWelded`. The filter's bounds come from that layout.
