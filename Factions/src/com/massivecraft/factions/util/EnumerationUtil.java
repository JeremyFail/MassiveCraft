package com.massivecraft.factions.util;

import com.massivecraft.factions.entity.MConf;
import com.massivecraft.massivecore.collections.BackstringSet;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class EnumerationUtil
{
    // -------------------------------------------- //
    // MATERIAL EDIT ON INTERACT
    // -------------------------------------------- //
    
    public static final BackstringSet<Material> MATERIALS_EDIT_ON_INTERACT = new BackstringSet<>(Material.class,
        "BEEHIVE",                      // Minecraft 1.15
        "BEE_NEST",                     // Minecraft 1.15
        "CAULDRON",                     // Minecraft 1.0
        "CHISELED_BOOKSHELF",           // Minecraft 1.20
        "COMPARATOR",                   // Minecraft 1.5
        "COMPOSTER",                    // Minecraft 1.14
        "CRAFTER",                      // Minecraft 1.21
        "DAYLIGHT_DETECTOR",            // Minecraft 1.5
        "DECORATED_POT",                // Minecraft 1.20
        "FARMLAND",                     // Minecraft 1.0
        "FLOWER_POT",                   // Minecraft 1.4.2
        "LECTERN",                      // Minecraft 1.14
        "LODESTONE",                    // Minecraft 1.16
        "NOTE_BLOCK",                   // Minecraft 1.0
        "REPEATER",                     // Minecraft 1.0
        "VAULT",                        // Minecraft 1.20
        
        // Potted plants - starting with Minecraft 1.13
        "POTTED_ACACIA_SAPLING",        // Minecraft 1.13
        "POTTED_ALLIUM",                // Minecraft 1.13
        "POTTED_AZALEA_BUSH",           // Minecraft 1.17
        "POTTED_AZURE_BLUET",           // Minecraft 1.13
        "POTTED_BAMBOO",                // Minecraft 1.13
        "POTTED_BIRCH_SAPLING",         // Minecraft 1.13
        "POTTED_BLUE_ORCHID",           // Minecraft 1.13
        "POTTED_BROWN_MUSHROOM",        // Minecraft 1.13
        "POTTED_CACTUS",                // Minecraft 1.13
        "POTTED_CHERRY_SAPLING",        // Minecraft 1.20
        "POTTED_CLOSED_EYEBLOSSOM",     // Minecraft 1.21.4
        "POTTED_CORNFLOWER",            // Minecraft 1.13
        "POTTED_CRIMSON_FUNGUS",        // Minecraft 1.16
        "POTTED_CRIMSON_ROOTS",         // Minecraft 1.16
        "POTTED_DANDELION",             // Minecraft 1.13
        "POTTED_DARK_OAK_SAPLING",      // Minecraft 1.13
        "POTTED_DEAD_BUSH",             // Minecraft 1.13
        "POTTED_FERN",                  // Minecraft 1.13
        "POTTED_FLOWERING_AZALEA_BUSH", // Minecraft 1.17
        "POTTED_JUNGLE_SAPLING",        // Minecraft 1.13
        "POTTED_LILY_OF_THE_VALLEY",    // Minecraft 1.13
        "POTTED_MANGROVE_PROPAGULE",    // Minecraft 1.19
        "POTTED_OAK_SAPLING",           // Minecraft 1.13
        "POTTED_OPEN_EYEBLOSSOM",       // Minecraft 1.21.4
        "POTTED_ORANGE_TULIP",          // Minecraft 1.13
        "POTTED_OXEYE_DAISY",           // Minecraft 1.13
        "POTTED_PALE_OAK_SAPLING",      // Minecraft 1.21.4
        "POTTED_PINK_TULIP",            // Minecraft 1.13
        "POTTED_POPPY",                 // Minecraft 1.13
        "POTTED_RED_MUSHROOM",          // Minecraft 1.13
        "POTTED_RED_TULIP",             // Minecraft 1.13
        "POTTED_SPRUCE_SAPLING",        // Minecraft 1.13
        "POTTED_TORCHFLOWER",           // Minecraft 1.20
        "POTTED_WARPED_FUNGUS",         // Minecraft 1.16
        "POTTED_WARPED_ROOTS",          // Minecraft 1.16
        "POTTED_WHITE_TULIP",           // Minecraft 1.13
        "POTTED_WITHER_ROSE",           // Minecraft 1.13
        
        // Sign editing as of Minecraft 1.20
        "ACACIA_SIGN",                  // Minecraft 1.20
        "ACACIA_HANGING_SIGN",          // Minecraft 1.20
        "ACACIA_WALL_SIGN",             // Minecraft 1.20
        "ACACIA_WALL_HANGING_SIGN",     // Minecraft 1.20
        "BAMBOO_SIGN",                  // Minecraft 1.20
        "BAMBOO_HANGING_SIGN",          // Minecraft 1.20
        "BAMBOO_WALL_SIGN",             // Minecraft 1.20
        "BAMBOO_WALL_HANGING_SIGN",     // Minecraft 1.20
        "BIRCH_SIGN",                   // Minecraft 1.20
        "BIRCH_HANGING_SIGN",           // Minecraft 1.20
        "BIRCH_WALL_SIGN",              // Minecraft 1.20
        "BIRCH_WALL_HANGING_SIGN",      // Minecraft 1.20
        "CHERRY_SIGN",                  // Minecraft 1.20
        "CHERRY_HANGING_SIGN",          // Minecraft 1.20
        "CHERRY_WALL_SIGN",             // Minecraft 1.20
        "CHERRY_WALL_HANGING_SIGN",     // Minecraft 1.20
        "CRIMSON_SIGN",                 // Minecraft 1.20
        "CRIMSON_HANGING_SIGN",         // Minecraft 1.20
        "CRIMSON_WALL_SIGN",            // Minecraft 1.20
        "CRIMSON_WALL_HANGING_SIGN",    // Minecraft 1.20
        "DARK_OAK_SIGN",                // Minecraft 1.20
        "DARK_OAK_HANGING_SIGN",        // Minecraft 1.20
        "DARK_OAK_WALL_SIGN",           // Minecraft 1.20
        "DARK_OAK_WALL_HANGING_SIGN",   // Minecraft 1.20
        "JUNGLE_SIGN",                  // Minecraft 1.20
        "JUNGLE_HANGING_SIGN",          // Minecraft 1.20
        "JUNGLE_WALL_SIGN",             // Minecraft 1.20
        "JUNGLE_WALL_HANGING_SIGN",     // Minecraft 1.20
        "MANGROVE_SIGN",                // Minecraft 1.20
        "MANGROVE_HANGING_SIGN",        // Minecraft 1.20
        "MANGROVE_WALL_SIGN",           // Minecraft 1.20
        "MANGROVE_WALL_HANGING_SIGN",   // Minecraft 1.20
        "OAK_SIGN",                     // Minecraft 1.20
        "OAK_HANGING_SIGN",             // Minecraft 1.20
        "OAK_WALL_SIGN",                // Minecraft 1.20
        "OAK_WALL_HANGING_SIGN",        // Minecraft 1.20
        "PALE_OAK_SIGN",                // Minecraft 1.21.4
        "PALE_OAK_HANGING_SIGN",        // Minecraft 1.21.4
        "PALE_OAK_WALL_SIGN",           // Minecraft 1.21.4
        "PALE_OAK_WALL_HANGING_SIGN",   // Minecraft 1.21.4
        "SPRUCE_SIGN",                  // Minecraft 1.20
        "SPRUCE_HANGING_SIGN",          // Minecraft 1.20
        "SPRUCE_WALL_SIGN",             // Minecraft 1.20
        "SPRUCE_WALL_HANGING_SIGN",     // Minecraft 1.20
        "WARPED_SIGN",                  // Minecraft 1.20
        "WARPED_HANGING_SIGN",          // Minecraft 1.20
        "WARPED_WALL_SIGN",             // Minecraft 1.20
        "WARPED_WALL_HANGING_SIGN"      // Minecraft 1.20
    );
    
    public static boolean isMaterialEditOnInteract(Material material)
    {
        return MATERIALS_EDIT_ON_INTERACT.contains(material) || MConf.get().materialsEditOnInteract.contains(material);
    }
    
    // -------------------------------------------- //
    // MATERIAL EDIT TOOLS
    // -------------------------------------------- //
    
    public static final BackstringSet<Material> MATERIALS_EDIT_TOOL = new BackstringSet<>(Material.class, 
        "BUCKET",               // Minecraft 1.0
        "WATER_BUCKET",         // Minecraft 1.0
        "LAVA_BUCKET",          // Minecraft 1.0
        "AXOLOTL_BUCKET",       // Minecraft 1.17
        "COD_BUCKET",           // Minecraft 1.13
        "POWDER_SNOW_BUCKET",   // Minecraft 1.17
        "PUFFERFISH_BUCKET",    // Minecraft 1.13
        "SALMON_BUCKET",        // Minecraft 1.13
        "TADPOLE_BUCKET",       // Minecraft 1.19
        "TROPICAL_FISH_BUCKET", // Minecraft 1.13
        
        "ARMOR_STAND",          // Minecraft 1.8
        "BRUSH",                // Minecraft 1.20
        "END_CRYSTAL",          // Minecraft 1.10
        "FIRE_CHARGE",          // Minecraft 1.0
        "FLINT_AND_STEEL",      // Minecraft 1.0
        "HONEYCOMB",            // Minecraft 1.15
        
        // The duplication bug found in Spigot 1.8 protocol patch
        // https://github.com/MassiveCraft/Factions/issues/693
        // TODO: Are these needed? Maybe only Bone Meal...?
        "CHEST",                // Minecraft 1.0
        "TRAPPED_CHEST",        // Minecraft 1.5
        "IRON_DOOR",            // Minecraft 1.0
        "BONE_MEAL"             // Minecraft 1.0
    );
    
    public static boolean isMaterialEditTool(Material material)
    {
        return MATERIALS_EDIT_TOOL.contains(material) || MConf.get().materialsEditTools.contains(material);
    }
    
    // -------------------------------------------- //
    // MATERIAL DOOR
    // -------------------------------------------- //
    
    // Interacting with these materials placed in the terrain results in door toggling.
    public static final BackstringSet<Material> MATERIALS_DOOR = new BackstringSet<>(Material.class, 
        "ACACIA_DOOR",          // Minecraft 1.8
        "ACACIA_FENCE_GATE",    // Minecraft 1.8
        "ACACIA_TRAPDOOR",      // Minecraft 1.8
        "BAMBOO_DOOR",          // Minecraft 1.20
        "BAMBOO_FENCE_GATE",    // Minecraft 1.20
        "BAMBOO_TRAPDOOR",      // Minecraft 1.20
        "BIRCH_DOOR",           // Minecraft 1.8
        "BIRCH_FENCE_GATE",     // Minecraft 1.8
        "BIRCH_TRAPDOOR",       // Minecraft 1.8
        "CHERRY_DOOR",          // Minecraft 1.20
        "CHERRY_FENCE_GATE",    // Minecraft 1.20
        "CHERRY_TRAPDOOR",      // Minecraft 1.20
        "CRIMSON_DOOR",         // Minecraft 1.16
        "CRIMSON_FENCE_GATE",   // Minecraft 1.16
        "CRIMSON_TRAPDOOR",     // Minecraft 1.16
        "DARK_OAK_DOOR",        // Minecraft 1.8
        "DARK_OAK_FENCE_GATE",  // Minecraft 1.8
        "DARK_OAK_TRAPDOOR",    // Minecraft 1.8
        "JUNGLE_DOOR",          // Minecraft 1.8
        "JUNGLE_FENCE_GATE",    // Minecraft 1.8
        "JUNGLE_TRAPDOOR",      // Minecraft 1.8
        "OAK_DOOR",             // Minecraft 1.8
        "OAK_FENCE_GATE",       // Minecraft 1.8
        "OAK_TRAPDOOR",         // Minecraft 1.8
        "PALE_OAK_DOOR",        // Minecraft 1.21.4
        "PALE_OAK_FENCE_GATE",  // Minecraft 1.21.4
        "PALE_OAK_TRAPDOOR",    // Minecraft 1.21.4
        "SPRUCE_DOOR",          // Minecraft 1.8
        "SPRUCE_FENCE_GATE",    // Minecraft 1.8
        "SPRUCE_TRAPDOOR",      // Minecraft 1.8
        "WARPED_DOOR",          // Minecraft 1.16
        "WARPED_FENCE_GATE",    // Minecraft 1.16
        "WARPED_TRAPDOOR"       // Minecraft 1.16
    );
    
    public static boolean isMaterialDoor(Material material)
    {
        return MATERIALS_DOOR.contains(material) || MConf.get().materialsDoor.contains(material);
    }
    
    // -------------------------------------------- //
    // MATERIAL CONTAINER
    // -------------------------------------------- //
    
    public static final BackstringSet<Material> MATERIALS_CONTAINER = new BackstringSet<>(Material.class, 
        "ANVIL",                    // Minecraft 1.4.2
        "CHIPPED_ANVIL",            // Minecraft 1.4.2
        "DAMAGED_ANVIL",            // Minecraft 1.4.2
        
        "BARREL",                   // Minecraft 1.14
        "BEACON",                   // Minecraft 1.4.2
        "BLAST_FURNACE",            // Minecraft 1.14
        "BREWING_STAND",            // Minecraft 1.0
        "CHEST",                    // Minecraft 1.0
        "DISPENSER",                // Minecraft 1.0
        "DROPPER",                  // Minecraft 1.5
        "ENCHANTING_TABLE",         // Minecraft 1.0
        "ENDER_CHEST",              // Minecraft 1.3.1
        "FURNACE",                  // Minecraft 1.0
        "HOPPER",                   // Minecraft 1.5
        "JUKEBOX",                  // Minecraft 1.0
        "RESPAWN_ANCHOR",           // Minecraft 1.16
        "SMOKER",                   // Minecraft 1.14
        "TRAPPED_CHEST",            // Minecraft 1.5
        
        // Shulker Boxes
        "SHULKER_BOX",              // Minecraft 1.11
        "BLACK_SHULKER_BOX",        // Minecraft 1.11
        "BLUE_SHULKER_BOX",         // Minecraft 1.11
        "BROWN_SHULKER_BOX",        // Minecraft 1.11
        "CYAN_SHULKER_BOX",         // Minecraft 1.11
        "GRAY_SHULKER_BOX",         // Minecraft 1.11
        "GREEN_SHULKER_BOX",        // Minecraft 1.11
        "LIGHT_BLUE_SHULKER_BOX",   // Minecraft 1.11
        "LIGHT_GRAY_SHULKER_BOX",   // Minecraft 1.11 (renamed SILVER_SHULKER_BOX)
        "LIME_SHULKER_BOX",         // Minecraft 1.11
        "MAGENTA_SHULKER_BOX",      // Minecraft 1.11
        "ORANGE_SHULKER_BOX",       // Minecraft 1.11
        "PINK_SHULKER_BOX",         // Minecraft 1.11
        "PURPLE_SHULKER_BOX",       // Minecraft 1.11
        "RED_SHULKER_BOX",          // Minecraft 1.11
        "SILVER_SHULKER_BOX",       // Minecraft 1.11
        "WHITE_SHULKER_BOX",        // Minecraft 1.11
        "YELLOW_SHULKER_BOX"        // Minecraft 1.11
    );
    
    public static boolean isMaterialContainer(Material material)
    {
        return MATERIALS_CONTAINER.contains(material) || MConf.get().materialsContainer.contains(material);
    }

    // -------------------------------------------- //
    // MATERIAL BUTTON
    // -------------------------------------------- //

    public static final BackstringSet<Material> MATERIALS_BUTTON = new BackstringSet<>(Material.class,
        "ACACIA_BUTTON",              // Minecraft 1.13
        "BAMBOO_BUTTON",              // Minecraft 1.20
        "BIRCH_BUTTON",               // Minecraft 1.13
        "CHERRY_BUTTON",              // Minecraft 1.20
        "CRIMSON_BUTTON",             // Minecraft 1.16
        "DARK_OAK_BUTTON",            // Minecraft 1.13
        "JUNGLE_BUTTON",              // Minecraft 1.13
        "LEGACY_WOOD_BUTTON",         // Minecraft 1.13 (deprecated)
        "LEGACY_STONE_BUTTON",        // Minecraft 1.13 (deprecated
        "MANGROVE_BUTTON",            // Minecraft 1.19
        "OAK_BUTTON",                 // Minecraft 1.4.2
        "PALE_OAK_BUTTON",            // Minecraft 1.21.4
        "POLISHED_BLACKSTONE_BUTTON", // Minecraft 1.16
        "SPRUCE_BUTTON",              // Minecraft 1.13
        "STONE_BUTTON",               // Minecraft 1.0
        "WARPED_BUTTON"               // Minecraft 1.16
    );

    public static boolean isMaterialButton(Material material)
    {
        return MATERIALS_BUTTON.contains(material) || MConf.get().materialsButton.contains(material);
    }

    // -------------------------------------------- //
    // MATERIAL PRESSURE PLATE
    // -------------------------------------------- //

    public static final BackstringSet<Material> MATERIALS_PRESSURE_PLATES = new BackstringSet<>(Material.class,
        "ACACIA_PRESSURE_PLATE",              // Minecraft 1.13
        "BAMBOO_PRESSURE_PLATE",              // Minecraft 1.20
        "BIRCH_PRESSURE_PLATE",               // Minecraft 1.13
        "CHERRY_PRESSURE_PLATE",              // Minecraft 1.20
        "CRIMSON_PRESSURE_PLATE",             // Minecraft 1.16
        "DARK_OAK_PRESSURE_PLATE",            // Minecraft 1.13
        "HEAVY_WEIGHTED_PRESSURE_PLATE",      // Minecraft 1.5
        "JUNGLE_PRESSURE_PLATE",              // Minecraft 1.13
        "LIGHT_WEIGHTED_PRESSURE_PLATE",      // Minecraft 1.5
        "MANGROVE_PRESSURE_PLATE",            // Minecraft 1.19
        "OAK_PRESSURE_PLATE",                 // Minecraft 1.0
        "PALE_OAK_PRESSURE_PLATE",            // Minecraft 1.21.4
        "POLISHED_BLACKSTONE_PRESSURE_PLATE", // Minecraft 1.16
        "SPRUCE_PRESSURE_PLATE",              // Minecraft 1.13
        "STONE_PRESSURE_PLATE",               // Minecraft 1.0
        "WARPED_PRESSURE_PLATE"               // Minecraft 1.16
    );

    public static boolean isMaterialPressurePlate(Material material)
    {
        return MATERIALS_PRESSURE_PLATES.contains(material) || MConf.get().materialsPressurePlate.contains(material);
    }
    
    // -------------------------------------------- //
    // ENTITY TYPE EDIT ON INTERACT
    // -------------------------------------------- //
    
    // Interacting with these entities results in an edit.
    public static final BackstringSet<EntityType> ENTITY_TYPES_EDIT_ON_INTERACT = new BackstringSet<>(EntityType.class,
        "ARMOR_STAND",          // Minecraft 1.8
        "GLOW_ITEM_FRAME",      // Minecraft 1.17
        "ITEM_FRAME",           // Minecraft 1.4.2
        "LEASH_KNOT"            // Minecraft 1.6.1
    );
    
    public static boolean isEntityTypeEditOnInteract(EntityType entityType)
    {
        return ENTITY_TYPES_EDIT_ON_INTERACT.contains(entityType)
                || MConf.get().entityTypesEditOnInteract.contains(entityType);
    }
    
    // -------------------------------------------- //
    // ENTITY TYPE EDIT ON DAMAGE
    // -------------------------------------------- //
    
    // Damaging these entities results in an edit.
    public static final BackstringSet<EntityType> ENTITY_TYPES_EDIT_ON_DAMAGE = new BackstringSet<>(EntityType.class,
        "ARMOR_STAND",          // Minecraft 1.8
        "ENDER_CRYSTAL",        // Minecraft 1.10
        "GLOW_ITEM_FRAME",      // Minecraft 1.17
        "ITEM_FRAME",           // Minecraft 1.4.2
        "LEASH_KNOT"            // Minecraft 1.6.1
    );
    
    public static boolean isEntityTypeEditOnDamage(EntityType entityType)
    {
        return ENTITY_TYPES_EDIT_ON_DAMAGE.contains(entityType) || MConf.get().entityTypesEditOnDamage.contains(entityType);
    }
    
    // -------------------------------------------- //
    // ENTITY TYPE CONTAINER
    // -------------------------------------------- //
    
    public static final BackstringSet<EntityType> ENTITY_TYPES_CONTAINER = new BackstringSet<>(EntityType.class,
        "ACACIA_CHEST_BOAT",    // Minecraft 1.19
        "BAMBOO_CHEST_RAFT",    // Minecraft 1.20
        "BIRCH_CHEST_BOAT",     // Minecraft 1.19
        "CHERRY_CHEST_BOAT",    // Minecraft 1.20
        "DARK_OAK_CHEST_BOAT",  // Minecraft 1.19
        "JUNGLE_CHEST_BOAT",    // Minecraft 1.19
        "MANGROVE_CHEST_BOAT",  // Minecraft 1.19
        "OAK_CHEST_BOAT",       // Minecraft 1.19
        "PALE_OAK_CHEST_BOAT",  // Minecraft 1.21.4
        "SPRUCE_CHEST_BOAT",    // Minecraft 1.19
        "MINECART_CHEST",       // Minecraft 1.0
        "MINECART_HOPPER"       // Minecraft 1.5
    );
    
    public static boolean isEntityTypeContainer(EntityType entityType)
    {
        return ENTITY_TYPES_CONTAINER.contains(entityType) || MConf.get().entityTypesContainer.contains(entityType);
    }
    
    // -------------------------------------------- //
    // ENTITY TYPE MONSTER
    // -------------------------------------------- //
    
    // https://minecraft.wiki/w/Mob#Hostile_mobs
    // https://minecraft.wiki/w/Mob#Neutral_mobs (some of these are considered animals)
    public static final BackstringSet<EntityType> ENTITY_TYPES_MONSTER = new BackstringSet<>(EntityType.class, 
        // - - - Hostile Mobs - - -    
        "BLAZE",            // Minecraft 1.0
        "BOGGED",           // Minecraft 1.21
        "BREEZE",           // Minecraft 1.21
        "CREAKING",         // Minecraft 1.21.4
        "CREEPER",          // Minecraft 1.0
        "ELDER_GUARDIAN",   // Minecraft 1.8
        "ENDERMITE",        // Minecraft 1.8
        "EVOKER",           // Minecraft 1.11
        "GHAST",            // Minecraft 1.0
        "GUARDIAN",         // Minecraft 1.8
        "HOGLIN",           // Minecraft 1.16
        "HUSK",             // Minecraft 1.10
        "MAGMA_CUBE",       // Minecraft 1.0
        "PHANTOM",          // Minecraft 1.13
        "PIGLIN_BRUTE",     // Minecraft 1.16
        "PILLAGER",         // Minecraft 1.14
        "RAVAGER",          // Minecraft 1.14
        "SHULKER",          // Minecraft 1.10
        "SILVERFISH",       // Minecraft 1.0
        "SKELETON",         // Minecraft 1.0
        "SLIME",            // Minecraft 1.0
        "STRAY",            // Minecraft 1.10
        "VEX",              // Minecraft 1.11
        "VINDICATOR",       // Minecraft 1.11
        "WARDEN",           // Minecraft 1.19
        "WITCH",            // Minecraft 1.4.2
        "WITHER_SKELETON",  // Minecraft 1.4.2
        "ZOGLIN",           // Minecraft 1.16
        "ZOMBIE",           // Minecraft 1.0
        "ZOMBIE_VILLAGER",  // Minecraft 1.4.2
        "PIG_ZOMBIE",       // Minecraft 1.0
        "ZOMBIFIED_PIGLIN", // Minecraft 1.16 (rename of PIG_ZOMBIE)

        // - - - Boss Mobs - - -
        "ENDER_DRAGON",     // Minecraft 1.0
        "WITHER",           // Minecraft 1.4.2

        // - - - Neutral Mobs - - -
        "CAVE_SPIDER",      // Minecraft 1.0
        "DROWNED",          // Minecraft 1.13
        "ENDERMAN",         // Minecraft 1.0
        "PIGLIN",           // Minecraft 1.16
        "POLAR_BEAR",       // Minecraft 1.10
        "SPIDER",           // Minecraft 1.0

        // - - - Passive Mobs - - -
        "SKELETON_HORSE",   // Minecraft 1.6.1

        // - - - Unused Mobs - - -
        "GIANT",            // Minecraft 1.0
        "ILLUSIONER",       // Minecraft 1.12
        "ZOMBIE_HORSE"      // Minecraft 1.6.1
    );
    
    public static boolean isEntityTypeMonster(EntityType entityType)
    {
        return ENTITY_TYPES_MONSTER.contains(entityType) || MConf.get().entityTypesMonsters.contains(entityType);
    }
    
    // -------------------------------------------- //
    // ENTITY TYPE ANIMAL
    // -------------------------------------------- //
    
    // https://minecraft.wiki/w/Mob#Passive_mobs
    // https://minecraft.wiki/w/Mob#Neutral_mobs (some of these are considered monsters)
    public static final BackstringSet<EntityType> ENTITY_TYPES_ANIMAL = new BackstringSet<>(EntityType.class, 
        // - - - Passive Mobs - - -
        "ALLAY",            // Minecraft 1.19
        "ARMADILLO",        // Minecraft 1.20.5
        "AXOLOTL",          // Minecraft 1.17
        "BAT",              // Minecraft 1.4.2
        "CAMEL",            // Minecraft 1.20
        "CAT",              // Minecraft 1.14
        "CHICKEN",          // Minecraft 1.0
        "COD",              // Minecraft 1.13
        "COW",              // Minecraft 1.0
        "DONKEY",           // Minecraft 1.6.1
        "FROG",             // Minecraft 1.19
        "GLOW_SQUID",       // Minecraft 1.17
        "HAPPY_GHAST",      // Minecraft 1.21.6
        "HORSE",            // Minecraft 1.6.1
        "MUSHROOM_COW",     // Minecraft 1.0
        "MOOSHROOM",        // Minecraft 1.11 (rename of MUSHROOM_COW)
        "MULE",             // Minecraft 1.6.1
        "OCELOT",           // Minecraft 1.2.1
        "PARROT",           // Minecraft 1.12
        "PIG",              // Minecraft 1.0
        "PUFFERFISH",       // Minecraft 1.13
        "RABBIT",           // Minecraft 1.8
        "SALMON",           // Minecraft 1.13
        "SHEEP",            // Minecraft 1.0
        "SKELETON_HORSE",   // Minecraft 1.6.1
        "SNIFFER",          // Minecraft 1.20
        "SQUID",            // Minecraft 1.0
        "STRIDER",          // Minecraft 1.16
        "TADPOLE",          // Minecraft 1.19
        "TROPICAL_FISH",    // Minecraft 1.13
        "TURTLE",           // Minecraft 1.13

        // - - - Neutral Mobs - - -
        "BEE",              // Minecraft 1.15
        "DOLPHIN",          // Minecraft 1.13
        "FOX",              // Minecraft 1.14
        "GOAT",             // Minecraft 1.17
        "LLAMA",            // Minecraft 1.14
        "LLAMA_SPIT",       // Minecraft 1.14
        "PANDA",            // Minecraft 1.14
        "POLAR_BEAR",       // Minecraft 1.10
        "WOLF",             // Minecraft 1.0

        // - - - Unused Mobs - - -
        "ZOMBIE_HORSE"      // Minecraft 1.6.1
    );
    
    public static boolean isEntityTypeAnimal(EntityType entityType)
    {
        return ENTITY_TYPES_ANIMAL.contains(entityType) || MConf.get().entityTypesAnimals.contains(entityType);
    }
    
}
