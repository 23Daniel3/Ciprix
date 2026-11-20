package frc.robot.subsystems.intake;

import static edu.wpi.first.units.Units.RotationsPerSecond;

import frc.robot.subsystems.intake.IntakeConstants.IntakeRequest;
import frc.robot.subsystems.intake.IntakeConstants.IntakeState;
import org.ciprix.annotations.AutoCommandFactory;
import org.ciprix.interfaces.subsystem.StateSubsystem;
import org.ciprix.util.PersistentAlert;
import org.ciprix.util.PersistentAlert.Level;

/**
 * A roller with the protections of {@link IntakeConstants#MOTOR_CONFIG}. The subsystem itself adds
 * one thing: an alert on the dashboard while the motor is un-jamming itself, so the driver knows
 * why the intake is going backwards.
 */
@AutoCommandFactory(requestEnum = IntakeRequest.class)
public class Intake
    extends StateSubsystem<IntakeRequest, IntakeState, IntakeIOInputsAutoLogged, IntakeIO> {

  private final PersistentAlert unjamAlert =
      new PersistentAlert("Intake jammed: reversing to clear it", Level.WARNING);

  public Intake(IntakeIO io) {
    super(
        "Subsystems/Intake",
        new IntakeIOInputsAutoLogged(),
        io,
        IntakeState.class,
        IntakeState.STOPPED,
        IntakeRequest.STOP,
        IntakeConstants.class);

    fsm.state(IntakeState.STOPPED).onEnter(() -> io.controlMotor().stop());
    fsm.state(IntakeState.INTAKING)
        .onEnter(() -> io.controlMotor().runPercentOutput(IntakeConstants.INTAKE_OUTPUT));

    bindRequest(IntakeRequest.STOP, IntakeState.STOPPED, IntakeState.STOPPED);
    bindRequest(IntakeRequest.INTAKE, IntakeState.INTAKING, IntakeState.INTAKING);
    fsm.validateComplete();

    selfTest()
        .motor("Motor", io::controlMotor, () -> inputs.motorInputs)
        .output(0.3)
        .mustTurn(RotationsPerSecond.of(IntakeConstants.MIN_SPEED_RPS));
  }

  @Override
  protected void sPeriodic() {
    unjamAlert.set(inputs.motorInputs.unjamming);
  }
}
