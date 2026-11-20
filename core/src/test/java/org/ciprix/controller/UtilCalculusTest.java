package org.ciprix.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.wpi.first.math.geometry.Translation2d;
import org.junit.jupiter.api.Test;

/** Joystick shaping: the direction is kept and the magnitude is squared for fine control. */
class UtilCalculusTest {

  @Test
  void centeredStickGivesZero() {
    Translation2d v = UtilCalculus.getLinearVelocityFromJoysticks(0.0, 0.0);
    assertEquals(0.0, v.getNorm(), 1e-9);
  }

  @Test
  void magnitudeIsSquared() {
    Translation2d v = UtilCalculus.getLinearVelocityFromJoysticks(0.5, 0.0);
    assertEquals(0.25, v.getX(), 1e-9);
    assertEquals(0.0, v.getY(), 1e-9);
  }

  @Test
  void directionIsKept() {
    Translation2d v = UtilCalculus.getLinearVelocityFromJoysticks(0.6, 0.8);
    assertEquals(1.0, v.getNorm(), 1e-9);
    assertEquals(0.6, v.getX(), 1e-9);
    assertEquals(0.8, v.getY(), 1e-9);
  }

  @Test
  void fullDeflectionStaysAtFull() {
    Translation2d v = UtilCalculus.getLinearVelocityFromJoysticks(0.0, -1.0);
    assertEquals(0.0, v.getX(), 1e-9);
    assertEquals(-1.0, v.getY(), 1e-9);
  }
}
