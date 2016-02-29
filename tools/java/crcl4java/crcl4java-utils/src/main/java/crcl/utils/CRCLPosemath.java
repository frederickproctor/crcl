/*
 * This is public domain software, however it is preferred
 * that the following disclaimers be attached.
 * 
 * Software Copywrite/Warranty Disclaimer
 * 
 * This software was developed at the National Institute of Standards and
 * Technology by employees of the Federal Government in the course of their
 * official duties. Pursuant to title 17 Section 105 of the United States
 * Code this software is not subject to copyright protection and is in the
 * public domain. This software is experimental.
 * NIST assumes no responsibility whatsoever for its use by other
 * parties, and makes no guarantees, expressed or implied, about its
 * quality, reliability, or any other characteristic. We would appreciate
 * acknowledgment if the software is used. This software can be
 * redistributed and/or modified freely provided that any derivative works
 * bear some notice that they are derived from it, and any modified
 * versions bear some notice that they have been modified.
 * 
 */
package crcl.utils;


import crcl.base.CRCLStatusType;
import crcl.base.DataThingType;
import crcl.base.PointType;
import crcl.base.PoseStatusType;
import crcl.base.PoseType;
import crcl.base.PoseToleranceType;
import crcl.base.VectorType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.logging.Logger;
import rcs.posemath.PmCartesian;
import rcs.posemath.PmException;
import rcs.posemath.PmPose;
import rcs.posemath.PmRotationMatrix;
import rcs.posemath.PmRotationVector;
import rcs.posemath.PmRpy;
import rcs.posemath.Posemath;
import rcs.posemath.PmQuaternion;


/*
 * 
 * NOTE: Comments beginning with {@literal @} or {@literal >>>} are used by Checker Framework Comments
 * beginning with {@literal @} must have no spaces in the comment or Checker will ignore
 * it.
 *
 * See http://types.cs.washington.edu/checker-framework for null pointer
 * checks. This file can be compiled without the Checker Framework, but using
 * the framework allows potential NullPointerExceptions to be found.
 */

/*>>>
import org.checkerframework.checker.nullness.qual.*;
 */

public class CRCLPosemath {

    private CRCLPosemath() {
        // never to be called.
    }
    
    /*@Nullable*/
    public static PoseType getPose(/*@Nullable*/CRCLStatusType stat) {
        if(stat != null) {
            PoseStatusType poseStatus = stat.getPoseStatus();
            if(null != poseStatus) {
                return poseStatus.getPose();
            }
        }
        return null;
    }
    
    /*@Nullable*/
    public static PointType getPoint(/*@Nullable*/CRCLStatusType stat) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(pose != null) {
                return pose.getPoint();
            }
        }
        return null;
    }
    
    /*@Nullable*/
    public static VectorType getXAxis(/*@Nullable*/CRCLStatusType stat) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(pose != null) {
                return pose.getXAxis();
            }
        }
        return null;
    }
    
    /*@Nullable*/
    public static VectorType getZAxis(/*@Nullable*/CRCLStatusType stat) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(pose != null) {
                return pose.getZAxis();
            }
        }
        return null;
    }
    
    public static void setPose(/*@Nullable*/CRCLStatusType stat, PoseType pose) {
        if(null != stat) {
            PoseStatusType poseStatus = stat.getPoseStatus();
            if(null != poseStatus) {
                poseStatus.setPose(pose);
            } else {
                PoseStatusType newPoseStatus = new PoseStatusType();
                newPoseStatus.setPose(pose);
                stat.setPoseStatus(newPoseStatus);
            }
        }
    }
    
    public static void setPoint(/*@Nullable*/CRCLStatusType stat, PointType pt) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(null != pose) {
                pose.setPoint(pt);
            } else {
                PoseType newPose = new PoseType();
                newPose.setPoint(pt);
                setPose(stat,newPose);
            }
        }
    }
    
    public static void setXAxis(/*@Nullable*/CRCLStatusType stat, VectorType xAxis) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(null != pose) {
                pose.setXAxis(xAxis);
            } else {
                PoseType newPose = new PoseType();
                newPose.setXAxis(xAxis);
                setPose(stat,newPose);
            }
        }
    }
    
    public static void setZAxis(/*@Nullable*/CRCLStatusType stat, VectorType zAxis) {
        if(stat != null) {
            PoseType pose = getPose(stat);
            if(null != pose) {
                pose.setZAxis(zAxis);
            }  else {
                PoseType newPose = new PoseType();
                newPose.setZAxis(zAxis);
                setPose(stat,newPose);
            }
        }
    }
    
    public static void initPose(CRCLStatusType status) {
        if(null == CRCLPosemath.getPose(status)) {
            CRCLPosemath.setPose(status, new PoseType());
        }
        if (null == CRCLPosemath.getPoint(status)) {
            CRCLPosemath.setPoint(status,new PointType());
        }
        if (null == CRCLPosemath.getXAxis(status)) {
            CRCLPosemath.setXAxis(status, new VectorType());
        }
        if (null == CRCLPosemath.getZAxis(status)) {
            CRCLPosemath.setZAxis(status,new VectorType());
        }
    }
    
    private static String toString(BigInteger bi) {
        if(null == bi) {
            return "null";
        }
        return bi.toString();
    }
    
    private static String toString(BigDecimal bd) {
        if(null == bd) {
            return "null";
        }
        return bd.toString();
    }
    
    private static String dataTypeThingToStartString(DataThingType dtt) {
        return "{"
                + ((dtt.getName() != null)?"name="+dtt.getName()+",":"");
    }
    
    public static String toString(PointType pt) {
        if(null == pt) {
            return "null";
        }
        return dataTypeThingToStartString(pt)
                + "x="+toString(pt.getX())+","
                + "y="+toString(pt.getY())+","
                +"z="+toString(pt.getZ())
                +"}";
    }
    
    public static String toString(VectorType v) {
        if(null == v) {
            return "null";
        }
        return dataTypeThingToStartString(v)
                + "i="+toString(v.getI())+","
                + "j="+toString(v.getJ())+","
                +"k="+toString(v.getK())
                +"}";
    }
    
    public static String toString(PoseType pose) {
        if(null == pose) {
            return "null";
        }
        return dataTypeThingToStartString(pose)
                + "pt="+toString(pose.getPoint())+","
                + "xAxis=" +toString(pose.getXAxis())
                + "zAxis="+toString(pose.getZAxis()) 
                +"}";
    }
    
    public static String toString(PoseToleranceType posetol) {
        if(null == posetol) {
            return "null";
        }
        return dataTypeThingToStartString(posetol)
                + "XPointTolerance="+toString(posetol.getXPointTolerance())+","
                + "YPointTolerance="+toString(posetol.getYPointTolerance())+","
                + "ZPointTolerance="+toString(posetol.getZPointTolerance())+","
                + "XAxisTolerance="+toString(posetol.getXAxisTolerance())+","
                + "ZAxisTolerance="+toString(posetol.getZAxisTolerance())+","
                +"}";
    }


    /**
     * Convert crcl.PointType to rcs.posemath.PmCartesian
     *
     * @param pt Point to be converted
     * @return PmCartesian equivalent
     */
    public static PmCartesian pointToPmCartesian(final PointType pt) {
        return new PmCartesian(
                pt.getX().doubleValue(),
                pt.getY().doubleValue(),
                pt.getZ().doubleValue());
    }

    public static PoseType identityPose() {
        PoseType newPose = new PoseType();
        PointType pt = new PointType();
        pt.setX(BigDecimal.ZERO);
        pt.setY(BigDecimal.ZERO);
        pt.setZ(BigDecimal.ZERO);
        newPose.setPoint(pt);
        VectorType xAxis = new VectorType();
        xAxis.setI(BigDecimal.ONE);
        xAxis.setJ(BigDecimal.ZERO);
        xAxis.setK(BigDecimal.ZERO);
        newPose.setXAxis(xAxis);
        VectorType zAxis = new VectorType();
        zAxis.setI(BigDecimal.ZERO);
        zAxis.setJ(BigDecimal.ZERO);
        zAxis.setK(BigDecimal.ONE);
        newPose.setZAxis(zAxis);
        return newPose;
    }

    public static String poseToString(PoseType pose) throws PmException {
        PmRotationMatrix rmat = toPmRotationMatrix(pose);
        PmCartesian cart = pointToPmCartesian(pose.getPoint());
        return String.format("{\n{%.3g,%.3g,%.3g,%.3g},\n{%.3g,%.3g,%.3g,%.3g},\n{%.3g,%.3g,%.3g,%.3g},\n{%.3g,%.3g,%.3g,%.3g}\n}",
                rmat.x.x, rmat.x.y, rmat.x.z, cart.x,
                rmat.y.x, rmat.y.y, rmat.y.z, cart.y,
                rmat.z.x, rmat.z.y, rmat.z.z, cart.z,
                0.0, 0.0, 0.0, 1.0);
    }

    public static PointType add(PointType p1, PointType p2) {
        PointType sum = new PointType();
        sum.setX(p1.getX().add(p2.getX()));
        sum.setY(p1.getY().add(p2.getY()));
        sum.setZ(p1.getZ().add(p2.getZ()));
        return sum;
    }

    public static PointType subtract(PointType p1, PointType p2) {
        PointType sum = new PointType();
        sum.setX(p1.getX().subtract(p2.getX()));
        sum.setY(p1.getY().subtract(p2.getY()));
        sum.setZ(p1.getZ().subtract(p2.getZ()));
        return sum;
    }
    
    public static PmPose toPmPose(CRCLStatusType stat) throws PmException {
        if(stat == null) {
            throw new IllegalArgumentException("Can not convert null status to PmPose");
        }
        PoseType pose = getPose(stat);
        if(pose != null) {
            PointType pt = getPoint(stat);
            if(null != pt) {
                PmCartesian cart = pointToPmCartesian(pt);
                PmRotationMatrix mat = toPmRotationMatrix(pose);
                if(null != mat) {
                    return new PmPose(cart,new PmQuaternion(mat));
                }
            }
        }
        throw new RuntimeException("stat has null pose components");
    }

    public static PmPose toPmPose(PoseType p) throws PmException {
        return new PmPose(pointToPmCartesian(p.getPoint()),
                Posemath.toQuat(toPmRotationMatrix(p)));
    }

    public static PointType multiply(final BigDecimal dist, final VectorType v) {
        PointType out = new PointType();
        out.setX(v.getI().multiply(dist));
        out.setY(v.getJ().multiply(dist));
        out.setZ(v.getK().multiply(dist));
        return out;
    }

    public static PointType multiply(double dist, VectorType v) {
        return multiply(BigDecimal.valueOf(dist), v);
    }

    public static PointType multiply(BigDecimal dist, PointType p) {
        PointType out = new PointType();
        out.setX(p.getX().multiply(dist));
        out.setY(p.getY().multiply(dist));
        out.setZ(p.getZ().multiply(dist));
        return out;
    }

    public static PointType multiply(double dist, PointType p) {
        return multiply(BigDecimal.valueOf(dist), p);
    }

    public static BigDecimal dot(VectorType v1, VectorType v2) {
        return v1.getI().multiply(v2.getI())
                .add(v1.getJ().multiply(v2.getJ()))
                .add(v1.getK().multiply(v2.getK()));
    }

    public static BigDecimal dot(VectorType v1, PointType p2) {
        return v1.getI().multiply(p2.getX())
                .add(v1.getJ().multiply(p2.getY()))
                .add(v1.getK().multiply(p2.getZ()));
    }

    /**
     * Generate L2 norm of Vector
     * WARNING: This function loses the BigDecimal precision and range in VectorType
     * @param v1 vector to compute norm of
     * @return norm of input vector
     */
    public static double norm(VectorType v1) {
        // FIXME(maybe?) It is difficult to take sqrt(BigDecimal) so
        // I punted and just hope double precision is good enough.
        double i = v1.getI().doubleValue();
        double j = v1.getJ().doubleValue();
        double k = v1.getK().doubleValue();
        return Math.sqrt(i*i + j*j + k*k);
    }
    
    public static class ZeroNormException extends RuntimeException {
        public ZeroNormException(String message) {
            super(message);
        }
    }
    
    /**
     * Normalize the vector so its L2 Norm is 1
     * WARNING: This function uses norm()
     * which loses the BigDecimal precision and range in VectorType
     * @param v vector to compute norm of
     * @return normalized input vector
     * @throws  ZeroNormException if norm(v) less than Double.MIN_VALUE
     */
    public static VectorType normalize(VectorType v) {
        VectorType vout  = new VectorType();
        double normv = norm(v);
        if(normv < Double.MIN_VALUE) {
            throw new ZeroNormException("Can't normalize vector with zero magnitude.");
        }
        BigDecimal normInv = BigDecimal.ONE.divide(BigDecimal.valueOf(norm(v)));
        vout.setI(v.getI().multiply(normInv));
        vout.setJ(v.getJ().multiply(normInv));
        vout.setK(v.getK().multiply(normInv));
        return vout;
    }
    
    /**
     * Compute cross product of two Vectors
     * WARNING: The output may not be normalized even if the input vectors are.
     * @param v1 first vector
     * @param v2 second vector
     * @return cross product vector
     */
    public static VectorType cross(VectorType v1, VectorType v2) {
        VectorType vout = new VectorType();
//        vout.x = v1.y * v2.z - v1.z * v2.y;
//        vout.y = v1.z * v2.x - v1.x * v2.z;
//        vout.z = v1.x * v2.y - v1.y * v2.x;
        vout.setI(v1.getJ().multiply(v2.getK()).subtract(v1.getK().multiply(v2.getJ())));
        vout.setJ(v1.getK().multiply(v2.getI()).subtract(v1.getI().multiply(v2.getK())));
        vout.setK(v1.getI().multiply(v2.getJ()).subtract(v1.getJ().multiply(v2.getI())));
        return vout;
    }

    public static PoseType toPose(double mat[][]) {
        if (null == mat || mat.length != 4
                || mat[0].length != 4
                || mat[1].length != 4
                || mat[2].length != 4
                || mat[3].length != 4) {
            throw new IllegalArgumentException("toPose() matrix should be 4x4");
        }
        PoseType newPose = new PoseType();
        PointType pt = new PointType();
        pt.setX(BigDecimal.valueOf(mat[0][3]));
        pt.setY(BigDecimal.valueOf(mat[1][3]));
        pt.setZ(BigDecimal.valueOf(mat[2][3]));
        newPose.setPoint(pt);
        VectorType xAxis = new VectorType();
        xAxis.setI(BigDecimal.valueOf(mat[0][0]));
        xAxis.setJ(BigDecimal.valueOf(mat[0][1]));
        xAxis.setK(BigDecimal.valueOf(mat[0][2]));
        newPose.setXAxis(xAxis);
        VectorType zAxis = new VectorType();
        zAxis.setI(BigDecimal.valueOf(mat[2][0]));
        zAxis.setJ(BigDecimal.valueOf(mat[2][1]));
        zAxis.setK(BigDecimal.valueOf(mat[2][2]));
        newPose.setZAxis(zAxis);
        return newPose;
    }

    public static double[][] toHomMat(PoseType poseIn) {
        double mat[][] = new double[][]{
            {1.0, 0.0, 0.0, 0.0},
            {0.0, 1.0, 0.0, 0.0},
            {0.0, 0.0, 1.0, 0.0},
            {0.0, 0.0, 0.0, 1.0}
        };
        PointType pt = poseIn.getPoint();
        mat[0][3] = pt.getX().doubleValue();
        mat[1][3] = pt.getY().doubleValue();
        mat[2][3] = pt.getZ().doubleValue();
        VectorType xAxis = poseIn.getXAxis();
        mat[0][0] = xAxis.getI().doubleValue();
        mat[0][1] = xAxis.getJ().doubleValue();
        mat[0][2] = xAxis.getK().doubleValue();
        VectorType yAxis = cross(poseIn.getZAxis(), poseIn.getXAxis());
        mat[1][0] = yAxis.getI().doubleValue();
        mat[1][1] = yAxis.getJ().doubleValue();
        mat[1][2] = yAxis.getK().doubleValue();
        VectorType zAxis = poseIn.getZAxis();
        mat[2][0] = zAxis.getI().doubleValue();
        mat[2][1] = zAxis.getJ().doubleValue();
        mat[2][2] = zAxis.getK().doubleValue();
        return mat;
    }

    public static PoseType invert(PoseType p) {
        PoseType pOut = new PoseType();
        VectorType xAxisIn = p.getXAxis();
        VectorType zAxisIn = p.getZAxis();
        VectorType yAxisIn = cross(p.getZAxis(), p.getXAxis());
        VectorType xAxisOut = new VectorType();
        xAxisOut.setI(p.getXAxis().getI());
        xAxisOut.setJ(yAxisIn.getI());
        xAxisOut.setK(p.getZAxis().getI());
        pOut.setXAxis(xAxisOut);
        VectorType zAxisOut = new VectorType();
        zAxisOut.setI(p.getXAxis().getK());
        zAxisOut.setJ(yAxisIn.getK());
        zAxisOut.setK(p.getZAxis().getK());
        pOut.setZAxis(zAxisOut);
//        VectorType yAxisOut = cross(zAxisOut,xAxisOut);

        PointType pt = new PointType();
        pt.setX(dot(xAxisIn, p.getPoint()).negate());
        pt.setY(dot(yAxisIn, p.getPoint()).negate());
        pt.setZ(dot(zAxisIn, p.getPoint()).negate());
        pOut.setPoint(pt);

        return pOut;
    }

    public static PoseType multiply(PoseType p1, PoseType p2) {
        PoseType poseOut = new PoseType();
        VectorType yAxis1 = cross(p1.getZAxis(), p1.getXAxis());
        VectorType yAxis2 = cross(p2.getZAxis(), p2.getXAxis());
        VectorType xAxisOut = new VectorType();
        VectorType zAxisOut = new VectorType();
        PointType pt2 = p2.getPoint();
        PointType pt2rot = new PointType();
        pt2rot.setX(p1.getXAxis().getI().multiply(pt2.getX())
                .add(yAxis1.getI().multiply(pt2.getY()))
                .add(p1.getZAxis().getI().multiply(pt2.getZ()))
        );
        pt2rot.setY(p1.getXAxis().getJ().multiply(pt2.getX())
                .add(yAxis1.getJ().multiply(pt2.getY()))
                .add(p1.getZAxis().getJ().multiply(pt2.getZ()))
        );
        pt2rot.setZ(p1.getXAxis().getK().multiply(pt2.getX())
                .add(yAxis1.getK().multiply(pt2.getY()))
                .add(p1.getZAxis().getK().multiply(pt2.getZ()))
        );
        PointType pt = add(p1.getPoint(), pt2rot);
        poseOut.setPoint(pt);
//        xAxisOut.setI(
//                p1.getXAxis().getI().multiply(p2.getXAxis().getI())
//                .add(p1.getXAxis().getJ().multiply(yAxis2.getI()))
//                .add(p1.getXAxis().getK().multiply(p2.getZAxis().getI()))
//                );
//        xAxisOut.setJ(
//                p1.getXAxis().getI().multiply(p2.getXAxis().getJ())
//                .add(p1.getXAxis().getJ().multiply(yAxis2.getJ()))
//                .add(p1.getXAxis().getK().multiply(p2.getZAxis().getJ()))
//                );
//        xAxisOut.setK(
//                p1.getXAxis().getI().multiply(p2.getXAxis().getK())
//                .add(p1.getXAxis().getJ().multiply(yAxis2.getK()))
//                .add(p1.getXAxis().getK().multiply(p2.getZAxis().getK()))
//                );
        xAxisOut.setI(
                p1.getXAxis().getI().multiply(p2.getXAxis().getI())
                .add(yAxis1.getI().multiply(p2.getXAxis().getJ()))
                .add(p1.getZAxis().getI().multiply(p2.getXAxis().getK()))
        );
        xAxisOut.setJ(
                p1.getXAxis().getJ().multiply(p2.getXAxis().getI())
                .add(yAxis1.getJ().multiply(p2.getXAxis().getJ()))
                .add(p1.getZAxis().getJ().multiply(p2.getXAxis().getK()))
        );
        xAxisOut.setK(
                p1.getXAxis().getK().multiply(p2.getXAxis().getI())
                .add(yAxis1.getK().multiply(p2.getXAxis().getJ()))
                .add(p1.getZAxis().getK().multiply(p2.getXAxis().getK()))
        );

        poseOut.setXAxis(xAxisOut);
        zAxisOut.setI(
                p1.getXAxis().getI().multiply(p2.getZAxis().getI())
                .add(yAxis1.getI().multiply(p2.getZAxis().getJ()))
                .add(p1.getZAxis().getI().multiply(p2.getZAxis().getK()))
        );
        zAxisOut.setJ(
                p1.getXAxis().getJ().multiply(p2.getZAxis().getI())
                .add(yAxis1.getJ().multiply(p2.getZAxis().getJ()))
                .add(p1.getZAxis().getJ().multiply(p2.getZAxis().getK()))
        );
        zAxisOut.setK(
                p1.getXAxis().getK().multiply(p2.getZAxis().getI())
                .add(yAxis1.getK().multiply(p2.getZAxis().getJ()))
                .add(p1.getZAxis().getK().multiply(p2.getZAxis().getK()))
        );
        poseOut.setZAxis(zAxisOut);
        return poseOut;
    }

    public static PoseType shift(final PoseType poseIn, final PointType pt) {
        PoseType poseOut = new PoseType();
        PointType sum = add(poseIn.getPoint(), pt);
        poseOut.setPoint(sum);
        poseOut.setXAxis(poseIn.getXAxis());
        poseOut.setZAxis(poseIn.getZAxis());
        return poseOut;
    }

    public static PoseType pointXAxisZAxisToPose(PointType pt, VectorType x, VectorType z) {
        PoseType pose = new PoseType();
        pose.setPoint(pt);
        pose.setXAxis(x);
        pose.setZAxis(z);
        return pose;
    }

    /**
     * Compute the cartesian distance between two points.
     *
     * @param pt1 first Point
     * @param pt2 second Point
     * @return distance between pt1 and pt2
     */
    public static double diffPoints(PointType pt1, PointType pt2) {
        return pointToPmCartesian(pt1).distFrom(pointToPmCartesian(pt2));
    }

    /**
     * Compute the cartesian distane between the translational components of two
     * poses. Rotations are ignored.
     *
     * @param p1 first Pose
     * @param p2 second Pose
     * @return distance between p1 and p2
     */
    public static double diffPosesTran(PoseType p1, PoseType p2) {
        return diffPoints(p1.getPoint(), p2.getPoint());
    }

    /**
     * Convert the crcl.VectorType to a PmCartesian. crcl.VectorType is
     * generally used as a unit vector for rotation.
     *
     * @param v Vector to convert
     * @return PmCartesian equivalent of v
     */
    public static PmCartesian vectorToPmCartesian(VectorType v) {
        return new PmCartesian(v.getI().doubleValue(),
                v.getJ().doubleValue(),
                v.getK().doubleValue());
    }

    /**
     * Combine a translation and rotation in a PoseType
     *
     * @param tran translational component of pose
     * @param v rotational component of pose
     * @param pose_in optional pose to be set instead of creating new Pose
     * @return new Pose creating from combining inputs or pose_in if not null
     * @throws PmException if rotation vector can not be converted to matrix
     */
    static public PoseType toPoseType(PmCartesian tran, PmRotationVector v, /*@Nullable*/ PoseType pose_in) throws PmException {
        PoseType pose = pose_in;
        if (pose == null) {
            pose = new PoseType();
        }
        pose.setPoint(toPointType(tran));
        PmRotationMatrix mat = Posemath.toMat(v);
        VectorType xVec = new VectorType();
        xVec.setI(BigDecimal.valueOf(mat.x.x));
        xVec.setJ(BigDecimal.valueOf(mat.x.y));
        xVec.setK(BigDecimal.valueOf(mat.x.z));
        pose.setXAxis(xVec);
        VectorType zVec = new VectorType();
        zVec.setI(BigDecimal.valueOf(mat.z.x));
        zVec.setJ(BigDecimal.valueOf(mat.z.y));
        zVec.setK(BigDecimal.valueOf(mat.z.z));
        pose.setZAxis(zVec);
        return pose;
    }

    /**
     * Combine an rcslib Posemath  translation and rotation(roll-pitch-yaw) in a PoseType
     *
     * @param tran translational component of pose
     * @param v rotational component of pose in roll-pith-yaw format.
     * @param pose_in optional pose to be set instead of creating new Pose
     * @return new Pose creating from combining inputs or pose_in if not null
     * @throws PmException if rotation vector can not be converted to matrix
     */
    static public PoseType toPoseType(PmCartesian tran, PmRpy v, /*@Nullable*/ PoseType pose_in) throws PmException {
        PoseType pose = pose_in;
        if (pose == null) {
            pose = new PoseType();
        }
        pose.setPoint(toPointType(tran));
        PmRotationMatrix mat = Posemath.toMat(v);
        VectorType xVec = new VectorType();
        xVec.setI(BigDecimal.valueOf(mat.x.x));
        xVec.setJ(BigDecimal.valueOf(mat.x.y));
        xVec.setK(BigDecimal.valueOf(mat.x.z));
        pose.setXAxis(xVec);
        VectorType zVec = new VectorType();
        zVec.setI(BigDecimal.valueOf(mat.z.x));
        zVec.setJ(BigDecimal.valueOf(mat.z.y));
        zVec.setK(BigDecimal.valueOf(mat.z.z));
        pose.setZAxis(zVec);
        return pose;
    }
    
    /**
     * Combine a translation and rotation in a PoseType
     *
     * @param tran translational component of Pose
     * @param v rotational component of Pose
     * @return new Pose
     * @throws PmException if rotation vector can not be converted to matrix
     */
    static public PoseType toPoseType(PmCartesian tran, PmRotationVector v) throws PmException {
        return toPoseType(tran, v, null);
    }

    /**
     * Combine a translation and rotation(roll-pitch-yaw) in a PoseType
     *
     * @param tran translational component of Pose
     * @param v rotational component in roll-pitch-yaw format  of Pose
     * @return new Pose
     * @throws PmException if rotation vector can not be converted to matrix
     */
    static public PoseType toPoseType(PmCartesian tran, PmRpy v) throws PmException {
        return toPoseType(tran, v, null);
    }
    
    /**
     * Extracts only the rotation part of a PoseType and converts it to
     * a PmRotationMatrix
     *
     * @param p Pose to be converted
     * @return Rotation matrix from rotational component of pose
     * @throws PmException if rotation vectors are invalid
     */
    public static PmRotationMatrix toPmRotationMatrix(PoseType p) throws PmException {
        PmCartesian cx = vectorToPmCartesian(p.getXAxis());
        PmCartesian cz = vectorToPmCartesian(p.getZAxis());
        //PmCartesian cy = Posemath.cross(cz, cx);
        PmCartesian cy = vectorToPmCartesian(cross(p.getZAxis(), p.getXAxis()));
        return new PmRotationMatrix(cx, cy, cz);
    }

    /**
     * Extracts only the rotation part of a PoseType and converts it to
     * a PmRotationMatrix
     *
     * @param p Pose to convert
     * @return Rotation Vector from rotational component of pose.
     * @throws PmException if rotation vectors are invalid
     */
    public static PmRotationVector toPmRotationVector(final PoseType p) throws PmException {
        return Posemath.toRot(toPmRotationMatrix(p));
    }

    /**
     * Extracts only the rotation part of a PoseType and converts it to
     * a roll-pitch-yaw
     *
     * @param p Pose to convert
     * @return Roll-pitch-yaw taken from rotational component of Pose
     * @throws PmException if rotation vectors are invalid
     */
    public static PmRpy toPmRpy(PoseType p) throws PmException {
        return Posemath.toRpy(toPmRotationMatrix(p));
    }

    /**
     * Convenience function that computes the maximum of the absolute value of
     * two arrays. The two arrays must be the same length.
     *
     * @param da first array of doubles
     * @param da2 second array of doubles
     * @return maximum difference between corresponding elements of two arrays
     */
    public static double maxDiffDoubleArray(double da[], double da2[]) {
        if (null == da || null == da2 || da.length != da2.length) {
            throw new IllegalArgumentException("maxDiffDoubleArray expencs two double arrays of same size");
        }
        double diff = 0.0;
        for (int i = 0; i < da.length; i++) {
            diff = Math.max(diff, Math.abs(da[i] - da2[i]));
        }
        return diff;
    }

    /**
     * Compute the magnitude of a rotation vector between the two poses in radians.
     * @param pose1 first pose to compare
     * @param pose2 second pose to compare
     * @return magnitude of rotation between poses.
     * @throws PmException if either pose has an invalid rotation.
     */
    public static double diffPosesRot(PoseType pose1, PoseType pose2) throws PmException {
        PmRotationMatrix m1 = toPmRotationMatrix(pose1);
        PmRotationMatrix m2 = toPmRotationMatrix(pose2);
        PmRotationVector r = Posemath.toRot(m1.multiply(m2.inv()));
        return r.s;
    }

    /**
     * Convert a PmCartesian to a crcl.PointType
     *
     * @param c Cartesian point to convert
     * @return Point equivalent of input cartesian
     */
    public static PointType toPointType(PmCartesian c) {
        PointType pt = new PointType();
        pt.setX(BigDecimal.valueOf(c.x));
        pt.setY(BigDecimal.valueOf(c.y));
        pt.setZ(BigDecimal.valueOf(c.z));
        return pt;
    }
    private static final Logger LOG = Logger.getLogger(CRCLPosemath.class.getName());
}