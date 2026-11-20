package org.ciprix;

import edu.wpi.first.wpilibj.RobotBase;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import java.time.Instant;
import org.ciprix.CiprixSettings.Mode;
import org.ciprix.util.PreMatchChecklist;
import org.littletonrobotics.junction.AutoLogOutputManager;
import org.littletonrobotics.junction.LogFileUtil;
import org.littletonrobotics.junction.LoggedRobot;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.networktables.NT4Publisher;
import org.littletonrobotics.junction.wpilog.WPILOGReader;
import org.littletonrobotics.junction.wpilog.WPILOGWriter;

/**
 * Base robot class for Ciprix projects.
 *
 * <p>Replaces direct use of {@link LoggedRobot} in {@code Robot.java} and handles the standard
 * setup automatically:
 *
 * <ul>
 *   <li>Selects the runtime {@link Mode} ({@link Mode#REAL} on a roboRIO, or the chosen desktop
 *       simulation/replay mode).
 *   <li>Configures AdvantageKit logging ({@code /U/logs/} + NetworkTables on the robot,
 *       NetworkTables in simulation, and log reader/writer in replay) and starts {@link Logger}.
 *   <li>Registers {@code org.ciprix} and the robot's own package with {@link AutoLogOutputManager}.
 *   <li>Installs the default pre-match checks ({@link PreMatchChecklist#addDefaults()}).
 *   <li>Runs {@link CommandScheduler} every cycle and manages the autonomous command lifecycle.
 * </ul>
 *
 * <p>The lifecycle methods ({@link #robotPeriodic()}, {@link #autonomousInit()}, {@link
 * #teleopInit()} and {@link #testInit()}) are {@code final} so the scheduler can never be turned
 * off by accident; override {@link #periodic()}, {@link #autonomousStart()}, {@link #teleopStart()}
 * or {@link #testStart()} when extra robot-level hooks are needed.
 */
public abstract class CiprixRobot extends LoggedRobot {

  private static volatile Runnable loggerStarter = null;

  private Command autonomousCommand;

  /**
   * Creates a Ciprix robot using {@link Mode#SIM} when running outside a real roboRIO and keeping
   * live tuning disabled by default.
   */
  protected CiprixRobot() {
    this(Mode.SIM, false, CiprixSettings.getLoopPeriodSeconds());
  }

  /**
   * Creates a Ciprix robot using the given desktop mode ({@link Mode#SIM} or {@link Mode#REPLAY})
   * when running outside a real roboRIO.
   *
   * @param desktopMode mode used when {@link RobotBase#isReal()} is {@code false}
   */
  protected CiprixRobot(Mode desktopMode) {
    this(desktopMode, false, CiprixSettings.getLoopPeriodSeconds());
  }

  /**
   * Creates a Ciprix robot with explicit desktop mode and live-tuning flag.
   *
   * @param desktopMode mode used when {@link RobotBase#isReal()} is {@code false}
   * @param tuningMode {@code true} to publish and allow live editing of tunable numbers
   */
  protected CiprixRobot(Mode desktopMode, boolean tuningMode) {
    this(desktopMode, tuningMode, CiprixSettings.getLoopPeriodSeconds());
  }

  /**
   * Creates a Ciprix robot with explicit desktop mode, live-tuning flag and main-loop period.
   *
   * @param desktopMode mode used when {@link RobotBase#isReal()} is {@code false}
   * @param tuningMode {@code true} to publish and allow live editing of tunable numbers
   * @param loopPeriodSeconds nominal period of the main robot loop, in seconds
   */
  protected CiprixRobot(Mode desktopMode, boolean tuningMode, double loopPeriodSeconds) {
    super(loopPeriodSeconds);

    CiprixSettings.setLoopPeriodSeconds(loopPeriodSeconds);
    Mode effectiveMode =
        RobotBase.isReal() ? Mode.REAL : (desktopMode != null ? desktopMode : Mode.SIM);
    CiprixSettings.setMode(effectiveMode);
    CiprixSettings.setTuningMode(tuningMode);

    configure();

    AutoLogOutputManager.addPackage("org.ciprix");
    String robotPackage = getClass().getPackageName();
    if (!robotPackage.isEmpty() && !robotPackage.equals("org.ciprix")) {
      AutoLogOutputManager.addPackage(robotPackage);
    }

    startAdvantageKit();
    PreMatchChecklist.addDefaults();
  }

  /**
   * Optional hook called near the start of the {@link CiprixRobot} constructor, before AdvantageKit
   * is started and before subclass fields (such as {@code RobotContainer}) are initialized.
   *
   * <p>Override this method to adjust {@link CiprixSettings} (field dimensions, controller
   * deadband, default alliance) or record extra {@link Logger#recordMetadata} entries. To use a
   * non-default loop period, pass it to {@link #CiprixRobot(Mode, boolean, double)}.
   */
  protected void configure() {}

  /**
   * Optional hook called at the end of every {@link #robotPeriodic()} cycle, after {@link
   * CommandScheduler#run()}.
   */
  protected void periodic() {}

  /**
   * Optional hook called at the end of {@link #autonomousInit()}, after scheduling {@link
   * #getAutonomousCommand()}.
   */
  protected void autonomousStart() {}

  /**
   * Optional hook called at the end of {@link #teleopInit()}, after cancelling the autonomous
   * command.
   */
  protected void teleopStart() {}

  /**
   * Optional hook called at the end of {@link #testInit()}, after cancelling all scheduled
   * commands.
   */
  protected void testStart() {}

  /** Project name recorded in the AdvantageKit log metadata. */
  protected String getProjectName() {
    return getClass().getSimpleName();
  }

  /** Directory on the roboRIO where {@code .wpilog} files are written in {@link Mode#REAL}. */
  protected String getLogDirectory() {
    return "/U/logs/";
  }

  /**
   * Returns the command to schedule at the start of autonomous. Override this to return {@code
   * robotContainer.getAutonomousCommand()}.
   */
  protected Command getAutonomousCommand() {
    return null;
  }

  private void startAdvantageKit() {
    Logger.recordMetadata("ProjectName", getProjectName());
    Logger.recordMetadata("BuildTime", Instant.now().toString());
    Runnable customStarter = loggerStarter;
    if (customStarter != null) {
      customStarter.run();
      return;
    }
    switch (CiprixSettings.getMode()) {
      case REAL -> {
        Logger.addDataReceiver(new WPILOGWriter(getLogDirectory()));
        Logger.addDataReceiver(new NT4Publisher());
      }
      case SIM -> Logger.addDataReceiver(new NT4Publisher());
      case REPLAY -> {
        setUseTiming(false);
        String logPath = LogFileUtil.findReplayLog();
        Logger.setReplaySource(new WPILOGReader(logPath));
        Logger.addDataReceiver(new WPILOGWriter(LogFileUtil.addPathSuffix(logPath, "_sim")));
      }
    }
    Logger.start();
  }

  /**
   * Replaces the AdvantageKit receiver/thread startup (intended for unit tests so {@code
   * Logger.start()} does not hijack {@code Timer.getFPGATimestamp()} or open NetworkTables ports).
   */
  static void setLoggerStarterForTesting(Runnable starter) {
    loggerStarter = starter;
  }

  /** Restores normal AdvantageKit startup. */
  static void resetLoggerStarterForTesting() {
    loggerStarter = null;
  }

  @Override
  public final void robotPeriodic() {
    CommandScheduler.getInstance().run();
    periodic();
  }

  @Override
  public final void autonomousInit() {
    autonomousCommand = getAutonomousCommand();
    if (autonomousCommand != null) {
      CommandScheduler.getInstance().schedule(autonomousCommand);
    }
    autonomousStart();
  }

  @Override
  public final void teleopInit() {
    if (autonomousCommand != null) {
      autonomousCommand.cancel();
    }
    teleopStart();
  }

  @Override
  public final void testInit() {
    CommandScheduler.getInstance().cancelAll();
    testStart();
  }
}
