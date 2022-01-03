package com.mrcrayfish.director.client.renderer.entity.model;

import com.mrcrayfish.director.path.PathManager;
import net.minecraft.client.renderer.entity.model.ArmorStandModel;
import net.minecraft.client.renderer.model.ModelRenderer;

import java.util.Collections;

/**
 * Author: MrCrayfish
 */
public class DirectorArmorStandModel extends ArmorStandModel
{
    @Override
    protected Iterable<ModelRenderer> getHeadParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.getHeadParts();
    }

    @Override
    protected Iterable<ModelRenderer> getBodyParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.getBodyParts();
    }
}
