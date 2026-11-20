package org.ciprix.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A latched boolean reports only the rising edge, so a held button fires once. */
class LatchedBooleanTest {

  @Test
  void trueOnlyOnTheRisingEdge() {
    LatchedBoolean latch = new LatchedBoolean();
    assertFalse(latch.update(false));
    assertTrue(latch.update(true));
    assertFalse(latch.update(true));
    assertFalse(latch.update(true));
    assertFalse(latch.update(false));
    assertTrue(latch.update(true));
  }

  @Test
  void startingTrueCountsAsARisingEdge() {
    assertTrue(new LatchedBoolean().update(true));
  }
}
