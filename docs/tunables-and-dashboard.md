---
sidebar_position: 70
---

# Tunables and dashboard

Package `org.ciprix.logger`. Values you can change from the dashboard while testing, and choosers
for the autonomous selector.

## Tunable numbers

`LoggedTunableNumber` returns its default value in a match and the dashboard's value in
**tuning mode** (`CiprixSettings.isTuningMode()`, read on every call). Outside tuning mode no
NetworkTables entry is created and `hasChanged(...)` is always `false`, so it costs nothing.

```java
private final LoggedTunableNumber kP = new LoggedTunableNumber("Arm/kP", 4.0);

@Override
public void periodic() {
  // Apply only when a value actually changed. The id must be unique per consumer.
  LoggedTunableNumber.ifChanged(hashCode(), () -> motor.setPID(kP.get(), 0, 0), kP);
}
```

`LoggedTunableUnit` is the same for WPILib units; the key gets the unit symbol appended
(`"Flywheel/CurrentLimit (A)"`):

```java
LoggedTunableUnit limit = new LoggedTunableUnit("Flywheel/CurrentLimit", Amps.of(40));
Current c = limit.get();
```

Motors already expose their PID, feedforward and motion profile to the dashboard through the
motor classes, so most subsystems never need to create tunables by hand.

## Choosers

`SwitchableChooser` is a string chooser whose options can change while the robot is running:

```java
SwitchableChooser startPosition = new SwitchableChooser("Auto/Start");
startPosition.setOptions(new String[] {"Left", "Center", "Right"});
String chosen = startPosition.get();
```

`LoggedSequentialDashboardChooser<V>` is a chain of N choosers, one per step of an autonomous
routine, where each slot can filter the options of the next (for example, only the scoring spots
reachable from the chosen start). Call `periodic()` from `Robot.periodic()` (or register it with
`RobotLoop.addPeriodic`).

```java
var chooser = new LoggedSequentialDashboardChooser<Supplier<Command>>("Auto/Step", 3);
chooser.addDefaultOption("None", Commands::none);
chooser.addOption("Score A", () -> autos.scoreA());
chooser.setFilter(1, key -> !key.equals("Score A")); // slot 1 can't repeat A

List<Supplier<Command>> steps = chooser.get();
```
