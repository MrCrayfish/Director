package com.mrcrayfish.director;

import com.mrcrayfish.director.client.renderer.entity.DirectorArmorStandRenderer;
import com.mrcrayfish.director.path.PathManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Author: MrCrayfish
 */
@Mod(Director.ID)
public class Director
{
    public static final String ID = "director";

    public Director()
    {
        MinecraftForge.EVENT_BUS.register(PathManager.instance());
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::onClientSetup);
    }

    private void onClientSetup(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(EntityType.ARMOR_STAND, DirectorArmorStandRenderer::new);
    }
}
