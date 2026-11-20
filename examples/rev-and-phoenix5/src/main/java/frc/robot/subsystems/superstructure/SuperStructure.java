package frc.robot.subsystems.superstructure;

import frc.robot.Constants.RobotRequest;
import frc.robot.Constants.RobotState;
import frc.robot.subsystems.flywheel.Flywheel;
import frc.robot.subsystems.flywheel.FlywheelConstants.FlywheelRequest;
import org.ciprix.interfaces.subsystem.SuperStructureBase;

/** The overall state machine: shooting means the flywheel spins up; idle means it stops. */
public class SuperStructure extends SuperStructureBase<RobotRequest, RobotState> {

  private final Flywheel flywheel;
  private FlywheelRequest flywheelRequest = FlywheelRequest.STOP;

  public SuperStructure(Flywheel flywheel) {
    super("Subsystems/SuperStructure", RobotState.class, RobotState.IDLE, RobotRequest.IDLE);
    this.flywheel = flywheel;

    generalFsm.state(RobotState.IDLE).onEnter(() -> flywheelRequest = FlywheelRequest.STOP);
    generalFsm.state(RobotState.SHOOTING).onEnter(() -> flywheelRequest = FlywheelRequest.SPIN_UP);

    bindRequest(RobotRequest.IDLE, RobotState.IDLE);
    bindRequest(RobotRequest.SHOOT, RobotState.SHOOTING);
    generalFsm.validateComplete();
  }

  /** The flywheel's request, read every cycle by the generated default command. */
  public FlywheelRequest getFlywheelRequest() {
    return flywheelRequest;
  }

  /** True when the flywheel is at speed: a feeder would wait for this before it shoots. */
  public boolean isFlywheelReady() {
    return flywheel.atGoal();
  }
}
