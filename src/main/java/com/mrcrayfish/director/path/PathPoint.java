package com.mrcrayfish.director.path;

import com.mrcrayfish.director.path.interpolator.InterpolateType;
import com.mrcrayfish.director.path.interpolator.PathType;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;

/**
 * Author: MrCrayfish
 */
public class PathPoint
{
    private double x;
    private double y;
    private double z;
    private double yaw;
    private double pitch;
    private double roll;
    private double fov;
    private double originalFov;
    private float[] positionSteps;
    private float[] rotationSteps;

    private EnumMap<InterpolateType, IProperties> positionProperties = new EnumMap<>(InterpolateType.class);
    private EnumMap<InterpolateType, IProperties> rotationProperties = new EnumMap<>(InterpolateType.class);

    public PathPoint(Player player, PathManager manager)
    {
        this.update(player, manager);
    }

    public void update(Player player, PathManager manager)
    {
        this.x = player.getX();
        this.y = player.getY();
        this.z = player.getZ();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.roll = manager.getRoll();
        this.fov = Minecraft.getInstance().options.fov + manager.getFov();
        this.originalFov = manager.getFov();
    }

    public void copy(Player player, PathManager manager)
    {
        player.absMoveTo(this.x, this.y, this.z, (float) this.yaw, (float) this.pitch);
        manager.setRoll(this.roll);
        manager.setFov(this.originalFov);
    }

    public double getX()
    {
        return this.x;
    }

    public double getY()
    {
        return this.y;
    }

    public double getZ()
    {
        return this.z;
    }

    public double getYaw()
    {
        return this.yaw;
    }

    public double getPitch()
    {
        return this.pitch;
    }

    public double getRoll()
    {
        return this.roll;
    }

    public double getFov()
    {
        return this.fov;
    }

    public double getOriginalFov()
    {
        return this.originalFov;
    }

    public int getStepCount()
    {
        return this.positionSteps.length;
    }

    public void setStepCount(int duration)
    {
        this.positionSteps = new float[duration];
        this.rotationSteps = new float[duration];
    }

    public void setPositionStep(int index, float progress)
    {
        this.positionSteps[index] = progress;
    }

    public float getPositionStep(int index)
    {
        return this.positionSteps[index];
    }

    public void setRotationStep(int index, float progress)
    {
        this.rotationSteps[index] = progress;
    }

    public float getRotationStep(int index)
    {
        return this.rotationSteps[index];
    }

    public IProperties getProperties(InterpolateType interpolateType, PathType pathType)
    {
        EnumMap<InterpolateType, IProperties> properties = pathType == PathType.POSITION ? this.positionProperties : this.rotationProperties;
        return properties.computeIfAbsent(interpolateType, type -> PathManager.instance().getInterpolator(pathType).propertySupplier().get());
    }

    public IProperties getPositionProperties(InterpolateType type)
    {
        return this.positionProperties.computeIfAbsent(type, type1 -> PathManager.instance().getInterpolator(PathType.POSITION).propertySupplier().get());
    }

    public IProperties getRotationProperties(InterpolateType type)
    {
        return this.rotationProperties.computeIfAbsent(type, type1 -> PathManager.instance().getInterpolator(PathType.ROTATION).propertySupplier().get());
    }
}
