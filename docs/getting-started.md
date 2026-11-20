---
sidebar_position: 1
---

# Getting started

Three steps take a new WPILib project to a Ciprix robot: install the vendordep, copy the template
and extend `CiprixRobot`.

## 1. Install `Ciprix.json`

In the WPILib VS Code extension, run **WPILib: Manage Vendor Libraries → Install new library
(online)** and paste:

```
https://23daniel3.github.io/Ciprix/Ciprix.json
```

To pin a version, use `https://23daniel3.github.io/Ciprix/vendordeps/<version>/Ciprix.json`
instead (for example `2026.3.0`).

`Ciprix.json` bundles every Ciprix module (core, CTRE Phoenix 6, Phoenix 5, REV, PathPlanner) and
`commons-math3`. It also declares **AdvantageKit** and **WPILibNewCommands** in its `"requires"`
block, so VS Code offers to install them if the project doesn't have them yet.

Beyond that, install only the official vendordep for the hardware or path library your robot
actually uses: Phoenix 6, Phoenix 5, REVLib or PathPlannerLib. Unused vendor integrations are
never loaded at runtime.

Then add the annotation processor, which generates each subsystem's `XxxCommands`, to
`build.gradle`:

```groovy
def ciprixJson = new groovy.json.JsonSlurper().parseText(new File(projectDir.getAbsolutePath() + "/vendordeps/Ciprix.json").text)
annotationProcessor "org.ciprix:ciprix-processor:$ciprixJson.version"
```

## 2. Copy the template

The fastest start is the
[`templates/skeleton`](https://github.com/23Daniel3/Ciprix/tree/main/templates/skeleton) folder:
the smallest complete robot in the pattern, with one subsystem (`Roller`), the `SuperStructure`,
the driver commands and the co-driver backup.

1. Copy the folder outside the Ciprix repository (or download it from GitHub).
2. In `.wpilib/wpilib_preferences.json`, change `teamNumber` to your team's number.
3. In `settings.gradle`, change `rootProject.name`.
4. Open the folder in the WPILib VS Code extension. **Simulate Robot Code** already works: the
   simulated roller spins with the driver's left bumper (controller 0) and reverses with the
   right one.

Outside the Ciprix repository, Gradle downloads the published version listed in
`vendordeps/Ciprix.json`.

## 3. Extend `CiprixRobot`

`Robot` extends `CiprixRobot` instead of `LoggedRobot`. The base class sets up AdvantageKit
logging (USB and NetworkTables on the robot, NetworkTables in simulation, and log replay), the
runtime and tuning modes, `CiprixSettings`, the default `PreMatchChecklist` and the
`CommandScheduler` lifecycle. This is the skeleton's `Robot.java`:

```java
package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import org.ciprix.CiprixRobot;
import org.ciprix.CiprixSettings;

public class Robot extends CiprixRobot {
  private final RobotContainer robotContainer;

  public Robot() {
    super(Constants.simMode, Constants.tuningMode);
    robotContainer = new RobotContainer();
  }

  @Override
  protected void configure() {
    CiprixSettings.setControllerDeadband(Constants.CONTROLLER_DEADBAND);
    CiprixSettings.setDefaultAlliance(Constants.DEFAULT_ALLIANCE);
    CiprixSettings.setFieldSize(Constants.FIELD_LENGTH_METERS, Constants.FIELD_WIDTH_METERS);
  }

  @Override
  protected Command getAutonomousCommand() {
    return robotContainer.getAutonomousCommand();
  }
}
```

The lifecycle methods are `final`, so the scheduler can't be switched off by accident. Use the
`periodic()`, `autonomousStart()`, `teleopStart()` and `testStart()` hooks, and adjust
`CiprixSettings` in `configure()`.

## Next steps

- Read [Architecture](architecture.md) to see how requests and state machines fit together, and
  how to add a subsystem.
- Run the robot in [Simulation](simulation.md) before touching hardware.
- Coming from 2026.2.x? See the migration steps in the [Release notes](release-notes.md).
