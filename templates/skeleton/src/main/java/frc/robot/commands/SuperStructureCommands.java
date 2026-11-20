package frc.robot.commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.Constants.RobotRequest;
import frc.robot.subsystems.superstructure.SuperStructure;
import java.util.function.BooleanSupplier;

/** Driver commands for the SuperStructure. */
public final class SuperStructureCommands {
  private SuperStructureCommands() {}

  /**
   * The SuperStructure's default command: translates the driver's buttons into the active {@link
   * RobotRequest}, every cycle. It is the only entry point for the driver's intent into the overall
   * FSM.
   *
   * <pre>
   * eject        -> EJECT   (beats the other buttons)
   * intake       -> INTAKE
   * (none)       -> IDLE
   * </pre>
   */
  public static Command manageRequests(
      SuperStructure superStructure, BooleanSupplier intakeButton, BooleanSupplier ejectButton) {
    return Commands.run(
            () -> {
              RobotRequest request;
              if (ejectButton.getAsBoolean()) {
                request = RobotRequest.EJECT;
              } else if (intakeButton.getAsBoolean()) {
                request = RobotRequest.INTAKE;
              } else {
                request = RobotRequest.IDLE;
              }
              superStructure.setRequest(request);
            },
            superStructure)
        .withName("Requests by pilot");
  }
}
