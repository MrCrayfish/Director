package com.mrcrayfish.director.util;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.ArmorStandModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

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
        handleRotationFloatMethod = ObfuscationReflectionHelper.findMethod(LivingRenderer.class, "func_77044_a", LivingEntity.class, float.class);
        handleRotationFloatMethod.setAccessible(true);
        applyRotationsMethod = ObfuscationReflectionHelper.findMethod(LivingRenderer.class, "func_225621_a_", LivingEntity.class, MatrixStack.class, float.class, float.class, float.class);
        applyRotationsMethod.setAccessible(true);
        preRenderCallbackMethod = ObfuscationReflectionHelper.findMethod(LivingRenderer.class, "func_225620_a_", LivingEntity.class, MatrixStack.class, float.class);
        preRenderCallbackMethod.setAccessible(true);
        layerRenderersField = ObfuscationReflectionHelper.findField(LivingRenderer.class, "field_177097_h");
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
    public static void renderAmourStand(ArmorStandEntity entityIn, LivingRenderer<ArmorStandEntity, ArmorStandModel> renderer, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, float partialTicks) throws InvocationTargetException, IllegalAccessException
    {
        initReflection();

        ArmorStandModel model = renderer.getEntityModel();

        matrixStack.push();
        model.swingProgress = entityIn.getSwingProgress(partialTicks);

        boolean shouldSit = entityIn.isPassenger() && (entityIn.getRidingEntity() != null && entityIn.getRidingEntity().shouldRiderSit());
        model.isSitting = shouldSit;
        model.isChild = entityIn.isChild();
        float yawOffset = MathHelper.interpolateAngle(partialTicks, entityIn.prevRenderYawOffset, entityIn.renderYawOffset);
        float yawHead = MathHelper.interpolateAngle(partialTicks, entityIn.prevRotationYawHead, entityIn.rotationYawHead);
        float deltaYaw = yawHead - yawOffset;
        if(shouldSit && entityIn.getRidingEntity() instanceof LivingEntity)
        {
            LivingEntity livingentity = (LivingEntity) entityIn.getRidingEntity();
            yawOffset = MathHelper.interpolateAngle(partialTicks, livingentity.prevRenderYawOffset, livingentity.renderYawOffset);
            deltaYaw = yawHead - yawOffset;

            float wrappedDegrees = MathHelper.wrapDegrees(deltaYaw);
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

        float f6 = MathHelper.lerp(partialTicks, entityIn.prevRotationPitch, entityIn.rotationPitch);
        if(entityIn.getPose() == Pose.SLEEPING)
        {
            Direction direction = entityIn.getBedDirection();
            if(direction != null)
            {
                float f4 = entityIn.getEyeHeight(Pose.STANDING) - 0.1F;
                matrixStack.translate((double) ((float) (-direction.getXOffset()) * f4), 0.0D, (double) ((float) (-direction.getZOffset()) * f4));
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
            f8 = MathHelper.lerp(partialTicks, entityIn.prevLimbSwingAmount, entityIn.limbSwingAmount);
            f5 = entityIn.limbSwing - entityIn.limbSwingAmount * (1.0F - partialTicks);
            if(entityIn.isChild())
            {
                f5 *= 3.0F;
            }

            if(f8 > 1.0F)
            {
                f8 = 1.0F;
            }
        }

        model.setLivingAnimations(entityIn, f5, f8, partialTicks);
        model.setRotationAngles(entityIn, f5, f8, f7, deltaYaw, f6);

        List<LayerRenderer<ArmorStandEntity, ArmorStandModel>> layers = (List<LayerRenderer<ArmorStandEntity, ArmorStandModel>>) layerRenderersField.get(renderer);
        if(!entityIn.isSpectator())
        {
            for(LayerRenderer<ArmorStandEntity, ArmorStandModel> layer : layers)
            {
                layer.render(matrixStack, buffer, packedLight, entityIn, f5, f8, partialTicks, f7, deltaYaw, f6);
            }
        }

        matrixStack.pop();
    }
}
