package frc.robot.subsystems.superstructure;

import static edu.wpi.first.units.Units.Rotations;

import frc.robot.Constants.RobotRequest;
import frc.robot.Constants.RobotState;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmConstants;
import frc.robot.subsystems.arm.ArmConstants.ArmRequest;
import frc.robot.subsystems.elevator.Elevator;
import frc.robot.subsystems.elevator.ElevatorConstants.ElevatorRequest;
import org.ciprix.interfaces.subsystem.SuperStructureBase;

/**
 * The overall state machine. Each overall state says where the arm and the elevator should be; the
 * getters below add the safe order of movement: the elevator rises before the arm swings out to
 * score, and the arm comes back in before the elevator goes down.
 */
public class SuperStructure extends SuperStructureBase<RobotRequest, RobotState> {

  private final Arm arm;
  private final Elevator elevator;

  private ArmRequest wantedArm = ArmRequest.STOW;
  private ElevatorRequest wantedElevator = ElevatorRequest.LOW;
  private ElevatorRequest sentElevator = ElevatorRequest.LOW;

  public SuperStructure(Arm arm, Elevator elevator) {
    super("Subsystems/SuperStructure", RobotState.class, RobotState.STOWED, RobotRequest.STOW);
    this.arm = arm;
    this.elevator = elevator;

    generalFsm.state(RobotState.STOWED).onEnter(() -> want(ArmRequest.STOW, ElevatorRequest.LOW));
    generalFsm
        .state(RobotState.INTAKING)
        .onEnter(() -> want(ArmRequest.INTAKE, ElevatorRequest.LOW));
    generalFsm
        .state(RobotState.SCORING_LOW)
        .onEnter(() -> want(ArmRequest.SCORE, ElevatorRequest.MID));
    generalFsm
        .state(RobotState.SCORING_HIGH)
        .onEnter(() -> want(ArmRequest.SCORE, ElevatorRequest.HIGH));

    bindRequest(RobotRequest.STOW, RobotState.STOWED);
    bindRequest(RobotRequest.INTAKE, RobotState.INTAKING);
    bindRequest(RobotRequest.SCORE_LOW, RobotState.SCORING_LOW);
    bindRequest(RobotRequest.SCORE_HIGH, RobotState.SCORING_HIGH);
    generalFsm.validateComplete();
  }

  private void want(ArmRequest armRequest, ElevatorRequest elevatorRequest) {
    wantedArm = armRequest;
    wantedElevator = elevatorRequest;
  }

  /** The arm's request, read every cycle by the generated default command. */
  public ArmRequest getArmRequest() {
    // Swing out to score only after the elevator got there.
    if (wantedArm == ArmRequest.SCORE && !elevator.atGoal()) {
      return ArmRequest.STOW;
    }
    return wantedArm;
  }

  /** The elevator's request, read every cycle by the generated default command. */
  public ElevatorRequest getElevatorRequest() {
    // Go down only after the arm is back in: while it is out, keep the last height.
    boolean armIsOut = arm.getAngle().in(Rotations) > ArmConstants.INTAKE_ROTATIONS + 0.05;
    if (wantedElevator == ElevatorRequest.LOW && armIsOut) {
      return sentElevator;
    }
    sentElevator = wantedElevator;
    return wantedElevator;
  }
}
