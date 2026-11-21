# Notice

Ciprix is licensed under the **GNU General Public License v3.0 only** (`GPL-3.0-only`); the full
text is in [`LICENSE`](./LICENSE). Some files are derived from other open-source projects and keep
their original copyright headers. Every derived file also has a `// Modified for Ciprix, 2026`
line under that header. The license named in a kept header is the **original project's** license
(its "LICENSE file at the root directory of this project" is that project's, not this one's);
the complete texts are reproduced below. All of them allow this reuse inside a GPLv3 project.

WPILib-derived code follows the [WPILib license](./WPILib-License.md).

## Author

Developed by **Daniel Cipriani**, who wrote the Ciprix library and designed its logo.

## Files derived from other projects

| Ciprix file | Original | Author | License of the original | Verification |
| --- | --- | --- | --- | --- |
| `rev/.../util/security/SparkUtil.java` | `template_projects/sources/spark_swerve/.../util/SparkUtil.java` in [AdvantageKit](https://github.com/Mechanical-Advantage/AdvantageKit) | FRC 6328 / Littleton Robotics | GPLv3 (header kept as found in the file; the current AdvantageKit template header says BSD) | Same code apart from the package (compared) |
| `ctre/.../util/security/PhoenixUtil.java` | `template_projects/sources/talonfx_swerve/.../util/PhoenixUtil.java` in [AdvantageKit](https://github.com/Mechanical-Advantage/AdvantageKit) | Littleton Robotics | BSD-style (below) | Same `tryUntilOk` method (compared) |
| `core/.../logger/SwitchableChooser.java` | `frc2023/util/SwitchableChooser.java` in [RobotCode2023](https://github.com/Mechanical-Advantage/RobotCode2023) | FRC 6328 | MIT (below) | About 93% of the code lines match |
| `core/.../controller/DoublePressTracker.java` | `frc2023/util/DoublePressTracker.java` in [RobotCode2023](https://github.com/Mechanical-Advantage/RobotCode2023) | FRC 6328 | MIT (below) | About 98% match; the factory method was renamed |
| `core/.../logger/LoggedTunableNumber.java` | `frc2023/util/LoggedTunableNumber.java` in [RobotCode2023](https://github.com/Mechanical-Advantage/RobotCode2023) (the same class evolved in later 6328 robot repositories) | FRC 6328 | MIT (below) for the 2023 version | Same class, API and javadoc, extended by Ciprix. Only the 2023 version could be reached for comparison; if a later 6328 version was the source, it is also open-source under terms compatible with GPLv3 |
| `core/.../util/GeomUtil.java` | `frc2023/util/GeomUtil.java` in [RobotCode2023](https://github.com/Mechanical-Advantage/RobotCode2023) | FRC 6328 | MIT (below) | All 13 javadocs of the original are in Ciprix's file, which adds its own methods |
| `pathplanner/.../util/LocalADStarAK.java` | [gist by mjansen4857](https://gist.github.com/mjansen4857/a8024b55eb427184dbd10ae8923bd57d), the AdvantageKit example in the PathPlanner documentation | Michael Jansen | The gist has no license of its own; [PathPlanner](https://github.com/mjansen4857/pathplanner) is MIT (below) | Declared by the note already in the file. The gist could not be fetched for a line-by-line comparison, so its header was left unchanged |

### Reviewed and not treated as derived

- `core/.../util/AllianceFlipUtil.java`: the class name and the `apply`/`flip` naming follow the
  convention in 6328's robot code, but the code and documentation are Ciprix's own (no line or
  javadoc in common with the 2023 original). Credit is given here as an acknowledgement.

## What Ciprix does not include

AdvantageKit, CTRE Phoenix (6 and 5), REVLib and PathPlannerLib are **not** embedded in Ciprix.
Each robot project installs them through their own vendordeps, under their own licenses
(`Ciprix.json` only declares AdvantageKit and WPILibNewCommands in its `"requires"` block).

## License texts of the originals

### FRC 6328, RobotCode2023 (MIT)

```
MIT License

Copyright (c) 2023 FRC 6328

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

### Littleton Robotics, AdvantageKit (BSD-style)

```
Copyright (c) 2021-2026 Littleton Robotics. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

- Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
- Neither the name of Littleton Robotics, FRC 6328 ("Mechanical Advantage"),
  AdvantageKit, nor the names of other AdvantageKit contributors may be
  used to endorse or promote products derived from this software without
  specific prior written permission.

THIS SOFTWARE IS PROVIDED BY LITTLETON ROBOTICS AND OTHER ADVANTAGEKIT
CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY NONINFRINGEMENT
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
LITTLETON ROBOTICS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### Michael Jansen, PathPlanner (MIT)

```
MIT License

Copyright (c) 2022 Michael Jansen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
