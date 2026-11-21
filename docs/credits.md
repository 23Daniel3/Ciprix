---
sidebar_position: 110
---

# Credits and licenses

Ciprix stands on the work of other open-source projects. Please credit them in your own robot
code and documentation too.

## Author

Developed by **Daniel Cipriani**, who wrote the Ciprix library and designed its logo.

## License of Ciprix

Ciprix is free software under the **GNU General Public License v3.0**
([LICENSE](https://github.com/23Daniel3/Ciprix/blob/main/LICENSE)). Teams can use it in their
robot code and link it with the WPILib and vendor libraries installed by their robot project
(AdvantageKit, CTRE Phoenix, REVLib, PathPlannerLib). Anyone who distributes a modified version
must keep the GPLv3 and publish the source. The
[NOTICE](https://github.com/23Daniel3/Ciprix/blob/main/NOTICE.md) file lists every file derived
from other projects, with its original, author and license.

## AdvantageKit (FRC 6328 Mechanical Advantage)

[AdvantageKit](https://github.com/Mechanical-Advantage/AdvantageKit) is the logging and replay
framework created by **Littleton Robotics, FRC Team 6328 "Mechanical Advantage"**. Ciprix is
built around it: `CiprixRobot` configures AdvantageKit's `Logger`, the IO-layer pattern of the
subsystems follows its recommended structure, and log replay and `@AutoLogOutput` come from it.
Its official documentation is at [docs.advantagekit.org](https://docs.advantagekit.org).

- **Origin:** https://github.com/Mechanical-Advantage/AdvantageKit
- **Copyright:** © 2021–2026 Littleton Robotics.
- **License:** a BSD-style license (keep the copyright notice and disclaimer; don't use the names
  of Littleton Robotics, FRC 6328, AdvantageKit or its contributors to endorse derived products).
  It is **not** the MIT license. Read the
  [LICENSE file](https://github.com/Mechanical-Advantage/AdvantageKit/blob/main/LICENSE) for the
  exact terms; this page is a summary, not legal advice.

How this affects your team:

- AdvantageKit is **not embedded** in Ciprix. `Ciprix.json` only declares it in the `"requires"`
  block, and WPILib's vendordep manager downloads it from its own repository, with its license.
- A few small utility classes are adapted from AdvantageKit's templates and from the 6328 robot
  code (`SparkUtil`, `PhoenixUtil`, `SwitchableChooser`, `DoublePressTracker`,
  `LoggedTunableNumber`, `GeomUtil`). They keep their original headers and are listed in the
  NOTICE file.
- The text and code of this documentation are original. Only the sidebar-style layout of the site
  is inspired by AdvantageKit's documentation. No logo, text or name of AdvantageKit is used as a
  brand here.
- Ciprix is an **independent project**, not affiliated with or endorsed by Littleton Robotics or
  FRC 6328.

## WPILib

Code derived from the WPILib templates follows the
[WPILib license](https://github.com/23Daniel3/Ciprix/blob/main/WPILib-License.md) (BSD-style,
© FIRST and other WPILib contributors). See [wpilib.org](https://docs.wpilib.org).

## Vendor and path libraries

Ciprix adapts to [CTRE Phoenix](https://v6.docs.ctr-electronics.com),
[REVLib](https://docs.revrobotics.com/revlib) and
[PathPlanner](https://pathplanner.dev). Each keeps its own license; install them from their
official vendordeps. The pathfinding example used by `LocalADStarAK` comes from PathPlanner's
author, Michael Jansen (PathPlanner is MIT licensed).

## This documentation site

Built with [Docusaurus](https://docusaurus.io) (MIT) and the
[local search plugin](https://github.com/easyops-cn/docusaurus-search-local) (MIT).
