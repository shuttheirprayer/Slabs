package com.misanthropy.slabs;

import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

import java.util.Arrays;
import java.util.List;

record Regions(long seed, double coverage, PerlinSimplexNoise noise, double cutoff) {
    private static volatile Regions cached;

    static Regions of(long seed, double coverage) {
        Regions r = cached;
        if (r == null || r.seed != seed || r.coverage != coverage) {
            PerlinSimplexNoise noise = new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(seed ^ 0x8E610L)), List.of(0, 1, 2));
            r = new Regions(seed, coverage, noise, cutoff(noise, coverage));
            cached = r;
        }
        return r;
    }

    static double cutoff(PerlinSimplexNoise noise, double coverage) {
        if (coverage >= 1) {
            return Double.NEGATIVE_INFINITY;
        }
        if (coverage <= 0) {
            return Double.POSITIVE_INFINITY;
        }
        int n = 256;
        double[] samples = new double[n * n];
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                samples[i * n + j] = noise.getValue(i * 0.25, j * 0.25, false);
            }
        }
        Arrays.sort(samples);
        return samples[Math.min(samples.length - 1, (int) ((1 - coverage) * samples.length))];
    }

    boolean in(int x, int z, int size) {
        return noise.getValue(x / (double) size, z / (double) size, false) >= cutoff;
    }
}
