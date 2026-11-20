package frc.robot.subsystems.flywheel;

import org.ciprix.interfaces.motor.MotorIOSparkMax;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorIO;

/** The flywheel on a REV SPARK MAX with a NEO (brushless). */
public class FlywheelIOSparkMax implements FlywheelIO {
  private final MotorIO motor =
      new MotorIOSparkMax(
          "Flywheel/Motor", FlywheelConstants.SPARK_MAX_ID, FlywheelConstants.SPARK_CONFIG);

  @Override
  public void updateInputs(FlywheelIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
