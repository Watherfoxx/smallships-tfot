package com.talhanation.smallships.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import com.talhanation.smallships.SmallShipsMod;
import com.talhanation.smallships.world.entity.ship.Ship;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Server-side config, loaded once. Restart to apply edits. Never overwrite invalid user data. */
public final class GhostShipsConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "smallships-ghosts.json");
    private static GhostShipsConfig loaded;
    private static final Path LEGACY = Path.of("config", "smallships-common.toml");
    private static final Path LEGACY_BACKUP = Path.of("config", "smallships-common.toml.before-ghost-migration.bak");
    private static JsonObject legacyValues = new JsonObject();
    private static Exception migrationError;
    public General general = new General();
    public static final class General {
        public boolean enabled = true;
        public int spawnIntervalTicks = 1200;
        public double spawnChance = 0.35;
        public int maxNearby = 2;
        public int requiredCannonBalls = 5;
        public double detectionRange = 72;
        public int spawnDistanceMin = 32;
        public int spawnDistanceMax = 48;
        public double cannonInaccuracy = 8;
    }

    public static synchronized General general() {
        if (loaded == null) load();
        return loaded.general;
    }

    /** Runs before Forge can remove old keys when correcting the common TOML. */
    public static synchronized void captureLegacyConfig() {
        try {
            Path source = Files.exists(LEGACY) ? LEGACY : LEGACY_BACKUP;
            if (!Files.exists(source)) return;
            Config config;
            try (Reader reader = Files.newBufferedReader(source)) {
                config = new TomlParser().parse(reader);
            }
            Config section = config.get(List.of("Ship", "General", "AI Pirate Ships"));
            if (section == null && Files.exists(LEGACY_BACKUP)) {
                try (Reader reader = Files.newBufferedReader(LEGACY_BACKUP)) {
                    section = new TomlParser().parse(reader).get(List.of("Ship", "General", "AI Pirate Ships"));
                }
            }
            if (section != null) {
                legacyValues = GSON.toJsonTree(section.valueMap()).getAsJsonObject();
                if (source.equals(LEGACY) && !Files.exists(LEGACY_BACKUP)) Files.copy(LEGACY, LEGACY_BACKUP);
            }
        } catch (Exception e) {
            migrationError = e;
            SmallShipsMod.LOGGER.error("Cannot preserve old ghost configuration", e);
        }
    }

    public Map<String, Profile> ships = new LinkedHashMap<>();

    public static final class Profile {
        public double health = 250;
        public double despawnSeconds = 30;
        public double combatIdleSeconds = 20;
        public double captainHealth = 40;
        public double crewHealth = 20;
        public int crewMin = 1;
        public int crewMax = 3;
        public List<Pool> lootPools = new ArrayList<>();
    }
    public static final class Pool {
        public String name = "cargo";
        public double chance = 1;
        public int rollsMin = 2;
        public int rollsMax = 4;
        public boolean uniqueEntries = false;
        public List<Entry> entries = new ArrayList<>();
    }
    public static final class Entry {
        public int weight = 1;
        public double chance = 1;
        public List<Stack> items = new ArrayList<>();
    }
    public static final class Stack {
        public String item = "minecraft:iron_ingot";
        public int min = 1;
        public int max = 3;
    }

    public static synchronized Profile forShip(Ship ship) {
        if (loaded == null) load();
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(ship.getType()).getPath();
        return loaded.ships.getOrDefault(key, loaded.ships.get("cog"));
    }

    public static synchronized void onServerStarting() {
        load();
    }

    private static void load() {
        try {
            JsonObject root;
            boolean changed;
            if (!Files.exists(FILE)) {
                if (migrationError != null) throw migrationError;
                GhostShipsConfig defaults = defaults();
                for (Profile profile : defaults.ships.values()) profile.lootPools.add(migrateLoot());
                root = GSON.toJsonTree(defaults).getAsJsonObject();
                root.remove("general");
                changed = true;
            } else {
                try (Reader reader = Files.newBufferedReader(FILE)) {
                    root = JsonParser.parseReader(reader).getAsJsonObject();
                }
                changed = !root.has("general");
            }
            if (!root.has("general")) {
                if (migrationError != null) throw migrationError;
                addMigratedGeneral(root, legacyValues);
            }
            if (root.get("general").isJsonObject()) {
                JsonObject general = root.getAsJsonObject("general");
                for (var entry : GSON.toJsonTree(new General()).getAsJsonObject().entrySet()) {
                    if (!general.has(entry.getKey())) {
                        general.add(entry.getKey(), entry.getValue());
                        changed = true;
                    }
                }
            }
            GhostShipsConfig candidate = GSON.fromJson(root, GhostShipsConfig.class);
            validate(candidate);
            if (changed) writeMigrated(root);
            loaded = candidate;
            SmallShipsMod.LOGGER.info("Loaded ghost ship configuration: {}", FILE.toAbsolutePath());
        } catch (Exception e) {
            SmallShipsMod.LOGGER.error("Invalid ghost config {}. File preserved; ghost spawning disabled.", FILE, e);
            loaded = defaults();
            loaded.general.enabled = false;
        }
    }

    static void addMigratedGeneral(JsonObject root, JsonObject legacy) {
        if (root.has("general")) return;
        JsonObject general = GSON.toJsonTree(new General()).getAsJsonObject();
        String[][] fields = {
                {"pirateShipsEnabled", "enabled"},
                {"pirateShipsSpawnInterval", "spawnIntervalTicks"},
                {"pirateShipsSpawnChance", "spawnChance"},
                {"pirateShipsMaxNearby", "maxNearby"},
                {"pirateShipsRequiredCannonBalls", "requiredCannonBalls"},
                {"pirateShipsDetectionRange", "detectionRange"}
        };
        for (String[] field : fields) if (legacy.has(field[0])) general.add(field[1], legacy.get(field[0]).deepCopy());
        root.add("general", general);
    }

    private static Pool migrateLoot() {
        Pool pool = new Pool();
        pool.rollsMin = legacyValues.has("pirateLootRollsMin") ? legacyValues.get("pirateLootRollsMin").getAsInt() : 6;
        pool.rollsMax = legacyValues.has("pirateLootRollsMax") ? legacyValues.get("pirateLootRollsMax").getAsInt() : 10;
        List<String> values = new ArrayList<>();
        if (legacyValues.has("pirateLoot")) {
            legacyValues.getAsJsonArray("pirateLoot").forEach(value -> values.add(value.getAsString()));
        } else {
            values.addAll(List.of("minecraft:oak_planks,8,24,18", "minecraft:iron_nugget,8,24,16",
                    "minecraft:gunpowder,2,6,14", "smallships:cannon_ball,2,6,14",
                    "minecraft:iron_ingot,2,6,14", "minecraft:gold_ingot,1,4,11",
                    "minecraft:emerald,1,3,8", "minecraft:diamond,1,1,5"));
        }
        for (String value : values) {
            String[] parts = value.split(",");
            if (parts.length != 4) throw new IllegalArgumentException("Invalid legacy ghost loot: " + value);
            Stack stack = new Stack();
            stack.item = parts[0].trim();
            stack.min = Integer.parseInt(parts[1].trim());
            stack.max = Integer.parseInt(parts[2].trim());
            Entry entry = new Entry();
            entry.weight = Integer.parseInt(parts[3].trim());
            entry.items.add(stack);
            pool.entries.add(entry);
        }
        return pool;
    }

    private static void writeMigrated(JsonObject root) throws Exception {
        Files.createDirectories(FILE.getParent());
        if (Files.exists(FILE)) {
            Path backup = FILE.resolveSibling("smallships-ghosts.before-migration-" + UUID.randomUUID() + ".json.bak");
            Files.copy(FILE, backup);
            SmallShipsMod.LOGGER.info("Ghost config backup: {}", backup);
        }
        Path temporary = Files.createTempFile(FILE.getParent(), "smallships-ghosts-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary)) { GSON.toJson(root, writer); }
            try {
                Files.move(temporary, FILE, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static GhostShipsConfig defaults() {
        GhostShipsConfig config = new GhostShipsConfig();
        for (String type : List.of("cog", "galley", "brigg")) {
            Profile profile = new Profile();
            profile.health = type.equals("brigg") ? 400 : type.equals("galley") ? 300 : 250;
            config.ships.put(type, profile);
        }
        return config;
    }

    private static void require(boolean valid) {
        if (!valid) throw new IllegalArgumentException("Invalid ghost config range or missing required field");
    }
    private static boolean probability(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 1;
    }
    static void validate(GhostShipsConfig config) {
        require(config != null && config.ships != null && config.general != null);
        General general = config.general;
        require(general.spawnIntervalTicks >= 100 && general.spawnIntervalTicks <= 72000);
        require(probability(general.spawnChance));
        require(general.maxNearby >= 0 && general.maxNearby <= 16);
        require(general.requiredCannonBalls >= 1 && general.requiredCannonBalls <= 4096);
        require(Double.isFinite(general.detectionRange) && general.detectionRange >= 8 && general.detectionRange <= 256);
        require(general.spawnDistanceMin >= 10 && general.spawnDistanceMax >= general.spawnDistanceMin && general.spawnDistanceMax <= 128);
        require(Double.isFinite(general.cannonInaccuracy) && general.cannonInaccuracy >= 0 && general.cannonInaccuracy <= 30);
        for (String type : List.of("cog", "galley", "brigg")) require(config.ships.containsKey(type));
        for (Profile profile : config.ships.values()) {
            require(profile != null && Double.isFinite(profile.health) && profile.health >= 1 && profile.health <= 1000000);
            require(Double.isFinite(profile.despawnSeconds) && profile.despawnSeconds >= 0 && profile.despawnSeconds <= 36000);
            require(Double.isFinite(profile.combatIdleSeconds) && profile.combatIdleSeconds >= 0 && profile.combatIdleSeconds <= 36000);
            require(Double.isFinite(profile.captainHealth) && profile.captainHealth >= 1 && profile.captainHealth <= 1024);
            require(Double.isFinite(profile.crewHealth) && profile.crewHealth >= 1 && profile.crewHealth <= 1024);
            require(profile.crewMin >= 0 && profile.crewMax >= profile.crewMin && profile.crewMax <= 12);
            require(profile.lootPools != null && profile.lootPools.size() <= 32);
            for (Pool pool : profile.lootPools) {
                require(pool != null && probability(pool.chance));
                require(pool.rollsMin >= 0 && pool.rollsMax >= pool.rollsMin && pool.rollsMax <= 128);
                require(pool.entries != null && pool.entries.size() <= 256);
                for (Entry entry : pool.entries) {
                    require(entry != null && entry.weight >= 1 && entry.weight <= 100000 && probability(entry.chance));
                    require(entry.items != null && entry.items.size() <= 16);
                    for (Stack stack : entry.items) {
                        require(stack != null && stack.item != null && stack.min >= 1 && stack.max >= stack.min && stack.max <= 4096);
                    }
                }
            }
        }
    }
}
