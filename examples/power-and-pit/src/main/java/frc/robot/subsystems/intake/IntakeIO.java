package frc.robot.subsystems.intake;

import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorControllerNone;
import org.ciprix.interfaces.motor.advanced.MotorIO.MotorIOInputs;
import org.ciprix.interfaces.subsystem.SubsystemIO;
import org.littletonrobotics.junction.AutoLog;

/** The intake's hardware layer. */
public interface IntakeIO extends SubsystemIO<IntakeIOInputsAutoLogged> {
  @AutoLog
  class IntakeIOInputs {
    public MotorIOInputs motorInputs = new MotorIOInputs();
  }

  @Override
  default void updateInputs(IntakeIOInputsAutoLogged inputs) {}

  default MotorController controlMotor() {
    return new MotorControllerNone();
  }
}
