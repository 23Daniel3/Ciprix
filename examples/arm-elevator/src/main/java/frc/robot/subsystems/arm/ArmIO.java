package frc.robot.subsystems.arm;

import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorControllerNone;
import org.ciprix.interfaces.motor.advanced.MotorIO.MotorIOInputs;
import org.ciprix.interfaces.subsystem.SubsystemIO;
import org.littletonrobotics.junction.AutoLog;

/** The arm's hardware layer. */
public interface ArmIO extends SubsystemIO<ArmIOInputsAutoLogged> {
  @AutoLog
  class ArmIOInputs {
    public MotorIOInputs motorInputs = new MotorIOInputs();
  }

  @Override
  default void updateInputs(ArmIOInputsAutoLogged inputs) {}

  default MotorController controlMotor() {
    return new MotorControllerNone();
  }
}
