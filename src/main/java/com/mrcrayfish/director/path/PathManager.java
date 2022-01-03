package com.mrcrayfish.director.path;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mrcrayfish.director.path.interpolator.AbstractInterpolator;
import com.mrcrayfish.director.path.interpolator.InterpolateType;
import com.mrcrayfish.director.path.interpolator.PathType;
import com.mrcrayfish.director.screen.EditPointScreen;
import com.mrcrayfish.director.screen.PathMenuScreen;
import com.mrcrayfish.director.util.EntityRenderUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
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
    private static final KeyMapping KEY_BIND_PLAY = new KeyMapping("key.director.play", GLFW.GLFW_KEY_I, "key.categories.director");
    private static final KeyMapping KEY_BIND_STOP = new KeyMapping("key.director.stop", GLFW.GLFW_KEY_O, "key.categories.director");
    private static final KeyMapping KEY_BIND_SETTINGS = new KeyMapping("key.director.settings", GLFW.GLFW_KEY_U, "key.categories.director");
    private static final KeyMapping KEY_BIND_POINT = new KeyMapping("key.director.point", GLFW.GLFW_KEY_P, "key.categories.director");

    private static final double POINT_BOX_SIZE = 0.5;
    private static final float[] START_POINT_COLOR = {1.0F, 0.18F, 0.0F};
    private static final float[] POINT_COLOR = {1.0F, 0.815F, 0.0F};
    private static final float[] END_POINT_COLOR = {0.549F, 1.0F, 0.0F};

    private static PathManager instance;

    /**
     * Gets the singleton instance of the path manager
     */
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

    private PathManager()
    {
        ClientRegistry.registerKeyBinding(KEY_BIND_PLAY);
        ClientRegistry.registerKeyBinding(KEY_BIND_STOP);
        ClientRegistry.registerKeyBinding(KEY_BIND_SETTINGS);
        ClientRegistry.registerKeyBinding(KEY_BIND_POINT);
    }

    /**
     * Gets the list of path points on the current path
     */
    public List<PathPoint> getPoints()
    {
        return this.points;
    }

    /**
     * Gets the instance of an {@link AbstractInterpolator} for the specified path type. The position
     * and rotation of the use separate interpolators for more control.
     *
     * @param pathType the type of path
     * @return an instance of the interpolator
     */
    public AbstractInterpolator getInterpolator(PathType pathType)
    {
        return pathType == PathType.POSITION ? this.positionInterpolator : this.rotationInterpolator;
    }

    /**
     * Creates and sets a new interpolator for the specified path type.
     *
     * @param interpolateType the type of interpolator
     * @param pathType        the path type to set the interpolator for
     */
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

    /**
     * Gets the duration of the path
     */
    public int getDuration()
    {
        return duration;
    }

    /**
     * Sets the duration of the path
     */
    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    /**
     * Gets the current roll of the camera
     */
    public double getRoll()
    {
        return this.roll;
    }

    /**
     * Sets the roll for the camera
     */
    public void setRoll(double roll)
    {
        this.roll = roll;
    }

    /**
     * Gets the current additional field of view for the camera
     */
    public double getFov()
    {
        return this.fov;
    }

    /**
     * Sets the additional field of view for the camera
     */
    public void setFov(double fov)
    {
        this.fov = fov;
    }

    /**
     * Gets the visibility of the path
     */
    public boolean isVisible()
    {
        return this.visible;
    }

    /**
     * Sets the visibility of the path. Setting to false will prevent it from rendering
     */
    public void setVisible(boolean visible)
    {
        this.visible = visible;
    }

    /**
     * Plays the current path
     */
    public void play()
    {
        if(!this.isPlayerValidDirector())
        {
            return;
        }

        this.repositioning = false;
        this.insertAfter = false;
        this.editingPoint = null;
        if(this.points.size() < 2)
        {
            Minecraft.getInstance().player.sendMessage(new TextComponent("You need at least 2 points to play the path"),  Minecraft.getInstance().player.getUUID());
            return;
        }
        Minecraft.getInstance().player.chat("/gamemode spectator");
        Minecraft.getInstance().player.getAbilities().flying = true;
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

    /**
     * Checks if player can be a director. In other words, are they in spectator mode as it's
     * required for the camera to work correctly.
     *
     * @return if the player is a valid director
     */
    public boolean isPlayerValidDirector()
    {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.isSpectator();
    }

    /**
     * Checks if the path is currently being played
     *
     * @return true if the path is playing
     */
    public boolean isPlaying()
    {
        return this.playing && this.currentPointIndex < this.points.size() - 1;
    }

    /**
     * Deletes the specified path point from the path and updates the remaining path points
     *
     * @param point the path point to delete
     */
    public void delete(PathPoint point)
    {
        this.points.remove(point);
        this.updatePathPoints();
    }

    /**
     * Begins the transaction of repositioning a point. The will copy the data of the point and
     * apply it player, such as position, yaw, pitch, etc. Path point is later updated by creating
     * a new path point.
     *
     * @param point the path point to reposition
     */
    public void reposition(PathPoint point)
    {
        point.copy(Minecraft.getInstance().player, this);
        this.editingPoint = point;
        this.repositioning = true;
        this.showMessage("Update the waypoint by creating a new waypoint");
    }

    /**
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
     * Deletes the entire path, simples!
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
                positionLengths[i] = this.positionInterpolator.length(i, i + 1);
                rotationLengths[i] = this.rotationInterpolator.length(i, i + 1);
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
                    p1.setPositionStep(j, this.positionInterpolator.progress(i, j * positionStep, positionLengths[i]));
                    p1.setRotationStep(j, this.rotationInterpolator.progress(i, j * rotationStep, rotationLengths[i]));
                }
            }
        }
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event)
    {
        if(this.isPlayerValidDirector() && event.getAction() == GLFW.GLFW_PRESS)
        {
            Minecraft mc = Minecraft.getInstance();
            if(KEY_BIND_POINT.matches(event.getKey(), event.getScanCode()))
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
            else if(KEY_BIND_PLAY.matches(event.getKey(), event.getScanCode()))
            {
                this.play();
            }
            else if(KEY_BIND_STOP.matches(event.getKey(), event.getScanCode()))
            {
                this.stop();
            }
            else if(KEY_BIND_SETTINGS.matches(event.getKey(), event.getScanCode()))
            {
                Minecraft.getInstance().setScreen(new PathMenuScreen());
            }
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event)
    {
        if(event.phase != TickEvent.Phase.START)
        {
            return;
        }

        if(this.isPlaying() && !this.isPlayerValidDirector())
        {
            this.stop();
            return;
        }

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

    /**
     * Shows a formatted message to the player. Uses the game bar to display the message.
     *
     * @param message the message
     */
    private void showMessage(String message)
    {
        Minecraft.getInstance().player.displayClientMessage(new TranslatableComponent("director.format.message", message), true);
    }

    /**
     * Shows a formatted value with the ability to set the prefix to the player. Uses the game bar
     * to display the message.
     *
     * @param prefix the prefix
     * @param value  the value
     */
    private void showValue(String prefix, String value)
    {
        Minecraft.getInstance().player.displayClientMessage(new TranslatableComponent("director.format.value", prefix, value), true);
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
            return Mth.clamp(s1 + (s2 - s1) * partialTicks, 0.0F, 1.0F);
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
            return Mth.clamp(s1 + (s2 - s1) * partialTicks, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(event.phase == TickEvent.Phase.END)
        {
            return;
        }

        if(this.isPlaying())
        {
            /* Stop playing if player suddenly can't play during sequence */
            if(!this.isPlayerValidDirector())
            {
                this.stop();
                return;
            }

            float positionProgress = this.getPositionProgress(event.renderTickTime);
            float rotationProgress = this.getRotationProgress(event.renderTickTime);

            /* Updated the position of the player */
            Vec3 pos = this.positionInterpolator.pos(this.currentPointIndex, positionProgress);
            LocalPlayer player = Minecraft.getInstance().player;
            player.setPos(pos.x, pos.y, pos.z);
            player.xo = pos.x;
            player.yo = pos.y;
            player.zo = pos.z;

            /* Updated the pitch of the player */
            float pitch = this.rotationInterpolator.pitch(this.currentPointIndex, rotationProgress);
            player.setXRot(pitch);
            player.xRotO = pitch;

            /* Updated the yaw of the player */
            float yaw = this.rotationInterpolator.yaw(this.currentPointIndex, rotationProgress);
            player.setYRot(yaw);
            player.yRotO = yaw;
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event)
    {
        if(this.isPlaying())
        {
            float progress = this.getRotationProgress((float) event.getPartialTicks());
            float roll = this.rotationInterpolator.roll(this.currentPointIndex, progress);
            event.setRoll(roll);
        }
        else
        {
            event.setRoll((float) (this.prevRoll + (this.roll - this.prevRoll) * event.getPartialTicks()));
        }
    }

    @SubscribeEvent
    public void fov(EntityViewRenderEvent.FieldOfView event)
    {
        if(this.isPlaying())
        {
            float progress = this.getRotationProgress((float) event.getPartialTicks());
            double fov = this.rotationInterpolator.fov(this.currentPointIndex, progress);
            event.setFOV(fov);
        }
        else
        {
            double fov = this.prevFov + (this.fov - this.prevFov) * event.getPartialTicks();
            event.setFOV(Minecraft.getInstance().options.fov + fov);
        }
    }

    @SubscribeEvent
    public void renderWorld(RenderLevelLastEvent event)
    {
        if(this.isPlaying() || !this.isVisible() || !this.isPlayerValidDirector())
        {
            return;
        }

        PoseStack matrixStack = event.getPoseStack();
        matrixStack.pushPose();

        Minecraft mc = Minecraft.getInstance();
        Vec3 view = mc.gameRenderer.getMainCamera().getPosition();
        matrixStack.translate(-view.x(), -view.y(), -view.z());

        AABB pointBox = new AABB(-0.10, -0.10, -0.10, 0.10, 0.10, 0.10);
        MultiBufferSource.BufferSource renderTypeBuffer = mc.renderBuffers().bufferSource();
        VertexConsumer builder = renderTypeBuffer.getBuffer(RenderType.lines());
        Matrix4f lastMatrix = matrixStack.last().pose();
        for(int i = 0; i < this.points.size() - 1; i++)
        {
            PathPoint p1 = this.points.get(i);
            for(int j = 0; j <= p1.getStepCount(); j++)
            {
                Vec3 v1 = this.positionInterpolator.pos(i, j == 0 ? 0.0F : p1.getPositionStep(j - 1));
                Vec3 v2 = this.positionInterpolator.pos(i, j == p1.getStepCount() ? 1.0F : p1.getPositionStep(j));
                builder.vertex(lastMatrix, (float) v1.x(), (float) v1.y(), (float) v1.z()).color(1.0F, 1.0F, 1.0F, 1.0F).normal(matrixStack.last().normal(), 1.0F, 0.0F, 0.0F).endVertex();
                builder.vertex(lastMatrix, (float) v2.x(), (float) v2.y(), (float) v2.z()).color(1.0F, 1.0F, 1.0F, 1.0F).normal(matrixStack.last().normal(), 1.0F, 0.0F, 0.0F).endVertex();

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
        AABB pathBox = new AABB(-halfBoxSize, -halfBoxSize, -halfBoxSize, halfBoxSize, halfBoxSize, halfBoxSize);
        for(int i = 0; i < this.points.size(); i++)
        {
            PathPoint p1 = this.points.get(i);
            matrixStack.pushPose();
            matrixStack.translate(p1.getX(), p1.getY(), p1.getZ());
            float[] color = this.getPointColor(i);
            LevelRenderer.renderLineBox(matrixStack, builder, pathBox, color[0], color[1], color[2], 1.0F);
            matrixStack.popPose();
        }
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.lines());

        matrixStack.popPose();
    }

    @SubscribeEvent
    public void renderEntity(RenderLivingEvent.Pre event)
    {
        if(!this.isPlayerValidDirector())
        {
            return;
        }

        /* Stops invisible armor stands from rendering while playing a path */
        if(this.isPlaying() && event.getEntity() instanceof ArmorStand)
        {
            try
            {
                EntityRenderUtil.renderAmourStand((ArmorStand) event.getEntity(), event.getRenderer(), event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), event.getPartialTick());
            }
            catch(InvocationTargetException | IllegalAccessException e)
            {
                e.printStackTrace();
            }
            event.setCanceled(true);
        }
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
        if(!this.isPlayerValidDirector())
        {
            return;
        }

        long windowId = Minecraft.getInstance().getWindow().getWindow();
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
        if(mc.getOverlay() != null || mc.screen != null || !mc.mouseHandler.isMouseGrabbed() || !this.isPlayerValidDirector())
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
                mc.setScreen(new EditPointScreen(hoveredPathPoint));
                event.setCanceled(true);
            }
        }

        if(event.getAction() == GLFW.GLFW_PRESS && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE)
        {
            long windowId = Minecraft.getInstance().getWindow().getWindow();
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

    /**
     * Gets the path point the player is currently hovering with their crosshair
     *
     * @return the hovered path point or null if not looking at anything
     */
    @Nullable
    private static PathPoint getHoveredPathPoint()
    {
        PathManager manager = PathManager.instance();
        if(manager.isPlaying() || !manager.isVisible() || !manager.isPlayerValidDirector())
        {
            return null;
        }

        Minecraft mc = Minecraft.getInstance();

        /* Setup the start and end vec of the ray trace */
        double reachDistance = mc.player.getAttribute(ForgeMod.REACH_DISTANCE.get()).getValue();
        Vec3 startVec = mc.player.getEyePosition(mc.getFrameTime());
        Vec3 endVec = startVec.add(mc.player.getLookAngle().scale(reachDistance));

        /* Creates axis aligned boxes for all path points then remove ones that aren't close enough to the player */
        double halfBoxSize = POINT_BOX_SIZE / 2;
        List<Pair<PathPoint, AABB>> pointPairs = PathManager.instance().points.stream().map(p -> Pair.of(p, new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + POINT_BOX_SIZE, p.getY() + POINT_BOX_SIZE, p.getZ() + POINT_BOX_SIZE).move(-halfBoxSize, -halfBoxSize, -halfBoxSize))).collect(Collectors.toList());
        pointPairs.removeIf(pair -> pair.getRight().getCenter().distanceTo(startVec) > reachDistance + 1);

        /* Ray trace and instance the closest path point */
        double closestDistance = Double.MAX_VALUE;
        PathPoint closestPoint = null;
        for(Pair<PathPoint, AABB> pair : pointPairs)
        {
            Optional<Vec3> optional = pair.getRight().clip(startVec, endVec);
            if(optional.isPresent())
            {
                double distance = startVec.distanceToSqr(optional.get());
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
