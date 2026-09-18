package com.talhanation.smallships.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntUnaryOperator;

/** Pure table selection, independent of entity spawning and item registries. */
public final class GhostLootTable {
    private GhostLootTable() {}

    public static void roll(List<GhostShipsConfig.Pool> pools, IntUnaryOperator nextInt,
                            DoubleSupplier nextDouble, Consumer<GhostShipsConfig.Stack> reward) {
        for (var pool : pools) {
            if (nextDouble.getAsDouble() >= pool.chance) continue;
            var candidates = new ArrayList<>(pool.entries);
            int rolls = pool.rollsMin + nextInt.applyAsInt(pool.rollsMax - pool.rollsMin + 1);
            for (int i = 0; i < rolls && !candidates.isEmpty(); i++) {
                int total = candidates.stream().mapToInt(entry -> entry.weight).sum();
                int choice = nextInt.applyAsInt(total);
                GhostShipsConfig.Entry selected = candidates.get(0);
                for (var entry : candidates) {
                    choice -= entry.weight;
                    if (choice < 0) { selected = entry; break; }
                }
                if (pool.uniqueEntries) candidates.remove(selected);
                // A failed chance consumes the roll; a bundle succeeds or fails together.
                if (nextDouble.getAsDouble() >= selected.chance) continue;
                selected.items.forEach(reward);
            }
        }
    }
}
