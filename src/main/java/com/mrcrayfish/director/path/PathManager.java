package com.mrcrayfish.director.path;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.mrcrayfish.director.path.interpolator.AbstractInterpolator;
import com.mrcrayfish.director.path.interpolator.InterpolateType;
import com.mrcrayfish.director.path.interpolator.PathType;
import com.mrcrayfish.director.screen.EditPointScreen;
import com.mrcrayfish.director.screen.PathMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
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

    public static PathManager instance()
    {
        if(instance == null)
        {
            instance = new PathManager();
        }
        return instance;
    }

    private List<PathPoint> points = new ArrayList<>();
    private AbstractInterpolator positionInterpolator = InterpolateType.HERMITE.get(PathType.POSITION);
    private AbstractInterpolator rotationInterpolator = InterpolateType.HERMITE.get(PathType.ROTATION);
    private int currentPointIndex;
    private int remainingPointDuration;
    private boolean visible = true;
    private boolean playing;
    private int duration = 100;
    private double prevRoll;
    private double roll;
    private double prevFov;
    private double fov;

    private PathPoint editingPoint;
    private boolean repositioning;
    private boolean insertAfter;

    public List<PathPoint> getPoints()
    {
        return this.points;
    }

    public AbstractInterpolator getInterpolator(PathType pathType)
    {
        return pathType == PathType.POSITION ? this.positionInterpolator : this.rotationInterpolator;
    }

    public void setInterpolator(InterpolateType interpolateType, PathType pathType)
    {
        AbstractInterpolator interpolator = interpolateType.get(pathType);
        switch(pathType)
        {
            case POSITION:
                this.positionInterpolator = interpolator;
                break;
            case ROTATION:
                this.rotationInterpolator = interpolator;
                break;
        }
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

    public boolean isVisible()
    {
        return this.visible;
    }

    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    /**
     * Plays the current path
     */
    public void play()
    {
        this.repositioning = false;
        this.insertAfter = false;
        this.editingPoint = null;
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
        this.roll = 0;
        this.fov = 0;
    }

    public boolean isPlaying()
    {
        return this.playing && this.currentPointIndex < this.points.size() - 1;
    }

    public void delete(PathPoint point)
    {
        this.points.remove(point);
        this.updatePathPoints();
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
    public void deletePath()
    {
        this.stop();
        this.points.clear();
        this.showMessage("Waypoints cleared!");
    }

    /**
     *
     */
    public void updatePathPoints()
    {
        if(this.points.size() > 1)
        {
            /* Gets the length of each connection on the path */
            double[] positionLengths = new double[this.points.size() - 1];
            double[] rotationLengths = new double[this.points.size() - 1];
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                double positionLength = this.positionInterpolator.length(i, i + 1);
                positionLengths[i] = positionLength;

                double rotationLength = positionLength;
                if(this.rotationInterpolator != this.positionInterpolator)
                {
                    rotationLength = this.rotationInterpolator.length(i, i + 1);
                }
                rotationLengths[i] = rotationLength;
            }

            /* Updates each point's step count based on the connection length over the total length
             * of the path*/
            double totalLength = Arrays.stream(positionLengths).sum();
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                this.points.get(i).setStepCount((int) (this.duration * (positionLengths[i] / totalLength) + 0.5));
            }

            /* Updates the step length to instance to each sub-point */
            for(int i = 0; i < this.points.size() - 1; i++)
            {
                PathPoint p1 = this.points.get(i);
                float positionStep = (float) (positionLengths[i] / p1.getStepCount());
                float rotationStep = (float) (rotationLengths[i] / p1.getStepCount());
                for(int j = 0; j < p1.getStepCount(); j++)
                {
                    float positionProgress = this.positionInterpolator.progress(i, j * positionStep, positionLengths[i]);
                    p1.setPositionStep(j, positionProgress);

                    /* Avoid calculating the steps twice if they are the same interpolator */
                    float rotationProgress = positionProgress;
                    if(this.rotationInterpolator != this.positionInterpolator)
                    {
                        rotationProgress = this.rotationInterpolator.progress(i, j * rotationStep, rotationLengths[i]);
                    }
                    p1.setRotationStep(j, rotationProgress);
                }
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
                this.updatePathPoints();
            }
            if(event.getKey() == GLFW.GLFW_KEY_I)
            {
                this.play();
            }
            if(event.getKey() == GLFW.GLFW_KEY_O)
            {
                this.stop();
            }
            if(event.getKey() == GLFW.GLFW_KEY_GRAVE_ACCENT)
            {
                Minecraft.getInstance().displayGuiScreen(new PathMenuScreen());
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

    private float getPositionProgress(float partialTicks)
    {
        if(this.currentPointIndex >= 0 && this.currentPointIndex < this.points.size())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            float s1 = p1.getPositionStep(p1.getStepCount() - this.remainingPointDuration);
            float s2 = 1.0F;
            if(this.remainingPointDuration - 1 != 0)
            {
                s2 = p1.getPositionStep(p1.getStepCount() - (this.remainingPointDuration - 1));
            }
            return MathHelper.clamp(s1 + (s2 - s1) * partialTicks, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    private float getRotationProgress(float partialTicks)
    {
        if(this.currentPointIndex >= 0 && this.currentPointIndex < this.points.size())
        {
            PathPoint p1 = this.points.get(this.currentPointIndex);
            float s1 = p1.getRotationStep(p1.getStepCount() - this.remainingPointDuration);
            float s2 = 1.0F;
            if(this.remainingPointDuration - 1 != 0)
            {
                s2 = p1.getRotationStep(p1.getStepCount() - (this.remainingPointDuration - 1));
            }
            return MathHelper.clamp(s1 + (s2 - s1) * partialTicks, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(event.phase == TickEvent.Phase.START)
        {
            return;
        }

        if(this.isPlaying())
        {
            float positionProgress = this.getPositionProgress(event.renderTickTime);
            //System.out.println("Pos: " + this.currentPointIndex + " " + positionProgress);

            float rotationProgress = this.getRotationProgress(event.renderTickTime);

            /* Updated the position of the player */
            Vec3d pos = this.positionInterpolator.pos(this.currentPointIndex, positionProgress);
            ClientPlayerEntity player = Minecraft.getInstance().player;
            player.setPosition(pos.x, pos.y, pos.z);
            player.prevPosX = pos.x;
            player.prevPosY = pos.y;
            player.prevPosZ = pos.z;

            /* Updated the pitch of the player */
            float pitch = this.rotationInterpolator.pitch(this.currentPointIndex, rotationProgress);
            player.rotationPitch = pitch;
            player.prevRotationPitch = pitch;

            /* Updated the yaw of the player */
            float yaw = this.rotationInterpolator.yaw(this.currentPointIndex, rotationProgress);
            player.rotationYaw = yaw;
            player.prevRotationYaw = yaw;
            System.out.println("Rot: " + this.currentPointIndex + " " + yaw);
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event)
    {
        if(this.isPlaying())
        {
            float progress = this.getRotationProgress((float) event.getRenderPartialTicks());
            float roll = this.rotationInterpolator.roll(this.currentPointIndex, progress);
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
            float progress = this.getRotationProgress((float) event.getRenderPartialTicks());
            double fov = this.rotationInterpolator.fov(this.currentPointIndex, progress);
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
        if(this.isPlaying() || !this.isVisible())
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
                Vec3d v1 = this.positionInterpolator.pos(i, j == 0 ? 0.0F : p1.getPositionStep(j - 1));
                Vec3d v2 = this.positionInterpolator.pos(i, j == p1.getStepCount() ? 1.0F : p1.getPositionStep(j));
                builder.pos(lastMatrix, (float) v1.getX(), (float) v1.getY(), (float) v1.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
                builder.pos(lastMatrix, (float) v2.getX(), (float) v2.getY(), (float) v2.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();

                //Renders the rotation path
                /*Vec3d v3 = this.rotationInterpolator.instance().pos(i, j == 0 ? 0.0F : p1.getRotationStep(j - 1));
                Vec3d v4 = this.rotationInterpolator.instance().pos(i, j == p1.getStepCount() ? 1.0F : p1.getRotationStep(j));
                builder.pos(lastMatrix, (float) v3.getX(), (float) v3.getY(), (float) v3.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();
                builder.pos(lastMatrix, (float) v4.getX(), (float) v4.getY(), (float) v4.getZ()).color(1.0F, 1.0F, 1.0F, 1.0F).endVertex();*/

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
        if(PathManager.instance().isPlaying() || !PathManager.instance().isVisible())
        {
            return null;
        }

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
        List<Pair<PathPoint, AxisAlignedBB>> pointPairs = PathManager.instance().points.stream().map(p -> Pair.of(p, new AxisAlignedBB(p.getX(), p.getY(), p.getZ(), p.getX() + POINT_BOX_SIZE, p.getY() + POINT_BOX_SIZE, p.getZ() + POINT_BOX_SIZE).offset(-halfBoxSize, -halfBoxSize, -halfBoxSize))).collect(Collectors.toList());
        pointPairs.removeIf(pair -> pair.getRight().getCenter().distanceTo(startVec) > reachDistance + 1);

        /* Ray trace and instance the closest path point */
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
