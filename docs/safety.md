---
sidebar_position: 40
---

# Safety

Package `org.ciprix.util.security`. Protections that keep the robot and its mechanisms alive.
For hardware alerts and the pit checklist, see [Hardware, power and pit](hardware-power-and-pit.md).

## Slew rate limiters

`DynamicSlewRateLimiter` limits how fast a velocity can change, like WPILib's `SlewRateLimiter`,
but the acceleration limits can change every cycle and the API uses WPILib units.

```java
DynamicSlewRateLimiter limiter =
    new DynamicSlewRateLimiter(MetersPerSecondPerSecond.of(6.0));

LinearVelocity vx = limiter.calculate(MetersPerSecond.of(requestedVx));

// Asymmetric limits (accelerate and decelerate differently), changeable at any time:
limiter.setAccelerationLimits(MetersPerSecondPerSecond.of(6.0), MetersPerSecondPerSecond.of(9.0));
limiter.reset(MetersPerSecond.of(0.0));
```

`AngularDynamicSlewRateLimiter` is the same for angular velocity.

## Current spikes and locked motors

Both detectors are `BooleanSupplier`s and can also become a `Trigger` with `asTrigger()`.

```java
// True when the current stays above 60 A for 0.25 s.
CurrentSpikeDetector spike = new CurrentSpikeDetector(60.0, 0.25);
boolean tripped = spike.update(motorInputs.current.in(Amps));

// True when the motor should be moving but is below 0.5 rot/s for 0.3 s.
// Never trips while the robot is disabled.
LockedMotorDetector locked = new LockedMotorDetector(0.3, 0.5);
locked.update(commandedSpeed != 0, motorInputs.velocity.in(RotationsPerSecond));
```

Call `update(...)` once per cycle.

## Anti-tipping

Two options. Both need the IMU's rotation, and both return `Optional<ChassisSpeeds>` with a
correction to add to the driver's speeds when the robot is tipping.

- `AntiTipping(kP, thresholdDegrees, maxCorrectionSpeed)`: proportional correction toward the low
  side of the tilt, robot-relative (a positive pitch drives +X, a positive roll drives -Y).
- `AntiTippingAccel(config)`: also estimates risk from the IMU's acceleration and your robot's
  center of mass, and **lowers the drivetrain's acceleration limits** before the tip happens. Your
  drivetrain must implement `DriveContext` and `DriveAccelerationLimits`. Tune it through
  `AntiTippingAccel.AntiTippingConfig` (center of mass height and offsets, thresholds, weights).
  `getLastState()` returns a record that is ready to log.

```java
AntiTippingAccel antiTip = new AntiTippingAccel(new AntiTippingAccel.AntiTippingConfig());

Optional<ChassisSpeeds> fix =
    antiTip.calculate(
        imu.getRotation3d(),
        MetersPerSecondPerSecond.of(imu.getAccelX()), // your IMU's reading, converted to units
        MetersPerSecondPerSecond.of(imu.getAccelY()),
        drive);
// addCorrection is a method of your own drivetrain, not of the library: add the speeds
// to the driver's request however your drive code does it.
fix.ifPresent(speeds -> drive.addCorrection(speeds));
```

Call `reset()` when the robot is re-enabled so the limits and smoothing start clean.

## CAN health

`CANHealthMonitor` takes the devices **in the physical order of the CAN chain** and logs the most
likely failure point (before the first device, or between two devices) when one stops answering.

```java
CANHealthMonitor can = new CANHealthMonitor("rio",
    new CANHealthMonitor.CANDeviceStatus("Gyro", 1, imu::isConnected),
    new CANHealthMonitor.CANDeviceStatus("FL Drive", 2, flDrive::isConnected));

// robotPeriodic:
can.checkHealth();
```
