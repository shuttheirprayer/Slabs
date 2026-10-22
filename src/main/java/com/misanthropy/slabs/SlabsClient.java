package com.misanthropy.slabs;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.GrassColor;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

public class SlabsClient {
    public static void init(IEventBus bus) {
        bus.addListener(SlabsClient::blocks);
        bus.addListener(SlabsClient::items);
    }

    private static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos)
                : GrassColor.getDefaultColor(), Slabs.SLABS.get("grass_block").get());
    }

    private static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> GrassColor.get(0.5, 1.0), Slabs.SLABS.get("grass_block").get());
    }
}
