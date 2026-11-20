package org.ciprix.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** An absolute sensor that wraps (0..360, 0..4096) becomes a continuous position. */
class ContinuousUnwrapperTest {

  @Test
  void rejectsANonPositiveRange() {
    assertThrows(IllegalArgumentException.class, () -> new ContinuousUnwrapper(0));
    assertThrows(IllegalArgumentException.class, () -> new ContinuousUnwrapper(-1));
  }

  @Test
  void rejectsReadingsOutsideTheRange() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(360);
    assertThrows(IllegalArgumentException.class, () -> unwrapper.update(-1));
    assertThrows(IllegalArgumentException.class, () -> unwrapper.update(361));
  }

  @Test
  void firstReadingIsThePosition() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(360);
    assertEquals(90.0, unwrapper.update(90), 1e-9);
  }

  @Test
  void followsSmallMovesBothWays() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(360);
    unwrapper.update(100);
    assertEquals(130.0, unwrapper.update(130), 1e-9);
    assertEquals(110.0, unwrapper.update(110), 1e-9);
  }

  @Test
  void keepsCountingForwardAcrossTheWrap() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(360);
    unwrapper.update(350);
    assertEquals(370.0, unwrapper.update(10), 1e-9);
    assertEquals(450.0, unwrapper.update(90), 1e-9);
    assertEquals(360.0, unwrapper.update(0), 1e-9); // 90 -> 0 is a 90 deg step back
  }

  @Test
  void keepsCountingBackwardAcrossTheWrap() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(4096);
    unwrapper.update(100);
    assertEquals(-100.0, unwrapper.update(3896), 1e-9);
  }

  @Test
  void resetMovesTheAccumulatedPosition() {
    ContinuousUnwrapper unwrapper = new ContinuousUnwrapper(360);
    unwrapper.update(200);
    unwrapper.reset();
    assertEquals(0.0, unwrapper.getAccumulatedPosition(), 1e-9);
    assertEquals(10.0, unwrapper.update(210), 1e-9);
    unwrapper.reset(1000);
    assertEquals(1005.0, unwrapper.update(215), 1e-9);
  }
}
