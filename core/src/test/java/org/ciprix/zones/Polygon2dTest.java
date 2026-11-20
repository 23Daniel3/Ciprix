package org.ciprix.zones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

/** Field zones (trench, alliance zone) depend on these geometric predicates. */
class Polygon2dTest {

  // Unit square (0,0)-(1,1)
  private final Polygon2d square =
      new Polygon2d(
          new Translation2d(0, 0),
          new Translation2d(1, 0),
          new Translation2d(1, 1),
          new Translation2d(0, 1));

  @Test
  void requiresAtLeastThreeVertices() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Polygon2d(new Translation2d(0, 0), new Translation2d(1, 1)));
  }

  @Test
  void containsInteriorAndExcludesExterior() {
    assertTrue(square.contains(new Translation2d(0.5, 0.5)));
    assertTrue(square.contains(new Translation2d(0.01, 0.99)));
    assertFalse(square.contains(new Translation2d(1.5, 0.5)));
    assertFalse(square.contains(new Translation2d(-0.1, 0.5)));
    assertFalse(square.contains(new Translation2d(0.5, 2.0)));
  }

  @Test
  void centerOfSquareIsMiddle() {
    assertEquals(0.5, square.getCenter().getX(), 1e-9);
    assertEquals(0.5, square.getCenter().getY(), 1e-9);
  }

  @Test
  void distanceIsZeroInsideAndEuclideanOutside() {
    assertEquals(0.0, square.distanceTo(new Translation2d(0.5, 0.5)), 1e-9);
    // point 1.0 to the right of the edge x=1
    assertEquals(1.0, square.distanceTo(new Translation2d(2.0, 0.5)), 1e-9);
    // diagonal from the vertex (1,1): sqrt(2)
    assertEquals(Math.sqrt(2.0), square.distanceTo(new Translation2d(2.0, 2.0)), 1e-9);
  }

  @Test
  void worksForConcavePolygons() {
    // "L": the upper-right cutout is OUTSIDE
    Polygon2d ell =
        new Polygon2d(
            new Translation2d(0, 0),
            new Translation2d(2, 0),
            new Translation2d(2, 1),
            new Translation2d(1, 1),
            new Translation2d(1, 2),
            new Translation2d(0, 2));
    assertTrue(ell.contains(new Translation2d(0.5, 1.5)));
    assertTrue(ell.contains(new Translation2d(1.5, 0.5)));
    assertFalse(ell.contains(new Translation2d(1.5, 1.5)), "the L cutout is outside");
  }
}
