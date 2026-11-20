package org.ciprix.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.simulation.DriverStationSim;
import edu.wpi.first.wpilibj.simulation.JoystickSim;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArcadeControllerTest {

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @Test
  void buttonsAreNumberedFromOneLikeTheDriverStation() {
    ArcadeController arcade = new ArcadeController(3);
    JoystickSim sim = new JoystickSim(arcade.getJoystick());
    sim.setButtonCount(16);
    sim.setButton(5, true);
    sim.notifyNewData();
    DriverStationSim.notifyNewData();
    DriverStation.refreshData();

    assertTrue(arcade.button5().getAsBoolean());
    assertFalse(arcade.button4().getAsBoolean());
    assertSame(arcade.button(5), arcade.button5());
    assertEquals(3, arcade.getJoystick().getPort());
  }

  @Test
  void everyShortcutMatchesItsIndex() {
    ArcadeController arcade = new ArcadeController(4);
    Trigger[] shortcuts = {
      arcade.button1(), arcade.button2(), arcade.button3(), arcade.button4(),
      arcade.button5(), arcade.button6(), arcade.button7(), arcade.button8(),
      arcade.button9(), arcade.button10(), arcade.button11(), arcade.button12(),
      arcade.button13(), arcade.button14(), arcade.button15(), arcade.button16()
    };
    for (int i = 0; i < shortcuts.length; i++) {
      assertSame(arcade.button(i + 1), shortcuts[i]);
    }
  }

  @Test
  void indexOutsideOneToSixteenIsRejected() {
    ArcadeController arcade = new ArcadeController(5);
    assertThrows(IllegalArgumentException.class, () -> arcade.button(0));
    assertThrows(IllegalArgumentException.class, () -> arcade.button(17));
  }
}
