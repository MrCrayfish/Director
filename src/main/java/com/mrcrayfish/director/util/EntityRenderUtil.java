package com.mrcrayfish.director.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmorStandModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Author: MrCrayfish
 */
public class EntityRenderUtil
{
    private static Method handleRotationFloatMethod = null;
    private static Method applyRotationsMethod = null;
    private static Method preRenderCallbackMethod = null;
    private static Field layerRenderersField = null;

    /**
     * Initialises the reflection and makes methods and fields accessible
     */
    private static void initReflection()
    {
        handleRotationFloatMethod = ObfuscationReflectionHelper.findMethod(LivingEntityRenderer.class, "getBob", LivingEntity.class, float.class);
        handleRotationFloatMethod.setAccessible(true);
        applyRotationsMethod = ObfuscationReflectionHelper.findMethod(LivingEntityRenderer.class, "setupRotations", LivingEntity.class, PoseStack.class, float.class, float.class, float.class);
        applyRotationsMethod.setAccessible(true);
        preRenderCallbackMethod = ObfuscationReflectionHelper.findMethod(LivingEntityRenderer.class, "scale", LivingEntity.class, PoseStack.class, float.class);
        preRenderCallbackMethod.setAccessible(true);
        layerRenderersField = ObfuscationReflectionHelper.findField(LivingEntityRenderer.class, "layers");
        layerRenderersField.setAccessible(true);
    }

    /**
     * A custom method to handle the rendering of armor stands. This stops the armor stand model
     * from rendering and still allows the layers to be rendered. This allows for hand items to be
     * rendered into the world without a translucent armor stand showing in spectator mode.
     *
     * @param entityIn     an armor stand entity to render
     * @param renderer     the armor stands model instance
     * @param matrixStack  the current matrix stack
     * @param buffer       a buffer instance
     * @param packedLight  the packed light of the armor stand
     * @param partialTicks partial ticks
     * @throws InvocationTargetException if it can't invoke a method
     * @throws IllegalAccessException    if it can't access one of the reflected methods or fields
     */
    @SuppressWarnings("unchecked")
    public static void renderAmourStand(ArmorStand entityIn, LivingEntityRenderer<ArmorStand, ArmorStandModel> renderer, PoseStack matrixStack, MultiBufferSource buffer, int packedLight, float partialTicks) throws InvocationTargetException, IllegalAccessException
    {
        initReflection();

        ArmorStandModel model = renderer.getModel();

        matrixStack.pushPose();
        model.attackTime = entityIn.getAttackAnim(partialTicks);

        boolean shouldSit = entityIn.isPassenger() && (entityIn.getVehicle() != null && entityIn.getVehicle().shouldRiderSit());
        model.riding = shouldSit;
        model.young = entityIn.isBaby();
        float yawOffset = Mth.rotLerp(partialTicks, entityIn.yBodyRotO, entityIn.yBodyRot);
        float yawHead = Mth.rotLerp(partialTicks, entityIn.yHeadRotO, entityIn.yHeadRot);
        float deltaYaw = yawHead - yawOffset;
        if(shouldSit && entityIn.getVehicle() instanceof LivingEntity)
        {
            LivingEntity livingentity = (LivingEntity) entityIn.getVehicle();
            yawOffset = Mth.rotLerp(partialTicks, livingentity.yBodyRotO, livingentity.yBodyRot);
            deltaYaw = yawHead - yawOffset;

            float wrappedDegrees = Mth.wrapDegrees(deltaYaw);
            if(wrappedDegrees < -85.0F)
            {
                wrappedDegrees = -85.0F;
            }
            if(wrappedDegrees >= 85.0F)
            {
                wrappedDegrees = 85.0F;
            }

            yawOffset = yawHead - wrappedDegrees;
            if(wrappedDegrees * wrappedDegrees > 2500.0F)
            {
                yawOffset += wrappedDegrees * 0.2F;
            }

            deltaYaw = yawHead - yawOffset;
        }

        float f6 = Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot());
        if(entityIn.getPose() == Pose.SLEEPING)
        {
            Direction direction = entityIn.getBedOrientation();
            if(direction != null)
            {
                float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
                matrixStack.translate((double) ((float) (-direction.getStepX()) * f4), 0.0D, (double) ((float) (-direction.getStepZ()) * f4));
            }
        }

        float f7 = (float) handleRotationFloatMethod.invoke(renderer, entityIn, partialTicks);
        applyRotationsMethod.invoke(renderer, entityIn, matrixStack, f7, yawOffset, partialTicks);
        matrixStack.scale(-1.0F, -1.0F, 1.0F);
        preRenderCallbackMethod.invoke(renderer, entityIn, matrixStack, partialTicks);
        matrixStack.translate(0.0D, (double) -1.501F, 0.0D);
        float f8 = 0.0F;
        float f5 = 0.0F;
        if(!shouldSit && entityIn.isAlive())
        {
            f8 = Mth.lerp(partialTicks, entityIn.animationSpeedOld, entityIn.animationSpeed);
            f5 = entityIn.animationPosition - entityIn.animationSpeed * (1.0F - partialTicks);
            if(entityIn.isBaby())
            {
                f5 *= 3.0F;
            }

            if(f8 > 1.0F)
            {
                f8 = 1.0F;
            }
        }

        model.prepareMobModel(entityIn, f5, f8, partialTicks);
        model.setupAnim(entityIn, f5, f8, f7, deltaYaw, f6);

        List<RenderLayer<ArmorStand, ArmorStandModel>> layers = (List<RenderLayer<ArmorStand, ArmorStandModel>>) layerRenderersField.get(renderer);
        if(!entityIn.isSpectator())
        {
            for(RenderLayer<ArmorStand, ArmorStandModel> layer : layers)
            {
                layer.render(matrixStack, buffer, packedLight, entityIn, f5, f8, partialTicks, f7, deltaYaw, f6);
            }
        }

        matrixStack.popPose();
    }
}
