package com.mrcrayfish.director;

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

    private int duration;

    public PathPoint(PlayerEntity player)
    {
        this.x = player.getPosX();
        this.y = player.getPosY();
        this.z = player.getPosZ();
        this.yaw = player.rotationYaw;
        this.pitch = player.rotationPitch;
        this.roll = 0;
        this.fov = Minecraft.getInstance().gameSettings.fov; //TODO change to something else
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

    public int getDuration()
    {
        return this.duration;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }
}
