package org.spoorn.starteritems.config;

import draylar.omegaconfig.OmegaConfig;
import draylar.omegaconfig.api.Comment;
import draylar.omegaconfig.api.Config;
import org.spoorn.starteritems.StarterItems;

import java.util.ArrayList;
import java.util.List;

public class ModConfig implements Config {
    
    private static ModConfig CONFIG;
    
    @Comment("Set to true to clear the player's inventory before adding starter items listed in this config file. [default = false]\n" +
            "This is useful for cases where mods in a modpack may actually add their own items to a player's inventory when\n" +
            "\tthey first join a world.  Setting this to true will attempt to load StarterItems's logic AFTER other mods, and\n" +
            "\tclear the inventory before.  There is a chance that other mods try to do the same, in which case, if you notice\n" +
            "\tthe player's inventory is not actually cleared before adding starterItems, please submit an issue to\n" +
            "\thttps://github.com/spoorn/StarterItems/issues, and I can add compatibility.")
    public boolean clearInventoryBeforeGivingItems = false;
    
    @Comment("List of items that should be given to a player when they first join a world.  This should be the full item Identifier.\n" +
            "The item identifier can be prefixed with the count for that item to give to the player.\n" +
            "\nExample:\n" +
            "\t\"starterItems\": [\n" +
            "\t\t\"minecraft:diamond\",\n" +
            "\t\t\"minecraft:iron_sword\",\n" +
            "\t\t\"5 minecraft:apple\",\n" +
            "\t\t\"20 minecraft:bread\"\n" +
            "\t]")
    public List<String> starterItems = new ArrayList<>();
    
    public static void init() {
        CONFIG = OmegaConfig.register(ModConfig.class);
    }
    
    public static ModConfig get() {
        return CONFIG;
    }

    @Override
    public String getName() {
        return StarterItems.MODID;
    }

    @Override
    public String getExtension() {
        // For nicer comments parsing in text editors
        return "json5";
    }
}
