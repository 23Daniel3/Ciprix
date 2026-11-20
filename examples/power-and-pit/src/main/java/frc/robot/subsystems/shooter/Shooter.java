package frc.robot.subsystems.shooter;

import static edu.wpi.first.units.Units.RPM;

import frc.robot.subsystems.shooter.ShooterConstants.ShooterRequest;
import frc.robot.subsystems.shooter.ShooterConstants.ShooterState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;

/** A velocity-controlled shooter, ready when it is within tolerance of the target. */
@AutoCommandFactory(requestEnum = ShooterRequest.class)
public class Shooter
    extends StateSubsystem<ShooterRequest, ShooterState, ShooterIOInputsAutoLogged, ShooterIO> {

  public Shooter(ShooterIO io) {
    super(
        "Subsystems/Shooter",
        new ShooterIOInputsAutoLogged(),
        io,
        ShooterState.class,
        ShooterState.STOPPED,
        ShooterRequest.STOP,
        ShooterConstants.class);

    fsm.state(ShooterState.STOPPED).onEnter(() -> io.controlMotor().stop());
    fsm.state(ShooterState.SPINNING_UP)
        .onEnter(() -> io.controlMotor().runVelocity(RPM.of(ShooterConstants.TARGET_RPM)))
        .transitionTo(ShooterState.READY, () -> inputs.motorInputs.atSetpoint, 0.15);
    fsm.state(ShooterState.READY)
        .onEnter(() -> io.controlMotor().runVelocity(RPM.of(ShooterConstants.TARGET_RPM)))
        .transitionTo(ShooterState.SPINNING_UP, () -> !inputs.motorInputs.atSetpoint, 0.3);

    bindRequest(ShooterRequest.STOP, ShooterState.STOPPED, ShooterState.STOPPED);
    bindRequest(ShooterRequest.SPIN_UP, ShooterState.SPINNING_UP, ShooterState.READY);
    fsm.validateComplete();

    selfTest()
        .motor("Motor", io::controlMotor, () -> inputs.motorInputs)
        .output(0.3)
        .mustTurn(RPM.of(300));
  }

  /** True when the shooter is at speed (for the pre-match checklist and the driver's screen). */
  public boolean isReady() {
    return getState() == ShooterState.READY;
  }
}
