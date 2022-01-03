package com.mrcrayfish.director.client.renderer.entity;

import com.mrcrayfish.director.client.renderer.entity.model.DirectorArmorStandModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class DirectorArmorStandRenderer extends ArmorStandRenderer
{
    public DirectorArmorStandRenderer(EntityRendererProvider.Context context)
    {
        super(context);
        this.model = new DirectorArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND));
    }
}
