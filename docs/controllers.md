---
sidebar_position: 30
---

# Controllers

Package `org.ciprix.controller`. Helpers to read the driver's input and give feedback.

| Class | What it does |
| --- | --- |
| `NaturalXboxController` | A `CommandXboxController` with a deadband and inverted axes, so the values follow the robot frame (+X forward, +Y to the left): pushing a stick **up or to the left** gives a positive value, and pushing it down or to the right gives a negative one. The X axes are inverted on purpose. Also has squared variants for finer control near the center. |
| `ArcadeController` | A generic arcade stick with up to 16 buttons, each exposed as a `Trigger` (`button1()` to `button16()`, or `button(index)`). |
| `DoublePressTracker` | Turns any `Trigger` into one that fires only on a quick double press. |
| `VibrateXboxController` | Command factory for rumble patterns. |
| `UtilCalculus` | Converts the two joystick axes into a translation vector. |

## Natural axes and deadband

```java
NaturalXboxController driver = new NaturalXboxController(0, 0.08); // port, deadband

double forward = driver.getLeftY();          // up is positive (+X forward)
double left = driver.getLeftX();             // left is positive (+Y left)
double turn = driver.getSquaredRightX();     // same axis, squared
```

Without the second argument, the deadband is `CiprixSettings.getControllerDeadband()`. Change it at
runtime with `setDeadband(...)`.

## Joystick to translation

`UtilCalculus.getLinearVelocityFromJoysticks(x, y)` keeps the stick's direction and squares its
magnitude, so small movements are gentle and full deflection is still full speed:

```java
Translation2d input = UtilCalculus.getLinearVelocityFromJoysticks(
    driver.getLeftY(), driver.getLeftX());
```

The result is in the driver frame described in [Driving helpers](driving-helpers.md).

## Double press

```java
Trigger reset = DoublePressTracker.doublePress(driver.start());
reset.onTrue(Commands.runOnce(drive::zeroHeading));
```

The second press has to start within `DoublePressTracker.maxLengthSecs` (0.4 s) of the first.

## Rumble

```java
VibrateXboxController rumble = new VibrateXboxController(driver);

driver.a().onTrue(rumble.lightConfirm());
intake.hasGamePiece().onTrue(rumble.pulses(0.8, 3, 0.6)); // intensity, pulses, total seconds
```

Available patterns: `continuous`, `pulses`, `zigZag`, `rampUp`, `lightConfirm` and
`heavyWarning`. Each one is a `Command` that stops the rumble when it ends or is interrupted.
