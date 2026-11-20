package org.ciprix.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** A double press fires only when the second press comes quickly after the first. */
class DoublePressTrackerTest {

  private boolean pressed = false;
  private Trigger doublePress;

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    SimHooks.pauseTiming();
    SimHooks.restartTiming();
    pressed = false;
    doublePress = DoublePressTracker.doublePress(new Trigger(() -> pressed));
  }

  @AfterEach
  void tearDown() {
    SimHooks.resumeTiming();
  }

  /** One robot cycle: set the button, step 20 ms, read the double-press trigger. */
  private boolean cycle(boolean buttonDown) {
    pressed = buttonDown;
    boolean value = doublePress.getAsBoolean();
    SimHooks.stepTiming(0.02);
    return value;
  }

  @Test
  void quickSecondPressFiresWhileHeld() {
    assertFalse(cycle(true));
    assertFalse(cycle(false));
    assertTrue(cycle(true));
    assertTrue(cycle(true));
    assertFalse(cycle(false), "releasing the second press ends it");
  }

  @Test
  void singlePressNeverFires() {
    assertFalse(cycle(true));
    for (int i = 0; i < 50; i++) {
      assertFalse(cycle(false));
    }
  }

  @Test
  void secondPressAfterTheWindowDoesNotFire() {
    cycle(true);
    cycle(false);
    SimHooks.stepTiming(DoublePressTracker.maxLengthSecs + 0.1);
    assertFalse(cycle(false)); // window expired, back to idle
    assertFalse(cycle(true), "this is a new first press");
    assertFalse(cycle(false));
    assertTrue(cycle(true));
  }

  @Test
  void holdingTheFirstPressTooLongCancelsIt() {
    cycle(true);
    SimHooks.stepTiming(DoublePressTracker.maxLengthSecs + 0.1);
    assertFalse(cycle(false));
    assertFalse(cycle(true), "a long first press does not count");
  }
}
