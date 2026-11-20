package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ciprix.calculus.LinearInterpolation.Point;
import org.junit.jupiter.api.Test;

/** Outside tuning mode the map is the {@link ThrottleMap} of its default points. */
class LoggedTunableMapTest {

  private static int counter = 0;

  private static String name() {
    return "Test/Map" + (counter++);
  }

  @Test
  void linearMapFromPoints() {
    LoggedTunableMap map =
        new LoggedTunableMap(name(), false, new Point(1, 1000), new Point(3, 3000));
    assertEquals(2000.0, map.applyThrottle(2.0), 1e-9);
    assertEquals(3000.0, map.applyThrottle(10.0), 1e-9);
  }

  @Test
  void pointsAreSortedByInput() {
    LoggedTunableMap map =
        new LoggedTunableMap(name(), new double[] {3, 1}, new double[] {3000, 1000}, false);
    assertEquals(1500.0, map.applyThrottle(1.5), 1e-9);
  }

  @Test
  void defaultRampIsTheIdentity() {
    LoggedTunableMap map = new LoggedTunableMap(name(), 5, false);
    assertEquals(0.3, map.applyThrottle(0.3), 1e-9);
    assertEquals(-0.3, map.applyThrottleAbs(-0.3), 1e-9);
    // Fewer than two points still builds a valid ramp.
    assertEquals(0.5, new LoggedTunableMap(name(), 1, false).applyThrottle(0.5), 1e-9);
  }

  @Test
  void splineAndPolynomialModesGoThroughTheirPoints() {
    LoggedTunableMap spline =
        new LoggedTunableMap(name(), true, new Point(0, 0), new Point(1, 1), new Point(2, 4));
    assertEquals(1.0, spline.applyThrottle(1.0), 1e-9);

    LoggedTunableMap quadratic =
        new LoggedTunableMap(name(), new double[] {0, 1, 2, 3}, new double[] {0, 1, 4, 9}, 2);
    assertEquals(6.25, quadratic.applyThrottle(2.5), 1e-6);

    LoggedTunableMap fromPoints =
        new LoggedTunableMap(name(), 2, new Point(0, 0), new Point(1, 1), new Point(2, 4));
    assertEquals(2.25, fromPoints.applyThrottle(1.5), 1e-6);
    assertEquals(0.5, new LoggedTunableMap(name(), 3, 1).applyThrottle(0.5), 1e-6);
  }

  @Test
  void calculateWithoutTuningKeepsTheMap() {
    LoggedTunableMap map = new LoggedTunableMap(name(), false, new Point(0, 0), new Point(1, 10));
    map.calculate();
    assertEquals(5.0, map.applyThrottle(0.5), 1e-9);
  }

  @Test
  void mismatchedArraysAreRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new LoggedTunableMap(name(), new double[] {0, 1}, new double[] {0}, false));
  }
}
