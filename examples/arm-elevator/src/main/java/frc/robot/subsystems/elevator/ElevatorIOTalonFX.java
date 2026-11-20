package frc.robot.subsystems.elevator;

import com.ctre.phoenix6.CANBus;
import org.ciprix.interfaces.motor.MotorIOTalonFX;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The elevator on the robot: a Kraken X60 (Talon FX) on the roboRIO CAN bus. */
public class ElevatorIOTalonFX implements ElevatorIO {
  private final MotorIO motor =
      new MotorIOTalonFX(
          "Elevator/Motor",
          ElevatorConstants.MOTOR_ID,
          new CANBus(),
          ElevatorConstants.MOTOR_CONFIG);

  @Override
  public void updateInputs(ElevatorIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
