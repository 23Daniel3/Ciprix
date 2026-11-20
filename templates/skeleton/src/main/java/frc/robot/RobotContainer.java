package frc.robot;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.SuperStructureCommands;
import frc.robot.subsystems.roller.Roller;
import frc.robot.subsystems.roller.RollerCommands;
import frc.robot.subsystems.roller.RollerIO;
import frc.robot.subsystems.roller.RollerIOSim;
import frc.robot.subsystems.roller.RollerIOTalonFX;
import frc.robot.subsystems.superstructure.SuperStructure;
import org.ciprix.CiprixSettings;

/** The robot's structure: subsystems, driver and co-driver buttons, autonomous. */
public class RobotContainer {

  private final Roller roller;
  private final SuperStructure superStructure;

  private final CommandXboxController pilot = new CommandXboxController(0);
  private final CommandXboxController copilot = new CommandXboxController(1);

  public RobotContainer() {
    roller =
        new Roller(
            switch (CiprixSettings.getMode()) {
              case REAL -> new RollerIOTalonFX();
              case SIM -> new RollerIOSim();
              case REPLAY -> new RollerIO() {}; // inputs come from the log
            });
    superStructure = new SuperStructure(roller);

    configureBindings();
  }

  private void configureBindings() {
    // Driver: chooses the robot's request; the overall FSM does the rest.
    superStructure.setDefaultCommand(
        SuperStructureCommands.manageRequests(
            superStructure, pilot.leftBumper(), pilot.rightBumper()));

    // Each subsystem follows the SuperStructure through its generated relay.
    roller.setDefaultCommand(RollerCommands.defaultCommand(superStructure, roller));

    // Co-driver: manual backup, only while the button is held.
    copilot.a().whileTrue(RollerCommands.intake(roller));
    copilot.b().whileTrue(RollerCommands.eject(roller));

    // Pit self-test (robot enabled, mechanism free to spin).
    SmartDashboard.putData("SelfTest/Roller", roller.selfTestCommand());
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
