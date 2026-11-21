---
sidebar_position: 90
---

# Migrating to WPILib 2027

> Based on the official 2027 changelog, still in **alpha** (Alpha 8, page updated 2026-10-30).
> Before migrating, check the final version at
> [New for 2027](https://docs.wpilib.org/en/latest/docs/yearly-overview/yearly-changelog.html) and
> [Removed features](https://docs.wpilib.org/en/latest/docs/yearly-overview/removed-features.html).

## What stays the same

The pattern doesn't change: the driver writes requests, the SuperStructure decides the robot's
state, each subsystem has its own state machine, the co-driver overrides a subsystem while
holding a button, and hardware is only ever reached through IO. Ciprix's state machine doesn't
depend on the command scheduler, and `StateSubsystem` is still a command-based "v2"
`SubsystemBase`, which keeps existing in 2027. This migration is about names and APIs, not
architecture.

## What changes in 2027 (and where it affects us)

| Change | Where it shows up here |
| --- | --- |
| roboRIO replaced by the **Systemcore**, with several CAN buses | CAN devices start taking a `CANPort`; the CTRE and REV IOs follow whatever the vendors ship for 2027 |
| **Java 25** and Gradle 9.4.1 | `sourceCompatibility` in Ciprix's `build.gradle` and in each project's `build.gradle` |
| `edu.wpi.first` packages become `org.wpilib` | Every file; the VS Code importer tries to rename them automatically |
| `DriverStation` split into `MatchState` and `RobotState` | If your project defines `Constants.RobotState`, it will share a name with the new WPILib class: rename it to `SuperState` during migration (if `RobotContainer` imports `wpilibj.*`) |
| SmartDashboard, `SendableChooser` and `Sendable` replaced by Telemetry and Tunables; Shuffleboard removed | `SelfTest/...` buttons, `LoggedDashboardChooser` (depends on AdvantageKit 2027), `PersistentAlert` |
| Timestamps are integer **nanoseconds** | `VisionIOLimelight` (`getFPGATime()` and NetworkTables timestamps, in microseconds) |
| `Rotation2d.getDegrees()`/`getRadians()` return the "wrapped" angle | Check any math that relies on a continuous angle (there's a `ContinuousUnwrapper` for this) |
| `MathUtil.clamp` is gone, use `Math.clamp`; `Pose.exp(twist)` becomes `plus(twist.exp())` | 6 library files (acceleration limiters, anti-tipping, `ThrottleMap`, `SimMechanism`) and any drivetrain code in your robot project that calls them |
| `MotorController.set()` becomes `setThrottle()`; PWM controllers move to a drivers library | The library's PWM IOs (`interfaces/motor/pwm`) |
| AprilTag and CameraServer become vendordeps; `AprilTagFields` moves into a `Fields` class | Your robot's vision constants and the `VisionFilter.field(...)` example |
| Jackson replaced by Avaje Jsonb | If your robot includes `LimelightHelpers` (which uses Jackson), swap it for the 2027 version of the file, or use the library's `VisionIOLimelight`, which doesn't need it |
| Test mode becomes **Utility**; `robotInit()` is gone (we already use the constructor) | `Robot.java` |
| Counters, servo, SPI, analog gyros and other devices leave the Systemcore | Nothing here uses them |

## Commands v3

2027 brings a new command-based framework ("v3") with a declarative state machine and a
`Mechanism` interface, documented alongside v2. For the 2027 season, the safe path is to stay on
v2. In the offseason, it's worth evaluating a `StateSubsystem` built on top of a v3 `Mechanism`
with the same API (`fsm`, `bindRequest`, `override`, `selfTest`), so the robot code doesn't need
to change at all.

## Suggested order

1. A `wpilib-2027` branch, with the WPILib 2027 VS Code extension.
2. `core` first: switch the GradleRIO version, let the importer rename the packages, update the
   table above and run the tests (most run without the HAL).
3. `ctre`, `rev` and `phoenix5` once CTRE and REV publish their 2027 releases (Phoenix 5 may not
   have a Systemcore version).
4. `templates/skeleton`, which is small and quickly shows what's still missing.
5. Each team's robot: if your code uses `RobotState` or `LimelightHelpers`, rename `RobotState` to
   `SuperState` and update `LimelightHelpers`, then check the whole simulation and replay a 2026
   log.
6. Only then power it up on the Systemcore, with the robot suspended.
