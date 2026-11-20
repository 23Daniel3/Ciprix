package frc.robot.subsystems.flywheel;

import org.ciprix.interfaces.motor.MotorIOTalonSRX;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The flywheel on a CTRE Talon SRX (Phoenix 5) with an encoder on its data port. */
public class FlywheelIOTalonSRX implements FlywheelIO {
  private final MotorIO motor =
      new MotorIOTalonSRX(
          "Flywheel/Motor", FlywheelConstants.TALON_SRX_ID, FlywheelConstants.SRX_CONFIG);

  @Override
  public void updateInputs(FlywheelIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
