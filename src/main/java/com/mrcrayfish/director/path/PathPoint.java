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

    private double length;
    private int duration;

    public PathPoint(PlayerEntity player, PathManager manager)
    {
        this.update(player, manager);
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

    public double getLength()
    {
        return length;
    }

    public void setLength(double length)
    {
        this.length = length;
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
