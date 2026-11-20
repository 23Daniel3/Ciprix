package frc.robot.subsystems.flywheel;

import static edu.wpi.first.units.Units.RPM;

import frc.robot.subsystems.flywheel.FlywheelConstants.FlywheelRequest;
import frc.robot.subsystems.flywheel.FlywheelConstants.FlywheelState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;

/**
 * A velocity-controlled flywheel. This class is identical for every motor controller: it only calls
 * {@code controlMotor().runVelocity(...)}, and the IO decides whether that goes to a SPARK MAX, a
 * Talon SRX or the simulation.
 */
@AutoCommandFactory(requestEnum = FlywheelRequest.class)
public class Flywheel
    extends StateSubsystem<FlywheelRequest, FlywheelState, FlywheelIOInputsAutoLogged, FlywheelIO> {

  public Flywheel(FlywheelIO io) {
    super(
        "Subsystems/Flywheel",
        new FlywheelIOInputsAutoLogged(),
        io,
        FlywheelState.class,
        FlywheelState.STOPPED,
        FlywheelRequest.STOP,
        FlywheelConstants.class);

    fsm.state(FlywheelState.STOPPED).onEnter(() -> io.controlMotor().stop());
    fsm.state(FlywheelState.SPINNING_UP)
        .onEnter(() -> io.controlMotor().runVelocity(RPM.of(FlywheelConstants.TARGET_RPM)))
        .transitionTo(FlywheelState.READY, () -> inputs.motorInputs.atSetpoint, 0.15);
    fsm.state(FlywheelState.READY)
        .onEnter(() -> io.controlMotor().runVelocity(RPM.of(FlywheelConstants.TARGET_RPM)))
        .transitionTo(FlywheelState.SPINNING_UP, () -> !inputs.motorInputs.atSetpoint, 0.3);

    bindRequest(FlywheelRequest.STOP, FlywheelState.STOPPED, FlywheelState.STOPPED);
    bindRequest(FlywheelRequest.SPIN_UP, FlywheelState.SPINNING_UP, FlywheelState.READY);
    fsm.validateComplete();

    selfTest()
        .motor("Motor", io::controlMotor, () -> inputs.motorInputs)
        .output(0.3)
        .mustTurn(RPM.of(300));
  }
}
