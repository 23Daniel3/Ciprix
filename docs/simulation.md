---
sidebar_position: 60
---

# Simulation

Simulation uses the same configuration and motor names as the robot, so state machines, gains,
alerts and logs behave the same way. `Simulate Robot Code` in VS Code (or `./gradlew
simulateJava`) opens the simulation; enable the robot in the simulation window.

## Motors with physics

`MotorIOSim` mimics the real controller (velocity/position loop, Motion Magic/Smart Motion,
kS/kV/kA/kG, current limit, un-jam, health) on top of a physical model of the mechanism:

```java
private final MotorIOSim flywheel =
    MotorIOSim.talonFX(
            "Shooter/Leader",
            ShooterConstants.MOTOR_LEADER_CONFIG,
            SimMechanism.flywheel(DCMotor.getKrakenX60(2), 1.0, 0.004).withFriction(0.03, 0.0))
        .withFollowers(1);
```

| Mechanism | Factory |
| --- | --- |
| Roller, conveyor | `SimMechanism.roller(motor, reduction, inertia)` |
| Flywheel | `SimMechanism.flywheel(motor, reduction, inertia)` |
| Arm, pivot | `SimMechanism.arm(motor, reduction, massKg, lengthM)`, with gravity |
| Elevator | `SimMechanism.elevator(motor, reduction, massKg, drumRadiusM)`, with gravity |

Adjustments: `withFriction(coulombNm, viscous)`, `withLimits(minRot, maxRot)` (hard stops),
`withStartPosition(rot)`, `withGravity(...)` and `withLoad(...)` (an attached piece, for
example). `MotorIOSim.talonFX(...)` uses Talon units and gains; `MotorIOSim.spark(...)` uses
SPARK's (kV in V/RPM, as in the REV Hardware Client).

To test how the robot reacts to problems: `setConnected(false)`, `setFaults("...")` and
`setTemperatureCelsius(95)` trigger the same alerts as the real robot.

## Battery

Every simulated motor adds its current draw to `SimBattery`, which sags the voltage like a real
battery. That's what makes [power management](hardware-power-and-pit.md#low-battery) kick in
during simulation too.

## Replay

With `Constants.simMode = Mode.REPLAY`, the robot re-runs an AdvantageKit log: the IOs are empty
and inputs come from the log. This is useful for testing a logic change (a vision filter, an FSM
transition) against the data from a real match, and comparing it in AdvantageScope.

## 3D robot

The CAD's moving parts follow the mechanisms (`RobotVisualizer` and `MechanismPoses`). The
step-by-step guide is in [3D Robot](3d-robot.md).

## Tests without a robot

`core/src/test` has the library's tests. Most run without the HAL, with `TestEnvironment`
controlling the clock, the enabled state and the battery. It's the fastest way to prove a new
rule works before taking it to the robot.
