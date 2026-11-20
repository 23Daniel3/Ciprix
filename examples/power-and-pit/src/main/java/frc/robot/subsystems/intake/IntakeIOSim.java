package frc.robot.subsystems.intake;

import edu.wpi.first.math.system.plant.DCMotor;
import org.ciprix.interfaces.motor.MotorIOSim;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.sim.SimMechanism;

/**
 * The simulated intake. Its current counts in the simulated battery, and the same MotorConfig
 * (priority, stall reversal) applies as on the robot.
 */
public class IntakeIOSim implements IntakeIO {
  private final MotorIOSim motor =
      MotorIOSim.talonFX(
          "Intake/Motor",
          IntakeConstants.MOTOR_CONFIG,
          SimMechanism.roller(DCMotor.getKrakenX60(1), 3.0, 0.002).withFriction(0.05, 0.0));

  @Override
  public void updateInputs(IntakeIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
