package frc.robot.subsystems.shooter;

import com.ctre.phoenix6.CANBus;
import org.ciprix.interfaces.motor.MotorIOTalonFX;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The shooter on the robot: a Talon FX on the roboRIO CAN bus. */
public class ShooterIOTalonFX implements ShooterIO {
  private final MotorIO motor =
      new MotorIOTalonFX(
          "Shooter/Motor", ShooterConstants.MOTOR_ID, new CANBus(), ShooterConstants.MOTOR_CONFIG);

  @Override
  public void updateInputs(ShooterIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
