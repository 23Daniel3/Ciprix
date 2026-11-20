package frc.robot.subsystems.elevator;

import static edu.wpi.first.units.Units.Meters;
import static edu.wpi.first.units.Units.Rotations;

import edu.wpi.first.units.measure.Distance;
import frc.robot.subsystems.elevator.ElevatorConstants.ElevatorRequest;
import frc.robot.subsystems.elevator.ElevatorConstants.ElevatorState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;

/**
 * An elevator with position control, built exactly like the arm: a "moving" and a "resting" state.
 */
@AutoCommandFactory(requestEnum = ElevatorRequest.class)
public class Elevator
    extends StateSubsystem<ElevatorRequest, ElevatorState, ElevatorIOInputsAutoLogged, ElevatorIO> {

  public Elevator(ElevatorIO io) {
    super(
        "Subsystems/Elevator",
        new ElevatorIOInputsAutoLogged(),
        io,
        ElevatorState.class,
        ElevatorState.AT_LOW,
        ElevatorRequest.LOW,
        ElevatorConstants.class);

    trip(ElevatorState.MOVING_TO_LOW, ElevatorState.AT_LOW, ElevatorConstants.LOW_METERS);
    trip(ElevatorState.MOVING_TO_MID, ElevatorState.AT_MID, ElevatorConstants.MID_METERS);
    trip(ElevatorState.MOVING_TO_HIGH, ElevatorState.AT_HIGH, ElevatorConstants.HIGH_METERS);

    bindRequest(ElevatorRequest.LOW, ElevatorState.MOVING_TO_LOW, ElevatorState.AT_LOW);
    bindRequest(ElevatorRequest.MID, ElevatorState.MOVING_TO_MID, ElevatorState.AT_MID);
    bindRequest(ElevatorRequest.HIGH, ElevatorState.MOVING_TO_HIGH, ElevatorState.AT_HIGH);
    fsm.validateComplete();
  }

  private void trip(ElevatorState moving, ElevatorState arrived, double meters) {
    double rotations = meters / ElevatorConstants.METERS_PER_ROTATION;
    fsm.state(moving)
        .onEnter(() -> io.controlMotor().runSmartPosition(Rotations.of(rotations)))
        .transitionTo(arrived, () -> inputs.motorInputs.atSetpoint, 0.1);
    fsm.state(arrived).onEnter(() -> io.controlMotor().runSmartPosition(Rotations.of(rotations)));
  }

  /** How high the carriage is, for the 3D robot. */
  public Distance getHeight() {
    return Meters.of(
        inputs.motorInputs.position.in(Rotations) * ElevatorConstants.METERS_PER_ROTATION);
  }
}
