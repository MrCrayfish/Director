package com.mrcrayfish.director.path;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.director.path.interpolator.AbstractInterpolator;
import com.mrcrayfish.director.path.interpolator.SmoothInterpolator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class PathManager
{
    private static PathManager instance;

    public static PathManager get()
    {
        if(instance == null)
        {
            instance = new PathManager();
        }
        return instance;
    }

    private List<PathPoint> points = new ArrayList<>();
    private AbstractInterpolator interpolator;
    private int currentPointIndex;
    private int remainingPointDuration;
    private boolean playing;
    private int duration = 1000;
    private double roll;
    private double fov;

    private PathManager() {}

    public double getRoll()
    {
        return this.roll;
    }

    public double getFov()
    {
        return this.fov;
    }

    /**
     * Plays the current path
     */
    public void play()
    {
        this.duration = 200;
        this.updateDuration();
        if(this.points.size() < 2)
        {
            Minecraft.getInstance().player.sendMessage(new StringTextComponent("You need at least 2 points to play the path"));
            return;
        }
        Minecraft.getInstance().player.sendChatMessage("/gamemode spectator");
        Minecraft.getInstance().player.abilities.isFlying = true;
        this.currentPointIndex = 0;
        this.remainingPointDuration = this.points.get(0).getDuration();
        this.playing = true;
    }

    /**
     * Stops playing the current path
     */
    public void stop()
    {
        this.playing = false;
    }

    private boolean isPlaying()
    {
        return this.playing && this.currentPointIndex < this.points.size() - 1;
    }

    /**
     *
     */
    private void updateDuration()
    {
        if(this.points.size() > 1)
        {
            double[] pathLengths = new double[this.points.size() - 1];
            double totalLength = 0;
            int segmentsPerPoint = 50;
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                double pathLength = 0;
                for(int j = 0; j < segmentsPerPoint; j++)
                {
                    float chunk = 1.0F / segmentsPerPoint;
                    float progress = (float) j / (float) segmentsPerPoint;
                    Vec3d p1 = this.interpolator.get(i, progress);
                    Vec3d p2 = this.interpolator.get(i, progress + chunk);
                    pathLength += p1.distanceTo(p2);
                }
                pathLengths[i] = pathLength;
                totalLength += pathLength;
                this.points.get(i).setLength(pathLength);
            }
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                this.points.get(i).setDuration((int) (duration * (pathLengths[i] / totalLength) + 0.5));
            }
        }
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.world != null && mc.player != null && event.getAction() == GLFW.GLFW_PRESS)
        {
            if(event.getKey() == GLFW.GLFW_KEY_P) //Add new point
            {
                this.points.add(new PathPoint(mc.player, this));
                this.interpolator = new SmoothInterpolator(this.points);
                this.updateDuration();
                mc.player.sendMessage(new StringTextComponent("Added new point!"));
            }
            if(event.getKey() == GLFW.GLFW_KEY_BACKSLASH) //Reset roll
            {
                this.roll = 0;
                mc.player.sendMessage(new StringTextComponent("Reset roll"));
            }
            if(event.getKey() == GLFW.GLFW_KEY_I)
            {
                this.play();
            }
            if(event.getKey() == GLFW.GLFW_KEY_O)
            {
                this.stop();
            }
            if(event.getKey() == GLFW.GLFW_KEY_U)
            {
                this.stop();
                this.points.clear();
                this.interpolator = null;
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
            return;

        if(this.playing)
        {
            if(this.remainingPointDuration > 0)
            {
                this.remainingPointDuration--;
                if(this.remainingPointDuration <= 0)
                {
                    if(this.currentPointIndex < this.points.size() - 1)
                    {
                        this.currentPointIndex++;
                        this.remainingPointDuration = this.points.get(this.currentPointIndex).getDuration();
                    }
                    else
                    {
                        this.stop();
                    }
                }
            }
            else
            {
                this.stop();
            }
        }
        else
        {
            long windowId = Minecraft.getInstance().getMainWindow().getHandle();
            if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_BRACKET) == GLFW.GLFW_PRESS)
            {
                this.roll -= 0.5;
                Minecraft.getInstance().player.sendMessage(new StringTextComponent("Roll:" + this.roll));
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_RIGHT_BRACKET) == GLFW.GLFW_PRESS)
            {
                this.roll += 0.5;
                Minecraft.getInstance().player.sendMessage(new StringTextComponent("Roll:" + this.roll));
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_EQUAL) == GLFW.GLFW_PRESS)
            {
                this.fov += 1;
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS)
            {
                this.fov -= 0.5;
            }
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(this.isPlaying())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            PathPoint p2 = this.points.get(this.currentPointIndex + 1);
            float progress = 1.0F - ((float) this.remainingPointDuration - event.renderTickTime) / (float) p1.getDuration();
            Vec3d pos = this.interpolator.get(this.currentPointIndex, progress);
            ClientPlayerEntity player = Minecraft.getInstance().player;
            player.setPosition(pos.x, pos.y, pos.z);
            player.prevPosX = pos.x;
            player.prevPosY = pos.y;
            player.prevPosZ = pos.z;
            double d = this.interpolator.distance(this.currentPointIndex, progress) / p1.getLength();
            player.rotationPitch = (float) (p1.getPitch() + (p2.getPitch() - p1.getPitch()) * d);
            player.prevRotationPitch = player.rotationPitch;
            float yawDistance = MathHelper.wrapSubtractDegrees((float) p1.getYaw(), (float) p2.getYaw());
            player.rotationYaw = (float) (p1.getYaw() + yawDistance * d);
            player.prevRotationYaw = player.rotationYaw;
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event)
    {
        if(this.isPlaying())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            PathPoint p2 = this.points.get(this.currentPointIndex + 1);
            float progress = 1.0F - ((float) this.remainingPointDuration - (float) event.getRenderPartialTicks()) / (float) p1.getDuration();
            double distance = this.interpolator.distance(this.currentPointIndex, progress) / p1.getLength();
            float roll = (float) (p1.getRoll() + (p2.getRoll() - p1.getRoll()) * distance);
            event.setRoll(roll);
        }
        else
        {
            event.setRoll((float) this.roll);
        }
    }

    @SubscribeEvent
    public void fov(EntityViewRenderEvent.FOVModifier event)
    {
        if(this.isPlaying())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            PathPoint p2 = this.points.get(this.currentPointIndex + 1);
            float progress = 1.0F - ((float) this.remainingPointDuration - (float) event.getRenderPartialTicks()) / (float) p1.getDuration();
            double distance = this.interpolator.distance(this.currentPointIndex, progress) / p1.getLength();
            double fov = p1.getFov() + (p2.getFov() - p1.getFov()) * distance;
            event.setFOV(fov);
        }
        else
        {
            event.setFOV(Minecraft.getInstance().gameSettings.fov + this.fov);
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event)
    {
        if(this.isPlaying())
        {
            return;
        }

        MatrixStack matrixStack = event.getMatrixStack();
        matrixStack.push();

        Minecraft mc = Minecraft.getInstance();
        Vec3d view = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
        matrixStack.translate(-view.getX(), -view.getY(), -view.getZ());

        IRenderTypeBuffer.Impl renderTypeBuffer = mc.getRenderTypeBuffers().getBufferSource();
        IVertexBuilder builder = renderTypeBuffer.getBuffer(RenderType.getLines());
        Matrix4f lastMatrix = matrixStack.getLast().getMatrix();
        for(int i = 0; i < this.points.size() - 1; i++)
        {
            PathPoint p1 = this.points.get(i);
            for(int j = 0; j < p1.getDuration(); j++)
            {
                float segment = 1.0F / p1.getDuration();
                float progress = j * segment;
                Vec3d v1 = this.interpolator.get(i, progress);
                Vec3d v2 = this.interpolator.get(i, progress + segment);
                builder.pos(lastMatrix, (float) v1.getX(), (float) v1.getY(), (float) v1.getZ()).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
                builder.pos(lastMatrix, (float) v2.getX(), (float) v2.getY(), (float) v2.getZ()).color(1.0F, 0.0F, 0.0F, 1.0F).endVertex();
            }
        }
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(RenderType.getLines());

        ItemStack snowball = new ItemStack(Items.SNOWBALL);
        RenderType renderType = RenderTypeLookup.getRenderType(snowball);
        for(int i = 0; i < this.points.size(); i++)
        {
            PathPoint p1 = this.points.get(i);
            matrixStack.push();
            matrixStack.translate(p1.getX(), p1.getY(), p1.getZ());
            matrixStack.scale(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderItem(snowball, ItemCameraTransforms.TransformType.GROUND, 0, 0, matrixStack, renderTypeBuffer);
            matrixStack.pop();
        }
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(renderType);

        matrixStack.pop();
    }

    private double easeInOut(double t)
    {
        return (t * t) / (2.0 * (t * t - t) + 1.0);
    }
}
