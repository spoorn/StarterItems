package org.spoorn.starteritems;

import lombok.extern.log4j.Log4j2;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.spoorn.starteritems.config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class StarterItems implements ModInitializer {
    
    public static final String MODID = "starteritems";
    private static final String JOINED_ID = MODID + "_joined";
    private static final Pattern ITEM_REGEX = Pattern.compile("^((?<count>\\d+)\\s+)?\\s*(?<item>[\\w:]+)$");
    
    @Override
    public void onInitialize() {
        log.info("Hello from StarterItems!");

        // Config
        ModConfig.init();
        
        // Validate and cache starter items
        List<String> starterItems = ModConfig.get().starterItems;
        List<ItemStack> starterItemStacks = new ArrayList<>();
        for (String starterItem : starterItems) {
            Matcher matcher = ITEM_REGEX.matcher(starterItem.trim());
            
            if (!matcher.matches()) {
                throw new RuntimeException("Starter Item {" + starterItem + "} is not in a valid format.  " +
                        "Please check the config file at config/starteritems.json5 for acceptable formats.");
            }
            
            String countStr = matcher.group("count");
            String itemStr = matcher.group("item");

            Identifier identifier = new Identifier(itemStr);
            if (!Registry.ITEM.containsId(identifier)) {
                throw new RuntimeException("Starter Item {" + itemStr + "} does not exist in the Item registry.  " +
                        "Are you sure the identifier is correct?");
            }
            
            Item item = Registry.ITEM.get(identifier);
            int count = countStr == null ? 1 : Integer.parseInt(countStr);
            
            starterItemStacks.add(new ItemStack(item, count));
        }
        
        // On player log in, give starter items
        if (!starterItemStacks.isEmpty()) {
            log.info("StarterItems={} loaded", starterItemStacks);
            
            ServerEntityEvents.ENTITY_LOAD.register(((entity, world) -> {
                if (entity instanceof ServerPlayerEntity player) {
                    Set<String> scoreboardTags = player.getScoreboardTags();
                    if (!scoreboardTags.contains(JOINED_ID)) {
                        log.info("Player={} is joining the world for the first time.  Giving them starter items...", player);

                        if (!player.addScoreboardTag(JOINED_ID)) {
                            throw new RuntimeException("Player " + player + " scoreboard tags are full!  Might need to find a different way to track player joined worlds");
                        }

                        for (ItemStack itemStack : starterItemStacks) {
                            boolean insertedItem = player.getInventory().insertStack(itemStack);
                            if (!insertedItem) {
                                log.error("[StarterItems] Player={} inventory is full!  Cannot add anymore starter items at={} and after", player, itemStack);
                                break;
                            }
                        }
                    }
                }
            }));
        }
    }
}
