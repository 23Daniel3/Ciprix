# Robot skeleton (Ciprix)

The smallest complete robot in the framework's pattern: one subsystem (`Roller`), the
`SuperStructure` with the overall state machine, the driver commands and the co-driver backup.
Use it as the starting point for a new robot.

```
src/main/java/frc/robot/
  Robot.java                     AdvantageKit + Ciprix setup
  RobotContainer.java            subsystems, buttons, autonomous
  Constants.java                 mode, RobotRequest, RobotState, field
  commands/SuperStructureCommands.java   driver buttons -> RobotRequest
  subsystems/superstructure/SuperStructure.java   overall FSM
  subsystems/roller/             one subsystem: Constants, IO, IOTalonFX, IOSim, Roller
```

## Starting a new robot

1. Copy this folder outside the Ciprix repository (or download it from GitHub).
2. In `.wpilib/wpilib_preferences.json`, change `teamNumber` to your team's number.
3. In `settings.gradle`, change `rootProject.name`.
4. Open the folder in the WPILib VS Code extension. `Simulate Robot Code` already works: the
   simulated roller spins with the driver's left bumper (controller 0) and reverses with the
   right one.

Outside the Ciprix repository, the library's source two levels up doesn't exist, so Gradle
downloads the published version listed in `vendordeps/Ciprix*.json`.

## Adding a subsystem

Follow `Roller` as a model, always in this order:

1. `XxxConstants`: ids, motor configurations (`MotorConfig`), `enum XxxRequest` and
   `enum XxxState`.
2. `XxxIO`: the inputs with `@AutoLog` and one `controlYyy()` per motor, returning
   `MotorControllerNone` by default (that's what runs in replay).
3. `XxxIOTalonFX` (or `XxxIOSparkMax`, ...) and `XxxIOSim`, with the same configuration and the
   same motor name, so the simulation and robot logs line up.
4. `Xxx extends StateSubsystem<...>` with `@AutoCommandFactory`: one `fsm.state(...)` per state,
   one `bindRequest(...)` per request, and `fsm.validateComplete()` at the end.
5. In `SuperStructure`: an `xxxRequest` field, the `getXxxRequest()` getter (the generated
   command reads this name), and each overall state's request in its `onEnter`.
6. In `RobotContainer`: `xxx.setDefaultCommand(XxxCommands.defaultCommand(superStructure, xxx))`
   and, for the co-driver, `XxxCommands.<request>(xxx)` on the backup buttons.

The full guide to the pattern is in [`docs/architecture.md`](../../docs/architecture.md).
