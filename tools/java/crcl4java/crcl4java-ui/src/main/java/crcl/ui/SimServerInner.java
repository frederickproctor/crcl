/*
 * This software is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain.
 *
 *  This software is experimental. NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic. We would appreciate
 * acknowledgement if the software is used. This software can be
 * redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified
 * versions bear some notice that they have been modified.
 * 
 */
package crcl.ui;

import com.github.wshackle.crcl4java.exi.CrclExiSocket;
import crcl.base.ActuateJointType;
import crcl.base.ActuateJointsType;
import crcl.base.AngleUnitEnumType;
import crcl.base.CRCLCommandInstanceType;
import crcl.base.CRCLCommandType;
import crcl.base.CRCLStatusType;
import crcl.base.CloseToolChangerType;
import crcl.base.CommandStateEnumType;
import crcl.base.CommandStatusType;
import crcl.base.ConfigureJointReportType;
import crcl.base.ConfigureJointReportsType;
import crcl.base.DwellType;
import crcl.base.EndCanonType;
import crcl.base.GetStatusType;
import crcl.base.InitCanonType;
import crcl.base.JointDetailsType;
import crcl.base.JointSpeedAccelType;
import crcl.base.JointStatusType;
import crcl.base.JointStatusesType;
import crcl.base.LengthUnitEnumType;
import static crcl.base.LengthUnitEnumType.INCH;
import static crcl.base.LengthUnitEnumType.METER;
import static crcl.base.LengthUnitEnumType.MILLIMETER;
import crcl.base.MessageType;
import crcl.base.MoveScrewType;
import crcl.base.MoveThroughToType;
import crcl.base.MoveToType;
import crcl.base.OpenToolChangerType;
import crcl.base.PointType;
import crcl.base.PoseAndSetType;
import crcl.base.PoseType;
import crcl.base.PoseToleranceType;
import crcl.base.RotAccelAbsoluteType;
import crcl.base.RotAccelRelativeType;
import crcl.base.RotAccelType;
import crcl.base.RotSpeedAbsoluteType;
import crcl.base.RotSpeedRelativeType;
import crcl.base.RotSpeedType;
import crcl.base.SetAngleUnitsType;
import crcl.base.SetEndEffectorParametersType;
import crcl.base.SetEndEffectorType;
import crcl.base.SetEndPoseToleranceType;
import crcl.base.SetIntermediatePoseToleranceType;
import crcl.base.SetLengthUnitsType;
import crcl.base.SetRotAccelType;
import crcl.base.SetRotSpeedType;
import crcl.base.SetTransAccelType;
import crcl.base.SetTransSpeedType;
import crcl.base.StopMotionType;
import crcl.base.TransAccelAbsoluteType;
import crcl.base.TransAccelRelativeType;
import crcl.base.TransAccelType;
import crcl.base.TransSpeedAbsoluteType;
import crcl.base.TransSpeedRelativeType;
import crcl.base.TransSpeedType;
import crcl.utils.CRCLPosemath;
import crcl.utils.CRCLSocket;
import crcl.utils.PoseToleranceChecker;
import crcl.utils.SimRobotEnum;
import crcl.utils.SimServerOuter;
import crcl.utils.SimulatedKinematicsPlausible;
import crcl.utils.SimulatedKinematicsSimple;
import crcl.utils.XpathUtils;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import static crcl.utils.CRCLPosemath.maxDiffDoubleArray;
import static crcl.utils.CRCLPosemath.shift;
import static crcl.utils.CRCLPosemath.toPmRotationVector;
import static crcl.utils.CRCLPosemath.vectorToPmCartesian;
import java.io.EOFException;
import java.io.PrintStream;
import java.net.SocketException;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Optional;
import java.util.Set;
import org.xml.sax.SAXException;
import rcs.posemath.PmCartesian;
import rcs.posemath.PmException;
import rcs.posemath.PmRotationVector;
import rcs.posemath.Posemath;
import crcl.utils.CRCLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import static crcl.utils.CRCLPosemath.toPmCartesian;
import static crcl.utils.CRCLPosemath.multiply;
import static crcl.utils.CRCLPosemath.toPoseType;

/**
 *
 * @author Will Shackleford {@literal <william.shackleford@nist.gov>}
 */
public class SimServerInner {

    private final static Set<SimServerInner> runningServers = new HashSet<>();
    private static final Logger LOGGER = Logger.getLogger(SimServerInner.class.getName());
    public static boolean testing = false;
    private static final double SCALE_FUDGE_FACTOR = 0.5;
    public static long debugCmdSendTime = 0;

    private static double getDoubleProperty(String propName, double defaultVal) {
        return Double.valueOf(System.getProperty(propName, Double.toString(defaultVal)));
    }

    static public void printAllClientStates(final PrintStream ps) {
        SimServerInner.runningServers.forEach(s -> s.printClientStates(ps));
    }

    private static long getLongProperty(String propName, long defaultLong) {
        return Long.valueOf(System.getProperty(propName, Long.toString(defaultLong)));
    }

    private CRCLSocket gripperSocket;

    private final XpathUtils xpu;
    private final SimServerOuter outer;
    Queue<CRCLCommandInstanceType> cmdQueue = new ConcurrentLinkedQueue<>();
    private final Queue<CRCLCommandInstanceType> gripperCmdQueue = new ConcurrentLinkedQueue<>();

    double[] jointPositions; // = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
    double[] lastJointPositions;// = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
    double[] commandedJointPositions;// = new double[]{0.0, 45.0, -90.0, 90.0, 0.0, 0.0};
    double[] jointVelocites;// = new double[jointPositions.length];
    double[] commandedJointVelocities;// = new double[]{0.0, 45.0, -90.0, 90.0, 0.0, 0.0};
    double[] commandedJointAccellerations;// = new double[]{0.0, 45.0, -90.0, 90.0, 0.0, 0.0};
    double[] jointmins; // = new double[]{-170.0, 5.0, -170.0, +10.0, -135.0, -135.0};
    double[] jointmaxs;// = new double[]{+170.0, 85.0, -10.0, 170.0, +135.0, +135.0};
    double[] seglengths;// = SimulatedKinematicsPlausible.DEFAULT_SEGLENGTHS;

//    double[] jointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
//    double[] lastJointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
//    double[] commandedJointPositions = new double[]{0.0, 45.0, -90.0, 90.0, 0.0, 0.0};
//    double[] jointVelocites = new double[jointPositions.length];
//    double jointmins[] = new double[]{-170.0, 5.0, -170.0, +10.0, -135.0, -135.0};
//    double jointmaxs[] = new double[]{+170.0, 85.0, -10.0, 170.0, +135.0, +135.0};
//    double seglengths[] = SimulatedKinematicsPlausible.DEFAULT_SEGLENGTHS;
    private PoseType goalPose;

    private final double maxTransSpeed = getDoubleProperty("SimServer.maxTransSpeed", 2.0);
    private final double maxTransAccel = getDoubleProperty("SimServer.maxTransAccell", 20.0);

    private double curTransSpeed = 0;
    private double commandedTransSpeed = maxTransSpeed * 0.5;
    private double curTransAccel = 0;
    private double commandedTransAccel = maxTransAccel * 0.5;

    private final double maxRotSpeed = getDoubleProperty("SimServer.maxRotSpeed", 2.0);
    private final double maxRotAccel = getDoubleProperty("SimServer.maxRotAccell", 20.0);

    private double curRotSpeed = 0;
    private double commandedRotSpeed = maxRotSpeed * 0.5;
    private double curRotAccel = 0;
    private double commandedRotAccel = maxRotAccel * 0.5;

    private List<CRCLCommandType> cmdLog;
    private SimRobotEnum robotType = SimRobotEnum.PLAUSIBLE;
    private int port;
    private boolean moveStraight = false;
    private ServerSocket ssock = null;
    private final SimulatedKinematicsPlausible skPlausible = new SimulatedKinematicsPlausible();
    private final SimulatedKinematicsSimple skSimple = new SimulatedKinematicsSimple();
    private CRCLStatusType status;
    private File[] statSchemaFiles = null;
    private CRCLCommandType multiStepCommand = null;
    private int moveScrewStep = 0;
    private BigDecimal moveScriptTurnComplete = BigDecimal.ZERO;
    private double jointSpeedMax = getDoubleProperty("SimServer.jointSpeedMax", 200.0);
    private PmRotationVector lastDiffRotv = null;
    private int cycle_count = 0;
    private final List<ClientState> clientStates = new ArrayList<>();
    private final Map<CRCLSocket, Thread> clientThreadMap = new ConcurrentHashMap<>();
    Thread simThread = null;
    private volatile int close_count = 0;
    BigInteger maxCmdId = BigInteger.ONE;
    Map<CRCLSocket, LastStatusInfo> lastStatusMap = null;
    private boolean executingMoveCommand = false;
    long debugUpdateStatusTime = 0;
    private int currentWaypoint;
    private final boolean enableGetStatusIDCheck
            = Boolean.valueOf(System.getProperty("crcl.utils.SimServerInner.enableGetStatusIDCheck", "false"));
    private final Set<Class<? extends CRCLCommandType>> gripperCommands = new HashSet<>(
            Arrays.asList(
                    InitCanonType.class,
                    EndCanonType.class,
                    SetEndEffectorType.class,
                    SetEndEffectorParametersType.class)
    );
    private long cmdQueuePutTime = 0;
    private boolean debug_this_command = false;
    private int cmdQueueCmdsOffered = 0;
    Thread acceptClientsThread = null;
    private long delayMillis = getLongProperty("SimServer.delayMillis", 100);
    private ConfigureJointReportsType cjrs = null;
    private Map<Integer, ConfigureJointReportType> cjrMap = null;
    private AngleUnitEnumType angleType = AngleUnitEnumType.RADIAN;
    private PoseToleranceType expectedEndPoseTolerance = new PoseToleranceType();
    private PoseToleranceType expectedIntermediatePoseTolerance;
    private long dwellEndTime = 0;
    private long debugCmdStartTime = 0;
    private final long debugCmdQueueTime = 0;
    private long cmdQueueMaxSize = 0;
    private long maxCmdQueuePollTime = 0;
    private CRCLCommandInstanceType lastReadCommandInstance = null;
    private int cmdQueuePollReturnCount = 0;
    private int cmdQueuePollReturnNonNullCount = 0;
    long maxDiffCmdQueuePutEmpty = 0;
    private double lengthScale = 0.01 * SCALE_FUDGE_FACTOR;
    private LengthUnitEnumType lengthUnit = LengthUnitEnumType.MILLIMETER;
    private List<PoseType> waypoints;
    private long maxReadCommandTime = 0;
    private long maxUpdateStatusTime = 0;
    private long maxSimCycleTime = 0;
    private long simCycleCount = 0;
    private CRCLSocket checkerCRCLSocket = null;

    public SimServerInner(SimServerOuter _outer) throws ParserConfigurationException {
        this.outer = _outer;
        this.xpu = new XpathUtils();
        this.status = new CRCLStatusType();
        this.robotType = SimRobotEnum.SIMPLE;
        this.port = CRCLSocket.DEFAULT_PORT;
        this.resetToDefaults();
        String portPropertyString = System.getProperty("crcljava.port");
        if (null != portPropertyString) {
            this.port = Integer.valueOf(portPropertyString);
        }
    }

    /**
     * Get the value of gripperSocket
     *
     * @return the value of gripperSocket
     */
    public CRCLSocket getGripperSocket() {
        return gripperSocket;
    }

    /**
     * Set the value of gripperSocket
     *
     * @param gripperSocket new value of gripperSocket
     */
    public void setGripperSocket(CRCLSocket gripperSocket) {
        this.gripperSocket = gripperSocket;
    }

    public Queue<CRCLCommandInstanceType> getGripperCmdQueue() {
        return gripperCmdQueue;
    }

    /**
     * Get the value of cmdLog
     *
     * @return the value of cmdLog
     */
    public List<CRCLCommandType> getCmdLog() {
        if (null == cmdLog) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(cmdLog);
    }

    private void resetToPlausibleDefaults() {
        jointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
        lastJointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
        commandedJointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
        jointVelocites = new double[jointPositions.length];
        jointmins = new double[]{-170.0, 5.0, -170.0, +10.0, -135.0, -135.0};
        jointmaxs = new double[]{+170.0, 85.0, -10.0, 170.0, +135.0, +135.0};
        seglengths = SimulatedKinematicsPlausible.DEFAULT_SEGLENGTHS;
    }

    private void resetToSimpleDefaults() {
        jointPositions = Arrays.copyOf(SimulatedKinematicsSimple.DEFAULT_JOINTVALS, SimulatedKinematicsSimple.DEFAULT_JOINTVALS.length);
        lastJointPositions = Arrays.copyOf(SimulatedKinematicsSimple.DEFAULT_JOINTVALS, SimulatedKinematicsSimple.DEFAULT_JOINTVALS.length);
        commandedJointPositions = Arrays.copyOf(SimulatedKinematicsSimple.DEFAULT_JOINTVALS, SimulatedKinematicsSimple.DEFAULT_JOINTVALS.length);
        jointVelocites = new double[jointPositions.length];
        jointmins = null;// new double[]{-170.0, 5.0, -170.0, +10.0, -135.0, -135.0};
        jointmaxs = null;//new double[]{+170.0, 85.0, -10.0, 170.0, +135.0, +135.0};
        seglengths = SimulatedKinematicsSimple.DEFAULT_SEGLENGTHS;
    }

    /**
     * Get the value of robotType
     *
     * @return the value of robotType
     */
    public SimRobotEnum getRobotType() {
        return robotType;
    }

    private void resetToDefaults() {
        switch (robotType) {
            case PLAUSIBLE:
                resetToPlausibleDefaults();
                break;

            case SIMPLE:
                resetToSimpleDefaults();
                break;
        }
    }

    /**
     * Set the value of robotType
     *
     * @param robotType new value of robotType
     */
    public void setRobotType(SimRobotEnum robotType) {
        this.robotType = robotType;
        this.resetToDefaults();
    }

    public XpathUtils getXpu() {
        return xpu;
    }

    /**
     * Get the value of port
     *
     * @return the value of port
     */
    public int getPort() {
        return port;
    }

    private boolean teleportToGoals;

    /**
     * Get the value of teleportToGoals
     *
     * @return the value of teleportToGoals
     */
    public boolean isTeleportToGoals() {
        return teleportToGoals;
    }

    /**
     * Set the value of teleportToGoals
     *
     * @param teleportToGoals new value of teleportToGoals
     */
    public void setTeleportToGoals(boolean teleportToGoals) {
        this.teleportToGoals = teleportToGoals;
    }

    public void simulatedTeleportToPose(PoseType pose) {
        try {
            if (null != pose) {
                PointType pt = pose.getPoint();
                if (null != pt) {
                    switch (robotType) {
                        case PLAUSIBLE:
                            jointPositions = skPlausible.poseToJoints(this.jointPositions, pose);

                            CRCLPosemath.setPose(this.getStatus(), skPlausible.jointsToPose(jointPositions, CRCLPosemath.getPose(this.getStatus())));
                            break;

                        case SIMPLE:
                            jointPositions = skSimple.poseToJoints(this.jointPositions, pose);
                            CRCLPosemath.setPose(this.getStatus(), skSimple.jointsToPose(jointPositions, CRCLPosemath.getPose(this.getStatus())));
                            break;
                    }
                    commandedJointPositions = Arrays.copyOf(jointPositions, jointPositions.length);
                    this.goalPose = null;
                    this.setWaypoints(null);
                    CommandStatusType cst = this.getStatus().getCommandStatus();
                    if (cst == null) {
                        cst = new CommandStatusType();
                    }
                    if (null == cst.getStatusID()) {
                        cst.setStatusID(BigInteger.ONE);
                    }

                    setCommandState(CommandStateEnumType.CRCL_DONE);
                }
            }

        } catch (PmException ex) {
            Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Set the value of port
     *
     * @param port new value of port
     */
    public void setPort(int port) {
        this.port = port;
        if (null != this.ssock) {
            this.restartServer();
        }
    }

    /**
     * Get the value of moveStraight
     *
     * @return the value of moveStraight
     */
    public boolean isMoveStraight() {
        return moveStraight;
    }

    public void setJointPosition(double _position, int index) {
        this.jointPositions[index] = _position;
    }

    public void setCommandedJointPosition(double _position, int index) {
        this.commandedJointPositions[index] = _position;
    }

    public void reset() {
        try {
            jointPositions = Arrays.copyOf(SimulatedKinematicsPlausible.DEFAULT_JOINTVALS, SimulatedKinematicsPlausible.DEFAULT_JOINTVALS.length);
            switch (robotType) {
                case PLAUSIBLE:
                    CRCLPosemath.setPose(this.getStatus(), skPlausible.jointsToPose(jointPositions, CRCLPosemath.getPose(this.getStatus())));
                    break;

                case SIMPLE:
                    CRCLPosemath.setPose(this.getStatus(), skSimple.jointsToPose(jointPositions, CRCLPosemath.getPose(this.getStatus())));
                    break;
            }
            commandedJointPositions = Arrays.copyOf(jointPositions, jointPositions.length);
            this.goalPose = null;
            this.setWaypoints(null);
            CommandStatusType cst = this.getStatus().getCommandStatus();
            if (cst == null) {
                cst = new CommandStatusType();
            }
            if (null == cst.getStatusID()) {
                cst.setStatusID(BigInteger.ONE);
            }
            setCommandState(CommandStateEnumType.CRCL_DONE);
        } catch (PmException ex) {
            Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public double[] getJointPositions() {
        return jointPositions;
    }

    public double[] getSeglengths() {
        return seglengths;
    }

    public void setGoalPose(PoseType goalPose) {
        this.goalPose = goalPose;
        if (null != goalPose) {
            checkPose(goalPose);
            if (teleportToGoals) {
                this.simulatedTeleportToPose(goalPose);
            }
        }

    }

    public boolean isFinishedMove() {
        double jointdiffs[] = new double[this.jointPositions.length];
        Arrays.setAll(jointdiffs, (i) -> Math.abs(jointPositions[i] - commandedJointPositions[i]));
        double maxdiff = Arrays.stream(jointdiffs).max().orElse(0);
        if (maxdiff > getJointDiffMax()) {
            return false;
        }
        Arrays.setAll(jointdiffs, (i) -> Math.abs(jointPositions[i] - lastJointPositions[i]));
        maxdiff = Arrays.stream(jointdiffs).max().orElse(0);
        return maxdiff <= getJointDiffMax();
    }

    private boolean handleContinueMoveScrew(MoveScrewType moveScrew) {
        switch (moveScrewStep) {
            case 0:
                setCommandState(CommandStateEnumType.CRCL_WORKING);
                if (moveScrew.getStartPosition() != null) {
                    goalPose = moveScrew.getStartPosition();
                    moveScrewStep = 1;
                } else {
                    moveScrewStep = 2;
                }
                break;

            case 1:
                if (isFinishedMove()) {
                    if (!PoseToleranceChecker.isInTolerance(CRCLPosemath.getPose(status), goalPose,
                            expectedEndPoseTolerance, angleType)) {
                        multiStepCommand = null;
                        setCommandState(CommandStateEnumType.CRCL_ERROR);
                        return false;
                    }
                }
                break;

            case 2:
                if (moveScrew.getAxialDistanceFree() != null && moveScrew.getAxialDistanceFree().compareTo(BigDecimal.ZERO) > 0) {
                    goalPose = shift(CRCLPosemath.getPose(status),
                            multiply(moveScrew.getAxialDistanceFree(), CRCLPosemath.getXAxis(status)));
                    setMoveStraight(true);
                    moveScrewStep = 3;
                } else {
                    moveScrewStep = 4;
                }
                break;

            case 3:
                if (isFinishedMove()) {
                    moveScrewStep = 4;
                }
                break;

            case 4:
                moveScriptTurnComplete = BigDecimal.ZERO;
                moveScrewStep = 5;
                break;

            case 5:
                multiStepCommand = null;
                setCommandState(CommandStateEnumType.CRCL_DONE);
                return false;
        }
        return true;
//        setCommandState(CommandStateEnumType.CRCL_DONE);
//        multiStepCommand = null;
//        return false;
    }

    private boolean handleMultiStepCommand() {
        if (null == multiStepCommand) {
            return false;
        }
        if (multiStepCommand instanceof MoveScrewType) {
            return handleContinueMoveScrew((MoveScrewType) multiStepCommand);
        }
        multiStepCommand = null;
        return false;
    }

    public void setCommandState(CommandStateEnumType state) {
        synchronized (status) {
            CommandStatusType cst = status.getCommandStatus();
            if (null == cst) {
                cst = new CommandStatusType();
                status.setCommandStatus(cst);
            }
            cst.setCommandState(state);
        }
    }

    public CommandStateEnumType getCommandState() {
        CommandStatusType cst = status.getCommandStatus();
        if (null == cst) {
            setCommandState(CommandStateEnumType.CRCL_ERROR);
        }
        cst = status.getCommandStatus();
        return cst.getCommandState();
    }

    private void showMessage(String s) {
        String short_status = s.trim();
        if (short_status.length() > 40) {
            short_status = short_status.substring(0, 36) + " ...";
        }
//        status.getCommandStatus().setStateDescription(short_status);
        outer.showMessage(s);
    }

    public boolean checkPose(PoseType goalPose) {
        PmCartesian xvec = vectorToPmCartesian(goalPose.getXAxis());
        if (Math.abs(xvec.mag() - 1.0) > 1e-3) {
            showMessage("Bad postion : xvec " + xvec + " has magnitude not equal to one.");
            setCommandState(CommandStateEnumType.CRCL_ERROR);
            return false;
        }
        PmCartesian zvec = vectorToPmCartesian(goalPose.getZAxis());
        if (Math.abs(zvec.mag() - 1.0) > 1e-3) {
            showMessage("Bad postion : zvec " + zvec + " has magnitude not equal to one.");
            setCommandState(CommandStateEnumType.CRCL_ERROR);
            return false;
        }
        if (Math.abs(Posemath.pmCartCartDot(xvec, zvec)) > 1e-3) {
            showMessage("Bad postion : xvec " + xvec + " and zvec " + zvec + " are not orthogonal.");
            setCommandState(CommandStateEnumType.CRCL_ERROR);
            return false;
        }
        return true;
    }

    public double getJointSpeedMax() {
        return jointSpeedMax;
    }

    public void setJointSpeedMax(double jointSpeedMax) {
        this.jointSpeedMax = jointSpeedMax;
    }

    public PoseType limitSpeedAccel(PoseType goal,
            PoseType current) {
        PoseType newGoal = goal;
        try {
            PmCartesian goalPt = toPmCartesian(goalPose.getPoint());
            PmCartesian currentPt = toPmCartesian(current.getPoint());
            PmCartesian diffPt = goalPt.subtract(currentPt);
            double lastTransSpeed = this.curTransSpeed;
            double diffTransSpeed = diffPt.mag() / (delayMillis * 1e-3);
            this.curTransSpeed = diffTransSpeed;
            this.curTransAccel = this.curTransSpeed - lastTransSpeed;
            if (Math.abs(curTransAccel) > this.commandedTransAccel) {
                this.curTransSpeed = lastTransSpeed
                        + Math.signum(curTransAccel) * this.commandedTransAccel;
            }
            if (this.curTransSpeed > this.commandedTransSpeed) {
                this.curTransSpeed = this.commandedTransSpeed;
            }
            PmRotationVector goalRotv = toPmRotationVector(goalPose);
            PmRotationVector currentRotv = toPmRotationVector(current);
            PmRotationVector diffRotv = currentRotv.inv().multiply(goalRotv);
            double lastRotSpeed = this.curRotSpeed;
            double diffRotSpeed = diffRotv.s / (delayMillis * 1e-3);
            this.curRotSpeed = diffRotSpeed;
            this.curRotAccel = this.curRotSpeed - lastRotSpeed;
            if (Math.abs(curRotAccel) > this.commandedRotAccel) {
                this.curRotSpeed = lastRotSpeed
                        + Math.signum(curRotAccel) * this.commandedRotAccel;
            }
            if (Math.abs(this.curRotSpeed) > this.commandedRotSpeed) {
                this.curRotSpeed = this.commandedRotSpeed * Math.signum(this.curRotSpeed);
            }
            double transMoveFraction = 1.0;
            if (Math.abs(diffTransSpeed - this.curTransSpeed) > this.commandedTransSpeed * 0.1
                    && Math.abs(diffTransSpeed) > 1e-6) {
                transMoveFraction = curTransSpeed / diffTransSpeed;
            }
            double rotMoveFraction = 1.0;
            if (Math.abs(diffRotSpeed - this.curRotSpeed) > this.commandedRotSpeed * 0.1
                    && Math.abs(diffRotSpeed) > 1e-6) {
                rotMoveFraction = Math.abs(curRotSpeed / diffRotSpeed);
            }
            double moveFraction = Math.min(transMoveFraction, rotMoveFraction);
            assert (moveFraction > 0);
            if (moveFraction < 0.99) {
                PmCartesian newGoalPt = currentPt.add(diffPt.multiply(moveFraction));
                PmRotationVector newGoalRotv = currentRotv.multiply(diffRotv.multiply(moveFraction));
                newGoal = toPoseType(newGoalPt, newGoalRotv);
            }
            lastDiffRotv = diffRotv;
        } catch (PmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            outer.showMessage(ex.toString());
        }
        return newGoal;
    }

    public double[] getStraightMoveCommandedJointVals(PoseType curGoalPose) {
        try {
            final double JOINT_DIFF_MAX = getJointDiffMax();
            PmCartesian goalPt = toPmCartesian(curGoalPose.getPoint());
            PmCartesian currentPt = toPmCartesian(CRCLPosemath.getPoint(status));
            PmCartesian diffPt = goalPt.subtract(currentPt);
            PmRotationVector goalRotv = toPmRotationVector(curGoalPose);
            PmRotationVector currentRotv = toPmRotationVector(CRCLPosemath.getPose(status));
            PmRotationVector diffRotv = goalRotv.multiply(currentRotv.inv());
            PoseType newGoalPose = curGoalPose;
            goalPoseToCommandedPositions(newGoalPose);
            double maxdiff = maxDiffDoubleArray(this.commandedJointPositions, this.jointPositions);
            double scale = 1.0;
            while (maxdiff > JOINT_DIFF_MAX) {
                scale *= JOINT_DIFF_MAX / (maxdiff + 0.01);
                PmCartesian scaledDiffPt = diffPt.multiply(scale);
                PmCartesian newGoalPt = currentPt.add(scaledDiffPt);
                PmRotationVector scaledDiffRot = diffRotv.multiply(scale);
                PmRotationVector newGoalRotv = currentRotv.multiply(scaledDiffRot);
                newGoalPose = toPoseType(newGoalPt, newGoalRotv);
                this.goalPoseToCommandedPositions(newGoalPose);
                maxdiff = maxDiffDoubleArray(this.commandedJointPositions, this.jointPositions);
            }
        } catch (PmException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return this.commandedJointPositions;
    }

    public void setCmdSchema(File[] fa) {
        try {
            fa = CRCLSocket.reorderCommandSchemaFiles(fa);
            CRCLSocket.defaultCmdSchema = CRCLSocket.filesToSchema(fa);
            for (ClientState state : this.clientStates) {
                state.getCs().setCmdSchema(CRCLSocket.defaultCmdSchema);
            }
        } catch (CRCLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void setStatSchema(File[] fa) {
        try {
            fa = CRCLSocket.reorderStatSchemaFiles(fa);
            CRCLSocket.defaultStatSchema = CRCLSocket.filesToSchema(fa);
            for (ClientState state : this.clientStates) {
                state.getCs().setStatSchema(CRCLSocket.defaultStatSchema);
            }
            this.statSchemaFiles = fa;
        } catch (CRCLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    public void setStatus(CRCLStatusType status) {
        this.status = status;
    }

    /**
     * Set the value of moveStraight
     *
     * @param moveStraight new value of moveStraight
     */
    public void setMoveStraight(boolean moveStraight) {
        this.moveStraight = moveStraight;
    }

    public void closeServer() {
        try {
            SimServerInner.runningServers.remove(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.close_count++;
//        System.out.println("close_count = " + close_count);
        if (null != acceptClientsThread) {
            try {
                acceptClientsThread.interrupt();
                acceptClientsThread.join(100);
                acceptClientsThread = null;
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
        }
        if (null != clientThreadMap) {
            synchronized (clientThreadMap) {
                for (Thread t : clientThreadMap.values()) {
                    try {
                        t.interrupt();
                        t.join(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (null != simThread) {
            try {
                simThread.interrupt();
                simThread.join(100);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            simThread = null;
        }
        if (null != clientStates) {
            for (int i = 0; i < clientStates.size(); i++) {
                try {
                    ClientState s = clientStates.get(i);
                    s.close();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
            clientStates.clear();
        }
        if (null != ssock) {
            try {
                this.ssock.close();
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            this.ssock = null;
        }
        this.updateConnectedClients();
        this.lastStatusMap = null;
        if (null != clientThreadMap) {
            synchronized (clientThreadMap) {
                for (Thread t : clientThreadMap.values()) {
                    try {
                        t.interrupt();
                        t.join(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                this.clientThreadMap.clear();
            }
        }

    }

    private void sendStatus(CRCLSocket socket) {
        CRCLSocket curSocket = socket;
        try {
            if (null == socket) {
                if (!outer.isSendStatusWithoutRequestSelected()
                        || null == clientStates
                        || clientStates.size() < 1) {
                    return;
                }
            }
            synchronized (status) {
                CommandStatusType cst = status.getCommandStatus();
                if (null == cst) {
                    cst = new CommandStatusType();
                    status.setCommandStatus(cst);
                }
                if (null == getCommandState()) {
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                }
                BigInteger sid = cst.getStatusID();
                if (sid == null) {
                    cst.setStatusID(BigInteger.ONE);
                }
                BigInteger cid = cst.getCommandID();
                if (cid == null) {
                    cst.setCommandID(BigInteger.ONE);
                }
//            outer.showDebugMessage("status="+CRCLSocket.statToDebugString(status));
                if (null != socket) {
                    try {
                        socket.appendTrailingZero = outer.isAppendZeroSelected();
                        socket.randomPacketing = outer.isRandomPacketSelected();
                        socket.setReplaceHeader(outer.isReplaceXmlHeaderSelected());
                        if (outer.isReplaceStateSelected()) {
                            socket.setStatusStringOutputFilter(CRCLSocket.removeCRCLFromState);
                        } else {
                            socket.setStatusStringOutputFilter(null);
                        }
                        boolean new_state = true;
                        if (null != this.lastStatusMap) {
                            LastStatusInfo lsi = this.lastStatusMap.get(socket);
                            new_state = (null == lsi
                                    || null == lsi.lastSentCid
                                    || null == lsi.lastSentState
                                    || !lsi.lastSentCid.equals(status.getCommandStatus().getCommandID())
                                    || !lsi.lastSentState.equals(status.getCommandStatus().getCommandState()));
                        }
                        if (outer.isDebugSendStatusSelected() && new_state) {
                            outer.showDebugMessage("Status sent to " + socket.getInetAddress() + ":" + socket.getPort()
                                    + " CommandId="
                                    + status.getCommandStatus().getCommandID()
                                    + " StatusId="
                                    + status.getCommandStatus().getStatusID()
                                    + " State="
                                    + status.getCommandStatus().getCommandState());
                        }
                        socket.writeStatus(status, outer.isValidateXMLSelected());
                        if (debugUpdateStatusTime > 0) {
                            long diffcurrentTimeMillisdebugUpdateStatusTime = System.currentTimeMillis() - debugUpdateStatusTime;
                            System.out.println("diffcurrentTimeMillisdebugUpdateStatusTime = " + diffcurrentTimeMillisdebugUpdateStatusTime);
                            debugUpdateStatusTime = 0;
                        }
                        if (outer.isDebugSendStatusSelected() && new_state) {
                            outer.showDebugMessage("writeStatus Complete");
                        }
                        if (new_state) {
                            if (null == this.lastStatusMap) {
                                this.lastStatusMap = new IdentityHashMap<>();
                            }
                            LastStatusInfo lsi = new LastStatusInfo();
                            lsi.lastSentCid = status.getCommandStatus().getCommandID();
                            lsi.lastSentState = status.getCommandStatus().getCommandState();
                            this.lastStatusMap.put(socket, lsi);
                        }
                    } catch (CRCLException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                        try {
                            socket.close();
                        } catch (IOException ex1) {
                            LOGGER.log(Level.SEVERE, null, ex1);
                        }
                        this.clientStates.remove(socket);
//                    showMessage(ex);
                    }
                    return;
                }
                curSocket = clientStates.get(0).getCs();
                if (outer.isReplaceStateSelected()) {
                    curSocket.setStatusStringOutputFilter(CRCLSocket.removeCRCLFromState);
                } else {
                    curSocket.setStatusStringOutputFilter(null);
                }
                boolean new_state = true;
                if (null != this.lastStatusMap) {
                    LastStatusInfo lsi = this.lastStatusMap.get(curSocket);
                    new_state = (null == lsi
                            || null == lsi.lastSentCid
                            || null == lsi.lastSentState
                            || !lsi.lastSentCid.equals(status.getCommandStatus().getCommandID())
                            || !lsi.lastSentState.equals(status.getCommandStatus().getCommandState()));
                }
                if (outer.isDebugSendStatusSelected() && new_state) {
                    outer.showDebugMessage("Status sent to " + socket.getInetAddress() + ":" + socket.getPort()
                            + " CommandId="
                            + status.getCommandStatus().getCommandID()
                            + " StatusId="
                            + status.getCommandStatus().getStatusID()
                            + " State="
                            + status.getCommandStatus().getCommandState());
                }
                String xmls = curSocket.statusToString(status, outer.isValidateXMLSelected());
//            System.out.println("SimServer.sendStatus() : xmls = " + xmls);
//            String strout = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+xmls;

//            byte ba[] = xmls.getBytes();
//            byte fill[] = new byte[2000];
                int write_count = 0;
                for (int i = 0; i < clientStates.size(); i++) {
                    curSocket = clientStates.get(i).getCs();

                    try {
                        curSocket.appendTrailingZero = outer.isAppendZeroSelected();
                        curSocket.randomPacketing = outer.isRandomPacketSelected();
                        curSocket.setReplaceHeader(outer.isReplaceXmlHeaderSelected());
                        curSocket.writeWithFill(xmls);
                        write_count++;
                    } catch (IOException ex) {
                        try {
                            LOGGER.log(Level.SEVERE, null, ex);
                            clientStates.remove(i);
                            Thread t = null;
                            synchronized (clientThreadMap) {
                                t = clientThreadMap.get(curSocket);
                                clientThreadMap.remove(curSocket);
                            }
                            if (null != t) {
                                t.interrupt();
                                t.join(100);
                            }
                            updateConnectedClients();
                        } catch (InterruptedException ex1) {
                            LOGGER.log(Level.SEVERE, null, ex1);
                        }
                    } catch (InterruptedException ex) {
                        LOGGER.log(Level.SEVERE, null, ex);
                    }
                }
                if (outer.isDebugSendStatusSelected() && new_state) {
                    outer.showDebugMessage("writeStatus  to " + write_count + " clients complete.");
                }
                if (new_state) {
                    if (null == this.lastStatusMap) {
                        this.lastStatusMap = new IdentityHashMap<>();
                    }
                    LastStatusInfo lsi = new LastStatusInfo();
                    lsi.lastSentCid = status.getCommandStatus().getCommandID();
                    lsi.lastSentState = status.getCommandStatus().getCommandState();
                    this.lastStatusMap.put(socket, lsi);
                }
            }
//            System.out.println("SimServer.sendStatus() sent data to " + write_count + " clients.");
        } catch (CRCLException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            showMessage(ex + "\n" + ((curSocket != null) ? curSocket.getLastStatusString() : ""));
        }
    }

    private boolean isCoordinated(PoseType pose) {
        if (pose instanceof PoseAndSetType) {
            PoseAndSetType pas = (PoseAndSetType) pose;
            return pas.isCoordinated();
        }
        return false;
    }

    public XMLGregorianCalendar getXMLGregorianCalendarNow()
            throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
        XMLGregorianCalendar now
                = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        return now;
    }

    private void incStatusId() {
        synchronized (status) {
            CommandStatusType cst = status.getCommandStatus();
            if (null != cst) {
                cst.setStatusID(
                        Optional.ofNullable(cst.getStatusID())
                        .orElse(BigInteger.ZERO)
                        .add(BigInteger.ONE)
                );
            }
            if (null != cmdLog && cmdLog.size() > 0) {
                cst.setStateDescription("Running " + this.cmdLog.get(cmdLog.size() - 1).getClass().getSimpleName());
            }
        }
    }

    /**
     *
     * @return the boolean
     */
    private boolean updateStatus() {
        boolean jointschanged = false;
        PoseType curGoalPose = null;
        try {
            synchronized (status) {
                if (!outer.isEditingStatus()) {
                    if (debugCmdStartTime > 0) {
                        debugUpdateStatusTime = System.currentTimeMillis();
                        long diffdebugUpdateStatusTimedebugCmdStartTime = debugUpdateStatusTime - debugCmdStartTime;
                        System.out.println("diffdebugUpdateStatusTimedebugCmdStartTime = " + diffdebugUpdateStatusTimedebugCmdStartTime);
                        debugCmdStartTime = 0;
                    }
                    if (null == status.getCommandStatus()) {
                        CommandStatusType cst = new CommandStatusType();
                        cst.setCommandID(BigInteger.ONE);
                        cst.setStatusID(BigInteger.ONE);
                        cst.setCommandState(CommandStateEnumType.CRCL_WORKING);
                        status.setCommandStatus(new CommandStatusType());
                    }
                    this.incStatusId();
                    if (null == goalPose
                            && null != waypoints
                            && this.currentWaypoint < waypoints.size() - 1) {
                        int new_waypoint = this.currentWaypoint + 1;
                        this.setCurrentWaypoint(new_waypoint);
                        this.goalPose = this.waypoints.get(this.currentWaypoint);
                    }
                    if (null != goalPose) {
                        curGoalPose = this.limitSpeedAccel(goalPose, CRCLPosemath.getPose(status));
                        if (this.moveStraight || isCoordinated(goalPose)) {
                            this.commandedJointPositions = getStraightMoveCommandedJointVals(curGoalPose);
                        } else {
                            goalPoseToCommandedPositions(curGoalPose);
                        }
                    }
                    if (null == commandedJointPositions) {
                        this.commandedJointPositions = Arrays.copyOf(jointPositions, jointPositions.length);
                    }
                    for (int i = 0; i < jointPositions.length; i++) {
                        final double JOINT_DIFF_MAX = getAllowedJointDiff(i);
                        if (Math.abs(commandedJointPositions[i] - jointPositions[i]) < JOINT_DIFF_MAX) {
                            if (Math.abs(commandedJointPositions[i] - jointPositions[i]) > 1e-4) {
                                jointschanged = true;
                            }
                            jointPositions[i] = commandedJointPositions[i];
                        } else {
                            jointPositions[i] += JOINT_DIFF_MAX * Math.signum(commandedJointPositions[i] - jointPositions[i]);
                            if (robotType == SimRobotEnum.SIMPLE && curGoalPose != null) {
                                jointPositions[i] = commandedJointPositions[i];
                            }
                            jointschanged = true;
                        }
                        if (jointmins != null && jointmins.length > i && jointPositions[i] < jointmins[i]) {
                            goalPose = null;
                            setCommandState(CommandStateEnumType.CRCL_ERROR);
                            showMessage("Joint " + (i + 1) + " at " + jointPositions[i] + " less than limit " + jointmins[i]);
                            jointPositions[i] = jointmins[i];
                            commandedJointPositions[i] = jointPositions[i];
                        }
                        if (jointmaxs != null && jointmaxs.length > i && jointPositions[i] > jointmaxs[i]) {
                            goalPose = null;
                            setCommandState(CommandStateEnumType.CRCL_ERROR);
                            showMessage("Joint " + (i + 1) + " at " + jointPositions[i] + " more than limit " + jointmaxs[i]);
                            jointPositions[i] = jointmaxs[i];
                            commandedJointPositions[i] = jointPositions[i];
                        }
                        jointVelocites[i] = ((jointPositions[i] - lastJointPositions[i]) * 1000.0) / delayMillis;
                        lastJointPositions[i] = jointPositions[i];
                        JointStatusesType jsst = status.getJointStatuses();
                        if (null == jsst) {
                            jsst = new JointStatusesType();
                            status.setJointStatuses(jsst);
                        }
                        List<JointStatusType> jsl = jsst.getJointStatus();
                        JointStatusType js = null;
                        if (i < jsl.size()) {
                            js = jsl.get(i);
                        }
                        if (null == js) {
                            js = new JointStatusType();
                            jsl.add(i, js);
                        }
                        js.setJointNumber(BigInteger.valueOf(i + 1));
                        if (null != cjrMap && cjrMap.size() > 0) {
                            js.setJointPosition(null);
                            js.setJointVelocity(null);
                            js.setJointTorqueOrForce(null);
                            ConfigureJointReportType cjrt = this.cjrMap.get(js.getJointNumber().intValue());
                            if (null != cjrt) {
                                if (cjrt.getJointNumber().compareTo(js.getJointNumber()) == 0) {
                                    if (cjrt.isReportPosition()) {
                                        js.setJointPosition(BigDecimal.valueOf(jointPositions[i]));
                                    }
                                    if (cjrt.isReportVelocity()) {
                                        js.setJointVelocity(BigDecimal.valueOf(jointVelocites[i]));
                                    }
                                    if (cjrt.isReportTorqueOrForce()) {
                                        js.setJointTorqueOrForce(BigDecimal.TEN);
                                    }
                                }
                            }
                            if (this.getCommandState() == CommandStateEnumType.CRCL_WORKING
                                    && cmdLog.get(cmdLog.size() - 1) instanceof ConfigureJointReportsType) {
                                this.setCommandState(CommandStateEnumType.CRCL_DONE);
                            }
                        } else {
                            js.setJointPosition(BigDecimal.valueOf(jointPositions[i]));
                        }
                    }
                    if (jointschanged
                            || null == CRCLPosemath.getPose(status)) {
                        switch (robotType) {
                            case PLAUSIBLE:
                                CRCLPosemath.setPose(status, skPlausible.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
                                break;

                            case SIMPLE:
                                CRCLPosemath.setPose(status, skSimple.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
                                break;
                        }
//                        if (null != curGoalPose) {
//                            PmCartesian curGoalCart = CRCLPosemath.toPmCartesian(curGoalPose.getPoint());
//                            PmCartesian curCart = CRCLPosemath.toPmCartesian(CRCLPosemath.getPoint(status));
//                            PmCartesian cartDiff = curCart.subtract(curGoalCart);
//                            double cartDiffMag = cartDiff.mag();
//                            PmRotationVector curGoalRotv = CRCLPosemath.toPmRotationVector(curGoalPose);
//                            PmRotationVector curRotv = CRCLPosemath.toPmRotationVector(CRCLPosemath.getPose(status));
//                            PmRotationVector diffRotv = curRotv.multiply(curGoalRotv.inv());
//                            double rotDiffMag = Math.abs(diffRotv.s);
//                            if (rotDiffMag > 1e-4 || cartDiffMag > 1e-4) {
//                                System.out.println("jointmins = " + Arrays.toString(jointmins));
//                                System.out.println("jointmaxs = " + Arrays.toString(jointmaxs));
//                                System.out.println("jointPositions = " + Arrays.toString(jointPositions));
//                                System.out.println("commandedJointPositions = " + Arrays.toString(commandedJointPositions));
//                                System.out.println("goalPose = " + CRCLPosemath.poseToString(goalPose));
//                                System.out.println("curGoalPose = " + CRCLPosemath.poseToString(curGoalPose));
//                                System.out.println("CRCLPosemath.getPose(status) = " + CRCLPosemath.poseToString(CRCLPosemath.getPose(status)));
//                                System.out.println("curGoalPose = " + curGoalPose);
//                                System.out.println("cartDiffMag = " + cartDiffMag);
//                                System.out.println("rotDiffMag = " + rotDiffMag);
//                                switch (robotType) {
//                                    case PLAUSIBLE:
//                                        CRCLPosemath.setPose(status, skPlausible.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
//                                        break;
//
//                                    case SIMPLE:
//                                        CRCLPosemath.setPose(status, skSimple.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
//                                        break;
//                                }
//                                goalPoseToCommandedPositions(curGoalPose);
//                                System.arraycopy(commandedJointPositions, 0, jointPositions, 0,
//                                        jointPositions.length);
//                                curGoalCart = CRCLPosemath.toPmCartesian(curGoalPose.getPoint());
//                                curCart = CRCLPosemath.toPmCartesian(CRCLPosemath.getPoint(status));
//                                cartDiff = curCart.subtract(curGoalCart);
//                                cartDiffMag = cartDiff.mag();
//                                curGoalRotv = CRCLPosemath.toPmRotationVector(curGoalPose);
//                                curRotv = CRCLPosemath.toPmRotationVector(CRCLPosemath.getPose(status));
//                                diffRotv = curRotv.multiply(curGoalRotv.inv());
//                                rotDiffMag = Math.abs(diffRotv.s);
//                                System.out.println("jointmins = " + Arrays.toString(jointmins));
//                                System.out.println("jointmaxs = " + Arrays.toString(jointmaxs));
//                                System.out.println("jointPositions = " + Arrays.toString(jointPositions));
//                                System.out.println("commandedJointPositions = " + Arrays.toString(commandedJointPositions));
//                                System.out.println("goalPose = " + CRCLPosemath.poseToString(goalPose));
//                                System.out.println("curGoalPose = " + CRCLPosemath.poseToString(curGoalPose));
//                                System.out.println("CRCLPosemath.getPose(status) = " + CRCLPosemath.poseToString(CRCLPosemath.getPose(status)));
//                                System.out.println("curGoalPose = " + curGoalPose);
//                                System.out.println("cartDiffMag = " + cartDiffMag);
//                                System.out.println("rotDiffMag = " + rotDiffMag);
//                            }
//                            curGoalPose = null;
//                        }
                    }
                    outer.updatePanels(jointschanged);
                    if (!jointschanged
                            && this.getCommandState() == CommandStateEnumType.CRCL_WORKING
                            && executingMoveCommand) {
                        if (null == goalPose
                                || null == this.waypoints
                                || this.currentWaypoint >= this.waypoints.size()) {
                            setCommandState(CommandStateEnumType.CRCL_DONE);
                            if (outer.isDebugMoveDoneSelected()) {
                                outer.showDebugMessage("SimServerInner DONE move command: " + status.getCommandStatus().getCommandID());
                                outer.showDebugMessage("SimServerInner jointpositions = " + Arrays.toString(jointPositions));
                            }
                            this.setMoveStraight(false);
                            this.setWaypoints(null);
                        } else {
                            goalPose = null;
                        }
                    }
                    cycle_count++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            outer.showMessage(e.getMessage());
            setCommandState(CommandStateEnumType.CRCL_ERROR);
            if (null != commandedJointPositions && null != jointPositions) {
                for (int i = 0; i < jointPositions.length && i < commandedJointPositions.length; i++) {
                    commandedJointPositions[i] = jointPositions[i];
                }
            }
            goalPose = null;
            if (testing) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    private double getJointDiffMax() {

        return jointSpeedMax * delayMillis * 1e-3;
    }

    private double getAllowedJointDiff(int i) {
        if (this.commandedJointVelocities == null
                && this.commandedJointAccellerations == null) {
            return getJointDiffMax();
        }
        double veldiff
                = this.commandedJointVelocities == null
                        ? Double.POSITIVE_INFINITY
                        : this.commandedJointVelocities[i] * delayMillis * 1e-3;
        double accdiff = Double.POSITIVE_INFINITY;
//                = this.commandedJointAccellerations == null
//                        ? Double.POSITIVE_INFINITY
//                        : Math.abs(this.jointVelocites[i] + this.commandedJointAccellerations[i] * delayMillis * 1e-3) * delayMillis * 1e-3;
        return Math.min(getJointDiffMax(), Math.min(accdiff, veldiff));
    }

    /**
     *
     * @param _goalPose the value of _goalPose
     */
    public void goalPoseToCommandedPositions(PoseType _goalPose) {
        switch (robotType) {
            case PLAUSIBLE:
                this.commandedJointPositions = skPlausible.poseToJoints(this.commandedJointPositions, _goalPose);
                break;

            case SIMPLE: {
                try {
                    this.commandedJointPositions = skSimple.poseToJoints(this.commandedJointPositions, _goalPose);
                } catch (PmException ex) {
                    Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            break;
        }
    }

    /**
     * Get the value of currentWaypoint
     *
     * @return the value of currentWaypoint
     */
    public int getCurrentWaypoint() {
        return currentWaypoint;
    }

    /**
     * Set the value of currentWaypoint
     *
     * @param currentWaypoint new value of currentWaypoint
     */
    public void setCurrentWaypoint(int currentWaypoint) {
        this.currentWaypoint = currentWaypoint;
        outer.finishSetCurrentWaypoint(currentWaypoint);
    }

    public void printClientStates(final PrintStream ps) {
        try {
            synchronized (status) {
                ps.println("Start printClientStates");
                ps.println("SimServerInner.this = " + SimServerInner.this);
                if (null != clientStates) {
                    clientStates.forEach(ps::println);
                }
                ps.println("cmdLog=" + this.cmdLog);
                if (null != cmdLog && !cmdLog.isEmpty()) {
                    String cmdString = this.getCheckerCRCLSocket().commandToPrettyString(cmdLog.get(cmdLog.size() - 1));
                    ps.println("SimServerInner : cmdString=" + cmdString);
                }
                String statString = this.getCheckerCRCLSocket().statusToPrettyString(status, false);
                ps.println("SimServerInner : statString=" + statString);
                ps.println("end SimServerInner statString");
                if (null != this.cmdQueue) {
                    ps.println("cmdQueue.peek() = " + cmdQueue.peek());
                }
                this.updateStatus();
                ps.println("End printClientStates");
            }
        } catch (JAXBException jAXBException) {
            LOGGER.log(Level.SEVERE, null, jAXBException);
        }
    }

    public boolean isGripperCommand(CRCLCommandInstanceType cmdInstance) {
        return Optional.ofNullable(cmdInstance)
                .map(CRCLCommandInstanceType::getCRCLCommand)
                .map(CRCLCommandType::getClass)
                .map(gripperCommands::contains)
                .orElse(false);
    }

    private void readCommandsRepeatedly(ClientState state) {
        final int start_close_count = this.close_count;
        final CRCLSocket cs = state.getCs();
        try {
            while (!Thread.currentThread().isInterrupted()) {
                final CRCLCommandInstanceType cmdInstance
                        = cs.readCommand(outer.isValidateXMLSelected());
                LOGGER.log(Level.FINER, () -> "cmdInstance = " + cmdInstance);
                if (null != cmdInstance && null != cmdInstance.getCRCLCommand()) {
                    CRCLCommandType cmd = cmdInstance.getCRCLCommand();
                    LOGGER.log(Level.FINEST, () -> "SimServerInner.readCommandsRepeatedly() : cmd = " + cmd + ", state=" + state);
                    if (cmd instanceof GetStatusType) {
                        state.getStatusRequests++;
                        state.lastStatRequestTime = System.currentTimeMillis();
                        GetStatusType getStatus = (GetStatusType) cmd;
                        if (debug_this_command || outer.isDebugReadCommandSelected() && !getStatus.getCommandID().equals(state.getStatusCmdId)) {
                            outer.showDebugMessage("SimServerInner.readCommandsRepeatedly() :  (getStatus=" + getStatus + " ID=" + getStatus.getCommandID() + ") state = " + state);
                        }
                        state.getStatusCmdId = getStatus.getCommandID();
                        if (enableGetStatusIDCheck && null != state.cmdId
                                && !state.getStatusCmdId.equals(state.cmdId)) {
                            LOGGER.log(Level.SEVERE, "SimServerInner.readCommandsRepeatedly() GetStatusIDCheck failed: state.getStatusCmdId={0}, state.cmdId = {1},status={2}", new Object[]{state.getStatusCmdId, state.cmdId, CRCLSocket.statToDebugString(status)});
                            LOGGER.setLevel(Level.OFF);
                            new Thread(() -> closeServer()).start();
                            return;
                        }
                        synchronized (status) {
                            CommandStatusType cst = status.getCommandStatus();
                            if (null == cst) {
                                cst = new CommandStatusType();
                                setCommandState(CommandStateEnumType.CRCL_WORKING);
                                cst.setCommandID(cmd.getCommandID());
                                cst.setStatusID(BigInteger.ONE);
                                status.setCommandStatus(cst);
//                                cst.setName(cmd.getClass().getName()
//                                        + ((cmd.getName() == null || cmd.getName().length() < 1) ? "" : ":" + cmd.getName()));
                            }
                            SimServerInner.this.sendStatus(cs);
                        }
                        continue;
                    }
                    state.lastCmdInstance = cmdInstance;
                    debug_this_command = false;
                    if (debugCmdSendTime > 0) {
                        long debugCmdRecvTime = System.currentTimeMillis();
                        long diffDebugCmdSend = debugCmdRecvTime - debugCmdSendTime;
                        System.out.println("diffDebugCmdSend = " + diffDebugCmdSend);
                        System.out.println("cmd = " + cmd);
                        System.out.println("cmd.getCommandID() = " + cmd.getCommandID());
                        debugCmdSendTime = 0;
                        debug_this_command = true;
                    }
                    state.cmdsRecieved++;
                    state.lastCmdTime = System.currentTimeMillis();
                    state.cmdId = cmdInstance.getCRCLCommand().getCommandID();
                    if (debug_this_command || outer.isDebugReadCommandSelected()) {
                        outer.showDebugMessage("SimServerInner.readCommandsRepeatedly() : cmdInstance.getCRCLCommand() = " + cmdInstance.getCRCLCommand()
                                + " cmdInstance.getCRCLCommand().getCommandID() = " + cmdInstance.getCRCLCommand().getCommandID() + ", state=" + state);
                    }
                    cmdQueuePutTime = System.currentTimeMillis();
                    SimServerInner.this.cmdQueue.offer(cmdInstance);
                    cmdQueueCmdsOffered++;
                    if (cmdQueueMaxSize < cmdQueue.size()) {
                        cmdQueueMaxSize = cmdQueue.size();
                    }
                    if (isGripperCommand(cmdInstance)) {
                        if (null != gripperSocket && gripperSocket.isConnected()) {
                            gripperCmdQueue.offer(cmdInstance);
                        }
                    }
                }
            }
        } catch (SocketException se) {
            try {
                cs.close();
            } catch (IOException ex1) {
//                LOGGER.log(Level.SEVERE, null, ex1);
            }
            this.clientStates.remove(state);
        } catch (CRCLException ex) {
//            System.err.println("ex.getCause() ="+ex.getCause());
//            if(null != ex.getCause()) {
//                System.err.println("ex.getCause().getCause()="+ex.getCause().getCause());
//            }
//            System.out.println("start_close_count = " + start_close_count);
//            System.out.println("close_count = " + close_count);
            if (ex.getCause() instanceof EOFException) {
                try {
                    cs.close();
                } catch (IOException ex1) {
                    LOGGER.log(Level.SEVERE, null, ex1);
                }
                this.clientStates.remove(state);
                return;
            }
            if (null != ex.getCause() && ex.getCause().getCause() instanceof EOFException) {
                try {
                    cs.close();
                } catch (IOException ex1) {
                    LOGGER.log(Level.SEVERE, null, ex1);
                }
                this.clientStates.remove(state);
                return;
            }
//            System.out.println("start_close_count = " + start_close_count);
//            System.out.println("close_count = " + close_count);
            if (close_count <= start_close_count) {
                System.err.println("String to parse was:" + cs.getLastCommandString());
                LOGGER.log(Level.SEVERE, null, ex);
                this.showMessage(ex.toString() + "\nString to parse was:" + cs.getLastCommandString());
            }
        } catch (IOException ex) {
//            System.out.println("start_close_count = " + start_close_count);
//            System.out.println("close_count = " + close_count);
            if (close_count <= start_close_count) {
                String str = cs.getReadInProgressString();
                if (str.length() == 0) {
                    return;
                }
                LOGGER.log(Level.SEVERE, "ReadInProgressString:{0}", str);
                LOGGER.log(Level.SEVERE, null, ex);
            }
            try {
                cs.close();
            } catch (IOException ex1) {
                LOGGER.log(Level.SEVERE, null, ex1);
            }
            this.clientStates.remove(state);
        }
    }

    public void runAcceptClients() {
        final int start_close_count = this.close_count;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final Socket s = ssock.accept();
                final CrclExiSocket cs = new CrclExiSocket(s);
                final ClientState state = new ClientState(cs);
                cs.setEXIEnabled(outer.isEXISelected());
                clientStates.add(state);
                Thread t = new Thread(() -> readCommandsRepeatedly(state),
                        "client" + s.getInetAddress().toString() + ":" + s.getPort()
                );
                clientThreadMap.put(cs, t);
                t.start();
                this.updateConnectedClients();
            } catch (CRCLException | IOException ex) {
                if (close_count <= start_close_count) {
                    LOGGER.log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public double getCurTransSpeed() {
        return curTransSpeed;
    }

    public void setCurTransSpeed(double curTransSpeed) {
        this.curTransSpeed = curTransSpeed;
    }

    public double getCommandedTransSpeed() {
        return commandedTransSpeed;
    }

    public void setCommandedTransSpeed(double commandedTransSpeed) {
        this.commandedTransSpeed = commandedTransSpeed;
    }

    public double getCurTransAccel() {
        return curTransAccel;
    }

    public void setCurTransAccel(double curTransAccel) {
        this.curTransAccel = curTransAccel;
    }

    public double getCommandedTransAccel() {
        return commandedTransAccel;
    }

    public void setCommandedTransAccel(double commandedTransAccel) {
        this.commandedTransAccel = commandedTransAccel;
    }

    public double getCurRotSpeed() {
        return curRotSpeed;
    }

    public void setCurRotSpeed(double curRotSpeed) {
        this.curRotSpeed = curRotSpeed;
    }

    public double getCommandedRotSpeed() {
        return commandedRotSpeed;
    }

    public void setCommandedRotSpeed(double commandedRotSpeed) {
        this.commandedRotSpeed = commandedRotSpeed;
    }

    public double getCurRotAccel() {
        return curRotAccel;
    }

    public void setCurRotAccel(double curRotAccel) {
        this.curRotAccel = curRotAccel;
    }

    public double getCommandedRotAccel() {
        return commandedRotAccel;
    }

    public void setCommandedRotAccel(double commandedRotAccel) {
        this.commandedRotAccel = commandedRotAccel;
    }

    /**
     * Get the value of delayMillis
     *
     * @return the value of delayMillis
     */
    public long getDelayMillis() {
        return delayMillis;
    }

    /**
     * Set the value of delayMillis
     *
     * @param delay_millis new value of delayMillis
     */
    public void setDelayMillis(long delay_millis) {
        this.delayMillis = delay_millis;
    }

    /**
     * Get the value of angleType
     *
     * @return the value of angleType
     */
    public AngleUnitEnumType getAngleType() {
        return angleType;
    }

    public void setAngleType(AngleUnitEnumType newAngleType) {
        this.angleType = newAngleType;
    }

    /**
     * Get the value of expectedEndPoseTolerance
     *
     * @return the value of expectedEndPoseTolerance
     */
    public PoseToleranceType getExpectedEndPoseTolerance() {
        return expectedEndPoseTolerance;
    }

    /**
     * Set the value of expectedEndPoseTolerance
     *
     * @param expectedEndPoseTolerance new value of expectedEndPoseTolerance
     */
    public void setExpectedEndPoseTolerance(PoseToleranceType expectedEndPoseTolerance) {
        this.expectedEndPoseTolerance = expectedEndPoseTolerance;
    }

    /**
     * Get the value of expectedIntermediatePoseTolerance
     *
     * @return the value of expectedIntermediatePoseTolerance
     */
    public PoseToleranceType getExpectedIntermediatePoseTolerance() {
        return expectedIntermediatePoseTolerance;
    }

    /**
     * Set the value of expectedIntermediatePoseTolerance
     *
     * @param expectedIntermediatePoseToleranceType new value of
     * expectedIntermediatePoseTolerance
     */
    public void setExpectedIntermediatePoseTolerance(PoseToleranceType expectedIntermediatePoseToleranceType) {
        this.expectedIntermediatePoseTolerance = expectedIntermediatePoseToleranceType;
    }

    public void updateConnectedClients() {
        outer.updateConnectedClients(Math.max(clientStates.size(), clientThreadMap.size()));
    }

    private void readCommand() throws ParserConfigurationException, IOException, SAXException {
        if (dwellEndTime > 0 && System.currentTimeMillis() < dwellEndTime) {
//            System.out.println("Dwelling");
//            System.out.println("dwellEndTime = " + dwellEndTime);
//            System.out.println("System.currentTimeMillis() = " + System.currentTimeMillis());
//            System.out.println("(dwellEndTime - System.currentTimeMillis()) = " + (dwellEndTime - System.currentTimeMillis()));
//            System.out.println("getCommandState() = " + getCommandState());
//            System.out.println("status.getCommandStatus().getCommandID() = " + status.getCommandStatus().getCommandID());
            return;
        }
        if (dwellEndTime > 0) {
            setCommandState(CommandStateEnumType.CRCL_DONE);
            dwellEndTime = 0;
            return;
        }
        if (cmdQueue.size() > cmdQueueMaxSize) {
            cmdQueueMaxSize = cmdQueue.size();
            System.out.println("cmdQueueMaxSize = " + cmdQueueMaxSize);
        }
        long cmdQueuePollStart = System.currentTimeMillis();
        CRCLCommandInstanceType instance = cmdQueue.poll();
        long cmdQueuePollEnd = System.currentTimeMillis();
        cmdQueuePollReturnCount++;
        if (instance != null) {
            cmdQueuePollReturnNonNullCount++;
        }
        long cmdQueuePollTime = cmdQueuePollEnd - cmdQueuePollStart;
        if (debug_this_command) {
            System.out.println("cmdQueuePollTime = " + cmdQueuePollTime);
        }
        if (cmdQueuePollTime > maxCmdQueuePollTime) {
            maxCmdQueuePollTime = cmdQueuePollTime;
        }
        int cmdsInCycle = 0;
        while (null != instance) {
            lastReadCommandInstance = instance;
            cmdsInCycle++;
            if (debug_this_command) {
                System.out.println("cmdsInCycle = " + cmdsInCycle);
            }
            if (cmdQueuePutTime > 0 && cmdQueue.isEmpty()) {
                long cmdQueueEmptyTime = System.currentTimeMillis();
                long diffCmdQueuePutEmpty = cmdQueueEmptyTime - cmdQueuePutTime;
                cmdQueuePutTime = 0;
                if (diffCmdQueuePutEmpty > maxDiffCmdQueuePutEmpty) {
                    maxDiffCmdQueuePutEmpty = diffCmdQueuePutEmpty;
                }
            }
            CRCLCommandType cmd = instance.getCRCLCommand();
            if (null == cmd) {
                System.err.println("cmd is null");
                return;
            }
            if (debug_this_command || outer.isDebugReadCommandSelected()) {
                outer.showDebugMessage("SimServerInner.readCommand() : cmd = " + cmd
                        + " cmd.getCommandID() = " + cmd.getCommandID());
            }
            if (null == cmdLog) {
                cmdLog = new ArrayList<>();
            }
            cmdLog.add(cmd);
            String cmdName = getCheckerCRCLSocket().commandToSimpleString(cmd);
            outer.updateCurrentCommandType(cmdName);
            synchronized (status) {
                CommandStatusType cst = status.getCommandStatus();
                if (null == cst) {
                    cst = new CommandStatusType();
                    status.setCommandStatus(cst);
                }
                if (getCommandState() == CommandStateEnumType.CRCL_DONE) {
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                }
            }
            executingMoveCommand = false;

//            System.out.println("cmd = " + cmd);
//            System.out.println("cmd.getCommandID() = " + cmd.getCommandID());
            if (cmd instanceof InitCanonType) {
                InitCanonType init = (InitCanonType) cmd;
                initialize();
            } else {
                if (this.getCommandState() == CommandStateEnumType.CRCL_DONE) {
                    this.setWaypoints(null);
                }
                if (!outer.isInitializedSelected()) {
                    setCommandState(CommandStateEnumType.CRCL_ERROR);
                    showMessage("Command other than init recieved when not initialized.");
                    return;
                }
                if (cmd instanceof SetEndEffectorType) {
                    SetEndEffectorType seet = (SetEndEffectorType) cmd;
                    outer.updateEndEffector(seet.getSetting().toString());
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof CloseToolChangerType) {
                    CloseToolChangerType ctc = (CloseToolChangerType) cmd;
                    outer.updateToolChangerIsOpen(false);
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof OpenToolChangerType) {
                    OpenToolChangerType otc = (OpenToolChangerType) cmd;
                    outer.updateToolChangerIsOpen(true);
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof MessageType) {
                    MessageType mt = (MessageType) cmd;
                    this.showMessage("MESSAGE: " + mt.getMessage() + "\n");
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof ConfigureJointReportsType) {
                    cjrs = (ConfigureJointReportsType) cmd;
                    if (cjrs.isResetAll() || null == this.cjrMap) {
                        this.cjrMap = new HashMap<>();
                    }
                    for (ConfigureJointReportType cjr : cjrs.getConfigureJointReport()) {
                        this.cjrMap.put(cjr.getJointNumber().intValue(),
                                cjr);
                    }
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                } else if (cmd instanceof SetLengthUnitsType) {
                    SetLengthUnitsType slu = (SetLengthUnitsType) cmd;
                    LengthUnitEnumType lu = slu.getUnitName();
                    setLengthUnit(lu);
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetTransSpeedType) {
                    SetTransSpeedType sts = (SetTransSpeedType) cmd;
                    TransSpeedType ts = sts.getTransSpeed();
                    if (ts instanceof TransSpeedAbsoluteType) {
                        TransSpeedAbsoluteType tsa = (TransSpeedAbsoluteType) ts;
                        this.setCommandedTransSpeed(tsa.getSetting().doubleValue());
                    } else if (ts instanceof TransSpeedRelativeType) {
                        TransSpeedRelativeType tsr = (TransSpeedRelativeType) ts;
                        this.setCommandedTransSpeed(tsr.getFraction().doubleValue() * maxTransSpeed);
                    } else {
                        outer.showMessage("Unrecognized type of TransSpeed in SetTransSpeedType");
                        setCommandState(CommandStateEnumType.CRCL_ERROR);
                        return;
                    }
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetTransAccelType) {
                    SetTransAccelType sts = (SetTransAccelType) cmd;
                    TransAccelType ts = sts.getTransAccel();
                    if (ts instanceof TransAccelAbsoluteType) {
                        TransAccelAbsoluteType taa = (TransAccelAbsoluteType) ts;
                        this.setCommandedTransAccel(taa.getSetting().doubleValue());
                    } else if (ts instanceof TransAccelRelativeType) {
                        TransAccelRelativeType tar = (TransAccelRelativeType) ts;
                        this.setCommandedTransAccel(tar.getFraction().doubleValue() * maxTransAccel);
                    } else {
                        outer.showMessage("Unrecognized type of TransAccel in SetTransAccelType");
                        setCommandState(CommandStateEnumType.CRCL_ERROR);
                        return;
                    }
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetRotSpeedType) {
                    SetRotSpeedType sts = (SetRotSpeedType) cmd;
                    RotSpeedType ts = sts.getRotSpeed();
                    if (ts instanceof RotSpeedAbsoluteType) {
                        RotSpeedAbsoluteType tsa = (RotSpeedAbsoluteType) ts;
                        this.setCommandedRotSpeed(tsa.getSetting().doubleValue());
                    } else if (ts instanceof RotSpeedRelativeType) {
                        RotSpeedRelativeType tsr = (RotSpeedRelativeType) ts;
                        this.setCommandedRotSpeed(tsr.getFraction().doubleValue() * maxRotSpeed);
                    } else {
                        outer.showMessage("Unrecognized type of RotSpeed in SetRotSpeedType");
                        setCommandState(CommandStateEnumType.CRCL_ERROR);
                        return;
                    }
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetRotAccelType) {
                    SetRotAccelType sts = (SetRotAccelType) cmd;
                    RotAccelType ts = sts.getRotAccel();
                    if (ts instanceof RotAccelAbsoluteType) {
                        RotAccelAbsoluteType taa = (RotAccelAbsoluteType) ts;
                        this.setCommandedRotAccel(taa.getSetting().doubleValue());
                    } else if (ts instanceof RotAccelRelativeType) {
                        RotAccelRelativeType tar = (RotAccelRelativeType) ts;
                        this.setCommandedRotAccel(tar.getFraction().doubleValue() * maxRotAccel);
                    } else {
                        outer.showMessage("Unrecognized type of RotAccel in SetRotAccelType");
                        setCommandState(CommandStateEnumType.CRCL_ERROR);
                        return;
                    }
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof EndCanonType) {
                    EndCanonType end = (EndCanonType) cmd;
                    setCommandState(CommandStateEnumType.CRCL_DONE);
//                    System.out.println("END");
                    outer.updateIsInitialized(false);
                    this.setWaypoints(null);
                    this.setGoalPose(null);
                    this.commandedJointPositions = Arrays.copyOf(jointPositions, jointPositions.length);
                } else if (cmd instanceof MoveThroughToType) {
                    this.executingMoveCommand = true;
                    MoveThroughToType mv = (MoveThroughToType) cmd;
//                    System.out.println("MoveThroughToType");
//                    System.out.println("mv.getNumPositions() = " + mv.getNumPositions());
//                    System.out.println("mv.isMoveStraight() = " + mv.isMoveStraight());
                    List<PoseType> wpts = mv.getWaypoint();
                    int numpositions = mv.getNumPositions().intValue();
                    if (numpositions < 2) {
                        throw new RuntimeException("MoveThroughToType must set NumPositions to at-least 2 but NumPositions=" + numpositions + ".");
                    }
                    if (wpts.size() < 2) {
                        throw new RuntimeException("MoveThroughToType must have at-least two waypoints but " + wpts.size() + " were given.");
                    }
                    if (wpts.size() != numpositions) {
                        throw new RuntimeException("MoveThroughToType has NumPositions=" + numpositions + " but " + wpts.size() + " waypoints.");
                    }
                    this.setWaypoints(wpts);
//                    System.out.println("wpts = " + wpts);
                    if (null != wpts) {
                        for (PoseType pose : wpts) {
                            checkPose(pose);
                        }
                    }
                    this.setCommandState(CommandStateEnumType.CRCL_WORKING);
                    this.setCurrentWaypoint(0);
                    this.setGoalPose(wpts.get(0));

                    this.commandedJointAccellerations = null;
                    this.commandedJointVelocities = null;
                    this.commandedJointPositions = null;
//                } else if (cmd instanceof SetJointControlModesType) {
//                    SetJointControlModesType setjcm = (SetJointControlModesType) cmd;
//                    List<JointControlModeType> ljcm = setjcm.getJointControlMode();
//                    for (JointControlModeType jcm : ljcm) {
//                        int index = jcm.getJointNumber().intValue();
//                        if (index < 0 || index > this.controlmodes.length) {
//                            setCommandState(CommandStateEnumType.CRCL_ERROR);
//                            Message("Bad joint index:" + index);
//                            break;
//                        }
//                        this.controlmodes[index] = jcm.getMode();
//                    }
                } else if (cmd instanceof ActuateJointsType) {
                    this.executingMoveCommand = true;
                    ActuateJointsType ajst = (ActuateJointsType) cmd;
                    this.goalPose = null;
                    List<ActuateJointType> ajl = ajst.getActuateJoint();
                    for (ActuateJointType aj : ajl) {
                        int index = aj.getJointNumber().intValue() - 1;
                        if (index < 0 || index > this.jointPositions.length) {
                            setCommandState(CommandStateEnumType.CRCL_ERROR);
                            showMessage("Bad joint index:" + index);
                            break;
                        }
                        if (index >= 0 && index < this.jointPositions.length) {
                            this.commandedJointPositions[index] = aj.getJointPosition().doubleValue();
                        }
                        JointDetailsType jd = aj.getJointDetails();
                        if (jd instanceof JointSpeedAccelType) {
                            JointSpeedAccelType jsa = (JointSpeedAccelType) jd;
                            BigDecimal vel = jsa.getJointSpeed();
                            if (null != vel) {
                                if (null == this.commandedJointVelocities) {
                                    this.commandedJointVelocities = new double[this.commandedJointPositions.length];
                                    Arrays.setAll(this.commandedJointVelocities, i -> Double.POSITIVE_INFINITY);
                                }
                                this.commandedJointVelocities[index] = vel.doubleValue();
                            }
                            BigDecimal acc = jsa.getJointAccel();
                            if (null != acc) {
                                if (null == this.commandedJointAccellerations) {
                                    this.commandedJointAccellerations = new double[this.commandedJointPositions.length];
                                    Arrays.setAll(this.commandedJointAccellerations, i -> Double.POSITIVE_INFINITY);
                                }
                                this.commandedJointAccellerations[index] = acc.doubleValue();
                            }
                        }
                    }
                    if (debug_this_command || outer.isDebugReadCommandSelected()) {
                        outer.showDebugMessage("SimServer commandedJointPositions = " + Arrays.toString(commandedJointPositions));
                    }
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                    outer.updatePanels(true);
                } else if (cmd instanceof MoveToType) {
                    this.executingMoveCommand = true;
                    MoveToType moveto = (MoveToType) cmd;
                    this.setGoalPose(moveto.getEndPosition());
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                    this.setMoveStraight(moveto.isMoveStraight());
                    this.setCurrentWaypoint(0);
                    outer.updatePanels(true);
                    this.commandedJointAccellerations = null;
                    this.commandedJointVelocities = null;
                    this.commandedJointPositions = null;
                } else if (cmd instanceof StopMotionType) {
                    this.executingMoveCommand = true;
                    StopMotionType stop = (StopMotionType) cmd;
                    this.setGoalPose(null);
                    this.setWaypoints(null);
                    if (null != this.jointPositions && null != this.commandedJointPositions) {
                        System.arraycopy(this.jointPositions, 0, this.commandedJointPositions, 0,
                                Math.min(this.jointPositions.length, this.commandedJointPositions.length));
                    }
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetAngleUnitsType) {
                    SetAngleUnitsType setAngle = (SetAngleUnitsType) cmd;
                    this.setAngleType(setAngle.getUnitName());
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetEndPoseToleranceType) {
                    SetEndPoseToleranceType endPoseTol = (SetEndPoseToleranceType) cmd;
                    this.setExpectedEndPoseTolerance(endPoseTol.getTolerance());
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof SetIntermediatePoseToleranceType) {
                    SetIntermediatePoseToleranceType intermediatePoseTol = (SetIntermediatePoseToleranceType) cmd;
                    this.setExpectedIntermediatePoseTolerance(intermediatePoseTol.getTolerance());
                    setCommandState(CommandStateEnumType.CRCL_DONE);
                } else if (cmd instanceof DwellType) {
                    DwellType dwellCmd = (DwellType) cmd;
                    dwellEndTime = System.currentTimeMillis() + dwellCmd.getDwellTime().longValue() * 1000;
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                } else if (cmd instanceof MoveScrewType) {
                    MoveScrewType moveScrew = (MoveScrewType) cmd;
                    setCommandState(CommandStateEnumType.CRCL_WORKING);
                    this.multiStepCommand = moveScrew;
                    this.moveScrewStep = 0;
                } else {
                    setCommandState(CommandStateEnumType.CRCL_DONE);
//                    System.err.println("Ignored type: " + cmd.getClass().getSimpleName());
                    outer.showDebugMessage("\nIgnored type: " + cmd.getClass().getSimpleName() + "\n");
//                    showMessage("Unrecognized command type: " + cmd.getClass().getSimpleName());
                }
            }
            synchronized (status) {
                status.getCommandStatus().setCommandID(cmd.getCommandID());
//                status.getCommandStatus().setName(cmd.getClass().getName()
//                        + ((cmd.getName() == null || cmd.getName().length() < 1) ? "" : ":" + cmd.getName()));

            }
            if (cmdQueue.size() > cmdQueueMaxSize) {
                cmdQueueMaxSize = cmdQueue.size();
            }
            cmdQueuePollStart = System.currentTimeMillis();
            instance = cmdQueue.poll();
            cmdQueuePollEnd = System.currentTimeMillis();
            cmdQueuePollReturnCount++;
            if (instance != null) {
                cmdQueuePollReturnNonNullCount++;
            }
            cmdQueuePollTime = cmdQueuePollEnd - cmdQueuePollStart;
            if (debug_this_command) {
                System.out.println("cmdQueuePollTime = " + cmdQueuePollTime);
            }
            if (cmdQueuePollTime > maxCmdQueuePollTime) {
                maxCmdQueuePollTime = cmdQueuePollTime;
            }
        }
        if (cmdQueuePutTime > 0) {
            long cmdQueueEmptyTime = System.currentTimeMillis();
            long diffCmdQueuePutEmpty = cmdQueueEmptyTime - cmdQueuePutTime;
            cmdQueuePutTime = 0;
            if (diffCmdQueuePutEmpty > maxDiffCmdQueuePutEmpty) {
                maxDiffCmdQueuePutEmpty = diffCmdQueuePutEmpty;
            }
        }
    }

    public void initialize() {
        setCommandState(CommandStateEnumType.CRCL_DONE);
        outer.updateIsInitialized(true);
//                System.out.println("INIT");
        this.setWaypoints(null);
        this.setGoalPose(null);
        this.commandedJointPositions = Arrays.copyOf(jointPositions, jointPositions.length);
    }

    public void setLengthUnit(LengthUnitEnumType lu) {
        try {
            outer.updateLengthUnit(lu);
            double oldLengthScale = lengthScale;
            switch (lu) {
                case METER:
                    lengthScale = 0.01 * SCALE_FUDGE_FACTOR;
                    break;

                case INCH:
                    lengthScale = 0.393701 * SCALE_FUDGE_FACTOR;
                    break;

                case MILLIMETER:
                    lengthScale = 10.0 * SCALE_FUDGE_FACTOR;
                    break;
            }

            switch (robotType) {
                case PLAUSIBLE:
                    this.skPlausible.setScale(lengthScale);
                    break;

                case SIMPLE:
                    this.skSimple.setScale(lengthScale);
                    break;
            }
            switch (robotType) {
                case PLAUSIBLE:
                    CRCLPosemath.setPose(status, skPlausible.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
                    break;

                case SIMPLE:
                    CRCLPosemath.setPose(status, skSimple.jointsToPose(jointPositions, CRCLPosemath.getPose(status)));
                    break;
            }
            this.setCommandedTransAccel(commandedTransAccel * lengthScale / oldLengthScale);
            this.setCommandedTransSpeed(commandedTransSpeed * lengthScale / oldLengthScale);
            this.lengthUnit = lu;
        } catch (PmException ex) {
            Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public LengthUnitEnumType getLengthUnit() {
        return this.lengthUnit;
    }

    /**
     * Get the value of waypoints
     *
     * @return the value of waypoints
     */
    public List<PoseType> getWaypoints() {
        return Collections.unmodifiableList(waypoints);
    }

    /**
     * Set the value of waypoints
     *
     * @param waypoints new value of waypoints
     */
    public void setWaypoints(List<PoseType> waypoints) {
        this.waypoints = waypoints;
        if (null != waypoints) {
            outer.updateNumWaypoints(waypoints.size());
        } else {
            outer.updateNumWaypoints(0);
            this.setCurrentWaypoint(0);
        }
    }

    public void restartServer() {
        try {
            closeServer();
            ssock = new ServerSocket(port);
            if (port == 0) {
                // This is a hack so the integration test can be run on a port 
                // found automatically with the client run in the same 
                // process.
                // For this test only force port to zero then it will be bound
                // to a free port which gets passed back to the client with a system property.
                this.port = ssock.getLocalPort();
                System.setProperty("crcljava.port", Integer.toString(port));
            }
            ssock.setReuseAddress(true);
            acceptClientsThread = new Thread(this::runAcceptClients,
                    "acceptClientsThread");
            acceptClientsThread.start();
            final int start_close_count = this.close_count;
            maxReadCommandTime = 0;
            maxUpdateStatusTime = 0;
            maxSimCycleTime = 0;
            simCycleCount = 0;

            simThread = new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            simCycleCount++;
                            long cycleStartTime = System.currentTimeMillis();
                            Thread.sleep(delayMillis);
                            long startCommandReadTime = System.currentTimeMillis();
                            if (!handleMultiStepCommand()) {
                                readCommand();
                            }
                            long endCommandReadTime = System.currentTimeMillis();
                            long commandReadTime = endCommandReadTime - startCommandReadTime;
                            if (debug_this_command) {
                                System.out.println("commandReadTime = " + commandReadTime);
                            }
                            if (commandReadTime > maxReadCommandTime) {
                                maxReadCommandTime = commandReadTime;
                            }
                            if (!updateStatus()) {
                                sendStatus(null);
                            }

                            long endCycleTime = System.currentTimeMillis();
                            long statusUpdateTime = endCycleTime - endCommandReadTime;
                            if (debug_this_command) {
                                System.out.println("statusUpdateTime = " + statusUpdateTime);
                            }
                            if (statusUpdateTime > maxUpdateStatusTime) {
                                maxUpdateStatusTime = statusUpdateTime;
                            }
                            if (debug_this_command) {
                                System.out.println("simCycleCount = " + simCycleCount);
                            }
                            long cycleTime = endCycleTime - cycleStartTime;
                            if (debug_this_command) {
                                System.out.println("cycleTime = " + cycleTime);
                            }
                            if (cycleTime > maxSimCycleTime) {
                                maxSimCycleTime = cycleTime;
                            }
                        }
                    } catch (InterruptedException ex) {
                        if (SimServerInner.this.close_count <= start_close_count) {
                            LOGGER.log(Level.SEVERE, null, ex);
                        }
                    } catch (ParserConfigurationException | IOException | SAXException ex) {
                        Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }, "simThread");
            simThread.start();
            SimServerInner.runningServers.add(this);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            showMessage("Can not start server on port " + port + " : " + ex.getMessage());
        }
    }

    public CRCLStatusType getStatus() {
        return status;
    }

    public String getStatusXmlString() throws JAXBException {
        return this.getCheckerCRCLSocket().statusToPrettyString(this.getStatus(), false);
    }

    public CRCLSocket getCheckerCRCLSocket() {
        if (null == checkerCRCLSocket) {
            try {
                checkerCRCLSocket = new CRCLSocket();
            } catch (CRCLException ex) {
                Logger.getLogger(SimServerInner.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return checkerCRCLSocket;
    }

    private static class LastStatusInfo {

        BigInteger lastSentCid = null;
        CommandStateEnumType lastSentState = null;
    }

    private static class ClientState implements AutoCloseable {

        private final CRCLSocket cs;
        public int getStatusRequests = 0;
        public int cmdsRecieved = 0;
        public long lastCmdTime = 0;
        public long lastStatRequestTime = 0;
        BigInteger getStatusCmdId = null;
        BigInteger cmdId = null;
        CRCLCommandInstanceType lastCmdInstance;

        ClientState(CRCLSocket cs) {
            this.cs = cs;
        }

        @Override
        public void close() throws Exception {
            cs.close();
        }

        public CRCLSocket getCs() {
            return cs;
        }

        @Override
        public String toString() {
            return "ClientState{" + "cs=" + cs + ", getStatusRequests=" + getStatusRequests + ", cmdsRecieved=" + cmdsRecieved + ", lastCmdTime=" + lastCmdTime + ", lastStatRequestTime=" + lastStatRequestTime + ", getStatusCmdId=" + getStatusCmdId + ", cmdId=" + cmdId + '}';
        }
    }

}
