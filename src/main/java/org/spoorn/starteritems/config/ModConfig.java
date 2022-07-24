package org.spoorn.starteritems.config;

import draylar.omegaconfig.OmegaConfig;
import draylar.omegaconfig.api.Comment;
import draylar.omegaconfig.api.Config;
import org.spoorn.starteritems.StarterItems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModConfig implements Config {
    
    private static ModConfig CONFIG;
    
    @Comment("Set to true to clear the player's inventory of any non-starter items listed in this config upon player login. [default = false]\n" +
            "This is useful for cases where mods in a modpack may actually add their own items to a player's inventory when\n" +
            "\tthey first join a world.  Setting this to true will attempt to load StarterItems's logic AFTER other mods, and\n" +
            "\tclear the inventory before.  There is a chance that other mods try to do the same, in which case, if you notice\n" +
            "\tthe player's inventory is not actually cleared before adding starterItems, please submit an issue to\n" +
            "\thttps://github.com/spoorn/StarterItems/issues, and I can add compatibility.")
    public boolean clearInventoryBeforeGivingItems = false;
    
    @Comment("List of items that should be given to a player when they first join a world.  This should be the full item Identifier.\n" +
            "The item identifier can be prefixed with the count for that item to give to the player.\n" +
            "The item identifier can be followed by NBT data for the item stack.  See below for an example NBT data on an iron_sword.\n" +
            "\nExample:\n" +
            "\t\"starterItems\": [\n" +
            "\t\t\"minecraft:diamond\",\n" +
            "\t\t\"minecraft:iron_sword {Damage:10}\",\n" +
            "\t\t\"5 minecraft:apple\",\n" +
            "\t\t\"20 minecraft:bread\"\n" +
            "\t]")
    public List<String> starterItems = new ArrayList<>();
    
    @Comment("List of messages to send to players when they join a world for the first time.  Each string in the array will be\n" +
            "a separate message sent to the player.\n" +
            "This is a list of Message objects which contain a required 'text' string field, and optional formatting fields such as color.\n" +
            "\nMessage Specification:\n" +
            "\t{\n" +
            "\t\t\"text\": \"Message Text\",\n" +
            "\t\t(optional) \"color\": \"<#Hex Color>|<RGB Decimal>\"\n" +
            "\t}" +
            "\n\nExample:\n" +
            "\t\"firstJoinMessages\": [\n" +
            "\t\t{\n" +
            "\t\t\t\"text\": \"Welcome to the Oasis!\",\n" +
            "\t\t\t\"color\": \"#47f5af\"\n" +
            "\t\t},\n" +
            "\t\t{\n" +
            "\t\t\t\"text\": \"Ready Player One?\",\n" +
            "\t\t\t\"color\": \"16074611\"\n" +
            "\t\t}\n" +
            "\t]")
    public List<Message> firstJoinMessages = Arrays.asList(
            new Message("Welcome to the Oasis!", "#47f5af"),
            new Message("Ready Player One?", "16074611")
    );

    @Comment("List of messages to send to players every time they join the world, except for the first time.  If you want\n" +
            "to send messages to players for the first time they join the world, set messages in firstJoinMessages.\n" +
            "The schema for this list is the same in firstJoinMessages.  See the documentation above for details and examples.")
    public List<Message> welcomeMessages = Arrays.asList(
            new Message("Welcome back to the Oasis!", "#47f5af")
    );
    
    @Comment("List of commands to trigger on each server startup.\n" +
            "Make sure if the command contains Strings, that you escape the quote characters (i.e. \\\")\n" +
            "For example, this can be used to disable Terralith Traveler's Map for Terralith version v2.2.1a due to\n" +
            "\ttimeout issues: https://github.com/spoorn/myLoot/issues/22#issuecomment-1193212917\n" +
            "\nExample:\n" +
            "\t\"serverStartCommands\": [\n" +
            "\t\t\"/scoreboard objectives add tr.disable_maps dummy\",\n" +
            "\t\t\"/scoreboard players set %DISABLE_MAP tr.disable_maps 1\",\n" +
            "\t\t\"/gamerule playersSleepingPercentage 25\"\n" +
            "\t]")
    public List<String> serverStartCommands = new ArrayList<>();
    
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
