package frc.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.commands.SuperStructureCommands;
import frc.robot.subsystems.intake.Intake;
import frc.robot.subsystems.intake.IntakeCommands;
import frc.robot.subsystems.intake.IntakeIO;
import frc.robot.subsystems.intake.IntakeIOSim;
import frc.robot.subsystems.intake.IntakeIOTalonFX;
import frc.robot.subsystems.shooter.Shooter;
import frc.robot.subsystems.shooter.ShooterCommands;
import frc.robot.subsystems.shooter.ShooterIO;
import frc.robot.subsystems.shooter.ShooterIOSim;
import frc.robot.subsystems.shooter.ShooterIOTalonFX;
import frc.robot.subsystems.superstructure.SuperStructure;
import org.ciprix.CiprixSettings;
import org.ciprix.CiprixSettings.Mode;
import org.ciprix.sim.SimBattery;
import org.ciprix.util.PreMatchChecklist;

/**
 * Power management, alerts and the pit checklist. {@code CiprixRobot} already installs the default
 * pre-match checks (battery, devices connected, no faults); this class adds the robot's own.
 */
public class RobotContainer {

  private static final String EXTRA_LOAD_KEY = "Sim/ExtraLoadAmps";

  private final Intake intake;
  private final Shooter shooter;
  private final SuperStructure superStructure;

  private final CommandXboxController pilot = new CommandXboxController(0);
  private final CommandXboxController copilot = new CommandXboxController(1);

  public RobotContainer() {
    Mode mode = CiprixSettings.getMode();
    intake =
        new Intake(
            switch (mode) {
              case REAL -> new IntakeIOTalonFX();
              case SIM -> new IntakeIOSim();
              case REPLAY -> new IntakeIO() {};
            });
    shooter =
        new Shooter(
            switch (mode) {
              case REAL -> new ShooterIOTalonFX();
              case SIM -> new ShooterIOSim();
              case REPLAY -> new ShooterIO() {};
            });
    superStructure = new SuperStructure();

    configureBindings();
    configureChecklist();
    configureSimulatedBattery(mode);
  }

  private void configureBindings() {
    superStructure.setDefaultCommand(
        SuperStructureCommands.manageRequests(
            superStructure, pilot.leftBumper(), pilot.rightBumper()));
    intake.setDefaultCommand(IntakeCommands.defaultCommand(superStructure, intake));
    shooter.setDefaultCommand(ShooterCommands.defaultCommand(superStructure, shooter));

    // Co-driver backup while held.
    copilot.a().whileTrue(IntakeCommands.intake(intake));
    copilot.b().whileTrue(ShooterCommands.spinUp(shooter));

    // The pit self-test: robot enabled, mechanisms free to spin.
    SmartDashboard.putData("SelfTest/Intake", intake.selfTestCommand());
    SmartDashboard.putData("SelfTest/Shooter", shooter.selfTestCommand());
  }

  /**
   * The robot's own items, on top of the defaults. While the robot is disabled the dashboard shows
   * {@code PreMatch/Ready} and the list {@code PreMatch/Pending}, and a warning alert names what is
   * still missing. Once the robot is enabled the checklist stops.
   */
  private void configureChecklist() {
    PreMatchChecklist.add("Alliance set", () -> DriverStation.getAlliance().isPresent());
    PreMatchChecklist.add(
        "Driver controller connected", () -> DriverStation.isJoystickConnected(0));
  }

  /**
   * In simulation, an adjustable load on the battery stands for a drivetrain pulling current. Set
   * {@code Sim/ExtraLoadAmps} on the dashboard to 150 or more, and watch {@code Power/Level} and
   * {@code Power/FilteredVoltage} in AdvantageScope: the intake's current limit drops first.
   */
  private void configureSimulatedBattery(Mode mode) {
    if (mode != Mode.SIM) {
      return;
    }
    SmartDashboard.putNumber(EXTRA_LOAD_KEY, 0.0);
    SimBattery.addLoad(() -> SmartDashboard.getNumber(EXTRA_LOAD_KEY, 0.0));
  }

  public Command getAutonomousCommand() {
    return Commands.none();
  }
}
