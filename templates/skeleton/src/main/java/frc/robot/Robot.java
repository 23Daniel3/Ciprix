package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import org.ciprix.CiprixSettings;
import org.littletonrobotics.junction.AutoLogOutputManager;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

/**
 * The robot: sets up Ciprix and AdvantageKit and runs the command scheduler. The structure
 * (subsystems, buttons, autonomous) lives in {@link RobotContainer}.
 */
public class Robot extends LoggedRobot {
  private final RobotContainer robotContainer;
  private Command autonomousCommand;

  public Robot() {
    // Ciprix comes first: everything created afterwards reads these settings.
    configureCiprix();

    Logger.recordMetadata("ProjectName", "skeleton");
    Logger.recordMetadata("BuildTime", java.time.Instant.now().toString());
    switch (Constants.currentMode) {
      case REAL -> {
        // On the robot: log to the USB stick ("/U/logs") and live over NetworkTables.
        Logger.addDataReceiver(new WPILOGWriter("/U/logs/"));
        Logger.addDataReceiver(new NT4Publisher());
      }
      case SIM -> Logger.addDataReceiver(new NT4Publisher());
      case REPLAY -> {
        // Replay: reads a log and writes another with the "_sim" suffix, as fast as possible.
        setUseTiming(false);
        String logPath = LogFileUtil.findReplayLog();
        Logger.setReplaySource(new WPILOGReader(logPath));
        Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
      }
    }
    Logger.start();

    robotContainer = new RobotContainer();
  }

  /** Hands the project's constants to Ciprix (it never reads {@link Constants} directly). */
  private static void configureCiprix() {
    CiprixSettings.setMode(
        switch (Constants.currentMode) {
          case REAL -> CiprixSettings.Mode.REAL;
          case SIM -> CiprixSettings.Mode.SIM;
          case REPLAY -> CiprixSettings.Mode.REPLAY;
        });
    CiprixSettings.setTuningMode(Constants.tuningMode);
    CiprixSettings.setControllerDeadband(Constants.CONTROLLER_DEADBAND);
    CiprixSettings.setDefaultAlliance(Constants.DEFAULT_ALLIANCE);
    CiprixSettings.setFieldSize(Constants.FIELD_LENGTH_METERS, Constants.FIELD_WIDTH_METERS);
    AutoLogOutputManager.addPackage("org.ciprix");
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
  }

  @Override
  public void autonomousInit() {
    autonomousCommand = robotContainer.getAutonomousCommand();
    if (autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(autonomousCommand);
    }
  }

  @Override
  public void teleopInit() {
    if (autonomousCommand != null) {
      autonomousCommand.cancel();
    }
  }

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
  }
}
