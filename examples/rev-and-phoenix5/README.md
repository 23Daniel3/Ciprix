# Example: the same subsystem on REV and on CTRE Phoenix 5

A velocity-controlled flywheel that runs on a **REV SPARK MAX + NEO** or on a **CTRE Talon SRX +
quadrature encoder**. Switching vendor is one line, `Constants.FLYWHEEL_VENDOR`; the subsystem,
its states, the SuperStructure and the simulation do not change.

What it shows:

- `Flywheel` only calls `io.controlMotor().runVelocity(...)`. It has no idea which controller
  is behind it.
- One IO class per vendor (`FlywheelIOSparkMax`, `FlywheelIOTalonSRX`) plus `FlywheelIOSim`,
  all with the motor name `Flywheel/Motor`, so the logs of the robot and of the simulation line
  up.
- One `MotorConfig` per vendor in `FlywheelConstants`. The fields mean the same thing; the **gains
  do not**, because each controller expects them in its own units. The Talon SRX has no kS, kA or
  kG (Ciprix warns once if you set them) and needs `countsPerRevolution` for its encoder.
- The vendordeps needed: `REVLib.json` and `Phoenix5-*.json` next to `Ciprix.json`.

## Controls

Hold the right bumper of controller 0 to shoot (the flywheel spins up to 3000 RPM and reports
`READY` when it is within 100 RPM). The co-driver can hold A to spin it up regardless.

The simulation uses the SPARK MAX configuration. The gains are starting points, not tuned
values: tune them on your robot.
