package frc.robot.subsystems.arm;

import edu.wpi.first.math.system.plant.DCMotor;
import org.ciprix.interfaces.motor.MotorIOSim;
import org.ciprix.interfaces.motor.advanced.MotorController;
import org.ciprix.sim.SimMechanism;

/** The simulated arm: gravity, friction and travel limits, with the robot's configuration. */
public class ArmIOSim implements ArmIO {
  private final MotorIOSim motor =
      MotorIOSim.talonFX(
          "Arm/Motor",
          ArmConstants.MOTOR_CONFIG,
          SimMechanism.arm(
                  DCMotor.getKrakenX60(1),
                  ArmConstants.GEARING,
                  ArmConstants.MASS_KG,
                  ArmConstants.LENGTH_METERS)
              .withLimits(ArmConstants.MIN_ROTATIONS, ArmConstants.MAX_ROTATIONS)
              .withStartPosition(ArmConstants.STOW_ROTATIONS)
              .withFriction(0.1, 0.0));

  @Override
  public void updateInputs(ArmIOInputsAutoLogged inputs) {
    motor.updateInputs(inputs.motorInputs);
  }

  @Override
  public MotorController controlMotor() {
    return motor.getMotorController();
  }
}
