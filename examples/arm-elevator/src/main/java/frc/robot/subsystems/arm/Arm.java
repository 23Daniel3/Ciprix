package frc.robot.subsystems.arm;

import static edu.wpi.first.units.Units.Rotations;

import edu.wpi.first.units.measure.Angle;
import frc.robot.subsystems.arm.ArmConstants.ArmRequest;
import frc.robot.subsystems.arm.ArmConstants.ArmState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;

/**
 * An arm with position control (Motion Magic). Each target has a "moving" state, which sends the
 * motor there, and a "resting" state, entered when the motor reports it arrived. A request is
 * served ({@code atGoal()}) only in the resting state, so the SuperStructure can wait for the arm
 * before it asks the next mechanism to move.
 */
@AutoCommandFactory(requestEnum = ArmRequest.class)
public class Arm extends StateSubsystem<ArmRequest, ArmState, ArmIOInputsAutoLogged, ArmIO> {

  public Arm(ArmIO io) {
    super(
        "Subsystems/Arm",
        new ArmIOInputsAutoLogged(),
        io,
        ArmState.class,
        ArmState.STOWED,
        ArmRequest.STOW,
        ArmConstants.class);

    trip(ArmState.MOVING_TO_STOW, ArmState.STOWED, ArmConstants.STOW_ROTATIONS);
    trip(ArmState.MOVING_TO_INTAKE, ArmState.AT_INTAKE, ArmConstants.INTAKE_ROTATIONS);
    trip(ArmState.MOVING_TO_SCORE, ArmState.AT_SCORE, ArmConstants.SCORE_ROTATIONS);

    // Request -> (entry state, goal state).
    bindRequest(ArmRequest.STOW, ArmState.MOVING_TO_STOW, ArmState.STOWED);
    bindRequest(ArmRequest.INTAKE, ArmState.MOVING_TO_INTAKE, ArmState.AT_INTAKE);
    bindRequest(ArmRequest.SCORE, ArmState.MOVING_TO_SCORE, ArmState.AT_SCORE);
    fsm.validateComplete();

    // Pit self-test: a gentle push, enough to see current. The arm only has a short travel.
    selfTest()
        .motor("Motor", io::controlMotor, () -> inputs.motorInputs)
        .output(0.05)
        .forSeconds(0.3);
  }

  private void trip(ArmState moving, ArmState arrived, double targetRotations) {
    fsm.state(moving)
        .onEnter(() -> io.controlMotor().runSmartPosition(Rotations.of(targetRotations)))
        .transitionTo(arrived, () -> inputs.motorInputs.atSetpoint, 0.1);
    fsm.state(arrived)
        .onEnter(() -> io.controlMotor().runSmartPosition(Rotations.of(targetRotations)));
  }

  /** The arm's angle, for the 3D robot. */
  public Angle getAngle() {
    return inputs.motorInputs.position;
  }
}
