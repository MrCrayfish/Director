package com.mrcrayfish.director.path;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Author: MrCrayfish
 */
public interface IProperties extends INBTSerializable<CompoundNBT>
{
    default CompoundNBT serializeNBT()
    {
        return new CompoundNBT();
    }

    default void deserializeNBT(CompoundNBT tag)
    {

    }
}
