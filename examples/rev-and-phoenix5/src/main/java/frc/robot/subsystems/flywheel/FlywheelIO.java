package frc.robot.subsystems.flywheel;

import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorControllerNone;
import org.ciprix.interfaces.motor.advanced.MotorIO.MotorIOInputs;
import org.ciprix.interfaces.subsystem.SubsystemIO;
import org.littletonrobotics.junction.AutoLog;

/** The flywheel's hardware layer: the same for every vendor. */
public interface FlywheelIO extends SubsystemIO<FlywheelIOInputsAutoLogged> {
  @AutoLog
  class FlywheelIOInputs {
    public MotorIOInputs motorInputs = new MotorIOInputs();
  }

  @Override
  default void updateInputs(FlywheelIOInputsAutoLogged inputs) {}

  default MotorController controlMotor() {
    return new MotorControllerNone();
  }
}
