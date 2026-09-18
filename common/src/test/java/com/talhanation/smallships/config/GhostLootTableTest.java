package com.talhanation.smallships.config;

import java.util.*;

/** Deterministic regression tests; no game world or additional test dependency needed. */
public final class GhostLootTableTest {
    public static void main(String[] args) {
        var root = com.google.gson.JsonParser.parseString(
                "{\"ships\":{\"cog\":{\"health\":777,\"lootPools\":[]}},\"custom\":42}").getAsJsonObject();
        var previousShips = root.get("ships").deepCopy();
        var legacy = com.google.gson.JsonParser.parseString(
                "{\"pirateShipsEnabled\":false,\"pirateShipsSpawnInterval\":100,\"pirateShipsSpawnChance\":0,"
                        + "\"pirateShipsMaxNearby\":0,\"pirateShipsRequiredCannonBalls\":15,\"pirateShipsDetectionRange\":120}").getAsJsonObject();
        GhostShipsConfig.addMigratedGeneral(root, legacy);
        check(root.get("ships").equals(previousShips), "migration preserves existing profiles and empty loot");
        check(root.get("custom").getAsInt() == 42, "migration preserves unknown fields");
        var general = root.getAsJsonObject("general");
        check(!general.get("enabled").getAsBoolean() && general.get("spawnChance").getAsDouble() == 0,
                "migration preserves disabled spawns");
        check(general.get("spawnIntervalTicks").getAsInt() == 100
                && general.get("maxNearby").getAsInt() == 0
                && general.get("requiredCannonBalls").getAsInt() == 15
                && general.get("detectionRange").getAsDouble() == 120, "migration maps all spawn settings");
        var snapshot = root.deepCopy();
        GhostShipsConfig.addMigratedGeneral(root, new com.google.gson.JsonObject());
        check(root.equals(snapshot), "migration is idempotent and JSON takes priority");
        var pool = new GhostShipsConfig.Pool();
        pool.rollsMin = pool.rollsMax = 3;
        var first = entry("iron", 3, 1);
        pool.entries.add(first);
        check(roll(pool).size() == 3, "fixed roll count");
        pool.chance = 0;
        check(roll(pool).isEmpty(), "disabled pool");
        pool.chance = 1;
        first.chance = 0;
        check(roll(pool).isEmpty(), "failed entries consume their rolls");
        first.chance = 1;
        first.items.add(stack("emerald"));
        check(roll(pool).size() == 6, "multi-item bundles");
        pool.uniqueEntries = true;
        check(roll(pool).size() == 2, "unique entry and exhausted candidates");
        pool.entries.clear();
        check(roll(pool).isEmpty(), "empty pool");
        pool.entries.add(entry("iron", 3, 1));
        pool.entries.add(entry("diamond", 1, 1));
        pool.uniqueEntries = false;
        pool.rollsMin = pool.rollsMax = 100000;
        var result = roll(pool);
        long iron = result.stream().filter(s -> s.item.equals("iron")).count();
        check(iron > 74000 && iron < 76000, "weighted selection 3:1");
        pool.rollsMin = pool.rollsMax = 0;
        check(roll(pool).isEmpty(), "zero rolls");
        pool.rollsMin = pool.rollsMax = 1;
        var output = new ArrayList<GhostShipsConfig.Stack>();
        var random = new Random(42);
        GhostLootTable.roll(List.of(pool, pool), random::nextInt, random::nextDouble, output::add);
        check(output.size() == 2, "independent pools");
        pool.rollsMin = 2;
        pool.rollsMax = 5;
        for (int i = 0; i < 100; i++) {
            output.clear();
            GhostLootTable.roll(List.of(pool), random::nextInt, random::nextDouble, output::add);
            check(output.size() >= 2 && output.size() <= 5, "inclusive roll bounds");
        }
        var config = new GhostShipsConfig();
        for (String type : List.of("cog", "galley", "brigg")) config.ships.put(type, new GhostShipsConfig.Profile());
        GhostShipsConfig.validate(config);
        config.ships.get("cog").health = Double.NaN;
        try {
            GhostShipsConfig.validate(config);
            throw new AssertionError("NaN health accepted");
        } catch (IllegalArgumentException expected) {}
        System.out.println("Ghost loot regression tests passed");
    }

    private static List<GhostShipsConfig.Stack> roll(GhostShipsConfig.Pool pool) {
        var output = new ArrayList<GhostShipsConfig.Stack>();
        var random = new Random(42);
        GhostLootTable.roll(List.of(pool), random::nextInt, random::nextDouble, output::add);
        return output;
    }
    private static GhostShipsConfig.Entry entry(String item, int weight, double chance) {
        var entry = new GhostShipsConfig.Entry();
        entry.weight = weight;
        entry.chance = chance;
        entry.items.add(stack(item));
        return entry;
    }
    private static GhostShipsConfig.Stack stack(String item) {
        var stack = new GhostShipsConfig.Stack();
        stack.item = item;
        return stack;
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
