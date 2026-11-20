package org.ciprix.calculus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.ciprix.calculus.LinearInterpolation.Point;
import org.junit.jupiter.api.Test;

/**
 * Throttle curves turn a stick or distance into an output with linear, spline or polynomial fits.
 */
class ThrottleMapTest {

  @Test
  void twoPointLinearMapInterpolatesAndClamps() {
    ThrottleMap map = new ThrottleMap(0.0, 1.0, 0.0, 10.0);
    assertEquals(ThrottleMap.FitMode.LINEAR, map.getMode());
    assertEquals(5.0, map.applyThrottle(0.5), 1e-9);
    assertEquals(0.0, map.applyThrottle(-3.0), 1e-9);
    assertEquals(10.0, map.applyThrottle(3.0), 1e-9);
  }

  @Test
  void threePointLinearMapUsesTheRightSegment() {
    ThrottleMap map = new ThrottleMap(0.0, 0.5, 1.0, 0.0, 0.2, 1.0);
    assertEquals(0.1, map.applyThrottle(0.25), 1e-9); // first, gentle segment
    assertEquals(0.6, map.applyThrottle(0.75), 1e-9); // second, steep segment
  }

  @Test
  void arrayAndPointConstructorsAgree() {
    ThrottleMap fromArrays = new ThrottleMap(new double[] {0, 2, 4}, new double[] {0, 1, 9});
    ThrottleMap fromPoints = new ThrottleMap(new Point(0, 0), new Point(2, 1), new Point(4, 9));
    for (double x = 0; x <= 4; x += 0.5) {
      assertEquals(fromArrays.applyThrottle(x), fromPoints.applyThrottle(x), 1e-9);
    }
  }

  @Test
  void needsAtLeastTwoPoints() {
    assertThrows(IllegalArgumentException.class, () -> new ThrottleMap(new Point(0, 0)));
  }

  @Test
  void absKeepsTheSignOfTheInput() {
    ThrottleMap map = new ThrottleMap(0.0, 1.0, 0.0, 0.5);
    assertEquals(0.25, map.applyThrottleAbs(0.5), 1e-9);
    assertEquals(-0.25, map.applyThrottleAbs(-0.5), 1e-9);
  }

  @Test
  void cubicSplinePassesThroughEveryPointAndIsSmooth() {
    ThrottleMap map = new ThrottleMap(true, new Point(2, 4), new Point(0, 0), new Point(1, 1));
    assertEquals(ThrottleMap.FitMode.SPLINE_CUBIC, map.getMode());
    assertEquals(0.0, map.applyThrottle(0.0), 1e-9);
    assertEquals(1.0, map.applyThrottle(1.0), 1e-9);
    assertEquals(4.0, map.applyThrottle(2.0), 1e-9);
    double left = map.applyThrottle(1.0 - 1e-6);
    double right = map.applyThrottle(1.0 + 1e-6);
    assertEquals(left, right, 1e-4, "no jump at the knot");
  }

  @Test
  void cubicSplineFromArrays() {
    ThrottleMap map = new ThrottleMap(new double[] {0, 1, 2}, new double[] {0, 1, 4}, true);
    assertEquals(ThrottleMap.FitMode.SPLINE_CUBIC, map.getMode());
    assertEquals(1.0, map.applyThrottle(1.0), 1e-9);
    ThrottleMap linear = new ThrottleMap(new double[] {0, 1, 2}, new double[] {0, 1, 4}, false);
    assertEquals(ThrottleMap.FitMode.LINEAR, linear.getMode());
    assertEquals(2.5, linear.applyThrottle(1.5), 1e-9);
  }

  @Test
  void polynomialFitRecoversAQuadratic() {
    ThrottleMap map =
        new ThrottleMap(2, new Point(0, 0), new Point(1, 1), new Point(2, 4), new Point(3, 9));
    assertEquals(ThrottleMap.FitMode.POLYNOMIAL_N, map.getMode());
    assertEquals(2, map.getPolynomialDegree());
    assertEquals(2.25, map.applyThrottle(1.5), 1e-6);
    ThrottleMap fromArrays =
        new ThrottleMap(new double[] {0, 1, 2, 3}, new double[] {0, 1, 4, 9}, 2.0);
    assertEquals(6.25, fromArrays.applyThrottle(2.5), 1e-6);
  }

  @Test
  void curveDataReturnsTheSortedPoints() {
    ThrottleMap map = new ThrottleMap(false, new Point(1, 10), new Point(0, 5));
    double[][] data = map.getCurveData();
    assertEquals(0.0, data[0][0], 1e-9);
    assertEquals(5.0, data[1][0], 1e-9);
    assertEquals(1.0, data[0][1], 1e-9);
    assertEquals(10.0, data[1][1], 1e-9);
  }
}
