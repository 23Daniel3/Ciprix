package org.ciprix.interfaces.subsystem;

import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import org.ciprix.interfaces.fsm.StateMachine;
import org.ciprix.util.ConstantsLogger;
import org.ciprix.util.PeriodicTimer;
import org.ciprix.util.Report;
import org.littletonrobotics.junction.Logger;
import org.littletonrobotics.junction.inputs.LoggableInputs;

/**
 * Subsystem built on the Request → State pattern: the outside world expresses intent through a
 * Request enum ({@link #setRequest}); the internal FSM decides how to get there; {@link #atGoal()}
 * says whether the current request has been served.
 *
 * <p>Use {@link #bindRequest} to declare, in one call: (a) which state the FSM enters to serve a
 * request; (b) which final state satisfies it; (c) which intermediate states must not be
 * interrupted by the global transition. With that, {@code atGoal()} is derived automatically, so
 * there is no need to override it with a manual switch.
 *
 * <p><b>Co-driver backup:</b> {@link #override} replaces the request for a single cycle. While the
 * co-driver holds the button, the subsystem follows the override; the SuperStructure's request is
 * kept and takes over again on its own the cycle after the button is released. {@code atGoal()}
 * stays relative to the SuperStructure's request, so the overall FSM never advances because of an
 * override.
 */
public abstract class StateSubsystem<
        R extends Enum<R>, S extends Enum<S>, I extends LoggableInputs, T extends SubsystemIO<I>>
    extends SubsystemBase {

  protected final StateMachine<S> fsm;
  protected final I inputs;
  protected final T io;

  /** Request from whoever normally commands the subsystem (the SuperStructure, via the relay). */
  protected R currentRequest;

  /**
   * Transient co-driver override: lasts one cycle and is cleared at the end of {@code periodic()}.
   */
  private R transientOverrideRequest = null;

  private R lastOverrideRequest = null;
  private boolean overrideActive = false;

  /** Map from request to the state that satisfies it, filled by {@link #bindRequest}. */
  private final Map<R, S> requestGoals;

  // Name and log keys computed once: the loop doesn't concatenate strings.
  private final String subsystemName;
  private final String requestKey;
  private final String activeRequestKey;
  private final String overrideKey;
  private final String atGoalKey;
  private final String faultKey;
  private final String statusKey;

  private final Alert faultAlert;
  private boolean faultAlertActive = false;
  private boolean nullRequestReported = false;

  // Pit self-test: declared with selfTest(), run by selfTestCommand().
  private SelfTest selfTest = null;
  private boolean selfTestRunning = false;

  // Text status: only rebuilt when something changes.
  private String status = "";
  private String statusDetail = "";
  private R statusRequest;
  private S statusState;
  private boolean statusAtGoal;
  private boolean statusOverride;
  private boolean statusFault;
  private boolean statusDirty = true;

  public StateSubsystem(
      String name, I inputs, T io, Class<S> stateEnum, S initialState, R initialRequest) {
    this(name, inputs, io, stateEnum, initialState, initialRequest, null);
  }

  public StateSubsystem(
      String name,
      I inputs,
      T io,
      Class<S> stateEnum,
      S initialState,
      R initialRequest,
      Class<?> constantsClass) {
    Objects.requireNonNull(initialRequest, "initialRequest");
    setName(name);
    this.subsystemName = name;
    this.inputs = inputs;
    this.io = io;
    this.currentRequest = initialRequest;
    this.requestGoals = new EnumMap<>(initialRequest.getDeclaringClass());
    this.fsm = new StateMachine<>(name, stateEnum, initialState);

    this.requestKey = name + "/request";
    this.activeRequestKey = name + "/activeRequest";
    this.overrideKey = name + "/overrideActive";
    this.atGoalKey = name + "/atGoal";
    this.faultKey = name + "/fault";
    this.statusKey = name + "/status";
    this.faultAlert = new Alert(name + ": state machine fault", AlertType.kWarning);

    if (constantsClass != null) {
      ConstantsLogger.logConstants(constantsClass, name);
    }
  }

  /**
   * Declares how a request is served by the FSM.
   *
   * <p>Registers a global transition to {@code entryState} whenever {@code request} is active and
   * the FSM is not in {@code entryState}, {@code goalState} or any of the {@code
   * intermediateStates}. Also registers {@code goalState} as the state that makes {@link #atGoal()}
   * return true for that request.
   *
   * <p>Example (Shooter): {@code bindRequest(SHOOT, FLYWHEEL_RAMPING, SHOOTING, KICKER_RAMPING)}:
   * the SHOOT request enters through FLYWHEEL_RAMPING, is satisfied in SHOOTING, and KICKER_RAMPING
   * must not be interrupted.
   *
   * @param request the request to map
   * @param entryState entry state to serve the request
   * @param goalState state that satisfies the request ({@code atGoal() == true})
   * @param intermediateStates intermediate states already "on the way" to the goal
   */
  @SafeVarargs
  protected final void bindRequest(R request, S entryState, S goalState, S... intermediateStates) {
    requestGoals.put(request, goalState);

    // goalState + intermediates; the entryState itself is excluded by the FSM automatically.
    final S[] satisfied = Arrays.copyOf(intermediateStates, intermediateStates.length + 1);
    satisfied[intermediateStates.length] = goalState;

    fsm.addRequestTransition(request.name(), () -> isRequest(request), entryState, satisfied);
  }

  /** Sets the subsystem's request. This is what the SuperStructure relay calls every cycle. */
  public void setRequest(R request) {
    if (request == null) {
      reportNullRequest("setRequest");
      return;
    }
    this.currentRequest = request;
  }

  /**
   * Co-driver backup: replaces the request for <b>one cycle</b>. Call it every cycle while the
   * button is held (the commands generated by {@code @AutoCommandFactory} already do this). Once
   * you stop calling it, the subsystem goes back to the SuperStructure's request on its own.
   */
  public void override(R request) {
    if (request == null) {
      reportNullRequest("override");
      return;
    }
    this.transientOverrideRequest = request;
  }

  /** Request from whoever normally commands the subsystem (ignoring the co-driver override). */
  public R getRequest() {
    return currentRequest;
  }

  /** Request the FSM is actually following: the co-driver override, if any, or the normal one. */
  public R getActiveRequest() {
    if (transientOverrideRequest != null) return transientOverrideRequest;
    return overrideActive ? lastOverrideRequest : currentRequest;
  }

  /** True while the co-driver is overriding this subsystem's request. */
  public boolean isOverrideActive() {
    return overrideActive || transientOverrideRequest != null;
  }

  public S getState() {
    return fsm.getCurrentState();
  }

  public I getInputs() {
    return inputs;
  }

  /** The subsystem's FSM, for telemetry, tests and visualizers. */
  public StateMachine<S> getStateMachine() {
    return fsm;
  }

  /** True when the request the FSM is following (including the override) is {@code request}. */
  protected boolean isRequest(R request) {
    return (transientOverrideRequest != null ? transientOverrideRequest : currentRequest)
        == request;
  }

  protected boolean notInState(S state) {
    return getState() != state;
  }

  /**
   * Trigger active while the subsystem is in one of the states. Example: {@code
   * shooter.inState(ShooterState.SHOOTING).onTrue(LedCommands.blink(led))}.
   */
  @SafeVarargs
  public final Trigger inState(S state, S... others) {
    return new Trigger(fsm.inState(state, others));
  }

  /** Trigger active while the SuperStructure's request is served. */
  public Trigger atGoalTrigger() {
    return new Trigger(this::atGoal);
  }

  /**
   * True when the FSM is on a failure path: a state exceeded its {@code timeoutAfter} or a safety
   * condition fired, and the request that failed is still blocked.
   */
  public boolean hasFault() {
    return fsm.hasFault();
  }

  /** Releases the request blocked by a failure again, without waiting for it to be released. */
  public void clearFault() {
    fsm.clearFault();
  }

  /**
   * Short text describing what the subsystem is doing, for example {@code "SHOOT |
   * KICKER_RAMPING"}. Published at {@code <name>/status}.
   */
  public String getStatus() {
    return status;
  }

  /** Appends a subsystem-specific detail to the status (e.g. "3120 rpm"). Empty removes it. */
  protected void setStatusDetail(String detail) {
    String value = detail == null ? "" : detail;
    if (!value.equals(statusDetail)) {
      statusDetail = value;
      statusDirty = true;
    }
  }

  @Override
  public final void periodic() {
    PeriodicTimer.start(subsystemName);

    io.updateInputs(inputs);
    Logger.processInputs(subsystemName, inputs);

    // An override requested after the last cycle applies for this entire periodic().
    overrideActive = transientOverrideRequest != null;
    lastOverrideRequest = transientOverrideRequest;

    // During the self-test the FSM is paused: nothing fights the test for the motors.
    if (!selfTestRunning) {
      fsm.update();
    }

    sPeriodic();

    final boolean atGoal = atGoal();
    final boolean fault = fsm.hasFault();
    final R activeRequest = overrideActive ? lastOverrideRequest : currentRequest;

    Logger.recordOutput(requestKey, currentRequest.toString());
    Logger.recordOutput(activeRequestKey, activeRequest.toString());
    Logger.recordOutput(overrideKey, overrideActive);
    Logger.recordOutput(atGoalKey, atGoal);
    Logger.recordOutput(faultKey, fault);

    updateFaultAlert(fault);
    updateStatus(activeRequest, atGoal, fault);

    // End of cycle: the override is transient. If the co-driver keeps holding the button, the
    // command calls override() again before the next periodic().
    transientOverrideRequest = null;

    PeriodicTimer.stop(subsystemName);
  }

  protected void sPeriodic() {}

  /**
   * Declares the motors checked by the pit self-test ({@link #selfTestCommand()}). Call it in the
   * constructor, alongside the FSM:
   *
   * <pre>{@code
   * selfTest()
   *     .motor("Roller", io::controlRollerMotor, () -> inputs.rollerMotorInputs)
   *     .output(0.3)
   *     .currentBetween(Amps.of(0.5), Amps.of(30))
   *     .mustTurn(RotationsPerSecond.of(5));
   * }</pre>
   */
  protected final SelfTest selfTest() {
    if (selfTest == null) {
      selfTest = new SelfTest(subsystemName);
    }
    return selfTest;
  }

  /**
   * Pit self-test: spins each motor declared in {@link #selfTest()} briefly and checks that it
   * responds, that the current makes sense and that the mechanism turned. While it runs, the FSM is
   * paused; when it ends, the FSM restarts from its initial state, as after boot. Results go to
   * {@code SelfTest/<subsystem>/...} and a failure raises a dashboard alert.
   *
   * <p>A subsystem with no declared motors returns a command that does nothing.
   */
  public Command selfTestCommand() {
    if (selfTest == null || selfTest.isEmpty()) {
      return Commands.none().withName("SelfTest " + getName() + " (nada declarado)");
    }
    return selfTest.command(
        this,
        running -> {
          selfTestRunning = running;
          if (!running) {
            fsm.restart();
          }
        });
  }

  /** The declared self-test, with the results of the last run. */
  public SelfTest getSelfTest() {
    return selfTest();
  }

  /** True while the self-test is running (the FSM is paused). */
  public boolean isSelfTestRunning() {
    return selfTestRunning;
  }

  /**
   * Returns true if the current request has been served. The default implementation uses the map
   * built by {@link #bindRequest}: the request counts as served when the FSM is in the {@code
   * goalState} bound to it. Unbound requests return true (no intent = satisfied). Override only if
   * the subsystem needs a notion of goal other than "being in a state".
   *
   * <p>The goal is always the normal request's ({@link #getRequest()}), never the co-driver
   * override's.
   */
  public boolean atGoal() {
    S goal = requestGoals.get(currentRequest);
    return goal == null || getState() == goal;
  }

  private void updateFaultAlert(boolean fault) {
    if (fault == faultAlertActive) return;
    faultAlertActive = fault;
    if (fault) {
      faultAlert.setText(subsystemName + ": " + fsm.getLastFault());
    }
    faultAlert.set(fault);
  }

  private void updateStatus(R activeRequest, boolean atGoal, boolean fault) {
    final S state = getState();
    if (!statusDirty
        && activeRequest == statusRequest
        && state == statusState
        && atGoal == statusAtGoal
        && overrideActive == statusOverride
        && fault == statusFault) {
      return;
    }
    statusRequest = activeRequest;
    statusState = state;
    statusAtGoal = atGoal;
    statusOverride = overrideActive;
    statusFault = fault;
    statusDirty = false;

    StringBuilder sb = new StringBuilder(48);
    sb.append(activeRequest).append(" | ").append(state);
    if (overrideActive) sb.append(" | OVERRIDE");
    if (fault) sb.append(" | FALHA: ").append(fsm.getLastFault());
    if (!statusDetail.isEmpty()) sb.append(" | ").append(statusDetail);
    status = sb.toString();
    Logger.recordOutput(statusKey, status);
  }

  private void reportNullRequest(String method) {
    if (nullRequestReported) return;
    nullRequestReported = true;
    Report.error(subsystemName + ": " + method + "(null) ignored. A request can never be null.");
  }
}
