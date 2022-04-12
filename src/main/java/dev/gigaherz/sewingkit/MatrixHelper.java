package dev.gigaherz.sewingkit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

public class MatrixHelper
{
    public static void rotateAroundPivot(PoseStack poseStack, Vector3f pivot, Vector3f axis, float angle, boolean degrees)
    {
        poseStack.translate(pivot.x(), pivot.y(), pivot.z());
        poseStack.mulPose(new Quaternion(axis, angle, degrees));
        poseStack.translate(-pivot.x(), -pivot.y(), -pivot.z());
    }

    public void scaleAroundPoint(PoseStack poseStack, Vector3f point, float scale)
    {
        poseStack.translate(point.x(), point.y(), point.z());
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-point.x(), -point.y(), -point.z());
    }

    public void scaleAroundPoint(PoseStack poseStack, Vector3f point, float xs, float ys, float zs)
    {
        poseStack.translate(point.x(), point.y(), point.z());
        poseStack.scale(xs, ys, zs);
        poseStack.translate(-point.x(), -point.y(), -point.z());
    }

    public void scaleAroundPoint(PoseStack poseStack, Vector3f point, Vector3f scale)
    {
        poseStack.translate(point.x(), point.y(), point.z());
        poseStack.scale(scale.x(), scale.y(), scale.z());
        poseStack.translate(-point.x(), -point.y(), -point.z());
    }
}
