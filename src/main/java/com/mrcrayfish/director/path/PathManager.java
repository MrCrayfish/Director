package com.mrcrayfish.director.path;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.director.path.interpolator.AbstractInterpolator;
import com.mrcrayfish.director.path.interpolator.SmoothInterpolator;
import com.mrcrayfish.director.screen.EditPointScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author: MrCrayfish
 */
public class PathManager
{
    private static final double POINT_BOX_SIZE = 0.5;
    private static final float[] START_POINT_COLOR = {1.0F, 0.18F, 0.0F};
    private static final float[] POINT_COLOR = {1.0F, 0.815F, 0.0F};
    private static final float[] END_POINT_COLOR = {0.549F, 1.0F, 0.0F};

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
    private int duration = 100;
    private double prevRoll;
    private double roll;
    private double prevFov;
    private double fov;

    private PathPoint repositionPoint;
    private boolean repositioning;

    private PathManager()
    {
    }

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
        this.duration = 100;
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

    public boolean isPlaying()
    {
        return this.playing && this.currentPointIndex < this.points.size() - 1;
    }

    public void delete(PathPoint point)
    {
        this.points.remove(point);
        this.updateDuration();
    }

    /**
     *
     * @param point
     */
    public void reposition(PathPoint point)
    {
        this.repositionPoint = point;
        this.repositioning = true;
        this.showMessage("Update the waypoint by creating a new waypoint");
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
                    Vec3d p1 = this.interpolator.pos(i, progress);
                    Vec3d p2 = this.interpolator.pos(i, progress + chunk);
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
                if(this.repositioning)
                {
                    this.repositionPoint.update(mc.player,this);
                    this.repositionPoint = null;
                    this.repositioning = false;
                    this.showMessage("Updated waypoint!");
                }
                else
                {
                    this.points.add(new PathPoint(mc.player, this));
                    this.interpolator = new SmoothInterpolator(this.points);
                    this.showMessage("Added a new waypoint!");
                }
                this.updateDuration();
            }
            if(event.getKey() == GLFW.GLFW_KEY_BACKSLASH) //Reset roll
            {
                this.roll = 0;
                this.showMessage("Reset Roll");
            }
            if(event.getKey() == GLFW.GLFW_KEY_0) //Reset fov
            {
                this.fov = 0;
                this.showMessage("Reset FOV");
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
                this.showMessage("Waypoints cleared!");
            }
            if(event.getKey() == GLFW.GLFW_KEY_L)
            {
                //Minecraft.getInstance().displayGuiScreen(new EditPointScreen());
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START) return;

        this.prevRoll = this.roll;
        this.prevFov = this.fov;

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
                this.showValue("Roll", String.valueOf(this.roll));
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_RIGHT_BRACKET) == GLFW.GLFW_PRESS)
            {
                this.roll += 0.5;
                this.showValue("Roll", String.valueOf(this.roll));
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_EQUAL) == GLFW.GLFW_PRESS)
            {
                this.fov += 1;
                this.showValue("FOV", String.valueOf(Minecraft.getInstance().gameSettings.fov + this.fov));
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_MINUS) == GLFW.GLFW_PRESS)
            {
                this.fov -= 1;
                this.showValue("FOV", String.valueOf(Minecraft.getInstance().gameSettings.fov + this.fov));
            }
        }
    }

    private void showMessage(String message)
    {
        Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent("director.format.message", message), true);
    }

    private void showValue(String name, String value)
    {
        Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent("director.format.value", name, value), true);
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(this.isPlaying())
        {
            /* Calculate the current progress between two points based on the remaining time */
            PathPoint p1 = this.points.get(this.currentPointIndex);
            PathPoint p2 = this.points.get(this.currentPointIndex + 1);
            float progress = 1.0F - ((float) this.remainingPointDuration - event.renderTickTime) / (float) p1.getDuration();

            /* Updated the position of the player */
            Vec3d pos = this.interpolator.pos(this.currentPointIndex, progress);
            ClientPlayerEntity player = Minecraft.getInstance().player;
            player.setPosition(pos.x, pos.y, pos.z);
            player.prevPosX = pos.x;
            player.prevPosY = pos.y;
            player.prevPosZ = pos.z;

            /* Updated the pitch of the player */
            float pitch = this.interpolator.pitch(this.currentPointIndex, progress);
            player.rotationPitch = pitch;
            player.prevRotationPitch = pitch;

            /* Updated the yaw of the player */
            float yaw = this.interpolator.yaw(this.currentPointIndex, progress);
            player.rotationYaw = yaw;
            player.prevRotationYaw = yaw;
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event)
    {
        if(this.isPlaying())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            float progress = 1.0F - ((float) this.remainingPointDuration - (float) event.getRenderPartialTicks()) / (float) p1.getDuration();
            float roll = this.interpolator.roll(this.currentPointIndex, progress);
            event.setRoll(roll);
        }
        else
        {
            event.setRoll((float) (this.prevRoll + (this.roll - this.prevRoll) * event.getRenderPartialTicks()));
        }
    }

    @SubscribeEvent
    public void fov(EntityViewRenderEvent.FOVModifier event)
    {
        if(this.isPlaying())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            float progress = 1.0F - ((float) this.remainingPointDuration - (float) event.getRenderPartialTicks()) / (float) p1.getDuration();
            double fov = this.interpolator.fov(this.currentPointIndex, progress);
            event.setFOV(fov);
        }
        else
        {
            double fov = this.prevFov + (this.fov - this.prevFov) * event.getRenderPartialTicks();
            event.setFOV(Minecraft.getInstance().gameSettings.fov + fov);
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

        AxisAlignedBB pointBox = new AxisAlignedBB(-0.10, -0.10, -0.10, 0.10, 0.10, 0.10);
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
                Vec3d v1 = this.interpolator.pos(i, progress);
                Vec3d v2 = this.interpolator.pos(i, progress + segment);
                builder.pos(lastMatrix, (float) v1.getX(), (float) v1.getY(), (float) v1.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
                builder.pos(lastMatrix, (float) v2.getX(), (float) v2.getY(), (float) v2.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();

                /*matrixStack.push();
                matrixStack.translate(v1.getX(), v1.getY(), v1.getZ());
                matrixStack.scale(0.5F, 0.5F, 0.5F);
                WorldRenderer.drawBoundingBox(matrixStack, builder, pointBox, 0.0F, 0.0F, 0.0F, 1.0F);
                matrixStack.pop();*/
            }
        }

        double halfBoxSize = POINT_BOX_SIZE / 2;
        AxisAlignedBB pathBox = new AxisAlignedBB(-halfBoxSize, -halfBoxSize, -halfBoxSize, halfBoxSize, halfBoxSize, halfBoxSize);
        for(int i = 0; i < this.points.size(); i++)
        {
            PathPoint p1 = this.points.get(i);
            matrixStack.push();
            matrixStack.translate(p1.getX(), p1.getY(), p1.getZ());
            float[] color = this.getPointColor(i);
            WorldRenderer.drawBoundingBox(matrixStack, builder, pathBox, color[0], color[1], color[2], 1.0F);
            matrixStack.pop();
        }
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(RenderType.getLines());

        matrixStack.pop();
    }

    private float[] getPointColor(int index)
    {
        if(index == 0)
        {
            return START_POINT_COLOR;
        }
        else if(index == this.points.size() - 1)
        {
            return END_POINT_COLOR;
        }
        return POINT_COLOR;
    }

    @SubscribeEvent
    public void onRawMouseInput(InputEvent.RawMouseEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.loadingGui != null || mc.currentScreen != null || !mc.mouseHelper.isMouseGrabbed() || mc.player == null || !mc.player.isSpectator())
        {
            return;
        }

        if(event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT)
        {
            PathPoint hoveredPathPoint = getHoveredPathPoint();
            if(hoveredPathPoint != null)
            {
                this.repositioning = false;
                this.repositionPoint = null;
                mc.displayGuiScreen(new EditPointScreen(hoveredPathPoint));
            }
        }
    }

    /**
     * Checks if the player is looking at a path point. This method is hooked via ASM so removing
     * this will cause the game to crash if you don't also remove it from the hook.
     *
     * @return if the player is looking at a path point
     */
    @SuppressWarnings("unused")
    public static boolean isLookingAtPathPoint()
    {
        return getHoveredPathPoint() != null;
    }

    @Nullable
    private static PathPoint getHoveredPathPoint()
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.player == null || !mc.player.isSpectator())
        {
            return null;
        }

        /* Setup the start and end vec of the ray trace */
        double reachDistance = mc.player.getAttribute(PlayerEntity.REACH_DISTANCE).getValue();
        Vec3d startVec = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vec3d endVec = startVec.add(mc.player.getLookVec().scale(reachDistance));

        /* Creates axis aligned boxes for all path points then remove ones that aren't close enough to the player */
        double halfBoxSize = POINT_BOX_SIZE / 2;
        List<Pair<PathPoint, AxisAlignedBB>> pointPairs = PathManager.get().points.stream().map(p -> Pair.of(p, new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + POINT_BOX_SIZE, p.getY() + POINT_BOX_SIZE, p.getZ() + POINT_BOX_SIZE).offset(-halfBoxSize, -halfBoxSize, -halfBoxSize))).collect(Collectors.toList());
        pointPairs.removeIf(pair -> pair.getRight().getCenter().distanceTo(startVec) > reachDistance + 1);

        /* Ray trace and get the closest path point */
        double closestDistance = Double.MAX_VALUE;
        PathPoint closestPoint = null;
        for(Pair<PathPoint, AxisAlignedBB> pair : pointPairs)
        {
            Optional<Vec3d> optional = pair.getRight().rayTrace(startVec, endVec);
            if(optional.isPresent())
            {
                double distance = startVec.squareDistanceTo(optional.get());
                if(distance < closestDistance)
                {
                    closestPoint = pair.getLeft();
                    closestDistance = distance;
                }
            }
        }

        return closestPoint;
    }
}
