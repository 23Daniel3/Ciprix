package frc.robot.subsystems.shooter;

import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.interfaces.motor.advanced.MotorControllerNone;
import org.ciprix.interfaces.motor.advanced.MotorIO.MotorIOInputs;
import org.ciprix.interfaces.subsystem.SubsystemIO;
import org.littletonrobotics.junction.AutoLog;

/** The shooter's hardware layer. */
public interface ShooterIO extends SubsystemIO<ShooterIOInputsAutoLogged> {
  @AutoLog
  class ShooterIOInputs {
    public MotorIOInputs motorInputs = new MotorIOInputs();
  }

  @Override
  default void updateInputs(ShooterIOInputsAutoLogged inputs) {}

  default MotorController controlMotor() {
    return new MotorControllerNone();
  }
}
