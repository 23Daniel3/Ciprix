package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.SuperStructureCommands;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmCommands;
import frc.robot.subsystems.arm.ArmConstants;
import frc.robot.subsystems.arm.ArmIO;
import frc.robot.subsystems.arm.ArmIOSim;
import frc.robot.subsystems.arm.ArmIOTalonFX;
import frc.robot.subsystems.elevator.Elevator;
import frc.robot.subsystems.elevator.ElevatorCommands;
import frc.robot.subsystems.elevator.ElevatorConstants;
import frc.robot.subsystems.elevator.ElevatorIO;
import frc.robot.subsystems.elevator.ElevatorIOSim;
import frc.robot.subsystems.elevator.ElevatorIOTalonFX;
import frc.robot.subsystems.superstructure.SuperStructure;
import org.ciprix.CiprixSettings;
import org.ciprix.visualization.MechanismPoses;
import org.ciprix.visualization.MechanismPoses.Axis;
import org.ciprix.visualization.RobotVisualizer;

/** The robot's structure: subsystems, driver and co-driver buttons, 3D robot, autonomous. */
public class RobotContainer {

  private final Arm arm;
  private final Elevator elevator;
  private final SuperStructure superStructure;

  private final CommandXboxController pilot = new CommandXboxController(0);
  private final CommandXboxController copilot = new CommandXboxController(1);

  public RobotContainer() {
    arm =
        new Arm(
            switch (CiprixSettings.getMode()) {
              case REAL -> new ArmIOTalonFX();
              case SIM -> new ArmIOSim();
              case REPLAY -> new ArmIO() {};
            });
    elevator =
        new Elevator(
            switch (CiprixSettings.getMode()) {
              case REAL -> new ElevatorIOTalonFX();
              case SIM -> new ElevatorIOSim();
              case REPLAY -> new ElevatorIO() {};
            });
    superStructure = new SuperStructure(arm, elevator);

    configureBindings();
    configureVisualizer();
  }

  private void configureBindings() {
    // Driver: chooses the robot's request; the overall FSM does the rest.
    superStructure.setDefaultCommand(
        SuperStructureCommands.manageRequests(
            superStructure, pilot.leftBumper(), pilot.a(), pilot.y()));

    // Each subsystem follows the SuperStructure through its generated relay.
    arm.setDefaultCommand(ArmCommands.defaultCommand(superStructure, arm));
    elevator.setDefaultCommand(ElevatorCommands.defaultCommand(superStructure, elevator));

    // Co-driver: manual backup, only while the button is held.
    copilot.a().whileTrue(ArmCommands.stow(arm));
    copilot.b().whileTrue(ArmCommands.score(arm));
  }

  /**
   * The moving parts of the 3D robot in AdvantageScope: the elevator carriage is {@code
   * model_0.glb} and the arm is {@code model_1.glb}, mounted on the carriage so it rises with it.
   */
  private void configureVisualizer() {
    RobotVisualizer.component(
        0,
        MechanismPoses.slider(
            ElevatorConstants.CARRIAGE_START, ElevatorConstants.UP, elevator::getHeight),
        ElevatorConstants.CARRIAGE_START);
    RobotVisualizer.component(
        1,
        MechanismPoses.attach(
            MechanismPoses.slider(
                ElevatorConstants.CARRIAGE_START, ElevatorConstants.UP, elevator::getHeight),
            MechanismPoses.pivot(ArmConstants.PIVOT_AXIS, Axis.PITCH, arm::getAngle)),
        ArmConstants.PIVOT_AXIS);
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
