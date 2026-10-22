package com.misanthropy.slabs;

import net.minecraftforge.common.ForgeConfigSpec;

public class SlabsConfig {
    static final ForgeConfigSpec SPEC;
    static final ForgeConfigSpec.BooleanValue ENABLED;
    static final ForgeConfigSpec.DoubleValue COVERAGE;
    static final ForgeConfigSpec.IntValue SIZE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Natural terrain slabs. Changes only affect newly generated chunks.").push("worldgen");
        ENABLED = b.comment("Place slabs on the edges of hills, dunes and cliffs at all.")
                .define("enabled", true);
        COVERAGE = b.comment("Share of the world that turns into smooth slab terrain.",
                        "1.0 = everywhere, 0.05 = now and then, 0.01 = very rare, 0.0 = never.")
                .defineInRange("regionCoverage", 1.0, 0.0, 1.0);
        SIZE = b.comment("Rough width of one smooth region, in blocks.")
                .defineInRange("regionSize", 384, 32, 8192);
        b.pop();
        SPEC = b.build();
    }
}
