package com.mrcrayfish.director;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeBuffers;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

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
    private HermiteInterpolator interpolator;
    private int currentPointIndex;
    private int remainingPointDuration;
    private boolean playing;
    private double roll;

    private PathManager() {}

    public void play()
    {
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

    public void stop()
    {
        this.playing = false;
    }

    @SubscribeEvent
    public void onKeyPress(InputEvent.KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.world != null && mc.player != null && event.getAction() == GLFW.GLFW_PRESS)
        {
            if(event.getKey() == GLFW.GLFW_KEY_P) //Add new point
            {
                this.points.add(new PathPoint(mc.player));
                this.interpolator = new HermiteInterpolator(this.points);
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
        if(event.phase != TickEvent.Phase.END)
            return;

        if(this.playing)
        {
            if(this.remainingPointDuration > 0)
            {
                this.remainingPointDuration--;
            }
            else
            {
                if(this.currentPointIndex < this.points.size() - 1)
                {
                    this.currentPointIndex++;
                    this.remainingPointDuration = this.points.get(this.currentPointIndex).getDuration() - 1;
                }
                else
                {
                    this.stop();
                }
            }
        }
        else
        {
            long windowId = Minecraft.getInstance().getMainWindow().getHandle();
            if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_LEFT_BRACKET) == GLFW.GLFW_PRESS)
            {
                this.roll -= 0.5;
            }
            else if(GLFW.glfwGetKey(windowId, GLFW.GLFW_KEY_RIGHT_BRACKET) == GLFW.GLFW_PRESS)
            {
                this.roll += 0.5;
            }
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event)
    {
        if(this.playing && this.currentPointIndex < this.points.size() - 1)
        {
            PathPoint currentPoint = this.points.get(this.currentPointIndex);
            float progress = 1.0F - ((float) this.remainingPointDuration - event.renderTickTime) / (float) currentPoint.getDuration();
            Vec3d pos = this.interpolator.get(this.currentPointIndex, progress);
            ClientPlayerEntity player = Minecraft.getInstance().player;
            player.setPosition(pos.x, pos.y, pos.z);
            player.prevPosX = pos.x;
            player.prevPosY = pos.y;
            player.prevPosZ = pos.z;
        }
    }

    @SubscribeEvent
    public void camera(EntityViewRenderEvent.CameraSetup event)
    {
        event.setRoll((float) this.roll);
    }

    @SubscribeEvent
    public void renderWorld(RenderWorldLastEvent event)
    {
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
            Minecraft.getInstance().getItemRenderer().renderItem(snowball, ItemCameraTransforms.TransformType.GROUND, 0, 0, matrixStack, renderTypeBuffer);
            matrixStack.pop();
        }
        Minecraft.getInstance().getRenderTypeBuffers().getBufferSource().finish(renderType);

        matrixStack.pop();
    }
}
