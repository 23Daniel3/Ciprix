package org.ciprix.util;

/**
 * Base class for logic that needs to run once per robot cycle but is not a mechanism: it has no
 * requirements, no default command and never shows up as a subsystem on the dashboard.
 *
 * <p>Extend it and implement {@link #periodic()}. The instance registers itself with {@link
 * RobotLoop} in the constructor, so there is nothing else to wire:
 *
 * <pre>{@code
 * public class BatteryLed extends VirtualSubsystem {
 *   public BatteryLed() {
 *     super("BatteryLed");
 *   }
 *
 *   @Override
 *   public void periodic() {
 *     led.setLowBattery(power.isBatteryLow());
 *   }
 * }
 * }</pre>
 *
 * <p>Like every {@link RobotLoop} entry, an exception thrown by {@link #periodic()} is reported
 * once and does not stop the other entries.
 */
public abstract class VirtualSubsystem {

  private final String name;

  /**
   * @param name name used by the periodic profiler ({@link PeriodicTimer}) and in error reports
   */
  protected VirtualSubsystem(String name) {
    this.name = name;
    RobotLoop.addPeriodic(name, this::periodic);
  }

  /** Called once per robot cycle. */
  public abstract void periodic();

  /** Name given to the constructor. */
  public String getName() {
    return name;
  }
}
