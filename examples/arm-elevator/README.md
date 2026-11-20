# Example: arm and elevator

A robot with two position-controlled mechanisms, built on the same pattern as the
[skeleton](../../templates/skeleton): requests, states, IO, generated relay and co-driver backup.
There is no pit self-test here on purpose: it spins the motor freely, and an arm or an elevator
would hit its hard stop (see [Hardware, power and pit](../../docs/hardware-power-and-pit.md)).

| What | Where |
| --- | --- |
| Arm with gravity (`armGravity()`, kG as a cosine), Motion Magic, soft limits | `subsystems/arm` |
| Elevator with constant gravity (the default), Motion Magic, soft limits | `subsystems/elevator` |
| A trip as two states: a "moving" state that sends the motor and a "resting" state entered when `atSetpoint` | `Arm.java`, `Elevator.java` |
| Safe order of movement between mechanisms (elevator up before the arm swings out, arm in before the elevator comes down) | `SuperStructure.java` |
| 3D robot: elevator carriage with the arm mounted on it | `RobotContainer.configureVisualizer()` |

## Controls (controller 0)

| Button | Request |
| --- | --- |
| Left bumper | `INTAKE`: arm down, elevator low |
| A | `SCORE_LOW`: elevator mid, then the arm swings out |
| Y | `SCORE_HIGH`: elevator high, then the arm swings out |
| (nothing) | `STOW` |

The co-driver (controller 1) can hold A to stow the arm or B to score with it, whatever the
SuperStructure is doing.

## Simulating

`Simulate Robot Code` in VS Code. Open AdvantageScope on the simulation, look at
`Subsystems/Arm`, `Subsystems/Elevator` and `Arm/Motor`, `Elevator/Motor`. The simulated arm and
elevator use the same `MotorConfig` as the robot, so gains tuned in simulation are a real
starting point.

## 3D robot

The visualizer is registered in `configureVisualizer()`. To see the parts you need your CAD
exported as `model_0.glb` (carriage) and `model_1.glb` (arm); see
[3D Robot](../../docs/3d-robot.md). The numbers in `ArmConstants` and `ElevatorConstants`
(pivot, carriage start, travel) are placeholders: measure yours in the CAD.
