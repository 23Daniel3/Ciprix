package frc.robot.subsystems.arm;

import com.ctre.phoenix6.CANBus;
import org.ciprix.interfaces.motor.MotorIOTalonFX;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The arm on the robot: a Kraken X60 (Talon FX) on the roboRIO CAN bus. */
public class ArmIOTalonFX implements ArmIO {
  private final MotorIO motor =
      new MotorIOTalonFX(
          "Arm/Motor", ArmConstants.MOTOR_ID, new CANBus(), ArmConstants.MOTOR_CONFIG);

  @Override
  public void updateInputs(ArmIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
