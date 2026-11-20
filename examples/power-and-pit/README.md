# Example: power management, alerts and the pit checklist

How a robot protects itself and tells the drive team what is wrong, with almost no code: each item
is one line of `MotorConfig` or one `PreMatchChecklist.add`.

| What | Where |
| --- | --- |
| Power priority: the intake is `LOW`, the shooter `HIGH`, so a sagging battery cuts the intake's current first | `IntakeConstants`, `ShooterConstants` |
| Automatic un-jam (`withStallReversal`) and a warning for sustained current (`withCurrentWarning`) | `IntakeConstants` |
| A dashboard alert while the intake is un-jamming | `Intake.sPeriodic()` |
| Pre-match checklist: the defaults plus the robot's own items | `RobotContainer.configureChecklist()` |
| The pit self-test buttons | `SelfTest/Intake`, `SelfTest/Shooter` on the dashboard |
| A simulated battery you can drag down | `Sim/ExtraLoadAmps` on the dashboard |

## Try the low-battery protection in simulation

1. `Simulate Robot Code`, enable the robot in teleop, hold the left bumper to run the intake.
2. In AdvantageScope, plot `Power/FilteredVoltage`, `Power/Level` and the intake's current.
3. Set `Sim/ExtraLoadAmps` to 150 or more. The voltage sags, `Power/Level` leaves `NORMAL`,
   and the intake's current limit drops while the shooter's does not.
4. Set it back to 0: the level steps back up one level at a time.

## What to put on the driver's screen (Elastic)

| Widget | Key (`/AdvantageKit/RealOutputs/...`) |
| --- | --- |
| Boolean Box, red when true | `Hardware/Blink` |
| List | `Hardware/Problems` |
| Boolean Box | `PreMatch/Ready` |
| List | `PreMatch/Pending` |

Details of every key are in [Hardware, power and pit](../../docs/hardware-power-and-pit.md).
