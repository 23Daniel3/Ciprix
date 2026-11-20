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
   * Translates the driver's buttons into the active {@link RobotRequest}, every cycle.
   *
   * <pre>
   * high   -> SCORE_HIGH
   * low    -> SCORE_LOW
   * intake -> INTAKE
   * (none) -> STOW
   * </pre>
   */
  public static Command manageRequests(
      SuperStructure superStructure,
      BooleanSupplier intake,
      BooleanSupplier scoreLow,
      BooleanSupplier scoreHigh) {
    return Commands.run(
            () -> {
              RobotRequest request;
              if (scoreHigh.getAsBoolean()) {
                request = RobotRequest.SCORE_HIGH;
              } else if (scoreLow.getAsBoolean()) {
                request = RobotRequest.SCORE_LOW;
              } else if (intake.getAsBoolean()) {
                request = RobotRequest.INTAKE;
              } else {
                request = RobotRequest.STOW;
              }
              superStructure.setRequest(request);
            },
            superStructure)
        .withName("Requests by pilot");
  }
}
