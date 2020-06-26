package com.mrcrayfish.director.path;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;

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
    private float[] steps;
    private IProperties properties;

    public PathPoint(PlayerEntity player, PathManager manager)
    {
        this.update(player, manager);
        this.properties = manager.getInterpolator().propertySupplier().get();
    }

    public void update(PlayerEntity player, PathManager manager)
    {
        this.x = player.getPosX();
        this.y = player.getPosY();
        this.z = player.getPosZ();
        this.yaw = player.rotationYaw;
        this.pitch = player.rotationPitch;
        this.roll = manager.getRoll();
        this.fov = Minecraft.getInstance().gameSettings.fov + manager.getFov();
        this.originalFov = manager.getFov();
    }

    public void copy(PlayerEntity player, PathManager manager)
    {
        player.setPositionAndRotation(this.x, this.y, this.z, (float) this.yaw, (float) this.pitch);
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
        return this.steps.length;
    }

    public void setStepCount(int duration)
    {
        this.steps = new float[duration];
    }

    public void setStep(int index, float progress)
    {
        this.steps[index] = progress;
    }

    public float getStep(int index)
    {
        return this.steps[index];
    }

    public IProperties getProperties()
    {
        return this.properties;
    }
}
