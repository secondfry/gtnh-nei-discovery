package com.secondfry.mc.neidiscovery;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(
    modid = "neidiscovery",
    name = "NEI Discovery",
    version = "0.1.2",
    dependencies = "required-after:NotEnoughItems@[2.8.171-GTNH,)",
    acceptableRemoteVersions = "*")
public final class DiscoveryMod {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (event.getSide()
            .isClient())
            FMLCommonHandler.instance()
                .bus()
                .register(DiscoveryClient.INSTANCE);
    }
}
