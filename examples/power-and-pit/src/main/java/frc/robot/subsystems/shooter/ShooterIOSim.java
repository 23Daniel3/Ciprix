package frc.robot.subsystems.shooter;

import edu.wpi.first.math.system.plant.DCMotor;
import org.ciprix.interfaces.motor.MotorIOSim;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.sim.SimMechanism;

/** The simulated shooter. */
public class ShooterIOSim implements ShooterIO {
  private final MotorIOSim motor =
      MotorIOSim.talonFX(
          "Shooter/Motor",
          ShooterConstants.MOTOR_CONFIG,
          SimMechanism.flywheel(DCMotor.getKrakenX60(1), ShooterConstants.GEARING, 0.004));

  @Override
  public void updateInputs(ShooterIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
