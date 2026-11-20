package frc.robot.subsystems.roller;

import static edu.wpi.first.units.Units.RotationsPerSecond;

import frc.robot.subsystems.roller.RollerConstants.RollerRequest;
import frc.robot.subsystems.roller.RollerConstants.RollerState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;

/**
 * A roller (simple intake), the smallest possible subsystem in the pattern: each request leads to a
 * state and each state says what the motor does. {@code @AutoCommandFactory} generates {@code
 * RollerCommands}, with the default command (the SuperStructure relay) and one command per request
 * for the co-driver.
 */
@AutoCommandFactory(requestEnum = RollerRequest.class)
public class Roller
    extends StateSubsystem<RollerRequest, RollerState, RollerIOInputsAutoLogged, RollerIO> {

  public Roller(RollerIO io) {
    super(
        "Subsystems/Roller",
        new RollerIOInputsAutoLogged(),
        io,
        RollerState.class,
        RollerState.STOPPED,
        RollerRequest.STOP,
        RollerConstants.class);

    fsm.state(RollerState.STOPPED).onEnter(() -> io.controlMotor().stop());
    fsm.state(RollerState.INTAKING)
        .onEnter(() -> io.controlMotor().runPercentOutput(RollerConstants.INTAKE_OUTPUT));
    fsm.state(RollerState.EJECTING)
        .onEnter(() -> io.controlMotor().runPercentOutput(RollerConstants.EJECT_OUTPUT));

    // Request -> (entry state, goal state).
    bindRequest(RollerRequest.STOP, RollerState.STOPPED, RollerState.STOPPED);
    bindRequest(RollerRequest.INTAKE, RollerState.INTAKING, RollerState.INTAKING);
    bindRequest(RollerRequest.EJECT, RollerState.EJECTING, RollerState.EJECTING);
    fsm.validateComplete();

    // Pit self-test: spins the motor briefly and checks current and speed.
    selfTest()
        .motor("Motor", io::controlMotor, () -> inputs.motorInputs)
        .output(0.3)
        .mustTurn(RotationsPerSecond.of(5));
  }
}
