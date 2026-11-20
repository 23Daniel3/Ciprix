package frc.robot.subsystems.flywheel;

import edu.wpi.first.math.system.plant.DCMotor;
import org.ciprix.interfaces.motor.MotorIOSim;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.sim.SimMechanism;

/** The simulated flywheel, using the SPARK MAX configuration (a NEO). */
public class FlywheelIOSim implements FlywheelIO {
  private final MotorIOSim motor =
      MotorIOSim.spark(
          "Flywheel/Motor",
          FlywheelConstants.SPARK_CONFIG,
          SimMechanism.flywheel(DCMotor.getNEO(1), FlywheelConstants.GEARING, 0.004));

  @Override
  public void updateInputs(FlywheelIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
