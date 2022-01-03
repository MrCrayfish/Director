package com.mrcrayfish.director.client.renderer.entity;

import com.mrcrayfish.director.client.renderer.entity.model.DirectorArmorStandModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Author: MrCrayfish
 */
@OnlyIn(Dist.CLIENT)
public class DirectorArmorStandRenderer extends ArmorStandRenderer
{
    public DirectorArmorStandRenderer(EntityRendererManager manager)
    {
        super(manager);
        this.model = new DirectorArmorStandModel();
    }
}
