package frc.robot;

import edu.wpi.first.wpilibj.RobotBase;

/** Entry point. Don't put anything here: initialization lives in {@link Robot}. */
public final class Main {
  private Main() {}

  public static void main(String... args) {
    RobotBase.startRobot(Robot::new);
  }
}
