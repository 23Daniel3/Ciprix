package org.ciprix.interfaces.subsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.wpi.first.hal.HAL;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import org.ciprix.TestEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The general robot state machine: request in, robot state out, each subsystem told what to do. */
class SuperStructureBaseTest {

  private enum Req {
    IDLE,
    SCORE
  }

  private enum St {
    IDLE,
    AIMING,
    SCORING,
    SAFE
  }

  private final TestEnvironment env = new TestEnvironment();

  @BeforeAll
  static void initHal() {
    HAL.initialize(500, 0);
  }

  @BeforeEach
  void setUp() {
    env.install();
  }

  @AfterEach
  void tearDown() {
    CommandScheduler.getInstance().unregisterAllSubsystems();
    env.uninstall();
  }

  private static final class FakeSuperStructure extends SuperStructureBase<Req, St> {
    boolean aimed = false;
    String shooterRequest = "STOP";
    int beforeCalls = 0;
    int afterCalls = 0;

    FakeSuperStructure() {
      super("Test/SuperStructure", St.class, St.IDLE, Req.IDLE);
      generalFsm.securityState(St.SAFE);
      generalFsm.state(St.IDLE).onEnter(() -> shooterRequest = "STOP");
      generalFsm
          .state(St.AIMING)
          .onEnter(() -> shooterRequest = "SPIN_UP")
          .transitionTo(St.SCORING, () -> aimed)
          .timeoutAfter(0.19);
      generalFsm.state(St.SCORING).onEnter(() -> shooterRequest = "SHOOT");
      generalFsm.state(St.SAFE).onEnter(() -> shooterRequest = "STOP");
      bindRequest(Req.IDLE, St.IDLE);
      bindRequest(Req.SCORE, St.AIMING, St.SCORING);
      assertTrue(generalFsm.validateComplete());
    }

    @Override
    protected void beforeUpdate() {
      beforeCalls++;
    }

    @Override
    protected void afterUpdate() {
      afterCalls++;
    }
  }

  private void cycles(FakeSuperStructure s, int n) {
    for (int i = 0; i < n; i++) {
      env.now += 0.02;
      s.periodic();
    }
  }

  @Test
  void initialRequestIsRequired() {
    assertThrows(
        NullPointerException.class,
        () ->
            new SuperStructureBase<Req, St>("Test/Null", St.class, St.IDLE, null) {
              {
                generalFsm.state(St.IDLE);
              }
            });
  }

  @Test
  void requestDrivesTheRobotStateAndEachStateSetsTheSubsystems() {
    FakeSuperStructure s = new FakeSuperStructure();
    assertEquals(St.IDLE, s.getRobotState());

    s.setRequest(Req.SCORE);
    assertEquals(Req.SCORE, s.getRequest());
    cycles(s, 1);
    assertEquals(St.AIMING, s.getRobotState());
    assertEquals("SPIN_UP", s.shooterRequest);
    assertTrue(s.inState(St.AIMING).getAsBoolean());
    assertTrue(s.whenRequested(Req.SCORE).getAsBoolean());

    s.aimed = true;
    cycles(s, 1);
    assertEquals(St.SCORING, s.getRobotState());
    assertEquals("SHOOT", s.shooterRequest);

    // SCORING already satisfies SCORE: holding the request does not restart the sequence.
    cycles(s, 5);
    assertEquals(St.SCORING, s.getRobotState());

    s.setRequest(Req.IDLE);
    cycles(s, 1);
    assertEquals(St.IDLE, s.getRobotState());
    assertEquals("STOP", s.shooterRequest);
    assertTrue(s.inState(St.IDLE, St.SAFE).getAsBoolean());
  }

  @Test
  void hooksRunAroundEveryUpdate() {
    FakeSuperStructure s = new FakeSuperStructure();
    cycles(s, 3);
    assertEquals(3, s.beforeCalls);
    assertEquals(3, s.afterCalls);
  }

  @Test
  void nullRequestIsIgnoredAndReportedOnce() {
    FakeSuperStructure s = new FakeSuperStructure();
    s.setRequest(Req.SCORE);
    s.setRequest(null);
    s.setRequest(null);
    assertEquals(Req.SCORE, s.getRequest());
    assertEquals(1, env.reports.stream().filter(r -> r.contains("setRequest(null)")).count());
  }

  @Test
  void timeoutFaultsToTheSecurityStateUntilCleared() {
    FakeSuperStructure s = new FakeSuperStructure();
    s.setRequest(Req.SCORE);
    cycles(s, 11); // never aimed: AIMING times out after 0.19 s
    assertEquals(St.SAFE, s.getRobotState());
    assertTrue(s.hasFault());
    assertEquals("STOP", s.shooterRequest);
    assertEquals(s.getStateMachine().getCurrentState(), s.getRobotState());

    s.clearFault();
    assertFalse(s.hasFault());
  }
}
