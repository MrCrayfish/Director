package com.mrcrayfish.director;

import com.mrcrayfish.director.path.PathManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

/**
 * Author: MrCrayfish
 */
@Mod(Director.ID)
public class Director
{
    public static final String ID = "director";

    public Director()
    {
        MinecraftForge.EVENT_BUS.register(PathManager.get());
    }
}
