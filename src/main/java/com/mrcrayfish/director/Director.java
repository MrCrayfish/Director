package com.mrcrayfish.director;

import com.mrcrayfish.director.path.PathManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod("director")
public class Director
{
    public Director()
    {
        MinecraftForge.EVENT_BUS.register(PathManager.get());
    }
}
