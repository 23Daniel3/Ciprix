package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import org.ciprix.calculus.TunableControls.ControlConstants;
import org.ciprix.calculus.TunableControls.TunableControlConstants;
import org.ciprix.calculus.TunableControls.TunablePIDController;
import org.ciprix.calculus.TunableControls.TunableProfiledController;
import org.junit.jupiter.api.Test;

class TunableControlsTest {

  private static int counter = 0;

  private static String key() {
    return "Test/Controls" + (counter++);
  }

  @Test
  void plainControllersCarryEverySetting() {
    ControlConstants constants =
        new ControlConstants()
            .withPID(2.0, 0.1, 0.3)
            .withTolerance(0.05, 0.5)
            .withIZone(1.0)
            .withPeriod(0.01)
            .withContinuous(-Math.PI, Math.PI);

    PIDController pid = constants.getPIDController();
    assertEquals(2.0, pid.getP());
    assertEquals(0.1, pid.getI());
    assertEquals(0.3, pid.getD());
    assertEquals(0.01, pid.getPeriod());
    assertEquals(1.0, pid.getIZone());
    assertEquals(0.05, pid.getErrorTolerance());
    assertEquals(0.5, pid.getErrorDerivativeTolerance());
    assertTrue(pid.isContinuousInputEnabled());

    ProfiledPIDController profiled = constants.withProfile(3.0, 6.0).getProfiledPIDController();
    assertEquals(0.01, profiled.getPeriod());
    assertEquals(3.0, profiled.getConstraints().maxVelocity);
    assertEquals(0.05, profiled.getPositionTolerance());
    assertEquals(0.5, profiled.getVelocityTolerance());
  }

  @Test
  void copyKeepsTheProfileFlag() {
    ControlConstants original = new ControlConstants().withProfile(1.0, 2.0);
    ControlConstants copy = new ControlConstants(original);
    // Without the flag a copy could not build a TunableProfiledController.
    new TunableProfiledController(new TunableControlConstants(key(), copy));
  }

  @Test
  void feedforwardsUseTheGains() {
    ControlConstants constants =
        new ControlConstants().withPhysical(0.2, 0.5).withFeedforward(2.0, 0.1);
    assertEquals(0.2, constants.getSimpleFeedforward().getKs());
    assertEquals(2.0, constants.getSimpleFeedforward().getKv());
    assertEquals(0.5, constants.getElevatorFeedforward().getKg());
  }

  @Test
  void tunableConstantsMirrorTheDefaults() {
    TunableControlConstants tunable =
        new TunableControlConstants(
            key(), new ControlConstants().withPID(1.0, 0, 0).withTolerance(0.1).withPeriod(0.01));
    assertEquals(14, tunable.getAllTunableNumbers().length);
    PIDController pid = tunable.getPIDController();
    assertEquals(1.0, pid.getP());
    assertEquals(0.01, pid.getPeriod());
    assertEquals(0.1, pid.getErrorTolerance());
  }

  @Test
  void tunablePidControllerAppliesTheIntegratorSettingsFromTheStart() {
    TunablePIDController pid =
        new TunablePIDController(
            new TunableControlConstants(
                key(),
                new ControlConstants()
                    .withPID(1.0, 10.0, 0)
                    .withIRange(-0.5, 0.5)
                    .withTolerance(0.1)));
    // Large error for many cycles: the integral term is clamped from the very first call.
    double output = 0;
    for (int i = 0; i < 100; i++) {
      output = pid.calculate(0.0, 1.0);
    }
    assertEquals(1.0 + 0.5, output, 1e-9);

    pid.reset();
    assertEquals(0.0, pid.getAccumulatedError(), 1e-12);
    pid.setSetpoint(2.0);
    assertEquals(2.0, pid.getSetpoint());
    pid.calculate(1.95);
    assertTrue(pid.atSetpoint());
    assertEquals(0.05, pid.getPositionError(), 1e-9);
  }

  @Test
  void profiledControllerNeedsAProfile() {
    TunableControlConstants notProfiled =
        new TunableControlConstants(key(), new ControlConstants().withPID(1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new TunableProfiledController(notProfiled));
  }

  @Test
  void profiledControllerAddsTheFeedforwardAndReachesTheGoal() {
    TunableProfiledController controller =
        new TunableProfiledController(
            new TunableControlConstants(
                key(),
                new ControlConstants()
                    .withPID(0.0, 0, 0)
                    .withPhysical(0.0, 0.7)
                    .withProfile(1.0, 2.0)
                    .withTolerance(0.01, 0.01)));
    controller.reset(0.0);
    controller.setGoal(1.0);
    assertEquals(1.0, controller.getGoal());
    // With no feedback only the gravity term is left once the profile is done.
    double output = 0;
    for (int i = 0; i < 200; i++) {
      output = controller.calculate(controller.getSetpoint().position);
    }
    assertEquals(0.7, output, 1e-9);
    assertEquals(1.0, controller.getSetpoint().position, 1e-9);
    assertTrue(controller.atGoal());
    assertFalse(Double.isNaN(controller.getVelocityError()));
  }
}
