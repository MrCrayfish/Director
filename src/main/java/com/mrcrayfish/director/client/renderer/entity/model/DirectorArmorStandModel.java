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
    protected Iterable<ModelRenderer> headParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.headParts();
    }

    @Override
    protected Iterable<ModelRenderer> bodyParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.bodyParts();
    }
}
