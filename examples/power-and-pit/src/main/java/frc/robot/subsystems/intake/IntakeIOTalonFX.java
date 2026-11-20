package frc.robot.subsystems.intake;

import com.ctre.phoenix6.CANBus;
import org.ciprix.interfaces.motor.MotorIOTalonFX;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The intake on the robot: a Talon FX on the roboRIO CAN bus. */
public class IntakeIOTalonFX implements IntakeIO {
  private final MotorIO motor =
      new MotorIOTalonFX(
          "Intake/Motor", IntakeConstants.MOTOR_ID, new CANBus(), IntakeConstants.MOTOR_CONFIG);

  @Override
  public void updateInputs(IntakeIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
