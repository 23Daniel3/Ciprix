package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import org.ciprix.CiprixRobot;
import org.ciprix.CiprixSettings;

/**
 * The robot: extends {@link CiprixRobot}, which configures Ciprix, AdvantageKit and the command
 * scheduler. The structure (subsystems, buttons, autonomous) lives in {@link RobotContainer}.
 */
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
