package org.ciprix.interfaces.subsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.littletonrobotics.junction.LogTable;
import org.littletonrobotics.junction.inputs.LoggableInputs;

/**
 * Tests of the StateSubsystem Request → State pattern: checks that bindRequest registers the global
 * transitions with the right exclusions and that the derived atGoal() matches the declared map.
 * Uses a fake subsystem without hardware (empty IO + a "sensor" simulated by a flag).
 */
class StateSubsystemTest {

  private enum Req {
    RUN,
    STOP
  }

  private enum St {
    RUNNING,
    STOPPING,
    STOPPED
  }

  /** Minimal inputs: an empty LoggableInputs (nothing to log in a fake). */
  private static class FakeInputs implements LoggableInputs {
    @Override
    public void toLog(LogTable table) {}

    @Override
    public void fromLog(LogTable table) {}
  }

  private static class FakeIO implements SubsystemIO<FakeInputs> {
    @Override
    public void updateInputs(FakeInputs inputs) {}
  }

  private static class FakeSubsystem extends StateSubsystem<Req, St, FakeInputs, FakeIO> {
    boolean spinStopped = false; // simulated sensor
    int stoppingEnters = 0;
    int runningEnters = 0;

    FakeSubsystem(String name) {
      super(name, new FakeInputs(), new FakeIO(), St.class, St.STOPPED, Req.STOP);

      fsm.state(St.RUNNING).onEnter(() -> runningEnters++);
      fsm.state(St.STOPPING)
          .onEnter(() -> stoppingEnters++)
          .transitionTo(St.STOPPED, () -> spinStopped);
      fsm.state(St.STOPPED);

      bindRequest(Req.RUN, St.RUNNING, St.RUNNING);
      bindRequest(Req.STOP, St.STOPPING, St.STOPPED);

      assertTrue(fsm.validateComplete());
    }

    /** Advances one FSM cycle without going through the scheduler/Logger. */
    void tick() {
      fsm.update();
    }
  }

  @BeforeAll
  static void initHAL() {
    HAL.initialize(500, 0);
  }

  @Test
  void initialRequestIsSatisfiedAtBoot() {
    FakeSubsystem s = new FakeSubsystem("Test/Boot");
    assertEquals(St.STOPPED, s.getState());
    assertTrue(s.atGoal(), "STOP with state STOPPED must start satisfied");
  }

  @Test
  void requestDrivesFsmAndAtGoalFollows() {
    FakeSubsystem s = new FakeSubsystem("Test/Flow");

    s.setRequest(Req.RUN);
    assertFalse(s.atGoal(), "request changed, state not yet");
    s.tick();
    assertEquals(St.RUNNING, s.getState());
    assertTrue(s.atGoal());

    s.setRequest(Req.STOP);
    s.tick();
    assertEquals(St.STOPPING, s.getState());
    assertFalse(s.atGoal(), "STOPPING is intermediate: the goal is STOPPED");

    s.spinStopped = true;
    s.tick();
    assertEquals(St.STOPPED, s.getState());
    assertTrue(s.atGoal());
  }

  @Test
  void intermediateStateIsNotReentered() {
    FakeSubsystem s = new FakeSubsystem("Test/NoReentry");

    s.setRequest(Req.RUN);
    s.tick(); // STOPPED -> RUNNING
    s.setRequest(Req.STOP);
    // spinStopped stays false: the mechanism "keeps spinning" for several cycles
    s.tick(); // RUNNING -> STOPPING
    s.tick();
    s.tick();

    assertEquals(St.STOPPING, s.getState());
    assertEquals(1, s.stoppingEnters, "the global transition must not re-run onEnter");
  }

  @Test
  void switchingRequestMidTravelRedirectsFsm() {
    FakeSubsystem s = new FakeSubsystem("Test/Switch");

    s.setRequest(Req.RUN);
    s.tick(); // STOPPED -> RUNNING
    s.setRequest(Req.STOP);
    s.tick(); // RUNNING -> STOPPING (spinStopped=false holds it here)
    assertEquals(St.STOPPING, s.getState());

    s.setRequest(Req.RUN); // the driver changes their mind halfway
    s.tick();
    assertEquals(St.RUNNING, s.getState());
    assertTrue(s.atGoal());
    assertEquals(2, s.runningEnters);
  }

  // ---------------------------------------------------------------------------------------------
  // Transient override (co-driver backup)
  // ---------------------------------------------------------------------------------------------

  @Test
  void overrideWinsForOneCycleAndTheRequestComesBackByItself() {
    FakeSubsystem s = new FakeSubsystem("Test/Override");
    s.periodic();
    assertEquals(St.STOPPED, s.getState());

    s.override(Req.RUN); // co-driver pressed the button
    assertTrue(s.isOverrideActive());
    assertEquals(Req.RUN, s.getActiveRequest());
    assertEquals(Req.STOP, s.getRequest(), "the SuperStructure request is still stored");

    s.periodic();
    assertEquals(St.RUNNING, s.getState());
    assertTrue(s.isOverrideActive());
    assertEquals(Req.RUN, s.getActiveRequest());

    s.override(Req.RUN); // still holding
    s.periodic();
    assertEquals(St.RUNNING, s.getState());
    assertEquals(1, s.runningEnters, "holding the override does not re-run onEnter");

    s.periodic(); // released: no override() this cycle
    assertFalse(s.isOverrideActive());
    assertEquals(Req.STOP, s.getActiveRequest());
    assertEquals(St.STOPPING, s.getState(), "goes back to the SuperStructure request on its own");
  }

  @Test
  void atGoalStaysRelativeToTheSuperStructureRequestDuringOverride() {
    FakeSubsystem s = new FakeSubsystem("Test/OverrideGoal");
    s.periodic();
    assertTrue(s.atGoal());

    s.override(Req.RUN);
    s.periodic();
    assertEquals(St.RUNNING, s.getState());
    assertFalse(
        s.atGoal(), "RUNNING serves the override, not the STOP requested by the SuperStructure");
  }

  @Test
  void relayKeepsUpdatingTheRequestWhileTheOverrideIsHeld() {
    FakeSubsystem s = new FakeSubsystem("Test/OverrideRelay");
    s.spinStopped = true;

    // The relay keeps running during the override: the request changes underneath without
    // affecting the FSM.
    s.setRequest(Req.RUN);
    s.override(Req.STOP);
    s.periodic();
    assertEquals(St.STOPPED, s.getState(), "STOPPED already serves the STOP override");
    assertEquals(Req.RUN, s.getRequest());

    s.periodic(); // button released: the RUN the relay left stored takes effect
    assertEquals(St.RUNNING, s.getState());
    assertTrue(s.atGoal());
  }

  @Test
  void nullRequestsAreIgnoredAndReportedOnce() {
    java.util.List<String> reports = new java.util.ArrayList<>();
    org.ciprix.util.Report.setSink((isError, message, printTrace) -> reports.add(message));
    try {
      FakeSubsystem s = new FakeSubsystem("Test/Null");
      s.setRequest(Req.RUN);
      s.setRequest(null);
      s.override(null);
      s.setRequest(null);

      assertEquals(Req.RUN, s.getRequest());
      assertFalse(s.isOverrideActive());
      assertEquals(1, reports.size());
      s.periodic(); // must not throw
      assertEquals(St.RUNNING, s.getState());
    } finally {
      org.ciprix.util.Report.resetSink();
    }
  }

  @Test
  void inStateTriggerAndStatusFollowTheSubsystem() {
    FakeSubsystem s = new FakeSubsystem("Test/Trigger");
    var running = s.inState(St.RUNNING);
    var stopped = s.inState(St.STOPPED, St.STOPPING);

    s.periodic();
    assertFalse(running.getAsBoolean());
    assertTrue(stopped.getAsBoolean());
    assertEquals("STOP | STOPPED", s.getStatus());

    s.override(Req.RUN);
    s.periodic();
    assertTrue(running.getAsBoolean());
    assertFalse(stopped.getAsBoolean());
    assertEquals("RUN | RUNNING | OVERRIDE", s.getStatus());
  }
}
