package com.misanthropy.slabs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionCoverageTest {
    static double measure(long seed, double coverage, int size) {
        Regions r = Regions.of(seed, coverage);
        int n = 1000, hits = 0;
        int step = size / 8;
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n; ++j) {
                if (r.in(-3_000_000 + i * step + 3, 7_000_000 + j * step + 5, size)) {
                    ++hits;
                }
            }
        }
        return hits / (double) (n * n);
    }

    @Test
    void coverageMatchesConfig() {
        for (long seed : new long[]{0L, 12345L, -987654321L}) {
            for (double coverage : new double[]{0.005, 0.01, 0.03, 0.1, 0.5}) {
                double measured = measure(seed, coverage, 384);
                System.out.printf("seed %d coverage %.3f -> measured %.4f%n", seed, coverage, measured);
                assertEquals(coverage, measured, Math.max(0.005, coverage * 0.25));
            }
        }
    }

    @Test
    void extremes() {
        assertEquals(0.0, measure(1L, 0.0, 384));
        assertEquals(1.0, measure(1L, 1.0, 384));
    }

    @Test
    void regionsAreBlobsNotNoise() {
        Regions r = Regions.of(42L, 0.05);
        int inside = 0, neighbourAlsoInside = 0;
        for (int x = 0; x < 200_000; x += 7) {
            for (int z = 0; z < 2_000; z += 7) {
                if (r.in(x, z, 384)) {
                    ++inside;
                    if (r.in(x + 1, z, 384)) {
                        ++neighbourAlsoInside;
                    }
                }
            }
        }
        assertTrue(inside > 0);
        assertTrue(neighbourAlsoInside / (double) inside > 0.98);
    }
}
