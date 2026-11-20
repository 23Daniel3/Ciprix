package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Constants.RobotRequest;
import frc.robot.subsystems.superstructure.SuperStructure;
import java.util.function.BooleanSupplier;

/** Driver commands for the SuperStructure. */
public final class SuperStructureCommands {
  private SuperStructureCommands() {}

  /** Shoot while the button is held; idle otherwise. */
  public static Command manageRequests(SuperStructure superStructure, BooleanSupplier shootButton) {
    return Commands.run(
            () ->
                superStructure.setRequest(
                    shootButton.getAsBoolean() ? RobotRequest.SHOOT : RobotRequest.IDLE),
            superStructure)
        .withName("Requests by pilot");
  }
}
