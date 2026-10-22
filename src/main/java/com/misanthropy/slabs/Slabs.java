package com.misanthropy.slabs;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mod(Slabs.MODID)
public class Slabs {
    public static final String MODID = "slabs";

    public static final List<String> SNOWY = List.of("grass_block", "podzol", "mycelium");
    public static final List<String> PLAIN = List.of(
            "dirt", "coarse_dirt", "rooted_dirt", "mud", "packed_mud", "clay", "moss_block",
            "sand", "red_sand", "gravel", "soul_sand", "soul_soil",
            "calcite", "tuff", "dripstone_block", "deepslate", "basalt", "smooth_basalt",
            "netherrack", "end_stone", "packed_ice", "blue_ice",
            "terracotta", "white_terracotta", "orange_terracotta", "magenta_terracotta",
            "light_blue_terracotta", "yellow_terracotta", "lime_terracotta", "pink_terracotta",
            "gray_terracotta", "light_gray_terracotta", "cyan_terracotta", "purple_terracotta",
            "blue_terracotta", "brown_terracotta", "green_terracotta", "red_terracotta",
            "black_terracotta");
    public static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("stone", "stone_slab"), Map.entry("cobblestone", "cobblestone_slab"),
            Map.entry("mossy_cobblestone", "mossy_cobblestone_slab"), Map.entry("andesite", "andesite_slab"),
            Map.entry("diorite", "diorite_slab"), Map.entry("granite", "granite_slab"),
            Map.entry("sandstone", "sandstone_slab"), Map.entry("red_sandstone", "red_sandstone_slab"),
            Map.entry("smooth_sandstone", "smooth_sandstone_slab"),
            Map.entry("smooth_red_sandstone", "smooth_red_sandstone_slab"),
            Map.entry("blackstone", "blackstone_slab"), Map.entry("cobbled_deepslate", "cobbled_deepslate_slab"));

    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, MODID);
    public static final Map<String, RegistryObject<Block>> SLABS = new LinkedHashMap<>();

    static {
        SNOWY.forEach(name -> add(name, true));
        PLAIN.forEach(name -> add(name, false));
        FEATURES.register("lips", LipFeature::new);
    }

    public Slabs() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FEATURES.register(bus);
        bus.addListener(this::tab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SlabsConfig.SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SlabsClient.init(bus));
    }

    public static String slabName(String source) {
        return source.replace("_block", "") + "_slab";
    }

    private static void add(String source, boolean snowy) {
        String id = slabName(source);
        RegistryObject<Block> block = BLOCKS.register(id, () -> {
            BlockBehaviour.Properties p = BlockBehaviour.Properties.copy(BuiltInRegistries.BLOCK.get(new ResourceLocation(source)));
            return snowy ? new SnowySlab(p) : new SlabBlock(p);
        });
        ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties()));
        SLABS.put(source, block);
    }

    private void tab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            SLABS.values().forEach(event::accept);
        }
    }

    private static volatile Map<Block, Block> terrain;

    public static Map<Block, Block> terrain() {
        Map<Block, Block> map = terrain;
        if (map == null) {
            map = new HashMap<>();
            for (Map.Entry<String, RegistryObject<Block>> e : SLABS.entrySet()) {
                map.put(BuiltInRegistries.BLOCK.get(new ResourceLocation(e.getKey())), e.getValue().get());
            }
            for (Map.Entry<String, String> e : VANILLA.entrySet()) {
                map.put(BuiltInRegistries.BLOCK.get(new ResourceLocation(e.getKey())), BuiltInRegistries.BLOCK.get(new ResourceLocation(e.getValue())));
            }
            terrain = map;
        }
        return map;
    }
}
