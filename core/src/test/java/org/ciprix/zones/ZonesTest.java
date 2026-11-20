package org.ciprix.zones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.math.geometry.Translation2d;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** Circles, rectangles and named zones answer "am I inside?" and "how far am I?". */
class ZonesTest {

  private enum Field {
    HUB,
    TRENCH
  }

  @Test
  void circleContainsItsInteriorAndBoundary() {
    Circle2d circle = new Circle2d(new Translation2d(1, 1), 2.0);
    assertTrue(circle.contains(new Translation2d(1, 1)));
    assertTrue(circle.contains(new Translation2d(3, 1))); // on the edge
    assertFalse(circle.contains(new Translation2d(3.01, 1)));
  }

  @Test
  void circleDistanceIsZeroInsideAndToTheEdgeOutside() {
    Circle2d circle = new Circle2d(new Translation2d(0, 0), 1.0);
    assertEquals(0.0, circle.distanceTo(new Translation2d(0.5, 0)), 1e-9);
    assertEquals(2.0, circle.distanceTo(new Translation2d(0, 3)), 1e-9);
    assertEquals(1.0, circle.getRadius(), 1e-9);
  }

  @Test
  void circleCenterCanMove() {
    AtomicReference<Translation2d> center = new AtomicReference<>(new Translation2d(0, 0));
    Circle2d circle = new Circle2d(center::get, 1.0);
    assertTrue(circle.contains(new Translation2d(0, 0)));
    center.set(new Translation2d(10, 10));
    assertFalse(circle.contains(new Translation2d(0, 0)));
    assertEquals(new Translation2d(10, 10), circle.getCenter());
  }

  @Test
  void rectangleAcceptsCornersInAnyOrder() {
    Rectangle2d rect = new Rectangle2d(new Translation2d(4, 3), new Translation2d(0, 1));
    assertEquals(0.0, rect.getMinX(), 1e-9);
    assertEquals(4.0, rect.getMaxX(), 1e-9);
    assertEquals(1.0, rect.getMinY(), 1e-9);
    assertEquals(3.0, rect.getMaxY(), 1e-9);
    assertEquals(new Translation2d(2, 2), rect.getCenter());
    assertEquals(new Translation2d(0, 1), rect.getBottomLeft());
    assertEquals(new Translation2d(4, 3), rect.getTopRight());
    assertEquals(new Translation2d(4, 1), rect.getBottomRight());
    assertEquals(new Translation2d(0, 3), rect.getTopLeft());
    assertEquals(rect.getBottomLeft(), rect.getCornerA());
    assertEquals(rect.getTopRight(), rect.getCornerB());
  }

  @Test
  void rectangleContainsInteriorAndEdges() {
    Rectangle2d rect = new Rectangle2d(new Translation2d(0, 0), new Translation2d(2, 1));
    assertTrue(rect.contains(new Translation2d(1, 0.5)));
    assertTrue(rect.contains(new Translation2d(2, 1)));
    assertFalse(rect.contains(new Translation2d(2.1, 0.5)));
    assertFalse(rect.contains(new Translation2d(1, -0.1)));
  }

  @Test
  void rectangleDistanceToSideAndToCorner() {
    Rectangle2d rect = new Rectangle2d(new Translation2d(0, 0), new Translation2d(2, 1));
    assertEquals(0.0, rect.distanceTo(new Translation2d(1, 0.5)), 1e-9);
    assertEquals(3.0, rect.distanceTo(new Translation2d(5, 0.5)), 1e-9);
    assertEquals(5.0, rect.distanceTo(new Translation2d(5, 5)), 1e-9); // 3-4-5 from (2,1)
  }

  @Test
  void namedZoneDelegatesToItsShape() {
    Circle2d shape = new Circle2d(new Translation2d(0, 0), 1.0);
    Zone2d<Field> hub = new Zone2d<>(Field.HUB, shape);
    assertEquals(Field.HUB, hub.getId());
    assertSame(shape, hub.getZone());
    assertTrue(hub.contains(new Translation2d(0.2, 0.2)));
    assertEquals(1.0, hub.distanceTo(new Translation2d(2, 0)), 1e-9);
  }
}
