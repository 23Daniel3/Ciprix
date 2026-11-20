---
sidebar_position: 100
---

# Release notes

Versions follow `<season>.<minor>.<patch>`. Every release is on
[GitHub](https://github.com/23Daniel3/Ciprix/releases), where the full changelog lives.

## 2026.3.0 (upcoming)

The final Ciprix release for the 2026 season. Setup is simpler: one vendordep and one base class.
It also adds more than 40 test classes and fixes the bugs those tests found.

### Highlights

- **`CiprixRobot`.** `Robot` extends `CiprixRobot` instead of `LoggedRobot`. It sets up AdvantageKit
  logging, the runtime and tuning modes, `AutoLogOutputManager`, the default pre-match checklist
  and the command scheduler. See [Getting started](getting-started.md).
- **One vendordep.** `Ciprix.json` bundles every module and `commons-math3`.
- **New guides:** [Controllers](controllers.md), [Zones](zones.md),
  [Driving helpers](driving-helpers.md), [Safety](safety.md) and
  [Tunables and dashboard](tunables-and-dashboard.md).

### Migrating from 2026.2.x

1. **Vendordeps.** Update `Ciprix.json`, then **delete** `vendordeps/Ciprix-CTRE.json`,
   `Ciprix-REV.json`, `Ciprix-Phoenix5.json` and `Ciprix-PathPlanner.json`. You can also remove
   the `commons-math3` line from `build.gradle`.
2. **Robot.java.** Extend `CiprixRobot`: call `super(Constants.simMode, Constants.tuningMode)`,
   move the `CiprixSettings.set...` calls into `configure()`, return your auto from
   `getAutonomousCommand()`, and remove your own Logger setup and scheduler calls. Use
   `CiprixSettings.Mode` instead of your own `Mode` enum. `LoggedRobot` still works if you are
   not ready to switch.
3. **Behavior changes. Retest these on the robot:**
   - `ShotOnTheMoveCalculator`: the sign of the lateral compensation angle is fixed. Remove any
     workaround that flipped it.
   - `AntiTipping` / `AntiTippingAccel`: the correction is now robot-relative (+pitch drives +X,
     +roll drives −Y).
   - `TargetAssistModifier`, `ZoneRepulsionModifier`, `DualZoneSuctionBumpModifier`: they now
     convert field directions to the driver frame. Before, they pushed the wrong way on Red.
   - `AllianceSelector.isRed()` / `isBlue()` use the resolved alliance, matching
     `AllianceFlipUtil`.
   - `PIDRotation`: the output is limited to the drivetrain's max angular speed.

### Fixes

- `TunableControls`: the copy constructor keeps the profiled flag, plain controllers apply the
  period, velocity tolerance and continuous input, and tunable controllers read their gains as
  soon as they are created.
- `AntiTippingAccel` reads the nominal limits once and restores them correctly; `reset()` clears
  all state; new `getLastState()` for logging.
- `SuperStructureBase` rejects a null initial request before the subsystem is registered.
- REV: SPARKs no longer print the REVLib "kG is overriden by kCos" warning on every boot.

### Additions

- `TranslationModifier.fieldToDriver(...)` for custom modifiers.
- `EncoderIOCANcoder(name, id, CANBus, config)` and `getCANcoder()`.
- `MotorIOTalonSRX.getTalon()` and `MotorIOVictorSPX.getVictor()`.
- `PreMatchChecklist.addDefaults()` can be called more than once safely.

### Tests

Pure-logic tests (filters, zones, slew limiters, detectors, alliance flip, drivetrain assists,
anti-tipping, encoders, SuperStructure, motors, tunables, shot calculator, controllers) and
hardware tests on the vendor simulators (Phoenix 6, REVLib, Phoenix 5 and every PWM motor type).

## 2026.2.1

Documentation release: all guides and code comments were translated to English.

## 2026.2.0

First release of Ciprix as a library, published as a vendordep: the core, CTRE, REV, Phoenix 5,
PathPlanner and annotation-processor modules, with a Maven repository and `Ciprix*.json` files on
GitHub Pages, plus the robot template in `templates/skeleton`.
