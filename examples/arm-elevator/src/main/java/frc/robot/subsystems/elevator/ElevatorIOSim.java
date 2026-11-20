package frc.robot.subsystems.elevator;

import edu.wpi.first.math.system.plant.DCMotor;
import org.ciprix.interfaces.motor.MotorIOSim;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.sim.SimMechanism;

/** The simulated elevator: the carriage's weight, friction and the end of the travel. */
public class ElevatorIOSim implements ElevatorIO {
  private final MotorIOSim motor =
      MotorIOSim.talonFX(
          "Elevator/Motor",
          ElevatorConstants.MOTOR_CONFIG,
          SimMechanism.elevator(
                  DCMotor.getKrakenX60(1),
                  ElevatorConstants.GEARING,
                  ElevatorConstants.CARRIAGE_MASS_KG,
                  ElevatorConstants.DRUM_RADIUS_METERS)
              .withLimits(
                  0.0, ElevatorConstants.HIGH_METERS / ElevatorConstants.METERS_PER_ROTATION + 0.1)
              .withFriction(0.2, 0.0));

  @Override
  public void updateInputs(ElevatorIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
