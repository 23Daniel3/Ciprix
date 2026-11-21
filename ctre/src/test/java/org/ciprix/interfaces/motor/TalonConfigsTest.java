package org.ciprix.interfaces.motor;

import static edu.wpi.first.units.Units.Amps;
import static edu.wpi.first.units.Units.Rotations;
import static edu.wpi.first.units.Units.RotationsPerSecond;
import static edu.wpi.first.units.Units.RotationsPerSecondPerSecond;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctre.phoenix6.StatusCode;
import com.ctre.phoenix6.configs.ClosedLoopGeneralConfigs;
import com.ctre.phoenix6.configs.ClosedLoopRampsConfigs;
import com.ctre.phoenix6.configs.CurrentLimitsConfigs;
import com.ctre.phoenix6.configs.MotionMagicConfigs;
import com.ctre.phoenix6.configs.MotorOutputConfigs;
import com.ctre.phoenix6.configs.OpenLoopRampsConfigs;
import com.ctre.phoenix6.configs.Slot1Configs;
import com.ctre.phoenix6.configs.SoftwareLimitSwitchConfigs;
import com.ctre.phoenix6.configs.VoltageConfigs;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.InvertedValue;
import com.ctre.phoenix6.signals.NeutralModeValue;
import java.util.ArrayList;
import java.util.List;
import org.ciprix.interfaces.motor.advanced.MotorConfig;
import org.ciprix.interfaces.motor.advanced.MotorConfig.FeedbackSensorType;
import org.ciprix.interfaces.motor.advanced.MotorConfig.GravityType;
import org.ciprix.util.Report;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** How a Ciprix {@link MotorConfig} becomes Phoenix 6 configuration, without a device. */
class TalonConfigsTest {

  private final List<String> reports = new ArrayList<>();

  @BeforeEach
  void captureReports() {
    reports.clear();
    Report.setSink((isError, message, printTrace) -> reports.add(message));
  }

  @AfterEach
  void restoreReports() {
    Report.resetSink();
  }

  @Test
  void outputFollowsInversionAndNeutralMode() {
    MotorOutputConfigs output = new MotorOutputConfigs();
    TalonConfigs.writeOutput(output, true, true);
    assertEquals(InvertedValue.Clockwise_Positive, output.Inverted);
    assertEquals(NeutralModeValue.Brake, output.NeutralMode);
    TalonConfigs.writeOutput(output, false, false);
    assertEquals(InvertedValue.CounterClockwise_Positive, output.Inverted);
    assertEquals(NeutralModeValue.Coast, output.NeutralMode);
  }

  @Test
  void bothCurrentLimitsAreEnabled() {
    CurrentLimitsConfigs limits = new CurrentLimitsConfigs();
    TalonConfigs.writeCurrentLimits(limits, 60, 40);
    assertEquals(60, limits.StatorCurrentLimit);
    assertEquals(40, limits.SupplyCurrentLimit);
    assertTrue(limits.StatorCurrentLimitEnable);
    assertTrue(limits.SupplyCurrentLimitEnable);
  }

  @Test
  void supplyLimitFallsBackToTheStatorLimit() {
    MotorConfig config = new MotorConfig();
    config.currentLimit = Amps.of(-50);
    assertEquals(50, TalonConfigs.supplyLimitAmps(config));
    config.supplyCurrentLimit = Amps.of(30);
    assertEquals(30, TalonConfigs.supplyLimitAmps(config));
  }

  @Test
  void peaksScaleTheNominalVoltage() {
    VoltageConfigs voltage = new VoltageConfigs();
    TalonConfigs.writePeaks(voltage, -0.5, 0.75, 12.0);
    assertEquals(9.0, voltage.PeakForwardVoltage, 1e-9);
    assertEquals(-6.0, voltage.PeakReverseVoltage, 1e-9);
  }

  @Test
  void rampsAreNeverNegative() {
    OpenLoopRampsConfigs open = new OpenLoopRampsConfigs();
    ClosedLoopRampsConfigs closed = new ClosedLoopRampsConfigs();
    TalonConfigs.writeRamps(open, closed, 0.3, -1.0);
    assertEquals(0.3, open.VoltageOpenLoopRampPeriod);
    assertEquals(0.3, open.DutyCycleOpenLoopRampPeriod);
    assertEquals(0.0, closed.VoltageClosedLoopRampPeriod);
    assertEquals(0.0, closed.DutyCycleClosedLoopRampPeriod);
  }

  @Test
  void softLimitsAreOnlyWrittenWhenEnabled() {
    MotorConfig config = new MotorConfig();
    SoftwareLimitSwitchConfigs limits = new SoftwareLimitSwitchConfigs();
    TalonConfigs.writeSoftLimits(limits, config);
    assertFalse(limits.ForwardSoftLimitEnable);

    config.softLimitEnabled = true;
    config.minPosition = Rotations.of(-0.25);
    config.maxPosition = Rotations.of(2.0);
    TalonConfigs.writeSoftLimits(limits, config);
    assertTrue(limits.ForwardSoftLimitEnable);
    assertTrue(limits.ReverseSoftLimitEnable);
    assertEquals(2.0, limits.ForwardSoftLimitThreshold);
    assertEquals(-0.25, limits.ReverseSoftLimitThreshold);
  }

  @Test
  void continuousWrapNeedsExactlyOneRotation() {
    MotorConfig config = new MotorConfig();
    config.positionWrap = true;
    config.minPosition = Rotations.of(-0.5);
    config.maxPosition = Rotations.of(0.5);
    ClosedLoopGeneralConfigs general = new ClosedLoopGeneralConfigs();
    TalonConfigs.writeWrap(general, config, "Arm");
    assertTrue(general.ContinuousWrap);
    assertTrue(reports.isEmpty());

    config.maxPosition = Rotations.of(1.5);
    ClosedLoopGeneralConfigs other = new ClosedLoopGeneralConfigs();
    TalonConfigs.writeWrap(other, config, "Arm");
    assertFalse(other.ContinuousWrap);
    assertEquals(1, reports.size());
  }

  @Test
  void motionMagicValuesAreNeverNegative() {
    MotionMagicConfigs motion = new MotionMagicConfigs();
    TalonConfigs.writeMotion(motion, -1.0, 4.0);
    assertEquals(0.0, motion.MotionMagicCruiseVelocity);
    assertEquals(4.0, motion.MotionMagicAcceleration);
  }

  @Test
  void gainsAndGravityGoToTheRightSlot() {
    MotorConfig config = new MotorConfig();
    config.kP[1] = 1.0;
    config.kI[1] = 2.0;
    config.kD[1] = 3.0;
    config.kS[1] = 4.0;
    config.kV[1] = 5.0;
    config.kA[1] = 6.0;
    config.kG[1] = 7.0;
    config.gravityType = GravityType.ARM;
    Slot1Configs slot = new Slot1Configs();
    TalonConfigs.writeGains(slot, config, 1);
    assertEquals(1.0, slot.kP);
    assertEquals(2.0, slot.kI);
    assertEquals(3.0, slot.kD);
    assertEquals(4.0, slot.kS);
    assertEquals(5.0, slot.kV);
    assertEquals(6.0, slot.kA);
    assertEquals(7.0, slot.kG);
    assertEquals(GravityTypeValue.Arm_Cosine, slot.GravityType);

    config.gravityType = GravityType.ELEVATOR;
    assertEquals(GravityTypeValue.Elevator_Static, TalonConfigs.gravity(config));
  }

  @Test
  void slotThreeIsUsedOnlyWhenSomethingIsSetInIt() {
    MotorConfig config = new MotorConfig();
    assertFalse(TalonConfigs.usesSlot3(config));
    config.maxMotionMaxVelocity[3] = RotationsPerSecond.of(2.0);
    assertTrue(TalonConfigs.usesSlot3(config));
    assertEquals(2.0, TalonConfigs.cruise(config, 3));

    MotorConfig withGain = new MotorConfig();
    withGain.kG[3] = 0.1;
    assertTrue(TalonConfigs.usesSlot3(withGain));

    withGain.maxMotionMaxAcceleration[0] = RotationsPerSecondPerSecond.of(8.0);
    assertEquals(8.0, TalonConfigs.acceleration(withGain, 0));
  }

  @Test
  void sensorToMechanismRatioIsTheInverseOfTheConversion() {
    MotorConfig config = new MotorConfig();
    config.positionConversionFactor = 0.1; // 10:1 reduction
    assertEquals(10.0, TalonConfigs.sensorToMechanismRatio(config), 1e-9);
    config.positionConversionFactor = 0.0;
    assertEquals(1.0, TalonConfigs.sensorToMechanismRatio(config));
  }

  @Test
  void canCoderFeedbackNeedsTheCanId() {
    MotorConfig config = new MotorConfig();
    config.feedbackType = FeedbackSensorType.FUSED_CANCODER;
    assertEquals(FeedbackSensorType.INTERNAL, TalonConfigs.resolveSensor(config, "Arm", false));
    assertEquals(1, reports.size());

    config.remoteSensorId = 21;
    assertEquals(
        FeedbackSensorType.FUSED_CANCODER, TalonConfigs.resolveSensor(config, "Arm", false));
    assertEquals(21, TalonConfigs.remoteSensorId(config));
  }

  @Test
  void fusionTypeIsUsedWhenTheFeedbackIsNotACanCoder() {
    MotorConfig config = new MotorConfig();
    config.externalFusionType = FeedbackSensorType.SYNC_CANCODER;
    config.remoteSensorId = 5;
    assertEquals(
        FeedbackSensorType.SYNC_CANCODER, TalonConfigs.resolveSensor(config, "Arm", false));
  }

  @Test
  void externalCanCoderWithSoftwareOffsetWarnsOnTalonFeedback() {
    edu.wpi.first.hal.HAL.initialize(500, 0);
    org.ciprix.interfaces.encoder.EncoderIOCANcoder cancoder =
        new org.ciprix.interfaces.encoder.EncoderIOCANcoder(
            "ArmCANcoder",
            49,
            new org.ciprix.interfaces.encoder.EncoderConfig().offsetRotations(0.25));
    reports.clear();

    MotorConfig config =
        new MotorConfig().withExternalEncoder(cancoder, FeedbackSensorType.FUSED_CANCODER);
    assertEquals(
        FeedbackSensorType.FUSED_CANCODER, TalonConfigs.resolveSensor(config, "Arm", false));
    assertEquals(1, reports.size());
    assertTrue(reports.get(0).contains("software offset"));
  }

  @Test
  void dataPortSensorsOnlyExistOnTheTalonFxs() {
    MotorConfig config = new MotorConfig();
    config.feedbackType = FeedbackSensorType.ALTERNATE;
    assertEquals(FeedbackSensorType.ALTERNATE, TalonConfigs.resolveSensor(config, "M", true));
    assertTrue(reports.isEmpty());
    assertEquals(FeedbackSensorType.INTERNAL, TalonConfigs.resolveSensor(config, "M", false));
    assertEquals(1, reports.size());
  }

  @Test
  void bootConfigurationRetriesThenReports() {
    int[] attempts = {0};
    TalonConfigs.applyAtBoot(
        "M",
        () -> {
          attempts[0]++;
          return attempts[0] < 3 ? StatusCode.TxTimeout : StatusCode.OK;
        });
    assertEquals(3, attempts[0]);
    assertTrue(reports.isEmpty());

    attempts[0] = 0;
    TalonConfigs.applyAtBoot(
        "M",
        () -> {
          attempts[0]++;
          return StatusCode.TxTimeout;
        });
    assertEquals(TalonConfigs.BOOT_ATTEMPTS, attempts[0]);
    assertEquals(1, reports.size());
  }
}
