package frc.robot;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.SuperStructureCommands;
import frc.robot.subsystems.flywheel.Flywheel;
import frc.robot.subsystems.flywheel.FlywheelCommands;
import frc.robot.subsystems.flywheel.FlywheelIO;
import frc.robot.subsystems.flywheel.FlywheelIOSim;
import frc.robot.subsystems.flywheel.FlywheelIOSparkMax;
import frc.robot.subsystems.flywheel.FlywheelIOTalonSRX;
import frc.robot.subsystems.superstructure.SuperStructure;
import org.ciprix.CiprixSettings;

/** The robot's structure. The only vendor-specific line is the choice of the flywheel's IO. */
public class RobotContainer {

  private final Flywheel flywheel;
  private final SuperStructure superStructure;

  private final CommandXboxController pilot = new CommandXboxController(0);
  private final CommandXboxController copilot = new CommandXboxController(1);

  public RobotContainer() {
    flywheel =
        new Flywheel(
            switch (CiprixSettings.getMode()) {
              case REAL ->
                  switch (Constants.FLYWHEEL_VENDOR) {
                    case REV_SPARK_MAX -> new FlywheelIOSparkMax();
                    case CTRE_TALON_SRX -> new FlywheelIOTalonSRX();
                  };
              case SIM -> new FlywheelIOSim();
              case REPLAY -> new FlywheelIO() {};
            });
    superStructure = new SuperStructure(flywheel);

    configureBindings();
  }

  private void configureBindings() {
    // Driver: hold the right bumper to shoot.
    superStructure.setDefaultCommand(
        SuperStructureCommands.manageRequests(superStructure, pilot.rightBumper()));
    flywheel.setDefaultCommand(FlywheelCommands.defaultCommand(superStructure, flywheel));

    // Co-driver: spin the flywheel while the button is held, whatever the SuperStructure says.
    copilot.a().whileTrue(FlywheelCommands.spinUp(flywheel));

    SmartDashboard.putData("SelfTest/Flywheel", flywheel.selfTestCommand());
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
