package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Constants.RobotRequest;
import frc.robot.subsystems.superstructure.SuperStructure;
import java.util.function.BooleanSupplier;

/** Driver commands for the SuperStructure. */
public final class SuperStructureCommands {
  private SuperStructureCommands() {}

  /** Shoot beats intake; with no button the robot is idle. */
  public static Command manageRequests(
      SuperStructure superStructure, BooleanSupplier intake, BooleanSupplier shoot) {
    return Commands.run(
            () -> {
              RobotRequest request = RobotRequest.IDLE;
              if (shoot.getAsBoolean()) {
                request = RobotRequest.SHOOT;
              } else if (intake.getAsBoolean()) {
                request = RobotRequest.INTAKE;
              }
              superStructure.setRequest(request);
            },
            superStructure)
        .withName("Requests by pilot");
  }
}
