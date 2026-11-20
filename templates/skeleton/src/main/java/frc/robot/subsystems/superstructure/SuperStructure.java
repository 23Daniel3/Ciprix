package frc.robot.subsystems.superstructure;

import frc.robot.Constants.RobotRequest;
import frc.robot.Constants.RobotState;
import frc.robot.subsystems.roller.Roller;
import frc.robot.subsystems.roller.RollerConstants.RollerRequest;
import org.ciprix.interfaces.subsystem.SuperStructureBase;

/**
 * The overall state machine: decides the robot's state from the driver's request and, on entering
 * each state, says what each subsystem should do. The subsystems receive those requests through the
 * generated default command ({@code RollerCommands.defaultCommand(superStructure, roller)}), which
 * reads {@link #getRollerRequest()}.
 */
public class SuperStructure extends SuperStructureBase<RobotRequest, RobotState> {

  private final Roller roller;
  private RollerRequest rollerRequest = RollerRequest.STOP;

  public SuperStructure(Roller roller) {
    super("Subsystems/SuperStructure", RobotState.class, RobotState.IDLE, RobotRequest.IDLE);
    this.roller = roller;

    generalFsm.state(RobotState.IDLE).onEnter(() -> rollerRequest = RollerRequest.STOP);
    generalFsm.state(RobotState.INTAKING).onEnter(() -> rollerRequest = RollerRequest.INTAKE);
    generalFsm.state(RobotState.EJECTING).onEnter(() -> rollerRequest = RollerRequest.EJECT);

    // Robot request -> entry state (and the states that already serve it).
    bindRequest(RobotRequest.IDLE, RobotState.IDLE);
    bindRequest(RobotRequest.INTAKE, RobotState.INTAKING);
    bindRequest(RobotRequest.EJECT, RobotState.EJECTING);
    generalFsm.validateComplete();
  }

  /** The roller's request, read every cycle by the generated default command. */
  public RollerRequest getRollerRequest() {
    return rollerRequest;
  }

  /** True when the roller reached the requested state (use it in the overall FSM's transitions). */
  public boolean isRollerAtGoal() {
    return roller.atGoal();
  }
}
