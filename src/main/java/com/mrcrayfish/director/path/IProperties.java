package com.mrcrayfish.director.path;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Author: MrCrayfish
 */
public interface IProperties extends INBTSerializable<CompoundTag>
{
    default CompoundTag serializeNBT()
    {
        return new CompoundTag();
    }

    default void deserializeNBT(CompoundTag tag)
    {

    }
}
