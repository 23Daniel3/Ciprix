---
sidebar_position: 20
---

# Hardware, power and pit

## Hardware alerts and the blinking screen

Every motor the code reads (and every camera in `AprilTagVision`) feeds into
`HardwareHealthMonitor`, which publishes:

| Key (`/AdvantageKit/RealOutputs/...`) | What it is |
| --- | --- |
| `Hardware/Blink` | Toggles at 2.5 Hz while something is wrong; stays false once everything is fine |
| `Hardware/Healthy` | True when everything is connected, with no faults and no problems |
| `Hardware/Disconnected` | Names of the devices that aren't responding |
| `Hardware/Problems` | One line per fault or problem, like `Intake/Roller: Over temperature` |

To make the driver's screen flash red: in Elastic, a **Boolean Box** bound to `Hardware/Blink`,
red for true and the background color for false. Next to it, a list showing `Hardware/Problems`
says what it is. `HardwareHealthMonitor.blink()` also works for an LED effect.

Motors the code never reads (followers, for example) aren't judged, and nothing counts as
disconnected during the first 5 s after power-on.

## Automatic un-jam

```java
new MotorConfig().withStallReversal(Amps.of(30), 0.2, 0.3, 0.5)
```

If the motor is being commanded, draws over 30 A and barely turns for 0.2 s, it reverses at 50%
for 0.3 s and then returns on its own to the last command the code actually requested. While it
reverses, the code's commands are kept, not dropped; a `stop()` still stops it immediately.
`inputs.stalled` and `inputs.unjamming` show up in the log.

## Low battery

`PowerManager` watches the filtered battery voltage and, below 9.5 V, lowers the current limit
of `NORMAL` and `LOW` priority motors and smooths the drivetrain's acceleration; below 8 V it
cuts further. It steps back up one level at a time once the battery stays 0.5 V above the
threshold for 0.5 s.

| Priority | Normal | Below 9.5 V | Below 8 V |
| --- | --- | --- | --- |
| `HIGH` (default) | 100% | 100% | 100% |
| `NORMAL` | 100% | 75% | 50% |
| `LOW` | 100% | 50% | 25% |

```java
new MotorConfig().currentLimit(Amps.of(35)).powerPriority(PowerPriority.LOW)
```

A common setup: rollers and conveyors as `LOW`, and match-deciding mechanisms (shooter,
drivetrain) without reduction (the drivetrain only gets smoother acceleration). In the log:
`Power/Level` and `Power/FilteredVoltage`.

## Pit self-test

Each subsystem declares, alongside its state machine, which motors can spin freely for a moment:

```java
selfTest()
    .motor("Roller", io::controlRollerMotor, () -> inputs.rollerMotorInputs)
    .output(0.3)
    .currentBetween(Amps.of(0.3), Amps.of(25))
    .mustTurn(RotationsPerSecond.of(5));
```

With the robot enabled, the `SelfTest/<subsystem>` and `SelfTest/All` dashboard buttons spin each
motor and check that it responds, draws a reasonable current and actually turns. It catches a
disconnected motor or wire, a broken coupling, a dead encoder and a jammed mechanism. During the
test the subsystem's state machine is paused and, at the end, restarts from its initial state.
The result shows up at `SelfTest/<subsystem>/<motor>` and a failure becomes an alert. Don't use it
on an arm or elevator, which would hit its hard stop.

## Pre-match checklist

With the robot disabled, `PreMatchChecklist` shows what's still missing before the match:

```java
PreMatchChecklist.addDefaults(); // battery >= 12.4 V, devices connected, no faults
PreMatchChecklist.add("Alliance set", () -> DriverStation.getAlliance().isPresent());
PreMatchChecklist.add("Vision located the robot", vision::hasRecentPose);
```

In Elastic: a Boolean Box on `PreMatch/Ready` and a list on `PreMatch/Pending`. A warning alert
lists what's pending. Once the robot is enabled, the checklist stops and the alert disappears.
