package org.ciprix.interfaces.fsm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj.simulation.SimHooks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests of the FSM core: 100% pure logic, runs without a robot. Protects the guarantees the
 * template promises: evaluation order, one jump per cycle, request-transition exclusions and time
 * units in seconds.
 */
class StateMachineTest {

  private enum S {
    A,
    B,
    C
  }

  @BeforeAll
  static void initHAL() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setup() {
    // Simulated HAL: needed because the FSM reads the FPGA timestamp (Logger.getTimestamp).
    SimHooks.pauseTiming(); // deterministic time for the transitionAfter tests
    SimHooks.restartTiming();
  }

  @AfterEach
  void teardown() {
    SimHooks.resumeTiming();
  }

  @Test
  void localTransitionFiresWhenConditionTrue() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    boolean[] go = {false};
    fsm.state(S.A).transitionTo(S.B, () -> go[0]);
    fsm.state(S.B);
    fsm.state(S.C);

    fsm.update();
    assertEquals(S.A, fsm.getCurrentState());

    go[0] = true;
    fsm.update();
    assertEquals(S.B, fsm.getCurrentState());
  }

  @Test
  void onEnterAndOnExitRunExactlyOncePerTransition() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    int[] enterB = {0};
    int[] exitA = {0};
    fsm.state(S.A).onExit(() -> exitA[0]++).transitionTo(S.B, () -> true);
    fsm.state(S.B).onEnter(() -> enterB[0]++);
    fsm.state(S.C);

    fsm.update(); // A -> B
    fsm.update(); // stays in B
    fsm.update();

    assertEquals(1, exitA[0]);
    assertEquals(1, enterB[0]);
  }

  @Test
  void globalTransitionPreemptsLocal() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    fsm.state(S.A).transitionTo(S.B, () -> true); // local wants B
    fsm.state(S.B);
    fsm.state(S.C);
    fsm.addGlobalTransition(S.C, () -> true); // global wants C

    fsm.update();
    assertEquals(S.C, fsm.getCurrentState(), "global must take priority over local");
  }

  @Test
  void atMostOneTransitionPerUpdate() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    fsm.state(S.A).transitionTo(S.B, () -> true);
    fsm.state(S.B).transitionTo(S.C, () -> true);
    fsm.state(S.C);

    fsm.update();
    assertEquals(S.B, fsm.getCurrentState(), "chains cost one cycle per jump");
    fsm.update();
    assertEquals(S.C, fsm.getCurrentState());
  }

  @Test
  void requestTransitionSkipsEntryAndSatisfyingStates() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    int[] enterB = {0};
    boolean[] request = {true};
    fsm.state(S.A);
    fsm.state(S.B).onEnter(() -> enterB[0]++).transitionTo(S.C, () -> true);
    fsm.state(S.C);
    // the request enters through B; C already satisfies the request
    fsm.addRequestTransition(() -> request[0], S.B, S.C);

    fsm.update(); // A -> B (entry)
    assertEquals(S.B, fsm.getCurrentState());
    fsm.update(); // B -> C via local; the global must NOT pull back to B
    assertEquals(S.C, fsm.getCurrentState());
    fsm.update();
    fsm.update();
    assertEquals(
        S.C, fsm.getCurrentState(), "the state that satisfies the request is not interrupted");
    assertEquals(1, enterB[0], "no re-entry into the entry state");
  }

  @Test
  void transitionAfterUsesSeconds() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    fsm.state(S.A).transitionAfter(0.5, S.B);
    fsm.state(S.B);
    fsm.state(S.C);

    fsm.update();
    assertEquals(S.A, fsm.getCurrentState(), "must not fire immediately (us vs s bug)");

    SimHooks.stepTiming(0.6);
    fsm.update();
    assertEquals(S.B, fsm.getCurrentState());
  }

  @Test
  void timeInStateResetsOnTransition() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    fsm.state(S.A).transitionTo(S.B, () -> true);
    fsm.state(S.B);
    fsm.state(S.C);

    SimHooks.stepTiming(2.0);
    assertTrue(fsm.getTimeInState() >= 1.95, "elapsed time must add up to about 2.0 s");
    fsm.update(); // A -> B
    assertTrue(fsm.getTimeInState() < 0.5, "time in state must reset after a transition");
  }

  @Test
  void validateCompleteDetectsMissingStates() {
    StateMachine<S> incomplete = new StateMachine<>("t1", S.class, S.A);
    incomplete.state(S.A);
    incomplete.state(S.B);
    assertFalse(incomplete.validateComplete(), "S.C was not configured");

    StateMachine<S> complete = new StateMachine<>("t2", S.class, S.A);
    complete.state(S.A);
    complete.state(S.B);
    complete.state(S.C);
    assertTrue(complete.validateComplete());
  }

  @Test
  void forceStateRunsLifecycleCallbacks() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    int[] exitA = {0};
    int[] enterC = {0};
    fsm.state(S.A).onExit(() -> exitA[0]++);
    fsm.state(S.B);
    fsm.state(S.C).onEnter(() -> enterC[0]++);

    fsm.forceState(S.C);
    assertEquals(S.C, fsm.getCurrentState());
    assertEquals(1, exitA[0]);
    assertEquals(1, enterC[0]);
  }

  @Test
  void initialStateOnEnterIsNotCalledAtInitialization() {
    StateMachine<S> fsm = new StateMachine<>("t", S.class, S.A);
    int[] enterA = {0};

    fsm.state(S.A).onEnter(() -> enterA[0]++);
    fsm.state(S.B);
    fsm.state(S.C);

    fsm.update();
    assertEquals(0, enterA[0], "the initial state's onEnter does not run automatically");

    fsm.forceState(S.B);
    fsm.forceState(S.A);
    assertEquals(1, enterA[0], "onEnter runs when A is re-entered through a transition");
  }
}
