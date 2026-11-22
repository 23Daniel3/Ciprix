package org.ciprix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.ciprix.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VirtualSubsystemTest {

  private final TestEnvironment env = new TestEnvironment();

  @BeforeEach
  void setUp() {
    env.install();
  }

  @AfterEach
  void tearDown() {
    env.uninstall();
  }

  private static final class Counter extends VirtualSubsystem {
    int runs = 0;

    Counter() {
      super("Counter");
    }

    @Override
    public void periodic() {
      runs++;
    }
  }

  @Test
  void runsOncePerCycleWithoutExtraWiring() {
    Counter counter = new Counter();
    RobotLoop.runOnce();
    RobotLoop.runOnce();
    assertEquals(2, counter.runs);
    assertEquals("Counter", counter.getName());
  }
}
