package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.ciprix.calculus.LinearInterpolation.Point;
import org.junit.jupiter.api.Test;

/**
 * Linear interpolation is the base of the shooter calibration maps (distance -> RPM/angle); getting
 * the clamp or the segment wrong means shooting wrong across the whole range.
 */
class LinearInterpolationTest {

  private final LinearInterpolation map =
      new LinearInterpolation(new Point(1.0, 100.0), new Point(2.0, 200.0), new Point(4.0, 300.0));

  @Test
  void returnsExactValuesAtKnownPoints() {
    assertEquals(100.0, map.calculate(1.0), 1e-9);
    assertEquals(200.0, map.calculate(2.0), 1e-9);
    assertEquals(300.0, map.calculate(4.0), 1e-9);
  }

  @Test
  void interpolatesWithinSegments() {
    assertEquals(150.0, map.calculate(1.5), 1e-9);
    // segment 2..4 has a different slope (50/unit): it picks the right segment
    assertEquals(250.0, map.calculate(3.0), 1e-9);
  }

  @Test
  void clampsOutsideRange() {
    assertEquals(100.0, map.calculate(0.0), 1e-9, "below the first point: clamp to the first y");
    assertEquals(300.0, map.calculate(10.0), 1e-9, "above the last point: clamp to the last y");
  }
}
