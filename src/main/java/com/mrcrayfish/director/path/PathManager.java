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
import java.util.Arrays;
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
    private AbstractInterpolator interpolator = new SmoothInterpolator(this.points);
    private int currentPointIndex;
    private int remainingPointDuration;
    private boolean playing;
    private int duration = 100;
    private double prevRoll;
    private double roll;
    private double prevFov;
    private double fov;

    private PathPoint editingPoint;
    private boolean repositioning;
    private boolean insertAfter;

    public AbstractInterpolator getInterpolator()
    {
        return this.interpolator;
    }

    public double getRoll()
    {
        return this.roll;
    }

    public void setRoll(double roll)
    {
        this.roll = roll;
    }

    public double getFov()
    {
        return this.fov;
    }

    public void setFov(double fov)
    {
        this.fov = fov;
    }

    /**
     * Plays the current path
     */
    public void play()
    {
        this.repositioning = false;
        this.insertAfter = false;
        this.editingPoint = null;
        this.duration = 100;
        this.updateLengthAndSteps();
        if(this.points.size() < 2)
        {
            Minecraft.getInstance().player.sendMessage(new StringTextComponent("You need at least 2 points to play the path"));
            return;
        }
        Minecraft.getInstance().player.sendChatMessage("/gamemode spectator");
        Minecraft.getInstance().player.abilities.isFlying = true;
        this.currentPointIndex = 0;
        this.remainingPointDuration = this.points.get(0).getStepCount();
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
        this.updateLengthAndSteps();
    }

    /**
     *
     * @param point
     */
    public void reposition(PathPoint point)
    {
        point.copy(Minecraft.getInstance().player, this);
        this.editingPoint = point;
        this.repositioning = true;
        this.showMessage("Update the waypoint by creating a new waypoint");
    }

    /**
     *
     * @param point
     */
    public void insertAfter(PathPoint point)
    {
        point.copy(Minecraft.getInstance().player, this);
        this.editingPoint = point;
        this.insertAfter = true;
        this.showMessage("Insert a waypoint by creating a new waypoint");
    }

    /**
     *
     */
    public void updateLengthAndSteps()
    {
        if(this.points.size() > 1)
        {
            /* Gets the length of each connection on the path */
            double[] lengths = new double[this.points.size() - 1];
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                lengths[i] = this.interpolator.length(i, i + 1);
            }

            /* Updates each point's step count based on the connection length over the total length
             * of the path*/
            double totalLength = Arrays.stream(lengths).sum();
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                this.points.get(i).setStepCount((int) (this.duration * (lengths[i] / totalLength) + 0.5));
            }

            /* Updates the step length to get to each sub-point */
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                PathPoint p1 = this.points.get(i);
                float step = (float) (lengths[i] / p1.getStepCount());
                for(int j = 0; j < p1.getStepCount(); j++)
                {
                    p1.setStep(j, this.getProgressForDistance(100, i, 0, j * step, 0.0F, 1.0F));
                }
            }

            for(int i = 0; i < this.points.get(0).getStepCount(); i++)
            {
                System.out.println(i + " " + this.points.get(0).getStep(i));
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
                    this.editingPoint.update(mc.player, this);
                    this.editingPoint = null;
                    this.repositioning = false;
                    this.showMessage("Updated waypoint!");
                }
                else if(this.insertAfter)
                {
                    int index = this.points.indexOf(this.editingPoint);
                    this.points.add(index + 1, new PathPoint(mc.player, this));
                    this.editingPoint = null;
                    this.insertAfter = false;
                    this.showMessage("Inserted a new waypoint!");
                }
                else
                {
                    this.points.add(new PathPoint(mc.player, this));
                    this.showMessage("Added a new waypoint!");
                }
                this.updateLengthAndSteps();
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
                this.showMessage("Waypoints cleared!");
            }
        }
    }

    private float getProgressForDistance(int limit, int index, double startDistance, double targetDistance, float startProgress, float endProgress)
    {
        if(limit <= 0)
        {
            return startProgress;
        }

        float tailProgress = 0;
        float headProgress = 0;
        double distance = startDistance;
        Vec3d lastPos = this.interpolator.pos(index, startProgress);
        float step = (endProgress - startProgress) / 10F;
        for(int i = 0; i <= 10; i++)
        {
            float progress = i * step;
            Vec3d pos = this.interpolator.pos(index, startProgress + progress);
            distance += pos.distanceTo(lastPos);
            lastPos = pos;
            if(distance < targetDistance)
            {
                tailProgress = progress;
                startDistance = distance;
            }
            else if(distance > targetDistance && headProgress == 0)
            {
                headProgress = progress;
            }
            if(Math.abs(targetDistance - distance) < 0.001)
            {
                return startProgress + progress;
            }
        }
        return this.getProgressForDistance(limit - 1, index, startDistance, targetDistance, startProgress + tailProgress, startProgress + headProgress);
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
                if(this.remainingPointDuration < 1)
                {
                    if(this.currentPointIndex < this.points.size() - 2)
                    {
                        this.currentPointIndex++;
                        this.remainingPointDuration = this.points.get(this.currentPointIndex).getStepCount();
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
    }

    private void showMessage(String message)
    {
        Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent("director.format.message", message), true);
    }

    private void showValue(String name, String value)
    {
        Minecraft.getInstance().player.sendStatusMessage(new TranslationTextComponent("director.format.value", name, value), true);
    }

    private float getProgress(float partialTicks)
    {
        if(this.currentPointIndex >= 0 && this.currentPointIndex < this.points.size())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            float s1 = p1.getStep(p1.getStepCount() - this.remainingPointDuration);
            float s2 = 1.0F;
            if(this.remainingPointDuration - 1 != 0)
            {
                s2 = p1.getStep(p1.getStepCount() - (this.remainingPointDuration - 1));
            }
            return s1 + (s2 - s1) * partialTicks;
        }
        return 0.0F;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(this.isPlaying())
        {
            float progress = this.getProgress(event.renderTickTime);

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
            float progress = this.getProgress((float) event.getRenderPartialTicks());
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
            float progress = this.getProgress((float) event.getRenderPartialTicks());
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
            for(int j = 0; j <= p1.getStepCount(); j++)
            {
                Vec3d v1 = this.interpolator.pos(i, j == 0 ? 0.0F : p1.getStep(j - 1));
                Vec3d v2 = this.interpolator.pos(i, j == p1.getStepCount() ? 1.0F : p1.getStep(j));
                builder.pos(lastMatrix, (float) v1.getX(), (float) v1.getY(), (float) v1.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
                builder.pos(lastMatrix, (float) v2.getX(), (float) v2.getY(), (float) v2.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();

                /*matrixStack.push();
                matrixStack.translate(v2.getX(), v2.getY(), v2.getZ());
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
    public void onMouseScroll(InputEvent.MouseScrollEvent event)
    {
        long windowId = Minecraft.getInstance().getMainWindow().getHandle();
        if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS)
        {
            this.fov -= event.getScrollDelta();
            this.showValue("FOV", String.valueOf(this.fov));
            event.setCanceled(true);
        }
        else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS)
        {
            this.roll += event.getScrollDelta();
            this.showValue("Roll", String.valueOf(this.roll));
            event.setCanceled(true);
        }
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
                this.editingPoint = null;
                mc.displayGuiScreen(new EditPointScreen(hoveredPathPoint));
                event.setCanceled(true);
            }
        }

        if(event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
        {
            long windowId = Minecraft.getInstance().getMainWindow().getHandle();
            if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS)
            {
                this.fov = 0;
                this.showMessage("Reset FOV");
                event.setCanceled(true);
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_ALT) == GLFW.GLFW_PRESS)
            {
                this.roll = 0;
                this.showMessage("Reset Roll");
                event.setCanceled(true);
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
