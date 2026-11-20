package frc.robot.subsystems.superstructure;

import frc.robot.Constants.RobotRequest;
import frc.robot.Constants.RobotState;
import frc.robot.subsystems.intake.IntakeConstants.IntakeRequest;
import frc.robot.subsystems.shooter.ShooterConstants.ShooterRequest;
import org.ciprix.interfaces.subsystem.SuperStructureBase;

/** The overall state machine: what the intake and the shooter do in each state. */
public class SuperStructure extends SuperStructureBase<RobotRequest, RobotState> {

  private IntakeRequest intakeRequest = IntakeRequest.STOP;
  private ShooterRequest shooterRequest = ShooterRequest.STOP;

  public SuperStructure() {
    super("Subsystems/SuperStructure", RobotState.class, RobotState.IDLE, RobotRequest.IDLE);

    generalFsm.state(RobotState.IDLE).onEnter(() -> set(IntakeRequest.STOP, ShooterRequest.STOP));
    generalFsm
        .state(RobotState.INTAKING)
        .onEnter(() -> set(IntakeRequest.INTAKE, ShooterRequest.STOP));
    generalFsm
        .state(RobotState.SHOOTING)
        .onEnter(() -> set(IntakeRequest.STOP, ShooterRequest.SPIN_UP));

    bindRequest(RobotRequest.IDLE, RobotState.IDLE);
    bindRequest(RobotRequest.INTAKE, RobotState.INTAKING);
    bindRequest(RobotRequest.SHOOT, RobotState.SHOOTING);
    generalFsm.validateComplete();
  }

  private void set(IntakeRequest intake, ShooterRequest shooter) {
    intakeRequest = intake;
    shooterRequest = shooter;
  }

  /** Read every cycle by the generated default command. */
  public IntakeRequest getIntakeRequest() {
    return intakeRequest;
  }

  /** Read every cycle by the generated default command. */
  public ShooterRequest getShooterRequest() {
    return shooterRequest;
  }
}
