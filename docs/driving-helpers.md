# Driving helpers

Package `org.ciprix.util.drivetrain`. Small, composable pieces that sit between the driver's
sticks and the drivetrain. The library never depends on your drivetrain class: your drive
subsystem implements `DriveContext` and every helper works with it.

```java
public class Drive extends SubsystemBase implements DriveContext, DriveAccelerationLimits {
  @Override public Pose2d getPose() { ... }
  @Override public ChassisSpeeds getRobotVelocity() { ... }
  @Override public LinearVelocity getMaxSpeed() { ... }
  @Override public AngularVelocity getMaxAngularSpeed() { ... }
  // DriveAccelerationLimits is only needed for anti-tipping, see Safety.
}
```

## The driver frame

Translation modifiers work in the **driver frame**: the joystick vector normalized to [-1, 1],
where +X points away from the driver's own alliance wall. It is the same on both alliances, so
modifiers chain without caring about the color. Convert to field speeds **once**, at the end, with
`AllianceFlipUtil.apply(drivetrain, vx, vy, omega)`. That method expects **m/s and rad/s**, not
the normalized values, so multiply the modifiers' output by `drivetrain.getMaxSpeed()` first
(and the rotation output of a `RotationStrategy`, which is already in rad/s, goes in as `omega`
unchanged).

A modifier that computes a direction from field positions (a target, a zone center) must convert
it first with `TranslationModifier.fieldToDriver(...)`.

## Translation modifiers

A `TranslationModifier` is one method: `Translation2d apply(DriveContext, Translation2d input)`.

| Modifier | Effect |
| --- | --- |
| `ThrottleMapModifier` | Non-linear stick curve (fine control at low speed). |
| `TargetAssistModifier(targetSupplier, weight)` | Blends the stick toward a field target. |
| `ZoneSuctionModifier(zone, strength)` | Inside the zone, pulls the robot through its center line and across it. |
| `ZoneRepulsionModifier(zone, strength)` | Pushes the robot away from a zone it should not enter. |
| `DualZoneSuctionBumpModifier(zoneA, zoneB, strength)` | Guides the robot from one zone to the other (crossing a bump, for example). |

Chain them in order:

```java
List<TranslationModifier> modifiers = List.of(
    new ThrottleMapModifier(),
    new ZoneRepulsionModifier(wallZone, 0.6),
    new TargetAssistModifier(() -> FieldPositions.hub(), 0.3));

Translation2d input = UtilCalculus.getLinearVelocityFromJoysticks(driver.getLeftY(), driver.getLeftX());
for (TranslationModifier m : modifiers) {
  input = m.apply(drive, input);
}

// `rotation` is any RotationStrategy (see below). Normalized [-1, 1] -> m/s, then driver frame -> field-relative speeds.
double maxSpeed = drive.getMaxSpeed().in(MetersPerSecond);
ChassisSpeeds speeds = AllianceFlipUtil.apply(
    drive, input.getX() * maxSpeed, input.getY() * maxSpeed, rotation.calculate(drive));
```

## Rotation strategies

A `RotationStrategy` returns the angular velocity each cycle with `calculate(DriveContext)` and can
be reset with `reset(DriveContext)` when you switch to it.

| Strategy | Behavior |
| --- | --- |
| `ManualRotation(stick, maxSpeed)` | The driver's rotation stick. |
| `PIDRotation(targetSupplier[, manualControl])` | Holds a heading with a PID. If a manual supplier is given, the stick takes over while it is outside the deadband. The output is limited to the drivetrain's max angular speed. |
| `HeadingStickRotation(xStick, yStick)` | Points the robot where the right stick points. |

The PID gains are shared by every `PIDRotation` and tunable under `Drive/RotationPID`. To start
from your own gains call `PIDRotation.setDefaultGains(kP, kI, kD)` once, before creating the first
instance.

## Acceleration limits

`DriveAccelerationLimits` is the small interface the safety features use to change the
drivetrain's acceleration limits at runtime. See [Safety](safety.md).
