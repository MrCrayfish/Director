package com.mrcrayfish.director.client.renderer.entity.model;

import com.mrcrayfish.director.path.PathManager;
import net.minecraft.client.model.ArmorStandModel;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Collections;

/**
 * Author: MrCrayfish
 */
public class DirectorArmorStandModel extends ArmorStandModel
{
    public DirectorArmorStandModel(ModelPart part)
    {
        super(part);
    }

    @Override
    protected Iterable<ModelPart> headParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.headParts();
    }

    @Override
    protected Iterable<ModelPart> bodyParts()
    {
        return PathManager.instance().isPlaying() ? Collections::emptyIterator : super.bodyParts();
    }
}
